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
package io.bsoa.rpc.client;

import io.bsoa.rpc.common.struct.ConcurrentHashSet;
import io.bsoa.rpc.common.struct.NamedThreadFactory;
import io.bsoa.rpc.common.struct.ScheduledService;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.context.AsyncContext;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.listener.ConsumerStateListener;
import io.bsoa.rpc.protocol.Protocol;
import io.bsoa.rpc.protocol.ProtocolFactory;
import io.bsoa.rpc.protocol.ProtocolNegotiator;
import io.bsoa.rpc.transport.ClientTransport;
import io.bsoa.rpc.transport.ClientTransportConfig;
import io.bsoa.rpc.transport.ClientTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * 全部建立长连接，自动维护心跳和长连接
 * <p>
 * Created by zhangg on 2016/7/17 15:27.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("all")
public class AllConnectConnectionHolder extends ConnectionHolder {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(AllConnectConnectionHolder.class);

    /**
     * 构造函数
     *
     * @param consumerConfig 服务消费者配置
     */
    protected AllConnectConnectionHolder(ConsumerConfig consumerConfig) {
        super(consumerConfig);
    }

    /**
     * 存活的客户端列表
     */
    private ConcurrentHashMap<ProviderInfo, ClientTransport> aliveConnections = new ConcurrentHashMap<>();

    /**
     * 存活但是亚健康节点（连续心跳超时，这种只发心跳，不发请求）
     */
    private ConcurrentHashMap<ProviderInfo, ClientTransport> subHealthConnections = new ConcurrentHashMap<>();

    /**
     * 失败待重试的客户端列表（连上后断开的）
     */
    private ConcurrentHashMap<ProviderInfo, ClientTransport> retryConnections = new ConcurrentHashMap<>();

    /**
     * 客户端变化provider的锁
     */
    private Lock providerLock = new ReentrantLock();

    /**
     * Gets retry connections.
     *
     * @return the retry connections
     */
    public ConcurrentHashMap<ProviderInfo, ClientTransport> getRetryConnections() {
        return retryConnections;
    }

    /**
     * Add alive.
     *
     * @param providerInfo the provider
     * @param transport    the transport
     */
    protected void addAlive(ProviderInfo providerInfo, ClientTransport transport) {
        if (checkState(providerInfo, transport)) {
            aliveConnections.put(providerInfo, transport);
            heartbeat_failed_counter.put(providerInfo, new AtomicInteger(0));
        }
    }

    /**
     * Add retry.
     *
     * @param providerInfo the provider
     * @param transport    the transport
     */
    protected void addRetry(ProviderInfo providerInfo, ClientTransport transport) {
        retryConnections.put(providerInfo, transport);
        heartbeat_failed_counter.put(providerInfo, new AtomicInteger(0));
    }

    /**
     * 从重试丢到存活列表
     *
     * @param providerInfo Provider
     * @param transport    连接
     */
    protected void retryToAlive(ProviderInfo providerInfo, ClientTransport transport) {
        providerLock.lock();
        try {
            if (retryConnections.remove(providerInfo) != null) {
                if (checkState(providerInfo, transport)) {
                    aliveConnections.put(providerInfo, transport);
                }
            }
        } finally {
            providerLock.unlock();
        }
    }

    /**
     * 检查状态是否可用
     *
     * @param providerInfo    服务提供者信息
     * @param clientTransport 客户端长连接
     * @return 状态是否可用
     */
    public boolean checkState(ProviderInfo providerInfo, ClientTransport clientTransport) {
        Protocol protocol = ProtocolFactory.getProtocol(providerInfo.getProtocolType());
        ProtocolNegotiator negotiator = protocol.negotiator();
        if (negotiator != null) {
            return negotiator.handshake(providerInfo, clientTransport);
        } else {
            return true;
        }
    }

    /**
     * 从存活丢到亚健康列表
     *
     * @param providerInfo Provider
     * @param transport    连接
     */
    protected void aliveToSubHealth(ProviderInfo providerInfo, ClientTransport transport) {
        providerLock.lock();
        try {
            if (aliveConnections.remove(providerInfo) != null) {
                subHealthConnections.put(providerInfo, transport);
            }
        } finally {
            providerLock.unlock();
        }
    }

    /**
     * 从亚健康丢到存活列表
     *
     * @param providerInfo Provider
     * @param transport    连接
     */
    protected void subHealthToAlive(ProviderInfo providerInfo, ClientTransport transport) {
        providerLock.lock();
        try {
            if (subHealthConnections.remove(providerInfo) != null) {
                if (checkState(providerInfo, transport)) {
                    aliveConnections.put(providerInfo, transport);
                }
            }
        } finally {
            providerLock.unlock();
        }
    }

    /**
     * 从存活丢到亚健康列表
     *
     * @param providerInfo Provider
     * @param transport    连接
     */
    protected void subHealthToRetry(ProviderInfo providerInfo, ClientTransport transport) {
        providerLock.lock();
        try {
            if (subHealthConnections.remove(providerInfo) != null) {
                retryConnections.put(providerInfo, transport);
            }
        } finally {
            providerLock.unlock();
        }
    }

    /**
     * 删除provider
     *
     * @param providerInfo the provider
     * @return 如果已经建立连接 ，返回ClientTransport
     */
    protected ClientTransport remove(ProviderInfo providerInfo) {
        providerLock.lock();
        try {
            ClientTransport transport = aliveConnections.remove(providerInfo);
            if (transport == null) {
                transport = subHealthConnections.remove(providerInfo);
                if (transport == null) {
                    transport = retryConnections.remove(providerInfo);
                }
            }
            heartbeat_failed_counter.remove(providerInfo);
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
        final List<ConsumerStateListener> onAvailable = consumerConfig.getOnAvailable();
        if (onAvailable != null) {
            AsyncContext.getAsyncThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    // 状态变化通知监听器
                    for (ConsumerStateListener listener : onAvailable) {
                        try {
                            listener.onUnavailable(consumerConfig.getBootstrap().getProxyIns());
                        } catch (Exception e) {
                            LOGGER.error("Failed to notify consumer state listener when state change to unavailable");
                        }
                    }
                }
            });
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
            AsyncContext.getAsyncThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    // 状态变化通知监听器
                    for (ConsumerStateListener listener : onprepear) {
                        try {
                            listener.onAvailable(consumerConfig.getBootstrap().getProxyIns());
                        } catch (Exception e) {
                            LOGGER.error("Failed to notify consumer state listener when state change to available");
                        }
                    }
                }
            });
        }
    }

    @Override
    public void init() {
        if (reconThread == null && hbThread == null) {
            startReconnectThread();
        }
    }

    @Override
    public void addProvider(List<ProviderInfo> providerInfoList) {
        final String interfaceId = consumerConfig.getInterfaceId();
        int providerSize = providerInfoList.size();
        LOGGER.info("Init provider of {}, size is : {}", interfaceId, providerSize);
        if (providerSize > 0) {
            // 多线程建立连接
            int threads = Math.min(10, providerSize); // 最大10个
            final CountDownLatch latch = new CountDownLatch(providerSize);
            ThreadPoolExecutor initPool = new ThreadPoolExecutor(threads, threads,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(providerInfoList.size()),
                    new NamedThreadFactory("CLI-CONN-" + interfaceId, true));
            int connectTimeout = consumerConfig.getConnectTimeout();
            for (final ProviderInfo providerInfo : providerInfoList) {
                final ClientTransportConfig config = providerToClientConfig(providerInfo);
                config.setConnectTimeout(connectTimeout);
                initPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        ClientTransport transport = null;
                        try {
                            transport = ClientTransportFactory.getClientTransport(config);
                            transport.connect();
                            if (doubleCheck(interfaceId, providerInfo, transport)) {
                                printSuccess(interfaceId, providerInfo, transport);
                                addAlive(providerInfo, transport);
                            } else {
                                printFailure(interfaceId, providerInfo, transport);
                                addRetry(providerInfo, transport);
                            }
                        } catch (Exception e) {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Failed to connect " + providerInfo, e);
                            }
                            printDead(interfaceId, providerInfo, transport, e);
                            addRetry(providerInfo, transport);
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
    public void removeProvider(List<ProviderInfo> providerInfos) {
        String interfaceId = consumerConfig.getInterfaceId();
        LOGGER.info("{} remove {} providers from list", interfaceId, providerInfos.size());
        for (ProviderInfo providerInfo : providerInfos) {
            try {
                // 从存活和重试列表里都删除
                //  判断一个删成功 就不走下一个
                ClientTransport transport = remove(providerInfo);
                LOGGER.info("Remove {} provider:{} from list success !", interfaceId, providerInfo);
                if (transport != null) {
                    ClientTransportFactory.releaseTransport(transport, consumerConfig.getDisconnectTimeout());
                }
            } catch (Exception e) {
                LOGGER.error("remove " + consumerConfig.getInterfaceId() + " provider " + providerInfo
                        + " from list error:", e);
            }
        }
    }

    @Override
    public ConcurrentHashMap<ProviderInfo, ClientTransport> getAvailableConnections() {
        return aliveConnections.isEmpty() ? subHealthConnections : aliveConnections;
    }

    @Override
    public List<ProviderInfo> getAvailableProviders() {
        // 存活为空的，那就用亚健康的
        ConcurrentHashMap<ProviderInfo, ClientTransport> map =
                aliveConnections.isEmpty() ? subHealthConnections : aliveConnections;
        return new ArrayList<ProviderInfo>(map.keySet());
    }

    @Override
    public ClientTransport getAvailableClientTransport(ProviderInfo providerInfo) {
        ClientTransport transport = aliveConnections.get(providerInfo);
        return transport != null ? transport : subHealthConnections.get(providerInfo);
    }

    @Override
    public boolean isAvailableEmpty() {
        return aliveConnections.isEmpty() && subHealthConnections.isEmpty();
    }

    /**
     * Provider对象得到 ClientTransportConfig
     *
     * @param providerInfo Provider
     * @return ClientTransportConfig
     */
    private ClientTransportConfig providerToClientConfig(ProviderInfo providerInfo) {
        return new ClientTransportConfig()
                .setProviderInfo(providerInfo)
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
    public Set<ProviderInfo> currentProviderList() {
        providerLock.lock();
        try {
            ConcurrentHashSet<ProviderInfo> providerInfos = new ConcurrentHashSet<ProviderInfo>();
            providerInfos.addAll(aliveConnections.keySet());
            providerInfos.addAll(subHealthConnections.keySet());
            providerInfos.addAll(retryConnections.keySet());
            return providerInfos;
        } finally {
            providerLock.unlock();
        }
    }

    @Override
    public void setUnavailable(ProviderInfo providerInfo, ClientTransport transport) {
        providerLock.lock();
        try {
            boolean first = isAvailableEmpty();
            if (aliveConnections.remove(providerInfo) != null) {
                retryConnections.put(providerInfo, transport);
                if (!first && isAvailableEmpty()) { // 原来不空，变成空
                    notifyStateChangeToUnavailable();
                }
            }
        } finally {
            providerLock.unlock();
        }
    }

    @Override
    public void destroy() {
        destroy(null);
    }

    @Override
    public void destroy(DestroyHook destroyHook) {
        // 关闭重连线程
        shutdownReconnectThread();
        // 关闭全部长连接
        closeAllConnections(destroyHook);
    }

    /**
     * 清空服务列表
     *
     * @return 带回收的服务列表
     */
    protected Map<ProviderInfo, ClientTransport> clearProviders() {
        providerLock.lock();
        try {
            // 当前存活+重试的
            HashMap<ProviderInfo, ClientTransport> all = new HashMap<ProviderInfo, ClientTransport>(aliveConnections);
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

    /**
     * 销毁全部连接
     *
     * @param destroyHook 销毁钩子
     */
    protected void closeAllConnections(DestroyHook destroyHook) {
        // 清空所有列表,不让再调了
        Map<ProviderInfo, ClientTransport> all = clearProviders();
        if (destroyHook != null) {
            try {
                destroyHook.preDestroy();
            } catch (Exception e) {
                LOGGER.warn("22222", e);
            }
        }
        // 多线程销毁已经建立的连接
        int providerSize = all.size();
        if (providerSize > 0) {
            int timeout = consumerConfig.getDisconnectTimeout();
            int threads = Math.min(10, providerSize); // 最大10个
            final CountDownLatch latch = new CountDownLatch(providerSize);
            ThreadPoolExecutor closePool = new ThreadPoolExecutor(threads, threads,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(providerSize),
                    new NamedThreadFactory("CLI-DISCONN-" + consumerConfig.getInterfaceId(), true));
            for (Map.Entry<ProviderInfo, ClientTransport> entry : all.entrySet()) {
                final ProviderInfo providerInfo = entry.getKey();
                final ClientTransport transport = entry.getValue();
                closePool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ClientTransportFactory.releaseTransport(transport, 0);
                        } catch (Exception e) {
                            LOGGER.warn("catch exception but ignore it when close alive client : {}", providerInfo);
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
                closePool.shutdown();
            }
        }
    }

    /**
     * 打印连接成功日志
     *
     * @param interfaceId  接口名称
     * @param providerInfo 服务端
     * @param transport    连接
     */
    private void printSuccess(String interfaceId, ProviderInfo providerInfo, ClientTransport transport) {
        LOGGER.info("Connect to {} provider:{} success ! The connection is "
                        + NetUtils.connectToString(transport.getChannel().remoteAddress(),
                transport.getChannel().localAddress())
                , interfaceId, providerInfo);
    }

    /**
     * 打印连接失败日志
     *
     * @param interfaceId  接口名称
     * @param providerInfo 服务端
     * @param transport    连接
     */
    private void printFailure(String interfaceId, ProviderInfo providerInfo, ClientTransport transport) {
        LOGGER.info("Connect to {} provider:{} failure !", interfaceId, providerInfo);
    }

    /**
     * 打印连不上日志
     *
     * @param interfaceId  接口名称
     * @param providerInfo 服务端
     * @param transport
     */
    private void printDead(String interfaceId, ProviderInfo providerInfo, ClientTransport transport, Exception e) {
        Throwable cause = e.getCause();
        LOGGER.warn("Connect to {} provider:{} failure !! The exception is " + ExceptionUtils.toShortString(e, 1)
                        + (cause != null ? ", cause by " + cause.getMessage() + "." : "."),
                interfaceId, providerInfo);
    }

    /**
     * 获取当前的Provider列表（包括连上和没连上的）
     *
     * @return 当前的Provider列表 set
     */
    public Map<String, Set<ProviderInfo>> currentProviderMap() {
        providerLock.lock();
        try {
            Map<String, Set<ProviderInfo>> tmp = new LinkedHashMap<String, Set<ProviderInfo>>();
            tmp.put("alive", new HashSet<ProviderInfo>(aliveConnections.keySet()));
            tmp.put("subHealth", new HashSet<ProviderInfo>(subHealthConnections.keySet()));
            tmp.put("retry", new HashSet<ProviderInfo>(retryConnections.keySet()));
            return tmp;
        } finally {
            providerLock.unlock();
        }
    }

    /**
     * 两次验证检查ClientTransport是否存活
     *
     * @param interfaceId 接口
     * @param transport   ClientTransport对象
     * @return 是否存活
     */
    protected boolean doubleCheck(String interfaceId, ProviderInfo providerInfo, ClientTransport transport) {
        if (transport.isAvailable()) {
            try { // 睡一下下 防止被连上又被服务端踢下线
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            if (transport.isAvailable()) { // double check
                return true;
            } else { // 可能在黑名单里，刚连上就断开了
                LOGGER.warn("[22004]Connection has been closed after connected (in last 100ms)!" +
                                " Maybe connection of provider has reached limit," +
                                " or your host is in the blacklist of provider {}/{}",
                        interfaceId, transport.getConfig().getProviderInfo());
                providerInfo.setReconnectPeriodCoefficient(5);
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
            reconThread = new ScheduledService("CLI-RC-" + interfaceId, ScheduledService.MODE_FIXEDDELAY, new
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
            hbThread = new ScheduledService("CLI-HB-" + interfaceId, ScheduledService.MODE_FIXEDDELAY, new Runnable() {
                @Override
                public void run() {
                    try {
                        sendHeartbeat();
                    } catch (Throwable e) {
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
        for (Map.Entry<ProviderInfo, ClientTransport> entry : getRetryConnections()
                .entrySet()) {
            ProviderInfo providerInfo = entry.getKey();
            int providerPeriodCoefficient = providerInfo.getReconnectPeriodCoefficient();
            if (thisTime % providerPeriodCoefficient != 0) {
                continue; // 如果命中重连周期，则进行重连
            }
            ClientTransport transport = entry.getValue();
            LOGGER.debug("Retry connect to {} provider:{} ...", interfaceId, providerInfo);
            try {
                transport.connect();
                if (doubleCheck(interfaceId, providerInfo, transport)) {
//                    LOGGER.info("Connect to {} provider:{} success by retry! The connection is " +
//                                    NetUtils.connectToString(transport.remoteAddress(), transport.localAddress()),
//                            interfaceId, provider);
                    providerInfo.setReconnectPeriodCoefficient(1);
                    retryToAlive(providerInfo, transport);
                }
            } catch (Exception e) {
                if (print) {
                    LOGGER.warn("[22008]Retry connect to {} provider:{} error ! The exception is " + e
                            .getMessage(), interfaceId, providerInfo);
                } else {
                    LOGGER.debug("Retry connect to {} provider:{} error ! The exception is " + e
                            .getMessage(), interfaceId, providerInfo);
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
    private ConcurrentHashMap<ProviderInfo, AtomicInteger> heartbeat_failed_counter = new ConcurrentHashMap<ProviderInfo,
            AtomicInteger>();

    /**
     * 给存活的和亚健康的节点发心跳
     */
    private void sendHeartbeat() {
        for (Map.Entry<ProviderInfo, ClientTransport> entry : aliveConnections.entrySet()) {
            sendHeartbeatToProvider(entry.getKey(), entry.getValue(), true);
        }
        for (Map.Entry<ProviderInfo, ClientTransport> entry : subHealthConnections.entrySet()) {
            sendHeartbeatToProvider(entry.getKey(), entry.getValue(), false);
        }
    }

    /**
     * 给单个节点发心跳
     *
     * @param providerInfo    服务端
     * @param transport       连接
     * @param isAliveProvider 是否存活列表
     */
    private void sendHeartbeatToProvider(ProviderInfo providerInfo, ClientTransport transport, boolean isAliveProvider) {
        /*
        //TODO
        tring interfaceId = consumerConfig.getInterfaceId();
        ProtocolType getProtocolType = provider.getProtocolType();
        if (getProtocolType != ProtocolType.jsf && getProtocolType != ProtocolType.dubbo) {
            return; // 指定协议才发
        }
        if (!transport.isOpen()) {
            aliveToRetryIfExist(provider, transport);
        }
        BaseMessage message = MessageBuilder.buildHeartbeatRequest();
        if (getProtocolType == ProtocolType.dubbo) { // dubbo的发hessian
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
                            : new ClientClosedException("[22009]Channel has been closed when send heartbeat");
                    break; // 正常断线的情况
                } else {
                    addFailedCnt(provider);
                    exception = e; // 记住上次异常
                }
            }
        }
        if (!ok && exception != null) { // 连续2次心跳异常
            LOGGER.warn("[22005]Send heartbeat to " + interfaceId + " provider:" + provider
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
            LOGGER.warn("[22006]Send heartbeat failed over 3 times, move {} from alive to" +
                    " sub-health provider", provider);
        }
        if (!isAliveProvider && getFailedCnt(provider) >= 60
                && subHealthConnections.containsKey(provider)) { // 连续失败60次（30个心跳周期），加入重连列表
            subHealthToRetry(provider, transport);
            LOGGER.warn("[22007]Send heartbeat failed over 30 times, move {} from sub-health to" +
                    " retry provider", provider);
        }*/
    }

    private void addFailedCnt(ProviderInfo providerInfo) {
        AtomicInteger cnt = heartbeat_failed_counter.get(providerInfo);
        if (cnt != null) {
            cnt.incrementAndGet();
        }
    }

    private void resetFailedCnt(ProviderInfo providerInfo) {
        AtomicInteger cnt = heartbeat_failed_counter.get(providerInfo);
        if (cnt != null) {
            cnt.set(0);
        }
    }

    private int getFailedCnt(ProviderInfo providerInfo) {
        AtomicInteger cnt = heartbeat_failed_counter.get(providerInfo);
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
}
