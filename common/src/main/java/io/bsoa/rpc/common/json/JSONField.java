package io.bsoa.rpc.common.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/7/31 18:16. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JSONField {

    /**
     * 字段别名
     *
     * @return 别名
     */
    String alias() default "";

    /**
     * 是否必填
     *
     * @return 是否必填（不能为空）
     */
    boolean isRequired() default false;

    /**
     * 是否为空跳过
     *
     * @return 是否为空跳过
     */
    boolean skipIfNull() default false;

}
