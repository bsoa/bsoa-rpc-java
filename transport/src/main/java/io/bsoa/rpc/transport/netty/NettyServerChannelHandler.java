/*
 * *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  * <p>
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  * <p>
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package io.bsoa.rpc.transport.netty;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.context.AsyncContext;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.listener.ChannelListener;
import io.bsoa.rpc.listener.NegotiationListener;
import io.bsoa.rpc.message.HeartbeatRequest;
import io.bsoa.rpc.message.HeartbeatResponse;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.NegotiatorRequest;
import io.bsoa.rpc.message.NegotiatorResponse;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.server.ServerHandler;
import io.bsoa.rpc.transport.AbstractChannel;
import io.bsoa.rpc.transport.ClientTransport;
import io.bsoa.rpc.transport.ClientTransportFactory;
import io.bsoa.rpc.transport.ServerTransportConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/18 00:01. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@ChannelHandler.Sharable
public class NettyServerChannelHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyServerChannelHandler.class);

    private final ServerTransportConfig transportConfig;

    private final ServerHandler serverHandler;

    private final List<ChannelListener> connectListeners;

    public NettyServerChannelHandler(ServerTransportConfig transportConfig) {
        this.transportConfig = transportConfig;
        this.serverHandler = transportConfig.getServerHandler();
        this.connectListeners = transportConfig.getChannelListeners();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel channel = ctx.channel();

        // 心跳请求：IO线程
        if (msg instanceof HeartbeatRequest) {
            HeartbeatRequest request = (HeartbeatRequest) msg;
            HeartbeatResponse response = MessageBuilder.buildHeartbeatResponse(request);
            channel.writeAndFlush(response);
        }
        // 协商请求：IO线程处理
        else if (msg instanceof NegotiatorRequest) {
            NegotiatorRequest request = (NegotiatorRequest) msg;
            NegotiationListener listener = transportConfig.getNegotiationListener();
            if (listener == null) {
                LOGGER.warn("Has no NegotiatorListener in server transport");
            } else {
                NegotiatorResponse response = listener.handshake(request);
                channel.writeAndFlush(response);
            }
        }
        // 协商响应：IO线程处理 TODO
        else if (msg instanceof NegotiatorResponse) {
            NegotiatorResponse response = (NegotiatorResponse) msg;
        }
        // RPC请求：业务线程处理
        else if (msg instanceof RpcRequest) {
            RpcRequest request = (RpcRequest) msg;
            if (serverHandler == null) {
                LOGGER.warn("Has no server handler in server transport");
                throw new BsoaRpcException(22222, "Has no server handler in server transport");
            } else {
                // 不管是正常请求还是Callback还是Stream
                serverHandler.handleRpcRequest(request, new NettyChannel(channel));
            }
        }
        // RPC响应：业务线程处理(Callback或者Stream）
        else if (msg instanceof RpcResponse) {
            RpcResponse response = (RpcResponse) msg;
            String channelKey = NetUtils.channelToString(channel.remoteAddress(), channel.localAddress());
            ClientTransport clientTransport = ClientTransportFactory.getReverseClientTransport(channelKey);
            if (clientTransport != null) {
                clientTransport.receiveRpcResponse(response);
            } else {
                LOGGER.error("no such clientTransport for channel:{}", channel);
                throw new BsoaRpcException(22222, "No such clientTransport");
            }
        } else {
            throw new BsoaRpcException(22222, "Only support base message");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, final Throwable cause) {
        Channel channel = ctx.channel();
        if (cause instanceof IOException) {
            LOGGER.warn("catch IOException at {} : {}",
                    NetUtils.channelToString(channel.remoteAddress(), channel.localAddress()),
                    cause.getMessage());
        } else if (cause instanceof BsoaRpcException) {
//            BsoaRpcException rpc = (BsoaRpcException) cause;
//            MessageHeader header = rpc.getMsgHeader();
//            if (header != null) {
//                RpcResponse rpcResponse = new RpcResponse();
//                rpcResponse.getMsgHeader().copyHeader(header);
//                rpcResponse.getMsgHeader().setMsgType(BsoaConstants.RESPONSE_MSG);
//                String causeMsg = cause.getMessage();
//                String channelInfo = BaseServerHandler.getKey(ctx.channel());
//                String causeMsg2 = "Remote Error Channel:" + channelInfo + " cause: " + causeMsg;
//                ((RpcException) cause).setErrorMsg(causeMsg2);
//                rpcResponse.setException(cause);
//                ChannelFuture channelFuture = ctx.writeAndFlush(rpcResponse);
//                channelFuture.addListener(new ChannelFutureListener() {
//
//                    @Override
//                    public void operationComplete(ChannelFuture future) throws Exception {
//                        if (future.isSuccess()) {
//                            if(LOGGER.isTraceEnabled()) {
//                                LOGGER.trace("have write the error message back to clientside..");
//                            }
//                            return;
//                        } else {
//                            LOGGER.error("fail to write error back status: {}", future.isSuccess());
//
//                        }
//                    }
//                });
//            }
        } else {
            LOGGER.warn("catch " + cause.getClass().getName() + " at {} : {}",
                    NetUtils.channelToString(channel.remoteAddress(), channel.localAddress()),
                    cause.getMessage());
        }
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        LOGGER.info("connected from {}", NetUtils.channelToString(channel.remoteAddress(), channel.localAddress()));
//        BaseServerHandler.addChannel(channel);
        if (connectListeners != null) {
            AbstractChannel nettyChannel = new NettyChannel(channel);
            AsyncContext.getAsyncThreadPool().execute(() -> {
                for (ChannelListener channelListener : connectListeners) {
                    try {
                        channelListener.onConnected(nettyChannel);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to call connect listener when channel active", e);
                    }
                }
            });
        }
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String key = NetUtils.channelToString(channel.localAddress(), channel.remoteAddress());
        LOGGER.info("Disconnected from {}", key);
        ClientTransportFactory.removeReverseClientTransport(key); // 删除callback等生成的反向长连接
//        BaseServerHandler.removeChannel(channel);
        if (connectListeners != null) {
            AbstractChannel nettyChannel = new NettyChannel(channel);
            AsyncContext.getAsyncThreadPool().execute(() -> {
                for (ChannelListener channelListener : connectListeners) {
                    try {
                        channelListener.onDisconnected(nettyChannel);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to call connect listener when channel active", e);
                    }
                }
            });
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        LOGGER.info("event triggered:{}", evt);
    }
}
