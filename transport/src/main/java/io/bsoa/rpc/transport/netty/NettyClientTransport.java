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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.listener.ResponseFuture;
import io.bsoa.rpc.message.BaseMessage;
import io.bsoa.rpc.message.HeartbeatResponse;
import io.bsoa.rpc.message.NegotiatorResponse;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.message.StreamResponse;
import io.bsoa.rpc.protocol.Protocol;
import io.bsoa.rpc.protocol.ProtocolFactory;
import io.bsoa.rpc.transport.AbstractChannel;
import io.bsoa.rpc.transport.AbstractClientTransport;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
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
    private final AtomicInteger requestId = new AtomicInteger();

    private final ConcurrentHashMap<Integer, MessageFuture<BaseMessage>> futureMap = new ConcurrentHashMap<>();


    private List<AbstractChannel> channels = new ArrayList<>();

    private AtomicBoolean connected;

    @Override
    public void connect() {
        // 已经初始化，或者被复用
        if (this.connected.get()) {
            throw new BsoaRuntimeException(22222, "Has been call connnect, Illega Access");
        }

        String host = config.getProvider().getIp();
        int port = config.getProvider().getPort();
        int num = config.getConnectionNum(); // 建立几个长连接
        int connectTimeout = config.getConnectTimeout();

        num = Math.max(1, num);
        for (int i = 0; i < num; i++) {
            Channel channel = null;
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(NettyTransportHelper.getClientIOEventLoopGroup())
                        .channel(config.isUseEpoll() ? EpollSocketChannel.class : NioSocketChannel.class)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .option(ChannelOption.ALLOCATOR, NettyTransportHelper.getByteBufAllocator())
                        .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(8 * 1024, 32 * 1024))
                        .option(ChannelOption.RCVBUF_ALLOCATOR, NettyTransportHelper.RECV_BYTEBUF_ALLOCATOR)
                        .handler(new NettyClientChannelInitializer(this));
                // Bind and start to accept incoming connections.

                ChannelFuture channelFuture = bootstrap.connect(host, port);
                channelFuture.awaitUninterruptibly(connectTimeout, TimeUnit.MILLISECONDS);
                if (channelFuture.isSuccess()) {
                    channel = channelFuture.channel();
                    if (NetUtils.toAddressString((InetSocketAddress) channel.remoteAddress())
                            .equals(NetUtils.toAddressString((InetSocketAddress) channel.localAddress()))) {
                        // 服务端不存活时，连接左右两侧地址一样的情况
                        channel.close(); // 关掉重连
                        throw new RuntimeException("Failed to connect " + host + ":" + port
                                + ". Cause by: Remote and local address are the same");
                    }
                } else {
                    Throwable cause = channelFuture.cause();
                    throw new RuntimeException("Failed to connect " + host + ":" + port +
                            (cause != null ? ". Cause by: " + cause.getMessage() : "."));
                }
                channels.add(new NettyChannel(channel));
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                //logger.error(e.getMessage(),e);
                String errorStr = "Failed to build channel for host:" + host + " port:" + port
                        + ". Cause by: " + e.getMessage();
                RuntimeException initException = new RuntimeException(errorStr, e);
                throw initException;
            }
        }
    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public List<AbstractChannel> getChannels() {
        return channels;
    }

    private Random random = new Random();

    private Channel getChannel() {
        if (channels.size() == 0) {
            throw new BsoaRpcException(22222, "No connected channel in client transport");
        } else if (channels.size() == 1) {
            return ((NettyChannel) channels.get(0)).getChannel();
        } else {
            do {
                NettyChannel nettyChannel = (NettyChannel) channels.get(random.nextInt(channels.size()));
                Channel tmp = nettyChannel.getChannel();
                if (tmp.isOpen()) {
                    return tmp;
                } else {
                    channels.remove(nettyChannel);
                }
            } while (channels.size() > 0); // TODO
            throw new BsoaRpcException(22222, "No connected channel in client transport");
        }
    }

    @Override
    public ResponseFuture<BaseMessage> asyncSend(BaseMessage message, int timeout) {
        if (message == null) {
            throw new BsoaRpcException(22222, "msg cannot be null.");
        }
        final MessageFuture messageFuture = new MessageFuture(getChannel(), message.getMessageId(), timeout);
        this.addFuture(message, messageFuture);

        Channel channel = getChannel();

        if (message instanceof RpcRequest) {
            RpcRequest request = (RpcRequest) message;

            Protocol protocol = ProtocolFactory.getProtocol(request.getProtocolType());
            // TODO 是否callback请求（需要特殊处理）
            // request = callBackHandler(request);

            ByteBuf byteBuf = NettyTransportHelper.getBuffer();
            protocol.encoder().encodeBody(request, new NettyByteBuf(byteBuf));

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
//                    RpcContext.getContext().removeAttachment(Constants.HIDDEN_KEY_DST_JSF_VERSION);
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
        messageFuture.setSentTime(BsoaContext.now());// 置为已发送
        return messageFuture;
    }

    @Override
    public BaseMessage syncSend(BaseMessage request, int timeout) {
        Integer msgId = null;
        try {
            currentRequests.incrementAndGet();
            request.setMessageId(genarateRequestId());

            ResponseFuture<BaseMessage> f = asyncSend(request, timeout);
            //futureMap.putIfAbsent(msgId,future); 子类已实现
            MessageFuture<BaseMessage> future = (MessageFuture<BaseMessage>) f;
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

    private Integer genarateRequestId() {
        return requestId.getAndIncrement() & 0x7FFFFFFF;
    }

    /*
 *different FutureMap for different Request msg type
 */
    protected void addFuture(BaseMessage message, MessageFuture msgFuture) {
//        int msgType = msg.getMsgHeader().getMsgType();
//        Integer msgId = msg.getMsgHeader().getMsgId();
//        if (msgType == Constants.REQUEST_MSG
//                || msgType == Constants.CALLBACK_REQUEST_MSG
//                || msgType == Constants.HEARTBEAT_REQUEST_MSG) {
        this.futureMap.put(message.getMessageId(), msgFuture);
//
//        } else {
//            LOGGER.error("cannot handle Future for this Msg:{}", msg);
//        }


    }

    public void removeFutureWhenChannelInactive() {


    }

    @Override
    public void receiveRpcResponse(RpcResponse response) {
        int messageId = response.getMessageId();
        MessageFuture<BaseMessage> future = futureMap.get(messageId);
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
        MessageFuture<BaseMessage> future = futureMap.get(messageId);
        if (future != null) {
            future.setSuccess(response);
            futureMap.remove(messageId);
        }
    }

    @Override
    public void handleStreamResponse(StreamResponse response) {
        //TODO
    }

    @Override
    public void receiveNegotiatorResponse(NegotiatorResponse response) {
        int messageId = response.getMessageId();
        MessageFuture<BaseMessage> future = futureMap.get(messageId);
        if (future != null) {
            future.setSuccess(response);
            futureMap.remove(messageId);
        }
    }
}
