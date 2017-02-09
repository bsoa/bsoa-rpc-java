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
package io.bsoa.rpc.client;

import java.util.List;
import javax.annotation.concurrent.ThreadSafe;

import io.bsoa.rpc.bootstrap.ConsumerBootstrap;
import io.bsoa.rpc.ext.Extensible;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * Created by zhangg on 16-6-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
@Extensible(singleton = false)
@ThreadSafe
public abstract class Client {

    /**
     * 服务端消费者启动器
     */
    protected final ConsumerBootstrap consumerBootstrap;

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务端消费者启动器
     */
    public Client(ConsumerBootstrap consumerBootstrap) {
        this.consumerBootstrap = consumerBootstrap;
    }

    public abstract void init();

    public abstract void destroy();

    public abstract boolean isAvailable();

    public abstract void addProvider(List<Provider> providers);

    public abstract void checkStateChange(boolean originalState);

    public abstract void removeProvider(List<Provider> providers);

    public abstract void updateProvider(List<Provider> newProviders);

    public abstract RpcResponse sendMsg(RpcRequest rpcRequest);
}
