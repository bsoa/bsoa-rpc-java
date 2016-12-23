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

import java.util.List;

import io.bsoa.rpc.ext.Extensible;
import io.bsoa.rpc.transport.AbstractByteBuf;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/17 18:39. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extensible
public interface ProtocolDecoder {
    /**
     * 设置协议基本信息
     *
     * @param protocolInfo 协议信息
     */
    void setProtocolInfo(ProtocolInfo protocolInfo);

    /**
     * 头部解码
     *
     * @param byteBuf 字节缓冲器
     * @param out     解析处理的对象列表
     */
    void decodeHeader(AbstractByteBuf byteBuf, List<Object> out);

    /**
     * body解码
     *
     * @param byteBuf 字节缓冲器
     * @param out     解析处理的对象列表
     */
    void decodeBody(AbstractByteBuf byteBuf, List<Object> out);

    /**
     * 全部解码
     *
     * @param byteBuf 字节缓冲器
     * @param out     解析处理的对象列表
     */
    void decodeAll(AbstractByteBuf byteBuf, List<Object> out);
}
