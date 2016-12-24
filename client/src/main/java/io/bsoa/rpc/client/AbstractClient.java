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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.struct.NamedThreadFactory;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.config.RegistryConfig;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.context.RpcContext;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.listener.ResponseListener;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.registry.RegistryFactory;

/**
 * Created by zhanggeng on 16-7-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public abstract class AbstractClient implements Client {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractClient.class);

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
    public void init(ConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;

        // 负载均衡策略 考虑是否可动态替换？
        String lb = consumerConfig.getLoadbalance();
        loadBalancer = LoadBalancerFactory.getLoadBalancer(lb);
        loadBalancer.init(consumerConfig);

        routers = consumerConfig.getRouter();
        if (routers == null) { // 未手动配置，取注册中心的配置
            routers = initRouterByRule();
        }
        // 连接管理器
        connectionHolder = new ConnectionHolder(consumerConfig);
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
        String interfaceId = consumerConfig.getInterfaceId();
        boolean open = CommonUtils.isTrue(BsoaContext.getInterfaceVal(interfaceId, BsoaConstants.SETTING_ROUTER_OPEN, "true"));
        if (open) {
            String routerRule = BsoaContext.getInterfaceVal(interfaceId, BsoaConstants.SETTING_ROUTER_RULE, null);
            if (StringUtils.isNotBlank(routerRule)) {
                routers = RouterFactory.buildRouters(routerRule);
            }
        }
        return routers;
    }

    /**
     * 和服务端建立连接
     */
    private void initConnections() {
        if (destroyed) { // 已销毁
            throw new BsoaRuntimeException(22001, "Client has been destroyed!");
        }
        if (inited) { // 已初始化
            return;
        }
        synchronized (this) {
            if (inited) {
                return;
            }
            try {
                // 得到服务端列表
                List<Provider> tmpProviderList = buildProviderList();
                connectToProviders(tmpProviderList); // 初始化服务端连接（建立长连接)
            } catch (InitErrorException e) {
                throw e;
            } catch (Exception e) {
                throw new InitErrorException("[JSF-22002]Init provider's transport error!", e);
            }

            // 如果check=true表示强依赖
            if (consumerConfig.isCheck() && connectionHolder.isAliveEmpty()) {
                throw new InitErrorException("[JSF-22003]The consumer is depend on alive provider " +
                        "and there is no alive provider, you can ignore it " +
                        "by <jsf:consumer check=\"false\"> (default is false)");
            }

            // 启动重连线程
            connectionHolder.startReconnectThread();
            // 启动成功
            inited = true;
        }
    }

    /**
     * 从直连地址或者注册中心得到服务端列表
     *
     * @return 服务端列表 provider list
     */
    protected List<Provider> buildProviderList() {
        List<Provider> tmpProviderList;
        String url = consumerConfig.getUrl();
        if (StringUtils.isNotEmpty(url)) { // 如果走直连
            tmpProviderList = new ArrayList<Provider>();
            String interfaceId = consumerConfig.getInterfaceId();
            String alias = consumerConfig.getAlias();
            BsoaConstants.ProtocolType pt = BsoaConstants.ProtocolType.valueOf(consumerConfig.getProtocol());
            String[] providerStrs = StringUtils.splitWithCommaOrSemicolon(url);
            for (int i = 0; i < providerStrs.length; i++) {
                Provider provider = Provider.valueOf(providerStrs[i]);
                if(!CommonUtils.isFalse(consumerConfig.getParameter(BsoaConstants.HIDDEN_KEY_WARNNING))) {
                    if (provider.getProtocolType() != pt) {
                        throw new IllegalConfigureException(21308, "consumer.url", url,
                                "there is a mismatch protocol between url[" + provider.getProtocolType()
                                        + "] and consumer[" + pt + "]");
                    }
                    String pitf = provider.getInterfaceId();
                    if (pitf != null) {
                        if (!pitf.equals(interfaceId)) {
                            throw new IllegalConfigureException(21308, "consumer.url", url,
                                    "there is a mismatch interfaceId between url[" + pitf
                                            + "] and consumer[" + interfaceId + "]");
                        }
                    } else {
                        provider.setInterfaceId(interfaceId);
                    }
                    String palias = provider.getAlias();
                    if (palias != null) {
                        if (!palias.equals(alias)) {
                            try {
                                throw new IllegalConfigureException(21308, "consumer.url", url,
                                        "there is a mismatch alias between url[" + palias
                                                + "] and consumer[" + alias + "]");
                            } catch (Exception e) {
                                LOGGER.warn(e.getMessage());
                            }
                        }
                    } else {
                        provider.setAlias(alias);
                    }
                }
                tmpProviderList.add(provider);
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
            tmpProviderList = consumerConfig.subscribe();
        }
        return tmpProviderList;
    }

    /**
     * 增加Provider
     *
     * @param providers
     *         Provider列表
     */
    public void addProvider(List<Provider> providers) {
        String interfaceId = consumerConfig.getInterfaceId();
        LOGGER.info("{} add {} providers to list", interfaceId, providers.size());
        for (Provider provider : providers) {
            try {
                if (connectionHolder.getRetryConnections().containsKey(provider)) {
                    // 失败里有，立即重试
                    ClientTransportConfig config = providerToClientConfig(provider);
                    ClientTransport transport = ClientTransportFactory.getClientTransport(config);
                    if (connectionHolder.doubleCheck(interfaceId, provider, transport)) {
                        printSuccess(interfaceId, provider, transport);
                        connectionHolder.retryToAlive(provider, transport);
                    }
                } else if (connectionHolder.getDeadConnections().containsKey(provider)) {
                    // 死亡里有，立即重试
                    ClientTransportConfig config = providerToClientConfig(provider);
                    ClientTransport transport = ClientTransportFactory.getClientTransport(config);
                    if (connectionHolder.doubleCheck(interfaceId, provider, transport)) {
                        printSuccess(interfaceId, provider, transport);
                        connectionHolder.deadToAlive(provider, transport);
                    }
                } else { // 失败/死亡里没有
                    // 存活里也没有
                    if (!connectionHolder.getAliveConnections().containsKey(provider)) {
                        ClientTransportConfig config = providerToClientConfig(provider);
                        try {
                            ClientTransport transport = ClientTransportFactory.getClientTransport(config);
                            if (connectionHolder.doubleCheck(interfaceId, provider, transport)) {
                                printSuccess(interfaceId, provider, transport);
                                connectionHolder.addAlive(provider, transport);
                            } else {
                                printFailure(interfaceId, provider, transport);
                                connectionHolder.addRetry(provider, transport);
                            }
                        } catch (Exception e) {
                            printDead(interfaceId, provider, e);
                            connectionHolder.addDead(provider, config);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("add " + consumerConfig.getInterfaceId() + " provider " + provider
                        + " to list error:", e);
            }
        }
    }

    /**
     * 删除Provider
     *
     * @param providers
     *         Provider列表
     */
    public void removeProvider(List<Provider> providers) {
        String interfaceId = consumerConfig.getInterfaceId();
        LOGGER.info("{} remove {} providers from list", interfaceId, providers.size());
        for (Provider provider : providers) {
            try {
                // 从存活和重试列表里都删除
                //  判断一个删成功 就不走下一个
                ClientTransport transport = connectionHolder.remove(provider);
                LOGGER.info("Remove {} provider:{} from list success !", interfaceId, provider);
                if (transport != null) {
                    ClientTransportFactory.releaseTransport(transport, consumerConfig.getDisconnectTimeout());
                }
            } catch (Exception e) {
                LOGGER.error("remove " + consumerConfig.getInterfaceId() + " provider " + provider
                        + " from list error:", e);
            }
        }
    }

    /**
     * 更新Provider
     *
     * @param newProviders
     *         Provider列表
     */
    public void updateProvider(List<Provider> newProviders) {
        try {
            if (CommonUtils.isEmpty(newProviders)) {
                if(CommonUtils.isNotEmpty(connectionHolder.currentProviderList())) {
                    LOGGER.info("Clear all providers, may be this consumer has been add to blacklist");
                    closeTransports();
                }
            } else {
                Set<Provider> nowall = currentProviderList();
                List<Provider> nowAllP = new ArrayList<Provider>(nowall);// 当前全部

                // 比较当前的和最新的
                ListDifference<Provider> diff = new ListDifference<Provider>(newProviders, nowAllP);
                List<Provider> needAdd = diff.getOnlyOnLeft(); // 需要新建
                List<Provider> needDelete = diff.getOnlyOnRight(); // 需要删掉
                if (!needAdd.isEmpty()) {
                    addProvider(needAdd);
                }
                if (!needDelete.isEmpty()) {
                    removeProvider(needDelete);
                }
            }
        } catch (Exception e) {
            LOGGER.error("update " + consumerConfig.getInterfaceId() + " provider (" + newProviders.size()
                    + ") from list error:", e);
        }
    }

    /**
     * Provider对象得到 ClientTransportConfig
     *
     * @param provider
     *         Provider
     * @return ClientTransportConfig
     */
    private ClientTransportConfig providerToClientConfig(Provider provider) {
        ClientTransportConfig config = new ClientTransportConfig(provider, consumerConfig.getConnectTimeout());
        config.setInvokeTimeout(consumerConfig.getTimeout());
        config.setPayload(consumerConfig.getPayload());
        config.setClientBusinessPoolType(consumerConfig.getThreadpool());
        config.setClientBusinessPoolSize(consumerConfig.getThreads());
        config.setChildNioEventThreads(consumerConfig.getIothreads());
        config.setConnectListeners(consumerConfig.getOnconnect());
        return config;
    }

    /**
     * 连接服务端，建立Connection
     *
     * @param providerList
     *         服务端列表
     */
    protected void connectToProviders(List<Provider> providerList) {
        final String interfaceId = consumerConfig.getInterfaceId();
        int providerSize = providerList.size();
        LOGGER.info("Init provider of {}, size is : {}", interfaceId, providerSize);
        if (providerSize > 0) {
            // 多线程建立连接
            int threads = Math.min(10, providerSize); // 最大10个
            final CountDownLatch latch = new CountDownLatch(providerSize);
            ThreadPoolExecutor initPool = new ThreadPoolExecutor(threads, threads,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(providerList.size()),
                    new NamedThreadFactory("JSF-CLI-CONN-" + interfaceId, true));
            int connectTimeout = consumerConfig.getConnectTimeout();
            for (final Provider provider : providerList) {
                final ClientTransportConfig config = providerToClientConfig(provider);
                config.setConnectionTimeout(connectTimeout);
                initPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ClientTransport transport = ClientTransportFactory.getClientTransport(config);
                            if (connectionHolder.doubleCheck(interfaceId, provider, transport)) {
                                printSuccess(interfaceId, provider, transport);
                                connectionHolder.addAlive(provider, transport);
                            } else {
                                printFailure(interfaceId, provider, transport);
                                connectionHolder.addRetry(provider, transport);
                            }
                        } catch (Exception e) {
                            printDead(interfaceId, provider, e);
                            connectionHolder.addDead(provider, config);
                        } finally {
                            latch.countDown(); // 连上或者抛异常
                        }
                    }
                });
            }

            try {
                int totalTimeout = ((providerSize % threads == 0) ? (providerSize / threads) : ((providerSize /
                        threads) + 1)) * connectTimeout + 500;
                latch.await(totalTimeout, TimeUnit.MILLISECONDS); // 一直等到子线程都结束
            } catch (InterruptedException e) {
                LOGGER.error("Exception when init provider", e);
            } finally {
                initPool.shutdown(); // 关闭线程池
            }
        }
    }

    /**
     * 打印连接成功日志
     *
     * @param interfaceId
     *         接口名称
     * @param provider
     *         服务端
     * @param transport
     *         连接
     */
    private void printSuccess(String interfaceId, Provider provider, ClientTransport transport) {
        LOGGER.info("Connect to {} provider:{} success ! The connection is " +
                        NetUtils.connectToString(transport.getRemoteAddress(), transport.getLocalAddress()),
                interfaceId, provider);
    }

    /**
     * 打印连接失败日志
     *
     * @param interfaceId
     *         接口名称
     * @param provider
     *         服务端
     * @param transport
     *         连接
     */
    private void printFailure(String interfaceId, Provider provider, ClientTransport transport) {
        LOGGER.info("Connect to {} provider:{} failure !", interfaceId, provider);
    }

    /**
     * 打印连不上日志
     *
     * @param interfaceId
     *         接口名称
     * @param provider
     *         服务端
     */
    private void printDead(String interfaceId, Provider provider, Exception e) {
        Throwable cause = e.getCause();
        LOGGER.warn("Connect to {} provider:{} failure !! The exception is " + ExceptionUtils.toShortString(e, 1)
                        + (cause != null ? ", cause by " + cause.getMessage() + "." : "."),
                interfaceId, provider);
    }

    /**
     * 是否可用（即有可用的服务端）
     *
     * @return 是/否
     */
    public boolean isAvailable() {
        if (destroyed || !inited)
            return false;
        if (connectionHolder.isAliveEmpty())
            return false;
        for (Map.Entry<Provider, ClientTransport> entry : connectionHolder.getAliveConnections().entrySet()) {
            Provider provider = entry.getKey();
            ClientTransport transport = entry.getValue();
            if (transport.isOpen()) {
                return true;
            } else {
                connectionHolder.aliveToRetryIfExist(provider, transport);
            }
        }
        return false;
    }

    /**
     * 检查状态是否变化，变化则通知监听器
     *
     * @param originalState
     *         原始状态
     */
    public void checkStateChange(boolean originalState) {
        if (originalState) { // 原来可以
            if (!isAvailable()) { // 变不可以
                connectionHolder.notifyStateChangeToUnavailable();
            }
        } else { // 原来不可用
            if (isAvailable()) { // 变成可用
                connectionHolder.notifyStateChangeToAvailable();
            }
        }
    }

    /**
     * 调用
     *
     * @param msg
     *         Request对象
     * @return 调用结果
     */
    public ResponseMessage sendMsg(Request msg) {
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
     * @param msg
     *         Request对象
     * @return 调用结果
     */
    protected abstract ResponseMessage doSendMsg(Request msg);

    /**
     * 调用客户端
     *
     * @param connection
     *         客户端连接
     * @param msg
     *         Request对象
     * @return 调用结果
     */
    protected ResponseMessage sendMsg0(Connection connection, Request msg) {
        Provider provider = connection.getProvider();
        ClientTransport transport = connection.getTransport();
        try {
            Invocation invocation = msg.getInvocationBody();
            checkProviderVersion(provider, msg); // 根据服务端版本特殊处理
            String interfaceId = invocation.getClazzName();
            String methodName = invocation.getMethodName();
            boolean async = consumerConfig.getMethodAsync(methodName);
            int timeout = consumerConfig.getMethodTimeout(methodName);
            Boolean genericAsync = (Boolean) invocation.getAttachment(BsoaConstants.INTERNAL_KEY_ASYNC);
            ResponseMessage response = null;

            // 异步调用
            if (async || CommonUtils.isTrue(genericAsync)) {
                // 回调监听器
                AsyncResultListener resultListener = null;
                // 接口或者方法级
                final List<ResponseListener> onreturns = consumerConfig.getMethodOnreturn(methodName);
                if (onreturns != null) {
                    resultListener = new AsyncResultListener();
                    resultListener.addResponseListeners(onreturns);
                }
                //方法级的回调listener since 1.6.1
                Object methodResponseListenerObj = RpcContext.getContext().getAttachment(BsoaConstants.INTERNAL_KEY_ONRETURN);
                if(methodResponseListenerObj instanceof ResponseListener){
                    ResponseListener methodResponseListener = (ResponseListener) methodResponseListenerObj;
                    if (resultListener == null){
                        resultListener = new AsyncResultListener();
                        resultListener.addResponseListener(methodResponseListener);
                    }
                } else if (methodResponseListenerObj != null){
                    LOGGER.warn("{},method response listener is not instance of ResponseListener",
                            methodResponseListenerObj.getClass().getCanonicalName());
                }
                // 调用级
                final ResponseListener genericAsynReturn = (ResponseListener)
                        invocation.getAttachment(BsoaConstants.INTERNAL_KEY_ONRETURN);
                if (genericAsynReturn != null) {
                    invocation.addAttachment(BsoaConstants.INTERNAL_KEY_ONRETURN, null);
                    if (resultListener == null) {
                        resultListener = new AsyncResultListener();
                    }
                    resultListener.addResponseListener(genericAsynReturn);
                }
                MsgFuture future = transport.sendAsyn(msg, timeout); // 开始调用
                if (resultListener != null) {
                    // 有listener则监听
                    future.addListener(resultListener);
                } else {
                    // 放入线程上下文
                    RpcContext.getContext().setFuture(new ResponseFuture(future));
                }
                response = MessageBuilder.buildResponse(msg);
                // 记录异步调用标记，如果是异步不清除threadlocal缓存，否则清除
                // @see com.jd.jsf.gd.filter.ConsumerContextFilter
                // RpcContext.getContext().setAttachment(BsoaConstants.CONFIG_KEY_ASYNC, true);
            }

            // 同步调用
            else {
                long start = BsoaContext.systemClock.now();
                try {
                    // 记录活跃数
                    RpcStatus.beginCount(interfaceId, methodName, provider);
                    response = transport.send(msg, timeout);
                } finally {
                    long elapsed = BsoaContext.systemClock.now() - start;
                    invocation.addAttachment(BsoaConstants.INTERNAL_KEY_ELAPSED, (int) elapsed);
                    // 去掉活跃数
                    RpcStatus.endCount(interfaceId, methodName, provider, elapsed,
                            response == null ? false : !response.isError());
                }
            }

            InetSocketAddress address = transport.getRemoteAddress();
            if (address != null) { // 添加调用的服务端远程地址
                RpcContext.getContext().setRemoteAddress(address);
            }

            return response;
        } catch (ClientClosedException e) { // 连接断开异常
            connectionHolder.aliveToRetryIfExist(provider, transport);
            throw e;
            //} catch (InvokerNotExportedException e) { // 在response里
        }
    }

    /**
     * 检查服务端版本，特殊处理
     *
     * @param provider
     *         服务端
     * @param request
     *         请求对象
     */
    private void checkProviderVersion(Provider provider, Request request) {
        int version = provider.getJsfVersion();
        if (version >= 1500) {
            // 服务端版本号供本地序列化使用
            RpcContext.getContext().setAttachment(BsoaConstants.HIDDEN_KEY_DST_JSF_VERSION, (short) version);
            // head增加客户端版本号供服务端用
            request.getMsgHeader().addHeadKey(BsoaConstants.HeadKey.jsfVersion, (short) BsoaConstants.JSF_VERSION);
            Invocation invocation = request.getInvocationBody();
            if (!CommonUtils.isTrue((Boolean) invocation.getAttachment(BsoaConstants.CONFIG_KEY_GENERIC))
                    && provider.openInvocationOptimizing()) { // 是否开启invocation优化
                invocation.setIfaceId(BsoaContext.getIfaceIdByClassName(consumerConfig.getInterfaceId()));
            } else {
                invocation.setIfaceId(null);
            }
        }
    }


    /**
     * 上一次连接，目前是记录整个接口的，是否需要方法级的？？
     */
    private volatile Provider lastProvider;

    /**
     * 根据规则进行负载均衡
     *
     * @param message
     *         调用对象
     * @return 一个可用的provider
     * @throws NoAliveProviderException
     *         没有可用的服务端
     */
    protected Connection select(Request message) throws NoAliveProviderException {
        return select(message, null);
    }

    /**
     * 根据规则进行负载均衡
     *
     * @param message
     *         调用对象
     * @param invokedProviders
     *         已调用列表
     * @return 一个可用的provider
     * @throws NoAliveProviderException
     *         没有可用的服务端
     */
    protected Connection select(Request message, List<Provider> invokedProviders) throws NoAliveProviderException {
        // 粘滞连接，当前连接可用
        if (consumerConfig.isSticky()) {
            if (lastProvider != null) {
                Provider provider = lastProvider;
                ClientTransport lastTransport = connectionHolder.getAliveClientTransport(provider);
                if (lastTransport != null && lastTransport.isOpen()) {
                    checkAlias(provider, message);
                    return new Connection(provider, lastTransport);
                }
            }
        }
        Invocation invocation = message.getInvocationBody();
        // 原始服务列表数据
        List<Provider> providers = connectionHolder.getAliveProviders();
        // 先进行路由规则匹配， 根据invocation + consumer信息
        if (providers.size() > 0 && CommonUtils.isNotEmpty(routers)) {
            for (Router router : routers) {
                providers = router.route(invocation, providers);
            }
        }
        if (invokedProviders != null && providers.size() > invokedProviders.size()) { // 总数大于已调用数
            providers.removeAll(invokedProviders);// 已经调用异常的本次不再重试
        }
        if (providers.size() == 0) {
            throw new NoAliveProviderException(consumerConfig.buildKey(), connectionHolder.currentProviderList());
        }
        do {
            // 再进行负载均衡筛选
            Provider provider = loadBalancer.select(invocation, providers);
            ClientTransport transport = selectByProvider(message, provider);
            if (transport != null) {
                return new Connection(provider, transport);
            }
        } while (!connectionHolder.isAliveEmpty());
        throw new NoAliveProviderException(consumerConfig.buildKey(), connectionHolder.currentProviderList());
    }

    /**
     * 得到provider得到连接
     *
     * @param message
     *         调用对象
     * @param provider
     *         指定Provider
     * @return 一个可用的transport或者null
     */
    protected ClientTransport selectByProvider(Request message, Provider provider) {
        ClientTransport transport = connectionHolder.getAliveClientTransport(provider);
        if (transport != null) {
            if (transport.isOpen()) {
                lastProvider = provider;
                checkAlias(provider, message); //检查分组
                return transport;
            } else {
                connectionHolder.aliveToRetryIfExist(provider, transport);
            }
        }
        return null;
    }

    /**
     * 检查分组映射
     *
     * @param provider
     *         服务端
     * @param message
     *         请求对象
     */
    private void checkAlias(Provider provider, Request message) {
        Invocation invocation = message.getInvocationBody();
        String pAlias = provider.getAlias();
        // 如果配置的分组和服务端的分组不一致，说明存在分组映射
        if (pAlias != null && !invocation.getAlias().equals(pAlias)) {
            // 分组映射，将调用里的分组改为服务端发布的
            invocation.setAlias(pAlias);
        }
        // 判断服务端codec兼容性，以服务端的为准
        CodecType ct = provider.getCodecType();
        if (ct != null) {
            message.getMsgHeader().setCodecType(ct.value());
        }
    }

    /**
     * 销毁方法
     */
    public void destroy() {
        if (destroyed) {
            return;
        }
        // 销毁重连client线程
        connectionHolder.shutdownReconnectThread();
        destroyed = true;
        // 关闭已有连接
        closeTransports();
        inited = false;
    }

    /**
     * 关闭连接<br/>
     * 注意：关闭有风险，可能有正在调用的请求，建议判断下isAlivable()
     */
    protected void closeTransports() {
        // 清空列表先
        HashMap<Provider, ClientTransport> all = connectionHolder.clear();

        // 准备关闭连接
        int count = countOfInvoke.get();
        final int timeout = consumerConfig.getDisconnectTimeout(); // 等待结果超时时间
        if (count > 0) { // 有正在调用的请求
            long start = BsoaContext.systemClock.now();
            LOGGER.warn("There are {} outstanding call in client, will close transports util return", count);
            while (countOfInvoke.get() > 0 && BsoaContext.systemClock.now() - start < timeout) { // 等待返回结果
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }
        }
        // 多线程销毁已经建立的连接
        int providerSize = all.size();
        if (providerSize > 0) {
            int threads = Math.min(10, providerSize); // 最大10个
            final CountDownLatch latch = new CountDownLatch(providerSize);
            ThreadPoolExecutor closepool = new ThreadPoolExecutor(threads, threads,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(providerSize),
                    new NamedThreadFactory("JSF-CLI-DISCONN-" + consumerConfig.getInterfaceId(), true));
            for (Map.Entry<Provider, ClientTransport> entry : all.entrySet()) {
                final Provider provider = entry.getKey();
                final ClientTransport transport = entry.getValue();
                closepool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ClientTransportFactory.releaseTransport(transport, 0);
                        } catch (Exception e) {
                            LOGGER.warn("catch exception but ignore it when close alive client : {}", provider);
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }
            try {
                int totalTimeout = ((providerSize % threads == 0) ? (providerSize / threads) : ((providerSize /
                        threads) + 1)) * timeout + 500;
                latch.await(totalTimeout, TimeUnit.MILLISECONDS); // 一直等到
            } catch (InterruptedException e) {
                LOGGER.error("Exception when close transport", e);
            } finally {
                closepool.shutdown();
            }
        }
    }

    /**
     * 获取当前的Provider全部列表（包括连上和没连上的）
     *
     * @return 当前的Provider列表
     */
    public Set<Provider> currentProviderList() {
        return connectionHolder.currentProviderList();
    }

    /**
     * 获取当前的Provider列表（每种状态已分开）
     *
     * @return 当前的Provider列表
     */
    public Map<String, Set<Provider>> currentProviderMap() {
        return connectionHolder.currentProviderMap();
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
