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

import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static io.bsoa.rpc.common.BsoaConfigs.getIntValue;
import static io.bsoa.rpc.common.BsoaOptions.PROVIDER_INVOKE_TIMEOUT;

/**
 * 服务端用，记录超时用
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "providerTimeout", order = -130)
@AutoActive(providerSide = true)
public class ProviderTimeoutFilter implements Filter {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ProviderTimeoutFilter.class);

    protected int defaultTimeout = getIntValue(PROVIDER_INVOKE_TIMEOUT);

    @Override
    public boolean needToLoad(FilterInvoker invoker) {
        return invoker.getConfig().hasTimeout();
    }

    @Override
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest request) {
        long start = BsoaContext.now();
        RpcResponse response = invoker.invoke(request);
        long elapsed = BsoaContext.now() - start;
        int providerTimeout = invoker.getIntMethodParam(request.getMethodName(),
                BsoaConstants.CONFIG_KEY_TIMEOUT, defaultTimeout);
        if (elapsed > providerTimeout) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("[22204]Provider invoke method [" + request.getInterfaceName() + "."
                        + request.getMethodName() + "] timeout. "
                        + "The arguments is: " + Arrays.toString(request.getArgs())
                        + ", timeout is " + providerTimeout + " ms, invoke elapsed " + elapsed + " ms.");
            }
        }
        request.addAttachment(BsoaConstants.INTERNAL_KEY_ELAPSED, (int) elapsed);
        return response;
    }
}