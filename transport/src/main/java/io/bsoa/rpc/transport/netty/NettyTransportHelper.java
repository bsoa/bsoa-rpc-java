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
package io.bsoa.rpc.transport.netty;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.SystemInfo;
import io.bsoa.rpc.common.struct.NamedThreadFactory;
import io.bsoa.rpc.transport.ServerTransportConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import static io.bsoa.rpc.common.BsoaConfigs.TRANSPORT_CLIENT_IO_RATIO;
import static io.bsoa.rpc.common.BsoaConfigs.TRANSPORT_CLIENT_IO_THREADS;
import static io.bsoa.rpc.common.BsoaConfigs.TRANSPORT_SERVER_IO_RATIO;
import static io.bsoa.rpc.common.BsoaConfigs.TRANSPORT_USE_EPOLL;
import static io.bsoa.rpc.common.BsoaConfigs.getBooleanValue;
import static io.bsoa.rpc.common.BsoaConfigs.getIntValue;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/17 17:36. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class NettyTransportHelper {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(NettyTransportHelper.class);

    /**
     * 服务端Boss线程池（一种协议一个）
     */
    private static ConcurrentHashMap<String, EventLoopGroup> serverBossGroups = new ConcurrentHashMap<>();
    /**
     * 服务端IO线程池（一种协议一个）
     */
    private static ConcurrentHashMap<String, EventLoopGroup> serverIoGroups = new ConcurrentHashMap<>();

    /**
     * 由于线程池是公用的，需要计数器，在最后一个人关闭时才能销毁
     */
    private static ConcurrentHashMap<EventLoopGroup, AtomicInteger> refCounter = new ConcurrentHashMap<>();

    /**
     * 得到服务端Boss线程池
     *
     * @param config 服务端配置
     * @return 服务端Boss线程池
     */
    public static EventLoopGroup getServerBossEventLoopGroup(ServerTransportConfig config) {
        String type = config.getProtocolType();
        EventLoopGroup bossGroup = serverBossGroups.get(type);
        AtomicInteger count;
        if (bossGroup == null) {
            synchronized (NettyTransportHelper.class) {
                bossGroup = serverBossGroups.get(config.getProtocolType());
                if (bossGroup == null) {
                    int bossThreads = config.getBossThreads();
                    bossThreads = bossThreads <= 0 ? Math.max(4, SystemInfo.CPU_CORES / 2) : bossThreads;
                    NamedThreadFactory threadName =
                            new NamedThreadFactory("BSOA-SEV-" + config.getPort() + "-BOSS", config.isDaemon());
                    bossGroup = config.isUseEpoll() ?
                            new EpollEventLoopGroup(bossThreads, threadName) :
                            new NioEventLoopGroup(bossThreads, threadName);
                    serverBossGroups.put(type, bossGroup);
                    refCounter.putIfAbsent(bossGroup, new AtomicInteger(0));
                }
            }
        }
        refCounter.get(bossGroup).incrementAndGet();
        return bossGroup;
    }

    /**
     * 关闭服务端Boss线程（只有最后一个使用者关闭才生效）
     *
     * @param config 服务端配置
     */
    public static void closeServerBossEventLoopGroup(ServerTransportConfig config) {
        EventLoopGroup bossGroup = serverBossGroups.get(config.getProtocolType());
        closeEventLoopGroupIfNoRef(bossGroup);
    }

    /**
     * 得到服务端IO线程池
     *
     * @param config 服务端配置
     * @return 服务端Boss线程池
     */
    public static EventLoopGroup getServerIoEventLoopGroup(ServerTransportConfig config) {
        String type = config.getProtocolType();
        EventLoopGroup ioGroup = serverIoGroups.get(type);
        if (ioGroup == null) {
            synchronized (NettyTransportHelper.class) {
                ioGroup = serverIoGroups.get(config.getProtocolType());
                if (ioGroup == null) {
                    int ioThreads = config.getIoThreads();
                    ioThreads = ioThreads <= 0 ? Math.max(8, SystemInfo.CPU_CORES + 1) : ioThreads;
                    NamedThreadFactory threadName =
                            new NamedThreadFactory("BSOA-SEV-" + config.getPort() + "-IO", config.isDaemon());
                    ioGroup = config.isUseEpoll() ?
                            new EpollEventLoopGroup(ioThreads, threadName) :
                            new NioEventLoopGroup(ioThreads, threadName);
                    setIoRatio(ioGroup, getIntValue(TRANSPORT_SERVER_IO_RATIO));

                    serverIoGroups.put(type, ioGroup);
                    refCounter.putIfAbsent(ioGroup, new AtomicInteger(0));
                }
            }
        }
        refCounter.get(ioGroup).incrementAndGet();
        return ioGroup;
    }

    /**
     * 关闭服务端IO线程（只有最后一个使用者关闭才生效）
     *
     * @param config 服务端配置
     */
    public static void closeServerIoEventLoopGroup(ServerTransportConfig config) {
        EventLoopGroup ioGroup = serverIoGroups.get(config.getProtocolType());
        closeEventLoopGroupIfNoRef(ioGroup);
    }

    /**
     * 如果是这个线程是最后一个使用者，直接删除
     *
     * @param eventLoopGroup 线程池
     */
    private static void closeEventLoopGroupIfNoRef(EventLoopGroup eventLoopGroup) {
        if (eventLoopGroup != null && refCounter.get(eventLoopGroup).decrementAndGet() <= 0) {
            if (!eventLoopGroup.isShuttingDown() && !eventLoopGroup.isShutdown()) {
                eventLoopGroup.shutdownGracefully();
            }
            refCounter.remove(eventLoopGroup);
        }
    }

    /**
     * 客户端IO线程 全局共用
     */
    private volatile static EventLoopGroup clientIOEventLoopGroup;

    /**
     * 获取客户端IO线程池
     *
     * @return
     */
    public static EventLoopGroup getClientIOEventLoopGroup() {
        if (clientIOEventLoopGroup == null || clientIOEventLoopGroup.isShutdown()) {
            synchronized (NettyTransportHelper.class) {
                if (clientIOEventLoopGroup == null || clientIOEventLoopGroup.isShutdown()) {
                    int clientIoThreads = getIntValue(TRANSPORT_CLIENT_IO_THREADS);
                    int threads = clientIoThreads > 0 ?
                            clientIoThreads : // 用户配置
                            Math.max(4, SystemInfo.CPU_CORES + 1); // 默认cpu+1,至少4个
                    NamedThreadFactory threadName = new NamedThreadFactory("BSOA-CLI-WORKER", true);
                    boolean useEpoll = getBooleanValue(TRANSPORT_USE_EPOLL);
                    clientIOEventLoopGroup = useEpoll ? new EpollEventLoopGroup(threads, threadName)
                            : new NioEventLoopGroup(threads, threadName);
                    setIoRatio(clientIOEventLoopGroup, getIntValue(TRANSPORT_CLIENT_IO_RATIO));
                    refCounter.putIfAbsent(clientIOEventLoopGroup, new AtomicInteger());
                    // SelectStrategyFactory 未设置
                }
            }
        }
        refCounter.get(clientIOEventLoopGroup).incrementAndGet();
        return clientIOEventLoopGroup;
    }

    private static void setIoRatio(EventLoopGroup eventLoopGroup, int ioRatio) {
//        TODO
//        if (eventLoopGroup instanceof EpollEventLoopGroup) {
//            ((EpollEventLoopGroup) eventLoopGroup).setIoRatio(ioRatio);
//        } else if (eventLoopGroup instanceof NioEventLoopGroup) {
//            ((NioEventLoopGroup) eventLoopGroup).setIoRatio(ioRatio);
//        }
    }

    /**
     * 关闭客户端IO线程池
     */
    public synchronized static void closeClientIOEventGroup() {
        LOGGER.debug("close Client EventLoopGroup...");
        if (clientIOEventLoopGroup != null) {
            AtomicInteger ref = refCounter.get(clientIOEventLoopGroup);
            if (ref.decrementAndGet() <= 0) {
                if (!clientIOEventLoopGroup.isShutdown() && !clientIOEventLoopGroup.isShuttingDown()) {
                    clientIOEventLoopGroup.shutdownGracefully();
                }
                refCounter.remove(clientIOEventLoopGroup);
            } else {
                LOGGER.warn("Client EventLoopGroup has ref : ", ref.get());
            }
            clientIOEventLoopGroup = null;
        }
    }

    protected static AdaptiveRecvByteBufAllocator RECV_BYTEBUF_ALLOCATOR = AdaptiveRecvByteBufAllocator.DEFAULT;

    private static ByteBufAllocator pooled = new UnpooledByteBufAllocator(false);

    public static ByteBufAllocator getByteBufAllocator() {
        return pooled;
    }

    public static ByteBuf getBuffer() {
        return pooled.buffer();
    }

    public static ByteBuf getBuffer(int size) {
        return pooled.buffer(size);
    }
}
