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
package io.bsoa.rpc.example.start;

import io.bsoa.test.EchoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.test.HelloService;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/18 10:58. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ClientTest {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ClientTest.class);

    public static void main(String[] args) throws InterruptedException {

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
                .setInterfaceId(HelloService.class.getName())
                .setUrl("bsoa://127.0.0.1:22000")
                .setRegister(false);
        HelloService helloService = consumerConfig.refer();

        ConsumerConfig<EchoService> consumerConfig2 = new ConsumerConfig<EchoService>()
                .setInterfaceId(EchoService.class.getName())
                .setUrl("bsoa://127.0.0.1:22000")
                .setRegister(false);
        EchoService echoService = consumerConfig2.refer();

//        try {
//            for (int i = 0; i < 100; i++) {
//                String s = helloService.sayHello("xxx", 22);
//                LOGGER.warn("{}", s);
//                try {
//                    Thread.sleep(2000);
//                } catch (Exception e) {
//                    // TODO
//                }
//            }
//
//        } catch (Exception e) {
//            LOGGER.error("", e);
//        }

        synchronized (ClientTest.class){
            while (true){
                ClientTest.class.wait();
            }
        }

//        consumerConfig.unRefer();
//
//        ConsumerConfig<HelloService> consumerConfig2 = new ConsumerConfig<HelloService>()
//                .setInterfaceId(HelloService.class.getName())
//                .setUrl("bsoa://127.0.0.1:22000")
//                .setRegister(false);
//        HelloService helloService2 = consumerConfig2.refer();
//        try {
//            String s = helloService2.sayHello("xxx", 22);
//            LOGGER.warn("{}", s);
//        } catch (Exception e) {
//            LOGGER.error("", e);
//        }
//
//
//
//        consumerConfig2.unRefer();
    }

}
