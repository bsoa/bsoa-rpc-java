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
package io.bsoa.rpc.listener;

import io.bsoa.rpc.client.ProviderInfo;

import java.util.List;

/**
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
