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
package io.bsoa.rpc.filter;

import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * 客户端收集监控过滤器,目前收集耗时数据
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "consumerMonitor", order = -10)
public class ConsumerMonitorFilter implements Filter{

    @Override
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest request) {
        String className = request.getInterfaceName();
        String methodName = request.getMethodName();
        // 是否强制关闭监控，默认开启
        String monitorCfg = (String) invoker.getConfigContext().get(BsoaConstants.HIDDEN_KEY_MONITOR);
        // 判断是否启动监控，如果启动则运行
//        if (!CommonUtils.isFalse(monitorCfg) && MonitorFactory.isMonitorOpen(className, "%" + methodName, "%*")) {
//            long start = BsoaContext.now();
//            RpcResponse rpcResponse = invoker.invoke(request);
//            long end = BsoaContext.now();
//            String providerIp = RpcContext.getContext().getRemoteHostName();
//            if (providerIp != null) { // 远程服务端为空，表示没有调用
//                Monitor monitor = MonitorFactory.getMonitor(MonitorFactory.MONITOR_CONSUMER_ELAPSED,
//                        className, methodName, providerIp, JSFContext.getLocalHost());
//                if (monitor != null) { // 需要记录日志
//                    boolean iserror = rpcResponse.isError();
//                    invocation.addAttachment(BsoaConstants.INTERNAL_KEY_ELAPSED, end - start);
//                    //invocation.addAttachment(BsoaConstants.INTERNAL_KEY_RESULT, !iserror);
//                    //invocation.addAttachment(BsoaConstants.INTERNAL_KEY_PROTOCOL, Constants.ProtocolType.jsf.value() + "");
//                    if (iserror) { // 失败
//                        //monitor.recordException(invocation, rpcResponse.getException());
//                    } else { // 成功
//                        monitor.recordInvoked(invocation);
//                    }
//                }
//            }
//            return rpcResponse;
//        } else {
            return invoker.invoke(request);
//        }
    }
}