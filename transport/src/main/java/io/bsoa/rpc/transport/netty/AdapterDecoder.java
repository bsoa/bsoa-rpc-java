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
package io.bsoa.rpc.transport.netty;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.transport.ServerTransportConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * <p></p>
 *
 * Created by zhangg on 2016/12/18 00:13. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class AdapterDecoder extends ByteToMessageDecoder {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(AdapterDecoder.class);

    public AdapterDecoder(NettyServerChannelHandler serverChannelHandler, ServerTransportConfig transportConfig) {
        this.serverChannelHandler = serverChannelHandler;
        this.payload = transportConfig.getPayload();
        this.telnet = transportConfig.isTelnet();
    }

    private final NettyServerChannelHandler serverChannelHandler;

    /**
     * 是否允许telnet
     */
    private boolean telnet;

    /**
     * 最大数据包大小 maxFrameLength
     */
    private int payload = 8 * 1024 * 1024;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

//        if (in.readableBytes() < 2) {
//            return;
//        }
//        Short magiccode_high = in.getUnsignedByte(0);
//        Short magiccode_low = in.getUnsignedByte(1);
//        byte b1 = magiccode_high.byteValue();
//        byte b2 = magiccode_low.byteValue();
//
//        InetSocketAddress localAddress = (InetSocketAddress) ctx.channel().localAddress();
//        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
//
//        // jsf协议
//        if (isJSF(b1, b2)) {
//            LOGGER.info("Accept jsf connection {}", NetUtils.connectToString(remoteAddress, localAddress));
//            ChannelPipeline pipeline = ctx.pipeline();
//            pipeline.addLast(new JSFDecoder(payload));
//            pipeline.addLast(new JSFEncoder());
//            pipeline.addLast(serverChannelHandler);
//            pipeline.remove(this);
//
//            pipeline.fireChannelActive(); // 重新触发连接建立事件
//        }
//        // 1.x dubbo协议
//        else if (DubboAdapter.match(b1, b2)) {
//            LOGGER.info("Accept dubbo connection {}", NetUtils.connectToString(remoteAddress, localAddress));
//            ChannelPipeline pipeline = ctx.pipeline();
//            pipeline.addLast(new DubboDecoder(payload));
//            pipeline.addLast(new DubboEncoder());
//            pipeline.addLast(serverChannelHandler);
//            pipeline.remove(this);
//
//            pipeline.fireChannelActive(); // 重新触发连接建立事件
//        }
//
//        // http协议
//        else if (isHttp(b1, b2)) {
//            if (LOGGER.isTraceEnabled()) {
//                LOGGER.trace("Accept http connection {}", NetUtils.connectToString(remoteAddress, localAddress));
//            }
//            ChannelPipeline pipeline = ctx.pipeline();
//            pipeline.addLast("decoder", new HttpRequestDecoder());
//            pipeline.addLast("http-aggregator", new HttpObjectAggregator(payload));
//            pipeline.addLast("encoder", new HttpResponseEncoder());
//            pipeline.addLast("jsonDecoder", new HttpJsonHandler(serverChannelHandler.getServerHandler()));
//            pipeline.remove(this);
//        }
//
//        // telnet
//        else {
//            LOGGER.info("Accept telnet connection {}", NetUtils.connectToString(remoteAddress, localAddress));
//
//            ChannelPipeline pipeline = ctx.pipeline();
//            pipeline.addLast(new TelnetCodec());
//            pipeline.addLast(new TelnetChannelHandler());
//            pipeline.remove(this);
//
//            if (telnet) {
//                pipeline.fireChannelActive(); // 重新触发连接建立事件
//            } else {
//                ctx.channel().writeAndFlush("Sorry! Not support telnet");
//                ctx.channel().close();
//            }
//        }
//    }
//
//    private boolean isJSF(short magic1, short magic2) {
//        return magic1 == Constants.MAGICCODEBYTE[0]
//                && magic2 == Constants.MAGICCODEBYTE[1];
//    }
//
//    private boolean isHttp(int magic1, int magic2) {
//        return (magic1 == 'G' && magic2 == 'E') || // GET
//                (magic1 == 'P' && magic2 == 'O') || // POST
//                (magic1 == 'P' && magic2 == 'U') || // PUT
//                (magic1 == 'H' && magic2 == 'E') || // HEAD
//                (magic1 == 'O' && magic2 == 'P') || // OPTIONS
//                (magic1 == 'P' && magic2 == 'A') || // PATCH
//                (magic1 == 'D' && magic2 == 'E') || // DELETE
//                (magic1 == 'T' && magic2 == 'R') || // TRACE
//                (magic1 == 'C' && magic2 == 'O');   // CONNECT
    }
}
