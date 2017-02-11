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

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.SystemInfo;
import io.bsoa.rpc.common.struct.PositiveAtomicCounter;
import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.invoke.StreamUtils;
import io.bsoa.rpc.message.BaseMessage;
import io.bsoa.rpc.message.HeartbeatResponse;
import io.bsoa.rpc.message.MessageConstants;
import io.bsoa.rpc.message.NegotiatorResponse;
import io.bsoa.rpc.message.ResponseFuture;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.protocol.Protocol;
import io.bsoa.rpc.protocol.ProtocolFactory;
import io.bsoa.rpc.transport.AbstractByteBuf;
import io.bsoa.rpc.transport.AbstractChannel;
import io.bsoa.rpc.transport.AbstractClientTransport;
import io.bsoa.rpc.transport.ClientTransportConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/15 22:51. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("netty4")
public class NettyClientTransport extends AbstractClientTransport {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(NettyClientTransport.class);

    /**
     * 正在发送的调用数量
     */
    protected AtomicInteger currentRequests = new AtomicInteger(0);
    /**
     * 请求id计数器（一个Transport一个）
     */
    private final PositiveAtomicCounter requestId = new PositiveAtomicCounter();

    private final ConcurrentHashMap<Integer, NettyMessageFuture<BaseMessage>> futureMap = new ConcurrentHashMap<>();

    private NettyChannel channel;

    /**
     * 客户端配置
     *
     * @param transportConfig 客户端配置
     */
    public NettyClientTransport(ClientTransportConfig transportConfig) {
        super(transportConfig);
    }

    @Override
    public void connect() {
        // 已经初始化，或者被复用
        if (channel != null) {
            LOGGER.info("Has been call connect(), ignore this if connection reuse");
        }

        String host = transportConfig.getProviderInfo().getIp();
        if (host.equals(SystemInfo.getLocalHost())) { //本机跨jvm服务，降级为127.0.0.1,不走网卡
            host = NetUtils.LOCALHOST;
        }
        int port = transportConfig.getProviderInfo().getPort();
        int num = transportConfig.getConnectionNum(); // 建立几个长连接
        int connectTimeout = transportConfig.getConnectTimeout();

        num = Math.max(1, num);
        for (int i = 0; i < num; i++) {
            Channel tmp = null;
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(NettyTransportHelper.getClientIOEventLoopGroup())
                        .channel(transportConfig.isUseEpoll() ? EpollSocketChannel.class : NioSocketChannel.class)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .option(ChannelOption.ALLOCATOR, NettyTransportHelper.getByteBufAllocator())
                        .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(8 * 1024, 32 * 1024))
                        .option(ChannelOption.RCVBUF_ALLOCATOR, NettyTransportHelper.RECV_BYTEBUF_ALLOCATOR)
                        .handler(new NettyClientChannelInitializer(this));
                // Bind and start to accept incoming connections.

                ChannelFuture channelFuture = bootstrap.connect(host, port);
                channelFuture.awaitUninterruptibly(connectTimeout, TimeUnit.MILLISECONDS);
                if (channelFuture.isSuccess()) {
                    tmp = channelFuture.channel();
                    if (NetUtils.toAddressString((InetSocketAddress) tmp.remoteAddress())
                            .equals(NetUtils.toAddressString((InetSocketAddress) tmp.localAddress()))) {
                        // 服务端不存活时，连接左右两侧地址一样的情况
                        tmp.close(); // 关掉重连
                        throw new RuntimeException("Failed to connect " + host + ":" + port
                                + ". Cause by: Remote and local address are the same");
                    }
                } else {
                    Throwable cause = channelFuture.cause();
                    throw new RuntimeException("Failed to connect " + host + ":" + port +
                            (cause != null ? ". Cause by: " + cause.getMessage() : "."), cause);
                }
                this.channel = new NettyChannel(tmp);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                //logger.error(e.getMessage(),e);
                String errorStr = "Failed to build channel for host:" + host + " port:" + port
                        + ". Cause by: " + e.getMessage();
                throw new RuntimeException(errorStr, e);
            }
        }
    }

    @Override
    public void disconnect() {
        if (channel != null) {
            channel.close();
            channel = null;
        }
    }

    @Override
    public void destroy() {
        try {
            disconnect();
        } finally {
            NettyTransportHelper.closeClientIOEventGroup();
        }
    }

    @Override
    public boolean isAvailable() {
        return channel != null && channel.isAvailable();
    }

    @Override
    public void setChannel(AbstractChannel channel) {
        this.channel = (NettyChannel) channel;
    }

    @Override
    public AbstractChannel getChannel() {
        return channel;
    }

    @Override
    public ResponseFuture<BaseMessage> asyncSend(BaseMessage message, int timeout) {
        Integer msgId = null;
        try {
            currentRequests.incrementAndGet();
            msgId = generateRequestId();
            message.setMessageId(msgId);
            ResponseFuture<BaseMessage> f = doSend(message, timeout);
            NettyMessageFuture<BaseMessage> future = (NettyMessageFuture<BaseMessage>) f;
            future.setAsyncCall(true); // 标记为异步调用
            return future;
        } catch (BsoaRpcException e) {
            try {
                if (msgId != null) {
                    futureMap.remove(msgId);
                }
            } catch (Exception e1) {
                LOGGER.error(e1.getMessage(), e1);
            }
            throw e;
        } finally {
            currentRequests.decrementAndGet();
        }
    }

    @Override
    public BaseMessage syncSend(BaseMessage message, int timeout) {
        Integer msgId = null;
        try {
            currentRequests.incrementAndGet();
            msgId = generateRequestId();
            message.setMessageId(msgId);
            ResponseFuture<BaseMessage> f = doSend(message, timeout);
            NettyMessageFuture<BaseMessage> future = (NettyMessageFuture<BaseMessage>) f;
            return future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new BsoaRpcException(22222, "[JSF-22113]Client request thread interrupted");
        } catch (BsoaRpcException e) {
            try {
                if (msgId != null) {
                    futureMap.remove(msgId);
                }
            } catch (Exception e1) {
                LOGGER.error(e1.getMessage(), e1);
            }
            throw e;
        } finally {
            currentRequests.decrementAndGet();
        }
    }

    @Override
    public void oneWaySend(BaseMessage message, int timeout) {
        Integer msgId = null;
        try {
            currentRequests.incrementAndGet();
            msgId = generateRequestId();
            message.setMessageId(msgId);

            doSend(message, timeout); // 发送 不管结果
        } catch (BsoaRpcException e) {
            try {
                if (msgId != null) {
                    futureMap.remove(msgId);
                }
            } catch (Exception e1) {
                LOGGER.error(e1.getMessage(), e1);
            }
            throw e;
        } finally {
            currentRequests.decrementAndGet();
        }
    }

    /**
     * 执行远程调用
     *
     * @param message 消息
     * @param timeout 超时
     * @return 如果是需要返回结果的得到MessageFuture，不需要结果的返回null
     */
    private NettyMessageFuture<BaseMessage> doSend(BaseMessage message, int timeout) {
        if (message == null) {
            throw new BsoaRpcException(22222, "msg cannot be null.");
        }
        if (!isAvailable()) {
            throw new BsoaRpcException(22222, "msg cannot be null.");
        }
        boolean oneWay = message.getDirectionType() == MessageConstants.DIRECTION_ONEWAY;
        NettyMessageFuture<BaseMessage> nettyMessageFuture = null;
        Channel channel = this.channel.getChannel();
        if (!oneWay) {
            nettyMessageFuture = new NettyMessageFuture<>(channel, message.getMessageId(), timeout);
            this.addFuture(message, nettyMessageFuture);
        }
        Integer msgId = null;
        if (message instanceof RpcRequest) {
            RpcRequest request = (RpcRequest) message;

            Protocol protocol = ProtocolFactory.getProtocol(request.getProtocolType());

            // TODO 是否callback请求（需要特殊处理）
            // Callback Stream调用等特殊处理
            if (StreamUtils.hasStreamObserverParameter(request.getInterfaceName(), request.getMethodName())) {
                StreamUtils.preMsgSend(request, this.channel);
            }

            ByteBuf byteBuf = NettyTransportHelper.getBuffer();
            AbstractByteBuf buf = new NettyByteBuf(byteBuf);
            protocol.encoder().encodeAll(request, buf);

            channel.writeAndFlush(buf, channel.voidPromise());
//            // 序列话Request  主要是body
//            ByteBuf byteBuf = NettyTransportHelper.getBuffer();
//            Protocol protocol = ProtocolFactory.getProtocol(request.getProtocolType());
//            request = callBackHandler(request);
//            try {
//                if (providerJsfVersion != null) { // 供序列化时特殊判断
//                    messageFuture.setProviderJsfVersion(providerJsfVersion); // 记录下请求值
//                }
//                byteBuf = protocol.encode(request, byteBuf);
//            } finally {
//                if (providerJsfVersion != null) {
//                    RpcContext.getContext().removeAttachment(BsoaConstants.HIDDEN_KEY_DST_JSF_VERSION);
//                }
//            }
//            request.setMsg(byteBuf);
//            Invocation invocation = request.getInvocationBody();
//            if (invocation != null) {
//                // 客户端批量发送默认开启
//                RingBufferHolder clientRingBufferHolder = RingBufferHolder.getClientRingbuffer(invocation.getClazzName());
//                if (clientRingBufferHolder != null) { // 批量发送
//                    request.setChannel(this.channel);
//                    clientRingBufferHolder.submit(request);
//                } else {
//                    channel.writeAndFlush(request, channel.voidPromise());
//                }
//            } else {  // heartbeat等
//                channel.writeAndFlush(request, channel.voidPromise());
//            }
        } else {
            channel.writeAndFlush(message, channel.voidPromise());
        }
        if (!oneWay) {
            nettyMessageFuture.setSentTime(BsoaContext.now());// 置为已发送
        }
        return nettyMessageFuture;
    }

    private Integer generateRequestId() {
        return requestId.getAndIncrement();
    }

    private void addFuture(BaseMessage message, NettyMessageFuture<BaseMessage> msgFuture) {
        this.futureMap.put(message.getMessageId(), msgFuture);
    }

    /**
     * Remove future when channel inactive.
     */
    public void removeFutureWhenChannelInactive() {
        // TODO
    }

    @Override
    public void receiveRpcResponse(RpcResponse response) {
        int messageId = response.getMessageId();
        NettyMessageFuture<BaseMessage> future = futureMap.get(messageId);
        if (future == null) {
            LOGGER.warn("[JSF-22114]Not found future which msgId is {} when receive response. May be " +
                    "this future have been removed because of timeout", messageId);
//            if (msg != null && msg.getMsgBody() != null) {
//                msg.getMsgBody().release();
//            }
            //throw new RpcException("No such Future maybe have been removed for Timeout..");
        } else {
            future.setSuccess(response);
            futureMap.remove(messageId);
        }
    }

    @Override
    public void receiveHeartbeatResponse(HeartbeatResponse response) {
        int messageId = response.getMessageId();
        NettyMessageFuture<BaseMessage> future = futureMap.get(messageId);
        if (future != null) {
            future.setSuccess(response);
            futureMap.remove(messageId);
        }
    }

    @Override
    public void receiveNegotiatorResponse(NegotiatorResponse response) {
        int messageId = response.getMessageId();
        NettyMessageFuture<BaseMessage> future = futureMap.get(messageId);
        if (future != null) {
            future.setSuccess(response);
            futureMap.remove(messageId);
        }
    }
}
