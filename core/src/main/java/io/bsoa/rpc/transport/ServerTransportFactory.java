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
package io.bsoa.rpc.transport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.ext.ExtensionLoader;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;

/**
 * <p>服务端通讯层工厂类</p>
 * <p>
 * Created by zhangg on 2016/12/17 22:17. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ServerTransportFactory {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ServerTransportFactory.class);

    /**
     * 接口扩张器
     */
    private final static ExtensionLoader<ServerTransport> extensionLoader
            = ExtensionLoaderFactory.getExtensionLoader(ServerTransport.class);

    /**
     * 保留了 端口 和 服务通讯层
     */
    public final static Map<String, ServerTransport> SERVER_TRANSPORT_MAP = new ConcurrentHashMap<>();

    /*
     *
     */
    public static ServerTransport getServerTransport(ServerTransportConfig serverConfig) {
        ServerTransport serverTransport = extensionLoader.getExtension(
                serverConfig.getContainer(),
                new Class[]{ServerTransportConfig.class},
                new Object[]{serverConfig});
        if (serverTransport != null) {
            String key = Integer.toString(serverConfig.getPort());
            SERVER_TRANSPORT_MAP.put(key, serverTransport);
        }
        return serverTransport;
    }

//    public ServerTransport getServerTransportByKey(String key) {
//        return SERVER_TRANSPORT_MAP.get(key);
//    }
}
