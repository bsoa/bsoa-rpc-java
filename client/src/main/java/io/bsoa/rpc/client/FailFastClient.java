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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.bootstrap.ConsumerBootstrap;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.transport.ClientTransport;

/**
 * 快速失败
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("failfast")
public class FailFastClient extends AbstractClient {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(FailFastClient.class);

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务端消费者启动器
     */
    public FailFastClient(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
    }

    @Override
    public RpcResponse doSendMsg(RpcRequest request) {
        ClientTransport connection = super.select(request);
        try {
            RpcResponse result = super.sendMsg0(connection, request);
            if (result != null) {
                return result;
            } else {
                throw new BsoaRpcException(22222, "Failed to call " + request.getInterfaceName() + "." + request.getMethodName()
                        + " on remote server " + connection.getConfig().getProviderInfo() + ", return null");
            }
        } catch (Exception e) {
            throw new BsoaRpcException(22222, "[JSF-22103]Failed to call " + request.getInterfaceName() + "." + request.getMethodName()
                    + " on remote server: " + connection.getConfig().getProviderInfo() + ", cause by: "
                    + e.getClass().getName() + ", message is: " + e.getMessage(), e);
        }
    }
}
