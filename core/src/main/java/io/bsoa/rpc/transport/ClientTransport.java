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
package io.bsoa.rpc.transport;

import io.bsoa.rpc.ext.Extensible;
import io.bsoa.rpc.message.ResponseFuture;
import io.bsoa.rpc.message.BaseMessage;

/**
 * Created by zhangg on 2016/7/17 15:37.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extensible(singleton = false)
public interface ClientTransport {

    /**
     * 设置配置
     *
     * @param config
     */
    void setConfig(ClientTransportConfig config);

    /**
     * 返回配置
     *
     * @return
     */
    ClientTransportConfig getConfig();

    /**
     * 建立长连接
     */
    public void connect();

    /**
     * 断开连接
     */
    public void disconnect();

    /**
     * 销毁
     */
    public void destroy();

    /**
     * 是否可用（有可用的长连接）
     *
     * @return the boolean
     */
    public boolean isAvailable();

    /**
     * 得到长连接
     *
     * @return
     */
    public AbstractChannel getChannel();

    /**
     * 当前请求数
     *
     * @return 当前请求数
     */
    public default int currentRequests() {
        return 0;
    }

    /**
     * 异步调用
     *
     * @param message 消息
     * @param timeout 超时时间
     * @return 异步Future
     */
    public ResponseFuture asyncSend(BaseMessage message, int timeout);

    /**
     * 同步调用
     *
     * @param message 消息
     * @param timeout 超时时间
     * @return RpcResponse
     */
    public BaseMessage syncSend(BaseMessage message, int timeout);

    /**
     * 单向调用
     *
     * @param message 消息
     * @param timeout 超时时间
     */
    public void oneWaySend(BaseMessage message, int timeout);

}
