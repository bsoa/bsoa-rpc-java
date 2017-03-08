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
package io.bsoa.rpc.invoke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/1/1 16:37. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class CallbackContext {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(CallbackContext.class);
    /**
     * 方法名notify
     */
    public static final String METHOD_NOTIFY = "notify";
    /**
     * 方法名onCompleted
     */
    public static final String METHOD_CLOSE = "close";
    /**
     * 实例Id生成器
     */
    public static final AtomicInteger CALLBACK_ID_GEN = new AtomicInteger();

    /**
     * 保留的streamId和 本地实例的对应关系
     */
    private static ConcurrentHashMap<String, Callback> insMap = new ConcurrentHashMap<>();

    /**
     * 接口+方法 ： 实际的ServerCallback数据类型
     */
    private static ConcurrentHashMap<String, Class> callbackNames = new ConcurrentHashMap<>();

    /**
     * 保存Callback的实际实例
     *
     * @param key  Callback的唯一标识
     * @param impl Callback实现类
     */
    public static void putCallbackIns(String key, Callback impl) {
        insMap.put(key, impl);
    }

    /**
     * 删除Callback的实际实例
     *
     * @param key Callback的唯一标识
     */
    public static Callback getCallbackIns(String key) {
        return insMap.get(key);
    }

    /**
     * 删除Callback的实际实例
     *
     * @param key Callback的唯一标识
     */
    public static void removeCallbackIns(String key) {
        insMap.remove(key);
    }

    /**
     * 得到缓存的Callback的数量
     *
     * @return 缓存的Callback的数量
     */
    public static int getInsMapSize() {
        return insMap.size();
    }

    /**
     * 保存Callback的实际实例
     *
     * @param key   Callback的唯一标识
     * @param clazz Callback实现类
     */
    public static void registryParamOfCallbackMethod(String key, Class clazz) {
        callbackNames.put(key, clazz);
    }

    /**
     * 是否有Callback参数
     *
     * @param key 接口方法
     * @return 是否有Callback参数
     */
    public static boolean hasCallbackParameter(String key) {
        return callbackNames.containsKey(key);
    }

    /**
     * 得到Callback的实际类型
     *
     * @param key Callback的唯一标识
     */
    public static Class getParamTypeOfCallbackMethod(String key) {
        return callbackNames.get(key);
    }
}
