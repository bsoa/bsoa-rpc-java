/*
 * *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  * <p>
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  * <p>
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package io.bsoa.rpc.protocol.bsoa;

import java.util.List;

import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.protocol.ProtocolDecoder;
import io.bsoa.rpc.protocol.ProtocolInfo;
import io.bsoa.rpc.transport.AbstractByteBuf;
import io.bsoa.rpc.transport.netty.NettyByteBuf;
import io.netty.buffer.ByteBuf;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/18 16:03. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("bsoa")
public class BsoaProtocolDecoder implements ProtocolDecoder {

    private ProtocolInfo protocolInfo;

    @Override
    public void setProtocolInfo(ProtocolInfo protocolInfo) {
        this.protocolInfo = protocolInfo;
    }

    @Override
    public void decodeHeader(AbstractByteBuf byteBuf, List<Object> out) {
        NettyByteBuf nettyByteBuf = (NettyByteBuf) byteBuf;
        ByteBuf in = nettyByteBuf.getByteBuf();
    }

    @Override
    public void decodeBody(AbstractByteBuf byteBuf, List<Object> out) {

    }

    @Override
    public void decodeAll(AbstractByteBuf byteBuf, List<Object> out) {
//        System.out.println("readIndex--->: " + in.readerIndex());
//        int length = in.readInt();
//        String s = (String) in.readCharSequence(length, Charset.forName("utf-8"));
//        out.add(s);
//        System.out.println("--msg-->" + s);
    }
}
