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

import io.bsoa.rpc.bootstrap.ConsumerBootstrap;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.ExtensionClass;
import io.bsoa.rpc.ext.ExtensionLoader;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;

/**
 * Created by zhangg on 2016/7/16 01:06.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ClientFactory {

    /**
     * 扩展加载器
     */
    private final static ExtensionLoader<Client> EXTENSION_LOADER
            = ExtensionLoaderFactory.getExtensionLoader(Client.class);

    /**
     * 构造Client对象
     *
     * @param consumerBootstrap 客户端配置
     * @return Client对象
     */
    public static Client getClient(ConsumerBootstrap consumerBootstrap) {
        try {
            ConsumerConfig consumerConfig = consumerBootstrap.getConsumerConfig();
            ExtensionClass<Client> ext = EXTENSION_LOADER.getExtensionClass(consumerConfig.getCluster());
            if (ext == null) {
                throw ExceptionUtils.buildRuntime(22222, "consumer.cluster",
                        consumerConfig.getCluster(), "Unsupported cluster of client!");
            }
            Client client = ext.getExtInstance(new Class[]{ConsumerBootstrap.class},
                    new Object[]{consumerBootstrap});
            return client;
        } catch (BsoaRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, e.getMessage(), e);
        }
    }
}
