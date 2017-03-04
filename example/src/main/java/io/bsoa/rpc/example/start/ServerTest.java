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
package io.bsoa.rpc.example.start;

import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.config.ServerConfig;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.test.EchoService;
import io.bsoa.test.EchoServiceImpl;
import io.bsoa.test.HelloService;
import io.bsoa.test.HelloServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/1/2 20:42. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ServerTest {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ServerTest.class);

    public static void main(String[] args) {
        ServerConfig serverConfig = new ServerConfig()
//        .setHost("0.0.0.0")
//        .setPort(22222)
                .setDaemon(false);

        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
                .setInterfaceId(HelloService.class.getName())
                .setRef(new HelloServiceImpl())
                .setServer(serverConfig)
                .setRegister(false);

        ProviderConfig<EchoService> providerConfig2 = new ProviderConfig<EchoService>()
                .setInterfaceId(EchoService.class.getName())
                .setRef(new EchoServiceImpl())
                .setServer(serverConfig)
                .setRegister(false);

        providerConfig.export();
        providerConfig2.export();

        LOGGER.warn("started at pid {}", BsoaContext.PID);
    }

}
