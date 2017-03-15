/*
 *
 * Copyright (c) 2017 The BSOA Project
 *
 * The BSOA Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */
package io.bsoa.rpc.registry.zk;

import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.config.RegistryConfig;
import io.bsoa.rpc.config.ServerConfig;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.test.TestService;
import io.bsoa.test.TestServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.bsoa.rpc.registry.zk.ZookeeperRegistry.PARAM_CREATE_EPHEMERAL;
import static io.bsoa.rpc.registry.zk.ZookeeperRegistry.PARAM_PREFER_LOCAL_FILE;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/1/2 20:42. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ZookeeperRegistryServer {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ZookeeperRegistryServer.class);

    public static void main(String[] args) {
        ServerConfig serverConfig = new ServerConfig()
                .setPort(12345)
                .setDaemon(false);

        RegistryConfig registryConfig = new RegistryConfig()
                .setProtocol("zookeeper")
                .setAddress("127.0.0.1:2181/test")
                .setParameter(PARAM_PREFER_LOCAL_FILE, "false")
                .setParameter(PARAM_CREATE_EPHEMERAL, "true");

        ProviderConfig<TestService> providerConfig = new ProviderConfig<TestService>()
                .setInterfaceId(TestService.class.getName())
                .setRef(new TestServiceImpl())
                .setServer(serverConfig)
                .setRegistry(registryConfig);

        providerConfig.export();

        LOGGER.warn("started at pid {}", BsoaContext.PID);
    }

}
