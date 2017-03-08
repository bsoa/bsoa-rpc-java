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
package io.bsoa.rpc.filter;

import io.bsoa.rpc.ext.Extensible;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * <p>过滤器接口</p>
 * <p>
 * Created by zhangg on 16-6-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
@Extensible(singleton = false)
public interface Filter {

    /**
     * 是否自动加载
     *
     * @param invoker 调用器
     * @return 是否加载本过滤器
     */
    public default boolean needToLoad(FilterInvoker invoker) {
        return true;
    }

    /**
     * 过滤执行
     * <pre><code>
     *  doBeforeInvoke(); // 调用前逻辑，甚至可以new一个Response进行提取返回
     *  RpcResponse response = invoker.invoke(request); // 调用链往后执行
     *  doAfterInvoke(); // 调用后的逻辑
     * </code></pre>
     *
     * @param invoker    调用器
     * @param rpcRequest 请求
     * @return RpcResponse 响应
     */
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest rpcRequest);
}
