/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bsoa.rpc.bootstrap;

import java.util.List;

import io.bsoa.rpc.client.Client;
import io.bsoa.rpc.client.ProviderInfo;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.ext.Extensible;

/**
 * <p>引用服务的包装类，包括具体的启动后的对象</p>
 * <p>
 * Created by zhangg on 2017/2/8 22:45. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extensible
public abstract class ConsumerBootstrap<T> {

    /**
     * 服务消费者配置
     */
    protected final ConsumerConfig<T> consumerConfig;

    /**
     * 构造函数
     *
     * @param consumerConfig 服务消费者配置
     */
    protected ConsumerBootstrap(ConsumerConfig<T> consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    /**
     * 得到服务消费者配置
     *
     * @return 服务消费者配置
     */
    public ConsumerConfig<T> getConsumerConfig() {
        return consumerConfig;
    }

    /**
     * 调用一个服务
     *
     * @return 代理类
     */
    public abstract T refer();

    /**
     * 取消调用一个服务
     */
    public abstract void unRefer();

    public abstract Client getClient();

    public abstract List<ProviderInfo> subscribe();

    public abstract T getProxyIns();
}
