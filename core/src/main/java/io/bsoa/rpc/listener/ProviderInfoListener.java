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
package io.bsoa.rpc.listener;

import java.util.List;

import io.bsoa.rpc.client.ProviderInfo;

/**
 *
 *
 * Created by zhangg on 2016/7/14 22:26.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public interface ProviderInfoListener {

    /**
     * 增加服务节点
     *
     * @param providerInfos 待新增的服务列表（部分）
     */
    void addProvider(List<ProviderInfo> providerInfos);

    /**
     * 删除服务节点
     *
     * @param providerInfos 待删除的服务列表(部分)
     */
    void removeProvider(List<ProviderInfo> providerInfos);

    /**
     * 更新服务节点
     *
     * @param providerInfos 新的服务列表(全)
     */
    void updateProvider(List<ProviderInfo> providerInfos);
}
