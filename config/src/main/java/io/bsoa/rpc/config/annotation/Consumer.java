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
package io.bsoa.rpc.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static io.bsoa.rpc.config.annotation.AnnotationConstants.DEFAULT_STRING_NULL;

/**
 * Created by zhangg on 2016/7/14 22:06.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Consumer {
    /**
     * 服务别名，必填
     *
     * @return the string
     */
    String alias();

    /**
     * 调用协议，必填.
     *
     * @return the string
     */
    String protocol() default DEFAULT_STRING_NULL;

    /**
     * 集群策略，选填
     *
     * @return the string
     */
    String cluster() default DEFAULT_STRING_NULL;

    /**
     * 失败后重试次数，选填
     *
     * @return the int
     */
    int retries() default -1;

    /**
     * 调用超时，选填
     *
     * @return the int
     */
    int timeout() default -1;

    /**
     * 直连地址，选填
     *
     * @return the string
     */
    String url() default DEFAULT_STRING_NULL;

    /**
     * 负载均衡算法，选填
     *
     * @return the string
     */
    String loadBalancer() default DEFAULT_STRING_NULL;

    /**
     * 序列化方式，选填
     *
     * @return the string
     */
    String serialization() default DEFAULT_STRING_NULL;

    /**
     * 是否延迟加载服务端连接，选填
     *
     * @return the boolean
     */
    boolean lazy() default false;
}


