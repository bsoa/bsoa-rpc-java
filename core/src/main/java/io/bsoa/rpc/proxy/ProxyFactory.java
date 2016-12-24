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
package io.bsoa.rpc.proxy;

import io.bsoa.rpc.base.Invoker;
import io.bsoa.rpc.common.utils.ClassUtils;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.ExtensionClass;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;

/**
 *
 *
 * Created by zhanggeng on 16-6-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public final class ProxyFactory {

    /**
     * 构建代理类实例
     *
     * @param proxyType
     *         代理类型
     * @param clazz
     *         原始类
     * @param proxyInvoker
     *         代码执行的Invoker
     * @param <T>
     *         类型
     * @return 代理类实例
     * @throws Exception
     */
    public static <T> T buildProxy(String proxyType, Class<T> clazz, Invoker proxyInvoker) throws Exception {
        try {
            ExtensionClass<Proxy> ext = ExtensionLoaderFactory.getExtensionLoader(Proxy.class)
                    .getExtensionClass(proxyType);
            if (ext == null) {
                throw ExceptionUtils.buildRuntime(22222, "consumer.proxy", proxyType,
                        "Unsupported proxy of client!");
            }
            Class<? extends Proxy> proxyClass = ext.getClazz();
            Proxy proxy = ClassUtils.newInstance(proxyClass);
            return proxy.getProxy(clazz, proxyInvoker);
        } catch (BsoaRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, e.getMessage(), e);
        }
    }
}