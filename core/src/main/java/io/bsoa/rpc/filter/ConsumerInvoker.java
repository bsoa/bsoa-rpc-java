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
package io.bsoa.rpc.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.base.Invoker;
import io.bsoa.rpc.client.Client;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.server.InvokerHolder;

/**
 * <p>执行真正的调用过程，使用client发送数据给server</p>
 * <p>
 * Created by zhangg on 2016/12/15 23:08. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ConsumerInvoker extends FilterInvoker {

    /**
     * slf4j logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ConsumerInvoker.class);

    /**
     * The Consumer config.
     */
    private ConsumerConfig<?> consumerConfig;

    /**
     * The Client.
     */
    private Client client;

    /**
     * Instantiates a new Consumer invoke filter.
     *
     * @param consumerConfig the consumer config
     * @param client         the client
     */
    public ConsumerInvoker(ConsumerConfig<?> consumerConfig, Client client) {
        super(consumerConfig);
        this.consumerConfig = consumerConfig;
        this.client = client;
    }

    @Override
    public RpcResponse invoke(RpcRequest rpcRequest) {
        // 优先本地调用，本地没有或者已经unexport，调用远程
        if (consumerConfig.isInJVM()) {
            String key = InvokerHolder.buildKey(consumerConfig.getInterfaceId(), consumerConfig.getTags());
            Invoker injvmProviderInvoker = InvokerHolder.getInvoker(key);
            if (injvmProviderInvoker != null) { // 本地有服务事项类
                return injvmProviderInvoker.invoke(rpcRequest);
            }
        }
        // 目前只是通过client发送给服务端
        try {
            return client.sendMsg(rpcRequest);
        } catch (BsoaRpcException e) {
            RpcResponse response = MessageBuilder.buildRpcResponse(rpcRequest);
            response.setException(e);
            return response;
        }
    }

}
