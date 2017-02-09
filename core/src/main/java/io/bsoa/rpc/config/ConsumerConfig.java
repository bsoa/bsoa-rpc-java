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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.GenericService;
import io.bsoa.rpc.client.Router;
import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.ClassLoaderUtils;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.listener.ChannelListener;
import io.bsoa.rpc.listener.ConsumerStateListener;
import io.bsoa.rpc.listener.ResponseListener;

import static io.bsoa.rpc.common.BsoaConfigs.*;

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
    protected int disconnectTimeout = getIntValue(BsoaConfigs.CONSUMER_DISCONNECT_TIMEOUT);

    /**
     * 集群处理，默认是failover
     */
    protected String cluster = getStringValue(BsoaConfigs.CONSUMER_CLUSTER);

    /**
     * The Retries. 失败后重试次数
     */
    protected int retries = getIntValue(BsoaConfigs.CONSUMER_RETRIES);

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
     * Gets connectionHolder.
     *
     * @return the connectionHolder
     */
    public String getConnectionHolder() {
        return connectionHolder;
    }

    /**
     * Sets connectionHolder.
     *
     * @param connectionHolder the connectionHolder
     */
    public void setConnectionHolder(String connectionHolder) {
        this.connectionHolder = connectionHolder;
    }

    /**
     * Gets loadBalancer.
     *
     * @return the loadBalancer
     */
    public String getLoadBalancer() {
        return loadBalancer;
    }

    /**
     * Sets loadBalancer.
     *
     * @param loadBalancer the loadBalancer
     */
    public void setLoadBalancer(String loadBalancer) {
        this.loadBalancer = loadBalancer;
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
     * Gets onReturn.
     *
     * @return the onReturn
     */
    public List<ResponseListener> getOnReturn() {
        return onReturn;
    }

    /**
     * Sets onReturn.
     *
     * @param onReturn the onReturn
     */
    public void setOnReturn(List<ResponseListener> onReturn) {
        this.onReturn = onReturn;
    }

    /**
     * Gets onConnect.
     *
     * @return the onConnect
     */
    public List<ChannelListener> getOnConnect() {
        return onConnect;
    }

    /**
     * Sets onConnect.
     *
     * @param onConnect the onConnect
     */
    public void setOnConnect(List<ChannelListener> onConnect) {
        this.onConnect = onConnect;
    }

    /**
     * Gets onAvailable.
     *
     * @return the onAvailable
     */
    public List<ConsumerStateListener> getOnAvailable() {
        return onAvailable;
    }

    /**
     * Sets onAvailable.
     *
     * @param onAvailable the onAvailable
     */
    public void setOnAvailable(List<ConsumerStateListener> onAvailable) {
        this.onAvailable = onAvailable;
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
     * Is inJVM.
     *
     * @return the boolean
     */
    public boolean isInJVM() {
        return inJVM;
    }

    /**
     * Sets inJVM.
     *
     * @param inJVM the inJVM
     */
    public void setInJVM(boolean inJVM) {
        this.inJVM = inJVM;
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
     * Is oneWay boolean.
     *
     * @return the boolean
     */
    public boolean isOneWay() {
        return oneWay;
    }

    /**
     * Sets oneWay.
     *
     * @param oneWay the oneWay
     * @return the oneWay
     */
    public ConsumerConfig setOneWay(boolean oneWay) {
        this.oneWay = oneWay;
        return this;
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
     */
    public void setConnection(int connection) {
        this.connection = connection;
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
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
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
}
