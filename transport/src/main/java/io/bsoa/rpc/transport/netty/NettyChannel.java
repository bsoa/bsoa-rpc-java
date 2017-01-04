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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.context.RpcContext;
import io.bsoa.rpc.transport.AbstractByteBuf;
import io.bsoa.rpc.transport.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

/**
 * <p></p>
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

    private Channel channel;

    public NettyChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public AbstractByteBuf getByteBuf() {
        return new NettyByteBuf(NettyTransportHelper.getBuffer());
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) channel.remoteAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) channel.localAddress();
    }

    @Override
    public void writeAndFlush(Object obj) {
        Future future = channel.writeAndFlush(obj);
        future.addListener((FutureListener) future1 -> {
            if (!future1.isSuccess()) {
                Throwable throwable = future1.cause();
                LOGGER.error("[JSF-23009]Failed to send to "
                        + NetUtils.channelToString(
                        RpcContext.getContext().getLocalAddress(),
                        RpcContext.getContext().getRemoteAddress())
                        + " for msg : " + obj
                        + ", Cause by:", throwable);
                //throw new RpcException("Fail to send Response msg for response:" + msg.getMsgHeader(), throwable);
            }
        });
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
