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

import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.config.AbstractInterfaceConfig;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * token过滤器，服务端和客户端配置一样的token才能完成调用
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "token", order = -150)
@AutoActive(providerSide = true)
public class TokenFilter implements Filter {

    @Override
    public boolean needToLoad(FilterInvoker invoker) {
        AbstractInterfaceConfig config = invoker.getConfig();
        // 客户端开启了token才启动
        return config.hasToken();
    }

    @Override
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest request) {
        String methodName = request.getMethodName();
        // providerToken在配置中
        String providerToken = invoker.getStringMethodParam(methodName, BsoaConstants.HIDDEN_KEY_TOKEN, null);
        if (providerToken != null) {
            // consumer在每次请求中
            String consumerToken = (String) request.getAttachment(BsoaConstants.HIDDEN_KEY_TOKEN);
            if (!providerToken.equals(consumerToken)) {
                BsoaRpcException exception = new BsoaRpcException("[22205]Invalid token! Invocation of "
                        + request.getInterfaceName() + "." + request.getMethodName()
                        + " from consumer " + request.getAttachment(BsoaConstants.INTERNAL_KEY_REMOTE)
                        + " to provider " + request.getAttachment(BsoaConstants.INTERNAL_KEY_LOCAL)
                        + " are forbidden by server. ");
                RpcResponse response = MessageBuilder.buildRpcResponse(request);
                response.setException(exception);
                return response;
            }
        }
        return invoker.invoke(request);
    }

}
