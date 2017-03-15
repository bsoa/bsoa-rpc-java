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

import io.bsoa.rpc.client.ProviderInfo;
import io.bsoa.rpc.common.annotation.JustForTest;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.config.AbstractInterfaceConfig;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.config.RegistryConfig;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.listener.ConfigListener;
import io.bsoa.rpc.listener.ProviderInfoListener;
import io.bsoa.rpc.registry.Registry;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static io.bsoa.rpc.registry.zk.ZookeeperRegistryHelper.buildConfigPath;
import static io.bsoa.rpc.registry.zk.ZookeeperRegistryHelper.buildConsumerPath;
import static io.bsoa.rpc.registry.zk.ZookeeperRegistryHelper.buildProviderPath;

/**
 * <p>简单的Zookeeper注册中心,具有如下特性：<br>
 * 1.可以设置优先读取远程，还是优先读取本地备份文件<br>
 * 2.如果zk不可用，自动读取本地备份文件<br>
 * 3.可以设置使用临时节点还是永久节点<br>
 * 4.断线了会自动重连，并且自动recover数据<br><br>
 * <pre>
 *  在zookeeper上存放的数据结构为：
 *  -$rootPath (根路径)
 *         └--bsoa
 *             |--io.bsoa.rpc.example.HelloService （服务）
 *             |       |-providers （服务提供者列表）
 *             |       |     |--bsoa://192.168.1.100:22000?xxx=yyy [1]
 *             |       |     |--bsoa://192.168.1.110:22000?xxx=yyy [1]
 *             |       |     └--bsoa://192.168.1.120?xxx=yyy [1]
 *             |       |-consumers （服务调用者列表）
 *             |       |     |--bsoa://192.168.3.100?xxx=yyy []
 *             |       |     |--bsoa://192.168.3.110?xxx=yyy []
 *             |       |     └--bsoa://192.168.3.120?xxx=yyy []
 *             |       └-configs (接口级配置）
 *             |            |--invoke.blacklist ["xxxx"]
 *             |            └--monitor.open ["true"]
 *             |--io.bsoa.rpc.example.EchoService （下一个服务）
 *             | ......
 *  </pre>
 * </p>
 * <p>
 * Created by zhangg on 2017/2/18 17:32. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("zookeeper")
public class ZookeeperRegistry extends Registry {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ZookeeperRegistry.class);

    /**
     * 注册中心配置
     *
     * @param registryConfig 注册中心配置
     */
    protected ZookeeperRegistry(RegistryConfig registryConfig) {
        super(registryConfig);
    }

    /**
     * 配置项：是否本地优先
     */
    public final static String PARAM_PREFER_LOCAL_FILE = "preferLocalFile";

    /**
     * 配置项：是否使用临时节点。<br>
     * 如果使用临时节点：那么断开连接的时候，将zookeeper将自动消失。好处是如果服务端异常关闭，也不会有垃圾数据。<br>
     * 坏处是如果和zookeeper的网络闪断也通知客户端，客户端以为是服务端下线<br>
     * 如果使用永久节点：好处：网络闪断时不会影响服务端，而是由客户端进行自己判断长连接<br>
     * 坏处：服务端如果是异常关闭（无反注册），那么数据里就由垃圾节点，得由另外的哨兵程序进行判断
     */
    public final static String PARAM_CREATE_EPHEMERAL = "createEphemeral";


    private final static byte[] PROVIDER_OFFLINE = new byte[]{0};
    private final static byte[] PROVIDER_ONLINE = new byte[]{1};
    private final static byte[] PROVIDER_NONE = new byte[0];

    /**
     * Zookeeper zkClient
     */
    private CuratorFramework zkClient;

    /**
     * Root path of registry data
     */
    private String rootPath;

    /**
     * Prefer get data from local file to remote zk cluster.
     *
     * @see ZookeeperRegistry#PARAM_PREFER_LOCAL_FILE
     */
    private boolean preferLocalFile;

    /**
     * Create EPHEMERAL node when true, otherwise PERSISTENT
     *
     * @see ZookeeperRegistry#PARAM_CREATE_EPHEMERAL
     * @see CreateMode#PERSISTENT
     * @see CreateMode#EPHEMERAL
     */
    private boolean ephemeralNode = true;

    /**
     * 配置项观察者
     */
    private ZookeeperConfigObserver configObserver;

    /**
     * 配置项观察者
     */
    private ZookeeperProviderObserver providerObserver;

    @Override
    public synchronized void init() {
        if (zkClient != null) {
            return;
        }
        String addressInput = registryConfig.getAddress(); // xxx:2181,yyy:2181/path1/paht2
        int idx = addressInput.indexOf("/");
        String address; // IP地址
        if (idx > 0) {
            address = addressInput.substring(0, idx);
            rootPath = addressInput.substring(idx);
            if (!rootPath.endsWith("/")) {
                rootPath += "/"; // 保证以"/"结尾
            }
        } else {
            address = addressInput;
            rootPath = "/";
        }
        preferLocalFile = CommonUtils.isTrue(registryConfig.getParameter(PARAM_PREFER_LOCAL_FILE));
        ephemeralNode = CommonUtils.isTrue(registryConfig.getParameter(PARAM_CREATE_EPHEMERAL));
        LOGGER.info("Init ZookeeperRegistry with address {}, root path is {}.", address, rootPath);
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        zkClient = CuratorFrameworkFactory.builder()
                .connectString(address)
                .sessionTimeoutMs(registryConfig.getConnectTimeout() * 3)
                .connectionTimeoutMs(registryConfig.getConnectTimeout())
                .canBeReadOnly(false)
                .retryPolicy(retryPolicy)
                .defaultData(null)
                .build();
    }

    @Override
    public synchronized boolean start() {
        if (zkClient == null) {
            LOGGER.warn("Start zookeeper registry must be do init first!");
            return false;
        }
        if (zkClient.getState() == CuratorFrameworkState.STARTED) {
            return true;
        }
        try {
            zkClient.start();
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, "Failed to start zookeeper zkClient", e);
        }
        return zkClient.getState() == CuratorFrameworkState.STARTED;
    }

    @Override
    public void destroy() {
        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            zkClient.close();
        }
    }

    /**
     * 接口配置{接口配置路径：PathChildrenCache} <br>
     * 例如：{/bsoa/io.bsoa.rpc.example/configs ： PathChildrenCache }
     */
    private static final ConcurrentHashMap<String, PathChildrenCache> INTERFACE_CONFIG_CACHE
            = new ConcurrentHashMap<>();

    @Override
    public void register(ProviderConfig config, ConfigListener listener) {
        if (config.isRegister()) {
            // 注册服务端节点
            try {
                List<String> urls = ZookeeperRegistryHelper.convertProviderToUrls(config);
                if (CommonUtils.isNotEmpty(urls)) {
                    String providerPath = buildProviderPath(rootPath, config);
                    for (String url : urls) {
                        url = URLEncoder.encode(url, "UTF-8");
                        getAndCheckZkClient().create().creatingParentContainersIfNeeded()
                                .withMode(ephemeralNode ? CreateMode.EPHEMERAL : CreateMode.PERSISTENT) // 是否永久节点
                                .forPath(providerPath + "/" + url,
                                        config.isDynamic() ? PROVIDER_ONLINE : PROVIDER_OFFLINE); // 是否默认上下线
                    }
                }
            } catch (Exception e) {
                throw new BsoaRuntimeException(22222,
                        "Failed to register provider config to zookeeperRegistry!", e);
            }
        }

        if (config.isSubscribe()) {
            // 订阅配置节点
            String configPath = buildConfigPath(rootPath, config);
            if (!INTERFACE_CONFIG_CACHE.containsKey(configPath)) {
                subscribeConfig(config, listener);
            }
        }
    }

    protected void subscribeConfig(AbstractInterfaceConfig config, ConfigListener listener) {
        String configPath = buildConfigPath(rootPath, config);
        try {
            if (configObserver == null) { // 初始化
                configObserver = new ZookeeperConfigObserver();
            }
            configObserver.addConfigListener(config, listener);
            // 监听配置节点下 子节点增加、子节点删除、子节点Data修改事件
            PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, configPath, true);
            pathChildrenCache.getListenable().addListener((client1, event) -> {
                LOGGER.debug("Receive zookeeper event: " + "type=[" + event.getType() + "]");
                switch (event.getType()) {
                    case CHILD_ADDED: //加了一个配置
                        configObserver.addConfig(config, event.getData());
                        break;
                    case CHILD_REMOVED: //删了一个配置
                        configObserver.removeConfig(config, event.getData());
                        break;
                    case CHILD_UPDATED:
                        configObserver.updateConfig(config, event.getData());
                        break;
                }
            });
            pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
            INTERFACE_CONFIG_CACHE.put(configPath, pathChildrenCache);
            configObserver.updateConfigAll(config, pathChildrenCache.getCurrentData());
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222,
                    "Failed to subscribe provider config to zookeeperRegistry!", e);
        }
    }

    @Override
    public void unRegister(ProviderConfig config) {
        // 反注册服务端节点
        if (config.isRegister()) {
            try {
                List<String> urls = ZookeeperRegistryHelper.convertProviderToUrls(config);
                if (CommonUtils.isNotEmpty(urls)) {
                    String providerPath = buildProviderPath(rootPath, config);
                    for (String url : urls) {
                        url = URLEncoder.encode(url, "UTF-8");
                        getAndCheckZkClient().delete().forPath(providerPath + "/" + url);
                    }
                }
            } catch (Exception e) {
                throw new BsoaRuntimeException(22222,
                        "Failed to register provider config to zookeeperRegistry!", e);
            }
        }
        // 反订阅配置节点
        if (config.isSubscribe()) {
            try {
                String configPath = buildConfigPath(rootPath, config);
                // 监听这个节点下 子节点增加、子节点删除、子节点Data修改事件
                // TODO
                configObserver.removeConfigListener(config);
            } catch (Exception e) {
                throw new BsoaRuntimeException(22222,
                        "Failed to register provider config to zookeeperRegistry!", e);
            }
        }
    }

    @Override
    public void batchUnRegister(List<ProviderConfig> configs) {
        // 一个一个来，后续看看要不要使用curator的事务
        for (ProviderConfig config : configs) {
            unRegister(config);
        }
    }

    @Override
    public List<ProviderInfo> subscribe(ConsumerConfig config,
                                        ProviderInfoListener providerInfoListener, ConfigListener configListener) {
        // 注册Consumer节点
        if (config.isRegister()) {
            try {
                String consumerPath = buildConsumerPath(rootPath, config);
                String url = ZookeeperRegistryHelper.convertConsumerToUrl(config);
                url = URLEncoder.encode(url, "UTF-8");
                getAndCheckZkClient().create().creatingParentContainersIfNeeded()
                        .withMode(CreateMode.EPHEMERAL) // Consumer临时节点
                        .forPath(consumerPath + "/" + url);
            } catch (Exception e) {
                throw new BsoaRuntimeException(22222,
                        "Failed to register consumer config to zookeeperRegistry!", e);
            }
        }
        if (config.isSubscribe()) {
            // 订阅配置
            String configPath = buildConfigPath(rootPath, config);
            if (!INTERFACE_CONFIG_CACHE.containsKey(configPath)) {
                subscribeConfig(config, configListener);
            }

            // 订阅Providers节点
            try {
                if (providerObserver == null) { // 初始化
                    providerObserver = new ZookeeperProviderObserver();
                }
                String providerPath = buildProviderPath(rootPath, config);

                // 监听配置节点下 子节点增加、子节点删除、子节点Data修改事件
                providerObserver.addProviderListener(config, providerInfoListener);
                // TODO 换成监听父节点变化（只是监听变化了，而不通知变化了什么，然后客户端自己来拉数据的）
                PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, providerPath, true);
                pathChildrenCache.getListenable().addListener((client1, event) -> {
                    LOGGER.debug("Receive zookeeper event: " + "type=[" + event.getType() + "]");
                    switch (event.getType()) {
                        case CHILD_ADDED: //加了一个provider
                            providerObserver.addProvider(config, providerPath, event.getData());
                            break;
                        case CHILD_REMOVED: //删了一个provider
                            providerObserver.removeProvider(config, providerPath, event.getData());
                            break;
                        case CHILD_UPDATED: // 更新一个Provider
                            providerObserver.updateProvider(config, providerPath, event.getData());
                            break;
                    }
                });
                pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
                return ZookeeperRegistryHelper.convertUrlsToProviders(providerPath, pathChildrenCache.getCurrentData());
            } catch (Exception e) {
                throw new BsoaRuntimeException(22222,
                        "Failed to register consumer config to zookeeperRegistry!", e);
            }
        }
        return null;
    }

    @Override
    public void unSubscribe(ConsumerConfig config) {
        // 反注册服务端节点
        if (config.isRegister()) {
            try {
                String consumerPath = buildConsumerPath(rootPath, config);
                String url = ZookeeperRegistryHelper.convertConsumerToUrl(config);
                url = URLEncoder.encode(url, "UTF-8");
                getAndCheckZkClient().delete().forPath(consumerPath + "/" + url);
            } catch (Exception e) {
                throw new BsoaRuntimeException(22222,
                        "Failed to register provider config to zookeeperRegistry!", e);
            }
        }
        // 反订阅配置节点
        if (config.isSubscribe()) {
            try {
                String configPath = buildConfigPath(rootPath, config);
                // 监听这个节点下 子节点增加、子节点删除、子节点Data修改事件
                // TODO

                configObserver.removeConfigListener(config);
                providerObserver.removeProviderListener(config);
            } catch (Exception e) {
                throw new BsoaRuntimeException(22222,
                        "Failed to register provider config to zookeeperRegistry!", e);
            }
        }
    }

    @Override
    public void batchUnSubscribe(List<ConsumerConfig> configs) {
        // 一个一个来，后续看看要不要使用curator的事务
        for (ConsumerConfig config : configs) {
            unSubscribe(config);
        }
    }

    @JustForTest
    CuratorFramework getZkClient() {
        return zkClient;
    }

    private CuratorFramework getAndCheckZkClient() {
        if (zkClient == null || zkClient.getState() != CuratorFrameworkState.STARTED) {
            throw new BsoaRuntimeException(22222, "Zookeeper client is not available");
        }
        return zkClient;
    }
}
