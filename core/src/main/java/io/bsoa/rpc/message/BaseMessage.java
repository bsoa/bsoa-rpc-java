/*
 * Copyright © 2016-2017 The BSOA Project
 *
 * The BSOA Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.bsoa.rpc.message;

import io.bsoa.rpc.context.BsoaContext;

import java.util.HashMap;
import java.util.Map;

import static io.bsoa.rpc.message.MessageConstants.COMPRESS_NONE;
import static io.bsoa.rpc.message.MessageConstants.DIRECTION_FORWARD;
import static io.bsoa.rpc.message.MessageConstants.DIRECTION_ONEWAY;
import static io.bsoa.rpc.message.MessageConstants.PROTOCOL_NONE;
import static io.bsoa.rpc.message.MessageConstants.SERIALIZATION_NONE;

/**
 * <p>表示一个RPC消息，非线程安全</p>
 * <p>
 * Created by zhangg on 2016/12/15 22:56. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public abstract class BaseMessage {

    /**
     * 消息类型：（在"bsoa"协议中，占6bit，和调用方向组成一个byte）
     */
    private transient final byte messageType;

    /**
     * 子类必须实现
     *
     * @param messageType 消息类型
     */
    protected BaseMessage(byte messageType) {
        this.messageType = messageType;
    }

    /**
     * 调用方向类型：（在"bsoa"协议中，占2bit，和消息类型组成一个byte=8bit）
     */
    protected transient byte directionType = DIRECTION_FORWARD;

    /**
     * 请求ID（在"bsoa"协议中，占4byte=32bit）
     */
    protected transient int messageId;
    /**
     * 总长度（在"bsoa"协议中，占4byte=32bit）
     */
    protected transient int totalLength;
    /**
     * 头部长度（在"bsoa"协议中，占2byte=16bit）
     */
    protected transient short headLength;
    /**
     * 协议类型：（在"bsoa"协议中，占1byte=8bit）
     */
    protected transient byte protocolType = PROTOCOL_NONE;
    /**
     * 序列化类型：（在"bsoa"协议中，占1byte=8bit）
     */
    protected transient byte serializationType = SERIALIZATION_NONE;
    /**
     * 压缩类型：（在"bsoa"协议中，占1byte=8bit）
     */
    protected transient byte compressType = COMPRESS_NONE;
    /**
     * 头部扩展字段（HeadKey.code : value）
     */
    protected transient Map<Byte, Object> headers;

    /**
     * 收到的时间
     */
    private transient long receiveTime = BsoaContext.now();

    /**
     * Add header.
     *
     * @param key   the key
     * @param value the value
     * @return the head key
     */
    public BaseMessage addHeader(Byte key, Object value) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        if (value == null) {
            headers.remove(key);
        } else {
            headers.put(key, value);
        }
        return this;
    }

    /**
     * Del header.
     *
     * @param key the key
     * @return the head key
     */
    public BaseMessage delHeader(Byte key) {
        if (headers != null) {
            headers.remove(key);
        }
        return this;
    }

    /**
     * Gets header.
     *
     * @param key the key
     * @return the head key
     */
    public Object getHeader(Byte key) {
        return headers != null ? headers.get(key) : key;
    }

    /**
     * Gets message type.
     *
     * @return the message type
     */
    public byte getMessageType() {
        return messageType;
    }

    /**
     * Gets message id.
     *
     * @return the message id
     */
    public int getMessageId() {
        return messageId;
    }

    /**
     * Sets message id.
     *
     * @param messageId the message id
     * @return the message id
     */
    public BaseMessage setMessageId(int messageId) {
        this.messageId = messageId;
        return this;
    }

    /**
     * Gets total length.
     *
     * @return the total length
     */
    public int getTotalLength() {
        return totalLength;
    }

    /**
     * Sets total length.
     *
     * @param totalLength the total length
     * @return the total length
     */
    public BaseMessage setTotalLength(int totalLength) {
        this.totalLength = totalLength;
        return this;
    }

    /**
     * Gets head length.
     *
     * @return the head length
     */
    public short getHeadLength() {
        return headLength;
    }

    /**
     * Sets head length.
     *
     * @param headLength the head length
     * @return the head length
     */
    public BaseMessage setHeadLength(short headLength) {
        this.headLength = headLength;
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
    public BaseMessage setProtocolType(byte protocolType) {
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
    public BaseMessage setSerializationType(byte serializationType) {
        this.serializationType = serializationType;
        return this;
    }

    /**
     * Gets sync type.
     *
     * @return the sync type
     */
    public byte getDirectionType() {
        return directionType;
    }

    /**
     * Sets sync type.
     *
     * @param directionType the sync type
     * @return the sync type
     */
    public BaseMessage setDirectionType(byte directionType) {
        this.directionType = directionType;
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
    public BaseMessage setCompressType(byte compressType) {
        this.compressType = compressType;
        return this;
    }

    /**
     * Sets head keys.
     *
     * @param headers the head keys
     * @return the head keys
     */
    public BaseMessage setHeaders(Map<Byte, Object> headers) {
        if (this.headers == null) {
            this.headers = headers;
        } else {
            this.headers.putAll(headers);
        }
        return this;
    }

    /**
     * Gets receive time.
     *
     * @return the receive time
     */
    public long getReceiveTime() {
        return receiveTime;
    }

    /**
     * Sets receive time.
     *
     * @param receiveTime the receive time
     * @return the receive time
     */
    public BaseMessage setReceiveTime(long receiveTime) {
        this.receiveTime = receiveTime;
        return this;
    }

    /**
     * Gets head keys.
     *
     * @return the head keys
     */
    public Map<Byte, Object> getHeaders() {
        return headers;
    }

    /**
     * Is one-way message
     *
     * @return
     * @see MessageConstants#DIRECTION_ONEWAY
     */
    public boolean isOneWay() {
        return directionType == DIRECTION_ONEWAY;
    }
}
