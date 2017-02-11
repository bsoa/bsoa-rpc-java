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

import io.bsoa.rpc.common.utils.StringUtils;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/1/23 20:59. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class AnnotationConstants {
    public static final String DEFAULT_PROTOCOL = "bsoa";
    public static final String DEFAULT_SERIALIZATION = "hessian2";
    public static final String CLUSTER_FAILOVER = "failover";
    public static final int DEFAULT_RETRIES_TIME = 0;
    public static final int DEFAULT_CLIENT_INVOKE_TIMEOUT = 5000;
    public static final boolean DEFAULT_REGISTER = true;
    public static final boolean DEFAULT_SUBSCRIBE = true;
    public static final String DEFAULT_STRING_NULL = StringUtils.EMPTY;
    public static final int DEFAULT_INT_NULL = -1;
}
