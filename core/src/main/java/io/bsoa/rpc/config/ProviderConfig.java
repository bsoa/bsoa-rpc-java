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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.struct.ConcurrentHashSet;
import io.bsoa.rpc.common.utils.ClassLoaderUtils;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.ReflectUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.listener.ConfigListener;
import io.bsoa.rpc.registry.Registry;
import io.bsoa.rpc.registry.RegistryFactory;
import io.bsoa.rpc.server.ProviderProxyInvoker;
import io.bsoa.rpc.server.Server;
import io.bsoa.rpc.server.ServerFactory;

/**
 * Created by zhanggeng on 16-7-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class ProviderConfig<T> extends AbstractInterfaceConfig implements Serializable {

    /**
     * The constant serialVersionUID.
     */
    private static final long serialVersionUID = 6333907358205293883L;

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
    protected int delay = BsoaConstants.DEFAULT_PROVIDER_DELAY;

    /**
     * 权重
     */
    protected int weight = BsoaConstants.DEFAULT_PROVIDER_WEIGHT;

    /**
     * 包含的方法
     */
    protected String include = "*";

    /**
     * 不发布的方法列表，逗号分隔
     */
    protected String exclude;

    /**
     * 是否动态注册，默认为true，配置为false代表不主动发布，需要到管理端进行上线操作
     */
    protected boolean dynamic = true;

    /**
     * 服务优先级，越大越高
     */
    protected int priority = BsoaConstants.DEFAULT_METHOD_PRIORITY;

    /**
     * whitelist blacklist
     */

    /*-------- 下面是方法级配置 --------*/

    /**
     * 接口下每方法的最大可并行执行请求数，配置-1关闭并发过滤器，等于0表示开启过滤但是不限制
     */
    protected int concurrents = 0;

    /*---------- 参数配置项结束 ------------*/

    /**
     * 是否已发布
     */
    protected transient volatile boolean exported;

    /**
     * 方法名称：是否可调用
     */
    protected transient volatile ConcurrentHashMap<String, Boolean> methodsLimit;

    /**
     * 发布的服务配置
     */
    private final static ConcurrentHashSet<String> EXPORTED_KEYS
            = new ConcurrentHashSet<String>();

    /**
     * 发布服务，有延迟加载
     * @throws BsoaRuntimeException the init error exception
     */
    public synchronized void export() throws BsoaRuntimeException {
        if (delay > 0) { // 延迟加载,单位毫秒
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(delay);
                    } catch (Throwable e) {
                    }
                    doExport();
                }
            });
            thread.setDaemon(true);
            thread.setName("DelayExportThread");
            thread.start();
        } else {
            doExport();
        }
    }

    /**
     * 发布服务
     * @throws BsoaRuntimeException the init error exception
     */
    private synchronized void doExport() throws BsoaRuntimeException {
        if (exported) {
            return;
        }
        String key = buildKey();

        // 检查参数
        // alias不能为空
        if (StringUtils.isBlank(alias)) {
            throw ExceptionUtils.buildRuntime(22222, "GROUP", "NULL", "[JSF-21200]Value of \"GROUP\" is not specified in provider" +
                    " config with key " + key + " !");
        }
        // 检查注入的ref是否接口实现类
        if (!getProxyClass().isInstance(ref)) {
            throw ExceptionUtils.buildRuntime(22222, "provider.ref", ref.getClass().getName(),
                    "This is not an instance of " + interfaceId
                            + " in provider config with key " + key + " !");
        }
        // server 不能为空
        List<ServerConfig> serverConfigs = getServer();
        if (CommonUtils.isEmpty(serverConfigs)) {
            throw ExceptionUtils.buildRuntime(22222, "server", "NULL", "[JSF-21202]Value of \"server\" is not specified in provider" +
                    " config with key " + key + " !");
        }

        LOGGER.info("Export provider config : {} with bean id {}", key, getId());
        if (EXPORTED_KEYS.contains(key)) {
            // 注意同一interface，同一alias，不同server情况
            throw new BsoaRuntimeException(21203, "Duplicate provider config with key " + key + " has been exported!");
        }

        // 检查多态（重载）方法
        checkOverloadingMethod(getProxyClass());

        // 检查是否有回调函数
//        CallbackUtil.autoRegisterCallBack(getProxyClass());

        // 构造请求调用器
        ProviderProxyInvoker invoker = new ProviderProxyInvoker(this);

        // 初始化注册中心
        if (isRegister()) {
            List<RegistryConfig> registryConfigs = super.getRegistry();
            if (CommonUtils.isEmpty(registryConfigs)) { // registry为空
                // 走默认的注册中心
                LOGGER.debug("Registry is undefined, will use default registry config instead");
                registryConfigs = new ArrayList<RegistryConfig>();
                registryConfigs.add(RegistryFactory.defaultConfig());
                setRegistry(registryConfigs); // 注入进去
            }
            for (RegistryConfig registryConfig : registryConfigs) {
                RegistryFactory.getRegistry(registryConfig); // 提前初始化Registry
            }
        }

        // 将处理器注册到server
        if (serverConfigs != null) {
            for (ServerConfig serverConfig : serverConfigs) {
                try {
                    serverConfig.start();
                    Server server = serverConfig.getServer();
                    // 注册序列化接口
//                    CodecUtils.registryService(serverConfig.getSerialization(), getProxyClass());
                    server.registerProcessor(this, invoker);
                } catch (BsoaRuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    LOGGER.error("[JSF-21204]Catch exception when register processor to server: "
                            + serverConfig.getId(), e);
                }
            }
        }

        // 注册到注册中心
        configListener = new ProviderAttributeListener();
        register();

        // 记录一些缓存数据
        EXPORTED_KEYS.add(key);
        BsoaContext.cacheProviderConfig(this);
        exported = true;
    }

    private void checkOverloadingMethod(Class<?> itfClass) {
        methodsLimit = new ConcurrentHashMap<String, Boolean>();
        for (Method method : itfClass.getMethods()) {
            String methodName = method.getName();
            if (methodsLimit.containsKey(methodName)) {
                // 重名的方法
                //LOGGER.warn("Method with same name \"{}\" exists ! The usage of " +
                //        "overloading method is deprecated.", methodName);
                throw new BsoaRuntimeException(21205, "[JSF-21205]Method with same name \"" + itfClass.getName() + "."
                        + methodName + "\" exists ! The usage of overloading method is deprecated.");
            }
            // 判断服务下方法的黑白名单
            Boolean include = methodsLimit.get(methodName);
            if (include == null) {
                include = inList(getInclude(), getExclude(), methodName); // 检查是否在黑白名单中
                methodsLimit.putIfAbsent(methodName, include);
            }
            ReflectUtils.cacheMethodArgsType(interfaceId, methodName, method.getParameterTypes());
        }
    }

    /**
     * 取消发布（从server里取消注册）
     */
    public synchronized void unexport() {
        if (!exported) {
            return;
        }
        String key = buildKey();
        LOGGER.info("Unexport provider config : {} {}", key, getId() != null ? "with bean id " + getId() : "");
        // 取消将处理器注册到server
        List<ServerConfig> serverConfigs = getServer();
        if (serverConfigs != null) {
            for (ServerConfig serverConfig : serverConfigs) {
                Server server = ServerFactory.getServer(serverConfig);
                try {
                    server.unRegisterProcessor(this, true);
                } catch (Exception e) {
                    LOGGER.warn("Catch exception when unregister processor to server: " + serverConfig.getId()
                            + ", but you can ignore if it's called by JVM shutdown hook", e);
                }
            }
        }

        // 取消注册到注册中心
        unregister();
        configListener = null;

        // 清除缓存状态
        EXPORTED_KEYS.remove(key);
        BsoaContext.invalidateProviderConfig(this);
//        RpcStatus.removeStatus(this); TODO
        exported = false;
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
     * 订阅服务列表
     *
     * @return 当前服务列表
     */
    protected void register() {
        if (isRegister()) {
            List<RegistryConfig> registryConfigs = super.getRegistry();
            if (registryConfigs != null) {
//                boolean crossLang = CodecUtils.isSupportCrossLang(getProxyClass());
//                super.setParameter(BsoaConstants.CONFIG_KEY_CROSSLANG, crossLang + "");
                for (RegistryConfig registryConfig : registryConfigs) {
                    Registry registry = RegistryFactory.getRegistry(registryConfig);
                    try {
                        registry.register(this, configListener);
                    } catch (BsoaRuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        LOGGER.warn("Catch exception when register to registry: "
                                + registryConfig.getId(), e);
                    }
                }
                super.setParameter(BsoaConstants.CONFIG_KEY_CROSSLANG, null);
            }
        }
    }

    /**
     * 取消订阅服务列表
     */
    protected void unregister() {
        if (isRegister()) {
            List<RegistryConfig> registryConfigs = super.getRegistry();
            if (registryConfigs != null) {
                for (RegistryConfig registryConfig : registryConfigs) {
                    Registry registry = RegistryFactory.getRegistry(registryConfig);
                    try {
                        registry.unregister(this);
                    } catch (Exception e) {
                        LOGGER.warn("Catch exception when unregister from registry: " + registryConfig.getId()
                                + ", but you can ignore if it's called by JVM shutdown hook", e);
                    }
                }
            }
        }
    }

    /**
     * Provider配置发生变化监听器
     */
    private class ProviderAttributeListener implements ConfigListener {

        public void configChanged(Map newValue) {
        }

        public synchronized void attrUpdated(Map newValueMap) {
            // 可以改变的配置 例如alias concurrents等
            Map<String, String> newValues = (Map<String, String>) newValueMap;
            Map<String, String> oldValues = new HashMap<String, String>();
            boolean reexport = false;

            // TODO 一些ServerConfig的配置 怎么处理？
            try { // 检查是否有变化
                // 是否过滤map?
                for (Map.Entry<String, String> entry : newValues.entrySet()) {
                    String newValue = entry.getValue();
                    String oldValue = queryAttribute(entry.getKey());
                    boolean changed = oldValue == null ? newValue != null : !oldValue.equals(newValue);
                    if (changed) {
                        oldValues.put(entry.getKey(), oldValue);
                    }
                    reexport = reexport || changed;
                }
            } catch (Exception e) {
                LOGGER.error("Catch exception when provider attribute compare", e);
                return;
            }

            // 需要重新发布
            if (reexport) {
                try {
                    LOGGER.info("Reexport service {}", buildKey());
                    unexport();
                    // change attrs
                    for (Map.Entry<String, String> entry : newValues.entrySet()) {
                        updateAttribute(entry.getKey(), entry.getValue(), true);
                    }
                    export();
                } catch (Exception e) {
                    LOGGER.error("Catch exception when provider attribute changed", e);
                    //rollback old attrs
                    for (Map.Entry<String, String> entry : oldValues.entrySet()) {
                        updateAttribute(entry.getKey(), entry.getValue(), true);
                    }
                    export();
                }
            }

        }
    }

    /**
     * Build key.
     *
     * @return the string
     */
    @Override
    public String buildKey() {
        return interfaceId + ":" + alias;
    }

    /**
     * Is register.
     *
     * @return the boolean
     */
    @Override
    public boolean isRegister() {
        return register;
    }

    /**
     * Sets register.
     *
     * @param register the register
     */
    @Override
    public void setRegister(boolean register) {
        this.register = register;
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
     * @param concurrents
     *         the concurrents
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
     * 得到已发布的全部list
     *
     * @return urls urls
     */
    public List<String> buildUrls() {
        if (exported) {
            List<ServerConfig> servers = getServer();
            if (servers != null && !servers.isEmpty()) {
                List<String> urls = new ArrayList<String>();
                for (ServerConfig server : servers) {
                    StringBuilder sb = new StringBuilder(200);
                    sb.append(server.getProtocol()).append("://").append(server.getHost())
                            .append(":").append(server.getPort()).append(server.getContextpath())
                            .append(getInterfaceId()).append("?GROUP=").append(getAlias())
                            .append(getKeyPairs("delay", getDelay()))
                            .append(getKeyPairs("weight", getWeight()))
                            .append(getKeyPairs("register", isRegister()))
                            .append(getKeyPairs("threads", server.getThreads()))
                            .append(getKeyPairs("iothreads", server.getIothreads()))
                            .append(getKeyPairs("threadpool", server.getThreadpool()))
                            .append(getKeyPairs("accepts", server.getAccepts()))
                            .append(getKeyPairs("dynamic", isDynamic()))
                            .append(getKeyPairs("debug", server.isDebug()))
                            .append(getKeyPairs(BsoaConstants.CONFIG_KEY_JSFVERSION, BsoaConstants.JSF_VERSION));
                    urls.add(sb.toString());
                }
                return urls;
            }
        }
        return null;
    }

    /**
     * Gets key pairs.
     *
     * @param key the key
     * @param value the value
     * @return the key pairs
     */
    private String getKeyPairs(String key, Object value) {
        if (value != null) {
            return "&" + key + "=" + value.toString();
        } else {
            return "";
        }
    }

    /**
     * 得到可调用的方法名称列表
     *
     * @return 可调用的方法名称列表
     */
    public Map<String, Boolean> getMethodsLimit() {
        return methodsLimit;
    }

    /**
     * 接口可以按方法发布
     *
     * @param includeMethods
     *         包含的方法列表
     * @param excludeMethods
     *         不包含的方法列表
     * @param methodName
     *         方法名
     * @return 方法
     */
    private boolean inList(String includeMethods,String excludeMethods, String methodName) {
        //判断是否在白名单中
        if (includeMethods != null && !"*".equals(includeMethods)) {
            includeMethods = includeMethods + ",";
            boolean inwhite = includeMethods.indexOf(methodName+",") >= 0;
            if (!inwhite) {
                return false;
            }
        }
        //判断是否在黑白单中
        if (StringUtils.isBlank(excludeMethods)) {
            return true;
        } else {
            excludeMethods = excludeMethods + ",";
            boolean inblack = excludeMethods.indexOf(methodName+",") >= 0;
            return !inblack;
        }
    }
}
