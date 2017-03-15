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
package io.bsoa.rpc.registry.zk;

import io.bsoa.rpc.config.AbstractInterfaceConfig;
import io.bsoa.rpc.listener.ConfigListener;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/2/20 23:05. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ZookeeperConfigObserver extends AbstractZookeeperObserver {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ZookeeperConfigObserver.class);

    /**
     * The Config listener map.
     */
    private ConcurrentHashMap<AbstractInterfaceConfig, List<ConfigListener>> configListenerMap
            = new ConcurrentHashMap<>();


    /**
     * 该接口下增加了一个配置
     *
     * @param config 接口名称
     * @param data        配置
     */
    public void addConfig(AbstractInterfaceConfig config, ChildData data) {
        if (data == null) {
            LOGGER.info("data is null");
        } else {
            LOGGER.info("Receive data: path=[" + data.getPath() + "]"
                    + ", data=[" + new String(data.getData()) + "]"
                    + ", stat=[" + data.getStat() + "]");
        }
    }

    public void removeConfig(AbstractInterfaceConfig config, ChildData data) {
        if (data == null) {
            LOGGER.info("data is null");
        } else {
            LOGGER.info("Receive data: path=[" + data.getPath() + "]"
                    + ", data=[" + new String(data.getData()) + "]"
                    + ", stat=[" + data.getStat() + "]");
        }
    }

    public void updateConfig(AbstractInterfaceConfig config, ChildData data) {
        if (data == null) {
            LOGGER.info("data is null");
        } else {
            LOGGER.info("Receive data: path=[" + data.getPath() + "]"
                    + ", data=[" + new String(data.getData()) + "]"
                    + ", stat=[" + data.getStat() + "]");
        }
    }

    public void updateConfigAll(AbstractInterfaceConfig config, List<ChildData> currentData) {

    }


    /**
     * Add config listener.
     *
     * @param config   the config
     * @param listener the listener
     */
    public void addConfigListener(AbstractInterfaceConfig config, ConfigListener listener) {
        if (listener != null) {
            initOrAddList(configListenerMap, config, listener);
        }
    }

    /**
     * Remove config listener.
     *
     * @param config the config
     */
    public void removeConfigListener(AbstractInterfaceConfig config) {
        configListenerMap.remove(config);
    }
}
