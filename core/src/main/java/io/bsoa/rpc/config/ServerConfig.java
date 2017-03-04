/*
 * Copyright 2016 The BSOA Project
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
package io.bsoa.rpc.config;

import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.SystemInfo;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.common.utils.FileUtils;
import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.listener.ChannelListener;
import io.bsoa.rpc.server.Server;
import io.bsoa.rpc.server.ServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static io.bsoa.rpc.common.BsoaConfigs.getBooleanValue;
import static io.bsoa.rpc.common.BsoaConfigs.getIntValue;
import static io.bsoa.rpc.common.BsoaConfigs.getStringValue;
import static io.bsoa.rpc.common.BsoaOptions.DEFAULT_PROTOCOL;
import static io.bsoa.rpc.common.BsoaOptions.DEFAULT_SERIALIZATION;
import static io.bsoa.rpc.common.BsoaOptions.DEFAULT_TRANSPORT;
import static io.bsoa.rpc.common.BsoaOptions.SERVER_ACCEPTS;
import static io.bsoa.rpc.common.BsoaOptions.SERVER_CONTEXT_PATH;
import static io.bsoa.rpc.common.BsoaOptions.SERVER_DAEMON;
import static io.bsoa.rpc.common.BsoaOptions.SERVER_EPOLL;
import static io.bsoa.rpc.common.BsoaOptions.SERVER_HOST;
import static io.bsoa.rpc.common.BsoaOptions.SERVER_IOTHREADS;
import static io.bsoa.rpc.common.BsoaOptions.SERVER_POOL_ALIVETIME;
import static io.bsoa.rpc.common.BsoaOptions.SERVER_POOL_CORE;
import static io.bsoa.rpc.common.BsoaOptions.SERVER_POOL_MAX;
import static io.bsoa.rpc.common.BsoaOptions.SERVER_POOL_QUEUE;
import static io.bsoa.rpc.common.BsoaOptions.SERVER_POOL_QUEUE_TYPE;
import static io.bsoa.rpc.common.BsoaOptions.SERVER_POOL_TYPE;
import static io.bsoa.rpc.common.BsoaOptions.SERVER_PORT_START;
import static io.bsoa.rpc.common.BsoaOptions.SERVER_TELNET;

/**
 * Created by zhangg on 16-7-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class ServerConfig extends AbstractIdConfig implements Serializable {
    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ServerConfig.class);
    /**
     * The constant serialVersionUID.
     */
    private static final long serialVersionUID = -574374673831680403L;

    /*------------- 参数配置项开始-----------------*/
    /**
     * 配置名称
     */
    protected String protocol = getStringValue(DEFAULT_PROTOCOL);

    /**
     * 实际监听IP，与网卡对应
     */
    protected String host = getStringValue(SERVER_HOST);

    /**
     * 监听端口
     */
    protected int port = getIntValue(SERVER_PORT_START);

    /**
     * 基本路径 默认"/"
     */
    protected String contextPath = getStringValue(SERVER_CONTEXT_PATH);

    /**
     * io线程池大小
     */
    protected int ioThreads = getIntValue(SERVER_IOTHREADS);

    /**
     * 线程池类型
     */
    protected String threadPoolType = getStringValue(SERVER_POOL_TYPE);

    /**
     * 业务线程池大小
     */
    protected int coreThreads = getIntValue(SERVER_POOL_CORE);

    /**
     * 业务线程池大小
     */
    protected int maxThreads = getIntValue(SERVER_POOL_MAX);

    /**
     * 是否允许telnet，针对自定义协议
     */
    protected boolean telnet = getBooleanValue(SERVER_TELNET);

    /**
     * 线程池类型，默认普通线程池
     */
    protected String queueType = getStringValue(SERVER_POOL_QUEUE_TYPE);

    /**
     * 业务线程池回收时间
     */
    protected int queues = getIntValue(SERVER_POOL_QUEUE);

    /**
     * 线程池类型，默认普通线程池
     */
    protected int aliveTime = getIntValue(SERVER_POOL_ALIVETIME);

    /**
     * 服务端允许客户端建立的连接数
     */
    protected int accepts = getIntValue(SERVER_ACCEPTS);

    /**
     * 最大数据包大小
     *
     * @Deprecated
     */
    @Deprecated
    protected int payload = BsoaConstants.DEFAULT_PAYLOAD;

    /**
     * 序列化方式
     */
    protected String serialization = getStringValue(DEFAULT_SERIALIZATION);

    /**
     * 事件分发规则。
     *
     * @deprecated
     */
    @Deprecated
    protected String dispatcher = BsoaConstants.DISPATCHER_MESSAGE;

    /**
     * The Parameters. 自定义参数
     */
    protected Map<String, String> parameters;

    /**
     * 镜像ip，例如监听地址是1.2.3.4，告诉注册中心的确是3.4.5.6
     */
    protected String virtualhost;

    /**
     * 镜像ip文件，例如保存到"echo 127.0.0.1 > /etc/vitualhost"，启动时会自动读取
     */
    protected String virtualhostfile;

    /**
     * 连接事件监听器实例，连接或者断开时触发
     */
    protected transient List<ChannelListener> onConnect;

    /**
     * 是否启动epoll
     */
    protected boolean epoll = getBooleanValue(SERVER_EPOLL);

    /**
     * 是否hold住端口，true的话随主线程退出而退出，false的话则要主动退出
     */
    protected boolean daemon = getBooleanValue(SERVER_DAEMON);

    /**
     * 传输层
     */
    protected String transport = getStringValue(DEFAULT_TRANSPORT);

    /*------------- 参数配置项结束-----------------*/

    /**
     * 是否随机端口
     */
    private transient boolean randomPort;

    /**
     * 服务端对象
     */
    private transient volatile Server server;

    /**
     * 绑定的地址。是某个网卡，还是全部地址
     */
    private transient String boundHost;

    /**
     * 启动服务
     */
    public synchronized void start() {
        if (server != null) {
            return;
        }

        String host = this.getHost();
        if (StringUtils.isBlank(host)) {
            host = SystemInfo.getLocalHost();
            this.host = host;
            // windows绑定到0.0.0.0的某个端口以后，其它进程还能绑定到该端口
            this.boundHost = SystemInfo.isWindows() ? host : NetUtils.ANYHOST;
        } else {
            this.boundHost = host;
        }

        // 绑定到指定网卡 或全部网卡
        int port = NetUtils.getAvailablePort(this.boundHost, this.getPort());
        if (port != this.port) {
            LOGGER.info("[21400]Changed port from {} to {} because the config port is disabled", this.port, port);
            this.port = port;
        }

        // 提前检查协议+序列化方式
//        ConfigValueHelper.check(ProtocolType.valueOf(getProtocol()),
//                SerializationType.valueOf(getSerialization()));

        server = ServerFactory.getServer(this);
        server.start();

        // 解析虚拟ip文件
        if (virtualhost != null) {
            LOGGER.info("Virtual host is specified, host will be change from {} to {} when register",
                    this.host, this.virtualhost);
        } else {
            if (virtualhostfile != null) {
                try {
                    virtualhost = FileUtils.file2String(new File(virtualhostfile));
                } catch (Exception e) {
                    throw ExceptionUtils.buildRuntime(21403, "server.virtualhostfile", virtualhostfile,
                            "file can not read cause by " + e.getMessage());
                }
                LOGGER.info("Virtual host file is specified, host will be change from {} to {} when register",
                        this.host, this.virtualhost);
            }
        }
    }

    /**
     * 关闭服务
     */
    public synchronized void stop() {
        //Server server = ServerFactory.getServer(this);
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Gets protocol.
     *
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Sets protocol.
     *
     * @param protocol the protocol
     * @return the protocol
     */
    public ServerConfig setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    /**
     * Gets host.
     *
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets host.
     *
     * @param host the host
     * @return the host
     */
    public ServerConfig setHost(String host) {
        this.host = host;
        return this;
    }

    /**
     * Gets port.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets port.
     *
     * @param port the port
     * @return the port
     */
    public ServerConfig setPort(int port) {
        if (NetUtils.isRandomPort(port)) {
            randomPort = true;
        } else if (NetUtils.isInvalidPort(port)) {
            throw ExceptionUtils.buildRuntime(21402, "server.port", port + "",
                    "port must between -1 and 65535 (-1 means random port)");
        }
        this.port = port;
        return this;
    }

    /**
     * Gets context path.
     *
     * @return the context path
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * Sets context path.
     *
     * @param contextPath the context path
     * @return the context path
     */
    public ServerConfig setContextPath(String contextPath) {
        this.contextPath = contextPath;
        return this;
    }

    /**
     * Gets ioThreads.
     *
     * @return the ioThreads
     */
    public int getIoThreads() {
        return ioThreads;
    }

    /**
     * Sets ioThreads.
     *
     * @param ioThreads the ioThreads
     * @return the ioThreads
     */
    public ServerConfig setIoThreads(int ioThreads) {
        this.ioThreads = ioThreads;
        return this;
    }

    /**
     * Gets threadPoolType.
     *
     * @return the threadPoolType
     */
    public String getThreadPoolType() {
        return threadPoolType;
    }

    /**
     * Sets threadPoolType.
     *
     * @param threadPoolType the threadPoolType
     * @return the threadPoolType
     */
    public ServerConfig setThreadPoolType(String threadPoolType) {
        this.threadPoolType = threadPoolType;
        return this;
    }

    /**
     * Gets core threads.
     *
     * @return the core threads
     */
    public int getCoreThreads() {
        return coreThreads;
    }

    /**
     * Sets core threads.
     *
     * @param coreThreads the core threads
     * @return the core threads
     */
    public ServerConfig setCoreThreads(int coreThreads) {
        this.coreThreads = coreThreads;
        return this;
    }

    /**
     * Gets max threads.
     *
     * @return the max threads
     */
    public int getMaxThreads() {
        return maxThreads;
    }

    /**
     * Sets max threads.
     *
     * @param maxThreads the max threads
     * @return the max threads
     */
    public ServerConfig setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        return this;
    }

    /**
     * Is telnet boolean.
     *
     * @return the boolean
     */
    public boolean isTelnet() {
        return telnet;
    }

    /**
     * Sets telnet.
     *
     * @param telnet the telnet
     * @return the telnet
     */
    public ServerConfig setTelnet(boolean telnet) {
        this.telnet = telnet;
        return this;
    }

    /**
     * Gets queue type.
     *
     * @return the queue type
     */
    public String getQueueType() {
        return queueType;
    }

    /**
     * Sets queue type.
     *
     * @param queueType the queue type
     * @return the queue type
     */
    public ServerConfig setQueueType(String queueType) {
        this.queueType = queueType;
        return this;
    }

    /**
     * Gets queues.
     *
     * @return the queues
     */
    public int getQueues() {
        return queues;
    }

    /**
     * Sets queues.
     *
     * @param queues the queues
     * @return the queues
     */
    public ServerConfig setQueues(int queues) {
        this.queues = queues;
        return this;
    }

    /**
     * Gets alive time.
     *
     * @return the alive time
     */
    public int getAliveTime() {
        return aliveTime;
    }

    /**
     * Sets alive time.
     *
     * @param aliveTime the alive time
     * @return the alive time
     */
    public ServerConfig setAliveTime(int aliveTime) {
        this.aliveTime = aliveTime;
        return this;
    }

    /**
     * Gets accepts.
     *
     * @return the accepts
     */
    public int getAccepts() {
        return accepts;
    }

    /**
     * Sets accepts.
     *
     * @param accepts the accepts
     * @return the accepts
     */
    public ServerConfig setAccepts(int accepts) {
        ConfigValueHelper.checkPositiveInteger("server.accept", accepts);
        this.accepts = accepts;
        return this;
    }

    /**
     * Gets payload.
     *
     * @return the payload
     */
    public int getPayload() {
        return payload;
    }

    /**
     * Sets payload.
     *
     * @param payload the payload
     * @return the payload
     */
    public ServerConfig setPayload(int payload) {
        this.payload = payload;
        return this;
    }

    /**
     * Gets serialization.
     *
     * @return the serialization
     */
    public String getSerialization() {
        return serialization;
    }

    /**
     * Sets serialization.
     *
     * @param serialization the serialization
     * @return the serialization
     */
    public ServerConfig setSerialization(String serialization) {
        this.serialization = serialization;
        return this;
    }

    /**
     * Gets dispatcher.
     *
     * @return the dispatcher
     */
    public String getDispatcher() {
        return dispatcher;
    }

    /**
     * Sets dispatcher.
     *
     * @param dispatcher the dispatcher
     * @return the dispatcher
     */
    public ServerConfig setDispatcher(String dispatcher) {
        this.dispatcher = dispatcher;
        return this;
    }

    /**
     * Gets parameters.
     *
     * @return the parameters
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Sets parameters.
     *
     * @param parameters the parameters
     * @return the parameters
     */
    public ServerConfig setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
        return this;
    }

    /**
     * Gets virtualhost.
     *
     * @return the virtualhost
     */
    public String getVirtualhost() {
        return virtualhost;
    }

    /**
     * Sets virtualhost.
     *
     * @param virtualhost the virtualhost
     * @return the virtualhost
     */
    public ServerConfig setVirtualhost(String virtualhost) {
        this.virtualhost = virtualhost;
        return this;
    }

    /**
     * Gets virtualhostfile.
     *
     * @return the virtualhostfile
     */
    public String getVirtualhostfile() {
        return virtualhostfile;
    }

    /**
     * Sets virtualhostfile.
     *
     * @param virtualhostfile the virtualhostfile
     * @return the virtualhostfile
     */
    public ServerConfig setVirtualhostfile(String virtualhostfile) {
        this.virtualhostfile = virtualhostfile;
        return this;
    }

    /**
     * Gets onConnect.
     *
     * @return the onConnect
     */
    public List<ChannelListener> getOnConnect() {
        return onConnect;
    }

    /**
     * Sets onConnect.
     *
     * @param onConnect the onConnect
     * @return the onConnect
     */
    public ServerConfig setOnConnect(List<ChannelListener> onConnect) {
        this.onConnect = onConnect;
        return this;
    }

    /**
     * Is epoll boolean.
     *
     * @return the boolean
     */
    public boolean isEpoll() {
        return epoll;
    }

    /**
     * Sets epoll.
     *
     * @param epoll the epoll
     * @return the epoll
     */
    public ServerConfig setEpoll(boolean epoll) {
        this.epoll = epoll;
        return this;
    }

    /**
     * Is daemon boolean.
     *
     * @return the boolean
     */
    public boolean isDaemon() {
        return daemon;
    }

    /**
     * Sets daemon.
     *
     * @param daemon the daemon
     * @return the daemon
     */
    public ServerConfig setDaemon(boolean daemon) {
        this.daemon = daemon;
        return this;
    }

    /**
     * Gets transport.
     *
     * @return the transport
     */
    public String getTransport() {
        return transport;
    }

    /**
     * Sets transport.
     *
     * @param transport the transport
     * @return the transport
     */
    public ServerConfig setTransport(String transport) {
        this.transport = transport;
        return this;
    }

    /**
     * Gets server.
     *
     * @return the server
     */
    public Server getServer() {
        return server;
    }

    /**
     * Gets bound host
     *
     * @return bound host
     */
    public String getBoundHost() {
        return boundHost;
    }

    /**
     * Hash code.
     *
     * @return int int
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + port;
        result = prime * result
                + ((protocol == null) ? 0 : protocol.hashCode());
        return result;
    }

    /**
     * Equals boolean.
     *
     * @param obj the obj
     * @return boolean boolean
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ServerConfig other = (ServerConfig) obj;
        if (host == null) {
            if (other.host != null)
                return false;
        } else if (!host.equals(other.host))
            return false;
        if (port != other.port)
            return false;
        if (protocol == null) {
            if (other.protocol != null)
                return false;
        } else if (!protocol.equals(other.protocol))
            return false;
        return true;
    }

    /**
     * To string.
     *
     * @return string string
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return "ServerConfig [protocol=" + protocol + ", port=" + port + ", host=" + host + "]";
    }

}