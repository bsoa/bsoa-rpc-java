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
package io.bsoa.rpc.server.bsoa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.base.Invoker;
import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.config.ServerConfig;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.server.Server;
import io.bsoa.rpc.transport.ServerTransport;
import io.bsoa.rpc.transport.ServerTransportConfig;
import io.bsoa.rpc.transport.ServerTransportFactory;

/**
 * Created by zhangg on 2016/7/15 23:02.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("bsoa")
public class BsoaServer implements Server {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(BsoaServer.class);

    /**
     * 是否已经启动
     */
    private volatile boolean started;
    /**
     * 服务端配置
     */
    private ServerTransportConfig serverTransportConfig;

    /**
     * 服务端通讯城
     */
    private ServerTransport serverTransport;

    /**
     * 服务端处理器
     */
    private BsoaServerHandler serverHandler;

    /**
     * private
     *
     * @param serverConfig
     */

    @Override
    public void init(ServerConfig serverConfig) {
        this.serverTransportConfig = convertConfig(serverConfig);
    }

    @Override
    public void start() {
        if (started) {
            return;
        }
        synchronized (this) {
            if (started) {
                return;
            }
            if (serverTransportConfig == null) {
                throw new BsoaRuntimeException(22222, "need init first");
            }
            serverHandler = new BsoaServerHandler(serverTransportConfig);
            serverTransportConfig.setServerHandler(serverHandler);
            serverTransport = ServerTransportFactory.getServerTransport(serverTransportConfig);
            started = serverTransport.start();
        }
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean hasNoEntry() {
        return serverHandler.entrySize() <= 0;
    }

    @Override
    public void stop() {
        if (!started) {
            return;
        }
        serverTransport.stop();
        started = false;
    }

    @Override
    public void registerProcessor(ProviderConfig providerConfig, Invoker instance) {
        String key = buildKey(providerConfig);
        serverHandler.registerProcessor(key, instance);
//        ServerAuthHelper.addInterface(providerConfig.getInterfaceId(), providerConfig.getTags());
    }

    private String buildKey(ProviderConfig providerConfig) {
        return providerConfig.getInterfaceId() + "/" + providerConfig.getTags();
    }

    @Override
    public void unRegisterProcessor(ProviderConfig providerConfig, boolean closeIfNoEntry) {
        String key = buildKey(providerConfig);
        serverHandler.unRegisterProcessor(key);
        if (closeIfNoEntry && hasNoEntry()) { //如果需要关闭 则关闭
            stop();
        }
    }

    private static ServerTransportConfig convertConfig(ServerConfig serverConfig) {
        ServerTransportConfig serverTransportConfig = new ServerTransportConfig();
        serverTransportConfig.setPort(serverConfig.getPort());
        serverTransportConfig.setProtocolType(serverConfig.getProtocol());
        serverTransportConfig.setHost(serverConfig.getBoundHost());
//        serverTransportConfig.setPrintMessage(serverConfig.isDebug());
        serverTransportConfig.setContextPath(serverConfig.getContextpath());
        serverTransportConfig.setBizMaxThreads(serverConfig.getThreads());
        serverTransportConfig.setBizPoolType(serverConfig.getThreadpool());
        serverTransportConfig.setIoThreads(serverConfig.getIothreads());
        serverTransportConfig.setChannelListeners(serverConfig.getOnconnect());
        serverTransportConfig.setMaxConnection(serverConfig.getAccepts());
        serverTransportConfig.setBuffer(serverConfig.getBuffer());
        serverTransportConfig.setPayload(serverConfig.getPayload());
        serverTransportConfig.setTelnet(serverConfig.isTelnet());
        serverTransportConfig.setUseEpoll(serverConfig.isEpoll());
        serverTransportConfig.setBizPoolQueueType(serverConfig.getQueuetype());
        serverTransportConfig.setBizPoolQueues(serverConfig.getQueues());
        serverTransportConfig.setDispatcher(serverConfig.getDispatcher());
        serverTransportConfig.setDaemon(serverConfig.isDaemon());
        serverTransportConfig.setParameters(serverConfig.getParameters());
        return serverTransportConfig;
    }
}
