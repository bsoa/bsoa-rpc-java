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
package io.bsoa.rpc.protocol.jsf;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.codec.Compressor;
import io.bsoa.rpc.codec.CompressorFactory;
import io.bsoa.rpc.codec.Serializer;
import io.bsoa.rpc.codec.SerializerFactory;
import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.BaseMessage;
import io.bsoa.rpc.message.DecodableMessage;
import io.bsoa.rpc.message.HeartbeatRequest;
import io.bsoa.rpc.message.HeartbeatResponse;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.NegotiatorRequest;
import io.bsoa.rpc.message.NegotiatorResponse;
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
@Extension("jsf")
public class JsfProtocolDecoder implements ProtocolDecoder {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(JsfProtocolDecoder.class);

    private ProtocolInfo protocolInfo;

    @Override
    public void setProtocolInfo(ProtocolInfo protocolInfo) {
        this.protocolInfo = protocolInfo;
    }

    @Override
    public Object decodeHeader(AbstractByteBuf byteBuf, Object out) {
        NettyByteBuf nettyByteBuf = (NettyByteBuf) byteBuf;
        ByteBuf in = nettyByteBuf.getByteBuf();

        // 前面2位magiccode 和 4位总长度 已经跳过
         if (in.readerIndex() != 0) {
            throw new BsoaRpcException(22222, "readerIndex!=0");
        }
        int totalLength = in.readableBytes() + 6; //跳过了6位
        Short headerLength = in.readShort();
        byte protocolType = in.readByte();
        byte serializationType = in.readByte();
        byte messageType = in.readByte();
        byte compressType = in.readByte();
        int messageId = in.readInt();

        BaseMessage message = MessageBuilder.buildMessage(messageType, messageId);
        message.setTotalLength(totalLength)
                .setHeadLength(headerLength)
                .setProtocolType(protocolType)
                .setSerializationType(serializationType)
                .setCompressType(compressType);
        if (headerLength > 10) { // 说明存在Map
            Map<Byte, Object> headKeys = new HashMap<>();
            bytes2Map(headKeys, in);
            message.setHeaders(headKeys);
        }
        return message;
    }

    @Override
    public Object decodeBody(AbstractByteBuf byteBuf, Object object) {
        NettyByteBuf nettyByteBuf = (NettyByteBuf) byteBuf;
        try {
            ByteBuf in = nettyByteBuf.getByteBuf();
            if (object == null) {
                throw new BsoaRpcException(22222, "Need decode header first!");
            }
            if (object instanceof RpcRequest) { // 收到请求
                RpcRequest request = (RpcRequest) object;

                byte[] bodyBytes = new byte[in.readableBytes()]; // 剩下的都读完
                in.readBytes(bodyBytes);

                // 反序列化开始
                // 如果需要解压缩
                if (request.getCompressType() > 0) {
                    Compressor compressor = CompressorFactory.getCompressor(request.getCompressType());
                    bodyBytes = compressor.deCompress(bodyBytes);
                }
                // 反序列话
                Serializer serializer = SerializerFactory.getSerializer(request.getSerializationType());
                serializer.decode(bodyBytes, request);

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
                serializer.decode(bodyBytes, response);
            } else if (object instanceof HeartbeatRequest) { // 收到心跳
                HeartbeatRequest request = (HeartbeatRequest) object;
                request.setTimestamp(in.readLong());
            } else if (object instanceof HeartbeatResponse) {
                HeartbeatResponse response = (HeartbeatResponse) object;
                response.setTimestamp(in.readLong());
            } else if (object instanceof NegotiatorRequest) {
                NegotiatorRequest request = (NegotiatorRequest) object;
                request.setCmd(readString(in));
                request.setData(readString(in));
            } else if (object instanceof NegotiatorResponse) {
                NegotiatorResponse response = (NegotiatorResponse) object;
                response.setRes(readString(in));
            }
        } catch (BsoaRpcException e) {
            throw e;
        } catch (Exception e) {
            throw new BsoaRpcException(22222, "Decode error!", e);
        }
        return object;
    }

    @Override
    public Object decodeAll(AbstractByteBuf byteBuf, Object out) {
        out = decodeHeader(byteBuf, out);
        BaseMessage msg = (BaseMessage) out;
        byte messageType = msg.getMessageType();
        if (messageType == 3 || messageType == 4 || messageType == 5 || messageType == 6) {
            // 心跳包 和协商包 在这里解析
            decodeBody(byteBuf, out);
        } else {
            // 其它在业务线程里decode
            DecodableMessage message = (DecodableMessage) msg;

            NettyByteBuf nettyByteBuf = (NettyByteBuf) byteBuf;
            ByteBuf in = nettyByteBuf.getByteBuf();
            int index = in.readerIndex() + in.readableBytes();
            ByteBuf body = in.slice(in.readerIndex(), in.readableBytes());
            body.retain();
            in.readerIndex(index);
            AbstractByteBuf newBb = new NettyByteBuf(body);
            message.setByteBuf(newBb);
        }
        return out;
    }

    /**
     * 自定义string解码
     *
     * @param in 输入流
     * @return 字符串
     * @see JsfProtocolEncoder#writeString(ByteBuf, String)
     */
    private String readString(ByteBuf in) {
        int length = in.readInt();
        if (length == -1) {
            return null;
        } else if (length == 0) {
            return StringUtils.EMPTY;
        } else {
            byte[] src = new byte[length];
            in.readBytes(src);
            return new String(src, BsoaConstants.DEFAULT_CHARSET);
        }
    }

    /**
     * <p>支持 void(代表是个映射）/int/String/byte/short/long/boolean</p>
     * <p>bsoa协议中如下定义：<br/>
     * void   : 1位key+1位标识(0)+1位ref值<br/>  3
     * int    : 1位key+1位标识(1)+4位值<br/>   6
     * String : 1位key+1位标识(2)+2位长度+N位值<br/> 4+n
     * byte   : 1位key+1位标识(3)+1位值<br/> 3
     * short  : 1位key+1位标识(4)+2位值<br/> 4
     * long   : 1位key+1位标识(5)+8位值<br/> 10
     * boolean: 1位key+1位标识(6)+1位值<br/> 3
     * </p>
     */
    protected static void bytes2Map(Map<Byte, Object> dataMap, ByteBuf byteBuf) {
        byte size = byteBuf.readByte();
        for (int i = 0; i < size; i++) {
            byte key = byteBuf.readByte();
            byte type = byteBuf.readByte();
            switch (type) {
                case 0:
                    byte ref = byteBuf.readByte();
                    // TODO 从缓存里去映射值
                    break;
                case 1:
                    dataMap.put(key, byteBuf.readInt());
                    break;
                case 2:
                    byte[] dataArr = new byte[byteBuf.readShort()];
                    byteBuf.readBytes(dataArr);
                    dataMap.put(key, new String(dataArr, BsoaConstants.DEFAULT_CHARSET));
                    break;
                case 3:
                    dataMap.put(key, byteBuf.readByte());
                    break;
                case 4:
                    dataMap.put(key, byteBuf.readShort());
                    break;
                case 5:
                    dataMap.put(key, byteBuf.readLong());
                    break;
                case 6:
                    dataMap.put(key, byteBuf.readBoolean());
                    break;
                default:
                    throw new BsoaRpcException(22222, "Value of attrs in message header must be byte/short/int/string");
            }
        }
    }
}
