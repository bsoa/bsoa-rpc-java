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

import io.bsoa.rpc.common.type.HeadKey;

/**
 *
 *
 * Created by zhangg on 2016/7/17 01:49.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class RPCMessage extends BaseMessage {

    public void addHeadKey(HeadKey key, Object value) {
        if (!key.getType().isInstance(value)) { // 检查类型
            throw new IllegalArgumentException("type mismatch of key:" + key.getNum() + ", expect:"
                    + key.getType().getName() + ", actual:" + value.getClass().getName());
        }
        headKeys.put(key.getNum(), value);
    }

    public Object removeByKey(HeadKey key){
        return headKeys.remove(key.getNum());
    }

    public Object getAttrByKey(HeadKey key){
        return headKeys.get(key.getNum());

    }

    public void setValuesInKeyMap(Map<Byte,Object> valueMap){
        this.headKeys.putAll(valueMap);

    }

    public int getAttrMapSize(){
        int mapSize = headKeys.size();
        return mapSize;
    }
}
