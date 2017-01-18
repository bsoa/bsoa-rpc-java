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

import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.ext.Extensible;
import io.bsoa.rpc.message.RpcRequest;

/**
 * 负载均衡器：从一堆Provider列表里选出一个
 * <p>
 * Created by zhangg on 2016/7/17 15:06.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extensible(singleton = false)
public interface LoadBalancer {

    /**
     * 初始化负载均衡器
     *
     * @param consumerConfig 客户端配置
     */
    public void init(ConsumerConfig consumerConfig);

    /**
     * 选择服务
     *
     * @param request   本次调用（可以得到类名，方法名，方法参数，参数值等）
     * @param providers providers（<b>当前可用</b>的服务Provider列表）
     * @return 选择其中一个Provider
     */
    public Provider select(RpcRequest request, List<Provider> providers);
}
