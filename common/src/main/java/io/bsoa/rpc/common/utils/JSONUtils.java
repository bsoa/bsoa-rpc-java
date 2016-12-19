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
package io.bsoa.rpc.common.utils;

import io.bsoa.rpc.common.json.JSON;

/**
 * <p></p>
 *
 * Created by zhangg on 2016/11/6 21:41. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class JSONUtils {

    /**
     * 对象转为json字符串
     *
     * @param object
     *         对象
     * @return json字符串
     */
    public static final String toJSONString(Object object) {
        return JSON.toJSONString(object);
    }

    /**
     * 解析为指定对象
     *
     * @param text
     *         json字符串
     * @param clazz
     *         指定类
     * @param <T>
     *         指定对象
     * @return 指定对象
     */
    public static final <T> T parseObject(String text, Class<T> clazz) {
//        return JSON.parseObject(text, clazz);
        return null;
    }

}
