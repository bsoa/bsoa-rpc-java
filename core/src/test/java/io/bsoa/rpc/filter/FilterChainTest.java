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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import io.bsoa.rpc.GenericService;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.config.MethodConfig;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.test.HelloService;

/**
 * Created by zhangg on 17-01-20.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class FilterChainTest {

    @Test
    public void testInvoke() {
        List<Filter> filters = new ArrayList<>();
        filters.add(new TimePrintFilter());
        filters.add(new EchoFilter());

        FilterChain filter = new FilterChain(filters, new MockInvoker(), null);
        FilterInvoker chain = (FilterInvoker) filter.getChain();
        Assert.assertNotNull(chain);
        Assert.assertEquals(chain.getNextFilter().getClass(), TimePrintFilter.class);
        Assert.assertEquals(chain.getInvoker().getNextFilter().getClass(), EchoFilter.class);
        Assert.assertNull(chain.getInvoker().getInvoker().getNextFilter());
        Assert.assertEquals(chain.getInvoker().getInvoker().getClass(), MockInvoker.class);

        RpcRequest rpcRequest = MessageBuilder.buildRpcRequest(HelloService.class, "world", new Class[0], new Object[0]);

        RpcResponse response = filter.invoke(rpcRequest);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getReturnData());
    }

    @Test
    public void testGetContextFromConfig() {
        ConsumerConfig<GenericService> consumerConfig = new ConsumerConfig<GenericService>();
        consumerConfig.setInterfaceId("io.bsoa.example.test.HelloService");
        consumerConfig.setTags("ZG1");
        consumerConfig.setTimeout(5000);
        consumerConfig.setConnectTimeout(2000);
        consumerConfig.setUrl("jsf://127.0.0.1:9090;jsf://127.0.0.1:9091");
        consumerConfig.setRetries(2);
        consumerConfig.setGeneric(true);
        consumerConfig.setCluster("failover");
        consumerConfig.setLoadBalancer("random");

        List<MethodConfig> methodConfigs = new ArrayList<MethodConfig>();
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName("hello");
        methodConfig.setValidation(true);
        methodConfig.setTimeout(3000);
        methodConfig.setRetries(10);
        methodConfigs.add(methodConfig);
        consumerConfig.setMethods(methodConfigs);

        FilterChain filterChain = FilterChain.buildConsumerChain(consumerConfig,
                new ConsumerInvoker(consumerConfig, null));
        FilterInvoker invoker = (FilterInvoker) filterChain.getChain();
        Map<String, Object> context = invoker.getConfigContext();
        Assert.assertNotNull(context);
        boolean error = false;
        try {
            context.put("aa", "bb");// 不能操作
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);
        Assert.assertEquals(context.get("retries"), 2);
        Assert.assertEquals(context.get("timeout"), 5000);
        Assert.assertEquals(invoker.getMethodParam("hello", "retries"), 10);
        Assert.assertEquals(invoker.getMethodParam("hello", "timeout"), 3000);
        Assert.assertTrue((Boolean) invoker.getMethodParam("hello", "validation"));

    }
}