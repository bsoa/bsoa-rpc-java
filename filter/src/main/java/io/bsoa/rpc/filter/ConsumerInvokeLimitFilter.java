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

import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.filter.limiter.LimiterFactory;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * 如果本app超过了调用次数限制，则不允许发起调用,
 * 限制是从注册中心以下发配置开关的方式发来的，根据接口+方法+app来打开或者关闭开关
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "consumerInvokeLimit", order = -60)
@AutoActive(consumerSide = true)
public class ConsumerInvokeLimitFilter implements Filter {

    @Override
    public boolean needToLoad(FilterInvoker invoker) {
        return StringUtils.isNotBlank(BsoaConfigs.getStringValue(BsoaConfigs.APP_ID));
    }

    @Override
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest request) {
        if (LimiterFactory.isFunctionOpen()) { // 开启了这个功能，
            String interfaceId = request.getInterfaceName();
            String methodName = request.getMethodName();
            String tags = request.getTags();
            String appId = BsoaConfigs.getStringValue(BsoaConfigs.APP_ID);
            if (LimiterFactory.isOverLimit(interfaceId, methodName, tags, appId)) {
                RpcResponse rpcResponse = MessageBuilder.buildRpcResponse(request);
                rpcResponse.setException(new BsoaRpcException(22222, "[JSF-22206]Invocation of "
                        + interfaceId + "." + methodName + " of app:" + appId
                        + " is over invoke limit, please wait next period or add upper limit."));
                return rpcResponse;
            }
        }
        return invoker.invoke(request);
    }
}