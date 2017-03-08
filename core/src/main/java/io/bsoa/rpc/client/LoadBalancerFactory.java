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

import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.ExtensionClass;
import io.bsoa.rpc.ext.ExtensionLoader;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;

/**
 * Created by zhangg on 2016/7/17 15:22.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class LoadBalancerFactory {

    /**
     * 扩展加载器
     */
    private static ExtensionLoader<LoadBalancer> EXTENSION_LOADER
            = ExtensionLoaderFactory.getExtensionLoader(LoadBalancer.class);

    /**
     * 根据名字的到负载均衡器
     *
     * @param consumerConfig 客户端配置
     * @return LoadBalancer
     */
    public static LoadBalancer getLoadBalancer(ConsumerConfig consumerConfig) {
        try {
            ExtensionClass<LoadBalancer> ext = EXTENSION_LOADER.getExtensionClass(consumerConfig.getLoadBalancer());
            if (ext == null) {
                throw ExceptionUtils.buildRuntime(22222, "consumer.loadBalancer",
                        consumerConfig.getLoadBalancer(), "Unsupported loadBalancer of client!");
            }
            return ext.getExtInstance(new Class[]{ConsumerConfig.class}, new Object[]{consumerConfig});
        } catch (BsoaRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, e.getMessage(), e);
        }
    }
}
