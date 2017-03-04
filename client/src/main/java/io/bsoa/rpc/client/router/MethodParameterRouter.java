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
package io.bsoa.rpc.client.router;

import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;

/**
 * 方法参数路由，需要指定方法名和参数索引
 * <p>
 * Created by zhangg on 2017/01/07 15:32.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("methodParam")
public class MethodParameterRouter extends ParameterizedRouter {

    private final String methodName;

    private final int paramIndex;

    public MethodParameterRouter(String routerKey, String routerRule) {
        super(routerKey, routerRule);
        String param = rule.getLeft();
        int idx = param.indexOf(".arg");
        methodName = param.substring(0, idx);
        paramIndex = Integer.parseInt(param.substring(idx + 4));
    }

    private Object getParamValue(RpcRequest request) {
        // 得到第几个参数
        return request.getArgs()[paramIndex];
    }

    @Override
    public boolean matchRule(ParameterizedRule rule, RpcRequest request) {
        // 非本方法的返回
        if (!request.getMethodName().equals(methodName) ||
                request.getArgs().length < paramIndex) {
            return false;
        }
        Object value = getParamValue(request);
        if (value == null) { // 值为空
            return false;
        }
        if (value instanceof String) { // 字符串
            return super.matchString(rule, (String) value);
        } else if (value instanceof Integer) { // 数字
            return super.matchInteger(rule, (Integer) value);
        } else if (value instanceof Long) { // 数字
            return super.matchLong(rule, (Long) value);
        } else { // 对象
            return super.matchObject(rule, value);
        }
    }
}