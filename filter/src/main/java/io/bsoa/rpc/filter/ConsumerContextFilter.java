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

import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.context.RpcContext;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * 客户端RpcContext处理过滤器, 将用户代码里设置的隐式传参复制到调用过程中
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "consumerContext", order = -180)
@AutoActive(consumerSide = true)
public class ConsumerContextFilter implements Filter {
    @Override
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest request) {
        RpcContext context = RpcContext.getContext();
        try {
            // 先清除该线程上次调用的缓存，防止数据污染
            context.setLocalAddress(null).setRemoteAddress(null).setTags(null).setFuture(null).setProviderSide(false);
            context.removeAttachment(BsoaConstants.CONFIG_KEY_GENERIC); // 旧版本发的generic特殊处理下
            // 将rpcContext的值复制到invocation
            request.setAttachments(context.getAttachments());

            return invoker.invoke(request);
        } finally {
            //InetSocketAddress address = RpcContext.getContext().getRemoteAddress();
            // 是否返回调用的远程ip？？
            //RpcContext.getContext().setRemoteAddress(address);

            if (context.getFuture() != null) {
                // 异步调用 删除缓存内key-value数据
                context.setLocalAddress(null).setRemoteAddress(null).setTags(null).clearAttachments();
            } else {
                // 其它调用 删除ThreadLocal对象
                context.setLocalAddress(null).setRemoteAddress(null).setTags(null).setFuture(null).clearAttachments();
                // RpcContext.removeContext();
            }
        }
    }
}