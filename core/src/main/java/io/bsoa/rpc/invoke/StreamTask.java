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
package io.bsoa.rpc.invoke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.message.HeadKey;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.MessageConstants;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.protocol.Protocol;
import io.bsoa.rpc.protocol.ProtocolFactory;
import io.bsoa.rpc.transport.AbstractByteBuf;
import io.bsoa.rpc.transport.AbstractChannel;

import static io.bsoa.rpc.invoke.StreamContext.METHOD_ONCOMPLETED;
import static io.bsoa.rpc.invoke.StreamContext.METHOD_ONERROR;
import static io.bsoa.rpc.invoke.StreamContext.METHOD_ONVALUE;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/2/12 22:31. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class StreamTask implements Runnable {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(StreamTask.class);

    /**
     * RpcRequest of StreamObserver
     */
    private RpcRequest request;

    /**
     * The Channel
     */
    private AbstractChannel channel;

    /**
     * Construct method
     *
     * @param request RpcRequest of StreamObserver
     * @param channel Channel
     */
    public StreamTask(RpcRequest request, AbstractChannel channel) {
        this.request = request;
        this.channel = channel;
    }

    @Override
    public void run() {
        Integer timeout = (Integer) request.getHeadKey(HeadKey.TIMEOUT);
        if (timeout != null && BsoaContext.now() - request.getReceiveTime() > timeout) { // 客户端已经超时的请求直接丢弃
            LOGGER.warn("[JSF-23008]Discard request cause by timeout after receive the request: {}", request.getMessageId());
            return;
        }

        byte directionType = request.getDirectionType();
        if (directionType != MessageConstants.DIRECTION_FORWARD) {
            LOGGER.warn("SteamEvent must be forward!");
        }

        final RpcResponse response = MessageBuilder.buildRpcResponse(request);
        Protocol protocol = ProtocolFactory.getProtocol(request.getProtocolType());

        String streamInsKey = (String) request.getHeadKey(HeadKey.STREAM_INS_KEY);
        StreamObserver observer = StreamContext.getStreamIns(streamInsKey);
        if (observer == null) {
            LOGGER.error("StreamObserver instance of {} is null!", streamInsKey);
            response.setException(new BsoaRpcException(22222,
                    "StreamObserver instance of {} is null!"));
        } else {
            AbstractByteBuf byteBuf = request.getByteBuf();
            try {
                protocol.decoder().decodeBody(byteBuf, request);
                String methodName = request.getMethodName();
                if (METHOD_ONVALUE.equals(methodName)) {
                    observer.onValue(request.getArgs()[0]);
                } else if (METHOD_ONERROR.equals(methodName)) {
                    observer.onError((Throwable) request.getArgs()[0]);
                    StreamContext.removeStreamIns(streamInsKey);
                } else if (METHOD_ONCOMPLETED.equals(methodName)) {
                    observer.onCompleted();
                    StreamContext.removeStreamIns(streamInsKey);
                } else {
                    response.setException(new BsoaRpcException(22222,
                            "Can not found method named \"" + methodName + "\""));
                }
            } catch (Exception e) {
                LOGGER.error("StreamObserver handler catch exception in channel "
                        + NetUtils.channelToString(channel.getRemoteAddress(), channel.getLocalAddress())
                        + ", error message is :" + e.getMessage(), e);
                response.setException(new BsoaRpcException(22222, "22222"));
            } finally {
                if (byteBuf != null) {
                    byteBuf.release();
                }
            }
        }
        AbstractByteBuf responseByteBuf = channel.getByteBuf();
        protocol.encoder().encodeAll(response, responseByteBuf);

        if (timeout != null && BsoaContext.now() - request.getReceiveTime() > timeout) { // 客户端已经超时的响应直接丢弃
            LOGGER.warn("[JSF-23008]Discard send response cause by " +
                    "timeout after receive the request: {}", request.getMessageId());
            responseByteBuf.release();
        } else {
            channel.writeAndFlush(responseByteBuf);
        }
    }
}
