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

import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * 方法调用验证器,检查服务端接口发布了哪些方法，放在providerGeneric之后
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "providerMethodCheck", order = -140)
@AutoActive(providerSide = true)
public class ProviderMethodCheckFilter implements Filter {

    @Override
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest request) {
        ProviderConfig config = (ProviderConfig) invoker.getConfig();
        String methodName = request.getMethodName();

        // 判断服务下方法的黑白名单
        Boolean include = (Boolean) config.getMethodsLimit().get(methodName);
        RpcResponse response;
        if (include == null || !include) { // 服务端未暴露这个方法
            response = MessageBuilder.buildRpcResponse(request);
            BsoaRpcException rpcException;
            if ("$invoke".equals(methodName) && request.getArgs().length == 3) {
                // 可能是dubbo2.3.2发来的请求，没有带上"generic"标记
                rpcException = new BsoaRpcException(22222, "[22203]Provider of " + request.getInterfaceName()
                        + " didn't export method named \"" + methodName + "\", maybe you are using"
                        + " SAF(<1.0.9) for generic invoke to JSF, please upgrade to JSF or SAF(>=1.0.9).");
            } else {
                rpcException = new BsoaRpcException(22222, "[22203]Provider of " + request.getInterfaceName()
                        + " didn't export method named \"" + methodName + "\", maybe provider"
                        + " has been exclude it, or don't have this method!");
            }
            response.setException(rpcException);
        } else {
            // 调用
            response = invoker.invoke(request);
        }
        // 得到结果
        return response;
    }

}
