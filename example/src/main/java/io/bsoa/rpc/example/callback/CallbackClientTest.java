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
package io.bsoa.rpc.example.callback;

import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.invoke.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/18 10:58. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class CallbackClientTest {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(CallbackClientTest.class);

    public static void main(String[] args) throws InterruptedException {

        ConsumerConfig<CallbackHelloService> consumerConfig = new ConsumerConfig<CallbackHelloService>()
                .setInterfaceId(CallbackHelloService.class.getName())
                .setUrl("bsoa://127.0.0.1:22000")
                .setTimeout(300000)
                .setRegister(false);
        CallbackHelloService helloService = consumerConfig.refer();

        try {
            for (int i = 0; i < 2; i++) {
                final String key = "callbackKey" + i;
                helloService.register(key, new Callback<List<String>, String>() {
                    @Override
                    public String notify(List<String> result) {
                        for (String s : result) {
                            LOGGER.info("receive {} 's callback:{}", key, s);
                        }
                        return key + " ok!";
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }

        try {
            Thread.sleep(10000);
        } catch (Exception e) {
            LOGGER.error("", e);
        }

    }

}
