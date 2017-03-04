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

import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.protocol.TelnetHandler;
import io.bsoa.rpc.protocol.TelnetHandlerFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class TelnetChannelHandler extends ChannelInboundHandlerAdapter {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(TelnetChannelHandler.class);
    /**
     * 客户端字符集
     */
    public static final Map<Channel, String> charsetMap = new ConcurrentHashMap<Channel, String>();

    /**
     * The constant HELP.
     */
    private static final String HELP = "help";
    /**
     * The constant EXIT.
     */
    private static final String EXIT = "exit";

    /**
     * 初始化
     */
    private static TelnetHandler HELP_HANDLER = TelnetHandlerFactory.getHandler(HELP);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        String message = msg.toString().trim();
        if (message.length() == 0) {
            return;
        }

        String command = "";
        //判断命令
        if (message.indexOf(" ") > 0) {
            int i = message.indexOf(' ');
            command = message.substring(0, i).trim();
            message = message.substring(i + 1).trim();
        } else {
            command = message;
            message = "";
        }
        //调用指定命令
        String result = "";
        TelnetHandler handler = TelnetHandlerFactory.getHandler(command);
        if (handler != null) {
            result = handler.telnet(new NettyChannel(ctx.channel()), message);
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append("ERROR:You input the command:[" + command + " " + message + "] is not exist!!\r\n");
            result = HELP_HANDLER.telnet(new NettyChannel(ctx.channel()), message);
            sb.append(result);
            sb.append("Please input again!\r\n");
            result = sb.toString();
        }
        if (result != null && !"".equals(result.trim())) {
            ctx.writeAndFlush(result + "\r\n");
            ctx.writeAndFlush("jsf>");
        }
        if (EXIT.equalsIgnoreCase(command)) {
            ctx.channel().close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        LOGGER.error("Telnet error", cause);
        ctx.channel().writeAndFlush(cause.getMessage());
    }

//    @Override
//    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        BaseServerHandler.addChannel(ctx.channel());
//    }
//
//    /**
//     * 允许执行远程invoke命令的连接，前面进行过sudo操作
//     */
//    public static final Set<Channel> ALLOW_INVOKE_CHANNELS = new ConcurrentHashSet<Channel>();

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        LOGGER.info("Disconnected telnet from {}", NetUtils.channelToString(channel.remoteAddress(), channel.localAddress()));
//        BaseServerHandler.removeChannel(channel);
//        charsetMap.remove(channel);
//        ALLOW_INVOKE_CHANNELS.remove(channel);
    }
}
