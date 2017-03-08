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
package io.bsoa.rpc.client;

import io.bsoa.rpc.bootstrap.ConsumerBootstrap;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.transport.ClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangg on 2017/01/07 15:32.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("failover")
public class FailoverClient extends AbstractClient {

    /**
     * slf4j logger for this class
     */
    private final static Logger LOGGER = LoggerFactory
            .getLogger(FailoverClient.class);

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务端消费者启动器
     */
    public FailoverClient(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
    }

    @Override
    public RpcResponse doSendMsg(RpcRequest msg) {
        String methodName = msg.getMethodName();
        int retries = consumerConfig.getMethodRetries(methodName);
        int time = 0;
        Throwable throwable = null;// 异常日志
        List<ProviderInfo> invokedProviderInfos = new ArrayList<ProviderInfo>(retries + 1);
        do {
            ClientTransport connection = super.select(msg, invokedProviderInfos);
            try {
                RpcResponse result = super.sendMsg0(connection, msg);
                if (result != null) {
                    if (throwable != null) {
                        LOGGER.warn("[22100]Although success by retry, last exception is: {}", throwable.getMessage());
                    }
                    return result;
                } else {
                    throwable = new BsoaRpcException("[22101]Failed to call " + msg.getInterfaceName() + "." + methodName
                            + " on remote server " + connection.getConfig().getProviderInfo() + ", return null");
                }
            } catch (BsoaRpcException e) { // rpc异常重试
                throwable = e;
                time++;
            } catch (Exception e) { // 其它异常不重试
                throw new BsoaRpcException(22222, "Failed to call " + msg.getInterfaceName() + "." + methodName
                        + " on remote server: " + connection.getConfig().getProviderInfo() + ", cause by unknown exception: "
                        + e.getClass().getName() + ", message is: " + e.getMessage(), e);
            }
            invokedProviderInfos.add(connection.getConfig().getProviderInfo());
        } while (time <= retries);

        if (retries == 0) {
            throw new BsoaRpcException(22222, "Failed to call " + msg.getInterfaceName() + "." + methodName
                    + " on remote server: " + invokedProviderInfos + ", cause by: "
                    + throwable.getClass().getName() + ", message is: " + throwable.getMessage(), throwable);
        } else {
            throw new BsoaRpcException(22222, "Failed to call " + msg.getInterfaceName() + "." + methodName
                    + " on remote server after retry " + (retries + 1) + " times: "
                    + invokedProviderInfos + ", last exception is cause by:"
                    + throwable.getClass().getName() + ", message is: " + throwable.getMessage(), throwable);
        }
    }
}
