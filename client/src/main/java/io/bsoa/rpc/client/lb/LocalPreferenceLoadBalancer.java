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

import io.bsoa.rpc.client.Provider;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;

/**
 * 本机优先的随机算法
 * <p>
 * Created by zhangg on 2017/01/07 15:32.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("localPref")
public class LocalPreferenceLoadBalancer extends RandomLoadBalancer {

    @Override
    public Provider doSelect(RpcRequest invocation, List<Provider> providers) {
        String localhost = BsoaContext.getLocalHost();
        if (StringUtils.isEmpty(localhost)) {
            return super.doSelect(invocation, providers);
        }
        List<Provider> localProvider = new ArrayList<>();
        for (Provider provider : providers) { // 解析IP，看是否和本地一致
            if (localhost.equals(provider.getIp())) {
                localProvider.add(provider);
            }
        }
        if (CommonUtils.isNotEmpty(localProvider)) { // 命中本机的服务端
            return super.doSelect(invocation, localProvider);
        } else { // 没有命中本机上的服务端
            return super.doSelect(invocation, providers);
        }
    }
}