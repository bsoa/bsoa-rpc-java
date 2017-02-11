/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bsoa.rpc.context;

import java.util.concurrent.ConcurrentHashMap;

import io.bsoa.rpc.invoke.StreamObserver;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/2/11 11:43. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class StreamContext {

    /**
     * 保留的streamId和 本地实例的对应关系
     */
    private static ConcurrentHashMap<String, StreamObserver> insMap = new ConcurrentHashMap<>();
    /**
     * 保留的streamId和 本地代理类的对应关系
     */
    private static ConcurrentHashMap<String, StreamObserver> proxyMap = new ConcurrentHashMap<>();
    /**
     * 保留的请求参数带StreamObserver的方法和实际传递类的对应关系
     */
    private static ConcurrentHashMap<String, Class> streamMethodReq = new ConcurrentHashMap<>();
    /**
     * 保留的响应是StreamObserver的方法和实际传递类的对应关系
     */
    private static ConcurrentHashMap<String, Class> streamMethodRes = new ConcurrentHashMap<>();

    /**
     * 保存StreamObserver的实际实例
     *
     * @param key  StreamObserver的唯一标识
     * @param impl StreamObserver实现类
     */
    public static void putStreamIns(String key, StreamObserver impl) {
        insMap.put(key, impl);
    }

    /**
     * 删除StreamObserver的实际实例
     *
     * @param key StreamObserver的唯一标识
     */
    public static StreamObserver getStreamIns(String key) {
        return insMap.get(key);
    }

    /**
     * 删除StreamObserver的实际实例
     *
     * @param key StreamObserver的唯一标识
     */
    public static void removeStreamIns(String key) {
        insMap.remove(key);
    }

    /**
     * 保存StreamObserver的代理实例
     *
     * @param key   StreamObserver的唯一标识
     * @param proxy StreamObserver代理类
     */
    public static void putStreamProxy(String key, StreamObserver proxy) {
        proxyMap.put(key, proxy);
    }

    /**
     * 保存StreamObserver的代理实例
     *
     * @param key StreamObserver的唯一标识
     */
    public static StreamObserver getStreamProxy(String key) {
        return proxyMap.get(key);
    }

    /**
     * 删除StreamObserver的代理实例
     *
     * @param key StreamObserver的唯一标识
     */
    public static void removeStreamProxy(String key) {
        proxyMap.remove(key);
    }

    /**
     * 保存StreamObserver的实际实例
     *
     * @param key   StreamObserver的唯一标识
     * @param clazz StreamObserver实现类
     */
    public static void registryParamOfStreamMethod(String key, Class clazz) {
        streamMethodReq.put(key, clazz);
    }

    /**
     * 是否有StreamObserver参数
     *
     * @param key StreamObserver的唯一标识
     * @return 是否有StreamObserver参数
     */
    public static boolean hasStreamObserverParameter(String key) {
        return streamMethodReq.containsKey(key);
    }

    /**
     * 得到StreamObserver的实际类型
     *
     * @param key StreamObserver的唯一标识
     */
    public static Class getParamTypeOfStreamMethod(String key) {
        return streamMethodReq.get(key);
    }

    /**
     * 保存StreamObserver的实际实例
     *
     * @param key   StreamObserver的唯一标识
     * @param clazz StreamObserver实现类
     */
    public static void registryReturnOfStreamMethod(String key, Class clazz) {
        streamMethodRes.put(key, clazz);
    }

    /**
     * 是否有StreamObserver参数
     *
     * @param key StreamObserver的唯一标识
     * @return 是否有StreamObserver参数
     */
    public static boolean hasStreamObserverReturn(String key) {
        return streamMethodRes.containsKey(key);
    }

    /**
     * 得到StreamObserver的实际类型
     *
     * @param key StreamObserver的唯一标识
     */
    public static Class getReturnTypeOfStreamMethod(String key) {
        return streamMethodRes.get(key);
    }
}
