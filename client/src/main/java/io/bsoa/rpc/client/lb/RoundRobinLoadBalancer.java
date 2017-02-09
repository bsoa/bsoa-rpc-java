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
package io.bsoa.rpc.client.lb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.bsoa.rpc.client.AbstractLoadBalancer;
import io.bsoa.rpc.client.ProviderInfo;
import io.bsoa.rpc.common.struct.PositiveAtomicCounter;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;

/**
 * 负载均衡轮询算法，按方法级进行轮询，互不影响
 * <p>
 * Created by zhangg on 2017/01/07 15:32.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("roundRobin")
public class RoundRobinLoadBalancer extends AbstractLoadBalancer {

    private final ConcurrentMap<String, PositiveAtomicCounter> sequences = new ConcurrentHashMap<String, PositiveAtomicCounter>();

    private final ConcurrentMap<String, PositiveAtomicCounter> weightSequences = new ConcurrentHashMap<String, PositiveAtomicCounter>();

    public ProviderInfo doSelect(RpcRequest request, List<ProviderInfo> providerInfos) {
        String key = getServiceKey(request); // 每个方法级自己轮询，互不影响
        int length = providerInfos.size(); // 总个数
        int maxWeight = 0; // 最大权重
        int minWeight = Integer.MAX_VALUE; // 最小权重
        for (int i = 0; i < length; i++) {
            int weight = getWeight(providerInfos.get(i));
            maxWeight = Math.max(maxWeight, weight); // 累计最大权重
            minWeight = Math.min(minWeight, weight); // 累计最小权重
        }
        if (maxWeight > 0 && minWeight < maxWeight) { // 权重不一样,不再按照之前轮询顺序，
            PositiveAtomicCounter weightSequence = weightSequences.get(key);
            if (weightSequence == null) {
                weightSequences.putIfAbsent(key, new PositiveAtomicCounter());
                weightSequence = weightSequences.get(key);
            }
            int currentWeight = weightSequence.getAndIncrement() % maxWeight;
            List<ProviderInfo> weightInvokers = new ArrayList<ProviderInfo>();
            for (ProviderInfo invoker : providerInfos) { // 筛选权重大于当前权重基数的provider,保证权重大的服务哪怕是轮询，被调用的机会也是最多的
                if (getWeight(invoker) > currentWeight) {
                    weightInvokers.add(invoker);
                }
            }
            int weightLength = weightInvokers.size();
            if (weightLength == 1) {
                return weightInvokers.get(0);
            } else if (weightLength > 1) {
                providerInfos = weightInvokers;
                length = providerInfos.size();
            }
        }
        PositiveAtomicCounter sequence = sequences.get(key);
        if (sequence == null) {
            sequences.putIfAbsent(key, new PositiveAtomicCounter());
            sequence = sequences.get(key);
        }
        return providerInfos.get(sequence.getAndIncrement() % length);
    }


    private String getServiceKey(RpcRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getInterfaceName()).append("#")
                .append(request.getTags()).append("#")
                .append(request.getMethodName());
        return builder.toString();
    }


}

