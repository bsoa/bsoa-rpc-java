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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.base.Cache;
import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.BeanUtils;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.CompatibleTypeUtils;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.common.utils.ReflectUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.filter.Filter;
import io.bsoa.rpc.listener.ConfigListener;

import static io.bsoa.rpc.common.BsoaConfigs.DEFAULT_PROXY;
import static io.bsoa.rpc.common.BsoaConfigs.DEFAULT_TAGS;
import static io.bsoa.rpc.common.BsoaConfigs.SERVICE_REGISTER;
import static io.bsoa.rpc.common.BsoaConfigs.SERVICE_SUBSCRIBE;
import static io.bsoa.rpc.common.BsoaConfigs.getBooleanValue;
import static io.bsoa.rpc.common.BsoaConfigs.getStringValue;
import static io.bsoa.rpc.config.ConfigValueHelper.checkNormalWithCommaColon;

/**
 * 接口级的公共配置
 * <p>
 * Created by zhangg on 16-7-7.
 *
 * @param <T> the type parameter
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public abstract class AbstractInterfaceConfig<T> extends AbstractIdConfig implements Serializable {

    /**
     * The constant serialVersionUID.
     */
    private static final long serialVersionUID = -8738241729920479618L;

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractInterfaceConfig.class);

    /*-------------配置项开始----------------*/
    /**
     * 不管普通调用和泛化调用，都是设置实际的接口类名称
     */
    protected String interfaceId;
    /**
     * 服务标签
     */
    protected String tags = getStringValue(DEFAULT_TAGS);

    /**
     * 过滤器配置实例，多个用逗号隔开
     */
    protected transient List<Filter> filterRef;

    /**
     * 过滤器配置别名
     */
    protected List<String> filter;

    /**
     * 注册中心配置，可配置多个
     */
    protected List<RegistryConfig> registry;

    /**
     * 方法配置，可配置多个
     */
    protected Map<String, MethodConfig> methods;

    /**
     * 是否注册，如果是false只订阅不注册
     */
    protected boolean register = getBooleanValue(SERVICE_REGISTER);

    /**
     * 是否订阅服务
     */
    protected boolean subscribe = getBooleanValue(SERVICE_SUBSCRIBE);

    /**
     * 代理类型
     */
    protected String proxy = getStringValue(DEFAULT_PROXY);

    /**
     * 结果缓存实现类
     */
    protected transient Cache cacheRef;

    /**
     * Mock实现类
     */
    protected transient T mockRef;

    /**
     * 自定义参数
     */
    protected Map<String, String> parameters;

    /*-------- 下面是方法级配置 --------*/

    /**
     * 接口下每方法的最大可并行执行请求数，配置-1关闭并发过滤器，等于0表示开启过滤但是不限制
     * 子类默认值不一样
     protected int concurrents = 0;*/

    /**
     * 是否开启mock
     */
    protected boolean mock;

    /**
     * 是否开启参数验证(jsr303)
     */
    protected boolean validation;

    /**
     * 压缩算法，为空则不压缩
     */
    protected String compress;

    /**
     * 是否启动结果缓存
     */
    protected boolean cache;

	/*-------------配置项结束----------------*/

    /**
     * 方法名称和方法参数配置的map，不需要遍历list
     */
    protected transient volatile Map<String, Object> configValueCache = null;

    /**
     * 代理接口类，和T对应，主要针对泛化调用
     */
    protected transient volatile Class proxyClass;

    /**
     * 服务配置的listener
     */
    protected transient volatile ConfigListener configListener;

    /**
     * Gets proxy class.
     *
     * @return the proxyClass
     */
    protected abstract Class<?> getProxyClass();

    /**
     * 构造关键字方法
     *
     * @return 唯一标识 string
     */
    protected abstract String buildKey();


    /**
     * Gets interface id.
     *
     * @return the interface id
     */
    public String getInterfaceId() {
        return interfaceId;
    }

    /**
     * Sets interface id.
     *
     * @param interfaceId the interface id
     * @return the interface id
     */
    public AbstractInterfaceConfig setInterfaceId(String interfaceId) {
        this.interfaceId = interfaceId;
        return this;
    }

    /**
     * Gets tags.
     *
     * @return the tags
     */
    public String getTags() {
        return tags;
    }

    /**
     * Sets tags.
     *
     * @param tags the tags
     */
    public void setTags(String tags) {
        checkNormalWithCommaColon("tags", tags);
        this.tags = tags;
    }

    /**
     * Gets filter ref.
     *
     * @return the filter ref
     */
    public List<Filter> getFilterRef() {
        return filterRef;
    }

    /**
     * Sets filter ref.
     *
     * @param filterRef the filter ref
     * @return the filter ref
     */
    public AbstractInterfaceConfig setFilterRef(List<Filter> filterRef) {
        this.filterRef = filterRef;
        return this;
    }

    /**
     * Gets filters.
     *
     * @return the filters
     */
    public List<String> getFilter() {
        return filter;
    }

    /**
     * Sets filter.
     *
     * @param filter the filter
     * @return the filter
     */
    public AbstractInterfaceConfig setFilters(List<String> filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Gets registry.
     *
     * @return the registry
     */
    public List<RegistryConfig> getRegistry() {
        return registry;
    }

    /**
     * Sets registry.
     *
     * @param registry the registry
     * @return the registry
     */
    public AbstractInterfaceConfig setRegistry(List<RegistryConfig> registry) {
        this.registry = registry;
        return this;
    }

    /**
     * Gets methods.
     *
     * @return the methods
     */
    public Map<String, MethodConfig> getMethods() {
        return methods;
    }

    /**
     * Sets methods.
     *
     * @param methods the methods
     * @return the methods
     */
    public AbstractInterfaceConfig setMethods(Map<String, MethodConfig> methods) {
        this.methods = methods;
        return this;
    }

    /**
     * Is register boolean.
     *
     * @return the boolean
     */
    public boolean isRegister() {
        return register;
    }

    /**
     * Sets register.
     *
     * @param register the register
     * @return the register
     */
    public AbstractInterfaceConfig setRegister(boolean register) {
        this.register = register;
        return this;
    }

    /**
     * Is subscribe boolean.
     *
     * @return the boolean
     */
    public boolean isSubscribe() {
        return subscribe;
    }

    /**
     * Sets subscribe.
     *
     * @param subscribe the subscribe
     * @return the subscribe
     */
    public AbstractInterfaceConfig setSubscribe(boolean subscribe) {
        this.subscribe = subscribe;
        return this;
    }

    /**
     * Gets proxy.
     *
     * @return the proxy
     */
    public String getProxy() {
        return proxy;
    }

    /**
     * Sets proxy.
     *
     * @param proxy the proxy
     * @return the proxy
     */
    public AbstractInterfaceConfig setProxy(String proxy) {
        this.proxy = proxy;
        return this;
    }

    /**
     * Gets cache ref.
     *
     * @return the cache ref
     */
    public Cache getCacheRef() {
        return cacheRef;
    }

    /**
     * Sets cache ref.
     *
     * @param cacheRef the cache ref
     * @return the cache ref
     */
    public AbstractInterfaceConfig setCacheRef(Cache cacheRef) {
        this.cacheRef = cacheRef;
        return this;
    }

    /**
     * Gets mock ref.
     *
     * @return the mock ref
     */
    public T getMockRef() {
        return mockRef;
    }

    /**
     * Sets mock ref.
     *
     * @param mockRef the mock ref
     * @return the mock ref
     */
    public AbstractInterfaceConfig setMockRef(T mockRef) {
        this.mockRef = mockRef;
        return this;
    }

    /**
     * Gets parameters.
     *
     * @return the parameters
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Sets parameters.
     *
     * @param parameters the parameters
     * @return the parameters
     */
    public AbstractInterfaceConfig setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
        return this;
    }

    /**
     * Is mock boolean.
     *
     * @return the boolean
     */
    public boolean isMock() {
        return mock;
    }

    /**
     * Sets mock.
     *
     * @param mock the mock
     * @return the mock
     */
    public AbstractInterfaceConfig setMock(boolean mock) {
        this.mock = mock;
        return this;
    }

    /**
     * Is validation boolean.
     *
     * @return the boolean
     */
    public boolean isValidation() {
        return validation;
    }

    /**
     * Sets validation.
     *
     * @param validation the validation
     * @return the validation
     */
    public AbstractInterfaceConfig setValidation(boolean validation) {
        this.validation = validation;
        return this;
    }

    /**
     * Gets compress.
     *
     * @return the compress
     */
    public String getCompress() {
        return compress;
    }

    /**
     * Sets compress.
     *
     * @param compress the compress
     * @return the compress
     */
    public AbstractInterfaceConfig setCompress(String compress) {
        this.compress = compress;
        return this;
    }

    /**
     * Is cache boolean.
     *
     * @return the boolean
     */
    public boolean isCache() {
        return cache;
    }

    /**
     * Sets cache.
     *
     * @param cache the cache
     * @return the cache
     */
    public AbstractInterfaceConfig setCache(boolean cache) {
        this.cache = cache;
        return this;
    }

    /**
     * Gets config value cache.
     *
     * @return the config value cache
     */
    public Map<String, Object> getConfigValueCache() {
        return configValueCache;
    }

    /**
     * Sets config value cache.
     *
     * @param configValueCache the config value cache
     * @return the config value cache
     */
    public AbstractInterfaceConfig setConfigValueCache(Map<String, Object> configValueCache) {
        this.configValueCache = configValueCache;
        return this;
    }

    /**
     * Sets proxy class.
     *
     * @param proxyClass the proxy class
     * @return the proxy class
     */
    public AbstractInterfaceConfig setProxyClass(Class proxyClass) {
        this.proxyClass = proxyClass;
        return this;
    }

    /**
     * Sets config listener.
     *
     * @param configListener the config listener
     * @return the config listener
     */
    public AbstractInterfaceConfig setConfigListener(ConfigListener configListener) {
        this.configListener = configListener;
        return this;
    }

    /**
     * 得到配置监听器
     *
     * @return 配置监听器 config listener
     */
    public ConfigListener getConfigListener() {
        return configListener;
    }

    /**
     * 是否有超时配置
     *
     * @return 是否配置了timeout
     */
    public abstract boolean hasTimeout();

    /**
     * 是否有并发限制配置
     *
     * @return 是否配置了并发限制
     */
    public abstract boolean hasConcurrents();

    /**
     * 除了判断自己，还有判断下面方法的自定义判断
     *
     * @return the validation
     */
    public boolean hasValidation() {
        if (validation) {
            return true;
        }
        if (CommonUtils.isNotEmpty(methods)) {
            for (MethodConfig methodConfig : methods.values()) {
                if (CommonUtils.isTrue(methodConfig.getValidation())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 是否有缓存
     *
     * @return 是否配置了cache boolean
     */
    public boolean hasCache() {
        if (isCache()) {
            return true;
        }
        if (CommonUtils.isNotEmpty(methods)) {
            for (MethodConfig methodConfig : methods.values()) {
                if (CommonUtils.isTrue(methodConfig.getCache())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 是否有token配置
     *
     * @return 是否配置了token boolean
     */
    public boolean hasToken() {
        if (getParameter(BsoaConstants.HIDDEN_KEY_TOKEN) != null) {
            return true;
        }
        if (CommonUtils.isNotEmpty(methods)) {
            for (MethodConfig methodConfig : methods.values()) {
                if (methodConfig.getParameter(BsoaConstants.HIDDEN_KEY_TOKEN) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Sets methods.
     *
     * @param methods the methods
     */
    public void setMethods(List<MethodConfig> methods) {
        if (this.methods == null) {
            this.methods = new ConcurrentHashMap<String, MethodConfig>();
        }
        if (methods != null) {
            for (MethodConfig methodConfig : methods) {
                this.methods.put(methodConfig.getName(), methodConfig);
            }
        }
    }

    /**
     * 设置注册中心
     *
     * @param registry RegistryConfig
     */
    public void setRegistry(RegistryConfig registry) {
        if (this.registry == null) {
            this.registry = new ArrayList<RegistryConfig>();
        }
        this.registry.add(registry);
    }

    /**
     * 得到方法名对应的方法配置
     *
     * @param methodName 方法名，不支持重载
     * @return method config
     */
    private MethodConfig getMethodConfig(String methodName) {
        if (methods == null) {
            return null;
        }
        return methods.get(methodName);
    }

    /**
     * 接口属性和方法属性加载配置到缓存
     *
     * @param rebuild 是否重建
     * @return Map<String Object> unmodifiableMap
     */
    public synchronized Map<String, Object> getConfigValueCache(boolean rebuild) {
        if (configValueCache != null && !rebuild) {
            return configValueCache;
        }
        Map<String, Object> context = new HashMap<String, Object>();
        Map<String, String> providerParams = getParameters();
        if (providerParams != null) {
            context.putAll(providerParams); // 复制接口的自定义参数
        }
        Map<String, MethodConfig> methodConfigs = getMethods();
        if (CommonUtils.isNotEmpty(methodConfigs)) {
            for (MethodConfig methodConfig : methodConfigs.values()) {
                String prefix = BsoaConstants.HIDE_KEY_PREFIX + methodConfig.getName() + BsoaConstants.HIDE_KEY_PREFIX;
                Map<String, String> methodparam = methodConfig.getParameters();
                if (methodparam != null) { // 复制方法级自定义参数
                    for (Map.Entry<String, String> entry : methodparam.entrySet()) {
                        context.put(prefix + entry.getKey(), entry.getValue());
                    }
                }
                // 复制方法级参数属性
                BeanUtils.copyPropertiesToMap(methodConfig, prefix, context);
            }
        }
        // 复制接口级参数属性
        BeanUtils.copyPropertiesToMap(this, StringUtils.EMPTY, context);
        configValueCache = Collections.unmodifiableMap(context);
        return configValueCache;
    }

    /**
     * 查询属性值
     *
     * @param property 属性
     * @return oldValue 属性值
     */
    protected String queryAttribute(String property) {
        try {
            Object oldValue = null;
            if (property.charAt(0) == BsoaConstants.HIDE_KEY_PREFIX) {
                // 方法级配置 例如.echoStr.timeout
                String methodAndP = property.substring(1);
                int index = methodAndP.indexOf(BsoaConstants.HIDE_KEY_PREFIX);
                if (index <= 0) {
                    throw ExceptionUtils.buildRuntime(22222, property, "", "Unknown query attribute key!");
                }
                String methodName = methodAndP.substring(0, index);
                String methodProperty = methodAndP.substring(index + 1);
                MethodConfig methodConfig = getMethodConfig(methodName);
                if (methodConfig != null) {
                    Method getMethod = ReflectUtils.getPropertyGetterMethod(MethodConfig.class, methodProperty);
                    Class propertyClazz = getMethod.getReturnType(); // 旧值的类型
                    oldValue = BeanUtils.getProperty(methodConfig, methodProperty, propertyClazz);
                }
            } else { // 接口级配置 例如timeout
                // 先通过get方法找到类型
                Method getMethod = ReflectUtils.getPropertyGetterMethod(getClass(), property);
                Class propertyClazz = getMethod.getReturnType(); // 旧值的类型
                // 拿到旧的值
                oldValue = BeanUtils.getProperty(this, property, propertyClazz);
            }
            return oldValue == null ? null : oldValue.toString();
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, "Exception when query attribute, The key is " + property, e);
        }
    }

    /**
     * 覆盖属性，可以检查，或者更新
     *
     * @param property    属性
     * @param newValueStr 要设置的值
     * @param overwrite   是否覆盖 true直接覆盖，false为检查
     * @return 是否有变更 boolean
     */
    protected boolean updateAttribute(String property, String newValueStr, boolean overwrite) {
        try {
            boolean changed = false;
            if (property.charAt(0) == BsoaConstants.HIDE_KEY_PREFIX) {
                // 方法级配置 例如.echoStr.timeout
                String methodAndP = property.substring(1);
                int index = methodAndP.indexOf(BsoaConstants.HIDE_KEY_PREFIX);
                if (index <= 0) {
                    throw ExceptionUtils.buildRuntime(22222, property, newValueStr,
                            "Unknown update attribute key!");
                }
                String methodName = methodAndP.substring(0, index);
                String methodProperty = methodAndP.substring(index + 1);
                MethodConfig methodConfig = getMethodConfig(methodName);
                Method getMethod = ReflectUtils.getPropertyGetterMethod(MethodConfig.class, methodProperty);
                Class propertyClazz = getMethod.getReturnType(); // 旧值的类型
                // 拿到旧的值
                Object oldValue = null;
                Object newValue = CompatibleTypeUtils.convert(newValueStr, propertyClazz);
                if (methodConfig == null) {
                    methodConfig = new MethodConfig();
                    methodConfig.setName(methodName);
                    if (this.methods == null) {
                        this.methods = new ConcurrentHashMap<String, MethodConfig>();
                    }
                    this.methods.put(methodName, methodConfig);
                    changed = true;
                } else {
                    oldValue = BeanUtils.getProperty(methodConfig, methodProperty, propertyClazz);
                    if (oldValue == null) {
                        if (newValueStr != null) {
                            changed = true;
                        }
                    } else {
                        changed = !oldValue.equals(newValue);
                    }
                }
                if (changed && overwrite) {
                    BeanUtils.setProperty(methodConfig, methodProperty, propertyClazz, newValue);// 覆盖属性
                    LOGGER.info("Property \"" + methodName + "." + methodProperty + "\" changed from {} to {}",
                            oldValue, newValueStr);
                }
            } else { // 接口级配置 例如timeout
                // 先通过get方法找到类型
                Method getMethod = ReflectUtils.getPropertyGetterMethod(getClass(), property);
                Class propertyClazz = getMethod.getReturnType(); // 旧值的类型
                // 拿到旧的值
                Object oldValue = BeanUtils.getProperty(this, property, propertyClazz);
                Object newValue = CompatibleTypeUtils.convert(newValueStr, propertyClazz);
                if (oldValue == null) {
                    if (newValueStr != null) {
                        changed = true;
                    }
                } else {
                    changed = !oldValue.equals(newValue);
                }
                if (changed && overwrite) {
                    BeanUtils.setProperty(this, property, propertyClazz, newValue);// 覆盖属性
                    LOGGER.info("Property \"" + property + "\" changed from {} to {}", oldValue, newValueStr);
                }
            }
            return changed;
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, "Exception when update attribute, The key is "
                    + property + " and value is " + newValueStr, e);
        }
    }

    /**
     * 得到方法级配置，找不到则返回默认值
     *
     * @param methodName   方法名
     * @param configKey    配置key，例如参数
     * @param defaultValue 默认值
     * @return 配置值 method config value
     */
    public Object getMethodConfigValue(String methodName, String configKey, Object defaultValue) {
        Object value = getMethodConfigValue(methodName, configKey);
        return value == null ? defaultValue : value;
    }

    /**
     * 得到方法级配置，找不到则返回null
     *
     * @param methodName 方法名
     * @param configKey  配置key，例如参数
     * @return 配置值 method config value
     */
    public Object getMethodConfigValue(String methodName, String configKey) {
        if (configValueCache == null) {
            return null;
        }
        String key = buildmkey(methodName, configKey);
        return configValueCache.get(key);
    }

    /**
     * Buildmkey string.
     *
     * @param methodName the method name
     * @param key        the key
     * @return the string
     */
    private String buildmkey(String methodName, String key) {
        return BsoaConstants.HIDE_KEY_PREFIX + methodName + BsoaConstants.HIDE_KEY_PREFIX + key;
    }

    /**
     * Sets parameter.
     *
     * @param key   the key
     * @param value the value
     */
    public void setParameter(String key, String value) {
        if (parameters == null) {
            parameters = new ConcurrentHashMap<String, String>();
        }
        if (value == null) {
            parameters.remove(key);
        } else {
            parameters.put(key, value);
        }
    }

    /**
     * Gets parameter.
     *
     * @param key the key
     * @return the value
     */
    public String getParameter(String key) {
        return parameters == null ? null : parameters.get(key);
    }
}
