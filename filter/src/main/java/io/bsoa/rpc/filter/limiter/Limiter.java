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
package io.bsoa.rpc.filter.limiter;

/**
 * 限制器接口
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public interface Limiter {

    /**
     * 是否超过限制
     *
     * @param interfaceName
     *         接口
     * @param methodName
     *         方法
     * @param alias
     *         别名
     * @param appId
     *         appId
     * @return true不可以调用 false可以调用
     */
    public boolean isOverLimit(String interfaceName, String methodName, String alias, String appId);

    /**
     * 得到明细
     *
     * @return 详细描述
     */
    public String getDetails();
}
