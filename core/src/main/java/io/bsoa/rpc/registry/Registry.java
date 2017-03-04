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
package io.bsoa.rpc.registry;

import io.bsoa.rpc.base.Destroyable;
import io.bsoa.rpc.base.Initializable;
import io.bsoa.rpc.client.ProviderInfo;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.config.RegistryConfig;
import io.bsoa.rpc.ext.Extensible;
import io.bsoa.rpc.listener.ConfigListener;
import io.bsoa.rpc.listener.ProviderInfoListener;

import java.util.List;

/**
 * Created by zhangg on 2016/7/16 00:29.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extensible(singleton = false)
public abstract class Registry implements Initializable, Destroyable {

    /**
     * 注册中心服务配置
     */
    protected RegistryConfig registryConfig;

    /**
     * 注册中心配置
     *
     * @param registryConfig 注册中心配置
     */
    protected Registry(RegistryConfig registryConfig) {
        this.registryConfig = registryConfig;
    }

    /**
     * 启动
     *
     * @return
     */
    public abstract boolean start();

    /**
     * 注册服务提供者
     *
     * @param config   Provider配置
     * @param listener 配置监听器
     */
    public abstract void register(ProviderConfig config, ConfigListener listener);

    /**
     * 反注册服务提供者
     *
     * @param config Provider配置
     */
    public abstract void unRegister(ProviderConfig config);

    /**
     * 反注册服务提供者
     *
     * @param configs Provider配置
     */
    public abstract void batchUnRegister(List<ProviderConfig> configs);

    /**
     * 订阅服务列表
     *
     * @param config               Consumer配置
     * @param providerInfoListener 配置监听器
     * @param configListener       配置监听器
     * @return 当前Provider列表 list
     */
    public abstract List<ProviderInfo> subscribe(ConsumerConfig config, ProviderInfoListener providerInfoListener,
                                                 ConfigListener configListener);

    /**
     * 反订阅服务调用者相关配置
     *
     * @param config Consumer配置
     */
    public abstract void unSubscribe(ConsumerConfig config);

    /**
     * 反订阅服务调用者相关配置
     *
     * @param configs Consumer配置
     */
    public abstract void batchUnSubscribe(List<ConsumerConfig> configs);

}
