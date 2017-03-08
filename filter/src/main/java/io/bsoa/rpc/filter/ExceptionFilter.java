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

import io.bsoa.rpc.GenericService;
import io.bsoa.rpc.common.json.JSON;
import io.bsoa.rpc.common.utils.ClassLoaderUtils;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.common.utils.ReflectUtils;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.HeadKey;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * 异常过滤器<br>
 * <p/>
 * 1.如果抛出的异常方法上有声明则返回<br>
 * 2.如果是一些已知异常，则返回<br>
 * 3.未知异常，保证为RuntimeException返回<br>
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "exception", order = -200)
@AutoActive(providerSide = true, consumerSide = true)
public class ExceptionFilter implements Filter {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ExceptionFilter.class);

    @Override
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest request) {

        RpcResponse response = null;

        // 先调用
        try {
            // 调用成功，或者调用返回已经封装的response
            response = invoker.invoke(request);
        } catch (Exception e) {
            // 此时的异常，是由于过滤器没有捕获导致的
            //LOGGER.warn("Catch unchecked and undeclared exception " + e.getClass().getCanonicalName()
            //                +" when invoke" + invocation.getClazzName() + "." + invocation.getMethodName(), e);
            response = MessageBuilder.buildRpcResponse(request);
            response.setException(e);
        }

        // 解析exception
        try {
            if (!response.hasError() // 没有错误
                    || GenericService.class.getCanonicalName().equals(request.getInterfaceName())) {
                return response;
            } else {
                try {
                    Throwable exception = response.getException();

                    // 跨语言 特殊处理。
                    if (response.getHeadKey(HeadKey.srcLanguage) != null) {
                        response.addHeadKey(HeadKey.responseCode, (byte) 1); // 标记结果为错误
                        String json = JSON.toJSONString(response.getException()); // 转为字符串
                        response.setReturnData(json);
                        response.setException(null);
                        return response;
                    }

                    // 如果是checked异常，直接抛出
                    if (!(exception instanceof RuntimeException) && (exception instanceof Exception)) {
                        return response;
                    }
                    // 在方法签名上有声明，直接抛出
                    try {
                        Method method = ReflectUtils.getMethod(request.getInterfaceName(),
                                request.getMethodName(), request.getArgsType());
                        Class<?>[] exceptionClassses = method.getExceptionTypes();
                        for (Class<?> exceptionClass : exceptionClassses) {
                            if (exception.getClass().equals(exceptionClass)) {
                                return response;
                            }
                        }
                    } catch (NoSuchMethodException e) {
                        return response;
                    }

                    // 未在方法签名上定义的异常，在服务器端打印ERROR日志
//                    LOGGER.error("Got unchecked and undeclared exception which called by " + RpcContext.getContext().getRemoteHostName()
//                            + ". service: " + invocation.getClazzName() + ", method: " + invocation.getMethodName()
//                            + ", exception: " + exception.getClass().getName() + ": " + exception.getMessage(), exception);

                    // 异常类和接口类在同一jar包里，直接抛出
                    String serviceFile = ReflectUtils.getCodeBase(ClassLoaderUtils.forName(request.getInterfaceName()));
                    String exceptionFile = ReflectUtils.getCodeBase(exception.getClass());
                    if (serviceFile == null || exceptionFile == null || serviceFile.equals(exceptionFile)) {
                        return response;
                    }
                    // 是JDK自带的异常，直接抛出
                    String className = exception.getClass().getName();
                    if (className.startsWith("java.") || className.startsWith("javax.")) {
                        return response;
                    }
                    // 是BSOA本身的异常，直接抛出
                    if (exception instanceof BsoaRpcException || exception instanceof BsoaRuntimeException) {
                        return response;
                    }

                    // 否则，包装成RuntimeException抛给客户端
                    response.setException(new RuntimeException(ExceptionUtils.toString(exception)));
                } catch (Throwable e) {
//                    LOGGER.warn("Fail to ExceptionFilter when called by " + RpcContext.getContext().getRemoteHost()
//                            + ". service: " + invocation.getClazzName() + ", method: " + invocation.getMethodName()
//                            + ", exception: " + e.getClass().getName() + ": " + e.getMessage(), e);
                    return response;
                }
            }

            // 没有错误
            return response;
        } catch (RuntimeException e) {
//            LOGGER.error("Got unchecked and undeclared exception which called by " + RpcContext.getContext().getRemoteHost()
//                    + ". service: " + invocation.getClazzName() + ", method: " + invocation.getMethodName()
//                    + ", exception: " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;
        }

    }

}