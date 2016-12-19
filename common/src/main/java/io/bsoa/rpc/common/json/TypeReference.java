/**
 * TypeReference.java Created on 16-11-7 下午4:40
 * <p>
 * Copyright (c) 2016 by www.jd.com.
 */
package io.bsoa.rpc.common.json;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Title: <br>
 * <p>
 * Description: <br>
 * <p>
 * Company: <a href=www.jd.com>京东</a><br>
 *
 * @author <a href=mailto:zhanggeng@jd.com>章耿</a>
 */
public class TypeReference<T> {

    private final Type type;

    protected TypeReference() {
        Type superClass = getClass().getGenericSuperclass();
        type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    public Type getType() {
        return type;
    }

//    public final static Type LIST_STRING = new TypeReference<List<String>>() {
//    }.getType();
}
