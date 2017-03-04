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

import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.common.utils.ReflectUtils;
import io.bsoa.rpc.config.AbstractInterfaceConfig;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Mock过滤器,服务端和客户端通用，就是如果设置mock=true就走本地mock实现
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "mock", order = -70)
@AutoActive(providerSide = true, consumerSide = true)
public class MockFilter implements Filter {

    @Override
    public boolean needToLoad(FilterInvoker invoker) {
        AbstractInterfaceConfig config = invoker.getConfig();
        // 客户端开启了cache才启动
        boolean need = config.isMock();
        if (need && config.getMockRef() == null) {
            throw ExceptionUtils.buildRuntime(21205,
                    "mockRef", null, "Must assign mockRef when mock=\"true\"");
        }
        return need;
    }

    @Override
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest request) {
        AbstractInterfaceConfig config = invoker.getConfig();
        String interfaceId = request.getInterfaceName();
        String methodName = request.getMethodName();
        String tags = request.getTags();
        // 从哪里取，config里动态取
        boolean isMock = invoker.getBooleanMethodParam(methodName, BsoaConstants.CONFIG_KEY_MOCK,
                config.isMock());
        if (isMock) {
            // mock 调用
            RpcResponse responseMessage = MessageBuilder.buildRpcResponse(request);
            try {
                Method method = ReflectUtils.getMethod(interfaceId, methodName, request.getArgsType());
                Object result = method.invoke(config.getMockRef(), request.getArgs()); // 调用本地Mock实现
                responseMessage.setReturnData(result);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
                responseMessage.setException(e);
            } catch (InvocationTargetException e) {
                responseMessage.setException(e.getCause());
            }
            return responseMessage;
        } else {
            // 远程调用
            return invoker.invoke(request);
        }
    }
}