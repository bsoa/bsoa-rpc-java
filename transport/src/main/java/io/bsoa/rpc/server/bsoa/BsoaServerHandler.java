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

import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.message.StreamRequest;
import io.bsoa.rpc.server.ServerHandler;
import io.bsoa.rpc.transport.AbstractChannel;

/**
 * <p></p>
 *
 * Created by zhangg on 2016/12/22 23:05. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("bsoa")
public class BsoaServerHandler implements ServerHandler {

    public void handleRpcRequest(RpcRequest rpcRequest, AbstractChannel channel) {
        // 丢到业务线程池去执行 TODO
        RpcResponse rpcResponse = MessageBuilder.buildRpcResponse(rpcRequest);
        rpcResponse.setReturnData("hello, this is response!");
        channel.writeAndFlush(rpcResponse);
    }

    @Override
    public void handleStreamRequest(StreamRequest request, AbstractChannel channel) {

    }
}
