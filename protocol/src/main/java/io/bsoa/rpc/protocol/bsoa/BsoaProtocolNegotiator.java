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
package io.bsoa.rpc.protocol.bsoa;

import io.bsoa.rpc.client.ProviderInfo;
import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.BsoaOptions;
import io.bsoa.rpc.common.BsoaVersion;
import io.bsoa.rpc.common.json.JSON;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.MessageConstants;
import io.bsoa.rpc.message.NegotiationRequest;
import io.bsoa.rpc.message.NegotiationResponse;
import io.bsoa.rpc.protocol.ProtocolFactory;
import io.bsoa.rpc.protocol.ProtocolNegotiator;
import io.bsoa.rpc.protocol.bsoa.handler.AppNegotiationHandler;
import io.bsoa.rpc.protocol.bsoa.handler.HeaderCacheNegotiationHandler;
import io.bsoa.rpc.protocol.bsoa.handler.SerialNegotiationHandler;
import io.bsoa.rpc.protocol.bsoa.handler.ServerBusyNegotiationHandler;
import io.bsoa.rpc.protocol.bsoa.handler.ServerClosingNegotiationHandler;
import io.bsoa.rpc.protocol.bsoa.handler.VersionNegotiationHandler;
import io.bsoa.rpc.transport.ChannelContext;
import io.bsoa.rpc.transport.ClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>bsoa协议的协商器</p>
 * <p>
 * Created by zhangg on 2017/2/20 21:44. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("bsoa")
public class BsoaProtocolNegotiator implements ProtocolNegotiator {

    /**
     * slf4j Logger for this class
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(BsoaProtocolNegotiator.class);

    /**
     * 命令列表
     */
    protected ConcurrentHashMap<String, BsoaNegotiationHandler> commands = new ConcurrentHashMap<>();

    private boolean negotiation = BsoaConfigs.getBooleanValue(BsoaOptions.BSOA_NEGOTIATION_ENABLE);
    private boolean headRef = BsoaConfigs.getBooleanValue(BsoaOptions.BSOA_HEAD_REF_ENABLE);

    /**
     * init
     */
    public BsoaProtocolNegotiator() {
        register(new VersionNegotiationHandler());
        register(new AppNegotiationHandler());
        register(new HeaderCacheNegotiationHandler());
        register(new ServerBusyNegotiationHandler());
        register(new ServerClosingNegotiationHandler());
        register(new SerialNegotiationHandler());
    }

    private void register(BsoaNegotiationHandler handler) {
        commands.put(handler.command(), handler);
    }

    @Override
    public boolean handshake(ProviderInfo providerInfo, ClientTransport clientTransport) {
        if (negotiation) {
            NegotiationRequest request = MessageBuilder.buildNegotiationRequest();
            request.setProtocolType(ProtocolFactory.getCodeByAlias(providerInfo.getProtocolType()));
            cacheProviderVersion(clientTransport, request); // 记住真正服务端版本
            sendConsumerAppId(clientTransport, request); // 发送给客户端信息给服务端
            if (headRef) {
                sendInterfaceNameCache(providerInfo, clientTransport, request);
            }
        }
        return true;
    }

    @Override
    public NegotiationResponse handleRequest(NegotiationRequest request, ChannelContext context) {
        NegotiationResponse response = MessageBuilder.buildNegotiationResponse(request);
        String cmd = request.getCmd();
        if (cmd == null) {
            response.setError(true).setData("command is null");
        } else {
            BsoaNegotiationHandler handler = commands.get(cmd);
            if (handler == null) {
                response.setError(true).setData("unsupported command: " + cmd);
            } else {
                try {
                    response.setData(handler.handle(request, context));
                } catch (Exception e) {
                    response.setError(true).setData(e.getMessage());
                }
            }
        }
        return response;
    }

    @Override
    public NegotiationResponse sendNegotiationRequest(ClientTransport clientTransport,
                                                      NegotiationRequest negotiationRequest, int timeout) {
        if (negotiationRequest.getDirectionType() == MessageConstants.DIRECTION_ONEWAY) {
            clientTransport.oneWaySend(negotiationRequest, timeout);
            return null;
        } else {
            return (NegotiationResponse) clientTransport.syncSend(negotiationRequest, timeout);
        }
    }

    protected String send(ClientTransport clientTransport, NegotiationRequest request) {
        NegotiationResponse response = sendNegotiationRequest(clientTransport, request,
                clientTransport.getConfig().getInvokeTimeout());
        if (response.isError()) {
            String errorMsg = response.getData();
            LOGGER.warn("negotiation failed, cause by: {}", response.getData());
            throw new BsoaRuntimeException(22222, errorMsg);
        }
        return response.getData();
    }

    /**
     * 协商双方版本
     *
     * @param clientTransport 客户端长连接
     * @param request         协商请求
     * @see VersionNegotiationHandler
     */
    protected void cacheProviderVersion(ClientTransport clientTransport, NegotiationRequest request) {
        request.setCmd("version");
        Map<String, String> map = new HashMap<>();
        map.put("version", BsoaVersion.BSOA_VERSION + "");
        map.put("build", BsoaVersion.BUILD_VERSION);
        request.setData(JSON.toJSONString(map)); // 把自己版本发给服务端
        String serverVersionJson = send(clientTransport, request);
        Map<String, String> serverMap = JSON.parseObject(serverVersionJson, Map.class);
        if (serverMap != null) {
            String serverVer = serverMap.get("version");
            if (serverVer != null) {
                // 记录到长连接的上下文
                clientTransport.getChannel().context().setDstVersion(Integer.parseInt(serverVer));
            }
        }
    }

    /**
     * 传递应用信息
     *
     * @param clientTransport 客户端长连接
     * @param request         协商请求
     * @see AppNegotiationHandler
     */
    protected void sendConsumerAppId(ClientTransport clientTransport, NegotiationRequest request) {
        request.setDirectionType(MessageConstants.DIRECTION_ONEWAY);
        request.setCmd("app");
        Map<String, String> map = new HashMap<>();
        map.put(BsoaOptions.APP_ID, BsoaConfigs.getStringValue(BsoaOptions.APP_ID));
        map.put(BsoaOptions.APP_NAME, BsoaConfigs.getStringValue(BsoaOptions.APP_NAME));
        map.put(BsoaOptions.INSTANCE_ID, BsoaConfigs.getStringValue(BsoaOptions.INSTANCE_ID));
        request.setData(JSON.toJSONString(map));
        send(clientTransport, request); // 单向
    }

    /**
     * 传递头部缓存信息
     *
     * @param providerInfo    服务端信息
     * @param clientTransport 客户端长连接
     * @param request         协商请求
     * @see HeaderCacheNegotiationHandler
     */
    protected void sendInterfaceNameCache(ProviderInfo providerInfo, ClientTransport clientTransport,
                                          NegotiationRequest request) {
        request.setCmd("headerCache");
        Map<String, String> map = new HashMap<>();
        int cacheId = 0;// clientTransport.getChannel().context().; //申请一个cache TODO
        map.put(providerInfo.getInterfaceId(), cacheId + "");
        request.setData(JSON.toJSONString(map));
        String result = send(clientTransport, request);
        if (CommonUtils.isTrue(result)) {
            LOGGER.info("------------{}", result);
            // 保留下来这个缓存  cacheId  TODO
        }
    }
}
