
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

import java.util.List;
import java.util.Map;

import io.bsoa.rpc.listener.ChannelListener;
import io.bsoa.rpc.listener.NegotiatorListener;
import io.bsoa.rpc.server.ServerHandler;

import static io.bsoa.rpc.common.BsoaConfigs.*;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/15 23:08. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ServerTransportConfig {

    private String host = "0.0.0.0";
    private int port = 20220;

    private String contextPath = getStringValue(SERVER_CONTEXT_PATH);
    private String container = getStringValue(DEFAULT_TRANSPORT);
    private int backlog = getIntValue(TRANSPORT_SERVER_BACKLOG);
    private String protocolType = getStringValue(DEFAULT_PROTOCOL);
    private boolean reuseAddr = getBooleanValue(TRANSPORT_SERVER_REUSE_ADDR);
    private boolean keepAlive =  getBooleanValue(TRANSPORT_SERVER_KEEPALIVE);
    private boolean tcpNoDelay =  getBooleanValue(TRANSPORT_SERVER_TCPNODELAY);
    //    private int CONNECTTIMEOUT = 5000;
//    private int TIMEOUT = 2000;//server side timeout config default ..
    private int bizMaxThreads = getIntValue(SERVER_POOL_MAX); //default business pool set to 200
    private String bizPoolType = getStringValue(SERVER_POOL_TYPE);

    private boolean useEpoll = getBooleanValue(TRANSPORT_USE_EPOLL);
    private String bizPoolQueueType = getStringValue(SERVER_POOL_QUEUE_TYPE);  // 队列类型
    private int bizPoolQueues = getIntValue(SERVER_POOL_QUEUE); // 队列大小

    private int bossThreads = getIntValue(TRANSPORT_SERVER_BOSS_THREADS); // boss线程,一个端口绑定到一个线程

    private int ioThreads = getIntValue(TRANSPORT_SERVER_IO_THREADS); // worker线程==IO线程，一个长连接绑定到一个线程

    private int maxConnection = getIntValue(TRANSPORT_SERVER_MAX_CONNECTION); // 最大连接数 default set to 100
    private int payload = getIntValue(TRANSPORT_PAYLOAD_MAX); // 最大数据包 default set to 8M
    private int buffer = getIntValue(TRANSPORT_BUFFER_SIZE); // 缓冲器大小
    private boolean telnet = getBooleanValue(TRANSPORT_SERVER_TELNET); // 是否允许telnet
    private String dispatcher = getStringValue(TRANSPORT_SERVER_DISPATCHER); // 线程方法模型
    private boolean daemon = getBooleanValue(TRANSPORT_SERVER_DAEMON); // 是否守护线程，true随主线程退出而退出，false需要主动退出
    private Map<String, String> parameters;//其他一些参数配置

    private List<ChannelListener> channelListeners;
    private ServerHandler serverHandler;
    private NegotiatorListener negotiatorListener;

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

    public String getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(String dispatcher) {
        this.dispatcher = dispatcher;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
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

    public NegotiatorListener getNegotiatorListener() {
        return negotiatorListener;
    }

    public void setNegotiatorListener(NegotiatorListener negotiatorListener) {
        this.negotiatorListener = negotiatorListener;
    }

    public ServerHandler getServerHandler() {
        return serverHandler;
    }

    public void setServerHandler(ServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }
}
