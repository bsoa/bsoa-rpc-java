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
package io.bsoa.rpc.protocol.jsf.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.config.ServerConfig;
import io.bsoa.rpc.context.BsoaContext;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/1/2 20:42. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class JSFServerTest {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(JSFServerTest.class);

    public static void main(String[] args) {
        ServerConfig serverConfig = new ServerConfig();
//        serverConfig.setHost("0.0.0.0");
//        serverConfig.setPort(22222);
//        serverConfig.setProtocol("jsf");
        serverConfig.setDaemon(false);
        //serverConfig.start();

        ProviderConfig<HelloService> providerConfig = new ProviderConfig<>();
        providerConfig.setInterfaceId(HelloService.class.getName());
        providerConfig.setRef(new HelloServiceImpl());
        providerConfig.setServer(serverConfig);
        providerConfig.setRegister(false);
        providerConfig.export();

        LOGGER.warn("started at pid {}", BsoaContext.PID);
    }

}
