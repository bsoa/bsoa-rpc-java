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
package io.bsoa.rpc.server.bsoa;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

import io.bsoa.rpc.base.Invoker;
import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.server.BusinessPool;
import io.bsoa.rpc.server.ServerHandler;
import io.bsoa.rpc.transport.AbstractChannel;
import io.bsoa.rpc.transport.ServerTransportConfig;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/22 23:05. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class BsoaServerHandler implements ServerHandler {

    /**
     * 当前handler的Invoker列表 一个接口+alias对应一个Invoker
     */
    private Map<String, Invoker> instanceMap = new ConcurrentHashMap<>();

    /**
     * Server Transport Config
     */
    private final ServerTransportConfig transportConfig;

    private ThreadPoolExecutor bizThreadPool;

    public BsoaServerHandler(ServerTransportConfig transportConfig) {
        this.transportConfig = transportConfig;
        this.bizThreadPool = BusinessPool.getBusinessPool(this.transportConfig);
    }

    public void registerProcessor(String instanceName, Invoker instance) {
        instanceMap.put(instanceName, instance);
//        InvokerHolder.cacheInvoker(instanceName, instance);
    }

    public void unRegisterProcessor(String instanceName) {
        if (instanceMap.containsKey(instanceName)) {
            instanceMap.remove(instanceName);
//            InvokerHolder.invalidateInvoker(instanceName);
        } else {
//            throw new RuntimeException("[JSF-23005]No such invoker key when unregister processor:" + instanceName);
        }
    }

    public Invoker getInvoker(String instanceName) {
        return instanceMap.get(instanceName);
    }

    @Override
    public void handleRpcRequest(RpcRequest rpcRequest, AbstractChannel channel) {
        try {
            // 丢到业务线程池去执行 TODO
//            RpcResponse rpcResponse = MessageBuilder.buildRpcResponse(rpcRequest);
//            rpcResponse.setReturnData("hello, this is response!");
//            channel.writeAndFlush(rpcResponse);

            BsoaTask task = new BsoaTask(this, rpcRequest, channel, BsoaConstants.DEFAULT_METHOD_PRIORITY);
            bizThreadPool.submit(task);
        } catch (BsoaRpcException e) {
            throw e;
        } catch (Exception e) {
            throw new BsoaRpcException(22222, e);
        }
    }

    public int entrySize() {
        return instanceMap.size();
    }

    public Map<String, Invoker> getAllOwnInvoker() {
        return instanceMap;
    }
}
