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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import io.bsoa.rpc.exception.BsoaRuntimeException;

/**
 * Created by zhangg on 2016/7/14 21:06.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public final class ClassUtils {

    /**
     * 迭代查询全部方法，包括本类和父类
     *
     * @param clazz 对象类
     * @return 所有字段列表
     */
    public static List<Method> getAllMethods(Class clazz) {
        List<Method> all = new ArrayList<>();
        for (Class<?> c = clazz; c != Object.class && c != null; c = c.getSuperclass()) {
            Method[] methods = c.getDeclaredMethods(); // 所有方法，不包含父类
            for (Method method : methods) {
                int mod = method.getModifiers();
                // native的不要
                if (Modifier.isNative(mod)) {
                    continue;
                }
                method.setAccessible(true); // 不管private还是protect都可以
                all.add(method);
            }
        }
        return all;
    }

    /**
     * 迭代查询全部字段，包括本类和父类
     *
     * @param clazz 对象类
     * @return 所有字段列表
     */
    public static List<Field> getAllFields(Class clazz) {
        List<Field> all = new ArrayList<>();
        for (Class<?> c = clazz; c != Object.class && c != null; c = c.getSuperclass()) {
            Field[] fields = c.getDeclaredFields(); // 所有方法，不包含父类
            for (Field field : fields) {
                int mod = field.getModifiers();
                // 过滤static 和 transient，支持final
                if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) {
                    continue;
                }
//                // 过滤ignore字段
//                Annotation as = field.getAnnotation(JSONIgnore.class);
//                if (as != null) {
//                    continue;
//                }
                field.setAccessible(true); // 不管private还是protect都可以
                all.add(field);
            }
        }
        return all;
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
        if (clazz == Short.class || clazz == short.class) {
            return (T) Short.valueOf((short) 0);
        } else if (clazz == Integer.class || clazz == int.class) {
            return (T) Integer.valueOf(0);
        } else if (clazz == Long.class || clazz == long.class) {
            return (T) Long.valueOf(0L);
        } else if (clazz == Double.class || clazz == double.class) {
            return (T) Double.valueOf(0d);
        } else if (clazz == Float.class || clazz == float.class) {
            return (T) Float.valueOf(0f);
        } else if (clazz == Byte.class || clazz == byte.class) {
            return (T) Byte.valueOf((byte) 0);
        } else if (clazz == Character.class || clazz == char.class) {
            return (T) Character.valueOf((char) 0);
        } else if (clazz == Boolean.class || clazz == boolean.class) {
            return (T) Boolean.FALSE;
        }

        try {
            // 普通类，如果是成员类（需要多传一个父类参数）
            if (!(clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers()))) {
                try {
                    // 先找一个空的构造函数
                    Constructor<T> constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    return constructor.newInstance();
                } catch (Exception e) {
                }
            }
            // 不行的话，找一个最少参数的构造函数
            Constructor<T>[] constructors = (Constructor<T>[]) clazz.getDeclaredConstructors();
            if (constructors == null || constructors.length == 0) {
                throw new BsoaRuntimeException(22222, "The " + clazz.getCanonicalName()
                        + " has no default constructor!");
            }
            Constructor<T> constructor = constructors[0];
            if (constructor.getParameterTypes().length > 0) {
                for (Constructor<T> c : constructors) {
                    if (c.getParameterTypes().length <
                            constructor.getParameterTypes().length) {
                        constructor = c;
                        if (constructor.getParameterTypes().length == 0) {
                            break;
                        }
                    }
                }
            }
            constructor.setAccessible(true);
            // 虚拟构造函数的参数值，基本类型使用默认值，其它类型使用null
            Class<?>[] argTypes = constructor.getParameterTypes();
            Object[] args = new Object[argTypes.length];
            for (int i = 0; i < args.length; i++) {
                args[i] = getParamArg(argTypes[i]);
            }
            return constructor.newInstance(args);
        } catch (BsoaRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, e);
        }
    }

    /**
     * 实例化一个对象(根据参数自动检测构造方法）
     *
     * @param clazz 对象类
     * @param args  构造函数需要的参数
     * @param <T>   对象具体类
     * @return 对象实例
     * @throws Exception 没有找到方法，或者无法处理，或者初始化方法异常等
     * @argTypes 构造函数需要的参数类型
     */
    public static <T> T newInstanceWithArgs(Class<T> clazz, Class<?>[] argTypes, Object[] args) throws BsoaRuntimeException {
        if (CommonUtils.isEmpty(argTypes)) {
            return newInstance(clazz);
        }
        try {
            if (!(clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers()))) {
                Constructor<T> constructor = clazz.getDeclaredConstructor(argTypes);
                constructor.setAccessible(true);
                return constructor.newInstance(args);
            } else {
                Constructor<T>[] constructors = (Constructor<T>[]) clazz.getDeclaredConstructors();
                if (constructors == null || constructors.length == 0) {
                    throw new BsoaRuntimeException(22222, "The " + clazz.getCanonicalName()
                            + " has no constructor with argTypes :" + argTypes);
                }
                Constructor<T> constructor = null;
                for (Constructor<T> c : constructors) {
                    Class[] ps = c.getParameterTypes();
                    if (ps.length == argTypes.length + 1) { // 长度多一
                        boolean allMath = true;
                        for (int i = 1; i < ps.length; i++) { // 而且第二个开始的参数类型匹配
                            if (ps[i] != argTypes[i - 1]) {
                                allMath = false;
                                break;
                            }
                        }
                        if (allMath) {
                            constructor = c;
                            break;
                        }
                    }
                }
                if (constructor == null) {
                    throw new BsoaRuntimeException(22222, "The " + clazz.getCanonicalName()
                            + " has no constructor with argTypes :" + argTypes);
                } else {
                    constructor.setAccessible(true);
                    Object[] newArgs = new Object[args.length + 1];
                    System.arraycopy(args, 0, newArgs, 1, args.length);
                    return constructor.newInstance(newArgs);
                }
            }
        } catch (BsoaRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, e);
        }
    }

    protected static Object getParamArg(Class clazz) {
        if (clazz == Short.class || clazz == short.class) {
            return (short) 0;
        } else if (clazz == Integer.class || clazz == int.class) {
            return 0;
        } else if (clazz == Long.class || clazz == long.class) {
            return 0L;
        } else if (clazz == Double.class || clazz == double.class) {
            return 0d;
        } else if (clazz == Float.class || clazz == float.class) {
            return 0f;
        } else if (clazz == Byte.class || clazz == byte.class) {
            return (byte) 0;
        } else if (clazz == Character.class || clazz == char.class) {
            return (char) 0;
        } else if (clazz == Boolean.class || clazz == boolean.class) {
            return Boolean.FALSE;
        } else {
            return null;
        }
    }

    /**
     * 得到方法关键字
     *
     * @param interfaceName 接口名
     * @param methodName    方法名
     * @return 关键字
     */
    public static String getMethodKey(String interfaceName, String methodName) {
        return interfaceName + "#" + methodName;
    }
}
