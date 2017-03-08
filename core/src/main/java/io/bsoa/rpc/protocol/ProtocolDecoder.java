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
package io.bsoa.rpc.protocol;

import io.bsoa.rpc.ext.Extensible;
import io.bsoa.rpc.transport.AbstractByteBuf;

/**
 * <p>协议解码器（注意，解码器应该不进行调用ByteBuf参数的释放，除非是解码过程中自己生产的ByteBuf）</p>
 * <p>
 * Created by zhangg on 2016/12/17 18:39. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extensible
public abstract class ProtocolDecoder {

    /**
     * 协议基本信息
     */
    protected ProtocolInfo protocolInfo;

    /**
     * 构造函数
     *
     * @param protocolInfo 协议基本信息
     */
    public ProtocolDecoder(ProtocolInfo protocolInfo) {
        this.protocolInfo = protocolInfo;
    }

    /**
     * 头部解码
     *
     * @param byteBuf 字节缓冲器
     * @param out     解析处理的对象列表
     */
    public abstract Object decodeHeader(AbstractByteBuf byteBuf, Object out);

    /**
     * body解码
     *
     * @param byteBuf 字节缓冲器
     * @param out     解析处理的对象列表
     */
    public abstract Object decodeBody(AbstractByteBuf byteBuf, Object out);

    /**
     * 全部解码
     *
     * @param byteBuf 字节缓冲器
     * @param out     解析处理的对象列表
     */
    public abstract Object decodeAll(AbstractByteBuf byteBuf, Object out);
}
