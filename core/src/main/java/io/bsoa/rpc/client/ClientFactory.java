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

import io.bsoa.rpc.common.utils.ClassUtils;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.ExtensionClass;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;

/**
 * Created by zhangg on 2016/7/16 01:06.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ClientFactory {


    /**
     * 构造Client对象
     *
     * @param consumerConfig 客户端配置
     * @return Client对象
     */
    public static Client getClient(ConsumerConfig consumerConfig) {
        try {
            Client client = null;
            ExtensionClass<Client> ext = ExtensionLoaderFactory.getExtensionLoader(Client.class)
                    .getExtensionClass(consumerConfig.getCluster());
            if (ext == null) {
                throw ExceptionUtils.buildRuntime(22222, "consumer.cluster", consumerConfig.getCluster(),
                        "Unsupported protocol of server!");
            }
            Class<? extends Client> clientClass = ext.getClazz();
            client = ClassUtils.newInstance(clientClass);
            client.init(consumerConfig);
            return client;
        } catch (BsoaRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, e.getMessage(), e);
        }
    }
}
