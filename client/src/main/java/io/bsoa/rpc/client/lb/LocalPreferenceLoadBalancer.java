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
package io.bsoa.rpc.client.lb;

import io.bsoa.rpc.client.ProviderInfo;
import io.bsoa.rpc.common.SystemInfo;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * 本机优先的随机算法
 * <p>
 * Created by zhangg on 2017/01/07 15:32.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("localPref")
public class LocalPreferenceLoadBalancer extends RandomLoadBalancer {

    /**
     * 构造函数
     *
     * @param consumerConfig 服务消费者配置
     */
    public LocalPreferenceLoadBalancer(ConsumerConfig consumerConfig) {
        super(consumerConfig);
    }

    @Override
    public ProviderInfo doSelect(RpcRequest invocation, List<ProviderInfo> providerInfos) {
        String localhost = SystemInfo.getLocalHost();
        if (StringUtils.isEmpty(localhost)) {
            return super.doSelect(invocation, providerInfos);
        }
        List<ProviderInfo> localProviderInfo = new ArrayList<>();
        for (ProviderInfo providerInfo : providerInfos) { // 解析IP，看是否和本地一致
            if (localhost.equals(providerInfo.getIp())) {
                localProviderInfo.add(providerInfo);
            }
        }
        if (CommonUtils.isNotEmpty(localProviderInfo)) { // 命中本机的服务端
            return super.doSelect(invocation, localProviderInfo);
        } else { // 没有命中本机上的服务端
            return super.doSelect(invocation, providerInfos);
        }
    }
}