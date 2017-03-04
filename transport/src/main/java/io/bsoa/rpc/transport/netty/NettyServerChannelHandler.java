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

import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.context.AsyncContext;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.listener.ChannelListener;
import io.bsoa.rpc.message.HeartbeatRequest;
import io.bsoa.rpc.message.NegotiationRequest;
import io.bsoa.rpc.message.NegotiationResponse;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.server.ServerHandler;
import io.bsoa.rpc.transport.AbstractChannel;
import io.bsoa.rpc.transport.ServerTransportConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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

    protected final ServerTransportConfig transportConfig;

    protected final ServerHandler serverHandler;

    protected final List<ChannelListener> connectListeners;

    /**
     * io.netty.channel.Channel --> io.bsoa.rpc.transport.AbstractChannel
     */
    private final ConcurrentHashMap<Channel, AbstractChannel> channelCache = new ConcurrentHashMap<>();

    /**
     * build NettyServerChannelHandler
     *
     * @param transportConfig ServerTransportConfig
     */
    public NettyServerChannelHandler(ServerTransportConfig transportConfig) {
        this.transportConfig = transportConfig;
        this.serverHandler = transportConfig.getServerHandler();
        this.connectListeners = transportConfig.getChannelListeners();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (serverHandler == null) {
            LOGGER.warn("Has no server handler in server transport");
            throw new BsoaRpcException(22222, "Has no server handler in server transport");
        }
        Channel channel = ctx.channel();
        AbstractChannel abstractChannel = channelCache.get(channel);
        // RPC请求：不管是正常请求还是Callback还是Stream
        if (msg instanceof RpcRequest) {
            RpcRequest request = (RpcRequest) msg;
            serverHandler.handleRpcRequest(request, abstractChannel);
        }
        // 心跳请求
        else if (msg instanceof HeartbeatRequest) {
            HeartbeatRequest request = (HeartbeatRequest) msg;
            serverHandler.handleHeartbeatRequest(request, abstractChannel);
        }
        // 协商请求：IO线程处理
        else if (msg instanceof NegotiationRequest) {
            NegotiationRequest request = (NegotiationRequest) msg;
            serverHandler.handleNegotiationRequest(request, abstractChannel);
        }
        // RPC响应
        else if (msg instanceof RpcResponse) {
            RpcResponse response = (RpcResponse) msg;
            serverHandler.receiveRpcResponse(response, abstractChannel);
        }
        // 协商响应
        else if (msg instanceof NegotiationResponse) {
            NegotiationResponse response = (NegotiationResponse) msg;
            serverHandler.receiveNegotiationResponse(response, abstractChannel);
        }
        else {
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
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("connected from {}",
                    NetUtils.channelToString(channel.remoteAddress(), channel.localAddress()));
        }
        // save to cache
        AbstractChannel abstractChannel = new NettyChannel(channel);
        channelCache.put(channel, abstractChannel);
        serverHandler.registerChannel(abstractChannel);
        // notify listener
        if (connectListeners != null) {
            AsyncContext.getAsyncThreadPool().execute(() -> {
                for (ChannelListener channelListener : connectListeners) {
                    try {
                        channelListener.onConnected(abstractChannel);
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
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Disconnected from {}",
                    NetUtils.channelToString(channel.localAddress(), channel.remoteAddress()));
        }
        // remove from cache
        AbstractChannel abstractChannel = channelCache.remove(channel);
        serverHandler.unRegisterChannel(abstractChannel);
        // notify listener
        if (connectListeners != null) {
            AsyncContext.getAsyncThreadPool().execute(() -> {
                for (ChannelListener channelListener : connectListeners) {
                    try {
                        channelListener.onDisconnected(abstractChannel);
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
