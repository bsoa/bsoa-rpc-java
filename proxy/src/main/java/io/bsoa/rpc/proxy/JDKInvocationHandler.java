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
package io.bsoa.rpc.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import io.bsoa.rpc.Invoker;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * JDK代理处理器，拦截请求变为invocation进行调用
 *
 * Created by zhanggeng on 16-6-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class JDKInvocationHandler implements InvocationHandler {

    private Invoker proxyInvoker;

    public JDKInvocationHandler(Invoker proxyInvoker) {
        this.proxyInvoker = proxyInvoker;
    }

    /**
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
     * java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] paramValues)
            throws Throwable {
        String methodName = method.getName();
        Class[] paramTypes = method.getParameterTypes();
        if ("toString".equals(methodName) && paramTypes.length == 0) {
            return proxyInvoker.toString();
        } else if ("hashCode".equals(methodName) && paramTypes.length == 0) {
            return proxyInvoker.hashCode();
        } else if ("equals".equals(methodName) && paramTypes.length == 1) {
            return proxyInvoker.equals(paramValues[0]);
        }
        RpcRequest requestMessage = MessageBuilder.buildRequest(method.getDeclaringClass(),
                methodName, paramTypes, paramValues);
        RpcResponse rpcResponseMessage = proxyInvoker.invoke(requestMessage);
        if(rpcResponseMessage.isError()){
            throw rpcResponseMessage.getException();
        }
        return rpcResponseMessage.getReturnData();
    }
}
