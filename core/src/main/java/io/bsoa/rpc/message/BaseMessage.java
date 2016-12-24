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
package io.bsoa.rpc.message;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>非线程安全</p>
 * <p>
 * Created by zhangg on 2016/12/15 22:56. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public abstract class BaseMessage {

    protected transient final byte messageType;
    /**
     * 请求ID
     */
    protected transient int messageId;

    protected transient int totalLength; // 总长度
    protected transient int headLength; // 头部长度

    protected transient byte protocolType;
    protected transient byte serializationType;
    protected transient byte compressType;


    protected transient Map<Byte, Object> headKeys;

    protected BaseMessage(byte messageType) {
        this.messageType = messageType;
    }


    public int getMessageId() {
        return messageId;
    }

    public BaseMessage setMessageId(int messageId) {
        this.messageId = messageId;
        return this;
    }

    public int getTotalLength() {
        return totalLength;
    }

    public BaseMessage setTotalLength(int totalLength) {
        this.totalLength = totalLength;
        return this;
    }

    public int getHeadLength() {
        return headLength;
    }

    public BaseMessage setHeadLength(int headLength) {
        this.headLength = headLength;
        return this;
    }

    public byte getProtocolType() {
        return protocolType;
    }

    public BaseMessage setProtocolType(byte protocolType) {
        this.protocolType = protocolType;
        return this;
    }

    public byte getSerializationType() {
        return serializationType;
    }

    public BaseMessage setSerializationType(byte serializationType) {
        this.serializationType = serializationType;
        return this;
    }

    public byte getCompressType() {
        return compressType;
    }

    public BaseMessage setCompressType(byte compressType) {
        this.compressType = compressType;
        return this;
    }

    public byte getMessageType() {
        return messageType;
    }

    public Map<Byte, Object> getHeadKeys() {
        return headKeys;
    }

    public BaseMessage setHeadKeys(Map<Byte, Object> headKeys) {
        this.headKeys = headKeys;
        return this;
    }

    public BaseMessage setHeadKey(Byte key, Object value) {
        if (headKeys == null) {
            headKeys = new HashMap<>();
        }
        headKeys.put(key, value);
        return this;
    }
}
