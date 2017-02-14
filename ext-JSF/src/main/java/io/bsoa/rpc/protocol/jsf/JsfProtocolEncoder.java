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
import io.bsoa.rpc.message.MessageConstants;
import io.bsoa.rpc.message.NegotiatorRequest;
import io.bsoa.rpc.message.NegotiatorResponse;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.protocol.ProtocolEncoder;
import io.bsoa.rpc.protocol.ProtocolInfo;
import io.bsoa.rpc.transport.AbstractByteBuf;
import io.bsoa.rpc.transport.netty.NettyByteBuf;
import io.netty.buffer.ByteBuf;

import static io.bsoa.rpc.common.BsoaOptions.COMPRESS_OPEN;
import static io.bsoa.rpc.common.BsoaOptions.COMPRESS_SIZE_BASELINE;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/18 16:02. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("jsf")
public class JsfProtocolEncoder implements ProtocolEncoder {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(JsfProtocolEncoder.class);

    private boolean compressOpen = BsoaConfigs.getBooleanValue(COMPRESS_OPEN);
    private int compressSize = BsoaConfigs.getIntValue(COMPRESS_SIZE_BASELINE);

    public JsfProtocolEncoder() {
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
            BaseMessage msg = (BaseMessage) object;

            short headerLength = 10; // 如果没有头部扩展

            // 0-1 魔术位(2位)
            out.writeBytes(protocolInfo.magicCode());
            // 2-5 总长度(4位，包括魔术位）
            out.writeInt(0); // 总长度会变，这里先占位
            // 6-7 2位头部长度(包括后面的头, 不包括魔术位+总长度+头部2位）
            int headLengthIndex = out.writerIndex();
            out.writeShort(headerLength);// 总长度可能会变，这里先占位
            // 8 协议类型（1位）
            out.writeByte(msg.getProtocolType());
            // 9 序列化类型（1位）
            out.writeByte(msg.getSerializationType());
            // 10 消息类型（1位）
            out.writeByte(msg.getMessageType());
            // 11 压缩类型（1位）
            out.writeByte(msg.getCompressType());
            // 12-15 消息Id（4位）
            out.writeInt(msg.getMessageId());

            // 如果有头部扩展
            if (CommonUtils.isNotEmpty(msg.getHeaders())) {
                headerLength += map2bytes(msg.getHeaders(), out);
                out.setBytes(headLengthIndex, CodecUtils.short2bytes(headerLength)); // 更新head长度的两位
            }
            msg.setTotalLength(headerLength + 4); // 目前消息总长度=4位总长度+头长度

//            byte[] bytes = new byte[out.readableBytes()];
//            out.readBytes(bytes);
//            LOGGER.debug(Arrays.toString(bytes));
//            out.readerIndex(0);
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
                        msg.setCompressType(MessageConstants.COMPRESS_NONE);
                        out.setByte(11, MessageConstants.COMPRESS_NONE); // 第11位 修改压缩类型
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
                NegotiatorRequest request = (NegotiatorRequest) object;
                totalLength += writeString(out, request.getCmd());
                totalLength += writeString(out, request.getData());
            } else if (object instanceof NegotiatorResponse) {
                NegotiatorResponse response = (NegotiatorResponse) object;
                totalLength += writeString(out, response.getRes());
            }
            msg.setTotalLength(totalLength);
            out.setBytes(2, CodecUtils.intToBytes(totalLength)); // 更新字段

//            byte[] bytes = new byte[out.readableBytes()];
//            out.readBytes(bytes);
//            LOGGER.debug("length : {}, data: {}", bytes.length, Arrays.toString(bytes));
//            out.readerIndex(0);
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

    /**
     * 自定义string编码
     *
     * @param out    输出流
     * @param string 字符串
     * @return 写入的长度
     * @see JsfProtocolDecoder#readString(ByteBuf)
     */
    private int writeString(ByteBuf out, String string) {
        if (string != null) {
            if (string.length() == 0) {
                out.writeInt(0);
                return 4;
            } else {
                byte[] bs = string.getBytes(BsoaConstants.DEFAULT_CHARSET);
                out.writeInt(bs.length);
                out.writeBytes(bs);
                return 4 + bs.length;
            }
        } else {
            out.writeInt(-1); // 长度-1代表null
            return 4;
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
    protected static short map2bytes(Map<Byte, Object> dataMap, ByteBuf byteBuf) {
        byteBuf.writeByte(dataMap.size());
        short writeBytes = 1; // 写入的字节数
        for (Map.Entry<Byte, Object> attr : dataMap.entrySet()) {
            byte headCode = attr.getKey();
            Object val = attr.getValue();
            if (val == Void.class) {
                byteBuf.writeByte(headCode);
                byteBuf.writeByte((byte) 0);
                // 从约定的 缓存中里面取;
                byteBuf.writeByte((byte) 1); //TODO 是2位还是1位
                writeBytes += 3;
            } else if (val instanceof Integer) {
                byteBuf.writeByte(headCode);
                byteBuf.writeByte((byte) 1);
                byteBuf.writeInt((Integer) val);
                writeBytes += 6;
            } else if (val instanceof String) {
                byteBuf.writeByte(headCode);
                byteBuf.writeByte((byte) 2);
                byte[] bs = ((String) val).getBytes(BsoaConstants.DEFAULT_CHARSET);
                byteBuf.writeShort(bs.length);
                byteBuf.writeBytes(bs);
                writeBytes += (4 + bs.length);
            } else if (val instanceof Byte) {
                byteBuf.writeByte(headCode);
                byteBuf.writeByte((byte) 3);
                byteBuf.writeByte((Byte) val);
                writeBytes += 3;
            } else if (val instanceof Short) {
                byteBuf.writeByte(headCode);
                byteBuf.writeByte((byte) 4);
                byteBuf.writeShort((Short) val);
                writeBytes += 4;
            } else if (val instanceof Long) {
                byteBuf.writeByte(headCode);
                byteBuf.writeByte((byte) 5);
                byteBuf.writeLong((Long) val);
                writeBytes += 10;
            } else if (val instanceof Boolean) {
                byteBuf.writeByte(headCode);
                byteBuf.writeByte((byte) 6);
                byteBuf.writeBoolean(((Boolean) val));
                writeBytes += 3;
            } else {
                throw new BsoaRpcException(22222,
                        "Class of value must be void(means ref)/int/String/byte/short/long/boolean");
            }
        }
        return writeBytes;
    }
}
