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
package io.bsoa.rpc.client;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import io.bsoa.rpc.common.BsoaConstants;

/**
 *
 *
 * Created by zhangg on 2016/7/17 15:32.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Deprecated
public class TelnetClient {

    private String ip;

    private int port;

    /**
     * 连接超时
     */
    private int connectTimeout;

    /**
     * 调用超时 SO_TIMEOUT
     */
    private final int readTimeout;

    /**
     * socket对象
     */
    private volatile transient Socket socket;

    private volatile transient InputStream in;

    private volatile transient OutputStream out;

    /**
     * @param ip             远程地址
     * @param port           远程端口
     * @param connectTimeout 连接超时
     * @param readTimeout    调用超时
     */
    public TelnetClient(String ip, int port, int connectTimeout, int readTimeout) {
        this.ip = ip;
        this.port = port;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    /**
     * 执行telnet命令，不关闭命令，需要手动调用close方法
     *
     * @param cmd 命令
     * @return 是否可连接
     */
    public String telnetJSF(String cmd) throws IOException {
        connect();

        ByteArrayOutputStream baout = null;
        try {
            // 发送请求
            out.write(cmd.getBytes(BsoaConstants.DEFAULT_CHARSET));
            out.write("\r\n".getBytes(BsoaConstants.DEFAULT_CHARSET));
            out.flush();

            baout = new ByteArrayOutputStream();
            // 解析得到的响应
            StringBuffer sb = new StringBuffer();
            byte[] bs = new byte[1024];
            int len;
            int i = 0;
            while (i < 1024 && (len = in.read(bs)) != -1) { // 防止无限循环 最多取 1M的数据
                String data = new String(bs, 0, len, BsoaConstants.DEFAULT_CHARSET);
                baout.write(bs, 0, len);
                sb.append(data);
                if (sb.length() > 4) {
                    String last = sb.substring(sb.length() - 4);
                    if ("jsf>".equals(last)) {
                        break; // 读到这个就断开连接返回
                    }
                }
                i++;
            }
            String result = new String(baout.toByteArray(), BsoaConstants.DEFAULT_CHARSET);
            result = result.endsWith("jsf>") ? result.substring(0, result.length() - 4).trim() : result.trim();
            return result;
        } catch (IOException e) {
            throw new RuntimeException("[22106]Failed to send command [" + cmd + "] to " + ip + ":" + port
                    + ", timeout is " + connectTimeout + ", soTimeout is " + readTimeout + " !", e);
        } finally {
            closeQuietly(baout); //关闭流
        }
    }

    /**
     * 连接
     *
     * @return 是否可连接
     */
    public void connect() throws IOException {
        if (socket == null) {
            socket = new Socket();
            socket.setSoTimeout(readTimeout);
            socket.connect(new InetSocketAddress(ip, port), connectTimeout);

            if (socket.isConnected()) {  // 初始化通道
                in = socket.getInputStream();
                out = socket.getOutputStream();
            }
        }
    }

    /**
     * 关闭
     *
     * @return
     */
    public void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
            }
            socket = null;
        }
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }
}
