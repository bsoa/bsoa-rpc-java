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

import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.BaseMessage;
import io.bsoa.rpc.protocol.ProtocolEncoder;
import io.bsoa.rpc.protocol.ProtocolInfo;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/18 16:02. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("bsoa")
public class BsoaProtocolEncoder implements ProtocolEncoder {

    public void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        ByteBuf headBody = null;
        if (out == null) {
            out = ctx.alloc().buffer();
        }
        try {
            if (msg instanceof BaseMessage) {
//                BaseMessage base = (BaseMessage)msg;
//                if(base.getMsg() != null){
//                    write(base.getMsg(),out);
//                    base.getMsg().release();
//                }else{
//                    headBody = ctx.alloc().heapBuffer();
//                    ProtocolUtil.encode(msg, headBody);
//                    write(headBody,out);
//                }
            } else if (msg instanceof String) {
                out.writeBytes(protocolInfo.magicCode().getBytes()); // 先来两个魔术位  2+4+4=10
                out.writeInt(((String) msg).length());
                out.writeBytes(((String) msg).getBytes("UTF-8"));
            } else {
                throw new BsoaRpcException(22222, "Not support this type of Object.");
            }

        } finally {
            if (headBody != null) {
                headBody.release();
            }
        }
    }

    /**
     * 复制数据
     *
     * @param data 序列化后的数据
     * @param out  回传的数据
     */
//    private void write(ByteBuf data, ByteBuf out) {
//        int totalLength = 2 + 4 + data.readableBytes();
//        if (out.capacity() < totalLength) out.capacity(totalLength);
//        out.writeBytes(Constants.MAGICCODEBYTE); // 写入magiccode
//        int length = totalLength - 2; //  data.readableBytes() + 4  (4指的是FULLLENGTH)
//        out.writeInt(length);   //4 for Length Field
//        out.writeBytes(data, data.readerIndex(), data.readableBytes());
//        //logger.trace("out length:{}",out.readableBytes());
//    }

    private ProtocolInfo protocolInfo;

    @Override
    public void setProtocolInfo(ProtocolInfo protocolInfo) {
        this.protocolInfo = protocolInfo;
    }
}
