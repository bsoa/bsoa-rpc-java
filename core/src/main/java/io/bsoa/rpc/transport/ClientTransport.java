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
package io.bsoa.rpc.transport;

import io.bsoa.rpc.context.AsyncContext;
import io.bsoa.rpc.ext.Extensible;
import io.bsoa.rpc.invoke.CallbackTask;
import io.bsoa.rpc.invoke.StreamTask;
import io.bsoa.rpc.message.BaseMessage;
import io.bsoa.rpc.message.HeadKey;
import io.bsoa.rpc.message.HeartbeatResponse;
import io.bsoa.rpc.message.MessageConstants;
import io.bsoa.rpc.message.NegotiationRequest;
import io.bsoa.rpc.message.NegotiationResponse;
import io.bsoa.rpc.message.ResponseFuture;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.protocol.ProtocolFactory;
import io.bsoa.rpc.protocol.ProtocolNegotiator;

/**
 * Created by zhangg on 2016/7/17 15:37.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extensible(singleton = false)
public abstract class ClientTransport {

    /**
     * 客户端配置
     */
    protected ClientTransportConfig transportConfig;

    /**
     * 客户端配置
     *
     * @param transportConfig 客户端配置
     */
    protected ClientTransport(ClientTransportConfig transportConfig) {
        this.transportConfig = transportConfig;
    }

    /**
     * 返回配置
     *
     * @return config
     */
    public ClientTransportConfig getConfig() {
        return transportConfig;
    }

    /**
     * 建立长连接
     */
    public abstract void connect();

    /**
     * 断开连接
     */
    public abstract void disconnect();

    /**
     * 销毁（最好是通过工厂模式销毁，这样可以清理缓存）
     */
    public abstract void destroy();

    /**
     * 是否可用（有可用的长连接）
     *
     * @return the boolean
     */
    public abstract boolean isAvailable();

    /**
     * 设置长连接
     *
     * @param channel the channel
     * @return
     */
    public abstract void setChannel(AbstractChannel channel);

    /**
     * 得到长连接
     *
     * @return channel
     */
    public abstract AbstractChannel getChannel();

    /**
     * 当前请求数
     *
     * @return 当前请求数 int
     */
    public abstract int currentRequests();

    /**
     * 异步调用
     *
     * @param message 消息
     * @param timeout 超时时间
     * @return 异步Future response future
     */
    public abstract ResponseFuture asyncSend(BaseMessage message, int timeout);

    /**
     * 同步调用
     *
     * @param message 消息
     * @param timeout 超时时间
     * @return RpcResponse base message
     */
    public abstract BaseMessage syncSend(BaseMessage message, int timeout);

    /**
     * 单向调用
     *
     * @param message 消息
     * @param timeout 超时时间
     */
    public abstract void oneWaySend(BaseMessage message, int timeout);

    /**
     * Receive rpc response.
     *
     * @param response the response
     */
    public abstract void receiveRpcResponse(RpcResponse response);

    /**
     * Receive heartbeat response.
     *
     * @param response the response
     */
    public abstract void receiveHeartbeatResponse(HeartbeatResponse response);

    /**
     * Receive negotiation response.
     *
     * @param response the response
     */
    public abstract void receiveNegotiationResponse(NegotiationResponse response);

    /**
     * Handle negotiation request.
     *
     * @param request the request
     */
    public void handleNegotiationRequest(NegotiationRequest request) {
        ProtocolNegotiator negotiator =
                ProtocolFactory.getProtocol(transportConfig.getProviderInfo().getProtocolType()).negotiator();
        if (negotiator != null) {
            NegotiationResponse response = negotiator.handleRequest(request, getChannel());
            if (request.getDirectionType() == MessageConstants.DIRECTION_FORWARD) {
                getChannel().writeAndFlush(response); // 不是单向的，需要返回
            }
        }
    }

    /**
     * Handle rpc request.
     *
     * @param request the request
     */
    public void handleRpcRequest(RpcRequest request) {
        String callbackInsKey = (String) request.getHeadKey(HeadKey.CALLBACK_INS_KEY);
        if (callbackInsKey != null) { // 服务端发来的callback请求
            CallbackTask task = new CallbackTask(request, getChannel());
            AsyncContext.getAsyncThreadPool().execute(task);
        }
        String streamInsKey = (String) request.getHeadKey(HeadKey.STREAM_INS_KEY);
        if (streamInsKey != null) {  // 服务端发给客户的stream请求
            StreamTask task = new StreamTask(request, getChannel());
            AsyncContext.getAsyncThreadPool().execute(task);
        }
    }
}
