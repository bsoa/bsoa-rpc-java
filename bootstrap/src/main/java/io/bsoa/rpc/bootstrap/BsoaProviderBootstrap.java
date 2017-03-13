/*
 * Copyright © 2016-2017 The BSOA Project
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
package io.bsoa.rpc.bootstrap;

import io.bsoa.rpc.base.Invoker;
import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.BsoaVersion;
import io.bsoa.rpc.common.struct.ConcurrentHashSet;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.common.utils.ReflectUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.config.RegistryConfig;
import io.bsoa.rpc.config.ServerConfig;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.invoke.CallbackUtils;
import io.bsoa.rpc.invoke.StreamUtils;
import io.bsoa.rpc.listener.ConfigListener;
import io.bsoa.rpc.registry.Registry;
import io.bsoa.rpc.registry.RegistryFactory;
import io.bsoa.rpc.server.InvokerHolder;
import io.bsoa.rpc.server.ProviderProxyInvoker;
import io.bsoa.rpc.server.Server;
import io.bsoa.rpc.server.ServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/2/8 23:03. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("bsoa")
public class BsoaProviderBootstrap<T> extends ProviderBootstrap<T> {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(BsoaProviderBootstrap.class);

    /**
     * 构造函数
     *
     * @param providerConfig 服务发布者配置
     */
    protected BsoaProviderBootstrap(ProviderConfig<T> providerConfig) {
        super(providerConfig);
    }

    /**
     * 是否已发布
     */
    protected transient volatile boolean exported;

    /**
     * 服务端Invoker对象
     */
    protected transient Invoker providerProxyInvoker;

    /**
     * 发布的服务配置
     */
    private final static ConcurrentHashSet<String> EXPORTED_KEYS
            = new ConcurrentHashSet<String>();

    @Override
    public void export() {
        if (providerConfig.getDelay() > 0) { // 延迟加载,单位毫秒
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(providerConfig.getDelay());
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

    private void doExport() {
        if (exported) {
            return;
        }
        String key = providerConfig.buildKey();

        // 检查参数
        // tag不能为空
        if (StringUtils.isBlank(providerConfig.getTags())) {
            throw ExceptionUtils.buildRuntime(22222, "tags", "NULL", "[21200]Value of \"tags\" is not specified in provider" +
                    " config with key " + key + " !");
        }
        // 检查注入的ref是否接口实现类
        T ref = providerConfig.getRef();
        Class proxyClass = providerConfig.getProxyClass();
        if (!proxyClass.isInstance(ref)) {
            throw ExceptionUtils.buildRuntime(22222, "provider.ref",
                    ref == null ? "null" : ref.getClass().getName(),
                    "This is not an instance of " + providerConfig.getInterfaceId()
                            + " in provider config with key " + key + " !");
        }
        // server 不能为空
        List<ServerConfig> serverConfigs = providerConfig.getServer();
        if (CommonUtils.isEmpty(serverConfigs)) {
            throw ExceptionUtils.buildRuntime(22222, "server", "NULL", "[21202]Value of \"server\" is not specified in provider" +
                    " config with key " + key + " !");
        }

        LOGGER.info("Export provider config : {} with bean id {}", key, providerConfig.getId());
        if (EXPORTED_KEYS.contains(key)) {
            // 注意同一interface，同一tag，不同server情况
            throw new BsoaRuntimeException(21203, "Duplicate provider config with key " + key + " has been exported!");
        }

        // 检查多态（重载）方法
        checkOverloadingMethod(proxyClass);

        // 检查是否有回调函数
        CallbackUtils.scanAndRegisterCallBack(proxyClass);
        // 检查是否有流式调用函数
        StreamUtils.scanAndRegisterStream(proxyClass);

        // 构造请求调用器
        providerProxyInvoker = new ProviderProxyInvoker(providerConfig);
        // 记录到本地
        InvokerHolder.setInvoker(InvokerHolder.buildKey(providerConfig), providerProxyInvoker);

        // 初始化注册中心
        if (providerConfig.isRegister()) {
            List<RegistryConfig> registryConfigs = providerConfig.getRegistry();
            if (CommonUtils.isEmpty(registryConfigs)) { // registry为空
                // 走默认的注册中心
                LOGGER.debug("Registry is undefined, will use default registry config instead");
                registryConfigs = new ArrayList<RegistryConfig>();
                registryConfigs.add(RegistryFactory.defaultConfig());
                providerConfig.setRegistry(registryConfigs); // 注入进去
            }
            for (RegistryConfig registryConfig : registryConfigs) {
                RegistryFactory.getRegistry(registryConfig); // 提前初始化Registry
            }
        }

        // 将处理器注册到server
        for (ServerConfig serverConfig : serverConfigs) {
            try {
                serverConfig.start();
                Server server = serverConfig.getServer();
                // 注册序列化接口
//                    CodecUtils.registryService(serverConfig.getSerialization(), getProxyClass());
                server.registerProcessor(providerConfig, providerProxyInvoker);
            } catch (BsoaRuntimeException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error("[21204]Catch exception when register processor to server: "
                        + serverConfig.getId(), e);
            }
        }

        // 注册到注册中心
        providerConfig.setConfigListener(new ProviderAttributeListener());
        register();

        // 记录一些缓存数据
        EXPORTED_KEYS.add(key);
        BsoaContext.cacheProviderConfig(this);
        exported = true;
    }

    private void checkOverloadingMethod(Class<?> itfClass) {
        ConcurrentHashMap<String, Boolean> methodsLimit = new ConcurrentHashMap<>();
        for (Method method : itfClass.getMethods()) {
            String methodName = method.getName();
            if (methodsLimit.containsKey(methodName)) {
                // 重名的方法
                //LOGGER.warn("Method with same name \"{}\" exists ! The usage of " +
                //        "overloading method is deprecated.", methodName);
                throw new BsoaRuntimeException(21205, "[21205]Method with same name \"" + itfClass.getName() + "."
                        + methodName + "\" exists ! The usage of overloading method is deprecated.");
            }
            // 判断服务下方法的黑白名单
            Boolean include = methodsLimit.get(methodName);
            if (include == null) {
                include = inList(providerConfig.getInclude(), providerConfig.getExclude(), methodName); // 检查是否在黑白名单中
                methodsLimit.putIfAbsent(methodName, include);
            }
            ReflectUtils.cacheMethodArgsType(providerConfig.getInterfaceId(), methodName, method.getParameterTypes());
            providerConfig.setMethodsLimit(methodsLimit);
        }
    }

    /**
     * 取消发布（从server里取消注册）
     */
    @Override
    public synchronized void unExport() {
        if (!exported) {
            return;
        }
        String key = providerConfig.buildKey();
        LOGGER.info("Unexport provider config : {} {}", key, providerConfig.getId() != null
                ? "with bean id " + providerConfig.getId() : "");

        // 取消注册到本地
        InvokerHolder.removeInvoker(InvokerHolder.buildKey(providerConfig));
        providerProxyInvoker = null;

        // 取消将处理器注册到server
        List<ServerConfig> serverConfigs = providerConfig.getServer();
        if (serverConfigs != null) {
            for (ServerConfig serverConfig : serverConfigs) {
                Server server = ServerFactory.getServer(serverConfig);
                try {
                    server.unRegisterProcessor(providerConfig, true);
                } catch (Exception e) {
                    LOGGER.warn("Catch exception when unRegister processor to server: " + serverConfig.getId()
                            + ", but you can ignore if it's called by JVM shutdown hook", e);
                }
            }
        }

        // 取消注册到注册中心
        unregister();
        providerConfig.setConfigListener(null);

        // 清除缓存状态
        EXPORTED_KEYS.remove(key);
        BsoaContext.invalidateProviderConfig(this);
//        RpcStatus.removeStatus(this); TODO
        exported = false;
    }


    /**
     * 订阅服务列表
     *
     * @return 当前服务列表
     */
    protected void register() {
        if (providerConfig.isRegister()) {
            List<RegistryConfig> registryConfigs = providerConfig.getRegistry();
            if (registryConfigs != null) {
//                boolean crossLang = CodecUtils.isSupportCrossLang(getProxyClass());
//                super.setParameter(BsoaConstants.CONFIG_KEY_CROSSLANG, crossLang + "");
                for (RegistryConfig registryConfig : registryConfigs) {
                    Registry registry = RegistryFactory.getRegistry(registryConfig);
                    try {
                        registry.register(providerConfig, providerConfig.getConfigListener());
                    } catch (BsoaRuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        LOGGER.warn("Catch exception when register to registry: "
                                + registryConfig.getId(), e);
                    }
                }
                providerConfig.setParameter(BsoaConstants.CONFIG_KEY_CROSSLANG, null);
            }
        }
    }

    /**
     * 取消订阅服务列表
     */
    protected void unregister() {
        if (providerConfig.isRegister()) {
            List<RegistryConfig> registryConfigs = providerConfig.getRegistry();
            if (registryConfigs != null) {
                for (RegistryConfig registryConfig : registryConfigs) {
                    Registry registry = RegistryFactory.getRegistry(registryConfig);
                    try {
                        registry.unRegister(providerConfig);
                    } catch (Exception e) {
                        LOGGER.warn("Catch exception when unRegister from registry: " + registryConfig.getId()
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
            // 可以改变的配置 例如tag concurrents等
            Map<String, String> newValues = (Map<String, String>) newValueMap;
            Map<String, String> oldValues = new HashMap<String, String>();
            boolean reexport = false;

            // TODO 一些ServerConfig的配置 怎么处理？
            try { // 检查是否有变化
                // 是否过滤map?
                for (Map.Entry<String, String> entry : newValues.entrySet()) {
                    String newValue = entry.getValue();
                    String oldValue = providerConfig.queryAttribute(entry.getKey());
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
                    LOGGER.info("Reexport service {}", providerConfig.buildKey());
                    unExport();
                    // change attrs
                    for (Map.Entry<String, String> entry : newValues.entrySet()) {
                        providerConfig.updateAttribute(entry.getKey(), entry.getValue(), true);
                    }
                    export();
                } catch (Exception e) {
                    LOGGER.error("Catch exception when provider attribute changed", e);
                    //rollback old attrs
                    for (Map.Entry<String, String> entry : oldValues.entrySet()) {
                        providerConfig.updateAttribute(entry.getKey(), entry.getValue(), true);
                    }
                    export();
                }
            }

        }
    }


    /**
     * 得到已发布的全部list
     *
     * @return urls urls
     */
    public List<String> buildUrls() {
        if (exported) {
            List<ServerConfig> servers = providerConfig.getServer();
            if (servers != null && !servers.isEmpty()) {
                List<String> urls = new ArrayList<String>();
                for (ServerConfig server : servers) {
                    StringBuilder sb = new StringBuilder(200);
                    sb.append(server.getProtocol()).append("://").append(server.getHost())
                            .append(":").append(server.getPort()).append(server.getContextPath())
                            .append(providerConfig.getInterfaceId())
                            .append("?tags=").append(providerConfig.getTags())
                            .append(getKeyPairs("delay", providerConfig.getDelay()))
                            .append(getKeyPairs("weight", providerConfig.getWeight()))
                            .append(getKeyPairs("register", providerConfig.isRegister()))
                            .append(getKeyPairs("maxThreads", server.getMaxThreads()))
                            .append(getKeyPairs("ioThreads", server.getIoThreads()))
                            .append(getKeyPairs("threadPoolType", server.getThreadPoolType()))
                            .append(getKeyPairs("accepts", server.getAccepts()))
                            .append(getKeyPairs("dynamic", providerConfig.isDynamic()))
                            .append(getKeyPairs(BsoaConstants.CONFIG_KEY_BSOAVERSION, BsoaVersion.BSOA_VERSION));
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
     * @param key   the key
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
     * 接口可以按方法发布
     *
     * @param includeMethods 包含的方法列表
     * @param excludeMethods 不包含的方法列表
     * @param methodName     方法名
     * @return 方法
     */
    private boolean inList(String includeMethods, String excludeMethods, String methodName) {
        //判断是否在白名单中
        if (includeMethods != null && !"*".equals(includeMethods)) {
            includeMethods = includeMethods + ",";
            boolean inwhite = includeMethods.indexOf(methodName + ",") >= 0;
            if (!inwhite) {
                return false;
            }
        }
        //判断是否在黑白单中
        if (StringUtils.isBlank(excludeMethods)) {
            return true;
        } else {
            excludeMethods = excludeMethods + ",";
            boolean inblack = excludeMethods.indexOf(methodName + ",") >= 0;
            return !inblack;
        }
    }
}
