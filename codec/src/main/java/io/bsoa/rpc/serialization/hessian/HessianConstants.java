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
package io.bsoa.rpc.serialization.hessian;

import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>保存一些新旧类的映射关系<br>
 * 例如旧的发来com.xxx.Obj，需要拿com.yyy.Obj去解析，则可以使用此类<br></p>
 * <p>
 * Created by zhangg on 2016/12/24 21:09. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class HessianConstants {

    /**
     * 保留映射关系 旧类-->新类
     */
    private final static ConcurrentHashMap<String, String> objectMap = new ConcurrentHashMap<String, String>();

    /**
     * Registry mapping.
     *
     * @param oldclass
     *         the oldclass
     * @param newclass
     *         the newclass
     */
    public static void registryMapping(String oldclass, String newclass) {
        objectMap.put(oldclass, newclass);
    }

    /**
     * Unregistry mapping.
     *
     * @param oldclass
     *         the oldclass
     */
    public static void unregistryMapping(String oldclass) {
        objectMap.remove(oldclass);
    }

    /**
     * Check mapping.
     *
     * @param clazz
     *         the clazz
     * @return the string
     */
    public static String checkMapping(String clazz) {
        if (objectMap.isEmpty()) {
            return clazz;
        }
        String mapclazz = objectMap.get(clazz);
        return mapclazz != null ? mapclazz : clazz;
    }

    /**
     * 返回是个异常
     */
    public static final byte RESPONSE_EXCEPTION = -1;
    /**
     * 返回为空
     */
    public static final byte RESPONSE_NULL = 0;
    /**
     * 返回有值
     */
    public static final byte RESPONSE_DATA = 1;


    /**
     * 空的Object数组，无参方法
     */
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    /**
     * 空的Class数组，无参方法
     */
    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
}