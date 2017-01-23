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

import java.lang.reflect.Method;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.CodecUtils;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.common.utils.PojoUtils;
import io.bsoa.rpc.common.utils.ReflectUtils;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * 服务端的泛化调用过滤器, 如果是generic请求，那么可能传递的参数值和参数类型不匹配 需要转换
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "providerGeneric", order = -180)
@AutoActive(providerSide = true)
public class ProviderGenericFilter implements Filter {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ProviderGenericFilter.class);

    @Override
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest request) {

        Boolean generic = (Boolean) request.getAttachment(BsoaConstants.CONFIG_KEY_GENERIC);
        // 如果是generic请求，
        if (CommonUtils.isTrue(generic)) {
            // 转换为正常请求
            genericToNormal(request);
            try {
                Method method = ReflectUtils.getMethod(request.getInterfaceName(),
                        request.getMethodName(), request.getArgsType());

                Class[] paramTypes = request.getArgClasses();
                Object[] paramValues = request.getArgs();
                paramTypes = paramTypes == null ? CodecUtils.EMPTY_CLASS_ARRAY : paramTypes; // 如果客户端写的是null
                paramValues = paramValues == null ? CodecUtils.EMPTY_OBJECT_ARRAY : paramValues; // 如果客户端写的是null
                // 参数值类型 和 参数类型不匹配 解析数据 TODO
                Object[] newParamValues = PojoUtils.realize(paramValues, paramTypes, method.getGenericParameterTypes());
                request.setArgs(newParamValues);

                /*for (int i = 0; i < paramTypes.length; i++) {
                    Class paramType  = paramTypes[i];
                    Object paramValue = paramValues[i];
                    if (paramType.isAssignableFrom(paramValue.getClass())) {
                        // 可以转换
                    } else {
                        // 参数值类型 和 参数类型不匹配
                        if(paramValue instanceof Map){
                            Map paramMap = (Map) paramValue;
                            // map转对象
                            LOGGER.debug("convert pojo map to pojo");
                            Object newParamValue = PojoUtils.realize(paramMap, paramType, paramType);
                            paramValues[i] = newParamValue; // 修改参数值
                        } else {
                            Exception e = new RpcException("can't parse generic param : "
                                    + paramValue.getClass().getName() + " which need " + paramType.getName());
                            RpcResponse response = MessageBuilder.buildResponse(request);
                            response.setException(e);
                        }
                    }
                }*/
            } catch (Exception e) {
                LOGGER.error("[JSF-22202]Failed to realize generic invocation of " + request.getInterfaceName()
                        + "." + request.getMethodName() + " from " + NetUtils.toAddressString((InetSocketAddress)
                        request.getAttachment(BsoaConstants.INTERNAL_KEY_REMOTE)) + ".", e);
                RpcResponse response = MessageBuilder.buildRpcResponse(request);
                response.setException(e);
                return response;
            }

            // 解析完毕后，将invocation从generic换成正常invocatio，往下调用
            RpcResponse response = invoker.invoke(request);

            if (response.hasError()) { // 有异常
                Throwable exception = response.getException();
                if (exception instanceof BsoaRpcException
                        || exception instanceof BsoaRuntimeException) {
                    return response; // rpc异常 直接返回
                } else { // 业务异常（可能业务异常类客户端没有，返回文本形式）
                    response.setException(new BsoaRpcException(22222, ExceptionUtils.toString(exception)));
                }
            } else { // 无异常
                Object result = response.getReturnData();
                result = PojoUtils.generalize(result);
                response.setReturnData(result);
            }
            return response;
        } else {
            // 正常请求
            return invoker.invoke(request);
        }
    }

    private RpcRequest genericToNormal(RpcRequest request) {
        Object[] genericArgs = request.getArgs();
        // 转为正常的请求，发给服务端
        request.setArgs((Object[]) genericArgs[2]);
        request.setArgsType((String[]) genericArgs[1]);
        request.setMethodName((String) genericArgs[0]);

        return request;
    }
}