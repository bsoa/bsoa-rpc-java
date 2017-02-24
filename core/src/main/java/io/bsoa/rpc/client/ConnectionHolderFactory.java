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

import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.ExtensionClass;
import io.bsoa.rpc.ext.ExtensionLoader;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;

/**
 * Created by zhangg on 2017/2/3 16:19. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ConnectionHolderFactory {

    /**
     * 扩展加载器
     */
    private static ExtensionLoader<ConnectionHolder> EXTENSION_LOADER
            = ExtensionLoaderFactory.getExtensionLoader(ConnectionHolder.class);

    /**
     * 根据配置得到连接管理器
     *
     * @param consumerConfig 服务消费者配置
     * @return ConnectionHolder
     */
    public static ConnectionHolder getConnectionHolder(ConsumerConfig consumerConfig) {
        try {
            String connectionHolder = consumerConfig.getConnectionHolder();
            ExtensionClass<ConnectionHolder> ext = EXTENSION_LOADER.getExtensionClass(connectionHolder);
            if (ext == null) {
                throw ExceptionUtils.buildRuntime(22222, "consumer.connectionHolder", connectionHolder,
                        "Unsupported connectionHolder of client!");
            }
            return ext.getExtInstance(new Class[]{ConsumerConfig.class}, new Object[]{consumerConfig});
        } catch (BsoaRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, e.getMessage(), e);
        }
    }
}
