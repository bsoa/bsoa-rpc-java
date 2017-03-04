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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhangg on 2016/7/14 21:05.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ReflectUtils {


    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ReflectUtils.class);

    /**
     * 是否默认类型，基本类型+string+date
     *
     * @param clazz the cls
     * @return the boolean
     */
    public static boolean isPrimitives(Class<?> clazz) {
        if (clazz.isArray()) { // 数组，检查数组类型
            return isPrimitiveType(clazz.getComponentType());
        }
        return isPrimitiveType(clazz);
    }

    private static boolean isPrimitiveType(Class<?> clazz) {
        return clazz.isPrimitive() // 基本类型
                // 基本类型的对象
                || Boolean.class == clazz
                || Character.class == clazz
                || Number.class.isAssignableFrom(clazz)
                // string 或者 date
                || String.class == clazz
                || Date.class.isAssignableFrom(clazz);
    }

    /**
     * 得到类所在地址，可以是文件，也可以是jar包
     *
     * @param cls the cls
     * @return the code base
     */
    public static String getCodeBase(Class<?> cls) {

        if (cls == null)
            return null;
        ProtectionDomain domain = cls.getProtectionDomain();
        if (domain == null)
            return null;
        CodeSource source = domain.getCodeSource();
        if (source == null)
            return null;
        URL location = source.getLocation();
        if (location == null)
            return null;
        return location.getFile();
    }

    /**
     * 方法对象缓存 {接口名#方法名#(参数列表):Method} <br>
     * 用于用户传了参数列表
     */
    private final static ConcurrentHashMap<String, Method> methodCache = new ConcurrentHashMap<String, Method>();

    /**
     * 加载Method方法，如果cache找不到，则新反射一个
     *
     * @param clazzName  类名
     * @param methodName 方法名
     * @param argsType   参数列表
     * @return Method对象
     * @throws ClassNotFoundException 如果指定的类加载器无法定位该类
     * @throws NoSuchMethodException  如果找不到匹配的方法
     */
    public static Method getMethod(String clazzName, String methodName, String[] argsType)
            throws ClassNotFoundException, NoSuchMethodException {
        StringBuilder sb = new StringBuilder(256);
        sb.append(clazzName).append("#").append(methodName).append("(");
        if (argsType != null && argsType.length > 0) {
            for (String argType : argsType) {
                sb.append(argType).append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append(")");

        String key = sb.toString();
        // 先从缓存里取
        Method method = methodCache.get(key);
        if (method == null) {
            Class clazz = ClassLoaderUtils.forName(clazzName);
            Class[] classes = ClassTypeUtils.getClasses(argsType);
            method = clazz.getMethod(methodName, classes);
            Method old = methodCache.putIfAbsent(key, method);
            if (old != null) {
                method = old;
            }
        }
        return method;
    }

    /**
     * 加载Method方法，如果cache找不到，则新反射一个
     *
     * @param clazz      类
     * @param methodName 方法名
     * @param argsType   参数列表
     * @return Method对象
     * @throws ClassNotFoundException 如果指定的类加载器无法定位该类
     * @throws NoSuchMethodException  如果找不到匹配的方法
     */
    public static Method getMethod(Class clazz, String methodName, String[] argsType)
            throws NoSuchMethodException, ClassNotFoundException {
        return getMethod(clazz.getCanonicalName(), methodName, argsType);
    }


    /**
     * 方法对象缓存 {接口名#方法名:Method}<br>
     * 用于用户没传了参数列表
     */
    private final static ConcurrentHashMap<String, Class[]> methodArgsTypeCache = new ConcurrentHashMap<String, Class[]>();

    /**
     * 缓存接口的方法，重载方法会被覆盖
     *
     * @param interfaceId 接口名
     * @param methodName  方法名
     * @param argsType    参数列表
     */
    public static void cacheMethodArgsType(String interfaceId, String methodName, Class[] argsType) {
        String key = interfaceId + "#" + methodName;
        methodArgsTypeCache.put(key, argsType);
    }

    /**
     * 缓存接口的方法，重载方法会被覆盖，没有则返回空
     *
     * @param interfaceId 接口类
     * @return 方法的参数列表，没有则返回空
     */
    public static Class[] getMethodArgsType(String interfaceId, String methodName) {
        String key = interfaceId + "#" + methodName;
        return methodArgsTypeCache.get(key);
    }

    /**
     * 得到set方法
     *
     * @param clazz         类
     * @param property      属性
     * @param propertyClazz 属性
     * @return Method 方法对象
     * @throws NoSuchMethodException 没找到
     */
    public static Method getPropertySetterMethod(Class clazz, String property, Class propertyClazz)
            throws NoSuchMethodException {
        String methodName = "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
        try {
            Method method = clazz.getMethod(methodName, propertyClazz);
            return method;
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodException("No setter method for " + clazz.getName() + "#" + property);
        }
    }

    /**
     * 得到get/is方法
     *
     * @param clazz    类
     * @param property 属性
     * @return Method 方法对象 没找到fa
     * @throws NoSuchMethodException
     */
    public static Method getPropertyGetterMethod(Class clazz, String property) throws NoSuchMethodException {
        String methodName = "get" + property.substring(0, 1).toUpperCase() + property.substring(1);
        Method method;
        try {
            method = clazz.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            try {
                methodName = "is" + property.substring(0, 1).toUpperCase() + property.substring(1);
                method = clazz.getMethod(methodName);
            } catch (NoSuchMethodException e1) {
                throw new NoSuchMethodException("No getter method for " + clazz.getName() + "#" + property);
            }
        }
        return method;
    }

    protected static boolean isBeanPropertyReadMethod(Method method) {
        return method != null
                && Modifier.isPublic(method.getModifiers())
                && !Modifier.isStatic(method.getModifiers())
                && method.getReturnType() != void.class
                && method.getDeclaringClass() != Object.class
                && method.getParameterTypes().length == 0
                && (method.getName().startsWith("get") || method.getName().startsWith("is"))
                && (!"get".equals(method.getName()) && !"is".equals(method.getName())); // 排除就叫get和is的方法
    }

    protected static String getPropertyNameFromBeanReadMethod(Method method) {
        if (isBeanPropertyReadMethod(method)) {
            if (method.getName().startsWith("get")) {
                return method.getName().substring(3, 4).toLowerCase()
                        + method.getName().substring(4);
            }
            if (method.getName().startsWith("is")) {
                return method.getName().substring(2, 3).toLowerCase()
                        + method.getName().substring(3);
            }
        }
        return null;
    }

    protected static boolean isBeanPropertyWriteMethod(Method method) {
        return method != null
                && Modifier.isPublic(method.getModifiers())
                && !Modifier.isStatic(method.getModifiers())
                && method.getDeclaringClass() != Object.class
                && method.getParameterTypes().length == 1
                && method.getName().startsWith("set")
                && !"set".equals(method.getName()); // 排除就叫set的方法
    }

    protected static boolean isPublicInstanceField(Field field) {
        return Modifier.isPublic(field.getModifiers())
                && !Modifier.isStatic(field.getModifiers())
                && !Modifier.isFinal(field.getModifiers())
                && !field.isSynthetic();
    }


    /**
     * get name.
     * java.lang.Object[][].class => "java.lang.Object[][]"
     *
     * @param c class.
     * @return name.
     */
    public static String getName(Class<?> c) {
        if (c.isArray()) {
            StringBuilder sb = new StringBuilder();
            do {
                sb.append("[]");
                c = c.getComponentType();
            }
            while (c.isArray());

            return c.getName() + sb.toString();
        }
        return c.getName();
    }


    /**
     * void(V).
     */
    public static final char JVM_VOID = 'V';

    /**
     * boolean(Z).
     */
    public static final char JVM_BOOLEAN = 'Z';

    /**
     * byte(B).
     */
    public static final char JVM_BYTE = 'B';

    /**
     * char(C).
     */
    public static final char JVM_CHAR = 'C';

    /**
     * double(D).
     */
    public static final char JVM_DOUBLE = 'D';

    /**
     * float(F).
     */
    public static final char JVM_FLOAT = 'F';

    /**
     * int(I).
     */
    public static final char JVM_INT = 'I';

    /**
     * long(J).
     */
    public static final char JVM_LONG = 'J';

    /**
     * short(S).
     */
    public static final char JVM_SHORT = 'S';

    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    public static final String JAVA_IDENT_REGEX = "(?:[_$a-zA-Z][_$a-zA-Z0-9]*)";

    public static final String CLASS_DESC = "(?:L" + JAVA_IDENT_REGEX + "(?:\\/" + JAVA_IDENT_REGEX + ")*;)";

    public static final String ARRAY_DESC = "(?:\\[+(?:(?:[VZBCDFIJS])|" + CLASS_DESC + "))";

    public static final String DESC_REGEX = "(?:(?:[VZBCDFIJS])|" + CLASS_DESC + "|" + ARRAY_DESC + ")";

    public static final Pattern DESC_PATTERN = Pattern.compile(DESC_REGEX);

    private static final ConcurrentMap<String, Class<?>> DESC_CLASS_CACHE = new ConcurrentHashMap<String, Class<?>>();

    private static final ConcurrentMap<String, Class<?>> NAME_CLASS_CACHE = new ConcurrentHashMap<String, Class<?>>();

    /**
     * get method name.
     * "void do(int)", "void do()", "int do(java.lang.String,boolean)"
     *
     * @param m method.
     * @return name.
     */
    public static String getName(final Method m) {
        StringBuilder ret = new StringBuilder();
        ret.append(getName(m.getReturnType())).append(' ');
        ret.append(m.getName()).append('(');
        Class<?>[] parameterTypes = m.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0)
                ret.append(',');
            ret.append(getName(parameterTypes[i]));
        }
        ret.append(')');
        return ret.toString();
    }

    /**
     * get constructor name.
     * "()", "(java.lang.String,int)"
     *
     * @param c constructor.
     * @return name.
     */
    public static String getName(final Constructor<?> c) {
        StringBuilder ret = new StringBuilder("(");
        Class<?>[] parameterTypes = c.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0)
                ret.append(',');
            ret.append(getName(parameterTypes[i]));
        }
        ret.append(')');
        return ret.toString();
    }

    /**
     * get class desc.
     * boolean[].class => "[Z"
     * Object.class => "Ljava/lang/Object;"
     *
     * @param c class.
     * @return desc.
     * @throws NotFoundException
     */
    public static String getDesc(Class<?> c) {
        StringBuilder ret = new StringBuilder();

        while (c.isArray()) {
            ret.append('[');
            c = c.getComponentType();
        }

        if (c.isPrimitive()) {
            String t = c.getName();
            if ("void".equals(t)) ret.append(JVM_VOID);
            else if ("boolean".equals(t)) ret.append(JVM_BOOLEAN);
            else if ("byte".equals(t)) ret.append(JVM_BYTE);
            else if ("char".equals(t)) ret.append(JVM_CHAR);
            else if ("double".equals(t)) ret.append(JVM_DOUBLE);
            else if ("float".equals(t)) ret.append(JVM_FLOAT);
            else if ("int".equals(t)) ret.append(JVM_INT);
            else if ("long".equals(t)) ret.append(JVM_LONG);
            else if ("short".equals(t)) ret.append(JVM_SHORT);
        } else {
            ret.append('L');
            ret.append(c.getName().replace('.', '/'));
            ret.append(';');
        }
        return ret.toString();
    }

    /**
     * get class array desc.
     * [int.class, boolean[].class, Object.class] => "I[ZLjava/lang/Object;"
     *
     * @param cs class array.
     * @return desc.
     * @throws NotFoundException
     */
    public static String getDesc(final Class<?>[] cs) {
        if (cs.length == 0)
            return "";

        StringBuilder sb = new StringBuilder(64);
        for (Class<?> c : cs)
            sb.append(getDesc(c));
        return sb.toString();
    }


    public static Class<?> forName(String name) {
        try {
            return name2class(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Not found class " + name + ", cause: " + e.getMessage(), e);
        }
    }

    /**
     * name to class.
     * "boolean" => boolean.class
     * "java.util.Map[][]" => java.util.Map[][].class
     *
     * @param name name.
     * @return Class instance.
     */
    public static Class<?> name2class(String name) throws ClassNotFoundException {
        return name2class(ClassLoaderUtils.getCurrentClassLoader(), name);
    }

    /**
     * name to class.
     * "boolean" => boolean.class
     * "java.util.Map[][]" => java.util.Map[][].class
     *
     * @param cl   ClassLoader instance.
     * @param name name.
     * @return Class instance.
     */
    private static Class<?> name2class(ClassLoader cl, String name) throws ClassNotFoundException {
        int c = 0, index = name.indexOf('[');
        if (index > 0) {
            c = (name.length() - index) / 2;
            name = name.substring(0, index);
        }
        if (c > 0) {
            StringBuilder sb = new StringBuilder();
            while (c-- > 0)
                sb.append("[");

            if ("void".equals(name)) sb.append(JVM_VOID);
            else if ("boolean".equals(name)) sb.append(JVM_BOOLEAN);
            else if ("byte".equals(name)) sb.append(JVM_BYTE);
            else if ("char".equals(name)) sb.append(JVM_CHAR);
            else if ("double".equals(name)) sb.append(JVM_DOUBLE);
            else if ("float".equals(name)) sb.append(JVM_FLOAT);
            else if ("int".equals(name)) sb.append(JVM_INT);
            else if ("long".equals(name)) sb.append(JVM_LONG);
            else if ("short".equals(name)) sb.append(JVM_SHORT);
            else sb.append('L').append(name).append(';'); // "java.lang.Object" ==> "Ljava.lang.Object;"
            name = sb.toString();
        } else {
            if ("void".equals(name)) return void.class;
            else if ("boolean".equals(name)) return boolean.class;
            else if ("byte".equals(name)) return byte.class;
            else if ("char".equals(name)) return char.class;
            else if ("double".equals(name)) return double.class;
            else if ("float".equals(name)) return float.class;
            else if ("int".equals(name)) return int.class;
            else if ("long".equals(name)) return long.class;
            else if ("short".equals(name)) return short.class;
        }

        if (cl == null)
            cl = ClassLoaderUtils.getCurrentClassLoader();
        Class<?> clazz = NAME_CLASS_CACHE.get(name);
        if (clazz == null) {
            clazz = Class.forName(name, true, cl);
            NAME_CLASS_CACHE.put(name, clazz);
        }
        return clazz;
    }

    /**
     * desc to class.
     * "[Z" => boolean[].class
     * "[[Ljava/util/Map;" => java.util.Map[][].class
     *
     * @param cl   ClassLoader instance.
     * @param desc desc.
     * @return Class instance.
     * @throws ClassNotFoundException
     */
    private static Class<?> desc2class(ClassLoader cl, String desc) throws ClassNotFoundException {
        switch (desc.charAt(0)) {
            case JVM_VOID:
                return void.class;
            case JVM_BOOLEAN:
                return boolean.class;
            case JVM_BYTE:
                return byte.class;
            case JVM_CHAR:
                return char.class;
            case JVM_DOUBLE:
                return double.class;
            case JVM_FLOAT:
                return float.class;
            case JVM_INT:
                return int.class;
            case JVM_LONG:
                return long.class;
            case JVM_SHORT:
                return short.class;
            case 'L':
                desc = desc.substring(1, desc.length() - 1).replace('/', '.'); // "Ljava/lang/Object;" ==> "java.lang.Object"
                break;
            case '[':
                desc = desc.replace('/', '.');  // "[[Ljava/lang/Object;" ==> "[[Ljava.lang.Object;"
                break;
            default:
                throw new ClassNotFoundException("Class not found: " + desc);
        }

        if (cl == null)
            cl = ClassLoaderUtils.getCurrentClassLoader();
        Class<?> clazz = DESC_CLASS_CACHE.get(desc);
        if (clazz == null) {
            clazz = Class.forName(desc, true, cl);
            DESC_CLASS_CACHE.put(desc, clazz);
        }
        return clazz;
    }

    /**
     * get class array instance.
     *
     * @param desc desc.
     * @return Class class array.
     * @throws ClassNotFoundException
     */
    public static Class<?>[] desc2classArray(String desc) throws ClassNotFoundException {
        Class<?>[] ret = desc2classArray(ClassLoaderUtils.getCurrentClassLoader(), desc);
        return ret;
    }

    /**
     * get class array instance.
     *
     * @param cl   ClassLoader instance.
     * @param desc desc.
     * @return Class[] class array.
     * @throws ClassNotFoundException
     */
    private static Class<?>[] desc2classArray(ClassLoader cl, String desc) throws ClassNotFoundException {
        if (desc.length() == 0)
            return EMPTY_CLASS_ARRAY;

        List<Class<?>> cs = new ArrayList<Class<?>>();
        Matcher m = DESC_PATTERN.matcher(desc);
        while (m.find())
            cs.add(desc2class(cl, m.group()));
        return cs.toArray(EMPTY_CLASS_ARRAY);
    }


//
//
//    private static final Map<ClassLoader, ClassPool> POOL_MAP = new ConcurrentHashMap<ClassLoader, ClassPool>(); //ClassLoader - ClassPool
//
//    public static ClassPool getClassPool(ClassLoader loader)
//    {
//        if( loader == null )
//            return ClassPool.getDefault();
//
//        ClassPool pool = POOL_MAP.get(loader);
//        if( pool == null )
//        {
//            pool = new ClassPool(true);
//            pool.appendClassPath(new LoaderClassPath(loader));
//            POOL_MAP.put(loader, pool);
//        }
//        return pool;
//    }
//
//    /**
//     * get class desc.
//     * Object.class => "Ljava/lang/Object;"
//     * boolean[].class => "[Z"
//     *
//     * @param c class.
//     * @return desc.
//     * @throws NotFoundException
//     */
//    public static String getDesc(final CtClass c) throws NotFoundException
//    {
//        StringBuilder ret = new StringBuilder();
//        if( c.isArray() )
//        {
//            ret.append('[');
//            ret.append(getDesc(c.getComponentType()));
//        }
//        else if( c.isPrimitive() )
//        {
//            String t = c.getName();
//            if( "void".equals(t) ) ret.append(JVM_VOID);
//            else if( "boolean".equals(t) ) ret.append(JVM_BOOLEAN);
//            else if( "byte".equals(t) ) ret.append(JVM_BYTE);
//            else if( "char".equals(t) ) ret.append(JVM_CHAR);
//            else if( "double".equals(t) ) ret.append(JVM_DOUBLE);
//            else if( "float".equals(t) ) ret.append(JVM_FLOAT);
//            else if( "int".equals(t) ) ret.append(JVM_INT);
//            else if( "long".equals(t) ) ret.append(JVM_LONG);
//            else if( "short".equals(t) ) ret.append(JVM_SHORT);
//        }
//        else
//        {
//            ret.append('L');
//            ret.append(c.getName().replace('.','/'));
//            ret.append(';');
//        }
//        return ret.toString();
//    }
}
