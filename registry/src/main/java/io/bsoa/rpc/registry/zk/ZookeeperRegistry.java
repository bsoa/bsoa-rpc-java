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

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.client.ProviderInfo;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.config.RegistryConfig;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.listener.ConfigListener;
import io.bsoa.rpc.listener.ProviderInfoListener;
import io.bsoa.rpc.registry.Registry;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/2/18 17:32. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("zookeeper")
public class ZookeeperRegistry implements Registry {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ZookeeperRegistry.class);

    public final static String PARAM_PREFER_LOCAL_FILE = "preferLocalFile";

    /**
     * Zookeeper client
     */
    private CuratorFramework  client;

    /**
     * Root path of registry data
     */
    private String rootPath;

    /**
     * prefer get data from local file to remote zk cluster.
     */
    private boolean preferLocalFile;

    @Override
    public void init(RegistryConfig registryConfig) {
        String address = registryConfig.getAddress(); // xxx:2181,yyy:2181/path1/paht2
        int idx = address.indexOf("/");
        if (idx > 0) {
            address = address.substring(idx);
            rootPath = address.substring(idx);
        } else {
            rootPath = "/";
        }
        preferLocalFile = CommonUtils.isTrue(registryConfig.getParameter(PARAM_PREFER_LOCAL_FILE));
        LOGGER.info("Init ZookeeperRegistry with address {}, root path is {}.", address, rootPath);
        try {
            RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
            client = CuratorFrameworkFactory.newClient(address, retryPolicy);
            client.start();
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, "Failed to start zookeeper client", e);
        }
    }

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public void register(ProviderConfig config, ConfigListener listener) {
//        client.create().forPath("/my/path", myData)
    }

    @Override
    public void unregister(ProviderConfig config) {

    }

    @Override
    public void batchUnRegister(List<ProviderConfig> configs) {

    }

    @Override
    public List<ProviderInfo> subscribe(ConsumerConfig config, ProviderInfoListener providerInfoListener, ConfigListener configListener) {
        return null;
    }

    @Override
    public void unSubscribe(ConsumerConfig config) {

    }

    @Override
    public void batchUnSubscribe(List<ConsumerConfig> configs) {

    }

    @Override
    public void destroy() {
        if (client != null && client.getState() == CuratorFrameworkState.STARTED) {
            client.close();
        }
    }
}
