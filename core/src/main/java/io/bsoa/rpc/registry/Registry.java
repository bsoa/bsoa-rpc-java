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
package io.bsoa.rpc.registry;

import java.util.List;

import io.bsoa.rpc.client.ProviderInfo;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.config.RegistryConfig;
import io.bsoa.rpc.ext.Extensible;
import io.bsoa.rpc.listener.ConfigListener;
import io.bsoa.rpc.listener.ProviderInfoListener;

/**
 * Created by zhangg on 2016/7/16 00:29.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extensible(singleton = false)
public interface Registry {

    /**
     * 初始化
     */
    public void init(RegistryConfig registryConfig);

    /**
     * 启动
     *
     * @return
     */
    public boolean start();

    /**
     * 注册服务提供者
     *
     * @param config   Provider配置
     * @param listener 配置监听器
     */
    public void register(ProviderConfig config, ConfigListener listener);

    /**
     * 反注册服务提供者
     *
     * @param config Provider配置
     */
    public void unRegister(ProviderConfig config);

    /**
     * 反注册服务提供者
     *
     * @param configs Provider配置
     */
    public void batchUnRegister(List<ProviderConfig> configs);

    /**
     * 订阅服务列表
     *
     * @param config           Consumer配置
     * @param providerInfoListener 配置监听器
     * @param configListener   配置监听器
     * @return 当前Provider列表 list
     */
    public List<ProviderInfo> subscribe(ConsumerConfig config, ProviderInfoListener providerInfoListener,
                                        ConfigListener configListener);

    /**
     * 反订阅服务调用者相关配置
     *
     * @param config Consumer配置
     */
    public void unSubscribe(ConsumerConfig config);

    /**
     * 反订阅服务调用者相关配置
     *
     * @param configs Consumer配置
     */
    public void batchUnSubscribe(List<ConsumerConfig> configs);

    /**
     * Destroy void.
     */
    public void destroy();
}
