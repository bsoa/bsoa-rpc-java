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
package io.bsoa.rpc.client;

import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.ExtensionClass;
import io.bsoa.rpc.ext.ExtensionLoader;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/1/4 23:53. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class RouterFactory {

    /**
     * 扩展加载器
     */
    private static ExtensionLoader<Router> EXTENSION_LOADER
            = ExtensionLoaderFactory.getExtensionLoader(Router.class);

    public static Router getRouters(String alias) {
        try {
            ExtensionClass<Router> ext = EXTENSION_LOADER.getExtensionClass(alias);
            if (ext == null) {
                throw ExceptionUtils.buildRuntime(22222, "consumer.router", alias,
                        "Unsupported router of client!");
            }
            return ext.getExtInstance();
        } catch (BsoaRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, e.getMessage(), e);
        }
    }
}
