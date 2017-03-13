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
package io.bsoa.rpc.transport.netty;

import io.bsoa.rpc.server.bsoa.BsoaServerHandler;
import io.bsoa.rpc.transport.ServerTransport;
import io.bsoa.rpc.transport.ServerTransportConfig;
import io.bsoa.rpc.transport.ServerTransportFactory;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhangg on 2016/12/18.
 */
public class ServerTransportTest {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ServerTransportTest.class);

    public void testStartServer() {
        ServerTransportConfig config = new ServerTransportConfig();
        config.setPort(22222);
        config.setDaemon(false);
        config.setServerHandler(new BsoaServerHandler(config));
        ServerTransport transport = ServerTransportFactory.getServerTransport(config);
        Assert.assertEquals(transport.getClass(), NettyServerTransport.class);
        transport.start();
    }

    public static void main(String[] args) {
        ServerTransportTest test = new ServerTransportTest();
        test.testStartServer();
        LOGGER.warn("Server started");
    }
}