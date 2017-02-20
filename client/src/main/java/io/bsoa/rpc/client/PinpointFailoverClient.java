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
package io.bsoa.rpc.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.bootstrap.ConsumerBootstrap;
import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.transport.ClientTransport;

/**
 * <p>客户端可以指定调用地址，继承于failover</p>
 * <p>
 * Created by zhangg on 2017/1/19 22:39. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 * @see io.bsoa.rpc.context.RpcContext
 * @see io.bsoa.rpc.common.BsoaConstants#HIDDEN_KEY_PINPOINT
 */
@Extension("pinpoint")
public class PinpointFailoverClient extends FailoverClient {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(PinpointFailoverClient.class);

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务端消费者启动器
     */
    public PinpointFailoverClient(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
    }

    @Override
    public RpcResponse doSendMsg(RpcRequest request) {
        String methodName = request.getMethodName();
        int retries = consumerConfig.getMethodRetries(methodName);
        int time = 0;
        Throwable throwable = null;// 异常日志
        List<ProviderInfo> invokedProviderInfos = new ArrayList<>(retries + 1);
        do {
            String targetIP = (String) request.getAttachment(BsoaConstants.HIDDEN_KEY_PINPOINT);
            ClientTransport connection;
            if (StringUtils.isNotBlank(targetIP)) { // 指定了调用地址
                ProviderInfo providerInfo = selectProvider(targetIP);
                if (providerInfo == null) { // 指定的不存在
                    throw noAliveProvider(consumerConfig.buildKey(), targetIP);
                }
                connection = super.selectByProvider(request, providerInfo);
                if (connection == null) { // 指定的不存在或已死
                    // 抛出异常
                    throw noAliveProvider(consumerConfig.buildKey(), targetIP);
                }
            } else { //未指定调用地址
                connection = super.select(request, invokedProviderInfos);
            }
            try {
                RpcResponse result = super.sendMsg0(connection, request);
                if (result != null) {
                    if (throwable != null) {
                        LOGGER.warn("[22100]Although success by retry, last exception is: {}", throwable.getMessage());
                    }
                    return result;
                } else {
                    throwable = new BsoaRpcException(22222, "[22101]Failed to call " + request.getInterfaceName() + "." + request.getMethodName()
                            + " on remote server " + connection.getConfig().getProviderInfo() + ", return null");
                }
            } catch (BsoaRpcException e) { // rpc异常重试
                throwable = e;
                time++;
            } catch (Exception e) { // 其它异常不重试
                throw new BsoaRpcException(22222, "[22102]Failed to call " + request.getInterfaceName() + "." + request.getMethodName()
                        + " on remote server: " + connection.getConfig().getProviderInfo() + ", cause by unknown exception: "
                        + e.getClass().getName() + ", message is: " + e.getMessage(), e);
            }
            invokedProviderInfos.add(connection.getConfig().getProviderInfo());
        } while (time <= retries);

        if (retries == 0) {
            throw new BsoaRpcException(22222, "[22103]Failed to call " + request.getInterfaceName() + "." + request.getMethodName()
                    + " on remote server: " + invokedProviderInfos + ", cause by: "
                    + throwable.getClass().getName() + ", message is :" + throwable.getMessage(), throwable);
        } else {
            throw new BsoaRpcException(22222, "[22104]Failed to call " + request.getInterfaceName() + "." + request.getMethodName()
                    + " on remote server after retry " + (retries + 1) + " times: "
                    + invokedProviderInfos + ", last exception is cause by: "
                    + throwable.getClass().getName() + ", message is: " + throwable.getMessage(), throwable);
        }
    }

    /**
     * The Provider map.
     */
    private Map<String, ProviderInfo> providerMap = new ConcurrentHashMap<>();

    /**
     * Select provider.
     *
     * @param serverIP the serverIP
     * @return the provider
     */
    private ProviderInfo selectProvider(String serverIP) {
        ProviderInfo p = providerMap.get(serverIP);
        if (p == null) {
            ProviderInfo p1 = ProviderInfo.valueOf(serverIP);
            for (ProviderInfo providerInfo : connectionHolder.getAvailableConnections().keySet()) {
                if (providerInfo.getIp().equals(p1.getIp())
                        && providerInfo.getProtocolType() == p1.getProtocolType()
                        && providerInfo.getPort() == p1.getPort()) {
                    // 相等，就是你了
                    p = providerInfo;
                    providerMap.put(serverIP, p);
                    return p;
                }
            }
        }
        return p;
    }

    /**
     * Destroy void.
     */
    @Override
    public void destroy() {
        providerMap.clear();
        super.destroy();
    }
}
