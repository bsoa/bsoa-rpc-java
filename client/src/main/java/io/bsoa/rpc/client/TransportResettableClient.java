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
import java.util.concurrent.locks.ReentrantLock;

import io.bsoa.rpc.bootstrap.ConsumerBootstrap;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * <p>可以运行时替换transport的client，继承于failover,切换中无法调用</p>
 * <p>
 * Created by zhangg on 2017/1/19 22:39. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("resettable")
public class TransportResettableClient extends FailoverClient {

    private volatile ReentrantLock lock = new ReentrantLock();

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务端消费者启动器
     */
    public TransportResettableClient(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
    }

    @Override
    public RpcResponse doSendMsg(RpcRequest msg) {
        if (lock.isLocked()) {
            throw new BsoaRpcException(22222, "[JSF-22105]Transport resettable client is resetting transports...");
        }
        return super.doSendMsg(msg);
    }

    /**
     * 重置客户端连接
     *
     * @param providerInfos 新的客户端列表
     */
    public void resetTransport(List<ProviderInfo> providerInfos) {
        lock.lock();
        try {
            super.closeTransports(); // 关闭旧的
            super.addProvider(providerInfos); // 连接新的
        } finally {
            lock.unlock();
        }
    }

    /**
     * 重置客户端连接
     *
     * @param url 新的地址
     */
    public void resetTransport(String url) {
        List<ProviderInfo> tmpProviderInfoList = new ArrayList<ProviderInfo>();
        if (StringUtils.isNotEmpty(url)) {
            String originalProtocol = consumerConfig.getProtocol();
            String[] providers = StringUtils.splitWithCommaOrSemicolon(url);
            for (int i = 0; i < providers.length; i++) {
                ProviderInfo providerInfo = ProviderInfo.valueOf(providers[i]);
                if (!providerInfo.getProtocolType().equals(consumerConfig.getProtocol())) {
                    throw ExceptionUtils.buildRuntime(21308, "consumer.url", url,
                            "there is a mismatch protocol between url[" + providerInfo.getProtocolType()
                                    + "] and consumer[" + originalProtocol + "]"
                    );
                }
                tmpProviderInfoList.add(providerInfo);
            }
        }
        resetTransport(tmpProviderInfoList);
    }
}