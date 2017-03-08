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
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.ext.Extensible;
import io.bsoa.rpc.transport.ClientTransport;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/2/3 16:19. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extensible(singleton = false)
@ThreadSafe
public abstract class ConnectionHolder implements Initializable, Destroyable {

    /**
     * 服务消费者配置
     */
    protected ConsumerConfig consumerConfig;

    /**
     * 构造函数
     *
     * @param consumerConfig 服务消费者配置
     */
    protected ConnectionHolder(ConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    /**
     * 增加多个服务提供者
     *
     * @param providerInfos 多个服务提供者
     */
    public abstract void addProvider(List<ProviderInfo> providerInfos);

    /**
     * 刪除多个服务提供者
     *
     * @param providerInfos 多个服务提供者
     */
    public abstract void removeProvider(List<ProviderInfo> providerInfos);

    /**
     * 存活的连接
     *
     * @return the alive connections
     */
    public abstract ConcurrentHashMap<ProviderInfo, ClientTransport> getAvailableConnections();

    /**
     * 存活的全部provider
     *
     * @return all alive providers
     */
    public abstract List<ProviderInfo> getAvailableProviders();

    /**
     * 根据provider查找存活的ClientTransport
     *
     * @param providerInfo the provider
     * @return the client transport
     */
    public abstract ClientTransport getAvailableClientTransport(ProviderInfo providerInfo);

    /**
     * 是否没有存活的的provider
     *
     * @return all alive providers
     */
    public abstract boolean isAvailableEmpty();

    /**
     * 获取当前的Provider列表（包括连上和没连上的）
     *
     * @return 当前的Provider列表 set
     */
    public abstract Collection<ProviderInfo> currentProviderList();

    /**
     * 设置为不可用
     *
     * @param providerInfo Provider
     * @param transport    连接
     */
    public abstract void setUnavailable(ProviderInfo providerInfo, ClientTransport transport);
}
