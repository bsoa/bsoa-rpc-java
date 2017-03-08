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
package io.bsoa.rpc.proxy;

import io.bsoa.rpc.base.Invoker;
import io.bsoa.rpc.common.utils.ClassLoaderUtils;
import io.bsoa.rpc.ext.Extension;

import java.lang.reflect.InvocationHandler;

/**
 * Created by zhangg on 16-6-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
@Extension("jdk")
public class JDKProxy implements Proxy {

    @Override
    public <T> T getProxy(Class<T> interfaceClass, Invoker proxyInvoker) {
        InvocationHandler handler = new JDKInvocationHandler(proxyInvoker);
        ClassLoader classLoader = ClassLoaderUtils.getCurrentClassLoader();
        T result = (T) java.lang.reflect.Proxy.newProxyInstance(classLoader,
                new Class[]{interfaceClass}, handler);
        return result;
    }
}
