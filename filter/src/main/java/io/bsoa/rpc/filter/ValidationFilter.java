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
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.config.AbstractInterfaceConfig;
import io.bsoa.rpc.context.RpcContext;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.filter.validation.Validator;
import io.bsoa.rpc.filter.validation.ValidatorFactory;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * 参数校验过滤器,支持接口级或者方法级配置，服务端和客户端都可以配置，需要引入第三方jar包
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "validation", order = -30)
@AutoActive(providerSide = true, consumerSide = true)
public class ValidationFilter implements Filter {

    @Override
    public boolean needToLoad(FilterInvoker invoker) {
        AbstractInterfaceConfig config = invoker.getConfig();
        // 客户端开启了alidation才启动
        return config.hasValidation();
    }

    @Override
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest request) {
        String methodName = request.getMethodName();
        // 该方法开启校验
        if (invoker.getBooleanMethodParam(methodName, BsoaConstants.CONFIG_KEY_VALIDATION, false)) {
            // 自定义参数<jsf:param>是否有自定义jsr303实现
            String customImpl = invoker.getStringMethodParam(methodName, BsoaConstants.HIDE_KEY_PREFIX + "customImpl", null);
            String className = request.getInterfaceName();
            Validator validator = ValidatorFactory.getValidator(className, customImpl);
            try {
                validator.validate(methodName, request.getArgsType(), request.getArgs());
            } catch (Exception e) { // 校验出现异常
                RpcResponse response = MessageBuilder.buildRpcResponse(request);
                BsoaRpcException re;
                if (RpcContext.getContext().isProviderSide()) { // 无法直接序列化异常，只能转为字符串然后包装为RpcException
                    re = new BsoaRpcException(22222, "[JSF-22209]validate is not passed, cause by: " + ExceptionUtils.toString(e));
                } else {
                    re = new BsoaRpcException(22222, "[JSF-22210]validate is not passed, cause by: " + e.getMessage(), e);
                }
                response.setException(re);
                return response;
            }
        }
        return invoker.invoke(request);
    }
}