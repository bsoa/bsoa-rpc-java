/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bsoa.rpc.invoke;

import java.io.Serializable;

import io.bsoa.rpc.codec.CompressorFactory;
import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.message.HeadKey;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.RPCMessage;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.transport.ClientTransport;

import static io.bsoa.rpc.common.BsoaConfigs.getIntValue;
import static io.bsoa.rpc.common.BsoaConfigs.getStringValue;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_INVOKE_TIMEOUT;
import static io.bsoa.rpc.common.BsoaOptions.DEFAULT_COMPRESS;

/**
 * <p>收到Callback的端，将生成一个本地代理类</p>
 * <p>
 * Created by zhangg on 2017/2/11 00:16. <br/>
 *
 * @param <Q> the request parameter
 * @param <S> the response parameter
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class CallbackStub<Q, S> implements Callback<Q, S>, Serializable {

    private static final long serialVersionUID = -7284179050061730184L;
    /**
     * Instance key of Callback
     */
    private final String callbackInsKey;
    /**
     * Parameter types of request message
     */
    private final Class[] argTypes;
    /**
     * Client transport of Callback
     */
    private ClientTransport clientTransport;

    /**
     * Protocol type of request message(Copy from old message).
     */
    private byte protocolType;
    /**
     * Serialization type of request message(Copy from old message).
     */
    private byte serializationType;
    /**
     * Timeout of request message(Copy from old message).
     */
    private int timeout;
    /**
     * Compress type of request message(Copy from old message).
     */
    private byte compressType;

    /**
     * Construct Method
     *
     * @param callbackInsKey Instance key of Callback
     * @param paramType      the param type
     */
    public CallbackStub(String callbackInsKey, Class paramType) {
        this.callbackInsKey = callbackInsKey;
        this.argTypes = new Class[]{paramType};
    }

    @Override
    public S notify(Q result) {
        if (clientTransport == null || !clientTransport.isAvailable()) {
            throw new BsoaRpcException(22222, "Callback invalidate cause by channel closed, " +
                    "you can remove the callback stub now, channel is"
                            + (clientTransport == null ? " null" : ": " +
                            NetUtils.connectToString(clientTransport.getChannel().getLocalAddress(),
                                    clientTransport.getChannel().getRemoteAddress())));
        }
        RpcRequest request = MessageBuilder.buildRpcRequest(
                Callback.class, CallbackContext.METHOD_NOTIFY, argTypes, new Object[]{result});
        request.setCompressType(compressType); // 默认开启压缩
        request.setProtocolType(protocolType);
        request.setSerializationType(serializationType);
        request.addHeadKey(HeadKey.CALLBACK_INS_KEY, this.callbackInsKey);  //最重要
        RpcResponse response = (RpcResponse) clientTransport.syncSend(request, timeout);
        if (response.hasError()) {
            Throwable e = response.getException();
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new BsoaRpcException(22222, e);
            }
        } else {
            return (S) response.getReturnData();
        }
    }

    /**
     * Init field by old message.
     *
     * @param message Old message, may be request or response
     * @return CallbackStub callback stub
     */
    public CallbackStub<Q, S> initByMessage(RPCMessage message) {
        this.protocolType = message.getProtocolType();
        this.serializationType = message.getSerializationType();
        Integer timeout = (Integer) message.getHeadKey(HeadKey.TIMEOUT);
        this.timeout = timeout == null ? getIntValue(CONSUMER_INVOKE_TIMEOUT) : timeout;
        this.compressType = CompressorFactory.getCodeByAlias(getStringValue(DEFAULT_COMPRESS));
        return this;
    }

    /**
     * Gets callback ins key.
     *
     * @return the callback ins key
     */
    public String getCallbackInsKey() {
        return callbackInsKey;
    }

    /**
     * Gets client transport.
     *
     * @return the client transport
     */
    public ClientTransport getClientTransport() {
        return clientTransport;
    }

    /**
     * Sets client transport.
     *
     * @param clientTransport the client transport
     * @return the client transport
     */
    public CallbackStub setClientTransport(ClientTransport clientTransport) {
        this.clientTransport = clientTransport;
        return this;
    }

    /**
     * Gets protocol type.
     *
     * @return the protocol type
     */
    public byte getProtocolType() {
        return protocolType;
    }

    /**
     * Sets protocol type.
     *
     * @param protocolType the protocol type
     * @return the protocol type
     */
    public CallbackStub setProtocolType(byte protocolType) {
        this.protocolType = protocolType;
        return this;
    }

    /**
     * Gets serialization type.
     *
     * @return the serialization type
     */
    public byte getSerializationType() {
        return serializationType;
    }

    /**
     * Sets serialization type.
     *
     * @param serializationType the serialization type
     * @return the serialization type
     */
    public CallbackStub setSerializationType(byte serializationType) {
        this.serializationType = serializationType;
        return this;
    }

    /**
     * Gets timeout.
     *
     * @return the timeout
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets timeout.
     *
     * @param timeout the timeout
     * @return the timeout
     */
    public CallbackStub setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Gets compress type.
     *
     * @return the compress type
     */
    public byte getCompressType() {
        return compressType;
    }

    /**
     * Sets compress type.
     *
     * @param compressType the compress type
     * @return the compress type
     */
    public CallbackStub setCompressType(byte compressType) {
        this.compressType = compressType;
        return this;
    }

    /**
     * Get arg types class [ ].
     *
     * @return the class [ ]
     */
    public Class[] getArgTypes() {
        return argTypes;
    }
}
