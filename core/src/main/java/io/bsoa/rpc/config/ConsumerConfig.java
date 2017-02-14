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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.GenericService;
import io.bsoa.rpc.base.Cache;
import io.bsoa.rpc.bootstrap.Bootstraps;
import io.bsoa.rpc.bootstrap.ConsumerBootstrap;
import io.bsoa.rpc.client.Router;
import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.ClassLoaderUtils;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.filter.Filter;
import io.bsoa.rpc.listener.ChannelListener;
import io.bsoa.rpc.listener.ConsumerStateListener;
import io.bsoa.rpc.listener.ResponseListener;

import static io.bsoa.rpc.common.BsoaConfigs.getBooleanValue;
import static io.bsoa.rpc.common.BsoaConfigs.getIntValue;
import static io.bsoa.rpc.common.BsoaConfigs.getStringValue;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_ASYNC;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_CHECK;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_CLUSTER;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_CONCURRENTS;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_CONNECTION;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_CONNECTION_HOLDER;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_CONNECT_TIMEOUT;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_DISCONNECT_TIMEOUT;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_HEARTBEAT_PERIOD;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_INJVM;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_INVOKE_TIMEOUT;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_LAZY;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_LOAD_BALANCER;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_ONEWAY;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_RECONNECT_PERIOD;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_RETRIES;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_STICKY;
import static io.bsoa.rpc.common.BsoaOptions.DEFAULT_PROTOCOL;
import static io.bsoa.rpc.common.BsoaOptions.DEFAULT_SERIALIZATION;
import static io.bsoa.rpc.config.ConfigValueHelper.checkNormalWithCommaColon;

/**
 * Created by zhangg on 16-7-7.
 *
 * @param <T> the type parameter
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class ConsumerConfig<T> extends AbstractInterfaceConfig<T> implements Serializable {

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
    protected String protocol = getStringValue(DEFAULT_PROTOCOL);

    /**
     * 默认序列化
     */
    protected String serialization = getStringValue(DEFAULT_SERIALIZATION);

    /**
     * 直连调用地址
     */
    protected String url;

    /**
     * 是否泛化调用
     */
    protected boolean generic;

    /**
     * 是否异步调用
     */
    protected boolean async = getBooleanValue(CONSUMER_ASYNC);

    /**
     * 连接超时时间
     */
    protected int connectTimeout = getIntValue(CONSUMER_CONNECT_TIMEOUT);

    /**
     * 关闭超时时间（如果还有请求，会等待请求结束或者超时）
     */
    protected int disconnectTimeout = getIntValue(CONSUMER_DISCONNECT_TIMEOUT);

    /**
     * 集群处理，默认是failover
     */
    protected String cluster = getStringValue(CONSUMER_CLUSTER);

    /**
     * The Retries. 失败后重试次数
     */
    protected int retries = getIntValue(CONSUMER_RETRIES);

    /**
     * The ConnectionHolder 连接管理器
     */
    protected String connectionHolder = getStringValue(CONSUMER_CONNECTION_HOLDER);

    /**
     * The LoadBalancer. 负载均衡
     */
    protected String loadBalancer = getStringValue(CONSUMER_LOAD_BALANCER);

    /**
     * 是否延迟建立长连接,
     * connect transport when invoke, but not when init
     */
    protected boolean lazy = getBooleanValue(CONSUMER_LAZY);

    /**
     * 粘滞连接，一个断开才选下一个
     * change transport when current is disconnected
     */
    protected boolean sticky = getBooleanValue(CONSUMER_STICKY);

    /**
     * 是否jvm内部调用（provider和consumer配置在同一个jvm内，则走本地jvm内部，不走远程）
     */
    protected boolean inJVM = getBooleanValue(CONSUMER_INJVM);

    /**
     * 是否强依赖（即没有服务节点就启动失败）
     */
    protected boolean check = getBooleanValue(CONSUMER_CHECK);

    /**
     * 是否单向调用（不关心结果，服务端不响应）
     */
    protected boolean oneWay = getBooleanValue(CONSUMER_ONEWAY);

    /**
     * 长连接个数
     */
    protected int connection = getIntValue(CONSUMER_CONNECTION);

    /**
     * Consumer给Provider发心跳的间隔
     */
    protected int heartbeat = getIntValue(CONSUMER_HEARTBEAT_PERIOD);

    /**
     * Consumer给Provider重连的间隔
     */
    protected int reconnect = getIntValue(CONSUMER_RECONNECT_PERIOD);

    /**
     * 路由规则引用，多个用英文逗号隔开。List<Router>
     */
    protected transient List<Router> router;

    /**
     * 返回值之前的listener,处理结果或者异常
     */
    protected transient List<ResponseListener> onReturn;

    /**
     * 连接事件监听器实例，连接或者断开时触发
     */
    protected transient List<ChannelListener> onConnect;

    /**
     * 客户端状态变化监听器实例，状态可用和不可以时触发
     */
    protected transient List<ConsumerStateListener> onAvailable;


    /*-------- 下面是方法级配置 --------*/
    /**
     * 服务端执行超时时间(毫秒)
     */
    protected int timeout = getIntValue(CONSUMER_INVOKE_TIMEOUT);

    /**
     * 接口下每方法的最大可并行执行请求数，配置-1关闭并发过滤器，等于0表示开启过滤但是不限制
     */
    protected int concurrents = getIntValue(CONSUMER_CONCURRENTS);

	/*---------- 参数配置项结束 ------------*/

    /**
     * 服务消费者启动类
     */
	private transient ConsumerBootstrap<T> consumerBootstrap;
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
    public Class<?> getProxyClass() {
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
     * @return the protocol
     */
    public ConsumerConfig<T> setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
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
     * @return the serialization
     */
    public ConsumerConfig<T> setSerialization(String serialization) {
        this.serialization = serialization;
        return this;
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
     * @return the url
     */
    public ConsumerConfig<T> setUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * Is generic boolean.
     *
     * @return the boolean
     */
    public boolean isGeneric() {
        return generic;
    }

    /**
     * Sets generic.
     *
     * @param generic the generic
     * @return the generic
     */
    public ConsumerConfig<T> setGeneric(boolean generic) {
        this.generic = generic;
        return this;
    }

    /**
     * Is async boolean.
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
     * @return the async
     */
    public ConsumerConfig<T> setAsync(boolean async) {
        this.async = async;
        return this;
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
     * @return the connect timeout
     */
    public ConsumerConfig<T> setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
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
     * @return the disconnect timeout
     */
    public ConsumerConfig<T> setDisconnectTimeout(int disconnectTimeout) {
        this.disconnectTimeout = disconnectTimeout;
        return this;
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
     * @return the cluster
     */
    public ConsumerConfig<T> setCluster(String cluster) {
        this.cluster = cluster;
        return this;
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
     * @return the retries
     */
    public ConsumerConfig<T> setRetries(int retries) {
        this.retries = retries;
        return this;
    }

    /**
     * Gets connection holder.
     *
     * @return the connection holder
     */
    public String getConnectionHolder() {
        return connectionHolder;
    }

    /**
     * Sets connection holder.
     *
     * @param connectionHolder the connection holder
     * @return the connection holder
     */
    public ConsumerConfig<T> setConnectionHolder(String connectionHolder) {
        this.connectionHolder = connectionHolder;
        return this;
    }

    /**
     * Gets load balancer.
     *
     * @return the load balancer
     */
    public String getLoadBalancer() {
        return loadBalancer;
    }

    /**
     * Sets load balancer.
     *
     * @param loadBalancer the load balancer
     * @return the load balancer
     */
    public ConsumerConfig<T> setLoadBalancer(String loadBalancer) {
        this.loadBalancer = loadBalancer;
        return this;
    }

    /**
     * Is lazy boolean.
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
     * @return the lazy
     */
    public ConsumerConfig<T> setLazy(boolean lazy) {
        this.lazy = lazy;
        return this;
    }

    /**
     * Is sticky boolean.
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
     * @return the sticky
     */
    public ConsumerConfig<T> setSticky(boolean sticky) {
        this.sticky = sticky;
        return this;
    }

    /**
     * Is in jvm boolean.
     *
     * @return the boolean
     */
    public boolean isInJVM() {
        return inJVM;
    }

    /**
     * Sets in jvm.
     *
     * @param inJVM the in jvm
     * @return the in jvm
     */
    public ConsumerConfig<T> setInJVM(boolean inJVM) {
        this.inJVM = inJVM;
        return this;
    }

    /**
     * Is check boolean.
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
     * @return the check
     */
    public ConsumerConfig<T> setCheck(boolean check) {
        this.check = check;
        return this;
    }

    /**
     * Is one way boolean.
     *
     * @return the boolean
     */
    public boolean isOneWay() {
        return oneWay;
    }

    /**
     * Sets one way.
     *
     * @param oneWay the one way
     * @return the one way
     */
    public ConsumerConfig<T> setOneWay(boolean oneWay) {
        this.oneWay = oneWay;
        return this;
    }

    /**
     * Gets connection.
     *
     * @return the connection
     */
    public int getConnection() {
        return connection;
    }

    /**
     * Sets connection.
     *
     * @param connection the connection
     * @return the connection
     */
    public ConsumerConfig<T> setConnection(int connection) {
        this.connection = connection;
        return this;
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
     * @return the heartbeat
     */
    public ConsumerConfig<T> setHeartbeat(int heartbeat) {
        this.heartbeat = heartbeat;
        return this;
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
     * @return the reconnect
     */
    public ConsumerConfig<T> setReconnect(int reconnect) {
        this.reconnect = reconnect;
        return this;
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
     * @return the router
     */
    public ConsumerConfig<T> setRouter(List<Router> router) {
        this.router = router;
        return this;
    }

    /**
     * Gets on return.
     *
     * @return the on return
     */
    public List<ResponseListener> getOnReturn() {
        return onReturn;
    }

    /**
     * Sets on return.
     *
     * @param onReturn the on return
     * @return the on return
     */
    public ConsumerConfig<T> setOnReturn(List<ResponseListener> onReturn) {
        this.onReturn = onReturn;
        return this;
    }

    /**
     * Gets on connect.
     *
     * @return the on connect
     */
    public List<ChannelListener> getOnConnect() {
        return onConnect;
    }

    /**
     * Sets on connect.
     *
     * @param onConnect the on connect
     * @return the on connect
     */
    public ConsumerConfig<T> setOnConnect(List<ChannelListener> onConnect) {
        this.onConnect = onConnect;
        return this;
    }

    /**
     * Gets on available.
     *
     * @return the on available
     */
    public List<ConsumerStateListener> getOnAvailable() {
        return onAvailable;
    }

    /**
     * Sets on available.
     *
     * @param onAvailable the on available
     * @return the on available
     */
    public ConsumerConfig<T> setOnAvailable(List<ConsumerStateListener> onAvailable) {
        this.onAvailable = onAvailable;
        return this;
    }

    /**
     * Gets timeout.
     *
     * @return the timeout
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets timeout.
     *
     * @param timeout the timeout
     * @return the timeout
     */
    public ConsumerConfig<T> setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
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
     * @return the concurrents
     */
    public ConsumerConfig<T> setConcurrents(int concurrents) {
        this.concurrents = concurrents;
        return this;
    }

    /**
     * Sets interface id.
     *
     * @param interfaceId the interface id
     * @return the interface id
     */
    public ConsumerConfig<T> setInterfaceId(String interfaceId) {
        this.interfaceId = interfaceId;
        return this;
    }

    /**
     * Sets tags.
     *
     * @param tags the tags
     */
    public ConsumerConfig<T> setTags(String tags) {
        checkNormalWithCommaColon("tags", tags);
        this.tags = tags;
        return this;
    }

    /**
     * Sets filter ref.
     *
     * @param filterRef the filter ref
     * @return the filter ref
     */
    public ConsumerConfig<T> setFilterRef(List<Filter> filterRef) {
        this.filterRef = filterRef;
        return this;
    }

    /**
     * Sets filter.
     *
     * @param filter the filter
     * @return the filter
     */
    public ConsumerConfig<T> setFilters(List<String> filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Sets registry.
     *
     * @param registry the registry
     * @return the registry
     */
    public ConsumerConfig<T> setRegistry(List<RegistryConfig> registry) {
        this.registry = registry;
        return this;
    }

    /**
     * Sets methods.
     *
     * @param methods the methods
     * @return the methods
     */
    public ConsumerConfig<T> setMethods(Map<String, MethodConfig> methods) {
        this.methods = methods;
        return this;
    }

    /**
     * Sets register.
     *
     * @param register the register
     * @return the register
     */
    public ConsumerConfig<T> setRegister(boolean register) {
        this.register = register;
        return this;
    }

    /**
     * Sets subscribe.
     *
     * @param subscribe the subscribe
     * @return the subscribe
     */
    public ConsumerConfig<T> setSubscribe(boolean subscribe) {
        this.subscribe = subscribe;
        return this;
    }

    /**
     * Sets proxy.
     *
     * @param proxy the proxy
     * @return the proxy
     */
    public ConsumerConfig<T> setProxy(String proxy) {
        this.proxy = proxy;
        return this;
    }

    /**
     * Sets cache ref.
     *
     * @param cacheRef the cache ref
     * @return the cache ref
     */
    public ConsumerConfig<T> setCacheRef(Cache cacheRef) {
        this.cacheRef = cacheRef;
        return this;
    }

    /**
     * Sets mock ref.
     *
     * @param mockRef the mock ref
     * @return the mock ref
     */
    public ConsumerConfig<T> setMockRef(T mockRef) {
        this.mockRef = mockRef;
        return this;
    }

    /**
     * Sets parameters.
     *
     * @param parameters the parameters
     * @return the parameters
     */
    public ConsumerConfig<T> setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
        return this;
    }

    /**
     * Sets mock.
     *
     * @param mock the mock
     * @return the mock
     */
    public ConsumerConfig<T> setMock(boolean mock) {
        this.mock = mock;
        return this;
    }


    /**
     * Sets validation.
     *
     * @param validation the validation
     * @return the validation
     */
    public ConsumerConfig<T> setValidation(boolean validation) {
        this.validation = validation;
        return this;
    }

    /**
     * Sets compress.
     *
     * @param compress the compress
     * @return the compress
     */
    public ConsumerConfig<T> setCompress(String compress) {
        this.compress = compress;
        return this;
    }

    /**
     * Sets cache.
     *
     * @param cache the cache
     * @return the cache
     */
    public ConsumerConfig<T> setCache(boolean cache) {
        this.cache = cache;
        return this;
    }

    @Override
    public boolean hasTimeout() {
        if (timeout > 0) {
            return true;
        }
        if (CommonUtils.isNotEmpty(methods)) {
            for (MethodConfig methodConfig : methods.values()) {
                if (methodConfig.getTimeout() != null && methodConfig.getTimeout() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 是否有并发控制需求，有就打开过滤器
     * 配置-1关闭并发过滤器，等于0表示开启过滤但是不限制
     *
     * @return 是否配置了concurrents boolean
     */
    public boolean hasConcurrents() {
        if (concurrents > 0) {
            return true;
        }
        if (CommonUtils.isNotEmpty(methods)) {
            for (MethodConfig methodConfig : methods.values()) {
                if (methodConfig.getConcurrents() != null
                        && methodConfig.getConcurrents() > 0) {
                    return true;
                }
            }
        }
        return false;
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
     * @return method onReturn
     */
    public List<ResponseListener> getMethodOnreturn(String methodName) {
        return (List<ResponseListener>) getMethodConfigValue(methodName, BsoaConstants.CONFIG_KEY_ONRETURN,
                getOnReturn());
    }

    /**
     * Gets time out.
     *
     * @param methodName the method name
     * @return the time out
     */
    @Deprecated
    public boolean getMethodAsync(String methodName) {
        return (Boolean) getMethodConfigValue(methodName, BsoaConstants.CONFIG_KEY_ASYNC,
                isAsync());
    }

    /**
     * 除了判断自己，还有判断下面方法的自定义判断
     *
     * @return the validation
     */
    @Deprecated
    public boolean hasAsyncMethod() {
        if (isAsync()) {
            return true;
        }
        if (CommonUtils.isNotEmpty(methods)) {
            for (MethodConfig methodConfig : methods.values()) {
                if (CommonUtils.isTrue(methodConfig.getAsync())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 引用服务
     *
     * @return 服务代理类
     */
    public T refer() {
        if (consumerBootstrap == null) {
            consumerBootstrap = Bootstraps.from(this);
        }
        return consumerBootstrap.refer();
    }

    /**
     * 取消引用服务
     */
    public void unRefer() {
        if (consumerBootstrap != null) {
            consumerBootstrap.unRefer();
        }
    }

    /**
     * 得到服务消费这启动器
     *
     * @return 服务消费这启动器
     */
    public ConsumerBootstrap<T> getBootstrap() {
        return consumerBootstrap;
    }
}
