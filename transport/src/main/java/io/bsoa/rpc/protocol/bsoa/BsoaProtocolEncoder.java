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

import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.codec.Compressor;
import io.bsoa.rpc.codec.CompressorFactory;
import io.bsoa.rpc.codec.Serializer;
import io.bsoa.rpc.codec.SerializerFactory;
import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.CodecUtils;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.BaseMessage;
import io.bsoa.rpc.message.HeartbeatRequest;
import io.bsoa.rpc.message.HeartbeatResponse;
import io.bsoa.rpc.message.NegotiatorRequest;
import io.bsoa.rpc.message.NegotiatorResponse;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.protocol.ProtocolEncoder;
import io.bsoa.rpc.protocol.ProtocolInfo;
import io.bsoa.rpc.transport.AbstractByteBuf;
import io.bsoa.rpc.transport.netty.NettyByteBuf;
import io.netty.buffer.ByteBuf;

import static io.bsoa.rpc.common.BsoaConfigs.COMPRESS_OPEN;
import static io.bsoa.rpc.common.BsoaConfigs.COMPRESS_SIZE_BASELINE;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/18 16:02. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("bsoa")
public class BsoaProtocolEncoder implements ProtocolEncoder {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(BsoaProtocolEncoder.class);

    private boolean compressOpen = BsoaConfigs.getBooleanValue(COMPRESS_OPEN);
    private int compressSize = BsoaConfigs.getIntValue(COMPRESS_SIZE_BASELINE);

    public BsoaProtocolEncoder() {
        BsoaConfigs.subscribe(COMPRESS_SIZE_BASELINE, (oldValue, newValue) -> compressSize = (int) newValue);
        BsoaConfigs.subscribe(COMPRESS_OPEN, (oldValue, newValue) -> compressOpen = (boolean) newValue);
    }

    private ProtocolInfo protocolInfo;

    @Override
    public void setProtocolInfo(ProtocolInfo protocolInfo) {
        this.protocolInfo = protocolInfo;
    }

    @Override
    public void encodeHeader(Object object, AbstractByteBuf byteBuf) {
        NettyByteBuf src = (NettyByteBuf) byteBuf;
        ByteBuf out = src.getByteBuf();

        if (object instanceof BaseMessage) {
            short headerLength = 10;
            BaseMessage msg = (BaseMessage) object;

            // 0-1 2位魔术位
            out.writeBytes(protocolInfo.getMagicCode().getBytes());
            // 2-5 4位总长度（包括魔术位和自己），先占位
            out.writeInt(0);
            // 6-7 2位头部长度(包括自己2位+后面的头），先占位
            int headLengthIndex = out.writerIndex();
            out.writeShort(headerLength);
            // 8-11 消息/协议/序列化/压缩
            out.writeByte(msg.getMessageType());
            out.writeByte(msg.getProtocolType());
            out.writeByte(msg.getSerializationType());
            out.writeByte(msg.getCompressType());
            // 12-15 4位 消息Id
            out.writeInt(msg.getMessageId());

            if (CommonUtils.isNotEmpty(msg.getHeadKeys())) {
                headerLength += map2bytes(msg.getHeadKeys(), out);
                out.setBytes(headLengthIndex, CodecUtils.short2bytes(headerLength)); // 替换head长度的两位
            }
            msg.setTotalLength(headerLength + 6); // 目前out


            byte[] bytes = new byte[out.readableBytes()];
            out.readBytes(bytes);
            LOGGER.debug(Arrays.toString(bytes));
            out.readerIndex(0);
        } else {
            LOGGER.warn("Unsupported type :{}", object.getClass());
            throw new BsoaRpcException(22222, "Unsupported object type");
        }
    }

    @Override
    public void encodeBody(Object object, AbstractByteBuf byteBuf) {
        NettyByteBuf src = (NettyByteBuf) byteBuf;
        ByteBuf out = src.getByteBuf();
        if (object instanceof BaseMessage) {
            BaseMessage msg = (BaseMessage) object;
            int totalLength = msg.getTotalLength();
            if (object instanceof RpcRequest
                    || object instanceof RpcResponse) {
                Serializer serializer = SerializerFactory.getSerializer(msg.getSerializationType());
                // 序列化
                byte[] bs = serializer.encode(object);
                // 如果配置了要压缩
                if (msg.getCompressType() > 0) {
                    if (compressOpen && msg.getCompressType() > 0 && bs.length > compressSize) {
                        // 全局开启压缩，且配置了要压缩，且超过压缩大小基线
                        Compressor compressor = CompressorFactory.getCompressor(msg.getCompressType());
                        bs = compressor.compress(bs);
                    } else {
                        msg.setCompressType(Compressor.NONE);
                        out.setByte(11, Compressor.NONE); // 修改压缩类型
                    }
                }
                out.writeBytes(bs);
                totalLength += bs.length;
            } else if (object instanceof HeartbeatRequest) {
                out.writeLong(((HeartbeatRequest) object).getTimestamp());
                totalLength += 8;
            } else if (object instanceof HeartbeatResponse) {
                out.writeLong(((HeartbeatResponse) object).getTimestamp());
                totalLength += 8;
            } else if (object instanceof NegotiatorRequest) {
                //TODO
                totalLength += 8;
            } else if (object instanceof NegotiatorResponse) {
                //TODO
                totalLength += 8;
            }
            out.setBytes(2, CodecUtils.intToBytes(totalLength)); // 更新字段

            byte[] bytes = new byte[out.readableBytes()];
            out.readBytes(bytes);
            LOGGER.debug(Arrays.toString(bytes));
            out.readerIndex(0);
        } else {
            LOGGER.warn("Unsupported type :{}", object.getClass());
            throw new BsoaRpcException(22222, "Unsupported object type");
        }

    }

    @Override
    public void encodeAll(Object object, AbstractByteBuf byteBuf) {
        encodeHeader(object, byteBuf);
        encodeBody(object, byteBuf);
    }


    protected static short map2bytes(Map<Byte, Object> dataMap, ByteBuf byteBuf) {
        byteBuf.writeByte(dataMap.size());
        short s = 1;
        for (Map.Entry<Byte, Object> attr : dataMap.entrySet()) {
            byte key = attr.getKey();
            Object val = attr.getValue();
            if (val instanceof Integer) {
                byteBuf.writeByte(key);
                byteBuf.writeByte((byte) 1);
                byteBuf.writeInt((Integer) val);
                s += 6;
            } else if (val instanceof String) {
                byteBuf.writeByte(key);
                byteBuf.writeByte((byte) 2);
                byte[] bs = ((String) val).getBytes(BsoaConstants.DEFAULT_CHARSET);
                byteBuf.writeShort(bs.length);
                byteBuf.writeBytes(bs);
                s += (4 + bs.length);
            } else if (val instanceof Byte) {
                byteBuf.writeByte(key);
                byteBuf.writeByte((byte) 3);
                byteBuf.writeByte((Byte) val);
                s += 3;
            } else if (val instanceof Short) {
                byteBuf.writeByte(key);
                byteBuf.writeByte((byte) 4);
                byteBuf.writeShort((Short) val);
                s += 4;
            } else {
                throw new BsoaRpcException(22222, "Value of attrs in message header must be byte/short/int/string");
            }
        }
        return s;
    }
}
