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
package io.bsoa.rpc.common.json;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class JSON {

    public static final String CLASS_KEY = "@type";

    /**
     * 对象转为json字符串
     *
     * @param object 对象
     * @return json字符串
     */
    public static String toJSONString(Object object) {
        return JSONSerializer.serialize(object);
    }

    /**
     * 序列化json基本类型（自定义对象需要先转换成Map）
     *
     * @param object  需要序列化的对象
     * @param addType 是否增加自定义对象标记
     * @return Json格式字符串
     */
    public static String toJSONString(Object object, boolean addType) {
        return JSONSerializer.serialize(object, addType);
    }

    /**
     * 解析为指定对象
     *
     * @param text  json字符串
     * @param clazz 指定类
     * @param <T>   指定对象
     * @return 指定对象
     */
    public static <T> T parseObject(String text, Class<T> clazz) {
        Object obj = JSONSerializer.deserialize(text);
        return BeanSerializer.deserializeByType(obj, clazz);
    }

    /**
     * 反序列化json转对象(只返回JSON的标准类型：String，Number，True/False/Null，Map，Array)
     *
     * @param json json字符串
     * @return 转换后的对象
     * @throws ParseException 解析异常
     */
//    public static <T> T parseObjectByType(String json, TypeReference typeReference) throws ParseException {
//        return parseObject(json, typeReference.getType());
//    }

    /**
     * 获取需要序列化的字段，跳过
     *
     * @param targetClass 目标类
     * @param <T>
     * @return
     */
    protected static <T> List<Field> getSerializeFields(Class targetClass) {
        List<Field> all = new ArrayList<Field>();
        for (Class<?> c = targetClass; c != Object.class && c != null; c = c.getSuperclass()) {
            Field[] fields = c.getDeclaredFields();

            for (Field f : fields) {
                int mod = f.getModifiers();
                // transient, static,  @JSONIgnore : skip
                if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) {
                    continue;
                }
                JSONIgnore ignore = f.getAnnotation(JSONIgnore.class);
                if (ignore != null) {
                    continue;
                }

                f.setAccessible(true);
                all.add(f);
            }
        }
        return all;
    }
}
