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
package io.bsoa.rpc.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.ext.ExtensionClass;
import io.bsoa.rpc.ext.ExtensionLoader;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/1/1 17:17. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Deprecated
public final class ServerHandlerFactory {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ServerHandlerFactory.class);

    /**
     * 服务端扩展器
     */
    private final static ExtensionLoader<ServerHandler> EXTENSION_LOADER
            = ExtensionLoaderFactory.getExtensionLoader(ServerHandler.class);

    /**
     * 初始化Server实例
     *
     * @param server handler名字
     * @return
     */
    public static ServerHandler getServerHaler(String server) {
        ExtensionClass<ServerHandler> ext = EXTENSION_LOADER.getExtensionClass(server);
        if (ext == null) {
            throw ExceptionUtils.buildRuntime(22222, "server.handler", server,
                    "Unsupported handler of server!");
        }
        return ext.getExtInstance();
    }
}
