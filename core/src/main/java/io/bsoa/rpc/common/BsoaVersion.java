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
package io.bsoa.rpc.common;

/**
 *
 *
 * Created by zhangg on 2016/7/14 21:23.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public final class BsoaVersion {

    /**
     * 当前JSF版本，例如：<br>
     * 1.2.3-SNAPSHOT对应1230<br>
     * 1.2.3正式版对应1231
     */
    public static final int JSF_VERSION = 1610;

    /**
     * 当前Build版本，每次发布修改  //FIXME
     */
    public static final String JSF_BUILD_VERSION = "1.6.1_201606211749";
}
