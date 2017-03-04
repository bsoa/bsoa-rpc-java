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
package io.bsoa.rpc.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static io.bsoa.rpc.config.annotation.AnnotationConstants.DEFAULT_INT_NULL;
import static io.bsoa.rpc.config.annotation.AnnotationConstants.DEFAULT_PROTOCOL;
import static io.bsoa.rpc.config.annotation.AnnotationConstants.DEFAULT_STRING_NULL;

/**
 * Created by zhangg on 2016/7/14 22:10.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface Server {

    /**
     * 协议名，必填
     *
     * @return the string
     */
    String protocol() default DEFAULT_PROTOCOL;

    /**
     * 主机地址，选填
     *
     * @return the string
     */
    String host() default DEFAULT_STRING_NULL;

    /**
     * 端口地址，选填
     *
     * @return the int
     */
    int port() default DEFAULT_INT_NULL;
}