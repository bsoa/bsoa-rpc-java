/*
 * Copyright 2016 The BSOA Project
 *
 * The BSOA Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.bsoa.rpc.client;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.bootstrap.ConsumerBootstrap;
import io.bsoa.rpc.codec.SerializerFactory;
import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.BsoaVersion;
import io.bsoa.rpc.common.struct.ListDifference;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.config.RegistryConfig;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.context.RpcContext;
import io.bsoa.rpc.context.RpcStatus;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.context.AsyncContext;
import io.bsoa.rpc.listener.ConsumerStateListener;
import io.bsoa.rpc.listener.ResponseListener;
import io.bsoa.rpc.message.HeadKey;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.ResponseFuture;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.registry.RegistryFactory;
import io.bsoa.rpc.transport.ClientTransport;
import io.bsoa.rpc.transport.ClientTransportConfig;
import io.bsoa.rpc.transport.ClientTransportUtils;

/**
 * Created by zhangg on 16-7-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public abstract class AbstractClient extends Client {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractClient.class);

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务端消费者启动器
     */
    public AbstractClient(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
        this.consumerConfig = consumerBootstrap.getConsumerConfig();
    }

    /**
     * 连接管理器
     */
    protected ConnectionHolder connectionHolder;

    /**
     * 当前服务集群对应的Consumer信息
     */
    protected ConsumerConfig<?> consumerConfig;

    /**
     * 是否已启动(已建立连接)
     */
    protected volatile boolean inited = false;

    /**
     * 是否已经销毁（已经销毁不能再继续使用）
     */
    protected volatile boolean destroyed = false;

    /**
     * 当前Client正在发送的调用数量
     */
    protected AtomicInteger countOfInvoke = new AtomicInteger(0);

    /**
     * 路由规则接口，在负载均衡之前
     */
    private volatile List<Router> routers;

    /**
     * 负载均衡接口
     */
    private volatile LoadBalancer loadBalancer;

    @Override
    public void init() {
        this.consumerConfig = consumerConfig;

        // 负载均衡策略 考虑是否可动态替换？
        String lb = consumerConfig.getLoadBalancer();
        loadBalancer = LoadBalancerFactory.getLoadBalancer(consumerConfig);

        routers = consumerConfig.getRouter();
        if (routers == null) { // 未手动配置，取注册中心的配置
            routers = initRouterByRule();
        }
        // 连接管理器
        connectionHolder = ConnectionHolderFactory.getConnectionHolder(consumerConfig.getConnectionHolder());
        if (consumerConfig.isLazy()) { // 延迟连接
            LOGGER.info("Connect will lazy init when first invoke.");
        } else { // 建立连接
            initConnections();
        }
    }

    /**
     * 从规则中build一个路由器列表
     */
    private List<Router> initRouterByRule() {
        List<Router> routers = null;
//        TODO
//        String interfaceId = consumerConfig.getInterfaceId();
//        boolean open = CommonUtils.isTrue(BsoaContext.getInterfaceVal(interfaceId, BsoaConstants.SETTING_ROUTER_OPEN, "true"));
//        if (open) {
//            String routerRule = BsoaContext.getInterfaceVal(interfaceId, BsoaConstants.SETTING_ROUTER_RULE, null);
//            if (StringUtils.isNotBlank(routerRule)) {
//                routers = RouterFactory.getRouters(routerRule);
//            }
//        }
        return routers;
    }

    /**
     * 和服务端建立连接
     */
    private void initConnections() {
        if (inited) { // 已初始化
            return;
        }
        if (destroyed) { // 已销毁
            throw new BsoaRuntimeException(22001, "Client has been destroyed!");
        }
        synchronized (this) {
            if (inited) {
                return;
            }
            // 启动重连线程
            connectionHolder.init(consumerConfig);
            try {
                // 得到服务端列表
                List<ProviderInfo> tmpProviderInfoList = buildProviderList();
                connectionHolder.addProvider(tmpProviderInfoList); // 初始化服务端连接（建立长连接)
            } catch (BsoaRuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new BsoaRuntimeException(22222, "[JSF-22002]Init provider's transport error!", e);
            }
            // 如果check=true表示强依赖
            if (consumerConfig.isCheck() && connectionHolder.isAvailableEmpty()) {
                throw new BsoaRuntimeException(22222, "[JSF-22003]The consumer is depend on alive provider " +
                        "and there is no alive provider, you can ignore it " +
                        "by <jsf:consumer check=\"false\"> (default is false)");
            }
            // 启动成功
            inited = true;
        }
    }

    /**
     * 从直连地址或者注册中心得到服务端列表
     *
     * @return 服务端列表 provider list
     */
    protected List<ProviderInfo> buildProviderList() {
        List<ProviderInfo> tmpProviderInfoList;
        String url = consumerConfig.getUrl();
        if (StringUtils.isNotEmpty(url)) { // 如果走直连
            tmpProviderInfoList = new ArrayList<ProviderInfo>();
            String cInterfaceId = consumerConfig.getInterfaceId();
            String cTags = consumerConfig.getTags();
            String cProtocol = consumerConfig.getProtocol();
            String[] providerStrs = StringUtils.splitWithCommaOrSemicolon(url);
            for (String providerStr : providerStrs) {
                ProviderInfo providerInfo = ProviderInfo.valueOf(providerStr);
                if (!CommonUtils.isFalse(consumerConfig.getParameter(BsoaConstants.HIDDEN_KEY_WARNNING))) {
                    if (!providerInfo.getProtocolType().equals(cProtocol)) {
                        throw ExceptionUtils.buildRuntime(21308, "consumer.url", url,
                                "there is a mismatch protocol between url[" + providerInfo.getProtocolType()
                                        + "] and consumer[" + cProtocol + "]");
                    }
                    String pInterfaceId = providerInfo.getInterfaceId();
                    if (pInterfaceId != null) {
                        if (!pInterfaceId.equals(cInterfaceId)) {
                            throw ExceptionUtils.buildRuntime(21308, "consumer.url", url,
                                    "there is a mismatch interfaceId between url[" + pInterfaceId
                                            + "] and consumer[" + cInterfaceId + "]");
                        }
                    } else {
                        providerInfo.setInterfaceId(cInterfaceId);
                    }
                    String pTags = providerInfo.getTags();
                    if (pTags != null) {
                        if (!pTags.equals(cTags)) {
                            try {
                                throw ExceptionUtils.buildRuntime(21308, "consumer.url", url,
                                        "there is a mismatch alias between url[" + pTags
                                                + "] and consumer[" + cTags + "]");
                            } catch (Exception e) {
                                LOGGER.warn(e.getMessage());
                            }
                        }
                    } else {
                        providerInfo.setTags(cTags);// 强制
                    }
                }
                tmpProviderInfoList.add(providerInfo);
            }
        } else { // 没有配置url直连
            List<RegistryConfig> registryConfigs = consumerConfig.getRegistry();
            if (CommonUtils.isEmpty(registryConfigs)) { // registry为空
                // 走默认的注册中心
                LOGGER.debug("Registry is undefined, will use default registry config instead");
                registryConfigs = new ArrayList<RegistryConfig>();
                registryConfigs.add(RegistryFactory.defaultConfig());
                consumerConfig.setRegistry(registryConfigs); // 注入进去
            }
            // 从多个注册中心订阅服务列表
            tmpProviderInfoList = consumerBootstrap.subscribe();
        }
        return tmpProviderInfoList;
    }

    /**
     * 增加Provider
     *
     * @param providerInfos Provider列表
     */
    public void addProvider(List<ProviderInfo> providerInfos) {
        connectionHolder.addProvider(providerInfos);
    }

    /**
     * 删除Provider
     *
     * @param providerInfos Provider列表
     */
    public void removeProvider(List<ProviderInfo> providerInfos) {
        connectionHolder.removeProvider(providerInfos);
    }

    /**
     * 更新Provider
     *
     * @param newProviderInfos Provider列表
     */
    public void updateProvider(List<ProviderInfo> newProviderInfos) {
        try {
            if (CommonUtils.isEmpty(newProviderInfos)) {
                if (CommonUtils.isNotEmpty(connectionHolder.currentProviderList())) {
                    LOGGER.info("Clear all providers, may be this consumer has been add to blacklist");
                    closeTransports();
                }
            } else {
                Collection<ProviderInfo> nowall = connectionHolder.currentProviderList();
                List<ProviderInfo> nowAllP = new ArrayList<ProviderInfo>(nowall);// 当前全部

                // 比较当前的和最新的
                ListDifference<ProviderInfo> diff = new ListDifference<ProviderInfo>(newProviderInfos, nowAllP);
                List<ProviderInfo> needAdd = diff.getOnlyOnLeft(); // 需要新建
                List<ProviderInfo> needDelete = diff.getOnlyOnRight(); // 需要删掉
                if (!needAdd.isEmpty()) {
                    addProvider(needAdd);
                }
                if (!needDelete.isEmpty()) {
                    removeProvider(needDelete);
                }
            }
        } catch (Exception e) {
            LOGGER.error("update " + consumerConfig.getInterfaceId() + " provider (" + newProviderInfos.size()
                    + ") from list error:", e);
        }
    }

    /**
     * Provider对象得到 ClientTransportConfig
     *
     * @param providerInfo Provider
     * @return ClientTransportConfig
     */
    private ClientTransportConfig providerToClientConfig(ProviderInfo providerInfo) {
        ClientTransportConfig config = new ClientTransportConfig();
        config.setProviderInfo(providerInfo);
        config.setConnectTimeout(config.getConnectTimeout());
        config.setInvokeTimeout(consumerConfig.getTimeout());
        config.setChannelListeners(consumerConfig.getOnConnect());
        return config;
    }

    /**
     * 是否可用（即有可用的服务端）
     *
     * @return 是/否
     */
    public boolean isAvailable() {
        if (destroyed || !inited)
            return false;
        if (connectionHolder.isAvailableEmpty())
            return false;
        for (Map.Entry<ProviderInfo, ClientTransport> entry : connectionHolder.getAvailableConnections().entrySet()) {
            ProviderInfo providerInfo = entry.getKey();
            ClientTransport transport = entry.getValue();
            if (transport.isAvailable()) {
                return true;
            } else {
                connectionHolder.setUnavailable(providerInfo, transport);
            }
        }
        return false;
    }

    /**
     * 检查状态是否变化，变化则通知监听器
     *
     * @param originalState 原始状态
     */
    public void checkStateChange(boolean originalState) {
        if (originalState) { // 原来可以
            if (!isAvailable()) { // 变不可以
                notifyStateChangeToUnavailable();
            }
        } else { // 原来不可用
            if (isAvailable()) { // 变成可用
                notifyStateChangeToAvailable();
            }
        }
    }

    /**
     * 通知状态变成不可用,主要是：<br>
     * 1.注册中心删除，更新节点后变成不可用时<br>
     * 2.连接断线后（心跳+调用），如果是可用节点为空
     */
    public void notifyStateChangeToUnavailable() {
        final List<ConsumerStateListener> onprepear = consumerConfig.getOnAvailable();
        if (onprepear != null) {
            AsyncContext.getAsyncThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    // 状态变化通知监听器
                    for (ConsumerStateListener listener : onprepear) {
                        try {
                            listener.onUnavailable(consumerBootstrap.getProxyIns());
                        } catch (Exception e) {
                            LOGGER.error("Failed to notify consumer state listener when state change to unavailable");
                        }
                    }
                }
            });
        }
    }

    /**
     * 通知状态变成可用,主要是：<br>
     * 1.启动成功变成可用时<br>
     * 2.注册中心增加，更新节点后变成可用时<br>
     * 3.重连上从一个可用节点都没有-->有可用节点时
     */
    public void notifyStateChangeToAvailable() {
        final List<ConsumerStateListener> onprepear = consumerConfig.getOnAvailable();
        if (onprepear != null) {
            AsyncContext.getAsyncThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    // 状态变化通知监听器
                    for (ConsumerStateListener listener : onprepear) {
                        try {
                            listener.onAvailable(consumerBootstrap.getProxyIns());
                        } catch (Exception e) {
                            LOGGER.error("Failed to notify consumer state listener when state change to available");
                        }
                    }
                }
            });
        }
    }


    /**
     * 调用
     *
     * @param msg Request对象
     * @return 调用结果
     */
    public RpcResponse sendMsg(RpcRequest msg) {
        // 做一些初始化检查，例如未连接可以连接
        try {
            countOfInvoke.incrementAndGet(); // 计数
            initConnections();
            return doSendMsg(msg);
        } finally {
            countOfInvoke.decrementAndGet();
        }
    }

    /**
     * 子类实现各自逻辑的调用，例如重试等
     *
     * @param msg Request对象
     * @return 调用结果
     */
    protected abstract RpcResponse doSendMsg(RpcRequest msg);

    /**
     * 调用客户端
     *
     * @param transport 客户端连接
     * @param msg       Request对象
     * @return 调用结果
     */
    protected RpcResponse sendMsg0(ClientTransport transport, RpcRequest msg) {
        ProviderInfo providerInfo = transport.getConfig().getProviderInfo();
        try {
//            checkProviderVersion(provider, msg); // 根据服务端版本特殊处理
            String interfaceId = msg.getInterfaceName();
            String methodName = msg.getMethodName();
            boolean async = consumerConfig.getMethodAsync(methodName);
            int timeout = consumerConfig.getMethodTimeout(methodName);
            Boolean genericAsync = (Boolean) msg.getAttachment(BsoaConstants.INTERNAL_KEY_ASYNC);
            RpcResponse response = null;

            // 异步调用
            if (async || CommonUtils.isTrue(genericAsync)) {
                // 接口或者方法级 回调监听器
                List<ResponseListener> onreturns = consumerConfig.getMethodOnreturn(methodName);
                // 方法级的 回调监听器
                ResponseListener methodResponseListener = (ResponseListener)
                        RpcContext.getContext().getAttachment(BsoaConstants.INTERNAL_KEY_ONRETURN);

                // 调用级 回调监听器
                ResponseListener genericAsynReturn = (ResponseListener) msg.getAttachment(BsoaConstants.INTERNAL_KEY_ONRETURN);

                // 开始调用
                ResponseFuture future = transport.asyncSend(msg, timeout);
                if (onreturns != null) {
                    future.addListeners(onreturns);
                }
                if (methodResponseListener != null) {
                    future.addListener(methodResponseListener);
                }
                if (genericAsynReturn != null) {
                    msg.addAttachment(BsoaConstants.INTERNAL_KEY_ONRETURN, null);
                    future.addListener(genericAsynReturn);
                }

                // 放入线程上下文
                RpcContext.getContext().setFuture(future);

                response = MessageBuilder.buildRpcResponse(msg);
                // 记录异步调用标记，如果是异步不清除threadlocal缓存，否则清除
                // @see com.jd.jsf.gd.filter.ConsumerContextFilter
                // RpcContext.getContext().setAttachment(BsoaConstants.CONFIG_KEY_ASYNC, true);
            }

            // 同步调用
            else {
                long start = BsoaContext.now();
                try {
                    // 记录活跃数
                    RpcStatus.beginCount(interfaceId, methodName, providerInfo);
                    response = (RpcResponse) transport.syncSend(msg, timeout);
                } finally {
                    long elapsed = BsoaContext.now() - start;
                    msg.addAttachment(BsoaConstants.INTERNAL_KEY_ELAPSED, (int) elapsed);
                    // 去掉活跃数
                    RpcStatus.endCount(interfaceId, methodName, providerInfo, elapsed,
                            response != null && !response.hasError());
                }
            }

            InetSocketAddress address = ClientTransportUtils.remoteAddress(transport);
            if (address != null) { // 添加调用的服务端远程地址
                RpcContext.getContext().setRemoteAddress(address);
            }

            return response;
        } catch (BsoaRpcException e) {
            if (e.getCode() == 11111) { // 连接断开异常 FIXME
                connectionHolder.setUnavailable(providerInfo, transport);
            }
            throw e;
        } catch (Exception e) {
            throw new BsoaRpcException(22222, "unknown exception");
        }
    }

    /**
     * 检查服务端版本，特殊处理
     *
     * @param providerInfo 服务端
     * @param request  请求对象
     */
    private void checkProviderVersion(ProviderInfo providerInfo, RpcRequest request) {
        int version = providerInfo.getBsoaVersion();
        if (version >= 1500) {
            // 服务端版本号供本地序列化使用
            RpcContext.getContext().setAttachment(BsoaConstants.HIDDEN_KEY_DST_JSF_VERSION, (short) version);
            // head增加客户端版本号供服务端用
            request.addHeadKey(HeadKey.BSOA_VERSION, (short) BsoaVersion.BSOA_VERSION);
            if (!CommonUtils.isTrue((Boolean) request.getAttachment(BsoaConstants.CONFIG_KEY_GENERIC))
                    && providerInfo.openInvocationOptimizing()) { // 是否开启invocation优化
//                request.setIfaceId(BsoaContext.getIfaceIdByClassName(consumerConfig.getInterfaceId()));
            } else {
//                request.set(null);
            }
        }
    }


    /**
     * 上一次连接，目前是记录整个接口的，是否需要方法级的？？
     */
    private volatile ProviderInfo lastProviderInfo;

    /**
     * 根据规则进行负载均衡
     *
     * @param message 调用对象
     * @return 一个可用的provider
     */
    protected ClientTransport select(RpcRequest message) {
        return select(message, null);
    }

    /**
     * 根据规则进行负载均衡
     *
     * @param message          调用对象
     * @param invokedProviderInfos 已调用列表
     * @return 一个可用的provider
     */
    protected ClientTransport select(RpcRequest message, List<ProviderInfo> invokedProviderInfos) {
        // 粘滞连接，当前连接可用
        if (consumerConfig.isSticky()) {
            if (lastProviderInfo != null) {
                ProviderInfo providerInfo = lastProviderInfo;
                ClientTransport lastTransport = connectionHolder.getAvailableClientTransport(providerInfo);
                if (lastTransport != null && lastTransport.isAvailable()) {
                    checkAlias(providerInfo, message);
                    return lastTransport;
                }
            }
        }
        // 原始服务列表数据
        List<ProviderInfo> providerInfos = connectionHolder.getAvailableProviders();
        // 先进行路由规则匹配， 根据invocation + consumer信息
        if (providerInfos.size() > 0 && CommonUtils.isNotEmpty(routers)) {
            for (Router router : routers) {
                providerInfos = router.route(message, providerInfos);
            }
        }
        if (CommonUtils.isNotEmpty(invokedProviderInfos) && providerInfos.size() > invokedProviderInfos.size()) { // 总数大于已调用数
            providerInfos.removeAll(invokedProviderInfos);// 已经调用异常的本次不再重试
        }
        if (providerInfos.size() == 0) {
            throw noAliveProviderException(consumerConfig.buildKey(), connectionHolder.currentProviderList());
        }
        do {
            // 再进行负载均衡筛选
            ProviderInfo providerInfo = loadBalancer.select(message, providerInfos);
            ClientTransport transport = selectByProvider(message, providerInfo);
            if (transport != null) {
                return transport;
            }
        } while (!connectionHolder.isAvailableEmpty());
        throw noAliveProviderException(consumerConfig.buildKey(), connectionHolder.currentProviderList());
    }

    private BsoaRpcException noAliveProviderException(String s, Collection<ProviderInfo> providerInfos) {
        return new BsoaRpcException(22222, "No Alive Provider");
    }

    /**
     * 得到provider得到连接
     *
     * @param message  调用对象
     * @param providerInfo 指定Provider
     * @return 一个可用的transport或者null
     */
    protected ClientTransport selectByProvider(RpcRequest message, ProviderInfo providerInfo) {
        ClientTransport transport = connectionHolder.getAvailableClientTransport(providerInfo);
        if (transport != null) {
            if (transport.isAvailable()) {
                lastProviderInfo = providerInfo;
                checkAlias(providerInfo, message); //检查分组
                return transport;
            } else {
                connectionHolder.setUnavailable(providerInfo, transport);
            }
        }
        return null;
    }

    /**
     * 检查分组映射
     *
     * @param providerInfo 服务端
     * @param message  请求对象
     */
    private void checkAlias(ProviderInfo providerInfo, RpcRequest message) {
        String pTags = providerInfo.getTags();
        // 如果配置的分组和服务端的分组不一致，说明存在分组映射
        if (pTags != null && !message.getTags().equals(pTags)) {
            // 分组映射，将调用里的分组改为服务端发布的
            message.setTags(pTags);
        }
        // 判断服务端codec兼容性，以服务端的为准
        String pSt = providerInfo.getSerializationType();
        if (pSt != null) {
            message.setSerializationType(SerializerFactory.getCodeByAlias(pSt));
        }
    }

    /**
     * 销毁方法
     */
    public void destroy() {
        if (destroyed) {
            return;
        }
        closeTransports();
        destroyed = true;
        // 关闭已有连接
        closeTransports();
        inited = false;
    }

    /**
     * 关闭连接<br/>
     * 注意：关闭有风险，可能有正在调用的请求，建议判断下isAvailable()
     */
    protected void closeTransports() {

        connectionHolder.preDestroy();

        // 准备关闭连接
        int count = countOfInvoke.get();
        final int timeout = consumerConfig.getDisconnectTimeout(); // 等待结果超时时间
        if (count > 0) { // 有正在调用的请求
            long start = BsoaContext.now();
            LOGGER.warn("There are {} outstanding call in client, will close transports util return", count);
            while (countOfInvoke.get() > 0 && BsoaContext.now() - start < timeout) { // 等待返回结果
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }
        }
        connectionHolder.destroy();
    }

    protected BsoaRpcException noAliveProvider(String key, String IP) {
        // TODO
        return new BsoaRpcException(22222, "No Alive Provider");
    }

    /**
     * 获取当前的Provider全部列表（包括连上和没连上的）
     *
     * @return 当前的Provider列表
     */
    public Collection<ProviderInfo> currentProviderList() {
        return connectionHolder.currentProviderList();
    }

    /**
     * 重建Routers
     */
    public void resetRouters() {
        this.routers = initRouterByRule();
    }

    /**
     * @return the consumerConfig
     */
    public ConsumerConfig<?> getConsumerConfig() {
        return consumerConfig;
    }
}
