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
package io.bsoa.rpc.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.GenericService;
import io.bsoa.rpc.base.Invoker;
import io.bsoa.rpc.client.Client;
import io.bsoa.rpc.client.ClientFactory;
import io.bsoa.rpc.client.ClientProxyInvoker;
import io.bsoa.rpc.client.Provider;
import io.bsoa.rpc.client.Router;
import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.ClassLoaderUtils;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.listener.ChannelListener;
import io.bsoa.rpc.listener.ConfigListener;
import io.bsoa.rpc.listener.ConsumerStateListener;
import io.bsoa.rpc.listener.ProviderListener;
import io.bsoa.rpc.listener.ResponseListener;
import io.bsoa.rpc.proxy.ProxyFactory;
import io.bsoa.rpc.registry.Registry;
import io.bsoa.rpc.registry.RegistryFactory;

/**
 * Created by zhanggeng on 16-7-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class ConsumerConfig<T> extends AbstractInterfaceConfig implements Serializable {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ConsumerConfig.class);

    /**
     * The constant serialVersionUID.
     */
    private static final long serialVersionUID = 4244077707655448146L;

    /**
     * 调用的协议
     */
    protected String protocol = BsoaConstants.DEFAULT_PROTOCOL;

    /**
     * 直连调用地址
     */
    protected String url;

    /**
     * 是否泛化调用
     */
    protected boolean generic = false;

    /**
     * 是否异步调用
     */
    protected boolean async = false;

    /**
     * 连接超时时间
     */
    protected int connectTimeout = BsoaConstants.DEFAULT_CLIENT_CONNECT_TIMEOUT;

    /**
     * 关闭超时时间（如果还有请求，会等待请求结束或者超时）
     */
    protected int disconnectTimeout = BsoaConstants.DEFAULT_CLIENT_DISCONNECT_TIMEOUT;

    /**
     * 集群处理，默认是failover
     */
    protected String cluster = BsoaConstants.CLUSTER_FAILOVER;

    /**
     * The Retries. 失败后重试次数
     */
    protected int retries = BsoaConstants.DEFAULT_RETRIES_TIME;

    /**
     * The Loadbalance. 负载均衡
     */
    protected String loadbalance = BsoaConstants.LOADBALANCE_RANDOM;

    /**
     * 是否延迟建立长连接,
     * connect transport when invoke, but not when init
     */
    protected boolean lazy = false;

    /**
     * 粘滞连接，一个断开才选下一个
     * change transport when current is disconnected
     */
    protected boolean sticky = false;

    /**
     * 是否jvm内部调用（provider和consumer配置在同一个jvm内，则走本地jvm内部，不走远程）
     */
    protected boolean injvm = true;

    /**
     * 是否强依赖（即没有服务节点就启动失败）
     */
    protected boolean check = false;

    /**
     * 默认序列化
     */
    protected String serialization = BsoaConstants.DEFAULT_SERIALIZATION;

    /**
     * 返回值之前的listener,处理结果或者异常
     */
    protected transient List<ResponseListener> onreturn;

    /**
     * 连接事件监听器实例，连接或者断开时触发
     */
    protected transient List<ChannelListener> onconnect;

    /**
     * 客户端状态变化监听器实例，状态可用和不可以时触发
     */
    protected transient List<ConsumerStateListener> onavailable;

    /**
     * 线程池类型
     */
    protected String threadpool = BsoaConstants.THREADPOOL_TYPE_CACHED;

    /**
     * 业务线程池大小
     */
    protected int threads = BsoaConstants.DEFAULT_CLIENT_BIZ_THREADS;

    /**
     * io线程池大小
     */
    protected int iothreads;

    /**
     * Consumer给Provider发心跳的间隔
     */
    protected int heartbeat = BsoaConstants.DEFAULT_HEARTBEAT_TIME;

    /**
     * Consumer给Provider重连的间隔
     */
    protected int reconnect = BsoaConstants.DEFAULT_RECONNECT_TIME;

    /**
     * 最大数据包大小
     */
    protected  int payload = BsoaConstants.DEFAULT_PAYLOAD;

    /**
     * 路由规则引用，多个用英文逗号隔开。List<Router>
     */
    protected transient List<Router> router;

    /*-------- 下面是方法级配置 --------*/

    /**
     * 接口下每方法的最大可并行执行请求数，配置-1关闭并发过滤器，等于0表示开启过滤但是不限制
     */
    protected int concurrents = 0;

	/*---------- 参数配置项结束 ------------*/

    /**
     * 代理实现类
     */
    protected transient volatile T proxyIns;

    /**
     * 代理的Invoker对象
     */
    protected transient volatile Invoker proxyInvoker;

    /**
     * 调用类
     */
    protected transient volatile Client client;

    /**
     * 服务配置的listener
     */
    protected transient volatile ProviderListener providerListener;

    /**
     * 发布的调用者配置（含计数器）
     */
    protected final static ConcurrentHashMap<String, AtomicInteger> REFERRED_KEYS
            = new ConcurrentHashMap<String, AtomicInteger>();

    /**
     * Refer t.
     *
     * @return the t
     * @throws BsoaRuntimeException the init error exception
     */
    public synchronized T refer() throws BsoaRuntimeException {
        if (proxyIns != null) {
            return proxyIns;
        }
        String key = buildKey();
        // 检查参数
        // alias不能为空
        if (StringUtils.isBlank(tags)) {
            throw new BsoaRuntimeException(21300, "[JSF-21300]Value of \"GROUP\" value is not specified in consumer" +
                    " config with key " + key + " !");
        }
        // 提前检查接口类
        getProxyClass();

        LOGGER.info("Refer consumer config : {} with bean id {}", key, getId());

        // 注意同一interface，同一alias，同一protocol情况
        AtomicInteger cnt = REFERRED_KEYS.get(key); // 计数器
        if (cnt == null) { // 没有发布过
            cnt = CommonUtils.putToConcurrentMap(REFERRED_KEYS, key, new AtomicInteger(0));
        }
        int c = cnt.incrementAndGet();
        if (c > 3) {
            if(!CommonUtils.isFalse(getParameter(BsoaConstants.HIDDEN_KEY_WARNNING))){
                throw new BsoaRuntimeException(21304, "[JSF-21304]Duplicate consumer config with key " + key
                        + " has been referred more than 3 times!"
                        + " Maybe it's wrong config, please check it."
                        + " Ignore this if you did that on purpose!");
            } else {
                LOGGER.warn("[JSF-21304]Duplicate consumer config with key {} "
                        + "has been referred more than 3 times!"
                        + " Maybe it's wrong config, please check it."
                        + " Ignore this if you did that on purpose!", key);
            }
        } else if (c > 1) {
            LOGGER.warn("[JSF-21303]Duplicate consumer config with key {} has been referred!"
                    + " Maybe it's wrong config, please check it."
                    + " Ignore this if you did that on purpose!", key);
        }

        // 检查是否有回调函数
//        CallbackUtil.autoRegisterCallBack(getProxyClass());
        // 注册接口类反序列化模板
//        if(!isGeneric()){
//            try {
//                CodecUtils.registryService(serialization, getProxyClass());
//            } catch (BsoaRuntimeException e) {
//                throw e;
//            } catch (Exception e) {
//                throw new BsoaRuntimeException("[JSF-21305]Registry codec template error!", e);
//            }
//        }
        // 如果本地发布了服务，则优选走本地代理，没有则走远程代理
//        if (isInjvm() && BaseServerHandler.getInvoker(getInterfaceId(), getTags()) != null) {
//            LOGGER.info("Find matched provider invoker in current jvm, " +
//                    "will invoke preferentially until it unexported");
//        }

        configListener = new ConsumerAttributeListener();
        providerListener = new ClientProviderListener();
        try {
            // 生成客户端
            client = ClientFactory.getClient(this);
            // 构造Invoker对象（执行链）
            proxyInvoker = new ClientProxyInvoker(this);
            // 提前检查协议+序列化方式 TODO
            //ProtocolFactory.check(ProtocolType.valueOf(getProtocol()), SerializationType.valueOf(getSerialization()));
            // 创建代理类
            proxyIns = (T) ProxyFactory.buildProxy(getProxy(), getProxyClass(), proxyInvoker);
        } catch (Exception e) {
            if (client != null) {
                client.destroy();
                client = null;
            }
            cnt.decrementAndGet(); // 发布失败不计数
            if (e instanceof BsoaRuntimeException) {
                throw (BsoaRuntimeException) e;
            } else {
                throw new BsoaRuntimeException(22222, "[JSF-21306]Build consumer proxy error!", e);
            }
        }
        if (onavailable != null && client != null) {
            client.checkStateChange(false); // 状态变化通知监听器
        }
        BsoaContext.cacheConsumerConfig(this);
        return proxyIns;
    }

    /**
     * Unrefer void.
     */
    public synchronized void unrefer() {
        if (proxyIns == null) {
            return;
        }
        String key = buildKey();
        LOGGER.info("Unrefer consumer config : {} with bean id {}", key, getId());
        try {
            client.destroy();
        } catch (Exception e) {
            LOGGER.warn("Catch exception when unrefer consumer config : " + key
                    + ", but you can ignore if it's called by JVM shutdown hook", e);
        }
        // 清除一些缓存
        AtomicInteger cnt = REFERRED_KEYS.get(key);
        if (cnt != null && cnt.decrementAndGet() <= 0) {
            REFERRED_KEYS.remove(key);
        }
        configListener = null;
        providerListener = null;
        BsoaContext.invalidateConsumerConfig(this);
//        RpcStatus.removeStatus(this); TODO
        proxyIns = null;

        // 取消订阅到注册中心
        unsubscribe();
    }

    /**
     * 订阅服务列表
     *
     * @return 当前服务列表 list
     */
    public List<Provider> subscribe() {
        List<Provider> tmpProviderList = new ArrayList<Provider>();
        for (Provider provider : tmpProviderList) {

        }
        List<RegistryConfig> registryConfigs = getRegistry();
        // 从注册中心订阅
        for (RegistryConfig registryConfig : registryConfigs) {
            Registry registry = RegistryFactory
                    .getRegistry(registryConfig);
            try {
                List<Provider> providers = registry.subscribe(this, providerListener, configListener);
                if (CommonUtils.isNotEmpty(providers)) {
                    tmpProviderList.addAll(providers);
                }
            } catch (BsoaRuntimeException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.warn("Catch exception when subscribe from registry: " + registryConfig.getId()
                        + ", but you can ignore if it's called by JVM shutdown hook", e);
            }
        }
        return tmpProviderList;
    }

    /**
     * 取消订阅服务列表
     */
    public void unsubscribe() {
        if (StringUtils.isEmpty(url) && isSubscribe()) {
            List<RegistryConfig> registryConfigs = super.getRegistry();
            if (registryConfigs != null) {
                for (RegistryConfig registryConfig : registryConfigs) {
                    Registry registry = RegistryFactory.getRegistry(registryConfig);
                    try {
                        registry.unsubscribe(this);
                    } catch (Exception e) {
                        LOGGER.warn("Catch exception when unsubscribe from registry: " + registryConfig.getId()
                                + ", but you can ignore if it's called by JVM shutdown hook", e);
                    }
                }
            }
        }
    }

    /**
     * 客户端节点变化监听器
     */
    private class ClientProviderListener implements ProviderListener {

        @Override
        public void addProvider(List<Provider> providers) {
            if (client != null) {
                boolean originalState = client.isAvailable();
                client.addProvider(providers);
                client.checkStateChange(originalState);
            }
        }

        @Override
        public void removeProvider(List<Provider> providers) {
            if (client != null) {
                boolean originalState = client.isAvailable();
                client.removeProvider(providers);
                client.checkStateChange(originalState);
            }
        }

        @Override
        public void updateProvider(List<Provider> newProviders) {
            if (client != null) {
                boolean originalState = client.isAvailable();
                client.updateProvider(newProviders);
                client.checkStateChange(originalState);
            }
        }
    }

    /**
     * Consumer配置发生变化监听器
     */
    private class ConsumerAttributeListener implements ConfigListener {

        @Override
        public void configChanged(Map newValue) {
            if (client != null) {
                if (newValue.containsKey(BsoaConstants.SETTING_ROUTER_OPEN) ||
                        newValue.containsKey(BsoaConstants.SETTING_ROUTER_RULE)) {
                    // 是否比较变化？ TODO
                    //client.resetRouters();
                }
            }
        }

        @Override
        public synchronized void attrUpdated(Map newValueMap) {
            // 重要： proxyIns不能换，只能换client。。。。
            // 修改调用的alias cluster(loadblance) timeout, retries？
            Map<String, String> newValues = (Map<String, String>) newValueMap;
            Map<String, String> oldValues = new HashMap<String, String>();
            boolean rerefer = false;
            try { // 检查是否有变化
                // 是否过滤map?
                for (Map.Entry<String, String> entry : newValues.entrySet()) {
                    String newValue = entry.getValue();
                    String oldValue = queryAttribute(entry.getKey());
                    boolean changed = oldValue == null ? newValue != null : !oldValue.equals(newValue);
                    if (changed) { // 记住旧的值
                        oldValues.put(entry.getKey(), oldValue);
                    }
                    rerefer = rerefer || changed;
                }
            } catch (Exception e) {
                LOGGER.error("Catch exception when consumer attribute comparing", e);
                return;
            }
            if (rerefer) {
                // 需要重新发布
                LOGGER.info("Rerefer consumer {}", buildKey());
                try {
                    unsubscribe();// 取消订阅旧的
                    for (Map.Entry<String, String> entry : newValues.entrySet()) { // change attrs
                        updateAttribute(entry.getKey(), entry.getValue(), true);
                    }
                } catch (Exception e) { // 切换属性出现异常
                    LOGGER.error("Catch exception when consumer attribute changed", e);
                    for (Map.Entry<String, String> entry : oldValues.entrySet()) { //rollback old attrs
                        updateAttribute(entry.getKey(), entry.getValue(), true);
                    }
                    subscribe(); // 重新订阅回滚后的旧的
                    return;
                }
                try {
                    switchClient();
                } catch (Exception e) { //切换客户端出现异常
                    LOGGER.error("Catch exception when consumer refer after attribute changed", e);
                    unsubscribe(); // 取消订阅新的
                    for (Map.Entry<String, String> entry : oldValues.entrySet()) { //rollback old attrs
                        updateAttribute(entry.getKey(), entry.getValue(), true);
                    }
                    subscribe(); // 重新订阅回滚后的旧的
                }
            }
        }

        /**
         * Switch client.
         * @throws Exception the exception
         */
        private void switchClient() throws Exception {
            Client newclient = null;
            Client oldClient;
            try { // 构建新的
                newclient = ClientFactory.getClient(ConsumerConfig.this); //生成新的 会再重新订阅
                oldClient = ((ClientProxyInvoker) proxyInvoker).setClient(newclient);
            } catch (Exception e) {
                if (newclient != null) {
                    newclient.destroy();
                }
                throw e;
            }
            try { // 切换
                client = newclient;
                if (oldClient != null) {
                    oldClient.destroy(); // 旧的关掉
                }
            } catch (Exception e) {
                LOGGER.warn("Catch exception when destroy");
            }
        }
    }

    /**
     * Gets client.
     *
     * @return the client
     */
    public Client getClient() {
        return client;
    }

    /**
     * Build key.
     *
     * @return the string
     */
    @Override
    public String buildKey() {
        return protocol + "://" + interfaceId + ":" + tags;
    }

    /**
     * Gets proxy class.
     *
     * @return the proxyClass
     */
    @Override
    protected Class<?> getProxyClass() {
        if (proxyClass != null) {
            return proxyClass;
        }
        if (generic) {
            return GenericService.class;
        }
        try {
            if (StringUtils.isNotBlank(interfaceId)) {
                this.proxyClass = ClassLoaderUtils.forName(interfaceId);
                if (!proxyClass.isInterface()) {
                    throw ExceptionUtils.buildRuntime(21301, "consumer.interface",
                            interfaceId, "interfaceId must set interface class, not implement class");
                }
            } else {
                throw ExceptionUtils.buildRuntime(21302, "consumer.interface",
                        "null", "interfaceId must be not null");
            }
        } catch (RuntimeException t) {
            throw new IllegalStateException(t.getMessage(), t);
        }
        return proxyClass;
    }

    /**
     * Is subscribe.
     *
     * @return the boolean
     */
    @Override
    public boolean isSubscribe() {
        return subscribe;
    }

    /**
     * Sets subscribe.
     *
     * @param subscribe the subscribe
     */
    @Override
    public void setSubscribe(boolean subscribe) {
        this.subscribe = subscribe;
    }

    /**
     * Gets url.
     *
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets url.
     *
     * @param url the url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Gets cluster.
     *
     * @return the cluster
     */
    public String getCluster() {
        return cluster;
    }

    /**
     * Sets cluster.
     *
     * @param cluster the cluster
     */
    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    /**
     * Gets retries.
     *
     * @return the retries
     */
    public int getRetries() {
        return retries;
    }

    /**
     * Sets retries.
     *
     * @param retries the retries
     */
    public void setRetries(int retries) {
        this.retries = retries;
    }

    /**
     * Gets loadbalance.
     *
     * @return the loadbalance
     */
    public String getLoadbalance() {
        return loadbalance;
    }

    /**
     * Sets loadbalance.
     *
     * @param loadbalance the loadbalance
     */
    public void setLoadbalance(String loadbalance) {
        this.loadbalance = loadbalance;
    }

    /**
     * Gets generic.
     *
     * @return the generic
     */
    public boolean isGeneric() {
        return generic;
    }

    /**
     * Sets generic.
     *
     * @param generic the generic
     */
    public void setGeneric(boolean generic) {
        this.generic = generic;
    }

    /**
     * Gets protocol.
     *
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Sets protocol.
     *
     * @param protocol the protocol
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Gets threads.
     *
     * @return the threads
     */
    public int getThreads() {
        return threads;
    }

    /**
     * Sets threads.
     *
     * @param threads the threads
     */
    @Deprecated
    public void setThreads(int threads) {
        this.threads = threads;
    }

    /**
     * Gets iothreads.
     *
     * @return the iothreads
     */
    public int getIothreads() {
        return iothreads;
    }

    /**
     * Sets iothreads.
     *
     * @param iothreads the iothreads
     */
    public void setIothreads(int iothreads) {
        this.iothreads = iothreads;
    }

    /**
     * Gets connect timeout.
     *
     * @return the connect timeout
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets connect timeout.
     *
     * @param connectTimeout the connect timeout
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Gets disconnect timeout.
     *
     * @return the disconnect timeout
     */
    public int getDisconnectTimeout() {
        return disconnectTimeout;
    }

    /**
     * Sets disconnect timeout.
     *
     * @param disconnectTimeout the disconnect timeout
     */
    public void setDisconnectTimeout(int disconnectTimeout) {
        this.disconnectTimeout = disconnectTimeout;
    }

    /**
     * Gets threadpool.
     *
     * @return the threadpool
     */
    public String getThreadpool() {
        return threadpool;
    }

    /**
     * Sets threadpool.
     *
     * @param threadpool the threadpool
     */
    @Deprecated
    public void setThreadpool(String threadpool) {
        this.threadpool = threadpool;
    }

    /**
     * Is check.
     *
     * @return the boolean
     */
    public boolean isCheck() {
        return check;
    }

    /**
     * Sets check.
     *
     * @param check the check
     */
    public void setCheck(boolean check) {
        this.check = check;
    }


    /**
     * Gets serialization.
     *
     * @return the serialization
     */
    public String getSerialization() {
        return serialization;
    }

    /**
     * Sets serialization.
     *
     * @param serialization the serialization
     */
    public void setSerialization(String serialization) {
        this.serialization = serialization;
    }

    /**
     * Gets onreturn.
     *
     * @return the onreturn
     */
    public List<ResponseListener> getOnreturn() {
        return onreturn;
    }

    /**
     * Sets onreturn.
     *
     * @param onreturn the onreturn
     */
    public void setOnreturn(List<ResponseListener> onreturn) {
        this.onreturn = onreturn;
    }

    /**
     * Gets onconnect.
     *
     * @return the onconnect
     */
    public List<ChannelListener> getOnconnect() {
        return onconnect;
    }

    /**
     * Sets onconnect.
     *
     * @param onconnect the onconnect
     */
    public void setOnconnect(List<ChannelListener> onconnect) {
        this.onconnect = onconnect;
    }

    /**
     * Gets onavailable.
     *
     * @return the onavailable
     */
    public List<ConsumerStateListener> getOnavailable() {
        return onavailable;
    }

    /**
     * Sets onavailable.
     *
     * @param onavailable  the onavailable
     */
    public void setOnavailable(List<ConsumerStateListener> onavailable) {
        this.onavailable = onavailable;
    }

    /**
     * Is async.
     *
     * @return the boolean
     */
    public boolean isAsync() {
        return async;
    }

    /**
     * Sets async.
     *
     * @param async the async
     */
    public void setAsync(boolean async) {
        this.async = async;
    }

    /**
     * Is injvm.
     *
     * @return the boolean
     */
    public boolean isInjvm() {
        return injvm;
    }

    /**
     * Sets injvm.
     *
     * @param injvm the injvm
     */
    public void setInjvm(boolean injvm) {
        this.injvm = injvm;
    }

    /**
     * Is lazy.
     *
     * @return the boolean
     */
    public boolean isLazy() {
        return lazy;
    }

    /**
     * Sets lazy.
     *
     * @param lazy the lazy
     */
    public void setLazy(boolean lazy) {
        this.lazy = lazy;
    }

    /**
     * Is sticky.
     *
     * @return the boolean
     */
    public boolean isSticky() {
        return sticky;
    }

    /**
     * Sets sticky.
     *
     * @param sticky the sticky
     */
    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }

    /**
     * Gets reconnect.
     *
     * @return the reconnect
     */
    public int getReconnect() {
        return reconnect;
    }

    /**
     * Sets reconnect.
     *
     * @param reconnect the reconnect
     */
    public void setReconnect(int reconnect) {
        this.reconnect = reconnect;
    }

    /**
     * Gets heartbeat.
     *
     * @return the heartbeat
     */
    public int getHeartbeat() {
        return heartbeat;
    }

    /**
     * Sets heartbeat.
     *
     * @param heartbeat the heartbeat
     */
    public void setHeartbeat(int heartbeat) {
        this.heartbeat = heartbeat;
    }

    /**
     * Gets payload.
     *
     * @return the payload
     */
    public int getPayload() {
        return payload;
    }

    /**
     * Sets payload.
     *
     * @param payload the payload
     */
    public void setPayload(int payload) {
        this.payload = payload;
    }

    /**
     * Gets router.
     *
     * @return the router
     */
    public List<Router> getRouter() {
        return router;
    }

    /**
     * Sets router.
     *
     * @param router the router
     */
    public void setRouter(List<Router> router) {
        this.router = router;
    }

    /**
     * Gets concurrents.
     *
     * @return the concurrents
     */
    public int getConcurrents() {
        return concurrents;
    }

    /**
     * Sets concurrents.
     *
     * @param concurrents the concurrents
     */
    public void setConcurrents(int concurrents) {
        this.concurrents = concurrents;
    }

    /**
     * 是否有并发控制需求，有就打开过滤器
     * 配置-1关闭并发过滤器，等于0表示开启过滤但是不限制
     *
     * @return 是否配置了concurrents boolean
     */
    public boolean hasConcurrents() {
        return concurrents >= 0;
    }

    /**
     * 得到方法的重试次数，默认接口配置
     *
     * @param methodName 方法名
     * @return 方法的重试次数 method retries
     */
    public int getMethodRetries(String methodName) {
        return (Integer) getMethodConfigValue(methodName, BsoaConstants.CONFIG_KEY_RETRIES,
                getRetries());
    }

    /**
     * Gets time out.
     *
     * @param methodName the method name
     * @return the time out
     */
    public int getMethodTimeout(String methodName) {
        return (Integer) getMethodConfigValue(methodName, BsoaConstants.CONFIG_KEY_TIMEOUT,
                getTimeout());
    }

    /**
     * 得到方法名对应的自定义参数列表
     *
     * @param methodName 方法名，不支持重载
     * @return method onreturn
     */
    public List<ResponseListener> getMethodOnreturn(String methodName) {
        return (List<ResponseListener>) getMethodConfigValue(methodName, BsoaConstants.CONFIG_KEY_ONRETURN,
                getOnreturn());
    }

    /**
     * Gets time out.
     *
     * @param methodName the method name
     * @return the time out
     */
    public boolean getMethodAsync(String methodName) {
        return (Boolean) getMethodConfigValue(methodName, BsoaConstants.CONFIG_KEY_ASYNC,
                isAsync());
    }

    /**
     * 除了判断自己，还有判断下面方法的自定义判断
     *
     * @return the validation
     */
    public boolean hasAsyncMethod() {
        if (isAsync()) {
            return true;
        }
        if (methods != null && methods.size() > 0) {
//            for (MethodConfig methodConfig : methods.values()) {
//                if (CommonUtils.isTrue(methodConfig.getAsync())) {
//                    return true;
//                }
//            }
        }
        return false;
    }

    /**
     * 得到实现代理类
     *
     * @return 实现代理类
     */
    public T getProxyIns() {
        return proxyIns;
    }

    /**
     * 得到实现代理类Invoker
     *
     * @return 实现代理类Invoker
     */
    public Invoker getProxyInvoker() {
        return proxyInvoker;
    }
}
