/**
 * JSON.java Created on 16-11-4 下午4:07
 * <p>
 * Copyright (c) 2016 by www.jd.com.
 */
package io.bsoa.rpc.common.json;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * <p></p>
 * Created by zhangg on 2016/7/31 18:16. <br/>
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
