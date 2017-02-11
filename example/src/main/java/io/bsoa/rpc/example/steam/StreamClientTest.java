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
package io.bsoa.rpc.example.steam;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.invoke.StreamObserver;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/18 10:58. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class StreamClientTest {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(StreamClientTest.class);

    public static void main(String[] args) throws InterruptedException {

        ConsumerConfig<StreamHelloService> consumerConfig = new ConsumerConfig<StreamHelloService>()
                .setInterfaceId(StreamHelloService.class.getName())
                .setUrl("bsoa://127.0.0.1:22000")
                .setRegister(false);
        StreamHelloService helloService = consumerConfig.refer();

        CountDownLatch latch = new CountDownLatch(1);

//        try {
//            String result = helloService.download("/User/zhanggeng/xxx", new StreamObserver<String>() {
//                @Override
//                public void onValue(String value) {
//                    LOGGER.info("Client receive data: {}", value);
//                }
//
//                @Override
//                public void onCompleted() {
//                    LOGGER.info("Client download over..");
//                    latch.countDown();
//                }
//
//                @Override
//                public void onError(Throwable t) {
//                    LOGGER.info("Client download exception", t);
//                    latch.countDown();
//                }
//            });
//            LOGGER.info(result);
//        } catch (Exception e) {
//            LOGGER.error("", e);
//            latch.countDown();
//        }

        try {
            StreamObserver<String> observer = helloService.upload("/User/zhanggeng/xxx");
            observer.onValue("aaaaaaaaaaaaaaaa");
            observer.onValue("bbbbbbbbbbbbbbbbbbbbbb");
            observer.onValue("cccc");
            observer.onCompleted();
        } catch (Exception e) {
            LOGGER.error("", e);
            latch.countDown();
        }



        try {
            latch.await();
        } catch (Exception e) {
            // TODO handle exception
        } finally {

        }

    }

}
