/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bsoa.rpc.context;

import io.bsoa.rpc.bootstrap.ConsumerBootstrap;
import io.bsoa.rpc.bootstrap.ProviderBootstrap;
import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.BsoaVersion;
import io.bsoa.rpc.common.struct.ConcurrentHashSet;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.server.ServerFactory;
import io.bsoa.rpc.transport.ClientTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 *
 * Created by zhangg on 2016/7/14 21:01.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class BsoaContext {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(BsoaContext.class);

    /**
     * 上下文信息，例如instancekey，本机ip等信息
     */
    private final static ConcurrentHashMap context = new ConcurrentHashMap();

    /**
     * 当前进程Id
     */
    public static final String PID = ManagementFactory.getRuntimeMXBean()
            .getName().split("@")[0];

    /**
     * 当前系统启动实际（用这个类加载实际为准）
     */
    public static final long START_TIME = now();

    /**
     * 发布的服务配置
     */
    private final static ConcurrentHashSet<ProviderBootstrap> EXPORTED_PROVIDER_CONFIGS
            = new ConcurrentHashSet<>();

    /**
     * 发布的订阅配置
     */
    private final static ConcurrentHashSet<ConsumerBootstrap> REFERRED_CONSUMER_CONFIGS
            = new ConcurrentHashSet<>();

    /**
     * 是否正在关闭
     */
    public static boolean IS_SHUTTING_DOWN = false;

    static {
        LOGGER.info("Welcome! Loading Beyond SOA RPC Framework : {}", BsoaVersion.BUILD_VERSION);
        put(BsoaConstants.CONFIG_KEY_BSOAVERSION, BsoaVersion.BUILD_VERSION);
        // 初始化一些上下文
        putPropertyToContext();
        // 增加jvm关闭事件
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Beyond SOA RPC Framework catch JVM shutdown event, Run shutdown hook now.");
                destroy(false);
            }
        }, "JSFShutdownHook"));
//        TODO
//        ResourceScheduleChecker.resourceCheck();
    }

    /**
     * 初始化一些上下文
     */
    private static void putPropertyToContext() {
        putIfAbsent(KEY_APPID, BsoaConfigs.getOrDefaultValue("bsoa.appId", null));
        putIfAbsent(KEY_APPNAME, BsoaConfigs.getOrDefaultValue("bsoa.appName", null));
        putIfAbsent(KEY_APPINSID, BsoaConfigs.getOrDefaultValue("bsoa.instanceId", null));
    }

    /**
     * 主动销毁全部JSF运行相关环境
     */
    public static void destroy() {
        destroy(true);
    }

    /**
     * 销毁方法
     *
     * @param active
     *         是否主动销毁
     */
    private static void destroy(boolean active) {
        // TODO 检查是否有其它需要释放的资源
        IS_SHUTTING_DOWN = true;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        // 关闭资源
      /*  ResourceScheduleChecker.close(); */
        // 关闭启动的服务端
        for (ProviderBootstrap bootstrap : EXPORTED_PROVIDER_CONFIGS) {
            bootstrap.unExport();
        }
        // 关闭启动的端口
        ServerFactory.destroyAll();
        /*
        // 关闭启动的分组调用端
        for (ConsumerGroupConfig config : REFERRED_CONSUMER_GROUP_CONFIGS) {
            if (!CommonUtils.isFalse(config.getParameter(BsoaConstants.HIDDEN_KEY_DESTROY))) { // 除非不让主动unrefer
                config.setRegister(config.isRegister() && !JSFLogicSwitch.REGISTRY_REGISTER_BATCH);
                config.unrefer();
            }
        }*/
        // 关闭启动的调用端
        for (ConsumerBootstrap bootstrap : REFERRED_CONSUMER_CONFIGS) {
            ConsumerConfig config = bootstrap.getConsumerConfig();
            if (!CommonUtils.isFalse(config.getParameter(BsoaConstants.HIDDEN_KEY_DESTROY))) { // 除非不让主动unrefer
                bootstrap.unRefer();
            }
        }
//        if (JSFLogicSwitch.REGISTRY_REGISTER_BATCH) { // 批量反注册
//            RegistryFactory.batchUnregister();
//        }
        /*
        // 关闭监控服务
        MonitorFactory.destroyAll();
        // 关闭注册中心
        RegistryFactory.destroyAll();
        //RingBuffer Close
        RingBufferHolder.destroyAll();
        */
        // 关闭客户端的一些公共资源
        ClientTransportFactory.closeAll();
        LOGGER.info("Beyond SOA RPC Framework has been release all resources {}...",
                active ? "actively " : "");
    }

    /**
     * 增加缓存ConsumerConfig
     *
     * @param consumerConfig
     *         the consumer config
     */
    public static void cacheConsumerConfig(ConsumerBootstrap consumerConfig) {
        REFERRED_CONSUMER_CONFIGS.add(consumerConfig);
    }

    /**
     * 缓存的ConsumerConfig失效
     *
     * @param consumerConfig
     *         the consumer config
     */
    public static void invalidateConsumerConfig(ConsumerBootstrap consumerConfig) {
        REFERRED_CONSUMER_CONFIGS.remove(consumerConfig);
    }

    /**
     * 增加缓存ProviderConfig
     *
     * @param providerConfig
     *         the provider config
     */
    public static void cacheProviderConfig(ProviderBootstrap providerConfig) {
        EXPORTED_PROVIDER_CONFIGS.add(providerConfig);
    }

    /**
     * 缓存的ProviderConfig失效
     *
     * @param providerConfig
     *         the provider config
     */
    public static void invalidateProviderConfig(ProviderBootstrap providerConfig) {
        EXPORTED_PROVIDER_CONFIGS.remove(providerConfig);
    }

    /**
     * 得到已发布的全部ProviderConfig
     *
     * @return the provider configs
     */
    public static List<ProviderBootstrap> getProviderConfigs() {
        return new ArrayList<ProviderBootstrap>(EXPORTED_PROVIDER_CONFIGS);
    }

    /**
     * 得到已调用的全部ConsumerConfig
     *
     * @return the consumer configs
     */
    public static List<ConsumerBootstrap> getConsumerConfigs() {
        return new ArrayList<ConsumerBootstrap>(REFERRED_CONSUMER_CONFIGS);
    }

    /**
     * 得到上下文信息
     *
     * @param key
     *         the key
     * @return the object
     * @see ConcurrentHashMap#get(Object)
     */
    public static Object get(String key) {
        return context.get(key);
    }

    /**
     * 设置上下文信息（不存在才设置成功）
     *
     * @param key
     *         the key
     * @param value
     *         the value
     * @return the object
     * @see ConcurrentHashMap#putIfAbsent(Object, Object)
     */
    public static Object putIfAbsent(String key, Object value) {
        return value == null ? context.remove(key) : context.putIfAbsent(key, value);
    }

    /**
     * 设置上下文信息
     *
     * @param key
     *         the key
     * @param value
     *         the value
     * @return the object
     * @see ConcurrentHashMap#put(Object, Object)
     */
    public static Object put(String key, Object value) {
        return value == null ? context.remove(key) : context.put(key, value);
    }

    /**
     * 得到全部上下文信息
     *
     * @return the context
     */
    public static ConcurrentHashMap getContext() {
        return new ConcurrentHashMap(context);
    }

    /**
     * junit测试时，不需要加载其它的
     */
    public static final String KEY_UNIT_TEST = "unitTestMode";

    /**
     * 配置的第一个可用的注册中心地址
     */
    public static final String KEY_REGISTRY_CONFIG = "configedRegistry";

    /**
     * 当前连接的注册中心地址
     */
    public static final String KEY_CONNECTED_REGISTRY = "connectedRegistry";

    /**
     * 最后和注册中心一次心跳时间
     */
    public static final String KEY_LAST_HEARTBEAT_TIME = "lastHeartbeatTime";
    /**
     * 当前所在文件夹地址
     */
    public static final String KEY_APPAPTH = "appPath";

    /**
     * 当前实例在注册中心的关键字
     */
    public static final String KEY_INSTANCEKEY = "instanceKey";

    /**
     * debug模式开启后，会打印一些额外的调试日志，不过还是受slf4j的日志级别限制
     */
    public static final String KEY_DEBUG_MODE = "debugMode";

    /**
     * 自动部署的appId
     */
    public static final String KEY_APPID = "appId";

    /**
     * 自动部署的appName
     */
    public static final String KEY_APPNAME = "appName";

    /**
     * 自动部署的appInsId
     */
    public static final String KEY_APPINSID = "appInsId";

    /**
     * 接口配置map<接口名，<key,value>>
     */
    public static final ConcurrentMap<String, Map<String, String>> interfaceConfigMap
            = new ConcurrentHashMap<String, Map<String, String>>();

    /**
     * 获取全局参数
     *
     * @param key
     *         the key
     * @param defaultVal
     *         the default val
     * @return the global val
     */
    public static String getGlobalVal(String key, String defaultVal) {
        return getInterfaceVal(BsoaConstants.GLOBAL_SETTING, key, defaultVal);
    }

    /**
     * 设置全局参数
     *
     * @param key
     *         the key
     * @param value
     *         the value
     */
    public static void putGlobalVal(String key, String value) {
        putInterfaceVal(BsoaConstants.GLOBAL_SETTING, key, value);
    }

    /**
     * 获取接口参数
     *
     * @param interfaceId
     *         the interface id
     * @param key
     *         the key
     * @param defaultVal
     *         the default val
     * @return the interface val
     */
    public static String getInterfaceVal(String interfaceId, String key, String defaultVal) {
        Map<String, String> map = interfaceConfigMap.get(interfaceId);
        if (map == null) {
            map = CommonUtils.putToConcurrentMap(interfaceConfigMap,
                    interfaceId, new ConcurrentHashMap<String, String>());
        }
        String val = map.get(key);
        return val == null ? defaultVal : val;
    }

    /**
     * 设置接口参数
     *
     * @param interfaceId
     *         the interface id
     * @param key
     *         the key
     * @param value
     *         the value
     */
    public static void putInterfaceVal(String interfaceId, String key, String value) {
        if (value != null) {
            Map<String, String> map = interfaceConfigMap.get(interfaceId);
            if (map == null) {
                map = CommonUtils.putToConcurrentMap(interfaceConfigMap,
                        interfaceId, new ConcurrentHashMap<String, String>());
            }
            map.put(key, value);
        }
    }


    /**
     * 得到全部接口下的全部参数
     *
     * @return the config map
     */
    public static Map<String, Map<String, String>> getConfigMaps() {
        return Collections.unmodifiableMap(interfaceConfigMap);
    }

    /**
     * 获取接口全部参数
     *
     * @param interfaceId
     *         the interface id
     * @return the config map
     */
    public static Map<String, String> getConfigMap(String interfaceId) {
        return interfaceConfigMap.get(interfaceId);
    }

    /**
     * {ifaceId:className}<br>
     * 用于接口id映射实际类名
     */
    private static ConcurrentHashMap<String, String> ifaceIdClassNameMap;

    /**
     * {className:ifaceId}<br>
     * 用于接口id映射实际类名
     */
    private static ConcurrentHashMap<String, String> classNameIfaceIdMap;

    /**
     * 通过注册中心接口id查询类名
     *
     * @param ifaceId
     *         注册中心接口id
     * @return 类名
     */
    public static String getClassNameByIfaceId(String ifaceId) {
        return ifaceIdClassNameMap == null ? null : (ifaceId == null ? null : ifaceIdClassNameMap.get(ifaceId));
    }

    /**
     * 通过类名查询注册中心接口id
     *
     * @param className
     *         类名
     * @return 注册中心接口id
     */
    public static String getIfaceIdByClassName(String className) {
        return classNameIfaceIdMap == null ? null : className == null ? null : classNameIfaceIdMap.get(className);
    }

    /**
     * 注册中心接口id和实际类名映射
     *
     * @param className
     *         实际类名
     * @param ifaceId
     *         注册中心接口id
     */
    public static synchronized void cacheClassNameAndIfaceId(String className, String ifaceId) {
        if (ifaceIdClassNameMap == null) {
            ifaceIdClassNameMap = new ConcurrentHashMap<String, String>();
        }
        if (classNameIfaceIdMap == null) {
            classNameIfaceIdMap = new ConcurrentHashMap<String, String>();
        }
        String oldClassName = getClassNameByIfaceId(ifaceId);
        if (oldClassName != null && !oldClassName.equals(className)) {
            LOGGER.warn("IfaceIdClassNameMap contains the value with same ifaceId \"" + ifaceId +
                    "\" but different class name : " + oldClassName + " != " + className +
                    ", may be connected to wrong registry?");
        }
        String oldIfaceId = getIfaceIdByClassName(className);
        if (oldIfaceId != null && !oldIfaceId.equals(ifaceId)) {
            LOGGER.warn("ClassNameIfaceIdMap contains the value with same class name \"" + className +
                    "\" but different ifaceId : " + oldIfaceId + " != " + ifaceId +
                    ", may be connected to wrong registry?");
        }

        if (oldClassName != null && oldIfaceId == null) { // 名字一样，id不一样
            ifaceIdClassNameMap.put(ifaceId, className);
            classNameIfaceIdMap.remove(oldClassName);
            classNameIfaceIdMap.putIfAbsent(className, ifaceId);
        } else if (oldClassName == null & oldIfaceId != null) { // id一样 名字不一样
            ifaceIdClassNameMap.remove(oldIfaceId);
            ifaceIdClassNameMap.put(ifaceId, className);
            classNameIfaceIdMap.put(className, ifaceId);
        } else if (oldClassName != null & oldIfaceId != null) {  // 相同？
            ifaceIdClassNameMap.remove(oldIfaceId);
            classNameIfaceIdMap.remove(oldClassName);
            ifaceIdClassNameMap.put(ifaceId, className);
            classNameIfaceIdMap.put(className, ifaceId);
        } else { // 未存在
            ifaceIdClassNameMap.put(ifaceId, className);
            classNameIfaceIdMap.put(className, ifaceId);
        }
    }

    /**
     * 得到接口名和id映射列表
     *
     * @return 接口名和id映射列表
     */
    public static ConcurrentHashMap<String, String> getClassNameIfaceIdMap() {
        return classNameIfaceIdMap == null ? new ConcurrentHashMap<String, String>() : classNameIfaceIdMap;
    }

    public static long now() {
        return System.currentTimeMillis();
    }
}
