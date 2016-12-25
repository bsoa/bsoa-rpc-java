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
 * Created by zhangg on 2016/7/17 01:49.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class RPCMessage extends BaseMessage {

    /**
     * 隐式传参用
     */
    private Map<String, Object> attachments = new HashMap<String, Object>();

    public RPCMessage(byte type) {
        super(type);
    }

    public void addHeadKey(HeadKey key, Object value) {
        if(value == null) {

        }
        if (!key.getValueType().isInstance(value)) { // 检查类型
            throw new IllegalArgumentException("type mismatch of key:" + key.getCode() + ", expect:"
                    + key.getValueType().getName() + ", actual:" + value.getClass().getName());
        }
        headers.put(key.getCode(), value);
    }

    public Object removeByKey(HeadKey key) {
        return headers.remove(key.getCode());
    }

    public Object getAttrByKey(HeadKey key) {
        return headers.get(key.getCode());

    }

    public void setValuesInKeyMap(Map<Byte, Object> valueMap) {
        this.headers.putAll(valueMap);

    }

    public int getAttrMapSize() {
        int mapSize = headers.size();
        return mapSize;
    }


    public void addAttachments(Map<String,Object> sourceMap){
        this.attachments.putAll(sourceMap);
    }
}
