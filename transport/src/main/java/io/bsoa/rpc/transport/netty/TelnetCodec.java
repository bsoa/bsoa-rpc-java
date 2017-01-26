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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.json.TelnetChannelHandler;
import io.bsoa.rpc.common.utils.StringUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/17 17:36. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class TelnetCodec extends ByteToMessageCodec<Object> {

    private static final Logger logger = LoggerFactory.getLogger(TelnetCodec.class);

    private static final List<?> EXIT = Arrays.asList(
            new byte[]{3} /* Windows Ctrl+C */,
            new byte[]{-1, -12, -1, -3, 6} /* Linux Ctrl+C */,
            new byte[]{-1, -19, -1, -3, 6} /* Linux Pause */);
    private static final List<?> ENTER = Arrays.asList(
            new byte[]{'\r', '\n'} /* Windows Enter */,
            new byte[]{'\n'} /* Linux Enter */);
    private static final byte[] UP = new byte[]{27, 91, 65};

    private static final byte[] DOWN = new byte[]{27, 91, 66};

    /**
     * 执行命令历史
     */
    private List<String> historyList = new LinkedList<String>();

    /**
     * 翻阅命令索引
     */
    private int historyIndex = -1;

    @Override
    public void decode(ChannelHandlerContext context, ByteBuf paramByteBuf, List<Object> paramList) throws Exception {
        int length = paramByteBuf.readableBytes();
        if (length == 0) {
            return;
        }
        byte message[] = new byte[length];
        paramByteBuf.readBytes(message);
        String charset = this.getEncoding(message);
        if (charset != null) {
            TelnetChannelHandler.charsetMap.put(context.channel(), charset);
        }
        //处理退格键
        if (message[length - 1] == '\b') {
            if (length == 1) {
                context.channel().writeAndFlush(">");
                paramByteBuf.clear();
                return;
            }
            //判断多字节字符
            boolean isDouble = this.isDoubleByteChar(message);
            if (isDouble) {
                StringBuilder sb = new StringBuilder();
                sb.append("\b").append(" ").append(" ").append("\b").append("\b");
                context.channel().writeAndFlush(sb.toString());
            } else {
                context.channel().writeAndFlush(new String(new byte[]{32, 8}));
            }
            length = isDouble ? length - 3 : length - 2;
            byte result[] = new byte[length];
            System.arraycopy(message, 0, result, 0, length);
            paramByteBuf.clear();
            paramByteBuf.writeBytes(result);
            return;
        }
        //上下键翻阅历史命令
        boolean up = endsWith(message, UP);
        boolean down = endsWith(message, DOWN);
        if (up || down) {
            if (historyList != null && historyList.size() > 0) {
                if (historyIndex == -1) {
                    historyIndex = historyList.size() - 1;
                }
                String command = null;
                if (up) {
                    historyIndex = historyIndex - 1 > 0 ? historyIndex - 1 : historyList.size() - 1;
                } else {
                    historyIndex = historyIndex + 1 >= historyList.size() ? 0 : historyIndex + 1;
                }
                if (historyIndex >= 0 && historyIndex < historyList.size()) {
                    command = historyList.get(historyIndex);
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i <= message.length; i++) {
                    sb.append("\b");
                }
                for (int i = 0; i <= message.length; i++) {
                    sb.append(" ");
                }
                for (int i = 0; i <= message.length; i++) {
                    sb.append("\b");
                }
                paramByteBuf.clear();
                command = StringUtils.trimToEmpty(command);
                paramByteBuf.writeBytes(command.getBytes(charset));
                context.channel().writeAndFlush(sb.append("jsf>").append(command).toString());
                return;
            } else {
                paramByteBuf.clear();
                context.channel().writeAndFlush("");
                return;
            }
        }

        //退出操作
        for (Object command : EXIT) {
            if (endsWith(message, (byte[]) command)) {
                context.channel().close();
                return;
            }
        }
        //回车键：一条命令输入完毕
        byte[] enter = null;
        for (Object command : ENTER) {
            if (endsWith(message, (byte[]) command)) {
                enter = (byte[]) command;
                break;
            }
        }
        if (enter == null) {
            paramByteBuf.resetReaderIndex();
            return;
        }
        send(message, context, paramList);
    }


    private void send(byte[] message, ChannelHandlerContext context, List<Object> paramList) {
        String charset = TelnetChannelHandler.charsetMap.get(context.channel());
        if (charset == null) {
            charset = this.getEncoding(message);
        }
        String temp = null;
        try {
            temp = new String(message, charset).trim();
        } catch (UnsupportedEncodingException e) {
            logger.error("", e);
            return;
        }
        if ("".equals(temp)) {
            context.channel().writeAndFlush("jsf>");
            return;
        }
        historyIndex = -1;
        historyList.add(temp);
        paramList.add(temp);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        historyList.clear();
        ctx.fireChannelInactive();
    }


    @Override
    public void encode(ChannelHandlerContext context, Object obj, ByteBuf paramByteBuf) throws Exception {
        String paramI = obj.toString();
        if (paramI.length() == 0) {
            return;
        }
        String charset = TelnetChannelHandler.charsetMap.get(context.channel());
        byte msg[] = paramI.getBytes(charset == null ? Charset.defaultCharset().displayName() : charset);
        paramByteBuf.writeBytes(msg);
    }

    private String getEncoding(byte message[]) {
        try {
            return Charset.forName("utf-8").displayName();
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
        return Charset.defaultCharset().displayName();
    }

    /**
     * 判断全角字符
     *
     * @param message 消息体
     * @return 是否全角
     */
    private boolean isDoubleByteChar(byte message[]) {
        if (message.length == 2) {
            return false;
        }
        return !(message.length > 2 && message[message.length - 2] > 0);
    }

    /**
     * 判断某字符结尾
     *
     * @param message 消息体
     * @param command 结尾命令
     * @return 是否某字符串结尾
     * @throws IOException
     */
    private static boolean endsWith(byte[] message, byte[] command) throws IOException {
        if (message.length < command.length) {
            return false;
        }
        int offset = message.length - command.length;
        for (int i = command.length - 1; i >= 0; i--) {
            if (message[offset + i] != command[i]) {
                return false;
            }
        }
        return true;
    }
}
