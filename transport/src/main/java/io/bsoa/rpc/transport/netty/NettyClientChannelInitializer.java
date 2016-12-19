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

import io.bsoa.rpc.common.type.ProtocolType;
import io.bsoa.rpc.protocol.Protocol;
import io.bsoa.rpc.protocol.ProtocolFactory;
import io.bsoa.rpc.protocol.ProtocolInfo;
import io.bsoa.rpc.transport.ClientTransportConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/17 18:26. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class NettyClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    private NettyClientChannelHandler clientChannelHandler;

    private ClientTransportConfig transportConfig;

    public NettyClientChannelInitializer(ClientTransportConfig transportConfig) {
        this.transportConfig = transportConfig;
        this.clientChannelHandler = new NettyClientChannelHandler(transportConfig);
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // 根据服务端协议，选择解码器
        ProtocolType type = transportConfig.getProvider().getProtocolType();
        Protocol protocol = ProtocolFactory.getProtocol(type.name());
        ProtocolInfo protocolInfo = protocol.protocolInfo();
        pipeline.addLast("frame", protocolInfo.isLengthFixed() ?
                new FixedLengthFrameDecoder(protocolInfo.lengthFieldLength()) :
                new LengthFieldBasedFrameDecoder(protocolInfo.maxFrameLength(),
                        protocolInfo.lengthFieldOffset(),
                        protocolInfo.lengthFieldLength(),
                        protocolInfo.lengthAdjustment(),
                        protocolInfo.initialBytesToStrip(),
                        false)); // TODO failfast ??
        pipeline.addLast("encoder", new NettyEncoder(protocol));
        pipeline.addLast("decoder", new NettyDecoder(protocol));
        pipeline.addLast("clientChannelHandler", clientChannelHandler);
    }

    public NettyClientChannelHandler getClientChannelHandler() {
        return clientChannelHandler;
    }
}
