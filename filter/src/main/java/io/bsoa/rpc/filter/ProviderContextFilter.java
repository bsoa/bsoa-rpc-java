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
package io.bsoa.rpc.filter;

import java.net.InetSocketAddress;
import java.util.Map;

import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.context.RpcContext;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * 服务端端RpcContext处理过滤器,将调用里的一些特殊处理放入到隐式传参中
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "providerContext", order = -190)
@AutoActive(providerSide = true)
public class ProviderContextFilter implements Filter {

    @Override
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest request) {
        try {
            RpcContext context = RpcContext.getContext();
            context.setProviderSide(true);
            // 自定义参数从invocation里复制到RpcContext
            Map<String, Object> attachments = request.getAttachments();
            context.setAttachments(attachments);
            context.removeAttachment(BsoaConstants.CONFIG_KEY_GENERIC); // 旧版本发的generic特殊处理下
            context.setSession((Map<String, Object>) attachments.get(BsoaConstants.HIDDEN_KEY_SESSION));

            if (context.getRemoteAddress() == null) { // 未提前设置过
                // 设置远程客户端的服务地址
                InetSocketAddress address = (InetSocketAddress) attachments.get(BsoaConstants.INTERNAL_KEY_REMOTE);
                if (address != null) {
                    context.setRemoteAddress(address);
                }
            }
            if (context.getLocalAddress() == null) { // 未提前设置过
                // 设置本地服务端地址
                InetSocketAddress address = (InetSocketAddress) attachments.get(BsoaConstants.INTERNAL_KEY_LOCAL);
                if (address != null) {
                    context.setLocalAddress(address);
                }
            }
            // 是否强制关闭监控，默认开启
            String monitor = (String) invoker.getConfigContext().get(BsoaConstants.HIDDEN_KEY_MONITOR);
            if (CommonUtils.isFalse(monitor)) { // 主动关闭
                request.addAttachment(BsoaConstants.INTERNAL_KEY_MONITOR, monitor);
            }
            context.setTags(request.getTags());

            return invoker.invoke(request);
        } finally {
            RpcContext.removeContext();
        }
    }
}