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
package io.bsoa.rpc.server.bsoa;

import io.bsoa.rpc.base.Invoker;
import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.BsoaOptions;
import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.invoke.StreamTask;
import io.bsoa.rpc.message.HeadKey;
import io.bsoa.rpc.message.HeartbeatRequest;
import io.bsoa.rpc.message.HeartbeatResponse;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.MessageConstants;
import io.bsoa.rpc.message.NegotiationRequest;
import io.bsoa.rpc.message.NegotiationResponse;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.protocol.Protocol;
import io.bsoa.rpc.protocol.ProtocolFactory;
import io.bsoa.rpc.protocol.ProtocolNegotiator;
import io.bsoa.rpc.server.BusinessPool;
import io.bsoa.rpc.server.ServerHandler;
import io.bsoa.rpc.transport.AbstractChannel;
import io.bsoa.rpc.transport.ClientTransport;
import io.bsoa.rpc.transport.ClientTransportFactory;
import io.bsoa.rpc.transport.ServerTransportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/22 23:05. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class BsoaServerHandler implements ServerHandler {

    /**
     * slf4j Logger for class
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(BsoaServerHandler.class);
    
    /**
     * 当前handler的Invoker列表 一个接口+alias对应一个Invoker
     * interface+alias --> Invoker
     */
    private ConcurrentHashMap<String, Invoker> instanceMap = new ConcurrentHashMap<>();

    /**
     * all connected client channel of this server
     */
    private ConcurrentHashMap<String, AbstractChannel> clientChannels = new ConcurrentHashMap<>();

    /**
     * Server Transport Config
     */
    private final ServerTransportConfig transportConfig;

    /**
     * biz thread pool
     */
    private ThreadPoolExecutor bizThreadPool;

    /**
     * Instantiates a new Bsoa server handler.
     *
     * @param transportConfig the transport config
     */
    public BsoaServerHandler(ServerTransportConfig transportConfig) {
        this.transportConfig = transportConfig;
        this.bizThreadPool = BusinessPool.getBusinessPool(this.transportConfig);
    }

    /**
     * Register processor.
     *
     * @param instanceName the instance name
     * @param instance     the instance
     */
    public void registerProcessor(String instanceName, Invoker instance) {
        instanceMap.put(instanceName, instance);
    }

    /**
     * Un register processor.
     *
     * @param instanceName the instance name
     */
    public void unRegisterProcessor(String instanceName) {
        if (instanceMap.containsKey(instanceName)) {
            instanceMap.remove(instanceName);
        }
    }

    /**
     * Gets invoker.
     *
     * @param instanceName the instance name
     * @return the invoker
     */
    public Invoker getInvoker(String instanceName) {
        return instanceMap.get(instanceName);
    }

    @Override
    public void handleRpcRequest(RpcRequest request, AbstractChannel channel) {
        try {
            // 丢到业务线程池去执行 TODO dispatch
//            RpcResponse rpcResponse = MessageBuilder.buildRpcResponse(rpcRequest);
//            rpcResponse.setReturnData("hello, this is response!");
//            channel.writeAndFlush(rpcResponse);
            try {
                String streamInsKey = (String) request.getHeadKey(HeadKey.STREAM_INS_KEY);
                if (streamInsKey != null) { // 服务端发给客户的stream请求
                    StreamTask task = new StreamTask(request, channel);
                    bizThreadPool.submit(task);
                } else {
                    BsoaTask task = new BsoaTask(this, request, channel, BsoaConstants.DEFAULT_METHOD_PRIORITY);
                    bizThreadPool.submit(task);
                }
            } catch (RejectedExecutionException e) {
                // 表示我很忙，等20s再来调我
                NegotiationRequest negotiationRequest = MessageBuilder.buildNegotiationRequest()
                        .setCmd("serverBusy").setData("{\"waitTime\": 20000}");
                broadcastNegotiation(negotiationRequest);
                throw e;
            }
        } catch (BsoaRpcException e) {
            throw e;
        } catch (Exception e) {
            throw new BsoaRpcException(22222, e);
        }
    }

    /**
     * Broadcast negotiation.
     *
     * @param negotiationRequest the negotiation request
     */
    protected void broadcastNegotiation(NegotiationRequest negotiationRequest){
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("Broadcast negotiation to {} clients: {}",
                    clientChannels.size(), negotiationRequest.getCmd());
        }
        negotiationRequest.setDirectionType(MessageConstants.DIRECTION_ONEWAY);// 单向，不求响应
        int timeout = BsoaConfigs.getIntValue(BsoaOptions.CONSUMER_INVOKE_TIMEOUT);
        for (Map.Entry<String, AbstractChannel> entry : clientChannels.entrySet()) {
            try {
                ClientTransport clientTransport = ClientTransportFactory.getReverseClientTransport(entry.getValue());
                clientTransport.oneWaySend(negotiationRequest, timeout);
            } catch (Exception e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Fail to send negotiation to {}, cause by: {}",
                            entry.getKey(), e.getMessage());
                }
            }
        }
    }

    @Override
    public void handleNegotiationRequest(NegotiationRequest request, AbstractChannel channel) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.info("Receive negotiation request: cmd:{}, data:{}", request.getCmd(), request.getData());
        }
        Protocol protocol = ProtocolFactory.getProtocol(request.getProtocolType());
        ProtocolNegotiator negotiator = protocol.negotiator();
        if (negotiator != null) {
            NegotiationResponse response = negotiator.handleRequest(request);
            channel.writeAndFlush(response);
        }
    }

    @Override
    public void registerChannel(AbstractChannel channel) {
        String key = NetUtils.channelToString(channel.localAddress(), channel.remoteAddress());
        clientChannels.put(key, channel);
    }

    @Override
    public void unRegisterChannel(AbstractChannel channel) {
        String key = NetUtils.channelToString(channel.localAddress(), channel.remoteAddress());
        ClientTransportFactory.removeReverseClientTransport(key); // 删除callback等生成的反向长连接
        clientChannels.remove(key);
    }

    @Override
    public void receiveRpcResponse(RpcResponse response, AbstractChannel channel) {
        String channelKey = NetUtils.channelToString(channel.remoteAddress(), channel.localAddress());
        ClientTransport clientTransport = ClientTransportFactory.getReverseClientTransport(channelKey);
        if (clientTransport != null) {
            clientTransport.receiveRpcResponse(response);
        } else {
            LOGGER.error("no such clientTransport for channel:{}", channel);
            throw new BsoaRpcException(22222, "No such clientTransport");
        }
    }

    @Override
    public void receiveNegotiationResponse(NegotiationResponse response, AbstractChannel channel) {
        String channelKey = NetUtils.channelToString(channel.remoteAddress(), channel.localAddress());
        ClientTransport clientTransport = ClientTransportFactory.getReverseClientTransport(channelKey);
        if (clientTransport != null) {
            clientTransport.receiveNegotiationResponse(response);
        } else {
            LOGGER.error("no such clientTransport for channel:{}", channel);
            throw new BsoaRpcException(22222, "No such clientTransport");
        }
    }

    @Override
    public void handleHeartbeatRequest(HeartbeatRequest request, AbstractChannel channel) {
        HeartbeatResponse response = MessageBuilder.buildHeartbeatResponse(request);
        channel.writeAndFlush(response);
    }

    /**
     * Entry size int.
     *
     * @return the int
     */
    public int entrySize() {
        return instanceMap.size();
    }

    /**
     * Gets all own invoker.
     *
     * @return the all own invoker
     */
    public Map<String, Invoker> getAllOwnInvoker() {
        return instanceMap;
    }
}
