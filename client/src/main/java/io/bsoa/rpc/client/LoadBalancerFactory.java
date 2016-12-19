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

import io.bsoa.rpc.common.utils.ClassLoaderUtils;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.ext.ExtensibleClass;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;

/**
 *
 *
 * Created by zhangg on 2016/7/17 15:22.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class LoadBalancerFactory {


    public static LoadBalancer getLoadBalancer(String lb) {
        try {
            LoadBalancer loadBalancer = null;
            ExtensibleClass<LoadBalancer> ext = ExtensionLoaderFactory.getExtensionLoader(LoadBalancer.class)
                    .getExtensibleClass(lb);
            if (ext == null) {
                throw ExceptionUtils.buildRuntime(22222, "consumer.loadbalance", lb,
                        "Unsupported loadbalance of server!");
            }
            Class<? extends LoadBalancer> clientClass = ext.getClazz();
            loadBalancer = ClassLoaderUtils.newInstance(clientClass);
            return loadBalancer;
        } catch (BsoaRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, e.getMessage(), e);
        }
    }
}
