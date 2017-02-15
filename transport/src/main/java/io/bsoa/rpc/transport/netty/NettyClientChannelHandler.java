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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.context.AsyncContext;
import io.bsoa.rpc.invoke.StreamTask;
import io.bsoa.rpc.listener.ChannelListener;
import io.bsoa.rpc.listener.NegotiatorListener;
import io.bsoa.rpc.message.BaseMessage;
import io.bsoa.rpc.message.HeadKey;
import io.bsoa.rpc.message.HeartbeatResponse;
import io.bsoa.rpc.message.NegotiatorRequest;
import io.bsoa.rpc.message.NegotiatorResponse;
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
//            CallbackUtil.getAsyncThreadPool().execute(new Runnable() {
//                @Override
//                public void run() {
//                    for (ConnectListener connectListener : channelListeners) {
//                        try {
//                            connectListener.onConnected(ctx);
//                        } catch (Exception e) {
//                            LOGGER.warn("Failed to call connect listener when channel active", e);
//                        }
//                    }
//                }
//            });
            //TODO
        }
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        LOGGER.info("Channel inactive: {}", channel);
        clientTransport.removeFutureWhenChannelInactive(); // 结束已有请求
        if (channelListeners != null) {
//            CallbackUtil.getAsyncThreadPool().execute(new Runnable() {
//                @Override
//                public void run() {
//                    for (ChannelListener connectListener : channelListeners) {
//                        try {
//                            connectListener.onDisconnected(ctx);
//                        } catch (Exception e) {
//                            LOGGER.warn("Failed to call connect listener when channel inactive", e);
//                        }
//                    }
//                }
//            });
        }
    }

    /**
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel channel = ctx.channel();
        try {
            // 心跳响应：TO线程处理
            if (msg instanceof HeartbeatResponse) {
                HeartbeatResponse response = (HeartbeatResponse) msg;
                clientTransport.receiveHeartbeatResponse(response);
            }
            // 协商请求：IO线程处理
            else if (msg instanceof NegotiatorRequest) {
                NegotiatorRequest request = (NegotiatorRequest) msg;
                NegotiatorListener listener = clientTransport.getConfig().getNegotiatorListener();
                if (listener == null) {
                    LOGGER.warn("Has no NegotiatorListener in client transport");
                } else {
                    NegotiatorResponse response = listener.handshake(request);
                    channel.writeAndFlush(response);
                }
            }
            // 协商响应：发起者线程处理
            else if (msg instanceof NegotiatorResponse) {
                NegotiatorResponse response = (NegotiatorResponse) msg;
                clientTransport.receiveNegotiatorResponse(response);
            }
            // RPC请求：callback线程池处理
            else if (msg instanceof RpcRequest) {
                RpcRequest request = (RpcRequest) msg;
                String callbackInsKey = (String) request.getHeadKey(HeadKey.CALLBACK_INS_KEY);
                if (callbackInsKey != null) {
                    // callback请求
                }
                String streamInsKey = (String) request.getHeadKey(HeadKey.STREAM_INS_KEY);
                if (streamInsKey != null) {
                    // stream请求
                    StreamTask task = new StreamTask(request, clientTransport.getChannel());
                    AsyncContext.getAsyncThreadPool().execute(task); // 怎么保证流式的顺序？？ TODO
                }
                // callback请求
//            //receive the callback RpcResponse
//            RpcResponse responseMsg = (RpcResponse) msg;
//            if (responseMsg.getMsgHeader().getMsgType() != Constants.CALLBACK_RESPONSE_MSG) {
//                throw new RpcException(responseMsg.getMsgHeader(), "Can not handle normal response message" +
//                        " in server channel handler : " + responseMsg.toString());
//            }
//            //find the transport
//            JSFClientTransport clientTransport = CallbackUtil.getClientTransport(channel);
//            if (clientTransport != null) {
//                clientTransport.receiveResponse(responseMsg);
//            } else {
//                LOGGER.error("no such clientTransport for channel:{}", channel);
//                throw new RpcException(responseMsg.getMsgHeader(), "No such clientTransport");
//            }
            }
            // RPC响应：业务线程处理
            else if (msg instanceof RpcResponse) {
                RpcResponse response = (RpcResponse) msg;
                clientTransport.receiveRpcResponse(response);
            } else {
                LOGGER.warn("Receive unsupported message! {}", msg.getClass());
                throw new BsoaRpcException(22222, "Only support base message");
            }
        } catch (BsoaRpcException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            BaseMessage base = (BaseMessage) msg;
            BsoaRpcException rpcException = new BsoaRpcException(22222, "打印连接信息和请求id信息");
            throw rpcException;
        }
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        LOGGER.info("event triggered:{}", evt);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        cause.printStackTrace();
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
