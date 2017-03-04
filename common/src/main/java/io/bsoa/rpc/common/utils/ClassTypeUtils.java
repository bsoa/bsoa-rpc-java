/*
 * Copyright 2016 The BSOA Project
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
package io.bsoa.rpc.common.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>类型转换工具类</p>
 * <p>调用端时将类描述转换为字符串传输。服务端将字符串转换为具体的类</p>
 * <pre>
 *     保证传递的时候值为可阅读格式，而不是jvm格式（[Lxxx;）：
 *         普通：java.lang.String、java.lang.String[]
 *         基本类型：int、int[]
 *         匿名类：io.bsoa.example.Xxx$1、io.bsoa.example.Xxx$1[]
 *         本地类：io.bsoa.example.Xxx$1Local、io.bsoa.example.Xxx$1Local[]
 *         成员类：io.bsoa.example.Xxx$Member、io.bsoa.example.Xxx$Member[]
 *         内部类：io.bsoa.example.Inner、io.bsoa.example.Inner[]
 *     同时Class.forName的时候又会解析出Class。
 *     </pre>
 * <p>
 * Created by zhangg on 2016/7/14 21:06.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ClassTypeUtils {


    /**
     * String-->Class 缓存，指定大小
     */
    private final static Map<String, Class> classMap = new ConcurrentHashMap<String, Class>();

    /**
     * String-->Class 缓存，指定大小
     */
    private final static Map<Class, String> typeStrMap = new ConcurrentHashMap<Class, String>();

    /**
     * Class[]转String[]
     *
     * @param typeStrs 对象描述[]
     * @return Class[]
     */
    public static Class[] getClasses(String[] typeStrs) throws RuntimeException {
        if (CommonUtils.isEmpty(typeStrs)) {
            return new Class[0];
        } else {
            Class[] classes = new Class[typeStrs.length];
            for (int i = 0; i < typeStrs.length; i++) {
                classes[i] = getClass(typeStrs[i]);
            }
            return classes;
        }
    }

    /**
     * String转Class
     *
     * @param typeStr 对象描述
     * @return Class[]
     */
    public static Class getClass(String typeStr) {
        Class clazz = classMap.get(typeStr);
        if (clazz == null) {
            if ("void".equals(typeStr)) clazz = void.class;
            else if ("boolean".equals(typeStr)) clazz = boolean.class;
            else if ("byte".equals(typeStr)) clazz = byte.class;
            else if ("char".equals(typeStr)) clazz = char.class;
            else if ("double".equals(typeStr)) clazz = double.class;
            else if ("float".equals(typeStr)) clazz = float.class;
            else if ("int".equals(typeStr)) clazz = int.class;
            else if ("long".equals(typeStr)) clazz = long.class;
            else if ("short".equals(typeStr)) clazz = short.class;
            else {
                String jvmName = canonicalNameToJvmName(typeStr);
                clazz = ClassLoaderUtils.forName(jvmName);
            }
            classMap.put(typeStr, clazz);
        }
        return clazz;
    }


    private static String canonicalNameToJvmName(String typeStr) {
        boolean isarray = typeStr.endsWith("[]");
        if (isarray) {
            String t = ""; // 计数，看上几维数组
            while (isarray) {
                typeStr = typeStr.substring(0, typeStr.length() - 2);
                t += "[";
                isarray = typeStr.endsWith("[]");
            }
            if ("boolean".equals(typeStr)) typeStr = t + "Z";
            else if ("byte".equals(typeStr)) typeStr = t + "B";
            else if ("char".equals(typeStr)) typeStr = t + "C";
            else if ("double".equals(typeStr)) typeStr = t + "D";
            else if ("float".equals(typeStr)) typeStr = t + "F";
            else if ("int".equals(typeStr)) typeStr = t + "I";
            else if ("long".equals(typeStr)) typeStr = t + "J";
            else if ("short".equals(typeStr)) typeStr = t + "S";
            else typeStr = t + "L" + typeStr + ";";
        }
        return typeStr;
    }

    /**
     * Class[]转String[] <br>
     * 注意，得到的String可能不能直接用于Class.forName，请使用getClass(String)反向获取
     *
     * @param types Class[]
     * @return 对象描述
     */
    public static String[] getTypeStrs(Class[] types) {
        if (CommonUtils.isEmpty(types)) {
            return new String[0];
        } else {
            String[] strings = new String[types.length];
            for (int i = 0; i < types.length; i++) {
                strings[i] = getTypeStr(types[i]);
            }
            return strings;
        }
    }

    /**
     * Class转String<br>
     * 注意，得到的String可能不能直接用于Class.forName，请使用getClass(String)反向获取
     *
     * @param clazz Class
     * @return 对象
     * @see #getClass(String)
     */
    public static String getTypeStr(Class clazz) {
        String typeStr = typeStrMap.get(clazz);
        if (typeStr == null) {
            if (clazz.isArray()) {
                String name = clazz.getName(); // 原始名字：[Ljava.lang.String;
                typeStr = jvmNameToCanonicalName(name); // java.lang.String[]
            } else {
                typeStr = clazz.getName();
            }
            typeStrMap.put(clazz, typeStr);
        }
        return typeStr;
    }

    private static String jvmNameToCanonicalName(String jvmName) {
        boolean isarray = jvmName.charAt(0) == '[';
        if (isarray) {
            String cnName = StringUtils.EMPTY; // 计数，看上几维数组
            int i = 0;
            for (; i < jvmName.length(); i++) {
                if (jvmName.charAt(i) != '[') {
                    break;
                }
                cnName += "[]";
            }
            String componentType = jvmName.substring(i, jvmName.length());
            if ("Z".equals(componentType)) cnName = "boolean" + cnName;
            else if ("B".equals(componentType)) cnName = "byte" + cnName;
            else if ("C".equals(componentType)) cnName = "char" + cnName;
            else if ("D".equals(componentType)) cnName = "double" + cnName;
            else if ("F".equals(componentType)) cnName = "float" + cnName;
            else if ("I".equals(componentType)) cnName = "int" + cnName;
            else if ("J".equals(componentType)) cnName = "long" + cnName;
            else if ("S".equals(componentType)) cnName = "short" + cnName;
            else cnName = componentType.substring(1, componentType.length() - 1) + cnName; // 对象的 去掉L
            return cnName;
        }
        return jvmName;
    }
}
