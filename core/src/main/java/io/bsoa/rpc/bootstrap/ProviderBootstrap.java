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
package io.bsoa.rpc.bootstrap;

import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.ext.Extensible;

import java.util.List;

/**
 * <p>发布服务的包装类，包括具体的启动后的对象</p>
 * <p>
 * Created by zhangg on 2017/2/8 22:54. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extensible(singleton = false)
public abstract class ProviderBootstrap<T> {

    /**
     * 服务发布者配置
     */
    protected final ProviderConfig<T> providerConfig;

    /**
     * 构造函数
     *
     * @param providerConfig 服务发布者配置
     */
    protected ProviderBootstrap(ProviderConfig<T> providerConfig) {
        this.providerConfig = providerConfig;
    }

    /**
     * 得到服务发布者配置
     *
     * @return 服务发布者配置
     */
    public ProviderConfig<T> getProviderConfig() {
        return providerConfig;
    }

    /**
     * 发布一个服务
     */
    public abstract void export();

    /**
     * 发布一个服务
     */
    public abstract void unExport();

    /**
     * 发布服务后的地址
     *
     * @return 服务后的地址
     */
    public abstract List<String> buildUrls();
}
