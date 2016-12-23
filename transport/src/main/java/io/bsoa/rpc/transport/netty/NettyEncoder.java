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
package io.bsoa.rpc.transport.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.protocol.Protocol;
import io.bsoa.rpc.protocol.ProtocolEncoder;
import io.bsoa.rpc.protocol.bsoa.BsoaProtocolEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * <p></p>
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
        ByteBuf headBody = null;
        if (out == null) {
            LOGGER.debug("ByteBuf out is null..");
            out = ctx.alloc().buffer();
        }
        try {
            if (protocolEncoder instanceof BsoaProtocolEncoder) {
                ((BsoaProtocolEncoder) protocolEncoder).encode(ctx, msg, new NettyByteBuf(out));
            } else {
                throw new BsoaRpcException("Not Supported!");
            }
        } finally {
            if (headBody != null) headBody.release();
        }

    }
}
