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

import io.bsoa.rpc.client.ProviderInfo;
import io.bsoa.rpc.message.MessageConstants;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.transport.ClientTransport;
import io.bsoa.rpc.transport.ClientTransportConfig;
import io.bsoa.rpc.transport.ClientTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/18 10:58. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ClientTransportTest {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ClientTransportTest.class);

    public static void main(String[] args) throws InterruptedException {
        ClientTransportConfig config = new ClientTransportConfig();
        ProviderInfo providerInfo = ProviderInfo.getProvider("127.0.0.1", 22222);
        config.setProviderInfo(providerInfo);
        config.setConnectionNum(2);

        ClientTransport transport = ClientTransportFactory.getClientTransport(config);
        try {
            transport.connect();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
        RpcRequest request1 = new RpcRequest();
        request1.setProtocolType((byte) 10) // bsoa
                .setSerializationType((byte) 2) // 3java 2hessian
                .setDirectionType(MessageConstants.DIRECTION_FORWARD);
        request1.setInterfaceName("io.bsoa.test.HelloService")
                .setTags("tag1")
                .setMethodName("sayHello")
                .setArgClasses(new Class[]{String.class, int.class})
                .setArgsType(new String[]{"java.lang.String", "int"})
                .setArgs(new Object[]{"jhahahaha", 123})
                .addAttachment(".token", "xxxx");
        RpcResponse response = (RpcResponse) transport.syncSend(request1, 60000);
        if (response.hasError()) {
            LOGGER.error("", response.getException());
        } else {
            LOGGER.info("{}", response.getReturnData());
        }


//        HeartbeatRequest request3 = new HeartbeatRequest();
//        request3.setTimestamp(System.currentTimeMillis());
//        HeartbeatResponse response4 = (HeartbeatResponse) transport.syncSend(request3, 60000);
//        LOGGER.info("{}", response4.getTimestamp());
//
//
//        NegotiatorRequest request5 = new NegotiatorRequest();
//        request5.setCmd("1");
//        request5.setData(null);
//        NegotiatorResponse response6 = (NegotiatorResponse) transport.syncSend(request5, 60000);
//        LOGGER.info("{}", response6.getRes());


    }

}
