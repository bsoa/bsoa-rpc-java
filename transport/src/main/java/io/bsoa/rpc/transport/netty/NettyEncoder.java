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
package io.bsoa.rpc.transport.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.protocol.Protocol;
import io.bsoa.rpc.protocol.ProtocolEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * <p>User protocol encoder, encode object to byte[]</p>
 * <p>
 * Created by zhangg on 2016/12/17 20:01. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class NettyEncoder extends MessageToByteEncoder {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(NettyEncoder.class);

    private final ProtocolEncoder protocolEncoder;

    public NettyEncoder(Protocol protocol) {
        this.protocolEncoder = protocol.encoder();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (out == null) {
            LOGGER.debug("ByteBuf out is null..");
            out = ctx.alloc().buffer();
        }
        try {
            if (msg instanceof ByteBuf) {
                out.writeBytes((ByteBuf) msg);
            } else if (msg instanceof NettyByteBuf) {
                out.writeBytes(((NettyByteBuf) msg).getByteBuf());
            } else {
                protocolEncoder.encodeAll(msg, new NettyByteBuf(out));
            }
        } catch (Exception e) {
            LOGGER.warn("Encode message " + msg.getClass() + " error on channel " + ctx.channel() + "！", e);
        }
    }
}
