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

import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.ext.ExtensionLoader;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;

import static io.bsoa.rpc.common.BsoaConfigs.getStringValue;
import static io.bsoa.rpc.common.BsoaOptions.DEFAULT_CONSUMER_BOOTSTRAP;
import static io.bsoa.rpc.common.BsoaOptions.DEFAULT_PROVIDER_BOOTSTRAP;

/**
 * <p>辅助工具类，更方便的使用发布方法</p>
 * <p>
 * Created by zhangg on 2017/2/8 19:46. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class Bootstraps {

    /**
     * ServiceExporter扩展加载器
     */
    private final static ExtensionLoader<ProviderBootstrap> PROVIDER_BOOTSTRAP_EXTENSION_LOADER
            = ExtensionLoaderFactory.getExtensionLoader(ProviderBootstrap.class);
    /**
     * ServiceReferencer扩展加载器
     */
    private final static ExtensionLoader<ConsumerBootstrap> CONSUMER_BOOTSTRAP_EXTENSION_LOADER
            = ExtensionLoaderFactory.getExtensionLoader(ConsumerBootstrap.class);

    /**
     * 发布一个服务
     *
     * @param providerConfig 服务发布者配置
     * @param <T>            接口类型
     * @return 发布启动类
     */
    public static <T> ProviderBootstrap<T> from(ProviderConfig<T> providerConfig) {
        ProviderBootstrap bootstrap = PROVIDER_BOOTSTRAP_EXTENSION_LOADER.getExtension(
                getStringValue(DEFAULT_PROVIDER_BOOTSTRAP),
                new Class[]{ProviderConfig.class},
                new Object[]{providerConfig});
        return (ProviderBootstrap<T>) bootstrap;
    }

    /**
     * 引用一个服务
     *
     * @param consumerConfig 服务消费者配置
     * @param <T>            接口类型
     * @return 引用启动类
     */
    public static <T> ConsumerBootstrap<T> from(ConsumerConfig<T> consumerConfig) {
        ConsumerBootstrap bootstrap = CONSUMER_BOOTSTRAP_EXTENSION_LOADER.getExtension(
                getStringValue(DEFAULT_CONSUMER_BOOTSTRAP),
                new Class[]{ConsumerConfig.class},
                new Object[]{consumerConfig});
        return (ConsumerBootstrap<T>) bootstrap;
    }
}
