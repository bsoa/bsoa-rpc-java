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
package io.bsoa.rpc.registry;

import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.config.RegistryConfig;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.ExtensionClass;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zhangg on 2016/7/16 00:30.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class RegistryFactory {

    /**
     * 保存全部的配置
     */
    private final static ConcurrentHashMap<RegistryConfig, Registry> CLIENTREGISTRY_MAP
            = new ConcurrentHashMap<RegistryConfig, Registry>();
    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(RegistryFactory.class);

    /**
     * 得到注册中心对象
     *
     * @param registryConfig RegistryConfig类
     * @return Registry实现
     */
    public static synchronized Registry getRegistry(RegistryConfig registryConfig) {
        if (CLIENTREGISTRY_MAP.size() > 3) { // 超过3次 是不是配错了？
            LOGGER.warn("Size of registry is greater than 3, Please check it!");
        }
        try {
            // 注意：RegistryConfig重写了equals方法，如果多个RegistryConfig属性一样，则认为是一个对象
            Registry registry = CLIENTREGISTRY_MAP.get(registryConfig);
            if (registry == null) {
                ExtensionClass<Registry> ext = ExtensionLoaderFactory.getExtensionLoader(Registry.class)
                        .getExtensionClass(registryConfig.getProtocol());
                if (ext == null) {
                    throw ExceptionUtils.buildRuntime(22222, "registry.protocol", registryConfig.getProtocol(),
                            "Unsupported protocol of registry config !");
                }
                registry = ext.getExtInstance(new Class[]{RegistryConfig.class}, new Object[]{registryConfig});
                CLIENTREGISTRY_MAP.putIfAbsent(registryConfig, registry);
            }
            return registry;
        } catch (BsoaRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, e.getMessage(), e);
        }
    }

    /**
     * 得到全部注册中心配置
     *
     * @return 注册中心配置
     */
    public static List<RegistryConfig> getRegistryConfigs() {
        return new ArrayList<RegistryConfig>(CLIENTREGISTRY_MAP.keySet());
    }

    /**
     * 得到全部注册中心
     *
     * @return 注册中心
     */
    public static List<Registry> getRegistries() {
        return new ArrayList<Registry>(CLIENTREGISTRY_MAP.values());
    }

    /**
     * 关闭全部注册中心
     */
    public static void destroyAll() {
        for (Map.Entry<RegistryConfig, Registry> entry : CLIENTREGISTRY_MAP.entrySet()) {
            RegistryConfig config = entry.getKey();
            Registry registry = entry.getValue();
            try {
                registry.destroy();
                CLIENTREGISTRY_MAP.remove(config);
            } catch (Exception e) {
                LOGGER.error("Error when destroy registry :" + config
                        + ", but you can ignore if it's called by JVM shutdown hook", e);
            }
        }
    }

    /**
     * 默认的注册中心配置
     */
    private volatile static RegistryConfig defaultRegistryConfig;

    /**
     * 构建默认的注册中心配置
     *
     * @return 配置中心RegistryConfig
     */
    public static RegistryConfig defaultConfig() {
        if (defaultRegistryConfig == null) {
            synchronized (RegistryFactory.class) {
                if (defaultRegistryConfig == null) {
                    RegistryConfig config = new RegistryConfig();
                    config.setParameter("_default", "true");
                    RegistryFactory.getRegistry(config); // 生成一个默认的注册中心
                    defaultRegistryConfig = config;
                }
            }
        }
        return defaultRegistryConfig;
    }

    /**
     * 批量反注册，目前只支持JSFRegistry
     */
    public static void batchUnregister() {
        for (Registry clientRegistry : CLIENTREGISTRY_MAP.values()) {
//            clientRegistry.batchUnregister();
//            }
        }
    }
}
