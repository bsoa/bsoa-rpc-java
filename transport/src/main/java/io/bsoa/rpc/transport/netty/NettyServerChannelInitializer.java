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

import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.protocol.Protocol;
import io.bsoa.rpc.protocol.ProtocolFactory;
import io.bsoa.rpc.protocol.ProtocolInfo;
import io.bsoa.rpc.transport.ServerTransportConfig;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/18 00:01. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@ChannelHandler.Sharable
public class NettyServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private NettyServerChannelHandler serverChannelHandler;

    private ConnectionChannelHandler connectionChannelHandler;

    private ServerTransportConfig transportConfig;

    public NettyServerChannelInitializer(ServerTransportConfig transportConfig) {
        this.transportConfig = transportConfig;
        this.serverChannelHandler = new NettyServerChannelHandler(transportConfig);
        this.connectionChannelHandler = new ConnectionChannelHandler(transportConfig);
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        boolean adaptive = BsoaConfigs.getBooleanValue(BsoaConfigs.TRANSPORT_SERVER_PROTOCOL_ADAPTIVE);
        if (adaptive) {
            // 支持一个端口多协议
            // 根据第一次请求识别协议，构建后面的ChannelHandler
            ch.pipeline().addLast(connectionChannelHandler)
                    .addLast(new AdapterDecoder(serverChannelHandler, transportConfig));
        } else {
            Protocol protocol = ProtocolFactory.getProtocol(transportConfig.getProtocolType());
            ProtocolInfo protocolInfo = protocol.protocolInfo();
            ch.pipeline().addLast(connectionChannelHandler)
                .addLast("frame", protocolInfo.isLengthFixed() ?
                    new FixedLengthFrameDecoder(protocolInfo.lengthFieldLength()) :
                    new LengthFieldBasedFrameDecoder(protocolInfo.maxFrameLength(),
                            protocolInfo.lengthFieldOffset(),
                            protocolInfo.lengthFieldLength(),
                            protocolInfo.lengthAdjustment(),
                            protocolInfo.initialBytesToStrip(),
                            false)) // TODO failfast ??
                    .addLast("encoder", new NettyEncoder(protocol))
                    .addLast("decoder", new NettyDecoder(protocol))
                    .addLast("logging", new LoggingHandler(LogLevel.INFO))
                    .addLast("serverChannelHandler", serverChannelHandler);
        }
    }
}
