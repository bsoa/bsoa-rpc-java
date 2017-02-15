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
 * <p>
 * Created by zhangg on 2017/2/12 22:31. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class CallbackTask implements Runnable {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(CallbackTask.class);

    /**
     * RpcRequest of Callback
     */
    private RpcRequest request;

    /**
     * The Channel
     */
    private AbstractChannel channel;

    /**
     * Construct method
     *
     * @param request RpcRequest of Callback
     * @param channel Channel
     */
    public CallbackTask(RpcRequest request, AbstractChannel channel) {
        this.request = request;
        this.channel = channel;
    }

    @Override
    public void run() {
        byte directionType = request.getDirectionType();
        if (directionType != MessageConstants.DIRECTION_FORWARD) {
            LOGGER.warn("CallbackEvent must be forward!");
        }

        String callbackInsKey = (String) request.getHeadKey(HeadKey.CALLBACK_INS_KEY);
        Callback callback = CallbackContext.getCallbackIns(callbackInsKey);
        if (callback == null) {
            LOGGER.error("Callback instance of {} is null!", callbackInsKey);
            return;
        }

        AbstractByteBuf byteBuf = request.getByteBuf();
        Protocol protocol = ProtocolFactory.getProtocol(request.getProtocolType());
        final RpcResponse response = MessageBuilder.buildRpcResponse(request);
        try {
            protocol.decoder().decodeBody(byteBuf, request);
            String methodName = request.getMethodName();
            if (CallbackContext.METHOD_NOTIFY.equals(methodName)) {
                Object ret = callback.notify(request.getArgs()[0]);
                response.setReturnData(ret);
            } else {
                response.setException(new BsoaRpcException("Can not found method named \"" + methodName + "\""));
            }
        } catch (Exception e) {
            LOGGER.error("Callback handler catch exception in channel "
                    + NetUtils.channelToString(channel.getRemoteAddress(), channel.getLocalAddress())
                    + ", error message is :" + e.getMessage(), e);
            response.setException(new BsoaRpcException(22222, "22222"));
        } finally {
            if (byteBuf != null) {
                byteBuf.release();
            }
        }

        AbstractByteBuf responseByteBuf = channel.getByteBuf();
        protocol.encoder().encodeAll(response, responseByteBuf);
        channel.writeAndFlush(responseByteBuf);
    }
}
