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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.config.ServerConfig;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.ExtensionClass;
import io.bsoa.rpc.ext.ExtensionLoader;
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
     * 服务端扩展器
     */
    private final static ExtensionLoader<Server> EXTENSION_LOADER
            = ExtensionLoaderFactory.getExtensionLoader(Server.class);
    /**
     * 全部服务端
     */
    private final static ConcurrentHashMap<String, Server> SERVER_MAP = new ConcurrentHashMap<>();

    /**
     * 初始化Server实例
     *
     * @param serverConfig 服务端配置
     * @return
     */
    public synchronized static Server getServer(ServerConfig serverConfig) {
        try {
            Server server = SERVER_MAP.get(Integer.toString(serverConfig.getPort()));
            if (server == null) {
                ExtensionClass<Server> ext = EXTENSION_LOADER.getExtensionClass(serverConfig.getProtocol());
                if (ext == null) {
                    throw ExceptionUtils.buildRuntime(22222, "server.protocol", serverConfig.getProtocol(),
                            "Unsupported protocol of server!");
                }
                server = ext.getExtInstance();
                server.init(serverConfig);
                SERVER_MAP.putIfAbsent(serverConfig.getPort() + "", server);
            }
            return server;
        } catch (BsoaRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, e.getMessage(), e);
        }
    }

    /**
     * 得到全部服务端
     *
     * @return 全部服务端
     */
    public static List<Server> getServers() {
        return new ArrayList<>(SERVER_MAP.values());
    }

    /**
     * 关闭全部服务端
     */
    public static void destroyAll() {
        if (CommonUtils.isNotEmpty(SERVER_MAP)
                && LOGGER.isInfoEnabled()) {
            LOGGER.info("Destroy all server now!");
        }
        for (Map.Entry<String, Server> entry : SERVER_MAP.entrySet()) {
            String key = entry.getKey();
            Server server = entry.getValue();
            try {
                server.stop();
                SERVER_MAP.remove(key);
            } catch (Exception e) {
                LOGGER.error("Error when destroy server with key:" + key, e);
            }
        }
    }
}
