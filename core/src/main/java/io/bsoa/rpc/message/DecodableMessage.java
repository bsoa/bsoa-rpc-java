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

import io.bsoa.rpc.transport.AbstractByteBuf;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/29 01:43. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public abstract class DecodableMessage extends BaseMessage {

    /**
     * 未解析的数据
     */
    private transient AbstractByteBuf byteBuf;

    /**
     * 子类必须实现
     *
     * @param messageType 消息类型
     */
    protected DecodableMessage(byte messageType) {
        super(messageType);
    }

    public AbstractByteBuf getByteBuf() {
        return byteBuf;
    }

    public DecodableMessage setByteBuf(AbstractByteBuf byteBuf) {
        this.byteBuf = byteBuf;
        return this;
    }

    private DecodableMessage() {
        super((byte) 0);
    }
}
