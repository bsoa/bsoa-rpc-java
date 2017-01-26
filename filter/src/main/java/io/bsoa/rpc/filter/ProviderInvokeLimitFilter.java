/*
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at
  <p>
  http://www.apache.org/licenses/LICENSE-2.0
  <p>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package io.bsoa.rpc.filter;

import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.filter.limiter.LimiterFactory;
import io.bsoa.rpc.filter.limiter.ProviderInvokerLimiter;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * 服务端限制流量filter,具体配置通过注册中心下发,粒度控制到接口+方法+alias.
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "providerInvokeLimit", order = -160)
//@AutoActive(providerSide = true)
public class ProviderInvokeLimitFilter implements Filter {

    @Override
    public boolean needToLoad(FilterInvoker invoker) {
        return StringUtils.isNotBlank(BsoaConfigs.getStringValue(BsoaConfigs.APP_ID));
    }

    @Override
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest request) {
        //全局的开关是否是开启状态
        if (LimiterFactory.isGlobalProviderLimitOpen()) {
            String interfaceId = request.getInterfaceName();
            String methodName = request.getMethodName();
            String tags = request.getTags();
            //获取调用方的appId
            String appId = BsoaConfigs.getStringValue(BsoaConfigs.APP_ID);;
            if (StringUtils.isEmpty(appId)) {
                appId = "";
            }
            ProviderInvokerLimiter limiter = LimiterFactory.getProviderLimiter(interfaceId, methodName, tags, appId);
            if (limiter != null && limiter.isOverLimit(interfaceId, methodName, tags, appId)) {
                RpcResponse rpcResponse = MessageBuilder.buildRpcResponse(request);
                String message = "[JSF-22211]Invocation of " + interfaceId + "." + methodName + " of app:" + appId
                        + " is over invoke limit:[" + limiter.getLimit() + "], please wait next period or add upper limit.";
                rpcResponse.setException(new BsoaRpcException(message));
                return rpcResponse;
            }
        }
        return invoker.invoke(request);
    }
}
