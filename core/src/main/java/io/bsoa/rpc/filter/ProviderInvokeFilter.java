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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.codec.CompressorFactory;
import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.ReflectUtils;
import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.context.RpcContext;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/15 23:08. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ProviderInvokeFilter<T> implements Filter {

    /**
     * slf4j logger for this class
     */
    private final static Logger LOGGER = LoggerFactory
            .getLogger(ProviderInvokeFilter.class);

    /**
     * The Provider config.
     */
    private final ProviderConfig<T> providerConfig;

    /**
     * The Ref.
     */
    private final T ref;

    /**
     * Instantiates a new Provider invoke filter.
     *
     * @param providerConfig the provider config
     */
    public ProviderInvokeFilter(ProviderConfig<T> providerConfig) {
        this.providerConfig = providerConfig;
        this.ref = providerConfig.getRef();
    }

    /**
     * Invoke response message.
     *
     * @param request the request
     * @return the response message
     */
    @Override
    public RpcResponse invoke(RpcRequest request) {

        // 将接口的<jsf:param />的配置复制到RpcContext
        RpcContext context = RpcContext.getContext();
        Map params = providerConfig.getParameters();
        if (params != null) {
            context.setAttachments(params);
        }
        // 将方法的<jsf:param />的配置复制到invocation
        String methodName = request.getMethodName();
        params = (Map) providerConfig.getMethodConfigValue(methodName, BsoaConstants.CONFIG_KEY_PARAMS);
        if (params != null) {
            context.setAttachments(params);
        }

        RpcResponse responseMessage = MessageBuilder.buildRpcResponse(request);

        // 是否启动压缩
        if (providerConfig.getCompress() != null) {
            byte b = CompressorFactory.getCodeByAlias(providerConfig.getCompress());
            responseMessage.setCompressType(b);
        }

        try {
            // 反射 真正调用业务代码
            Method method = ReflectUtils.getMethod(request.getInterfaceName(),
                    request.getMethodName(), request.getArgsType());
            Object result = method.invoke(ref, request.getArgs());

            responseMessage.setReturnData(result);
        } catch (IllegalArgumentException   // 非法参数，可能是实现类和接口类不对应
                | IllegalAccessException    //如果此 Method 对象强制执行 Java 语言访问控制，并且底层方法是不可访问的
                | NoSuchMethodException     // 如果找不到匹配的方法
                | ClassNotFoundException e  // 如果指定的类加载器无法定位该类
                ) {
            responseMessage.setException(e);
        } catch (InvocationTargetException e) { // 业务代码抛出异常
            responseMessage.setException(e.getCause());
        }

        return responseMessage;
    }


}
