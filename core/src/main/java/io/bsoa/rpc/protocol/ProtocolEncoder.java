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
package io.bsoa.rpc.protocol;

import io.bsoa.rpc.ext.Extensible;
import io.bsoa.rpc.transport.AbstractByteBuf;

/**
 * <p>协议编码器</p>
 * <p>
 * Created by zhangg on 2016/12/17 18:38. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extensible
public interface ProtocolEncoder {

    /**
     * 设置协议基本信息
     *
     * @param protocolInfo 协议信息
     */
    public void setProtocolInfo(ProtocolInfo protocolInfo);

    /**
     * 头部编码
     *
     * @param object  对象
     * @param byteBuf 字节缓冲器
     */
    void encodeHeader(Object object, AbstractByteBuf byteBuf);

    /**
     * body编码
     *
     * @param object  对象
     * @param byteBuf 字节缓冲器
     */
    void encodeBody(Object object, AbstractByteBuf byteBuf);

    /**
     * 全部编码
     *
     * @param object  对象
     * @param byteBuf 字节缓冲器
     */
    void encodeAll(Object object, AbstractByteBuf byteBuf);
}
