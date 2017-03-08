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
package io.bsoa.rpc.protocol;

import io.bsoa.rpc.client.ProviderInfo;
import io.bsoa.rpc.ext.Extensible;
import io.bsoa.rpc.message.NegotiationRequest;
import io.bsoa.rpc.message.NegotiationResponse;
import io.bsoa.rpc.transport.ChannelContext;
import io.bsoa.rpc.transport.ClientTransport;

/**
 * <p>协议谈判</p>
 * <p>
 * Created by zhangg on 2017/2/20 20:56. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extensible
public interface ProtocolNegotiator {

    /**
     * 握手操作
     *
     * @param providerInfo    服务提供者信息
     * @param clientTransport 和服务提供者的长连接
     * @return 握手言和
     */
    public boolean handshake(ProviderInfo providerInfo, ClientTransport clientTransport);

    /**
     * 处理协商请求（服务端和客户端都可以互发）
     *
     * @param negotiationRequest 协商请求
     * @return 协商响应
     */
    NegotiationResponse handleRequest(NegotiationRequest negotiationRequest, ChannelContext context);

    /**
     * 发送协议请求
     *
     * @param transport          长连接
     * @param negotiationRequest 协商请求
     * @param timeout            超时时间
     * @return 协商响应（如果是单向协商则返回null）
     */
    NegotiationResponse sendNegotiationRequest(ClientTransport transport,
                                               NegotiationRequest negotiationRequest, int timeout);
}
