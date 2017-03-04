/*
 * Copyright 2016 The BSOA Project
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

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhangg on 2016/7/17 01:49.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public abstract class RPCMessage extends DecodableMessage {
    /**
     * 隐式传参用
     */
    private Map<String, Object> attachments;

    /**
     * 子类必须实现
     *
     * @param messageType 消息类型
     */
    protected RPCMessage(byte messageType) {
        super(messageType);
    }

//    @JustForTest
//    private RPCMessage() {
//        super((byte) 0);
//    }

    /**
     * 加一个头部数据
     *
     * @param key
     * @param value
     */
    public void addHeadKey(HeadKey key, Object value) {
        if (!key.getValueType().isInstance(value)) { // 检查类型
            throw new IllegalArgumentException("type mismatch of key:" + key.getCode() + ", expect:"
                    + key.getValueType().getName() + ", actual:" + value.getClass().getName());
        }
        super.addHeader(key.getCode(), value);
    }

    /**
     * 删一个头部信息
     *
     * @param key
     * @return
     */
    public Object delHeadKey(HeadKey key) {
        return super.delHeader(key.getCode());
    }

    /**
     * 得到头部信息
     *
     * @param key
     * @return
     */
    public Object getHeadKey(HeadKey key) {
        return headers == null ? null : headers.get(key.getCode());
    }

    /**
     * 得到全部隐式传参信息
     *
     * @return 全部kv值
     */
    public Map<String, Object> getAttachments() {
        return this.attachments;
    }

    /**
     * 得到单个隐式传参信息
     *
     * @param key 单个key
     * @return 单个值
     */
    public Object getAttachment(String key) {
        return attachments == null ? null : attachments.get(key);
    }

    /**
     * 增加多个附件信息
     *
     * @param attachments
     * @return 对象自己
     */
    public RPCMessage setAttachments(Map<String, Object> attachments) {
        if (attachments == null) {
            return this;
        }
        if (this.attachments == null) {
            this.attachments = new HashMap<>();
        }
        this.attachments.putAll(attachments);
        return this;
    }

    /**
     * 增加多个附件信息
     *
     * @param key 附件信息关键字
     * @return
     */
    public RPCMessage delAttachment(String key) {
        if (this.attachments != null) {
            this.attachments.remove(key);
        }
        return this;
    }

    /**
     * 增加一个附件信息
     *
     * @param key   附件信息关键字
     * @param value 附件值
     * @return
     */
    public RPCMessage addAttachment(String key, Object value) {
        if (this.attachments == null) {
            this.attachments = new HashMap<>();
        }
        this.attachments.put(key, value);
        return this;
    }
}