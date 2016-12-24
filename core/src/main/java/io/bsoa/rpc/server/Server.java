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
package io.bsoa.rpc.server;

import io.bsoa.rpc.base.Invoker;
import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.config.ServerConfig;

/**
 * Created by zhanggeng on 16-6-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public interface Server {
    /**
     * 启动server端
     *
     * @param serverConfig
     */
    public void init(ServerConfig serverConfig);

    /**
     * 启动
     */
    public void start();

    /**
     * 是否已经启动
     *
     * @return 是否启动
     */
    public boolean isStarted();

    /**
     * 是否还绑定了服务（没有可以销毁）
     *
     * @return
     */
    public boolean hasNoEntry();

    /**
     * 停止
     */
    public void stop();

    /**
     * 注册服务
     *
     * @param providerConfig
     * @param instance
     */
    public void registerProcessor(ProviderConfig providerConfig, Invoker instance);

    /**
     * 取消注册服务
     *
     * @param providerConfig
     * @param closeIfNoEntry
     */
    public void unRegisterProcessor(ProviderConfig providerConfig, boolean closeIfNoEntry);
}
