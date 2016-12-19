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

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.utils.ClassLoaderUtils;
import io.bsoa.rpc.config.ServerConfig;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.ext.ExtensionClass;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;

/**
 * Created by zhangg on 2016/7/15 23:31.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public final class ServerFactory {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ServerFactory.class);

    /**
     * 全部服务端
     */
    private final static ConcurrentHashMap<String, Server> SERVERMAP = new ConcurrentHashMap<String, Server>();

    public synchronized static Server getServer(ServerConfig serverConfig) {
        try {
            Server server = SERVERMAP.get(Integer.toString(serverConfig.getPort()));
            if (server == null) {
                ExtensionClass<Server> ext = ExtensionLoaderFactory.getExtensionLoader(Server.class)
                        .getExtensionClass(serverConfig.getProtocol());
                if (ext == null) {
                    throw ExceptionUtils.buildRuntime(22222, "server.protocol", serverConfig.getProtocol(),
                            "Unsupported protocol of server!");
                }
                Class<? extends Server> serverClazz = ext.getClazz();
                server = ClassLoaderUtils.newInstance(serverClazz);
                server.init(serverConfig);
                SERVERMAP.putIfAbsent(serverConfig.getPort()+"", server);
                server.start();
            }
            return server;
        } catch (BsoaRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, e.getMessage(), e);
        }
    }
}
