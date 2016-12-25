/*
 * *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  * <p>
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  * <p>
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package io.bsoa.rpc.transport.netty;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.NegotiatorResponse;
import io.bsoa.rpc.transport.ServerTransport;
import io.bsoa.rpc.transport.ServerTransportConfig;
import io.bsoa.rpc.transport.ServerTransportFactory;

/**
 * Created by zhangg on 2016/12/18.
 */
public class ServerTest {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ServerTest.class);

    public void testStartServer() {
        ServerTransportConfig config = new ServerTransportConfig();
        config.setPort(22222);
        config.setDaemon(false);
        config.setNegotiatorListener(negotiatorRequest -> {
            LOGGER.info(negotiatorRequest.getCmd());
            LOGGER.info(negotiatorRequest.getCmd());

            NegotiatorResponse response = MessageBuilder.buildNegotiatorResponse(negotiatorRequest);
            response.setRes("nego response from server");
            return response;
        });
        ServerTransport transport = ServerTransportFactory.getServerTransport(config);
        Assert.assertEquals(transport.getClass(), NettyServerTransport.class);
        transport.start();
    }

    public static void main(String[] args) {
        ServerTest test = new ServerTest();
        test.testStartServer();
        LOGGER.warn("Server started");
    }
}