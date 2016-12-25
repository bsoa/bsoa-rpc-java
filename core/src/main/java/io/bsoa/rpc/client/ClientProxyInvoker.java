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

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.base.Invoker;
import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.filter.ConsumerInvokeFilter;
import io.bsoa.rpc.filter.FilterChain;
import io.bsoa.rpc.listener.ResponseListener;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 *
 *
 * Created by zhangg on 2016/7/16 01:11.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ClientProxyInvoker implements Invoker {
    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ClientProxyInvoker.class);

    /**
     * 对应的客户端信息
     */
    private final ConsumerConfig consumerConfig;

    /**
     *
     */
    private Client client;

    /**
     * 过滤器执行链
     */
    private FilterChain filterChain;

    /**
     * 构造执行链
     *
     * @param consumerConfig
     *         调用端配置
     */
    public ClientProxyInvoker(ConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
        // 构建客户端
        this.client = consumerConfig.getClient();
        // 构造执行链,最底层是调用过滤器
        this.filterChain = FilterChain.buildConsumerChain(this.consumerConfig,
                new ConsumerInvokeFilter(consumerConfig, client));
    }

    /**
     * proxy拦截的调用
     *
     * @param request
     *         请求消息
     * @return 调用结果
     */
    @Override
    public RpcResponse invoke(RpcRequest request) {
        String methodName = request.getMethodName();

        request.setGroup(consumerConfig.getAlias());
        request.setInterfaceName(consumerConfig.getInterfaceId());

        // 是否缓存，减少valueof操作？ TODO
//        request.setProtocolType(ProtocolType.valueOf(consumerConfig.getProtocol()).value());
//        request.setSerializationType(SerializationType.valueOf(consumerConfig.getSerialization()).value());
//        String compress = (String) consumerConfig.getMethodConfigValue(methodName,
//                BsoaConstants.CONFIG_KEY_COMPRESS, consumerConfig.getCompress());
//        if (compress != null) {
//            request.setCompressType(CompressType.valueOf(compress).value());
//        }
//        request.addHeadKey(HeadKey.timeout, consumerConfig.getMethodTimeout(methodName));

        // 将接口的<jsf:param />的配置复制到invocation
        Map params = consumerConfig.getParameters();
        if (params != null) {
            request.addAttachments(params);
        }
        // 将方法的<jsf:param />的配置复制到invocation
        params = (Map) consumerConfig.getMethodConfigValue(methodName, BsoaConstants.CONFIG_KEY_PARAMS);
        if (params != null) {
            request.addAttachments(params);
        }

        // 调用
        RpcResponse rpcResponse = filterChain.invoke(request);

        // 通知ResponseListener
        // 异步的改到msgfuture处返回才是真正的异步
        if (!consumerConfig.getMethodAsync(methodName)) {
            notifyResponseListener(methodName, rpcResponse);
        }

        // 得到结果
        return rpcResponse;
    }

    /**
     * 通知响应监听器
     *
     * @param rpcResponse
     *         响应结果
//     * @see AsyncResultListener#operationComplete(com.jd.jsf.gd.client.MsgFuture)
     */
    private void notifyResponseListener(String methodName, RpcResponse rpcResponse){
        // 返回结果增加事件监听
        List<ResponseListener> onreturn = consumerConfig.getMethodOnreturn(methodName);
        if (onreturn != null && !onreturn.isEmpty()) {
            if (rpcResponse.isError()) {
                Throwable responseException = rpcResponse.getException();
                for (ResponseListener responseListener : onreturn) {
                    try {
                        responseListener.catchException(responseException);
                    } catch (Exception e) {
                        LOGGER.warn("notify response listener error", e);
                    }
                }
            } else {
                Object result = rpcResponse.getReturnData();
                for (ResponseListener responseListener : onreturn) {
                    try {
                        responseListener.handleResult(result);
                    } catch (Exception e) {
                        LOGGER.warn("notify response listener error", e);
                    }
                }
            }
        }
    }

    /**
     * @return the consumerConfig
     */
    public ConsumerConfig<?> getConsumerConfig() {
        return consumerConfig;
    }

    /**
     * 切换客户端
     *
     * @param newClient
     *         新客户端
     * @return 旧客户端
     */
    public Client setClient(Client newClient) {
        // 构造执行链,最底层是调用过滤器
        FilterChain newChain = FilterChain.buildConsumerChain(this.consumerConfig,
                new ConsumerInvokeFilter(consumerConfig, newClient));
        // 开始切换
        Client old = this.client;
        this.client = newClient;
        this.filterChain = newChain;
        return old;
    }
}
