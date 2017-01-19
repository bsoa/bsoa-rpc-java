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
package io.bsoa.rpc.client.router;

import io.bsoa.rpc.common.SystemInfo;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;

/**
 * 方法级的IP路由
 * <p>
 * Created by zhangg on 2017/01/07 15:32.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("methodIp")
public class MethodIpRouter extends ParameterizedRouter {

    private String methodName;

    @Override
    public void setRule(String ruleJson) {
        super.setRule(ruleJson);
        String param = rule.getLeft();
        int idx = param.indexOf(".ip");
        methodName = param.substring(0, idx);
    }

    @Override
    public boolean matchRule(ParameterizedRule rule, RpcRequest invocation) {
        if (!invocation.getMethodName().equals(methodName)) {
            return false; // 不是本方法的干掉
        }
        return matchString(rule, SystemInfo.getLocalHost());
    }

}