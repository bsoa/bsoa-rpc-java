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
package io.bsoa.rpc.protocol.bsoa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.codec.Compressor;
import io.bsoa.rpc.codec.CompressorFactory;
import io.bsoa.rpc.codec.Serializer;
import io.bsoa.rpc.codec.SerializerFactory;
import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.BaseMessage;
import io.bsoa.rpc.message.HeartbeatRequest;
import io.bsoa.rpc.message.HeartbeatResponse;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.NegotiatorRequest;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.protocol.ProtocolDecoder;
import io.bsoa.rpc.protocol.ProtocolInfo;
import io.bsoa.rpc.transport.AbstractByteBuf;
import io.bsoa.rpc.transport.netty.NettyByteBuf;
import io.netty.buffer.ByteBuf;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/18 16:03. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("bsoa")
public class BsoaProtocolDecoder implements ProtocolDecoder {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(BsoaProtocolDecoder.class);

    private ProtocolInfo protocolInfo;

    @Override
    public void setProtocolInfo(ProtocolInfo protocolInfo) {
        this.protocolInfo = protocolInfo;
    }

    @Override
    public void decodeHeader(AbstractByteBuf byteBuf, List<Object> out) {
        NettyByteBuf nettyByteBuf = (NettyByteBuf) byteBuf;
        ByteBuf in = nettyByteBuf.getByteBuf();

        // 前面2位magiccode 和 4位总长度 已经跳过
        if (in.readerIndex() != 6) {
            throw new BsoaRpcException(22222, "readerIndex!=6");
        }
        Short headerLength = in.readShort();

        byte messageType = in.readByte();
        byte protocolType = in.readByte();
        byte serializationType = in.readByte();
        byte compressType = in.readByte();
        int messageId = in.readInt();

        BaseMessage message = MessageBuilder.buildMessage(messageType, messageId);
        message.setProtocolType(protocolType)
                .setSerializationType(serializationType)
                .setCompressType(compressType);
        if (headerLength > 10) { // 说明存在Map
            Map<Byte, Object> headKeys = new HashMap<>();
            bytes2Map(headKeys, in);
            message.setHeadKeys(headKeys);
        }
        out.add(message);
    }

    @Override
    public void decodeBody(AbstractByteBuf byteBuf, List<Object> out) {
        NettyByteBuf nettyByteBuf = (NettyByteBuf) byteBuf;
        try {
            ByteBuf in = nettyByteBuf.getByteBuf();

            if (CommonUtils.isEmpty(out)) {
                throw new BsoaRpcException(22222, "Need decode header first!");
            }
            Object object = out.get(0);
            if (object instanceof RpcRequest) { // 收到请求
                RpcRequest request = (RpcRequest) object;

                byte[] bodyBytes = new byte[in.readableBytes()]; // 剩下的都读完
                in.readBytes(bodyBytes);

                // 反序列化
                if (request.getCompressType() > 0) {
                    // 反压缩 TODO
                    // out.setByte(11, ); // 修改压缩类型
                    // bodyBytes = compressor.deCompress(bodyBytes);
                }
                Serializer serializer = SerializerFactory.getSerializer(request.getSerializationType());
                RpcRequest tmp = (RpcRequest) serializer.decode(bodyBytes, RpcRequest.class);

                request.setAttachments(tmp.getAttachments());
                request.setArgs(tmp.getArgs());
            } else if (object instanceof RpcResponse) { // 收到响应
                RpcResponse response = (RpcResponse) object;

                byte[] bodyBytes = new byte[in.readableBytes()]; // 剩下的都读完
                in.readBytes(bodyBytes);
                // 如果需要解压
                if (response.getCompressType() > 0) {
                    Compressor compressor = CompressorFactory.getCompressor(response.getCompressType());
                    bodyBytes = compressor.deCompress(bodyBytes);
                }
                // 反序列化
                Serializer serializer = SerializerFactory.getSerializer(response.getSerializationType());
                RpcResponse tmp = (RpcResponse) serializer.decode(bodyBytes, RpcResponse.class);

                response.setReturnData(tmp.getReturnData());
                response.setException(tmp.getException());
            } else if (object instanceof HeartbeatRequest) { // 收到心跳
                HeartbeatRequest request = (HeartbeatRequest) object;
                request.setTimestamp(in.readLong());
            } else if (object instanceof HeartbeatResponse) {
                HeartbeatResponse response = (HeartbeatResponse) object;
                response.setTimestamp(in.readLong());
            } else if (object instanceof NegotiatorRequest) {

            }
        } catch (BsoaRpcException e) {
            throw e;
        } catch (Exception e) {
            throw new BsoaRpcException(22222, "Decode error!", e);
        } finally {
            nettyByteBuf.release();
        }
    }

    @Override
    public void decodeAll(AbstractByteBuf byteBuf, List<Object> out) {
        decodeHeader(byteBuf, out);
        decodeBody(byteBuf, out);
    }


    protected static void bytes2Map(Map<Byte, Object> dataMap, ByteBuf byteBuf) {
        byte size = byteBuf.readByte();
        for (int i = 0; i < size; i++) {
            byte key = byteBuf.readByte();
            byte type = byteBuf.readByte();
            if (type == 1) {
                int value = byteBuf.readInt();
                dataMap.put(key, value);
            } else if (type == 2) {
                int length = byteBuf.readShort();
                byte[] dataArr = new byte[length];
                byteBuf.readBytes(dataArr);
                dataMap.put(key, new String(dataArr, BsoaConstants.DEFAULT_CHARSET));
            } else if (type == 3) {
                byte value = byteBuf.readByte();
                dataMap.put(key, value);
            } else if (type == 4) {
                short value = byteBuf.readShort();
                dataMap.put(key, value);
            } else {
                throw new BsoaRpcException(22222, "Value of attrs in message header must be byte/short/int/string");
            }
        }
    }
}
