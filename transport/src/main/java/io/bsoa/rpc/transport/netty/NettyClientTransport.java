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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.listener.ResponseFuture;
import io.bsoa.rpc.message.BaseMessage;
import io.bsoa.rpc.transport.AbstractClientTransport;
import io.bsoa.rpc.transport.BsoaChannel;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/15 22:51. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("netty4")
public class NettyClientTransport extends AbstractClientTransport {

    private List<BsoaChannel> channels = new ArrayList<>();

    private boolean connected;

    @Override
    public void connect() {
        // 已经初始化，或者被复用
        if (this.connected) {
            throw new BsoaRuntimeException(22222, "Has been call connnect, Illega Access");
        }

        String host = config.getProvider().getIp();
        int port = config.getProvider().getPort();
        int num = config.getConnectionNum(); // 建立几个长连接
        int connectTimeout = config.getConnectTimeout();

        num = Math.max(1, num);
        for (int i = 0; i < num; i++) {
            Channel channel = null;
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(NettyTransportHelper.getClientIOEventLoopGroup())
                        .channel(config.isUseEpoll() ? EpollSocketChannel.class : NioSocketChannel.class)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .option(ChannelOption.ALLOCATOR, NettyTransportHelper.getByteBufAllocator())
                        .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
                        .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
                        .option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT)
                        .handler(new NettyClientChannelInitializer(config));
                // Bind and start to accept incoming connections.

                ChannelFuture channelFuture = bootstrap.connect(host, port);
                channelFuture.awaitUninterruptibly(connectTimeout, TimeUnit.MILLISECONDS);
                if (channelFuture.isSuccess()) {
                    channel = channelFuture.channel();
                    if (NetUtils.toAddressString((InetSocketAddress) channel.remoteAddress())
                            .equals(NetUtils.toAddressString((InetSocketAddress) channel.localAddress()))) {
                        // 服务端不存活时，连接左右两侧地址一样的情况
                        channel.close(); // 关掉重连
                        throw new RuntimeException("Failed to connect " + host + ":" + port
                                + ". Cause by: Remote and local address are the same");
                    }
                } else {
                    Throwable cause = channelFuture.cause();
                    throw new RuntimeException("Failed to connect " + host + ":" + port +
                            (cause != null ? ". Cause by: " + cause.getMessage() : "."));
                }
                channels.add(new NettyChannel(channel));
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                //logger.error(e.getMessage(),e);
                String errorStr = "Failed to build channel for host:" + host + " port:" + port
                        + ". Cause by: " + e.getMessage();
                RuntimeException initException = new RuntimeException(errorStr, e);
                throw initException;
            }
        }
    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public List<BsoaChannel> getChannels() {
        return channels;
    }

    Random random = new Random();

    private Channel getChannel() {
        if (channels.size() == 0) {
            throw new BsoaRpcException(22222, "No connected channel in client transport");
        } else if (channels.size() == 1) {
            return ((NettyChannel) channels.get(0)).getChannel();
        } else {
            NettyChannel channel = (NettyChannel) channels.get(random.nextInt(channels.size()));
            return channel.getChannel();
        }
    }

    @Override
    public ResponseFuture asyncSend(BaseMessage request, int timeout) {
        return null;
    }

    @Override
    public BaseMessage syncSend(BaseMessage request, int timeout) {
        ChannelFuture future=  getChannel().writeAndFlush(request.getRequestId()+"");
        return null;
    }

    public void removeFutureWhenChannelInactive() {


    }
}
