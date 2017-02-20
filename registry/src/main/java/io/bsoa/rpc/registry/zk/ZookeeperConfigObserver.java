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
package io.bsoa.rpc.registry.zk;

import java.util.List;

import org.apache.curator.framework.recipes.cache.ChildData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/2/20 23:05. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ZookeeperConfigObserver {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ZookeeperConfigObserver.class);
    
    /**
     * 该接口下增加了一个配置
     *
     * @param interfaceId 接口名称
     * @param data        配置
     */
    public void addConfig(String interfaceId, ChildData data) {
        if (data == null) {
            LOGGER.info("data is null");
        } else {
            LOGGER.info("Receive data: path=[" + data.getPath() + "]"
                    + ", data=[" + new String(data.getData()) + "]"
                    + ", stat=[" + data.getStat() + "]");
        }
    }

    public void removeConfig(String interfaceId, ChildData data) {
        if (data == null) {
            LOGGER.info("data is null");
        } else {
            LOGGER.info("Receive data: path=[" + data.getPath() + "]"
                    + ", data=[" + new String(data.getData()) + "]"
                    + ", stat=[" + data.getStat() + "]");
        }
    }

    public void updateConfig(String interfaceId, ChildData data) {
        if (data == null) {
            LOGGER.info("data is null");
        } else {
            LOGGER.info("Receive data: path=[" + data.getPath() + "]"
                    + ", data=[" + new String(data.getData()) + "]"
                    + ", stat=[" + data.getStat() + "]");
        }
    }

    public void updateConfigAll(String interfaceId, List<ChildData> currentData) {

    }
}
