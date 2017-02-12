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
import io.bsoa.rpc.context.StreamContext;
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

/**
 * <p></p>
 *
 * Created by zhangg on 2017/2/12 22:31. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class StreamTask implements Runnable {
    
    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(StreamTask.class);

    private static final String METHOD_ONVALUE = "onValue";
    private static final String METHOD_ONERROR = "onError";
    private static final String METHOD_ONCOMPLETED = "onCompleted";

    private RpcRequest request;

    private AbstractChannel channel;

    public StreamTask(RpcRequest request, AbstractChannel channel) {
        this.request = request;
        this.channel = channel;
    }

    @Override
    public void run() {
        byte directionType = request.getDirectionType();
        if (directionType != MessageConstants.DIRECTION_ONEWAY) {
            LOGGER.warn("SteamEvent must be onWay!");
        }

        String streamInsKey = (String) request.getHeadKey(HeadKey.STREAM_INS_KEY);
        StreamObserver observer = StreamContext.getStreamIns(streamInsKey);
        if (observer == null) {
            LOGGER.error("StreamObserver instance of {} is null!", streamInsKey);
            return;
        }

        AbstractByteBuf byteBuf = request.getByteBuf();
        Protocol protocol = ProtocolFactory.getProtocol(request.getProtocolType());
        final RpcResponse response = MessageBuilder.buildRpcResponse(request);
        try {
            protocol.decoder().decodeBody(byteBuf, request);
            String methodName = request.getMethodName();
            if (METHOD_ONVALUE.equals(methodName)) {
                observer.onValue(request.getArgs()[0]);
            } else if (METHOD_ONERROR.equals(methodName)) {
                observer.onError((Throwable) request.getArgs()[0]);
            } else if (METHOD_ONCOMPLETED.equals(methodName)) {
                observer.onCompleted();
            }
        } catch (Exception e) {
            LOGGER.error("Stream handler catch exception in channel "
                    + NetUtils.channelToString(channel.getRemoteAddress(), channel.getLocalAddress())
                    + ", error message is :" + e.getMessage(), e);
            BsoaRpcException rpcException = new BsoaRpcException(22222, "22222");
            response.setException(rpcException);
        } finally {
            if (byteBuf != null) {
                byteBuf.release();
            }
        }
    }
}
