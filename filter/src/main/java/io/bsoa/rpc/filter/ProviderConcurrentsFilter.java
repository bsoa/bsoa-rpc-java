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
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.context.RpcStatus;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * 服务端并发限制器,方法级别的并发限制，超过报异常
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "providerConcurrents", order = -20)
public class ProviderConcurrentsFilter implements Filter {

    @Override
    public boolean needToLoad(FilterInvoker invoker) {
        ConsumerConfig config = (ConsumerConfig) invoker.getConfig();
        // 客户端开启了concurrents才启动
        return config.hasConcurrents();
    }

    @Override
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest request) {
        ProviderConfig config = (ProviderConfig) invoker.getConfig();
        String interfaceId = request.getInterfaceName();
        String methodName = request.getMethodName();
        int concurrents = invoker.getIntMethodParam(methodName, BsoaConstants.CONFIG_KEY_CONCURRENTS, config.getConcurrents());
        if (concurrents > 0) {
            // 判断是否超过并发数大小
            RpcStatus count = RpcStatus.getMethodStatus(config, methodName);
            if (count.getActive() >= concurrents) {
                throw new BsoaRpcException("[JSF-22208]Failed to invoke method " + interfaceId + "." + methodName
                        + ", The service using threads greater than: " + concurrents + ". Change it by "
                        + "<jsf:provider concurrents=\"\"/> or <jsf:method concurrents=\"\"/> on provider");
            }
        }
        boolean isException = false;
        long begin = BsoaContext.now();
        RpcStatus.beginCount(config, methodName);
        try {
            RpcResponse response = invoker.invoke(request);
            isException = response.hasError();
            return response;
        } catch (Throwable t) {
            isException = true;
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new BsoaRpcException(22222, "unexpected exception", t);
            }
        } finally {
            RpcStatus.endCount(config, methodName, BsoaContext.now() - begin, !isException);
        }
    }
}