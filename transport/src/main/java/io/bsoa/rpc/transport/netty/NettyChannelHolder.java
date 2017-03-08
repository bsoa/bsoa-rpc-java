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

import java.util.concurrent.ConcurrentHashMap;

import io.bsoa.rpc.transport.AbstractChannel;
import io.netty.channel.Channel;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/03/2017/3/5 20:39. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class NettyChannelHolder {

    /**
     * io.netty.channel.Channel --> io.bsoa.rpc.transport.AbstractChannel
     */
    private static ConcurrentHashMap<Channel, AbstractChannel> allChannels
            = new ConcurrentHashMap<>();

    /**
     * 使用Netty初始化一个AbstractChannel，用于重复使用，而不是每次创建
     *
     * @param nettyChannel io.netty.channel.Channel
     * @return io.bsoa.rpc.transport.AbstractChannel
     */
    public static AbstractChannel initAbstractChannel(Channel nettyChannel) {
        AbstractChannel abstractChannel = allChannels.get(nettyChannel);
        if (abstractChannel == null) {
            synchronized (NettyChannelHolder.class) {
                abstractChannel = allChannels.get(nettyChannel);
                if (abstractChannel == null) {
                    abstractChannel = new NettyChannel(nettyChannel);
                    allChannels.put(nettyChannel, abstractChannel);
                }
            }
        }
        return abstractChannel;
    }

    /**
     * 获取Netty的Channel对应的AbstractChannel
     *
     * @param nettyChannel io.netty.channel.Channel
     * @return io.bsoa.rpc.transport.AbstractChannel
     */
    public static AbstractChannel getAbstractChannel(Channel nettyChannel) {
        return allChannels.get(nettyChannel);
    }

    /**
     * 获取Netty的Channel对应的AbstractChannel
     *
     * @param nettyChannel io.netty.channel.Channel
     * @return io.bsoa.rpc.transport.AbstractChannel
     */
    public static AbstractChannel removeAbstractChannel(Channel nettyChannel) {
        return allChannels.remove(nettyChannel);
    }
}
