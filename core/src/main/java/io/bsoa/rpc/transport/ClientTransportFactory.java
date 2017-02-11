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
package io.bsoa.rpc.transport;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

import io.bsoa.rpc.client.ProviderInfo;
import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.ext.ExtensionLoader;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;

import static io.bsoa.rpc.common.BsoaConfigs.TRANSPORT_CONNECTION_REUSE;
import static io.bsoa.rpc.common.BsoaConfigs.getBooleanValue;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/17 17:48. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ClientTransportFactory {
    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = getLogger(ClientTransportFactory.class);

    /**
     * Extension加载器
     */
    private final static ExtensionLoader<ClientTransport> extensionLoader
            = ExtensionLoaderFactory.getExtensionLoader(ClientTransport.class);

    /**
     * 是否长连接复用
     */
    private final static boolean channelReuse = getBooleanValue(TRANSPORT_CONNECTION_REUSE);

    /**
     * 长连接不复用的时候，一个ClientTransportConfig对应一个ClientTransport
     */
    private final static Map<ClientTransportConfig, ClientTransport> ALL_TRANSPORT_MAP
            = channelReuse ? null : new ConcurrentHashMap<>();

    /**
     * 长连接复用时，共享长连接的连接池，一个服务端ip和端口同一协议只建立一个长连接，不管多少接口，共用长连接
     */
    private final static Map<String, ClientTransport> CLIENT_TRANSPORT_MAP
            = channelReuse ? new ConcurrentHashMap<>() : null;

    /**
     * 长连接复用时，共享长连接的计数器
     */
    private final static Map<ClientTransport, AtomicInteger> TRANSPORT_REF_COUNTER
            = channelReuse ? new ConcurrentHashMap<>() : null;

    /**
     * 通过配置获取长连接
     *
     * @param config 长连接
     * @return
     */
    public static ClientTransport getClientTransport(ClientTransportConfig config) {
        if (channelReuse) {
            String key = getAddr(config);
            ClientTransport transport = CLIENT_TRANSPORT_MAP.get(key);
            if (transport == null) {
                transport = extensionLoader.getExtension(config.getContainer(),
                        new Class[]{ClientTransportConfig.class},
                        new Object[]{config});
                ClientTransport oldTransport = CLIENT_TRANSPORT_MAP.putIfAbsent(key, transport); // 保存唯一长连接
                if (oldTransport != null) {
                    LOGGER.warn("Multiple threads init ClientTransport with same key:" + key);
                    transport.destroy(); //如果同时有人插入，则使用第一个
                    transport = oldTransport;
                }
            }
            AtomicInteger counter = TRANSPORT_REF_COUNTER.get(transport);
            if (counter == null) {
                counter = new AtomicInteger(0);
                AtomicInteger oldCounter = TRANSPORT_REF_COUNTER.putIfAbsent(transport, counter);
                if (oldCounter != null) {
                    counter = oldCounter;
                }
            }
            counter.incrementAndGet(); // 计数器加1
            return transport;
        } else {
            ClientTransport transport = ALL_TRANSPORT_MAP.get(config);
            if (transport == null) {
                transport = extensionLoader.getExtension(config.getContainer(),
                        new Class[]{ClientTransportConfig.class},
                        new Object[]{config});
                ClientTransport old = ALL_TRANSPORT_MAP.putIfAbsent(config, transport); // 保存唯一长连接
                if (old != null) {
                    LOGGER.warn("Multiple threads init ClientTransport with same ClientTransportConfig!");
                    transport.destroy(); //如果同时有人插入，则使用第一个
                    transport = old;
                }
            }
            return transport;
        }
    }

    private static String getAddr(ClientTransportConfig config) {
        ProviderInfo providerInfo = config.getProviderInfo();
        return providerInfo.getProtocolType() + "://" + providerInfo.getIp() + ":" + providerInfo.getPort();
    }

    /**
     * 销毁长连接
     * @param clientTransport
     * @param disconnectTimeout
     */
    public static void releaseTransport(ClientTransport clientTransport, int disconnectTimeout) {
        if (clientTransport == null) {
            return;
        }
        boolean needDestroy;
        if (channelReuse) { // 开启长连接复用，根据连接引用数判断
            AtomicInteger integer = TRANSPORT_REF_COUNTER.get(clientTransport);
            if (integer == null) {
                needDestroy = true;
            } else {
                int currentCount = integer.decrementAndGet(); // 当前连接引用数
                if (LOGGER.isDebugEnabled()) {
                    InetSocketAddress local = clientTransport.getChannel().getLocalAddress();
                    InetSocketAddress remote = clientTransport.getChannel().getRemoteAddress();
                    LOGGER.debug("Client transport {} of {} , current ref count is: {}", clientTransport,
                            NetUtils.channelToString(local, remote), currentCount);
                }
                if (currentCount <= 0) { // 此长连接无任何引用，可以销毁
                    String key = getAddr(clientTransport.getConfig());
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Shutting down client transport {} now..",
                                NetUtils.channelToString(clientTransport.getChannel().getLocalAddress(),
                                        clientTransport.getChannel().getRemoteAddress()));
                    }
                    CLIENT_TRANSPORT_MAP.remove(key);
                    TRANSPORT_REF_COUNTER.remove(clientTransport);
                    needDestroy = true;
                } else {
                    needDestroy = false;
                }
            }
        } else {  // 未开启长连接复用，可以销毁
            ALL_TRANSPORT_MAP.remove(clientTransport.getConfig());
            needDestroy = true;
        }
        // 执行销毁动作
        if (needDestroy) {
            if (disconnectTimeout > 0) { // 需要等待结束时间
                int count = clientTransport.currentRequests();
                if (count > 0) { // 有正在调用的请求
                    long start = BsoaContext.now();
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("There are {} outstanding call in transport, wait {}ms to end",
                                count, disconnectTimeout);
                    }
                    while (clientTransport.currentRequests() > 0
                            && BsoaContext.now() - start < disconnectTimeout) { // 等待返回结果
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                        }
                    }
                } // 关闭前检查已有请求？
            }
            // disconnectTimeout已过
            int count = clientTransport.currentRequests();
            if (count > 0) { // 还有正在调用的请求
                LOGGER.warn("There are {} outstanding call in client transport," +
                        " and shutdown now", count);
            }
            // 反向的也删一下
            if (REVERSE_CLIENT_TRANSPORT_MAP != null) {
                REVERSE_CLIENT_TRANSPORT_MAP.remove(clientTransport.getChannel());
            }
            clientTransport.destroy();
        }
    }

    /**
     * 关闭全部客户端连接
     */
    public static void closeAll() {
        if (((channelReuse && CommonUtils.isNotEmpty(CLIENT_TRANSPORT_MAP))
                || (!channelReuse && CommonUtils.isNotEmpty(ALL_TRANSPORT_MAP)))
                && LOGGER.isInfoEnabled()) {
            LOGGER.info("Shutdown all bsoa client transport now!");
        }
        try {
            if (channelReuse) {
                for (Map.Entry<String, ClientTransport> entrySet : CLIENT_TRANSPORT_MAP.entrySet()) {
                    ClientTransport clientTransport = entrySet.getValue();
                    if (clientTransport.isAvailable()) {
                        clientTransport.destroy();
                    }
                }
                CLIENT_TRANSPORT_MAP.clear();
                TRANSPORT_REF_COUNTER.clear();
            } else {
                for (Map.Entry<ClientTransportConfig, ClientTransport> entrySet : ALL_TRANSPORT_MAP.entrySet()) {
                    ClientTransport clientTransport = entrySet.getValue();
                    if (clientTransport.isAvailable()) {
                        clientTransport.destroy();
                    }
                }
                ALL_TRANSPORT_MAP.clear();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * 反向虚拟的长连接对象, 缓存一个长连接一个
     */
    private static ConcurrentHashMap<AbstractChannel, ClientTransport> REVERSE_CLIENT_TRANSPORT_MAP = null;

    /**
     * 构建反向的（服务端到客户端）虚拟长连接
     *
     * @param channel 已有长连接Channel
     * @return 虚拟长连接
     */
    public static ClientTransport getReverseClientTransport(AbstractChannel channel) {
        if (REVERSE_CLIENT_TRANSPORT_MAP == null) { // 初始化
            synchronized (ClientTransportFactory.class) {
                if (REVERSE_CLIENT_TRANSPORT_MAP == null) {
                    REVERSE_CLIENT_TRANSPORT_MAP = new ConcurrentHashMap<>();
                }
            }
        }
        ClientTransport transport = REVERSE_CLIENT_TRANSPORT_MAP.get(channel);
        if (transport == null) {
            synchronized (ClientTransportFactory.class) {
                transport = REVERSE_CLIENT_TRANSPORT_MAP.get(channel);
                if (transport == null) {
                    transport = extensionLoader.getExtension(BsoaConfigs.getStringValue(BsoaConfigs.DEFAULT_TRANSPORT));
                    transport.setChannel(channel);
                    REVERSE_CLIENT_TRANSPORT_MAP.putIfAbsent(channel, transport); // 保存唯一长连接
                }
            }
        }
        return transport;
    }

    /**
     * 检查Future列表，删除超时请求
     */
//    public static void checkFuture() {
//        for (Map.Entry<String, ClientTransport> entrySet : connectionPool.entrySet()) {
//            try {
//                ClientTransport clientTransport = entrySet.getValue();
//                if (clientTransport instanceof AbstractTCPClientTransport) {
//                    AbstractTCPClientTransport aClientTransport = (AbstractTCPClientTransport) clientTransport;
//                    aClientTransport.checkFutureMap();
//                }
//            } catch (Exception e) {
//                logger.error(e.getMessage(), e);
//            }
//        }
}
