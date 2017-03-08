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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
     * Zookeeper client
     */
    private CuratorFramework client;

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
    private boolean ephemeralNode;

    /**
     * 配置项观察者
     */
    private ZookeeperConfigObserver configObserver;

    @Override
    public void init() {
        String address = registryConfig.getAddress(); // xxx:2181,yyy:2181/path1/paht2
        int idx = address.indexOf("/");
        if (idx > 0) {
            address = address.substring(idx);
            rootPath = address.substring(idx);
            if (!rootPath.endsWith("/")) {
                rootPath += "/"; // 保证以"/"结尾
            }
        } else {
            rootPath = "/";
        }
        configObserver = new ZookeeperConfigObserver();
        preferLocalFile = CommonUtils.isTrue(registryConfig.getParameter(PARAM_PREFER_LOCAL_FILE));
        ephemeralNode = CommonUtils.isTrue(registryConfig.getParameter(PARAM_CREATE_EPHEMERAL));
        LOGGER.info("Init ZookeeperRegistry with address {}, root path is {}.", address, rootPath);
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        client = CuratorFrameworkFactory.builder()
                .connectString(address)
                .sessionTimeoutMs(registryConfig.getConnectTimeout() * 3)
                .connectionTimeoutMs(registryConfig.getConnectTimeout())
                .canBeReadOnly(false)
                .retryPolicy(retryPolicy)
                .defaultData(null)
                .build();
    }

    @Override
    public boolean start() {
        try {
            client.start();
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, "Failed to start zookeeper client", e);
        }
        return client.getState() == CuratorFrameworkState.STARTED;
    }

    @Override
    public void destroy() {
        if (client != null && client.getState() == CuratorFrameworkState.STARTED) {
            client.close();
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
        // 注册服务端节点
        if (config.isRegister()) {
            try {
                List<String> urls = config.getBootstrap().buildUrls();
                if (CommonUtils.isNotEmpty(urls)) {
                    String providerPath = buildProviderPath(config);
                    for (String url : urls) {
                        client.create()
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

        // 订阅配置节点
        if (config.isSubscribe()) {
            try {
                String configPath = buildConfigPath(config);
                // 监听配置节点下 子节点增加、子节点删除、子节点Data修改事件
                PathChildrenCache pathChildrenCache = new PathChildrenCache(client, configPath, true);
                pathChildrenCache.getListenable().addListener((client1, event) -> {
                    System.out.println("Receive event: " + "type=[" + event.getType() + "]");
                    switch (event.getType()) {
                        case CHILD_ADDED: //加了一个配置
                            configObserver.addConfig(config.getInterfaceId(), event.getData());
                            break;
                        case CHILD_REMOVED: //删了一个配置
                            configObserver.removeConfig(config.getInterfaceId(), event.getData());
                            break;
                        case CHILD_UPDATED:
                            configObserver.updateConfig(config.getInterfaceId(), event.getData());
                            break;
                    }
                });
//        watcher.start(PathChildrenCache.StartMode.NORMAL);// 历史数据触发事件，CurrentData为空
                pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);// 历史数据不触发事件，而是初始化到CurrentData
//        watcher.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);// 历史数据触发事件，CurrentData为空，最后会收到一个加载完毕事件
                INTERFACE_CONFIG_CACHE.put(configPath, pathChildrenCache);
                configObserver.updateConfigAll(config.getInterfaceId(), pathChildrenCache.getCurrentData());


            } catch (Exception e) {
                throw new BsoaRuntimeException(22222,
                        "Failed to subscribe provider config to zookeeperRegistry!", e);
            }
        }
    }

    @Override
    public void unRegister(ProviderConfig config) {
        // 反注册服务端节点
        if (config.isRegister()) {
            try {
                List<String> urls = config.getBootstrap().buildUrls();
                if (CommonUtils.isNotEmpty(urls)) {
                    String providerPath = buildProviderPath(config);
                    for (String url : urls) {
                        client.delete().forPath(providerPath + "/" + url);
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
                String configPath = buildConfigPath(config);
                // 监听这个节点下 子节点增加、子节点删除、子节点Data修改事件
                // TODO
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
                String consumerNode = buildConsumerPath(config);
                client.create()
                        .withMode(ephemeralNode ? CreateMode.EPHEMERAL : CreateMode.PERSISTENT) // 是否永久节点
                        .forPath(consumerNode);
            } catch (Exception e) {
                throw new BsoaRuntimeException(22222, "Failed to register consumer config to zookeeperRegistry!");
            }
        }
        // 订阅Providers节点
        try {
            String consumerNode = buildConsumerPath(config);
            client.create()
                    .withMode(ephemeralNode ? CreateMode.EPHEMERAL : CreateMode.PERSISTENT) // 是否永久节点
                    .forPath(consumerNode, PROVIDER_NONE); // 是否
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, "Failed to register consumer config to zookeeperRegistry!");
        }
        // 订阅配置
        return null;
    }

    @Override
    public void unSubscribe(ConsumerConfig config) {

    }

    @Override
    public void batchUnSubscribe(List<ConsumerConfig> configs) {

    }


    private String buildProviderPath(ProviderConfig config) {
        return "bsoa/" + config.getInterfaceId() + "/providers";
    }

    private String buildConsumerPath(ConsumerConfig config) {
        return "bsoa/" + config.getInterfaceId() + "/consumers";
    }

    private String buildConfigPath(AbstractInterfaceConfig config) {
        return "bsoa/" + config.getInterfaceId() + "/configs";
    }

}
