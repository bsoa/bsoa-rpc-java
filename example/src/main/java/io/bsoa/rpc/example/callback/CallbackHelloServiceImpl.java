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
package io.bsoa.rpc.example.callback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.invoke.Callback;

/**
 * <p></p>
 *
 * Created by zhangg on 2017/2/17 17:21. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class CallbackHelloServiceImpl implements CallbackHelloService {

    static Map<String, Callback<String, List<String>>> callbacks = new HashMap<>();


    @Override
    public boolean register(String name, Callback<String, List<String>> callback) {
        callbacks.put(name, callback);
        return true;
    }

    static {
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate((Runnable) () -> {
            try {
                if(CommonUtils.isNotEmpty(callbacks)){
                }
            } catch (Throwable e) {

            }
        }, 1000,1000, TimeUnit.SECONDS);

    }

}
