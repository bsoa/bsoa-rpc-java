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
package io.bsoa.rpc.bootstrap;

import javax.annotation.concurrent.ThreadSafe;

import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.ext.Extensible;

/**
 * <p>服务引用器扩展接口，单例</p>
 * <p>
 * Created by zhangg on 2017/2/8 19:18. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@ThreadSafe
@Extensible
public interface ServiceReferencer {

    /**
     * 调用一个服务
     *
     * @param <T>
     * @param consumerConfig 调用端配置
     * @return
     */
    public <T> AbstractConsumerBootstrap<T> refer(ConsumerConfig<T> consumerConfig);
}
