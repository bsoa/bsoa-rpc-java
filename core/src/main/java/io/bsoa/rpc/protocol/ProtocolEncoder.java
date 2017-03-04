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
package io.bsoa.rpc.protocol;

import io.bsoa.rpc.ext.Extensible;
import io.bsoa.rpc.transport.AbstractByteBuf;

/**
 * <p>协议编码器（注意，编码器应该不进行调用ByteBuf参数的释放，除非是编码过程中自己生产的Bytebuf）</p>
 * <p>
 * Created by zhangg on 2016/12/17 18:38. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extensible
public abstract class ProtocolEncoder {

    /**
     * 协议基本信息
     */
    protected ProtocolInfo protocolInfo;

    /**
     * 构造函数
     *
     * @param protocolInfo 协议基本信息
     */
    public ProtocolEncoder(ProtocolInfo protocolInfo) {
        this.protocolInfo = protocolInfo;
    }

    /**
     * 头部编码
     *
     * @param object  对象
     * @param byteBuf 字节缓冲器
     */
    public abstract void encodeHeader(Object object, AbstractByteBuf byteBuf);

    /**
     * body编码
     *
     * @param object  对象
     * @param byteBuf 字节缓冲器
     */
    public abstract void encodeBody(Object object, AbstractByteBuf byteBuf);

    /**
     * 全部编码
     *
     * @param object  对象
     * @param byteBuf 字节缓冲器
     */
    public abstract void encodeAll(Object object, AbstractByteBuf byteBuf);
}
