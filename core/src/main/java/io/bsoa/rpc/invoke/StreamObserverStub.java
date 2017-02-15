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

import java.io.Serializable;

import io.bsoa.rpc.codec.CompressorFactory;
import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.BsoaOptions;
import io.bsoa.rpc.common.utils.CodecUtils;
import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.message.HeadKey;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.MessageConstants;
import io.bsoa.rpc.message.RPCMessage;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.transport.ClientTransport;

import static io.bsoa.rpc.invoke.StreamContext.METHOD_ONCOMPLETED;
import static io.bsoa.rpc.invoke.StreamContext.METHOD_ONERROR;
import static io.bsoa.rpc.invoke.StreamContext.METHOD_ONVALUE;

/**
 * <p>收到StreamObserver的端，将生成一个本地代理类</p>
 * <p>
 * Created by zhangg on 2017/2/12 18:10. <br/>
 *
 * @param <V> the type parameter
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class StreamObserverStub<V> implements StreamObserver<V>, Serializable {

    /**
     * Instance key of StreamObserver
     */
    private final String streamInsKey;

    /**
     * Client transport of StreamObserver
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
     * Is this StreamObserver called method named "onCompleted" or "onError"
     */
    private boolean complete = false;

    /**
     * Construct Method
     *
     * @param streamInsKey Instance key of StreamObserver
     */
    public StreamObserverStub(String streamInsKey) {
        this.streamInsKey = streamInsKey;
    }

    @Override
    public void onValue(V value) {
        if (complete) {
            throw new BsoaRpcException(22222, "StreamObserver is completed!");
        }
        doSendMsg(METHOD_ONVALUE, new Class[]{value.getClass()}, new Object[]{value});
    }

    @Override
    public void onCompleted() {
        if (complete) {
            throw new BsoaRpcException(22222, "StreamObserver is completed!");
        }
        complete = true;
        doSendMsg(METHOD_ONCOMPLETED, CodecUtils.EMPTY_CLASS_ARRAY, CodecUtils.EMPTY_OBJECT_ARRAY);
    }

    @Override
    public void onError(Throwable t) {
        if (complete) {
            throw new BsoaRpcException(22222, "StreamObserver is completed!");
        }
        complete = true;
        doSendMsg(METHOD_ONERROR, new Class[]{t.getClass()}, new Object[]{t});
    }

    private void doSendMsg(String methodName, Class[] argTypes, Object[] args) {
        if (clientTransport == null || !clientTransport.isAvailable()) {
            throw new BsoaRpcException(22222,
                    "StreamObserver invalidate cause by channel closed, " +
                            "you can remove the stream proxy stub now, channel is"
                            + (clientTransport == null ? " null" : ": " +
                            NetUtils.connectToString(clientTransport.getChannel().getLocalAddress(),
                                    clientTransport.getChannel().getRemoteAddress())));
        }
        RpcRequest request = MessageBuilder.buildRpcRequest(StreamObserver.class, methodName, argTypes, args);
        request.setCompressType(compressType); // 默认开启压缩
        request.setProtocolType(protocolType);
        request.setSerializationType(serializationType);
        request.setDirectionType(MessageConstants.DIRECTION_ONEWAY); // 单向
        request.addHeadKey(HeadKey.STREAM_INS_KEY, this.streamInsKey);  //最重要
        clientTransport.oneWaySend(request, timeout);
    }

    /**
     * Init field by old message.
     *
     * @param message Old message, may be request or response
     * @return StreamObserverStub stream observer stub
     */
    public StreamObserverStub initByMessage(RPCMessage message) {
        this.protocolType = message.getProtocolType();
        this.serializationType = message.getSerializationType();
        Integer timeout = (Integer) message.getHeadKey(HeadKey.TIMEOUT);
        this.timeout = timeout == null ? BsoaConfigs.getIntValue(BsoaOptions.CONSUMER_INVOKE_TIMEOUT) : timeout;
        this.compressType = CompressorFactory.getCodeByAlias(BsoaConfigs.getStringValue(BsoaOptions.DEFAULT_COMPRESS));
        return this;
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
    public StreamObserverStub setClientTransport(ClientTransport clientTransport) {
        this.clientTransport = clientTransport;
        return this;
    }

    /**
     * Gets stream ins key.
     *
     * @return the stream ins key
     */
    public String getStreamInsKey() {
        return streamInsKey;
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
    public StreamObserverStub setProtocolType(byte protocolType) {
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
    public StreamObserverStub setSerializationType(byte serializationType) {
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
    public StreamObserverStub setTimeout(int timeout) {
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
    public StreamObserverStub setCompressType(byte compressType) {
        this.compressType = compressType;
        return this;
    }

}
