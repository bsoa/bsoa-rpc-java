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
package io.bsoa.rpc.filter;

import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by zhangg on 17-01-20.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class EchoFilter implements Filter {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(EchoFilter.class);


    @Override
    public RpcResponse invoke(FilterInvoker invoker, RpcRequest request) {
        Map<String, Object> configContext = invoker.getConfigContext(); // 可以拿到一些配置里的信息
        LOGGER.info("before invoke..");  // 在invoker.invoke(request)前加的代码，将在远程方法调用前执行
        RpcResponse response = invoker.invoke(request); // 调用链自动往下层执行
        LOGGER.info("after invoke..");  // 在invoker.invoke(request)后加的代码，将在远程方法调用后执行
        return response;
    }
}