/*
 * Copyright © 2016-2017 The BSOA Project
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
package io.bsoa.rpc.client;

import io.bsoa.rpc.ext.Extensible;
import io.bsoa.rpc.message.RpcRequest;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;

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
     * @param request       本次调用（可以得到类名，方法名，方法参数，参数值等）
     * @param providerInfos providers（<b>当前可用</b>的服务Provider列表）
     * @return 路由匹配的服务Provider列表
     */
    public List<ProviderInfo> route(RpcRequest request, List<ProviderInfo> providerInfos);

}
