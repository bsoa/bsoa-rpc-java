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

import io.bsoa.rpc.common.json.JSONIgnore;
import io.bsoa.rpc.exception.BsoaRuntimeException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by zhangg on 2016/7/14 21:06.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public final class ClassUtils {

    /**
     * 迭代查询全部字段，包括本类和父类
     *
     * @param clazz 对象类
     * @return 所有字段列表
     */
    public static List<Field> getAllFields(Class clazz) {
        List<Field> all = new ArrayList<Field>();
        for (Class<?> c = clazz; c != Object.class && c != null; c = c.getSuperclass()) {
            Field[] fields = c.getDeclaredFields(); // 所有方法，不包含父类
            for (Field field : fields) {
                int mod = field.getModifiers();
                // 过滤static 和 transient，支持final
                if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) {
                    continue;
                }
                // 过滤ignore字段
                Annotation as = field.getAnnotation(JSONIgnore.class);
                if (as != null) {
                    continue;
                }
                field.setAccessible(true); // 不管private还是protect都可以
                all.add(field);
            }
        }
        return all;
    }


    private static Set<Class> primitiveSet = new HashSet<Class>();

    static {
        primitiveSet.add(Integer.class);
        primitiveSet.add(Long.class);
        primitiveSet.add(Float.class);
        primitiveSet.add(Byte.class);
        primitiveSet.add(Short.class);
        primitiveSet.add(Double.class);
        primitiveSet.add(Character.class);
        primitiveSet.add(Boolean.class);
    }


    /**
     * 实例化一个对象(只检测默认构造函数，其它不管）
     *
     * @param clazz 对象类
     * @param <T>   对象具体类
     * @return 对象实例
     * @throws Exception 没有找到方法，或者无法处理，或者初始化方法异常等
     */
    public static <T> T newInstance(Class<T> clazz) throws BsoaRuntimeException {
        if (primitiveSet.contains(clazz)) {
            return null;
        }
        try {
            if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
                Constructor constructorList[] = clazz.getDeclaredConstructors();
                Constructor defaultConstructor = null;
                for (Constructor con : constructorList) {
                    if (con.getParameterTypes().length == 1) {
                        defaultConstructor = con;
                        break;
                    }
                }
                if (defaultConstructor != null) {
                    if (defaultConstructor.isAccessible()) {
                        return (T) defaultConstructor.newInstance(new Object[]{null});
                    } else {
                        try {
                            defaultConstructor.setAccessible(true);
                            return (T) defaultConstructor.newInstance(new Object[]{null});
                        } finally {
                            defaultConstructor.setAccessible(false);
                        }
                    }
                } else {
                    throw new BsoaRuntimeException(22222, "The " + clazz.getCanonicalName() + " has no default constructor!");
                }
            }
            try {
                return clazz.newInstance();
            } catch (Exception e) {
                Constructor<T> constructor = clazz.getDeclaredConstructor();
                if (constructor.isAccessible()) {
                    throw new RuntimeException("The " + clazz.getCanonicalName() + " has no default constructor!", e);
                } else {
                    try {
                        constructor.setAccessible(true);
                        return constructor.newInstance();
                    } finally {
                        constructor.setAccessible(false);
                    }
                }
            }
        } catch (BsoaRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, e);
        }
    }
}
