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
package io.bsoa.rpc.server;

import io.bsoa.rpc.message.HeartbeatRequest;
import io.bsoa.rpc.message.NegotiationRequest;
import io.bsoa.rpc.message.NegotiationResponse;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.transport.AbstractChannel;

/**
 * <p></p>
 *
 * Created by zhangg on 2016/12/22 23:03. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public interface ServerHandler {

    public void handleRpcRequest(RpcRequest request, AbstractChannel channel);

    void handleNegotiationRequest(NegotiationRequest request, AbstractChannel channel);

    void registerChannel(AbstractChannel nettyChannel);

    void unRegisterChannel(AbstractChannel nettyChannel);

    void receiveRpcResponse(RpcResponse response, AbstractChannel channel);

    void receiveNegotiationResponse(NegotiationResponse response, AbstractChannel channel);

    void handleHeartbeatRequest(HeartbeatRequest request, AbstractChannel abstractChannel);
}
