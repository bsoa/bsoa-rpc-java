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

import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.config.RegistryConfig;
import io.bsoa.rpc.config.ServerConfig;
import io.bsoa.rpc.registry.RegistryFactory;
import io.bsoa.test.TestService;
import io.bsoa.test.TestServiceImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URLEncoder;
import java.util.List;

import static io.bsoa.rpc.registry.zk.ZookeeperRegistry.PARAM_CREATE_EPHEMERAL;
import static io.bsoa.rpc.registry.zk.ZookeeperRegistry.PARAM_PREFER_LOCAL_FILE;

/**
 * Created by zhangg on 2017/2/20.
 */
public class ZookeeperRegistryTest {

    private static RegistryConfig registryConfig;
    private static ZookeeperRegistry zookeeperRegistry;

    @BeforeClass
    public static void beforeClass() throws Exception {
        registryConfig = new RegistryConfig()
                .setProtocol("zookeeper")
                .setAddress("127.0.0.1:2181/test")
                .setParameter(PARAM_PREFER_LOCAL_FILE, "false")
                .setParameter(PARAM_CREATE_EPHEMERAL, "true");
        zookeeperRegistry = (ZookeeperRegistry) RegistryFactory.getRegistry(registryConfig);

        zookeeperRegistry.init();

        Assert.assertTrue(zookeeperRegistry.start());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (zookeeperRegistry != null) {
            zookeeperRegistry.destroy();
        }
    }

    @Test
    public void init() throws Exception {
        zookeeperRegistry.init(); // test duplicate init
    }

    @Test
    public void start() throws Exception {
        Assert.assertTrue(zookeeperRegistry.start()); // test duplicate start
    }

    @Test
    public void register() throws Exception {
        ProviderConfig providerConfig = new ProviderConfig<TestService>()
                .setId("xxx")
                .setServer(new ServerConfig())
                .setInterfaceId(TestService.class.getName())
                .setRef(new TestServiceImpl())
                .setRegistry(registryConfig);
        providerConfig.export();
        zookeeperRegistry.register(providerConfig, null);

        String key = (String) providerConfig.getBootstrap().buildUrls().get(0);
        key = URLEncoder.encode(key, "UTF-8");
        String path = "/test/bsoa/io.bsoa.test.TestService/providers";
        List<String> providers = zookeeperRegistry.getClient().getChildren()
                .forPath(path);

        boolean exists = false;
        for (String provider : providers) {
            if(provider.equals(key)){
                exists = true;
            }
        }
        Assert.assertTrue(exists);

        providerConfig.unExport();


//        List<ProviderInfo> providerInfos =zookeeperRegistry.subscribe(consumerConfig, null, null);
//        Assert.assertArrayEquals(providerInfos.size() = 1);

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