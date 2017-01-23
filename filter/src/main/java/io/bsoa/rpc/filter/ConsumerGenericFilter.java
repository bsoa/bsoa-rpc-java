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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.listener.ResponseListener;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * 调用端的泛化调用过滤器<br>
 * 将泛化调用拼成普通调用，注意有可能参数值和参数类型不匹配，要服务端处理<br>
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "consumerGeneric", order = -190)
@AutoActive(consumerSide = true)
public class ConsumerGenericFilter implements Filter {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ConsumerGenericFilter.class);

    @Override
    public boolean needToLoad(FilterInvoker invoker) {
        ConsumerConfig config = (ConsumerConfig) invoker.getConfig();
        // 客户端是泛化调用才看起
        return config.isGeneric();
    }

    @Override
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest request) {

        String methodName = request.getMethodName();

        Object[] args = request.getArgs();
        // generic 调用 consumer不处理，服务端做转换
        //if (GenericService.class.getCanonicalName().equals(invocation.getClazzName())) {
        if ("$invoke".equals(methodName) && args.length == 3) {
            request.setMethodName(StringUtils.toString(args[0]));
            request.addAttachment(BsoaConstants.CONFIG_KEY_GENERIC, true);
        } else if ("$asyncInvoke".equals(methodName) && args.length == 4) {
            request.setMethodName(StringUtils.toString(args[0]));
            request.addAttachment(BsoaConstants.CONFIG_KEY_GENERIC, true);
            request.addAttachment(BsoaConstants.INTERNAL_KEY_ASYNC, true);

            ResponseListener responseListener = (ResponseListener) args[3];
            // 干掉最后一个参数
            String[] newArgTypes = new String[3];
            Object[] newArgs = new Object[3];
            System.arraycopy(request.getArgsType(), 0, newArgTypes, 0, 3);
            System.arraycopy(args, 0, newArgs, 0, 3);
            request.setArgsType(newArgTypes);
            request.setArgs(newArgs);
            // 删掉最后一个参数
            if (responseListener != null) {
                request.addAttachment(BsoaConstants.INTERNAL_KEY_ONRETURN, responseListener);
            }
        }

        RpcResponse response = invoker.invoke(request);
        // 返回的如果是自定义对象 则返回的是Map
        return response;
    }
}
