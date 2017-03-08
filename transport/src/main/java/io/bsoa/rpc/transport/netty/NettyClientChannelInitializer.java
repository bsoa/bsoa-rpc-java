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
package io.bsoa.rpc.transport.netty;

import io.bsoa.rpc.protocol.Protocol;
import io.bsoa.rpc.protocol.ProtocolFactory;
import io.bsoa.rpc.protocol.ProtocolInfo;
import io.bsoa.rpc.transport.ClientTransportConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/17 18:26. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class NettyClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    /**
     * ClientTransportConfig
     */
    private ClientTransportConfig transportConfig;
    
    /**
     * 客户端的ChannelHandler
     */
    private NettyClientChannelHandler clientChannelHandler;

    /**
     * Construct of
     *
     * @param clientTransport ClientTransportConfig
     */
    public NettyClientChannelInitializer(NettyClientTransport clientTransport) {
        this.transportConfig = clientTransport.getConfig();
        this.clientChannelHandler = new NettyClientChannelHandler(clientTransport);
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // 根据服务端协议，选择解码器
        String type = transportConfig.getProviderInfo().getProtocolType();
        Protocol protocol = ProtocolFactory.getProtocol(type);
        ProtocolInfo protocolInfo = protocol.protocolInfo();
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
                .addLast("clientChannelHandler", clientChannelHandler);
    }

}
