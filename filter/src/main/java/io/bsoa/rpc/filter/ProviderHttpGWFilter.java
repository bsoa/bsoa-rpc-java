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

import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.HeadKey;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 *  http网关调用过滤器
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "providerHttpGW", order = -170)
@AutoActive(providerSide = true)
public class ProviderHttpGWFilter<T> implements Filter {

    @Override
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest request) {
        Byte src = (Byte) request.getHeadKey(HeadKey.srcLanguage);
        if (src != null && src == 2) { // fromGateway
            // 如果是网关请求
            RpcResponse response = invoker.invoke(request);

            if (response.hasError()) { // 有异常
                Throwable exception = response.getException();
                if (exception instanceof BsoaRpcException
                        || exception instanceof BsoaRuntimeException) {
                    return response; // SAF定义的异常或者rpc异常 直接返回
                } else { // 业务异常（可能业务异常类客户端没有，返回文本形式）
                    response.setException(new BsoaRpcException(exception.getMessage()));
                }
            }
            return response;
        } else {
            return invoker.invoke(request);
        }
    }

}
