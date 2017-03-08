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
import io.bsoa.rpc.client.Client;
import io.bsoa.rpc.client.ClientFactory;
import io.bsoa.rpc.client.ClientProxyInvoker;
import io.bsoa.rpc.client.ProviderInfo;
import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.config.RegistryConfig;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.invoke.CallbackUtils;
import io.bsoa.rpc.invoke.StreamUtils;
import io.bsoa.rpc.listener.ConfigListener;
import io.bsoa.rpc.listener.ProviderInfoListener;
import io.bsoa.rpc.proxy.ProxyFactory;
import io.bsoa.rpc.registry.Registry;
import io.bsoa.rpc.registry.RegistryFactory;
import io.bsoa.rpc.server.InvokerHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/2/8 23:04. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("bsoa")
public class BsoaConsumerBootstrap<T> extends ConsumerBootstrap<T> {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(BsoaConsumerBootstrap.class);

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
    protected transient volatile ProviderInfoListener providerInfoListener;

    /**
     * 发布的调用者配置（含计数器）
     */
    protected final static ConcurrentHashMap<String, AtomicInteger> REFERRED_KEYS
            = new ConcurrentHashMap<String, AtomicInteger>();

    /**
     * 构造函数
     *
     * @param consumerConfig 服务消费者配置
     */
    protected BsoaConsumerBootstrap(ConsumerConfig<T> consumerConfig) {
        super(consumerConfig);
    }

    /**
     * Refer t.
     *
     * @return the t
     * @throws BsoaRuntimeException the init error exception
     */
    @Override
    public synchronized T refer() {
        if (proxyIns != null) {
            return proxyIns;
        }
        String key = consumerConfig.buildKey();
        // 检查参数
        // tags不能为空
        if (StringUtils.isBlank(consumerConfig.getTags())) {
            throw new BsoaRuntimeException(21300, "[21300]Value of \"tags\" value is " +
                    "not specified in consumer config with key " + key + " !");
        }
        // 提前检查接口类
        Class proxyClass = consumerConfig.getProxyClass();

        LOGGER.info("Refer consumer config : {} with bean id {}", key, consumerConfig.getId());

        // 注意同一interface，同一tags，同一protocol情况
        AtomicInteger cnt = REFERRED_KEYS.get(key); // 计数器
        if (cnt == null) { // 没有发布过
            cnt = CommonUtils.putToConcurrentMap(REFERRED_KEYS, key, new AtomicInteger(0));
        }
        int c = cnt.incrementAndGet();
        if (c > 3) {
            if (!CommonUtils.isFalse(consumerConfig.getParameter(BsoaConstants.HIDDEN_KEY_WARNNING))) {
                throw new BsoaRuntimeException(21304, "[21304]Duplicate consumer config with key " + key
                        + " has been referred more than 3 times!"
                        + " Maybe it's wrong config, please check it."
                        + " Ignore this if you did that on purpose!");
            } else {
                LOGGER.warn("[21304]Duplicate consumer config with key {} "
                        + "has been referred more than 3 times!"
                        + " Maybe it's wrong config, please check it."
                        + " Ignore this if you did that on purpose!", key);
            }
        } else if (c > 1) {
            LOGGER.warn("[21303]Duplicate consumer config with key {} has been referred!"
                    + " Maybe it's wrong config, please check it."
                    + " Ignore this if you did that on purpose!", key);
        }

        // 检查是否有回调函数
        CallbackUtils.scanAndRegisterCallBack(proxyClass);
        // 检查是否有流式调用函数
        StreamUtils.scanAndRegisterStream(proxyClass);
        // 注册接口类反序列化模板
//        if(!isGeneric()){
//            try {
//                CodecUtils.registryService(serialization, getProxyClass());
//            } catch (BsoaRuntimeException e) {
//                throw e;
//            } catch (Exception e) {
//                throw new BsoaRuntimeException("[21305]Registry codec template error!", e);
//            }
//        }
        // 如果本地发布了服务，则优选走本地代理，没有则走远程代理
        if (consumerConfig.isInJVM() && InvokerHolder.getInvoker(InvokerHolder.buildKey(consumerConfig)) != null) {
            LOGGER.info("Find matched provider invoker in current jvm, " +
                    "will invoke preferentially until it unexported");
        }

        ConfigListener configListener = new ConsumerAttributeListener();
        consumerConfig.setConfigListener(configListener);
        providerInfoListener = new ClientProviderInfoListener();
        try {
            // 生成客户端
            client = ClientFactory.getClient(this);
            client.init();
            // 构造Invoker对象（执行链）
            proxyInvoker = new ClientProxyInvoker(this);
            // 提前检查协议+序列化方式 TODO
            //ProtocolFactory.check(ProtocolType.valueOf(getProtocol()), SerializationType.valueOf(getSerialization()));
            // 创建代理类
            proxyIns = (T) ProxyFactory.buildProxy(consumerConfig.getProxy(), proxyClass, proxyInvoker);
        } catch (Exception e) {
            if (client != null) {
                client.destroy();
                client = null;
            }
            cnt.decrementAndGet(); // 发布失败不计数
            if (e instanceof BsoaRuntimeException) {
                throw (BsoaRuntimeException) e;
            } else {
                throw new BsoaRuntimeException(22222, "[21306]Build consumer proxy error!", e);
            }
        }
        if (consumerConfig.getOnAvailable() != null && client != null) {
            client.checkStateChange(false); // 状态变化通知监听器
        }
        BsoaContext.cacheConsumerConfig(this);
        return proxyIns;
    }

    /**
     * unRefer void.
     */
    @Override
    public synchronized void unRefer() {
        if (proxyIns == null) {
            return;
        }
        String key = consumerConfig.buildKey();
        LOGGER.info("UnRefer consumer config : {} with bean id {}", key, consumerConfig.getId());
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
        consumerConfig.setConfigListener(null);
        providerInfoListener = null;
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
    public List<ProviderInfo> subscribe() {
        List<ProviderInfo> tmpProviderInfoList = new ArrayList<ProviderInfo>();
        for (ProviderInfo providerInfo : tmpProviderInfoList) {

        }
        List<RegistryConfig> registryConfigs = consumerConfig.getRegistry();
        // 从注册中心订阅
        for (RegistryConfig registryConfig : registryConfigs) {
            Registry registry = RegistryFactory
                    .getRegistry(registryConfig);
            try {
                List<ProviderInfo> providerInfos = registry.subscribe(consumerConfig,
                        providerInfoListener, consumerConfig.getConfigListener());
                if (CommonUtils.isNotEmpty(providerInfos)) {
                    tmpProviderInfoList.addAll(providerInfos);
                }
            } catch (BsoaRuntimeException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.warn("Catch exception when subscribe from registry: " + registryConfig.getId()
                        + ", but you can ignore if it's called by JVM shutdown hook", e);
            }
        }
        return tmpProviderInfoList;
    }

    /**
     * 取消订阅服务列表
     */
    public void unsubscribe() {
        if (StringUtils.isEmpty(consumerConfig.getUrl()) && consumerConfig.isSubscribe()) {
            List<RegistryConfig> registryConfigs = consumerConfig.getRegistry();
            if (registryConfigs != null) {
                for (RegistryConfig registryConfig : registryConfigs) {
                    Registry registry = RegistryFactory.getRegistry(registryConfig);
                    try {
                        registry.unSubscribe(consumerConfig);
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
    private class ClientProviderInfoListener implements ProviderInfoListener {

        @Override
        public void addProvider(List<ProviderInfo> providerInfos) {
            if (client != null) {
                boolean originalState = client.isAvailable();
                client.addProvider(providerInfos);
                client.checkStateChange(originalState);
            }
        }

        @Override
        public void removeProvider(List<ProviderInfo> providerInfos) {
            if (client != null) {
                boolean originalState = client.isAvailable();
                client.removeProvider(providerInfos);
                client.checkStateChange(originalState);
            }
        }

        @Override
        public void updateProvider(List<ProviderInfo> newProviderInfos) {
            if (client != null) {
                boolean originalState = client.isAvailable();
                client.updateProvider(newProviderInfos);
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
            // 修改调用的tags cluster(loadblance) timeout, retries？
            Map<String, String> newValues = (Map<String, String>) newValueMap;
            Map<String, String> oldValues = new HashMap<String, String>();
            boolean rerefer = false;
            try { // 检查是否有变化
                // 是否过滤map?
                for (Map.Entry<String, String> entry : newValues.entrySet()) {
                    String newValue = entry.getValue();
                    String oldValue = consumerConfig.queryAttribute(entry.getKey());
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
                LOGGER.info("Rerefer consumer {}", consumerConfig.buildKey());
                try {
                    unsubscribe();// 取消订阅旧的
                    for (Map.Entry<String, String> entry : newValues.entrySet()) { // change attrs
                        consumerConfig.updateAttribute(entry.getKey(), entry.getValue(), true);
                    }
                } catch (Exception e) { // 切换属性出现异常
                    LOGGER.error("Catch exception when consumer attribute changed", e);
                    for (Map.Entry<String, String> entry : oldValues.entrySet()) { //rollback old attrs
                        consumerConfig.updateAttribute(entry.getKey(), entry.getValue(), true);
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
                        consumerConfig.updateAttribute(entry.getKey(), entry.getValue(), true);
                    }
                    subscribe(); // 重新订阅回滚后的旧的
                }
            }
        }

        /**
         * Switch client.
         *
         * @throws Exception the exception
         */
        private void switchClient() throws Exception {
            Client newclient = null;
            Client oldClient;
            try { // 构建新的
                newclient = ClientFactory.getClient(BsoaConsumerBootstrap.this); //生成新的 会再重新订阅
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
     * 得到实现代理类
     *
     * @return 实现代理类 proxy ins
     */
    public T getProxyIns() {
        return proxyIns;
    }

    /**
     * 得到实现代理类Invoker
     *
     * @return 实现代理类Invoker proxy invoker
     */
    public Invoker getProxyInvoker() {
        return proxyInvoker;
    }
}
