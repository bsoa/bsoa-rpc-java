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

import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.transport.AbstractByteBuf;
import io.bsoa.rpc.transport.AbstractChannel;
import io.bsoa.rpc.transport.ChannelContext;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * <p>包装了Netty的Channel为AbstractChannel</p>
 * <p>
 * Created by zhangg on 2016/12/17 17:50. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class NettyChannel implements AbstractChannel {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(NettyChannel.class);

    /**
     * 长连接上下文
     */
    private ChannelContext context;

    /**
     * Netty的Channel
     */
    private Channel channel;

    public NettyChannel(Channel channel) {
        this.channel = channel;
        this.context = new ChannelContext();
    }

    @Override
    public String getContainer() {
        return "netty";
    }

    @Override
    public ChannelContext context() {
        return context;
    }

    @Override
    public AbstractByteBuf getByteBuf() {
        return new NettyByteBuf(NettyTransportHelper.getBuffer());
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return (InetSocketAddress) channel.remoteAddress();
    }

    @Override
    public InetSocketAddress localAddress() {
        return (InetSocketAddress) channel.localAddress();
    }

    @Override
    public void writeAndFlush(Object obj) {
        Future future = channel.writeAndFlush(obj);
        future.addListener((FutureListener) future1 -> {
            if (!future1.isSuccess()) {
                Throwable throwable = future1.cause();
                LOGGER.error("[23009]Failed to send to "
                        + NetUtils.channelToString(this.localAddress(), this.remoteAddress())
                        + " for msg : " + obj
                        + ", Cause by:", throwable);
                //throw new RpcException("Fail to send Response msg for response:" + msg.getMsgHeader(), throwable);
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return channel.isOpen();
    }

    public void close() {
        if (channel.isOpen() || channel.isActive()) {
            channel.close();
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
