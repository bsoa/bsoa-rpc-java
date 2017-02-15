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

import io.bsoa.rpc.codec.CompressorFactory;
import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.context.CallbackContext;
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
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class CallbackStub<Q, S> implements Callback<Q, S> {

    /**
     * Instance key of Callback
     */
    private final String callbackInsKey;
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
     * Parameter types of request message(Read from cache).
     */
    private Class[] argTypes;

    /**
     * Construct Method
     *
     * @param callbackInsKey Instance key of Callback
     */
    public CallbackStub(String callbackInsKey) {
        this.callbackInsKey = callbackInsKey;
        this.argTypes = CallbackContext.getParamTypeOfStreamMethod();
    }

    @Override
    public S invoke(Q result)  {
        if (clientTransport == null || !clientTransport.isAvailable()) {
            BsoaRpcException stubClosed = new BsoaRpcException(22222,
                    "StreamObserver invalidate cause by channel closed, you can remove the stream proxy stub now, channel is"
                            + (clientTransport == null ? " null" : ": " +
                            NetUtils.connectToString(clientTransport.getChannel().getLocalAddress(),
                                    clientTransport.getChannel().getRemoteAddress())));
            throw stubClosed;
        }
        RpcRequest request = MessageBuilder.buildRpcRequest(
                Callback.class, "invoke", argTypes, new Object[]{});
        request.setCompressType(compressType); // 默认开启压缩
        request.setProtocolType(protocolType);
        request.setSerializationType(serializationType);
        request.addHeadKey(HeadKey.STREAM_INS_KEY, this.callbackInsKey);  //最重要
        RpcResponse response = (RpcResponse) clientTransport.syncSend(request, timeout);
        if(response.hasError()){
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
     * @return StreamObserverStub
     */
    public CallbackStub<Q, S> initByMessage(RPCMessage message) {
        this.protocolType = message.getProtocolType();
        this.serializationType = message.getSerializationType();
        Integer timeout = (Integer) message.getHeadKey(HeadKey.TIMEOUT);
        this.timeout = timeout == null ? getIntValue(CONSUMER_INVOKE_TIMEOUT) : timeout;
        this.compressType = CompressorFactory.getCodeByAlias(getStringValue(DEFAULT_COMPRESS));
        return this;
    }
}
