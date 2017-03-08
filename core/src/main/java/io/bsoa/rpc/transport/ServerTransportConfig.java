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
package io.bsoa.rpc.transport;

import io.bsoa.rpc.common.BsoaOptions;
import io.bsoa.rpc.listener.ChannelListener;
import io.bsoa.rpc.server.ServerHandler;

import java.util.List;
import java.util.Map;

import static io.bsoa.rpc.common.BsoaConfigs.getBooleanValue;
import static io.bsoa.rpc.common.BsoaConfigs.getIntValue;
import static io.bsoa.rpc.common.BsoaConfigs.getStringValue;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/15 23:08. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ServerTransportConfig {

    private String host = getStringValue(BsoaOptions.SERVER_HOST);
    private int port = getIntValue(BsoaOptions.SERVER_PORT_START);

    private String contextPath = getStringValue(BsoaOptions.SERVER_CONTEXT_PATH);
    private String container = getStringValue(BsoaOptions.DEFAULT_TRANSPORT);
    private int backlog = getIntValue(BsoaOptions.TRANSPORT_SERVER_BACKLOG);
    private String protocolType = getStringValue(BsoaOptions.DEFAULT_PROTOCOL);
    private boolean reuseAddr = getBooleanValue(BsoaOptions.TRANSPORT_SERVER_REUSE_ADDR);
    private boolean keepAlive = getBooleanValue(BsoaOptions.TRANSPORT_SERVER_KEEPALIVE);
    private boolean tcpNoDelay = getBooleanValue(BsoaOptions.TRANSPORT_SERVER_TCPNODELAY);
    //    private int CONNECTTIMEOUT = 5000;
//    private int TIMEOUT = 2000;//server side timeout config default ..
    private int bizMaxThreads = getIntValue(BsoaOptions.SERVER_POOL_MAX); //default business pool set to 200
    private String bizPoolType = getStringValue(BsoaOptions.SERVER_POOL_TYPE);

    private boolean useEpoll = getBooleanValue(BsoaOptions.TRANSPORT_USE_EPOLL);
    private String bizPoolQueueType = getStringValue(BsoaOptions.SERVER_POOL_QUEUE_TYPE);  // 队列类型
    private int bizPoolQueues = getIntValue(BsoaOptions.SERVER_POOL_QUEUE); // 队列大小

    private int bossThreads = getIntValue(BsoaOptions.TRANSPORT_SERVER_BOSS_THREADS); // boss线程,一个端口绑定到一个线程

    private int ioThreads = getIntValue(BsoaOptions.TRANSPORT_SERVER_IO_THREADS); // worker线程==IO线程，一个长连接绑定到一个线程

    private int maxConnection = getIntValue(BsoaOptions.SERVER_ACCEPTS); // 最大连接数 default set to 100
    private int payload = getIntValue(BsoaOptions.TRANSPORT_PAYLOAD_MAX); // 最大数据包 default set to 8M
    private int buffer = getIntValue(BsoaOptions.TRANSPORT_BUFFER_SIZE); // 缓冲器大小
    private boolean telnet = getBooleanValue(BsoaOptions.SERVER_TELNET); // 是否允许telnet
    private boolean daemon = getBooleanValue(BsoaOptions.SERVER_DAEMON); // 是否守护线程，true随主线程退出而退出，false需要主动退出

    private int bufferMin = getIntValue(BsoaOptions.TRANSPORT_BUFFER_MIN);
    private int bufferMax = getIntValue(BsoaOptions.TRANSPORT_BUFFER_MAX);

    private Map<String, String> parameters;//其他一些参数配置

    private List<ChannelListener> channelListeners;
    private ServerHandler serverHandler;

//    private boolean printMessage = false; // 是否debug模式打印消息体

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    public boolean isReuseAddr() {
        return reuseAddr;
    }

    public void setReuseAddr(boolean reuseAddr) {
        this.reuseAddr = reuseAddr;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public int getBizMaxThreads() {
        return bizMaxThreads;
    }

    public void setBizMaxThreads(int bizMaxThreads) {
        this.bizMaxThreads = bizMaxThreads;
    }

    public String getBizPoolType() {
        return bizPoolType;
    }

    public void setBizPoolType(String bizPoolType) {
        this.bizPoolType = bizPoolType;
    }

    public boolean isUseEpoll() {
        return useEpoll;
    }

    public void setUseEpoll(boolean useEpoll) {
        this.useEpoll = useEpoll;
    }

    public String getBizPoolQueueType() {
        return bizPoolQueueType;
    }

    public void setBizPoolQueueType(String bizPoolQueueType) {
        this.bizPoolQueueType = bizPoolQueueType;
    }

    public int getBizPoolQueues() {
        return bizPoolQueues;
    }

    public void setBizPoolQueues(int bizPoolQueues) {
        this.bizPoolQueues = bizPoolQueues;
    }

    public int getBossThreads() {
        return bossThreads;
    }

    public void setBossThreads(int bossThreads) {
        this.bossThreads = bossThreads;
    }

    public int getIoThreads() {
        return ioThreads;
    }

    public void setIoThreads(int ioThreads) {
        this.ioThreads = ioThreads;
    }

    public int getMaxConnection() {
        return maxConnection;
    }

    public void setMaxConnection(int maxConnection) {
        this.maxConnection = maxConnection;
    }

    public int getPayload() {
        return payload;
    }

    public void setPayload(int payload) {
        this.payload = payload;
    }

    public int getBuffer() {
        return buffer;
    }

    public void setBuffer(int buffer) {
        this.buffer = buffer;
    }

    public boolean isTelnet() {
        return telnet;
    }

    public void setTelnet(boolean telnet) {
        this.telnet = telnet;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public int getBufferMin() {
        return bufferMin;
    }

    public ServerTransportConfig setBufferMin(int bufferMin) {
        this.bufferMin = bufferMin;
        return this;
    }

    public int getBufferMax() {
        return bufferMax;
    }

    public ServerTransportConfig setBufferMax(int bufferMax) {
        this.bufferMax = bufferMax;
        return this;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public List<ChannelListener> getChannelListeners() {
        return channelListeners;
    }

    public void setChannelListeners(List<ChannelListener> channelListeners) {
        this.channelListeners = channelListeners;
    }

    public ServerHandler getServerHandler() {
        return serverHandler;
    }

    public void setServerHandler(ServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }
}
