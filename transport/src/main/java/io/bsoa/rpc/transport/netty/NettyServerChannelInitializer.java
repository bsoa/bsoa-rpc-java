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

import io.bsoa.rpc.transport.ServerTransportConfig;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/18 00:01. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@ChannelHandler.Sharable
public class NettyServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    /**
     * slf4j Logger for this class
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(NettyServerChannelInitializer.class);

    private NettyServerChannelHandler serverChannelHandler;

    private ServerTransportConfig transportConfig;

    public NettyServerChannelInitializer(ServerTransportConfig transportConfig) {
        this.transportConfig = transportConfig;
        this.serverChannelHandler = new NettyServerChannelHandler(transportConfig);
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        int currentChannelNum = serverChannelHandler.getChannelNum(); // 当前连接数
        // 刚建立连接，不管是啥长连接
        if (transportConfig.getMaxConnection() > 0 && currentChannelNum >= transportConfig.getMaxConnection()) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Maximum connection {} have been reached, cannot create channel any more",
                        transportConfig.getMaxConnection());
            }
            ch.close();
            return;
        }

        ch.pipeline().addLast("adapter", new AdapterDecoder(serverChannelHandler, transportConfig));
    }
}
