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

import com.sun.corba.se.impl.protocol.giopmsgheaders.RequestMessage;
import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.listener.ChannelListener;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.transport.ServerTransportConfig;
import io.netty.buffer.ByteBuf;
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

    private ServerTransportConfig transportConfig;

//    private serr serverHandler;

    private final List<ChannelListener> connectListeners;

    public NettyServerChannelHandler(ServerTransportConfig transportConfig) {
        this.transportConfig = transportConfig;
        this.connectListeners = transportConfig.getChannelListeners();
//        serverHandler = BaseServerHandler.getInstance(transportConfig);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel channel = ctx.channel();
        LOGGER.info("------{}------->{}", channel.remoteAddress(), msg);
        if (msg instanceof RpcRequest) {
//            RequestMessage requestMsg = (RequestMessage) msg;
//            //
//            if (handleOtherMsg(ctx, requestMsg)) return;
//
//            serverHandler.handlerRequest(channel, requestMsg);


        } else if (msg instanceof RpcResponse) {
//            //receive the callback ResponseMessage
//            ResponseMessage responseMsg = (ResponseMessage) msg;
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
        } else if (msg instanceof ByteBuf) {
            channel.writeAndFlush(msg);
        } else if (msg instanceof String) {
            channel.writeAndFlush("hello: " + msg + "!");
        } else {
            throw new BsoaRpcException(22222, "Only support base message");
        }

    }

    /*
     *
     */
    private boolean handleOtherMsg(ChannelHandlerContext ctx, RequestMessage requestMsg) {

//        int msgType = requestMsg.getMsgHeader().getMsgType();
//        if (msgType == Constants.REQUEST_MSG) return false; // 正常的请求
//        Channel channel = ctx.channel();
//        ResponseMessage response = null;
//        switch (msgType) {
//            case Constants.SHAKEHAND_MSG:
//                response = new ResponseMessage();
//                response.getMsgHeader().setMsgType(Constants.SHAKEHAND_RESULT_MSG);
//                response.getMsgHeader().setMsgId(requestMsg.getRequestId());
//                //DO SHAKEHAND CHECK HERE
//
//                break;
//
//            case Constants.HEARTBEAT_REQUEST_MSG:
//                response = MessageBuilder.buildHeartbeatResponse(requestMsg);
//                break;
//            default:
//                throw new RpcException(requestMsg.getMsgHeader(), " no such msgType:" + msgType);
//
//        }
//        channel.writeAndFlush(response);
        return true;
    }

    /*
     *handle the error
     */
    public void exceptionCaught(ChannelHandlerContext ctx, final Throwable cause) {
        Channel channel = ctx.channel();
        if (cause instanceof IOException) {
            LOGGER.warn("catch IOException at {} : {}",
                    NetUtils.channelToString(channel.remoteAddress(), channel.localAddress()),
                    cause.getMessage());
        } else if (cause instanceof BsoaRpcException) {
            BsoaRpcException rpc = (BsoaRpcException) cause;
//            MessageHeader header = rpc.getMsgHeader();
//            if (header != null) {
//                ResponseMessage responseMessage = new ResponseMessage();
//                responseMessage.getMsgHeader().copyHeader(header);
//                responseMessage.getMsgHeader().setMsgType(Constants.RESPONSE_MSG);
//                String causeMsg = cause.getMessage();
//                String channelInfo = BaseServerHandler.getKey(ctx.channel());
//                String causeMsg2 = "Remote Error Channel:" + channelInfo + " cause: " + causeMsg;
//                ((RpcException) cause).setErrorMsg(causeMsg2);
//                responseMessage.setException(cause);
//                ChannelFuture channelFuture = ctx.writeAndFlush(responseMessage);
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
        //LOGGER.info("connected from {}", NetUtils.channelToString(channel.remoteAddress(), channel.localAddress()));
//        BaseServerHandler.addChannel(channel);
//        if (connectListeners != null) {
//            serverHandler.getBizThreadPool().execute(new Runnable() {
//                @Override
//                public void run() {
//                    for (ConnectListener connectListener : connectListeners) {
//                        try {
//                            connectListener.connected(ctx);
//                        } catch (Exception e) {
//                            LOGGER.warn("Failed to call connect listener when channel active", e);
//                        }
//                    }
//                }
//            });
//        }
    }


    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        LOGGER.info("Disconnected from {}", NetUtils.channelToString(channel.remoteAddress(), channel.localAddress()));
//        BaseServerHandler.removeChannel(channel);
//        if (connectListeners != null) {
//            serverHandler.getBizThreadPool().execute(new Runnable() {
//                @Override
//                public void run() {
//                    for (ConnectListener connectListener : connectListeners) {
//                        try {
//                            connectListener.disconnected(ctx);
//                        } catch (Exception e) {
//                            LOGGER.warn("Failed to call connect listener when channel inactive", e);
//                        }
//                    }
//                }
//            });
//        }
//        CallbackUtil.removeTransport(channel);
    }

//    public BaseServerHandler getServerHandler() {
//        return serverHandler;
//    }
}
