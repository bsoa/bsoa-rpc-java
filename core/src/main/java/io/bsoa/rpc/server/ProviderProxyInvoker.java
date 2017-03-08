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
package io.bsoa.rpc.server;

import io.bsoa.rpc.base.Invoker;
import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.filter.FilterChain;
import io.bsoa.rpc.filter.ProviderInvoker;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhangg on 2016/7/16 00:55.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ProviderProxyInvoker implements Invoker {
    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ProviderProxyInvoker.class);

    /**
     * 对应的客户端信息
     */
    private final ProviderConfig providerConfig;

    /**
     * 过滤器执行链
     */
    private final FilterChain filterChain;

    /**
     * 构造执行链
     *
     * @param providerConfig 服务端配置
     */
    public ProviderProxyInvoker(ProviderConfig providerConfig) {
        this.providerConfig = providerConfig;
        // 最底层是调用过滤器
        this.filterChain = FilterChain.buildProviderChain(providerConfig,
                new ProviderInvoker(providerConfig));
    }

    /**
     * proxy拦截的调用
     *
     * @param request 请求消息
     * @return 调用结果
     */
    @Override
    public RpcResponse invoke(RpcRequest request) {
        return filterChain.invoke(request);
    }

    /**
     * @return the providerConfig
     */
    public ProviderConfig getProviderConfig() {
        return providerConfig;
    }
}
