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

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.transport.ServerTransportConfig;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/18 00:09. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@ChannelHandler.Sharable
public class ConnectionChannelHandler extends ChannelInboundHandlerAdapter {
    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ConnectionChannelHandler.class);

    private final ServerTransportConfig transportConfig;

    public ConnectionChannelHandler(ServerTransportConfig transportConfig) {
        this.transportConfig = transportConfig;
    }

    private AtomicInteger counter = new AtomicInteger(0);

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        /*
         * 最好其实是实现channelActive方法，
         * 但是AdapterDecoder会重新fireChannelActive，导致重复执行，所以用此事件
         */
        int now = counter.incrementAndGet();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Connected from {}, now connection is {}",
                    NetUtils.channelToString(ctx.channel().remoteAddress(), ctx.channel().localAddress()), now);
        }
        // 刚建立连接直接计数器加一，不管是长连接
        if (now > transportConfig.getMaxConnection()) {
            LOGGER.error("Maximum connection {} have been reached, cannot create channel any more",
                    transportConfig.getMaxConnection());
            ctx.channel().close();
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        int now = counter.decrementAndGet();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Disconnected from {}, now connection is {}",
                    NetUtils.channelToString(ctx.channel().remoteAddress(), ctx.channel().localAddress()), now);
        }
    }
}
