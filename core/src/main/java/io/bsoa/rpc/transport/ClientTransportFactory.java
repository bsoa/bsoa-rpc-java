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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

import io.bsoa.rpc.ext.ExtensionLoader;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;

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

    private final static ExtensionLoader<ClientTransport> extensionLoader
            = ExtensionLoaderFactory.getExtensionLoader(ClientTransport.class);

    /**
     * 连接池，一个服务端ip和端口同一协议只建立一个长连接，不管多少接口，共用长连接
     */
    private final static Map<String, ClientTransport> CLIENT_TRANSPORT_MAP = new ConcurrentHashMap<>();

    /**
     * 共享长连接？？
     */
    private final static Map<ClientTransport, AtomicInteger> refCountPool = new ConcurrentHashMap<>();//weak ref

    public static ClientTransport getClientTransport(ClientTransportConfig config) {
        ClientTransport clientTransport = extensionLoader.getExtension(config.getContainer());
        clientTransport.setConfig(config);
        CLIENT_TRANSPORT_MAP.put(clientTransport.toString(), clientTransport); // FIXME
        return clientTransport;
    }

    public static void releaseTransport(ClientTransport clientTransport, int timeout){
        if (clientTransport == null) {
            return;
        }
        clientTransport.disconnect();
//        AtomicInteger integer = refCountPool.get(clientTransport);
//        if (integer == null) {
//            return;
//        } else {
//            int currentCount = refCountPool.get(clientTransport).decrementAndGet();
//            InetSocketAddress local = clientTransport.getLocalAddress();
//            InetSocketAddress remote = clientTransport.getRemoteAddress();
//            logger.debug("Client transport {} of {} , current ref count is: {}", new Object[]{clientTransport,
//                    NetUtils.channelToString(local, remote), currentCount});
//            if(currentCount <= 0){ // 此长连接无任何引用
//                String ip = NetUtils.toIpString(remote);
//                int port = remote.getPort();
//                String key = NetUtils.getClientTransportKey(clientTransport.getConfig().getProvider().getProtocolType().name(),ip,port);
//                logger.info("Shutting down client transport {} now..", NetUtils.channelToString(local, remote));
//                connectionPool.remove(key);
//                refCountPool.remove(clientTransport);
//                if (timeout > 0) {
//                    int count = clientTransport.currentRequests();
//                    if (count > 0) { // 有正在调用的请求
//                        long start = JSFContext.systemClock.now();
//                        logger.info("There are {} outstanding call in transport, will shutdown util return", count);
//                        while (clientTransport.currentRequests() > 0
//                                && JSFContext.systemClock.now() - start < timeout) { // 等待返回结果
//                            try {
//                                Thread.sleep(10);
//                            } catch (InterruptedException e) {
//                            }
//                        }
//                    } // 关闭前检查已有请求？
//                }
//                clientTransport.shutdown();
//            }
//        }
    }

    /**
     * 关闭全部客户端连接
     */
    public static void closeAll() {
        LOGGER.info("Shutdown all bsoa client transport now...");
        try {
            for (Map.Entry<String, ClientTransport> entrySet : CLIENT_TRANSPORT_MAP.entrySet()) {
                ClientTransport clientTransport = entrySet.getValue();
                if (clientTransport.isAvailable()) {
                    clientTransport.disconnect();
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
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
