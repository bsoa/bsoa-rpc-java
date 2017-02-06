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

import java.net.InetSocketAddress;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.protocol.Protocol;
import io.bsoa.rpc.protocol.ProtocolFactory;
import io.bsoa.rpc.protocol.ProtocolInfo;
import io.bsoa.rpc.transport.ServerTransportConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * <p>同一个端口支持多种协议</p>
 * <p>根据第一次请求的前几位自动判断，一个长连接只能传递一种协议</p>
 * <p>
 * Created by zhangg on 2016/12/18 00:13. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class AdapterDecoder extends ByteToMessageDecoder {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(AdapterDecoder.class);

    /**
     * NettyServerChannelHandler
     */
    private final NettyServerChannelHandler serverChannelHandler;

    /**
     * 是否允许telnet
     */
    private boolean telnet;

    /**
     * Instantiates a new Adapter decoder.
     *
     * @param serverChannelHandler the server channel handler
     * @param transportConfig      the transport config
     */
    public AdapterDecoder(NettyServerChannelHandler serverChannelHandler, ServerTransportConfig transportConfig) {
        this.serverChannelHandler = serverChannelHandler;
        this.telnet = transportConfig.isTelnet();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 判断下，已经注册的头协议
        int offset = ProtocolFactory.getMaxMagicOffset();
        if (in.readableBytes() < offset) {
            return;
        }
        // 读取头几位
        byte[] magicHeadBytes = new byte[offset];
        in.readBytes(magicHeadBytes);
        in.readerIndex(in.readerIndex() - offset);
        // 自动判断协议
        Protocol protocol = ProtocolFactory.adaptiveProtocol(magicHeadBytes);

        InetSocketAddress localAddress = (InetSocketAddress) ctx.channel().localAddress();
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();

        if (protocol != null) {
            ChannelPipeline pipeline = ctx.pipeline();
            ProtocolInfo protocolInfo = protocol.protocolInfo();
            if (protocolInfo.getNetProtocol() == ProtocolInfo.NET_PROTOCOL_TCP) {
                LOGGER.info("Accept tcp connection of protocol:{} {}", protocolInfo.getName(),
                        NetUtils.connectToString(remoteAddress, localAddress));
                pipeline.addLast("frame", protocolInfo.isLengthFixed() ?
                        new FixedLengthFrameDecoder(protocolInfo.lengthFieldLength()) :
                        new LengthFieldBasedFrameDecoder(protocolInfo.maxFrameLength(),
                                protocolInfo.lengthFieldOffset(),
                                protocolInfo.lengthFieldLength(),
                                protocolInfo.lengthAdjustment(),
                                protocolInfo.initialBytesToStrip(),
                                false)) // TODO failfast ??
                        .addLast("encoder", new NettyEncoder(protocol))
                        .addLast("decoder", new NettyDecoder(protocol))
                        .addLast("logging", new LoggingHandler(LogLevel.DEBUG))
                        .addLast("serverChannelHandler", serverChannelHandler);
                pipeline.remove(this);
                pipeline.fireChannelActive(); // 重新触发连接建立事件
            } else if (protocolInfo.getNetProtocol() == ProtocolInfo.NET_PROTOCOL_HTTP) {
//                FIXME
//                if (LOGGER.isTraceEnabled()) {
//                    LOGGER.trace("Accept http connection {}", NetUtils.connectToString(remoteAddress, localAddress));
//                }
//                pipeline.addLast("decoder", new HttpRequestDecoder());
//                pipeline.addLast("http-aggregator", new HttpObjectAggregator(payload));
//                pipeline.addLast("encoder", new HttpResponseEncoder());
//                pipeline.addLast("jsonDecoder", new HttpJsonHandler(serverChannelHandler.getServerHandler()));
//                pipeline.remove(this);
            }
        } else { //telnet
            LOGGER.info("Accept telnet connection {}", NetUtils.connectToString(remoteAddress, localAddress));

            ChannelPipeline pipeline = ctx.pipeline();
            pipeline.addLast(new TelnetCodec());
            pipeline.addLast(new TelnetChannelHandler());
            pipeline.remove(this);

            if (telnet) {
                pipeline.fireChannelActive(); // 重新触发连接建立事件
            } else {
                ctx.channel().writeAndFlush("Sorry! Not support telnet");
                ctx.channel().close();
            }
        }
    }
}
