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
package io.bsoa.rpc.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.struct.ConcurrentHashSet;
import io.bsoa.rpc.common.struct.NamedThreadFactory;
import io.bsoa.rpc.common.struct.ScheduledService;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.listener.ConsumerStateListener;
import io.bsoa.rpc.transport.ClientTransport;
import io.bsoa.rpc.transport.ClientTransportConfig;
import io.bsoa.rpc.transport.ClientTransportFactory;

/**
 * 全部建立长连接，自动维护心跳和长连接
 *
 * Created by zhangg on 2016/7/17 15:27.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("all")
public class AllConnectConnectionHolder implements ConnectionHolder {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(AllConnectConnectionHolder.class);

    /**
     * 存活的客户端列表
     */
    private ConcurrentHashMap<Provider, ClientTransport> aliveConnections = new ConcurrentHashMap<>();

    /**
     * 存活但是亚健康节点（连续心跳超时，这种只发心跳，不发请求）
     */
    private ConcurrentHashMap<Provider, ClientTransport> subHealthConnections = new ConcurrentHashMap<>();

    /**
     * 失败待重试的客户端列表（连上后断开的）
     */
    private ConcurrentHashMap<Provider, ClientTransport> retryConnections = new ConcurrentHashMap<>();

    /**
     * 客户端变化provider的锁
     */
    private Lock providerLock = new ReentrantLock();

    /**
     * 当前服务集群对应的Consumer信息
     */
    protected ConsumerConfig<?> consumerConfig;

    /**
     * 根据provider查找存活的ClientTransport
     *
     * @param provider
     *         the provider
     * @return the client transport
     */
    @Deprecated
    public ClientTransport getAliveClientTransport(Provider provider) {
        ClientTransport transport = aliveConnections.get(provider);
        return transport != null ? transport : subHealthConnections.get(provider);
    }

    /**
     * Gets retry connections.
     *
     * @return the retry connections
     */
    public ConcurrentHashMap<Provider, ClientTransport> getRetryConnections() {
        return retryConnections;
    }

    /**
     * Add alive.
     *
     * @param provider
     *         the provider
     * @param transport
     *         the transport
     */
    protected void addAlive(Provider provider, ClientTransport transport) {
        ProviderCheckedInfo checkedInfo = checkProvider(provider);
        checkedInfo.setProviderExportedFully(true); // todo
        if (reliveToRetry(checkedInfo.isProviderExportedFully(),provider, transport)) {
            return;
        }
        aliveConnections.put(provider, transport);
        heartbeat_failed_counter.put(provider, new AtomicInteger(0));
    }

    /**
     * Add retry.
     *
     * @param provider
     *         the provider
     * @param transport
     *         the transport
     */
    protected void addRetry(Provider provider, ClientTransport transport) {
        retryConnections.put(provider, transport);
        heartbeat_failed_counter.put(provider, new AtomicInteger(0));
    }

    /**
     * 从重试丢到存活列表
     *
     * @param provider
     *         Provider
     * @param transport
     *         连接
     */
    protected void retryToAlive(Provider provider, ClientTransport transport) {
        providerLock.lock();
        try {
            if (retryConnections.remove(provider) != null) {
                ProviderCheckedInfo checkedInfo = checkProvider(provider);
                if (reliveToRetry(checkedInfo.isProviderExportedFully(),provider,transport)){
                    return;
                }
                aliveConnections.put(provider, transport);
            }
        } finally {
            providerLock.unlock();
        }
    }

    /**
     * 从存活丢到亚健康列表
     *
     * @param provider
     *         Provider
     * @param transport
     *         连接
     */
    protected void aliveToSubHealth(Provider provider, ClientTransport transport) {
        providerLock.lock();
        try {
            if (aliveConnections.remove(provider) != null) {
                subHealthConnections.put(provider, transport);
            }
        } finally {
            providerLock.unlock();
        }
    }

    /**
     * 从亚健康丢到存活列表
     *
     * @param provider
     *         Provider
     * @param transport
     *         连接
     */
    protected void subHealthToAlive(Provider provider, ClientTransport transport) {
        providerLock.lock();
        try {
            if (subHealthConnections.remove(provider) != null) {
                ProviderCheckedInfo checkedInfo = checkProvider(provider);
                if (reliveToRetry(checkedInfo.isProviderExportedFully(),provider, transport)){
                    return;
                }
                aliveConnections.put(provider, transport);
            }
        } finally {
            providerLock.unlock();
        }
    }

    /**
     * 从存活丢到亚健康列表
     *
     * @param provider
     *         Provider
     * @param transport
     *         连接
     */
    protected void subHealthToRetry(Provider provider, ClientTransport transport) {
        providerLock.lock();
        try {
            if (subHealthConnections.remove(provider) != null) {
                retryConnections.put(provider, transport);
            }
        } finally {
            providerLock.unlock();
        }
    }

    /**
     * 删除provider
     *
     * @param provider
     *         the provider
     * @return 如果已经建立连接 ，返回ClientTransport
     */
    protected ClientTransport remove(Provider provider) {
        providerLock.lock();
        try {
            ClientTransport transport = aliveConnections.remove(provider);
            if (transport == null) {
                transport = subHealthConnections.remove(provider);
                if (transport == null) {
                    transport = retryConnections.remove(provider);
                }
            }
            heartbeat_failed_counter.remove(provider);
            return transport;
        } finally {
            providerLock.unlock();
        }
    }

    /**
     * 通知状态变成不可用,主要是：<br>
     * 1.注册中心删除，更新节点后变成不可用时<br>
     * 2.连接断线后（心跳+调用），如果是可用节点为空
     */
    public void notifyStateChangeToUnavailable() {
        final List<ConsumerStateListener> onprepear = consumerConfig.getOnAvailable();
        if (onprepear != null) {
//            TODO
//            CallbackUtil.getCallbackThreadPool().execute(new Runnable() {
//                @Override
//                public void run() {
//                    // 状态变化通知监听器
//                    for (ConsumerStateListener listener : onprepear) {
//                        try {
//                            listener.onUnavailable(consumerConfig.getProxyIns());
//                        } catch (Exception e) {
//                            LOGGER.error("Failed to notify consumer state listener when state change to unavailable");
//                        }
//                    }
//                }
//            });
        }
    }

    /**
     * 通知状态变成可用,主要是：<br>
     * 1.启动成功变成可用时<br>
     * 2.注册中心增加，更新节点后变成可用时<br>
     * 3.重连上从一个可用节点都没有-->有可用节点时
     */
    public void notifyStateChangeToAvailable() {
        final List<ConsumerStateListener> onprepear = consumerConfig.getOnAvailable();
        if (onprepear != null) {
            //            TODO
//            CallbackUtil.getCallbackThreadPool().execute(new Runnable() {
//                @Override
//                public void run() {
//                    // 状态变化通知监听器
//                    for (ConsumerStateListener listener : onprepear) {
//                        try {
//                            listener.onAvailable(consumerConfig.getProxyIns());
//                        } catch (Exception e) {
//                            LOGGER.error("Failed to notify consumer state listener when state change to available");
//                        }
//                    }
//                }
//            });
        }
    }

    @Override
    public void init(ConsumerConfig consumerConfig) {
        if (this.consumerConfig != null) {
            throw new BsoaRuntimeException(22222, "init call multi-times");
        }
        this.consumerConfig = consumerConfig;
        startReconnectThread();
    }

    @Override
    public void addProvider(List<Provider> providerList) {
        final String interfaceId = consumerConfig.getInterfaceId();
        int providerSize = providerList.size();
        LOGGER.info("Init provider of {}, size is : {}", interfaceId, providerSize);
        if (providerSize > 0) {
            // 多线程建立连接
            int threads = Math.min(10, providerSize); // 最大10个
            final CountDownLatch latch = new CountDownLatch(providerSize);
            ThreadPoolExecutor initPool = new ThreadPoolExecutor(threads, threads,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(providerList.size()),
                    new NamedThreadFactory("JSF-CLI-CONN-" + interfaceId, true));
            int connectTimeout = consumerConfig.getConnectTimeout();
            for (final Provider provider : providerList) {
                final ClientTransportConfig config = providerToClientConfig(provider);
                config.setConnectTimeout(connectTimeout);
                initPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        ClientTransport transport = null;
                        try {
                            transport = ClientTransportFactory.getClientTransport(config);
                            transport.connect();
                            if (doubleCheck(interfaceId, provider, transport)) {
                                printSuccess(interfaceId, provider, transport);
                                addAlive(provider, transport);
                            } else {
                                printFailure(interfaceId, provider, transport);
                                addRetry(provider, transport);
                            }
                        } catch (Exception e) {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Failed to connect " + provider, e);
                            }
                            printFailure(interfaceId, provider, transport);
                            addRetry(provider, transport);
                        } finally {
                            latch.countDown(); // 连上或者抛异常
                        }
                    }

//                    if (connectionHolder.getRetryConnections().containsKey(provider)) {
//                        // 失败里有，立即重试
//                        ClientTransportConfig config = providerToClientConfig(provider);
//                        ClientTransport transport = ClientTransportFactory.getClientTransport(config);
//                        if (connectionHolder.doubleCheck(interfaceId, provider, transport)) {
//                            printSuccess(interfaceId, provider, transport);
//                            connectionHolder.retryToAlive(provider, transport);
//                        }
//                    } else { // 失败/死亡里没有
//                        // 存活里也没有
//                        if (!connectionHolder.getAliveConnections().containsKey(provider)) {
//                            ClientTransportConfig config = providerToClientConfig(provider);
//                            ClientTransport transport = ClientTransportFactory.getClientTransport(config);
//                            try {
//                                transport.connect();
//                                if (connectionHolder.doubleCheck(interfaceId, provider, transport)) {
//                                    printSuccess(interfaceId, provider, transport);
//                                    connectionHolder.addAlive(provider, transport);
//                                } else {
//                                    printFailure(interfaceId, provider, transport);
//                                    connectionHolder.addRetry(provider, transport);
//                                }
//                            } catch (Exception e) {
//                                printDead(interfaceId, provider, e);
//                            }
//                        }
//                    }
                });
            }

            try {
                int totalTimeout = ((providerSize % threads == 0) ? (providerSize / threads) : ((providerSize /
                        threads) + 1)) * connectTimeout + 500;
                latch.await(totalTimeout, TimeUnit.MILLISECONDS); // 一直等到子线程都结束
            } catch (InterruptedException e) {
                LOGGER.error("Exception when init provider", e);
            } finally {
                initPool.shutdown(); // 关闭线程池
            }
        }
    }

    @Override
    public void removeProvider(List<Provider> providers) {
        String interfaceId = consumerConfig.getInterfaceId();
        LOGGER.info("{} remove {} providers from list", interfaceId, providers.size());
        for (Provider provider : providers) {
            try {
                // 从存活和重试列表里都删除
                //  判断一个删成功 就不走下一个
                ClientTransport transport = remove(provider);
                LOGGER.info("Remove {} provider:{} from list success !", interfaceId, provider);
                if (transport != null) {
                    ClientTransportFactory.releaseTransport(transport, consumerConfig.getDisconnectTimeout());
                }
            } catch (Exception e) {
                LOGGER.error("remove " + consumerConfig.getInterfaceId() + " provider " + provider
                        + " from list error:", e);
            }
        }
    }

    @Override
    public void updateProviders(List<Provider> providers) {

    }

    @Override
    public Map<Provider, ClientTransport> clearProviders() {
        providerLock.lock();
        try {
            // 当前存活+重试的
            HashMap<Provider, ClientTransport> all = new HashMap<Provider, ClientTransport>(aliveConnections);
            all.putAll(subHealthConnections);
            all.putAll(retryConnections);
            subHealthConnections.clear();
            aliveConnections.clear();
            retryConnections.clear();
            heartbeat_failed_counter.clear();
            return all;
        } finally {
            providerLock.unlock();
        }
    }

    @Override
    public ConcurrentHashMap<Provider, ClientTransport> getAvailableConnections() {
        return aliveConnections.isEmpty() ? subHealthConnections : aliveConnections;
    }

    @Override
    public List<Provider> getAvailableProviders() {
        // 存活为空的，那就用亚健康的
        ConcurrentHashMap<Provider, ClientTransport> map =
                aliveConnections.isEmpty() ? subHealthConnections : aliveConnections;
        return new ArrayList<Provider>(map.keySet());
    }

    @Override
    public ClientTransport getAvailableClientTransport(Provider provider) {
        ClientTransport transport = aliveConnections.get(provider);
        return transport != null ? transport : subHealthConnections.get(provider);
    }

    @Override
    public boolean isAvailableEmpty() {
        return aliveConnections.isEmpty() && subHealthConnections.isEmpty();
    }

    /**
     * Provider对象得到 ClientTransportConfig
     *
     * @param provider
     *         Provider
     * @return ClientTransportConfig
     */
    private ClientTransportConfig providerToClientConfig(Provider provider) {
        return new ClientTransportConfig()
                .setProvider(provider)
                .setConnectTimeout(consumerConfig.getConnectTimeout())
                .setInvokeTimeout(consumerConfig.getTimeout())
                .setDisconnectTimeout(consumerConfig.getDisconnectTimeout())
                .setInvokeTimeout(consumerConfig.getTimeout())
                .setConnectionNum(consumerConfig.getConnection())
                .setChannelListeners(consumerConfig.getOnConnect());
    }

    /**
     * 获取当前的Provider列表（包括连上和没连上的）
     *
     * @return 当前的Provider列表 set
     */
    public Set<Provider> currentProviderList() {
        providerLock.lock();
        try {
            ConcurrentHashSet<Provider> providers = new ConcurrentHashSet<Provider>();
            providers.addAll(aliveConnections.keySet());
            providers.addAll(subHealthConnections.keySet());
            providers.addAll(retryConnections.keySet());
            return providers;
        } finally {
            providerLock.unlock();
        }
    }

    @Override
    public void setUnavailable(Provider provider, ClientTransport transport) {
        providerLock.lock();
        try {
            boolean first = isAvailableEmpty();
            if (aliveConnections.remove(provider) != null) {
                retryConnections.put(provider, transport);
                if (!first && isAvailableEmpty()) { // 原来不空，变成空
                    notifyStateChangeToUnavailable();
                }
            }
        } finally {
            providerLock.unlock();
        }
    }

    @Override
    public void preDestroy() {
        // 关闭线程
        shutdownReconnectThread();
        // 清空可用列表，不让再调了
        providerLock.lock();
        try {
            // 当前存活+重试的
            retryConnections.putAll(aliveConnections);
            aliveConnections.clear();
        } finally {
            providerLock.unlock();
        }
    }

    @Override
    public void destroy() {
        // 清空所有列表
        Map<Provider, ClientTransport> all = clearProviders();

        // 多线程销毁已经建立的连接
        int providerSize = all.size();
        if (providerSize > 0) {
            int timeout = consumerConfig.getDisconnectTimeout();
            int threads = Math.min(10, providerSize); // 最大10个
            final CountDownLatch latch = new CountDownLatch(providerSize);
            ThreadPoolExecutor closepool = new ThreadPoolExecutor(threads, threads,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(providerSize),
                    new NamedThreadFactory("JSF-CLI-DISCONN-" + consumerConfig.getInterfaceId(), true));
            for (Map.Entry<Provider, ClientTransport> entry : all.entrySet()) {
                final Provider provider = entry.getKey();
                final ClientTransport transport = entry.getValue();
                closepool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ClientTransportFactory.releaseTransport(transport, 0);
                        } catch (Exception e) {
                            LOGGER.warn("catch exception but ignore it when close alive client : {}", provider);
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }
            try {
                int totalTimeout = ((providerSize % threads == 0) ? (providerSize / threads) : ((providerSize /
                        threads) + 1)) * timeout + 500;
                latch.await(totalTimeout, TimeUnit.MILLISECONDS); // 一直等到
            } catch (InterruptedException e) {
                LOGGER.error("Exception when close transport", e);
            } finally {
                closepool.shutdown();
            }
        }
    }

    /**
     * 打印连接成功日志
     *
     * @param interfaceId 接口名称
     * @param provider    服务端
     * @param transport   连接
     */
    private void printSuccess(String interfaceId, Provider provider, ClientTransport transport) {
        LOGGER.info("Connect to {} provider:{} success ! The connection is "
//                        + NetUtils.connectToString(transport.getRemoteAddress(), transport.getLocalAddress())
                , interfaceId, provider);
    }

    /**
     * 打印连接失败日志
     *
     * @param interfaceId 接口名称
     * @param provider    服务端
     * @param transport   连接
     */
    private void printFailure(String interfaceId, Provider provider, ClientTransport transport) {
        LOGGER.info("Connect to {} provider:{} failure !", interfaceId, provider);
    }

    /**
     * 打印连不上日志
     *
     * @param interfaceId 接口名称
     * @param provider    服务端
     */
    private void printDead(String interfaceId, Provider provider, Exception e) {
        Throwable cause = e.getCause();
        LOGGER.warn("Connect to {} provider:{} failure !! The exception is " + ExceptionUtils.toShortString(e, 1)
                        + (cause != null ? ", cause by " + cause.getMessage() + "." : "."),
                interfaceId, provider);
    }

    /**
     * 获取当前的Provider列表（包括连上和没连上的）
     *
     * @return 当前的Provider列表 set
     */
    public Map<String, Set<Provider>> currentProviderMap() {
        providerLock.lock();
        try {
            Map<String, Set<Provider>> tmp = new LinkedHashMap<String, Set<Provider>>();
            tmp.put("alive", new HashSet<Provider>(aliveConnections.keySet()));
            tmp.put("subHealth", new HashSet<Provider>(subHealthConnections.keySet()));
            tmp.put("retry", new HashSet<Provider>(retryConnections.keySet()));
            return tmp;
        } finally {
            providerLock.unlock();
        }
    }

    /**
     * 两次验证检查ClientTransport是否存活
     *
     * @param interfaceId
     *         接口
     * @param transport
     *         ClientTransport对象
     * @return 是否存活
     */
    protected boolean doubleCheck(String interfaceId, Provider provider, ClientTransport transport) {
        if (transport.isAvailable()) {
            try { // 睡一下下 防止被连上又被服务端踢下线
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            if (transport.isAvailable()) { // double check
                return true;
            } else { // 可能在黑名单里，刚连上就断开了
                LOGGER.warn("[JSF-22004]Connection has been closed after connected (in last 100ms)!" +
                                " Maybe connection of provider has reached limit," +
                                " or your host is in the blacklist of provider {}/{}",
                         interfaceId, transport.getConfig().getProvider());
                provider.setReconnectPeriodCoefficient(5);
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * 重连+心跳线程
     */
    private volatile ScheduledService reconThread;

    /**
     * 心跳线程
     */
    private volatile ScheduledService hbThread;

    /**
     * 重试次数标记（针对每个Provider不一样）
     */
    private AtomicInteger reconnectFlag = new AtomicInteger();

    /**
     * 启动重连+心跳线程
     */
    protected void startReconnectThread() {
        final String interfaceId = consumerConfig.getInterfaceId();
        // 启动线程池
        // 默认每隔10秒重连
        int reconnect = consumerConfig.getReconnect();
        if (reconnect > 0) {
            reconnect = Math.max(reconnect, 2000); // 最小2000
            reconThread = new ScheduledService("JSF-CLI-RC-" + interfaceId, ScheduledService.MODE_FIXEDDELAY, new
                    Runnable() {
                        @Override
                        public void run() {
                            try {
                                doReconnect();
                            } catch (Throwable e) {
                                LOGGER.error("Exception when retry connect to provider", e);
                            }
                        }
                    }, reconnect, reconnect, TimeUnit.MILLISECONDS).start();
        }
        // 默认每隔30秒发心跳
        int heartbeat = consumerConfig.getHeartbeat();
        if (heartbeat > 0) {
            heartbeat = Math.max(heartbeat, 5000); // 最小5000
            hbThread = new ScheduledService("JSF-CLI-HB-" + interfaceId, ScheduledService.MODE_FIXEDDELAY, new Runnable() {
                @Override
                public void run() {
                    try {
                        sendHeartbeat();
                    } catch(Throwable e) {
                        LOGGER.error("Exception when send heartbeat to provider", e);
                    }
                }
            }, heartbeat, heartbeat, TimeUnit.MILLISECONDS).start();
        }
    }

    /**
     * 重连断开和死亡的节点
     */
    private void doReconnect() {
        String interfaceId = consumerConfig.getInterfaceId();
        int thisTime = reconnectFlag.incrementAndGet();
        boolean print = thisTime % 6 == 0; //是否打印error,每6次打印一次
        boolean isAliveEmptyFirst = isAvailableEmpty();
        for (Map.Entry<Provider, ClientTransport> entry : getRetryConnections()
                .entrySet()) {
            Provider provider = entry.getKey();
            int providerPeriodCoefficient = provider.getReconnectPeriodCoefficient();
            if (thisTime % providerPeriodCoefficient != 0) {
                continue; // 如果命中重连周期，则进行重连
            }
            ClientTransport transport = entry.getValue();
            LOGGER.debug("Retry connect to {} provider:{} ...", interfaceId, provider);
            try {
                transport.connect();
                if (doubleCheck(interfaceId, provider, transport)) {
//                    LOGGER.info("Connect to {} provider:{} success by retry! The connection is " +
//                                    NetUtils.connectToString(transport.getRemoteAddress(), transport.getLocalAddress()),
//                            interfaceId, provider);
                    provider.setReconnectPeriodCoefficient(1);
                    retryToAlive(provider, transport);
                }
            } catch (Exception e) {
                if (print) {
                    LOGGER.warn("[JSF-22008]Retry connect to {} provider:{} error ! The exception is " + e
                            .getMessage(), interfaceId, provider);
                } else {
                    LOGGER.debug("Retry connect to {} provider:{} error ! The exception is " + e
                            .getMessage(), interfaceId, provider);
                }
            }
        }
        if (isAliveEmptyFirst && !isAvailableEmpty()) { // 原来空，变成不空
            notifyStateChangeToAvailable();
        }
    }

    /**
     * 心跳失败计数器
     */
    private ConcurrentHashMap<Provider, AtomicInteger> heartbeat_failed_counter = new ConcurrentHashMap<Provider,
            AtomicInteger>();

    /**
     * 给存活的和亚健康的节点发心跳
     */
    private void sendHeartbeat() {
        for (Map.Entry<Provider, ClientTransport> entry : aliveConnections.entrySet()) {
            sendHeartbeatToProvider(entry.getKey(), entry.getValue(), true);
        }
        for (Map.Entry<Provider, ClientTransport> entry : subHealthConnections.entrySet()) {
            sendHeartbeatToProvider(entry.getKey(), entry.getValue(), false);
        }
    }

    /**
     * 给单个节点发心跳
     *
     * @param provider
     *         服务端
     * @param transport
     *         连接
     * @param isAliveProvider
     *         是否存活列表
     */
    private void sendHeartbeatToProvider(Provider provider, ClientTransport transport, boolean isAliveProvider) {
        /*
        //TODO
        tring interfaceId = consumerConfig.getInterfaceId();
        ProtocolType protocolType = provider.getProtocolType();
        if (protocolType != ProtocolType.jsf && protocolType != ProtocolType.dubbo) {
            return; // 指定协议才发
        }
        if (!transport.isOpen()) {
            aliveToRetryIfExist(provider, transport);
        }
        BaseMessage message = MessageBuilder.buildHeartbeatRequest();
        if (protocolType == ProtocolType.dubbo) { // dubbo的发hessian
            message.getMsgHeader().setCodecType(BsoaConstants.CodecType.hessian.value());
        }
        LOGGER.debug("Send heartbeat to {} provider:{} ...", interfaceId, provider);
        boolean ok = false;
        Throwable exception = null;
        for (int i = 0; i < 2; i++) { // 试两次
            try {
                BaseMessage response = transport.send(message, 2000);
                if (response == null
                        || (message.getMsgHeader().getMsgId() != response.getMsgHeader().getMsgId())) {
                    // 心跳发送返回错误数据，打警告
                    LOGGER.warn("Send heartbeat to {} provider:{} " +
                            "return unmatched response", interfaceId, provider);
                    Thread.sleep(1000);
                    addFailedCnt(provider);
                } else {
                    if (!isAliveProvider) {
                        subHealthToAlive(provider, transport);
                        LOGGER.info("Sub-health provider has been recovered, move {} from sub-health" +
                                " to alive provider", provider);
                    }
                    ok = true; // 正常返回的情况
                    resetFailedCnt(provider);
                    break;
                }
            } catch (Throwable e) {
                if (!transport.isOpen()) { // 已经断开连接的，则不重试
                    resetFailedCnt(provider);
                    if (isAliveProvider) {
                        aliveToRetryIfExist(provider, transport); // 存活到重试
                    } else {
                        subHealthToRetry(provider, transport); // 亚健康到重试
                    }
                    exception = e instanceof ClientClosedException ? e
                            : new ClientClosedException("[JSF-22009]Channel has been closed when send heartbeat");
                    break; // 正常断线的情况
                } else {
                    addFailedCnt(provider);
                    exception = e; // 记住上次异常
                }
            }
        }
        if (!ok && exception != null) { // 连续2次心跳异常
            LOGGER.warn("[JSF-22005]Send heartbeat to " + interfaceId + " provider:" + provider
                    + " error !", ExceptionUtils.toShortString(exception, 1));
            String monitor = consumerConfig.getParameter(BsoaConstants.HIDDEN_KEY_MONITOR);
//            if (!CommonUtils.isFalse(monitor) && MonitorFactory.isMonitorOpen(interfaceId,
//                    MonitorFactory.STATUS_FLAG, null)) {
//                // 除非主动不监控
//                ProviderStat stat = new ProviderStat(provider.getIp(), provider.getPort(),
//                        JSFContext.getLocalIpv4(), exception.getClass().getName());
//                MonitorFactory.getMonitor(MonitorFactory.MONITOR_CONSUMER_STATUS, interfaceId)
//                        .recordInvoked(stat);
//            }
        }
        if (isAliveProvider && getFailedCnt(provider) >= 6
                && aliveConnections.containsKey(provider)) { // 连续失败6次（3个心跳周期），加入亚健康
            aliveToSubHealth(provider, transport);
            LOGGER.warn("[JSF-22006]Send heartbeat failed over 3 times, move {} from alive to" +
                    " sub-health provider", provider);
        }
        if (!isAliveProvider && getFailedCnt(provider) >= 60
                && subHealthConnections.containsKey(provider)) { // 连续失败60次（30个心跳周期），加入重连列表
            subHealthToRetry(provider, transport);
            LOGGER.warn("[JSF-22007]Send heartbeat failed over 30 times, move {} from sub-health to" +
                    " retry provider", provider);
        }*/
    }

    private void addFailedCnt(Provider provider) {
        AtomicInteger cnt = heartbeat_failed_counter.get(provider);
        if (cnt != null) {
            cnt.incrementAndGet();
        }
    }

    private void resetFailedCnt(Provider provider) {
        AtomicInteger cnt = heartbeat_failed_counter.get(provider);
        if (cnt != null) {
            cnt.set(0);
        }
    }

    private int getFailedCnt(Provider provider) {
        AtomicInteger cnt = heartbeat_failed_counter.get(provider);
        return cnt != null ? cnt.get() : 0;
    }

    /**
     * 关闭线程
     */
    protected void shutdownReconnectThread() {
        if (reconThread != null) {
            reconThread.shutdown();
            reconThread = null;
        }
        if (hbThread != null) {
            hbThread.shutdown();
            hbThread = null;
        }
    }

    /**
     * 通过telnet命令检查Provider是否支持调用优化 1.5.0+支持</br>
     *
     * 检查服务是否存在此节点上
     *
     * @param provider
     *         服务端
     */
    public ProviderCheckedInfo checkProvider(Provider provider) {
        ProviderCheckedInfo checkedInfo = new ProviderCheckedInfo();
//        if (provider.getProtocolType() == ProtocolType.jsf) {
//            for (int i = 0; i < 2; i++) { // 试2次
//                TelnetClient client = new TelnetClient(provider.getIp(), provider.getPort(), 2000, 2000);
//                try {
//                    // 发送握手检查服务端版本
//                    String versionStr = client.telnetJSF("version");
//                    try {
//                        Map map = JSON.parseObject(versionStr, Map.class);
//                        int realVersion = CommonUtils.parseInt(StringUtils.toString(map.get("jsfVersion")),
//                                provider.getBsoaVersion());
//                        if (realVersion != provider.getBsoaVersion()) {
//                            provider.setBsoaVersion(realVersion);
//                        }
//                    } catch (Exception e) {
//                    }
//                    // 检查服务端是否支持invocation简化
//                    String ifaceId = consumerConfig.getIfaceId();
//                    if (StringUtils.isNotEmpty(ifaceId)) {
//                        if (provider.getBsoaVersion() >= 1500) {
//                            String result = client.telnetJSF("check iface " + consumerConfig.getInterfaceId()
//                                    + " " + ifaceId);
//                            if (result != null) {
//                                provider.setInvocationOptimizing("1".equals(result));
//                            }
//                        } else {
//                            provider.setInvocationOptimizing(false);
//                        }
//                    }
//                    //检查指定服务是否已经存在
//                    checkedInfo.setProviderExportedFully(checkProviderExportedFully(client, provider));
//
//                    return checkedInfo; // 正常情况直接返回
//                } catch (Exception e) {
//                    LOGGER.warn(e.getMessage());
//                } finally {
//                    client.close();
//                }
//            }
//        }
        return checkedInfo;
    }

    /**
     * 通过执行telnet ls -l 命令，查询指定节点上是否已经发布了相关条件的服务
     *
     * @param provider
     *
     * @return
     * @since 1.6.1
     */
    private boolean checkProviderExportedFully(TelnetClient client, Provider provider) throws IOException {
        String interfaceId = StringUtils.isEmpty(provider.getInterfaceId()) ? consumerConfig.getInterfaceId() : provider.getInterfaceId();
        String alias = StringUtils.isEmpty(provider.getTags()) ? consumerConfig.getTags() : provider.getTags();
        String serviceStr = String.format("%s?alias=%s&",interfaceId,alias);
        // telnet 检查该节点上是否已经发布此服务
        String exportedService = client.telnetJSF("ls -l");
        if (exportedService != null && exportedService.indexOf(serviceStr) > -1){
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     * 存活节（通过重试检查后）点如果不包含指定的服务，则加入重试列表中
     *
     * @param provider
     * @param transport
     * @return true 存活节点如果不包含指定的服务
     */
    private boolean reliveToRetry(boolean isProviderExportedFully,Provider provider, ClientTransport transport) {
        if (!isProviderExportedFully){
            provider.setReconnectPeriodCoefficient(5);
            addRetry(provider,transport);
            LOGGER.warn("No {}/{} service in {}:{} at the moment.add this node to retry connection list.",new Object[]{
                            provider.getInterfaceId(),
                            provider.getTags(),
                            provider.getIp(),
                            provider.getPort()
                    }
            );
            return true;
        }
        return false;
    }


    /**
     * telnet check provider节点信息
     */
    private class ProviderCheckedInfo{

        private boolean providerExportedFully;

        //telnet是否成功
        private boolean telnetOk;

        public ProviderCheckedInfo() {
        }

        public boolean isProviderExportedFully() {
            return providerExportedFully;
        }

        public void setProviderExportedFully(boolean providerExportedFully) {
            this.providerExportedFully = providerExportedFully;
        }

        public boolean isTelnetOk() {
            return telnetOk;
        }

        public void setTelnetOk(boolean telnetOk) {
            this.telnetOk = telnetOk;
        }
    }
}
