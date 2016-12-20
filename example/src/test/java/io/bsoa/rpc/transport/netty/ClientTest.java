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

import io.bsoa.rpc.client.Provider;
import io.bsoa.rpc.message.NegotiatorRequest;
import io.bsoa.rpc.transport.ClientTransport;
import io.bsoa.rpc.transport.ClientTransportConfig;
import io.bsoa.rpc.transport.ClientTransportFactory;

/**
 * <p></p>
 *
 * Created by zhangg on 2016/12/18 10:58. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ClientTest {

    public static void main(String[] args) throws InterruptedException {
        ClientTransportConfig config = new ClientTransportConfig();
        Provider provider = Provider.getProvider("127.0.0.1", 22222);
        config.setProvider(provider);
        config.setConnectionNum(2);

        ClientTransport transport = ClientTransportFactory.getClientTransport(config);
        try {
            transport.connect();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }


        NegotiatorRequest request = new NegotiatorRequest();
        request.setRequestId(2345);
        transport.syncSend(request, 5000);

        Thread.sleep(50000);
    }

}
