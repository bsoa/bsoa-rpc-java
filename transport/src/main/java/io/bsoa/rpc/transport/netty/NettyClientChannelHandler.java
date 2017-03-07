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
package io.bsoa.rpc.transport.netty;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.context.AsyncContext;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.listener.ChannelListener;
import io.bsoa.rpc.message.HeartbeatResponse;
import io.bsoa.rpc.message.NegotiationRequest;
import io.bsoa.rpc.message.NegotiationResponse;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/17 18:27. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class NettyClientChannelHandler extends ChannelInboundHandlerAdapter {

    /**
     * slf4j Logger for this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyClientChannelHandler.class);

    private NettyClientTransport clientTransport;

    private List<ChannelListener> channelListeners;

    public NettyClientChannelHandler(NettyClientTransport clientTransport) {
        this.clientTransport = clientTransport;
        if (clientTransport != null) {
            this.channelListeners = clientTransport.getConfig().getChannelListeners();
        }
    }

    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        if (channelListeners != null) {
            AsyncContext.getAsyncThreadPool().execute(() -> {
                for (ChannelListener channelListener : channelListeners) {
                    try {
                        channelListener.onConnected(clientTransport.getChannel());
                    } catch (Throwable e) {
                        LOGGER.warn("Failed to call channel listener when channel active", e);
                    }
                }
            });
        }
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        LOGGER.info("Channel inactive: {}", channel);
        clientTransport.removeFutureWhenChannelInactive(); // 结束已有请求
        if (channelListeners != null) {
            AsyncContext.getAsyncThreadPool().execute(() -> {
                for (ChannelListener channelListener : channelListeners) {
                    try {
                        channelListener.onDisconnected(clientTransport.getChannel());
                    } catch (Exception e) {
                        LOGGER.warn("Failed to call channel listener when channel inactive", e);
                    }
                }
            });
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            // RPC响应
            if (msg instanceof RpcResponse) {
                RpcResponse response = (RpcResponse) msg;
                // 不管是正常响应还是Callback还是Stream
                clientTransport.receiveRpcResponse(response);
            }
            // 心跳响应
            else if (msg instanceof HeartbeatResponse) {
                HeartbeatResponse response = (HeartbeatResponse) msg;
                clientTransport.receiveHeartbeatResponse(response);
            }
            // 协商响应
            else if (msg instanceof NegotiationResponse) {
                NegotiationResponse response = (NegotiationResponse) msg;
                clientTransport.receiveNegotiationResponse(response);
            }
            // 协商请求：IO线程处理
            else if (msg instanceof NegotiationRequest) {
                NegotiationRequest request = (NegotiationRequest) msg;
                clientTransport.handleNegotiationRequest(request);
            }
            // RPC请求
            else if (msg instanceof RpcRequest) {
                RpcRequest request = (RpcRequest) msg;
                clientTransport.handleRpcRequest(request);
            } else {
                LOGGER.warn("Receive unsupported message! {}", msg.getClass());
                throw new BsoaRpcException(22222, "Only support base message");
            }
        } catch (BsoaRpcException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new BsoaRpcException(22222, "打印连接信息和请求id信息");
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        LOGGER.info("event triggered:{}", evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        cause.printStackTrace(); //TODO
//        if (cause instanceof IOException) {
//            LOGGER.warn("catch IOException at {} : {}",
//                    NetUtils.channelToString(channel.localAddress(), channel.remoteAddress()),
//                    cause.getMessage());
//        } else if (cause instanceof RpcException) {
//            RpcException rpcException = (RpcException) cause;
//            final MessageHeader header = rpcException.getMsgHeader();
//            if (header != null && header.getMsgType() == Constants.CALLBACK_REQUEST_MSG) {
//                RpcResponse response = new RpcResponse();
//                response.getMsgHeader().copyHeader(header);
//                response.getMsgHeader().setMsgType(BsoaConstants.CALLBACK_RESPONSE_MSG);
//                Future future = channel.writeAndFlush(response);
//                future.addListener(new FutureListener() {
//                    @Override
//                    public void operationComplete(Future future) throws Exception {
//                        if (future.isSuccess()) {
//                            LOGGER.debug("error of callback msg has been send to serverside..{}", header);
//                            return;
//                        } else {
//                            LOGGER.error("error of callback msg to the serverSide have failed. {}", header);
//                            LOGGER.error(cause.getMessage(), cause);
//                        }
//                    }
//                });
//            }
//        } else {
//            LOGGER.warn("catch " + cause.getClass().getName() + " at {} : {}",
//                    NetUtils.channelToString(channel.localAddress(), channel.remoteAddress()),
//                    cause.getMessage());
//        }
    }
}
