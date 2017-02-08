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
package io.bsoa.rpc.bootstrap;

import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.ext.ExtensionLoader;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;

import static io.bsoa.rpc.common.BsoaConfigs.DEFAULT_PROVIDER_BOOTSTRAP;

/**
 * <p></p>
 *
 * Created by zhangg on 2017/2/8 19:46. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class Bootstrap {

    private final static ExtensionLoader<ProviderBootstrap> PROVIDER_BOOTSTRAP_EXTENSION_LOADER
            = ExtensionLoaderFactory.getExtensionLoader(ProviderBootstrap.class);

    private final static ExtensionLoader<ConsumerBootstrap> CONSUMER_BOOTSTRAP_EXTENSION_LOADER
            = ExtensionLoaderFactory.getExtensionLoader(ConsumerBootstrap.class);

    public static void provide(ProviderConfig providerConfig){
        PROVIDER_BOOTSTRAP_EXTENSION_LOADER.getExtension(BsoaConfigs.getStringValue(DEFAULT_PROVIDER_BOOTSTRAP));
    }
}
