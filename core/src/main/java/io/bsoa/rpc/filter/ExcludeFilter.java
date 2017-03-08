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

import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * Created by zhangg on 16-6-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class ExcludeFilter implements Filter {

    /**
     * 要排除的过滤器 -*和 -default表示不加载默认过滤器
     */
    private final String excludeFilterName;

    public ExcludeFilter(String excludeFilterName) {
        this.excludeFilterName = excludeFilterName;
    }

    public RpcResponse invoke(RpcRequest request) {
        throw new UnsupportedOperationException();
    }

    public String getExcludeFilterName() {
        return excludeFilterName;
    }

    @Override
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest request) {
        throw new UnsupportedOperationException();
    }
}