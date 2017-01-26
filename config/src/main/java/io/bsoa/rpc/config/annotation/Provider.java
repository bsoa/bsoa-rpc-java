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

import static io.bsoa.rpc.config.annotation.AnnotationConstants.DEFAULT_INT_NULL;
import static io.bsoa.rpc.config.annotation.AnnotationConstants.DEFAULT_REGISTER;
import static io.bsoa.rpc.config.annotation.AnnotationConstants.DEFAULT_SUBSCRIBE;

/**
 *
 *
 * Created by zhangg on 2016/7/14 22:09.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Provider {

    /**
     * 服务别名，必填
     *
     * @return the string
     */
    String alias();

    /**
     * 注册到的服务端，必填
     *
     * @return the server [ ]
     */
    Server[] server();

    /**
     * 是否注册到注册中心
     *
     * @return the boolean
     */
    boolean register() default DEFAULT_REGISTER;

    /**
     * 是否动态发布服务
     *
     * @return the boolean
     */
    boolean dynamic() default DEFAULT_SUBSCRIBE;

    /**
     * 服务端权重
     *
     * @return the int
     */
    int weight() default DEFAULT_INT_NULL;
}
