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

import io.bsoa.rpc.base.Invoker;
import io.bsoa.rpc.codec.CompressorFactory;
import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.context.StreamContext;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.message.HeadKey;
import io.bsoa.rpc.message.MessageConstants;
import io.bsoa.rpc.message.RPCMessage;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.transport.ClientTransport;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/2/11 13:06. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class StreamInvoker implements Invoker {

    public static final RpcResponse VOID_RESPONSE = new RpcResponse();

    private ClientTransport clientTransport;

    private final String streamInsKey;

    private final byte protocolType;
    private final byte serializationType;
    private final int timeout;
    private final byte compressType;

    /**
     * notify实际的参数类型
     */
    //private String[] argTypes;

    public StreamInvoker(ClientTransport clientTransport, String streamInsKey, RPCMessage request) {
        this.clientTransport = clientTransport;
        this.streamInsKey = streamInsKey;
        this.protocolType = request.getProtocolType();
        this.serializationType = request.getSerializationType();
        Integer timeout = (Integer) request.getHeadKey(HeadKey.TIMEOUT);
        this.timeout = timeout == null ? BsoaConfigs.getIntValue(BsoaConfigs.CONSUMER_INVOKE_TIMEOUT) : timeout;
        this.compressType = CompressorFactory.getCodeByAlias(BsoaConfigs.getStringValue(BsoaConfigs.DEFAULT_COMPRESS));
       // this.argTypes = new String[]{ClassTypeUtils.getTypeStr(actualType)};
    }


    @Override
    public RpcResponse invoke(RpcRequest request) {
        if (clientTransport == null || !clientTransport.isAvailable()) {
            BsoaRpcException callbackException = new BsoaRpcException(
                    "[JSF-23011]StreamObserver invalidate cause by channel closed, you can remove the stream proxy stub now, channel is"
                            + (clientTransport == null ? " null" : ": " +
                            NetUtils.connectToString(clientTransport.getChannel().getLocalAddress(),
                                    clientTransport.getChannel().getRemoteAddress())));
            StreamContext.removeStreamProxy(streamInsKey);
            throw callbackException;
        }

//        if (request instanceof RequestMessage && argTypes != null) {
//            // 将T 设置为实际类型
//            request.setArgsType(argTypes);
//        }

        request.setCompressType(compressType); // 默认开启压缩
        request.setProtocolType(protocolType);
        request.setSerializationType(serializationType);
        request.setDirectionType(MessageConstants.DIRECTION_ONEWAY); // 单向
        request.addHeadKey(HeadKey.STREAM_INS_KEY, this.streamInsKey);
        clientTransport.oneWaySend(request, timeout);
        return VOID_RESPONSE;
    }

    public String getStreamInsKey() {
        return streamInsKey;
    }

}
