/*
 * Copyright 2016 The BSOA Project
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
package io.bsoa.rpc.client.lb;

import io.bsoa.rpc.client.AbstractLoadBalancer;
import io.bsoa.rpc.client.ProviderInfo;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;

import java.util.List;
import java.util.Random;

/**
 * 负载均衡随机算法:全部列表按权重随机选择
 * <p>
 * Created by zhangg on 2017/01/07 15:32.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("random")
public class RandomLoadBalancer extends AbstractLoadBalancer {

    /**
     * 随机
     */
    private final Random random = new Random();

    /**
     * 构造函数
     *
     * @param consumerConfig 服务消费者配置
     */
    public RandomLoadBalancer(ConsumerConfig consumerConfig) {
        super(consumerConfig);
    }

    @Override
    public ProviderInfo doSelect(RpcRequest invocation, List<ProviderInfo> providerInfos) {
        ProviderInfo providerInfo = null;
        int length = providerInfos.size(); // 总个数
        int totalWeight = 0; // 总权重
        boolean sameWeight = true; // 权重是否都一样
        for (int i = 0; i < length; i++) {
            int weight = getWeight(providerInfos.get(i));
            totalWeight += weight; // 累计总权重
            if (sameWeight && i > 0
                    && weight != getWeight(providerInfos.get(i - 1))) {
                sameWeight = false; // 计算所有权重是否一样
            }
        }
        if (totalWeight > 0 && !sameWeight) {
            // 如果权重不相同且权重大于0则按总权重数随机
            int offset = random.nextInt(totalWeight);
            // 并确定随机值落在哪个片断上
            for (int i = 0; i < length; i++) {
                offset -= getWeight(providerInfos.get(i));
                if (offset < 0) {
                    providerInfo = providerInfos.get(i);
                    break;
                }
            }
        } else {
            // 如果权重相同或权重为0则均等随机
            providerInfo = providerInfos.get(random.nextInt(length));
        }
        return providerInfo;
    }
}

