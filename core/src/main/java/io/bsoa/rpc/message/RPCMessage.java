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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.type.HeadKey;

/**
 *
 *
 * Created by zhangg on 2016/7/17 01:49.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class RPCMessage extends BaseMessage {

    private int length; // 总长度
    private int headLength;
    private byte protocolType;
    private byte serializationType;
    private byte compressType;
    private byte messageType;
    private int messageId;
    private Map<Byte,Object> keysMap = new ConcurrentHashMap<Byte,Object>();


    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getHeadLength() {
        return headLength;
    }

    public void setHeadLength(int headLength) {
        this.headLength = headLength;
    }

    public byte getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(byte protocolType) {
        this.protocolType = protocolType;
    }

    public byte getSerializationType() {
        return serializationType;
    }

    public void setSerializationType(byte serializationType) {
        this.serializationType = serializationType;
    }

    public byte getCompressType() {
        return compressType;
    }

    public void setCompressType(byte compressType) {
        this.compressType = compressType;
    }

    public byte getMessageType() {
        return messageType;
    }

    public void setMessageType(byte messageType) {
        this.messageType = messageType;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public Map<Byte, Object> getKeysMap() {
        return keysMap;
    }

    public void setKeysMap(Map<Byte, Object> keysMap) {
        this.keysMap = keysMap;
    }


    public void addHeadKey(HeadKey key, Object value) {
        if (!key.getType().isInstance(value)) { // 检查类型
            throw new IllegalArgumentException("type mismatch of key:" + key.getNum() + ", expect:"
                    + key.getType().getName() + ", actual:" + value.getClass().getName());
        }
        keysMap.put(key.getNum(), value);
    }

    public Object removeByKey(HeadKey key){
        return keysMap.remove(key.getNum());
    }

    public Object getAttrByKey(HeadKey key){
        return keysMap.get(key.getNum());

    }

    public void setValuesInKeyMap(Map<Byte,Object> valueMap){
        this.keysMap.putAll(valueMap);

    }

    public int getAttrMapSize(){
        int mapSize = keysMap.size();
        return mapSize;
    }
}
