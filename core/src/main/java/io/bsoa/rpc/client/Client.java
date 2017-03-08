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
package io.bsoa.rpc.client;

import io.bsoa.rpc.base.Destroyable;
import io.bsoa.rpc.base.Initializable;
import io.bsoa.rpc.bootstrap.ConsumerBootstrap;
import io.bsoa.rpc.ext.Extensible;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;

/**
 * 客户端，封装了集群模式、长连接管理、服务路由、负载均衡等抽象类
 * <p>
 * Created by zhangg on 16-12-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
@Extensible(singleton = false)
@ThreadSafe
public abstract class Client implements Initializable, Destroyable {

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

    /**
     * 是否可用
     *
     * @return
     */
    public abstract boolean isAvailable();

    /**
     * 增加服务端列表 （增量）
     *
     * @param providerInfos 服务端列表
     */
    public abstract void addProvider(List<ProviderInfo> providerInfos);

    /**
     * 删除服务端列表（增量）
     *
     * @param providerInfos 服务端列表
     */
    public abstract void removeProvider(List<ProviderInfo> providerInfos);

    /**
     * 更新服务端列表（全量）
     *
     * @param providerInfos 服务端列表，为空代表清空已有列表
     */
    public abstract void updateProvider(List<ProviderInfo> providerInfos);

    /**
     * 状态变化通知 TODO
     *
     * @param originalState
     */
    public abstract void checkStateChange(boolean originalState);

    /**
     * 发送请求
     *
     * @param rpcRequest Rpc请求
     * @return Rpc响应
     */
    public abstract RpcResponse sendMsg(RpcRequest rpcRequest);
}
