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
package io.bsoa.rpc.example.callback;

import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.invoke.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/2/17 17:21. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class CallbackHelloServiceImpl implements CallbackHelloService {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(CallbackHelloServiceImpl.class);


    private static ConcurrentHashMap<String, Callback<List<String>, String>> callbacks = new ConcurrentHashMap<>();

    @Override
    public boolean register(String name, Callback<List<String>, String> callback) {
        LOGGER.info("registry callback, key is {}", name);
        callbacks.put(name, callback);
        return true;
    }

    static {
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            try {
                if (CommonUtils.isNotEmpty(callbacks)) {
                    for (Map.Entry<String, Callback<List<String>, String>> entry : callbacks.entrySet()) {
                        try {
                            String callResult = entry.getValue().notify(Arrays.asList("111", "222"));
                            LOGGER.info("callback client to {}, result is {}", entry.getKey(), callResult);
                        } catch (Exception e) {
                            LOGGER.error("Catch exception when callback, remove callback by key: "
                                    + entry.getKey(), e);
                            callbacks.remove(entry.getKey());
                        }
                    }
                }
            } catch (Throwable e) {
                LOGGER.error("", e);
            }
        }, 1, 1, TimeUnit.SECONDS);

    }

}
