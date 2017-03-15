/*
 *
 * Copyright (c) 2017 The BSOA Project
 *
 * The BSOA Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */
package io.bsoa.rpc.registry.zk;

import io.bsoa.rpc.client.ProviderInfo;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.listener.ProviderInfoListener;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/03/2017/3/15 23:21. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ZookeeperProviderObserver extends AbstractZookeeperObserver {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ZookeeperConfigObserver.class);


    /**
     * The Provider add listener map.
     */
    private ConcurrentHashMap<ConsumerConfig, List<ProviderInfoListener>> providerListenerMap
            = new ConcurrentHashMap<>();

    /**
     * Add provider listener.
     *
     * @param consumerConfig the consumer config
     * @param listener       the listener
     */
    public void addProviderListener(ConsumerConfig consumerConfig, ProviderInfoListener listener) {
        if (listener != null) {
            initOrAddList(providerListenerMap, consumerConfig, listener);
        }
    }

    /**
     * Remove provider listener.
     *
     * @param consumerConfig the consumer config
     */
    public void removeProviderListener(ConsumerConfig consumerConfig) {
        providerListenerMap.remove(consumerConfig);
    }

    public void updateProvider(ConsumerConfig config, String providerPath, ChildData data)
            throws UnsupportedEncodingException {
        List<ProviderInfoListener> providerInfoListeners = providerListenerMap.get(config);
        if (CommonUtils.isNotEmpty(providerInfoListeners)) {
            List<ProviderInfo> providerInfos = Arrays.asList(
                    ZookeeperRegistryHelper.convertUrlToProvider(providerPath, data));
            for (ProviderInfoListener listener : providerInfoListeners) {
                listener.addProvider(providerInfos);
            }
        }
    }

    public void removeProvider(ConsumerConfig config, String providerPath, ChildData data)
            throws UnsupportedEncodingException {
        List<ProviderInfoListener> providerInfoListeners = providerListenerMap.get(config);
        if (CommonUtils.isNotEmpty(providerInfoListeners)) {
            List<ProviderInfo> providerInfos = Arrays.asList(
                    ZookeeperRegistryHelper.convertUrlToProvider(providerPath, data));
            for (ProviderInfoListener listener : providerInfoListeners) {
                listener.removeProvider(providerInfos);
            }
        }
    }

    public void addProvider(ConsumerConfig config, String providerPath, ChildData data)
            throws UnsupportedEncodingException {
        List<ProviderInfoListener> providerInfoListeners = providerListenerMap.get(config);
        if (CommonUtils.isNotEmpty(providerInfoListeners)) {
            List<ProviderInfo> providerInfos = Arrays.asList(
                    ZookeeperRegistryHelper.convertUrlToProvider(providerPath, data));
            for (ProviderInfoListener listener : providerInfoListeners) {
                // TODO
                listener.removeProvider(providerInfos);
                listener.addProvider(providerInfos);
            }
        }
    }
}
