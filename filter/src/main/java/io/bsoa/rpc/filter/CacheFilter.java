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
package io.bsoa.rpc.filter;

import io.bsoa.rpc.base.Cache;
import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.config.AbstractInterfaceConfig;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * 结果缓存过滤器,服务端和客户端均可使用
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "cache", order = -120)
@AutoActive(providerSide = true, consumerSide = true)
public class CacheFilter implements Filter {

    @Override
    public boolean needToLoad(FilterInvoker invoker) {
        AbstractInterfaceConfig config = invoker.getConfig();
        // 客户端开启了cache才启动
        boolean need = config.hasCache();
        if (need && config.getCacheRef() == null) {
            throw ExceptionUtils.buildRuntime(21205,
                    "cacheRef", null, "Must assign cacheRef when cache=\"true\"");
        }
        return need;
    }

    @Override
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest request) {
        // 命中缓存，直接就不走rpc调用
        AbstractInterfaceConfig config = invoker.getConfig();
        String methodName = request.getMethodName();
        boolean iscache = invoker.getBooleanMethodParam(methodName, BsoaConstants.CONFIG_KEY_CACHE, config.isCache());
        // 该方法对应有结果缓存
        if (iscache) {
            Cache cache = config.getCacheRef();
            String interfaceId = request.getInterfaceName();
            Object key = cache.buildKey(interfaceId, methodName, request.getArgs());
            if (key != null) { // 有key
                Object result = cache.get(key);
                if (result != null) {
                    // 命中缓存，直接就不走rpc调用
                    RpcResponse response = MessageBuilder.buildRpcResponse(request);
                    response.setReturnData(result);
                    return response;
                } else {
                    // 未命中发起远程调用
                    RpcResponse response = invoker.invoke(request);
                    if (!response.hasError()) {
                        // 调用成功 保存结果
                        cache.put(key, response.getReturnData());
                    }
                    return response;
                }
            }
        }
        // 该方法未开启缓存
        return invoker.invoke(request);
    }
}