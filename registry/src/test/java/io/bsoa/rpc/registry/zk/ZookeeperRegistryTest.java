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

import io.bsoa.rpc.config.RegistryConfig;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.bsoa.rpc.registry.zk.ZookeeperRegistry.PARAM_CREATE_EPHEMERAL;
import static io.bsoa.rpc.registry.zk.ZookeeperRegistry.PARAM_PREFER_LOCAL_FILE;

/**
 * Created by zhangg on 2017/2/20.
 */
public class ZookeeperRegistryTest {

    private static ZookeeperRegistry zookeeperRegistry;

    @BeforeClass
    public static void beforeClass() throws Exception {
        RegistryConfig config = new RegistryConfig()
                .setAddress("127.0.0.1:2181")
                .setParameter(PARAM_PREFER_LOCAL_FILE, "false")
                .setParameter(PARAM_CREATE_EPHEMERAL, "true");
        zookeeperRegistry = new ZookeeperRegistry(config);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (zookeeperRegistry != null) {
            zookeeperRegistry.destroy();
        }
    }

    @Test
    public void init() throws Exception {
        zookeeperRegistry.init();
    }

    @Test
    public void start() throws Exception {
        Assert.assertTrue(zookeeperRegistry.start());
    }

    @Test
    public void register() throws Exception {

    }

    @Test
    public void unRegister() throws Exception {

    }

    @Test
    public void batchUnRegister() throws Exception {

    }

    @Test
    public void subscribe() throws Exception {

    }

    @Test
    public void unSubscribe() throws Exception {

    }

    @Test
    public void batchUnSubscribe() throws Exception {

    }

    @Test
    public void destroy() throws Exception {
        zookeeperRegistry.destroy();
    }

}