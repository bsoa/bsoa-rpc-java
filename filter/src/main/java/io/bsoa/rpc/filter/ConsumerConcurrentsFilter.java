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
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.context.RpcStatus;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * 调用端并发限制器，按接口和方法进行限制，超过限制排队等待执行，直到超时
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "consumerConcurrents", order = -20)
@AutoActive(consumerSide = true)
public class ConsumerConcurrentsFilter implements Filter {

    @Override
    public boolean needToLoad(FilterInvoker invoker) {
        ConsumerConfig config = (ConsumerConfig) invoker.getConfig();
        // 客户端开启了concurrents才启动
        return config.hasConcurrents();
    }

    @Override
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest request) {
        ConsumerConfig config = (ConsumerConfig) invoker.getConfig();
        String interfaceId = request.getInterfaceName();
        String methodName = request.getMethodName();
        int concurrents = invoker.getIntMethodParam(methodName, BsoaConstants.CONFIG_KEY_CONCURRENTS,
                config.getConcurrents());
        RpcStatus counter = RpcStatus.getMethodStatus(config, methodName);
        if (concurrents > 0) { // 存在并发限制
            long timeout = invoker.getIntMethodParam(methodName, BsoaConstants.CONFIG_KEY_TIMEOUT,
                    config.getTimeout());
            long start = BsoaContext.now();
            long remain = timeout;
            int active = counter.getActive();
            if (active >= concurrents) {
                synchronized (counter) {
                    while ((active = counter.getActive()) >= concurrents) {
                        try {
                            counter.wait(remain); // 等待执行
                        } catch (InterruptedException e) {
                        }
                        long elapsed = BsoaContext.now() - start;
                        remain = timeout - elapsed;
                        if (remain <= 0) {
                            throw new BsoaRpcException(22222,
                                    "[22207]Waiting concurrent timeout in client-side when invoke"
                                            + interfaceId + "." + methodName + ", elapsed: " + elapsed
                                            + ", timeout: " + timeout + ". concurrent invokes: " + active
                                            + ". max concurrents: " + concurrents + ". You can change it by "
                                            + "<jsf:consumer concurrents=\"\"/> or <jsf:method concurrents=\"\"/>");
                        }
                    }
                }
            }
        }
        try {
            boolean isException = false;
            long begin = BsoaContext.now();
            RpcStatus.beginCount(config, methodName);
            try {
                RpcResponse response = invoker.invoke(request);
                isException = response.hasError();
                return response;
            } catch (Throwable t) {
                isException = true;
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                } else {
                    throw new BsoaRpcException(22222, "unexpected exception", t);
                }
            } finally {
                RpcStatus.endCount(config, methodName, BsoaContext.now() - begin, !isException);
            }
        } finally {
            if (concurrents > 0) {
                synchronized (counter) {
                    counter.notify(); // 调用结束 通知等待的人
                }
            }
        }
    }
}