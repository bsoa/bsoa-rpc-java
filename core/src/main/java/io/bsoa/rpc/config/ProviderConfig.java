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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.utils.ClassLoaderUtils;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.exception.BsoaRuntimeException;

import static io.bsoa.rpc.common.BsoaConfigs.PROVIDER_CONCURRENTS;
import static io.bsoa.rpc.common.BsoaConfigs.PROVIDER_DELAY;
import static io.bsoa.rpc.common.BsoaConfigs.PROVIDER_DYNAMIC;
import static io.bsoa.rpc.common.BsoaConfigs.PROVIDER_EXCLUDE;
import static io.bsoa.rpc.common.BsoaConfigs.PROVIDER_INCLUDE;
import static io.bsoa.rpc.common.BsoaConfigs.PROVIDER_INVOKE_TIMEOUT;
import static io.bsoa.rpc.common.BsoaConfigs.PROVIDER_PRIORITY;
import static io.bsoa.rpc.common.BsoaConfigs.PROVIDER_WEIGHT;
import static io.bsoa.rpc.common.BsoaConfigs.getBooleanValue;
import static io.bsoa.rpc.common.BsoaConfigs.getIntValue;
import static io.bsoa.rpc.common.BsoaConfigs.getStringValue;

/**
 * Created by zhangg on 16-7-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class ProviderConfig<T> extends AbstractInterfaceConfig<T> implements Serializable {

    /**
     * The constant serialVersionUID.
     */
    private static final long serialVersionUID = -3058073881775315962L;

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ProviderConfig.class);

    /*---------- 参数配置项开始 ------------*/
    /**
     * 接口实现类引用
     */
    protected transient T ref;

    /**
     * 配置的协议列表
     */
    protected List<ServerConfig> server;

    /**
     * 服务发布延迟,单位毫秒，默认0，配置为-1代表spring加载完毕（通过spring才生效）
     */
    protected int delay = getIntValue(PROVIDER_DELAY);

    /**
     * 权重
     */
    protected int weight = getIntValue(PROVIDER_WEIGHT);

    /**
     * 包含的方法
     */
    protected String include = getStringValue(PROVIDER_INCLUDE);

    /**
     * 不发布的方法列表，逗号分隔
     */
    protected String exclude = getStringValue(PROVIDER_EXCLUDE);

    /**
     * 是否动态注册，默认为true，配置为false代表不主动发布，需要到管理端进行上线操作
     */
    protected boolean dynamic = getBooleanValue(PROVIDER_DYNAMIC);

    /**
     * 服务优先级，越大越高
     */
    protected int priority = getIntValue(PROVIDER_PRIORITY);

    /**
     * whitelist blacklist
     */

    /*-------- 下面是方法级配置 --------*/

    /**
     * 服务端执行超时时间(毫秒)
     */
    protected int timeout = getIntValue(PROVIDER_INVOKE_TIMEOUT);

    /**
     * 接口下每方法的最大可并行执行请求数，配置-1关闭并发过滤器，等于0表示开启过滤但是不限制
     */
    protected int concurrents = getIntValue(PROVIDER_CONCURRENTS);

    /*---------- 参数配置项结束 ------------*/

    /**
     * 方法名称：是否可调用
     */
    protected transient volatile ConcurrentHashMap<String, Boolean> methodsLimit;

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
        try {
            if (StringUtils.isNotBlank(interfaceId)) {
                this.proxyClass = ClassLoaderUtils.forName(interfaceId);
                if (!proxyClass.isInterface()) {
                    throw ExceptionUtils.buildRuntime(21206, "service.interfaceId",
                            interfaceId,
                            "interfaceId must set interface class, not implement class");
                }
            } else {
                throw ExceptionUtils.buildRuntime(21207, "service.interfaceId",
                        "null", "interfaceId must be not null");
            }
        } catch (BsoaRuntimeException t) {
            throw new BsoaRuntimeException(22222, t.getMessage(), t);
        }
        return proxyClass;
    }

    /**
     * Build key.
     *
     * @return the string
     */
    @Override
    public String buildKey() {
        return interfaceId + ":" + tags;
    }

    /**
     * Gets server.
     *
     * @return the server
     */
    public List<ServerConfig> getServer() {
        return server;
    }

    /**
     * Sets server.
     *
     * @param server the server
     */
    public void setServer(List<ServerConfig> server) {
        this.server = server;
    }

    /**
     * Gets ref.
     *
     * @return the ref
     */
    public T getRef() {
        return ref;
    }

    /**
     * Sets ref.
     *
     * @param ref the ref
     */
    public void setRef(T ref) {
        this.ref = ref;
    }

    /**
     * Gets weight.
     *
     * @return the weight
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Sets weight.
     *
     * @param weight the weight
     */
    public void setWeight(int weight) {
        this.weight = weight;
    }

    /**
     * Gets include.
     *
     * @return the include
     */
    public String getInclude() {
        return include;
    }

    /**
     * Sets include.
     *
     * @param include the include
     */
    public void setInclude(String include) {
        this.include = include;
    }

    /**
     * Gets exclude.
     *
     * @return the exclude
     */
    public String getExclude() {
        return exclude;
    }

    /**
     * Sets exclude.
     *
     * @param exclude the exclude
     */
    public void setExclude(String exclude) {
        this.exclude = exclude;
    }

    /**
     * Gets delay.
     *
     * @return the delay
     */
    public int getDelay() {
        return delay;
    }

    /**
     * Sets delay.
     *
     * @param delay the delay
     */
    public void setDelay(int delay) {
        this.delay = delay;
    }

    /**
     * Is dynamic.
     *
     * @return the boolean
     */
    public boolean isDynamic() {
        return dynamic;
    }

    /**
     * Sets dynamic.
     *
     * @param dynamic the dynamic
     */
    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    /**
     * Gets priority.
     *
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Sets priority.
     *
     * @param priority the priority
     */
    public void setPriority(int priority) {
        this.priority = priority;
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
                if (methodConfig.getTimeout() > 0) {
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
     * add server.
     *
     * @param server ServerConfig
     */
    public void setServer(ServerConfig server) {
        if (this.server == null) {
            this.server = new ArrayList<ServerConfig>();
        }
        this.server.add(server);
    }

    /**
     * 得到可调用的方法名称列表
     *
     * @return 可调用的方法名称列表
     */
    public Map<String, Boolean> getMethodsLimit() {
        return methodsLimit;
    }

}
