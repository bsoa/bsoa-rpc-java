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
package io.bsoa.rpc.client;

import java.util.List;
import javax.annotation.concurrent.ThreadSafe;

import io.bsoa.rpc.ext.Extensible;
import io.bsoa.rpc.message.RpcRequest;

/**
 * 路由器：从一堆Provider中筛选出一堆Provider
 * <p>
 * Created by zhangg on 2016/7/16 01:05.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extensible(singleton = false)
@ThreadSafe
public interface Router {

    /**
     * 指定规则，自定义路由的话可以忽略。
     *
     * @param ruleJson 规则json字符串
     */
    public void setRule(String ruleJson);

    /**
     * 筛选Provider
     *
     * @param request   本次调用（可以得到类名，方法名，方法参数，参数值等）
     * @param providers providers（<b>当前可用</b>的服务Provider列表）
     * @return 路由匹配的服务Provider列表
     */
    public List<Provider> route(RpcRequest request, List<Provider> providers);

}
