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
package io.bsoa.rpc.server;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.base.Invoker;
import io.bsoa.rpc.exception.BsoaRpcException;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/1/1 17:17. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public final class InvokerHolder {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(InvokerHolder.class);

    /**
     * 服务端扩展器
     */
    private final static ConcurrentHashMap<String, Invoker> EXPORTED_INVOKER = new ConcurrentHashMap<>();

    /**
     * 初始化Server实例
     *
     * @param key invoker
     * @return
     */
    public static Invoker getInvoker(String key) {
        return EXPORTED_INVOKER.get(key);
    }

    /**
     * 初始化Server实例
     *
     * @param key     invoker关键字
     * @param invoker invoker
     * @return
     */
    public static void setInvoker(String key, Invoker invoker) {
        Invoker oldInvoker = EXPORTED_INVOKER.putIfAbsent(key, invoker);
        if (oldInvoker != null) {
            throw new BsoaRpcException("xxxxx");
        }
    }

    /**
     * 初始化Server实例
     *
     * @param key     invoker关键字
     * @param invoker invoker
     * @return
     */
    public static void removeInvoker(String key) {
        EXPORTED_INVOKER.remove(key);
    }

    /**
     * 构建key
     */
    public static String buildKey(String interfaceId, String tags) {
        return interfaceId + "#" + tags;
    }
}
