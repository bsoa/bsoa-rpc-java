package io.bsoa.rpc.registry.zk;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.bsoa.rpc.config.RegistryConfig;

import static io.bsoa.rpc.registry.zk.ZookeeperRegistry.PARAM_CREATE_EPHEMERAL;
import static io.bsoa.rpc.registry.zk.ZookeeperRegistry.PARAM_PREFER_LOCAL_FILE;

/**
 * Created by zhangg on 2017/2/20.
 */
public class ZookeeperRegistryTest {

    private static ZookeeperRegistry zookeeperRegistry;

    @BeforeClass
    public static void beforeClass() throws Exception {
        zookeeperRegistry = new ZookeeperRegistry();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (zookeeperRegistry != null) {
            zookeeperRegistry.destroy();
        }
    }

    @Test
    public void init() throws Exception {
        RegistryConfig config = new RegistryConfig()
                .setAddress("127.0.0.1:2181")
                .setParameter(PARAM_PREFER_LOCAL_FILE, "false")
                .setParameter(PARAM_CREATE_EPHEMERAL, "true");
        zookeeperRegistry.init(config);
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