/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bsoa.rpc.protocol.bsoa;

import java.util.HashMap;
import java.util.Map;

import io.bsoa.rpc.client.ProviderInfo;
import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.BsoaOptions;
import io.bsoa.rpc.common.BsoaVersion;
import io.bsoa.rpc.common.json.JSON;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.listener.NegotiationListener;
import io.bsoa.rpc.message.NegotiationRequest;
import io.bsoa.rpc.message.NegotiationResponse;
import io.bsoa.rpc.protocol.ProtocolNegotiator;
import io.bsoa.rpc.transport.ClientTransport;

/**
 * <p>bsoa协议的协商器</p>
 * <p>
 * Created by zhangg on 2017/2/20 21:44. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("bsoa")
public class BsoaProtocolNegotiator implements ProtocolNegotiator{
    @Override
    public boolean handshake(ProviderInfo providerInfo, ClientTransport clientTransport) {
        cacheProviderVersion(clientTransport); // 记住真正服务端版本
        sendConsumerAppId(clientTransport); // 发送给客户端信息给服务端
        sendInterfaceNameCache(providerInfo, clientTransport);
        return false;
    }

    protected void cacheProviderVersion(ClientTransport clientTransport) {
        NegotiationRequest request = new NegotiationRequest();
        request.setCmd("version");
        Map<String, String> map = new HashMap<>();
        map.put("version", BsoaVersion.BSOA_VERSION + "");
        map.put("build", BsoaVersion.BUILD_VERSION);
        request.setData(JSON.toJSONString(map)); // 把自己版本发给服务端
        NegotiationResponse response = (NegotiationResponse) clientTransport
                .syncSend(request, clientTransport.getConfig().getInvokeTimeout());
        String supportedVersion = response.getRes(); // TODO 记住长连接对于的服务端版本
    }

    protected void sendConsumerAppId(ClientTransport clientTransport) {
        NegotiationRequest request = new NegotiationRequest();
        request.setCmd("app");
        Map<String, String> map = new HashMap<>();
        map.put("appId", BsoaConfigs.getStringValue(BsoaOptions.APP_ID));
        map.put("appName", BsoaConfigs.getStringValue(BsoaOptions.APP_NAME));
        map.put("instanceId", BsoaConfigs.getStringValue(BsoaOptions.INSTANCE_ID));
        request.setData(JSON.toJSONString(map));
        NegotiationResponse response = (NegotiationResponse) clientTransport
                .syncSend(request, clientTransport.getConfig().getInvokeTimeout());
        String supportedVersion = response.getRes(); // TODO 记住长连接对于的服务端版本
    }

    protected void sendInterfaceNameCache(ProviderInfo providerInfo, ClientTransport clientTransport) {
        NegotiationRequest request = new NegotiationRequest();
        request.setCmd("cache");
        Map<String, String> map = new HashMap<>();
        int cacheId = 0; //申请一个cache TODO
        map.put(providerInfo.getInterfaceId(), cacheId + "");
        request.setData(JSON.toJSONString(map));
        NegotiationResponse response = (NegotiationResponse) clientTransport
                .syncSend(request, clientTransport.getConfig().getInvokeTimeout());
        String result = response.getRes();
        if (CommonUtils.isTrue(result)) {
            // 保留下来这个缓存  cacheId  TODO
        }
    }

    @Override
    public NegotiationListener getListener() {
        return null;
    }
}
