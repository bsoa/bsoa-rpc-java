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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.concurrent.ThreadSafe;

import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.ext.Extensible;
import io.bsoa.rpc.transport.ClientTransport;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/2/3 16:19. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extensible(singleton = false)
@ThreadSafe
public interface ConnectionHolder {

    /**
     * 初始化
     */
    public void init(ConsumerConfig consumerConfig);

    /**
     * 增加多个服务提供者
     *
     * @param providers 多个服务提供者
     */
    public void addProvider(List<Provider> providers);

    /**
     * 刪除多个服务提供者
     *
     * @param providers 多个服务提供者
     */
    public void removeProvider(List<Provider> providers);

    /**
     * 覆盖多个服务提供者
     *
     * @param providers 多个服务提供者
     */
    public void updateProviders(List<Provider> providers);

    /**
     * 清空服务提供者
     *
     * @return 多个服务提供者
     */
    public Map<Provider, ClientTransport> clearProviders();

    /**
     * 存活的连接
     *
     * @return the alive connections
     */
    public ConcurrentHashMap<Provider, ClientTransport> getAvailableConnections();

    /**
     * 存活的全部provider
     *
     * @return all alive providers
     */
    public List<Provider> getAvailableProviders();

    /**
     * 根据provider查找存活的ClientTransport
     *
     * @param provider the provider
     * @return the client transport
     */
    public ClientTransport getAvailableClientTransport(Provider provider);

    /**
     * 是否没有存活的的provider
     *
     * @return all alive providers
     */
    public boolean isAvailableEmpty();

    /**
     * 获取当前的Provider列表（包括连上和没连上的）
     *
     * @return 当前的Provider列表 set
     */
    public Collection<Provider> currentProviderList();

    /**
     * 设置为不可用
     *
     * @param provider  Provider
     * @param transport 连接
     */
    public void setUnavailable(Provider provider, ClientTransport transport);

    /**
     * 销毁之前做的事情
     */
    public void preDestroy();

    /**
     * 销毁
     */
    public void destroy();

}
