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
import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.BsoaVersion;
import io.bsoa.rpc.common.SystemInfo;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.config.AbstractInterfaceConfig;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.config.ServerConfig;
import io.bsoa.rpc.context.BsoaContext;
import org.apache.curator.framework.recipes.cache.ChildData;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/03/2017/3/15 01:26. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ZookeeperRegistryHelper {

    /**
     * Convert provider to url.
     *
     * @param providerConfig the ProviderConfig
     * @return the JsfUrl list
     */
    static List<String> convertProviderToUrls(ProviderConfig providerConfig) {
        List<ServerConfig> servers = providerConfig.getServer();
        if (servers != null && !servers.isEmpty()) {
            List<String> urls = new ArrayList<String>();
            for (ServerConfig server : servers) {
                StringBuilder sb = new StringBuilder(200);
                String host = server.getVirtualhost(); // 虚拟ip
                if (host == null) {
                    host = server.getHost();
                    if (NetUtils.isLocalHost(host) || NetUtils.isAnyHost(host)) {
                        host = SystemInfo.getLocalHost();
                    }
                }
                sb.append(server.getProtocol()).append("://").append(host)
                        .append(":").append(server.getPort()).append(server.getContextPath())
                        .append(providerConfig.getInterfaceId())
                        .append("?tags=").append(providerConfig.getTags())
                        //.append(getKeyPairs("randomPort", server.isRandomPort()))
                        .append(getKeyPairs("timeout", providerConfig.getTimeout()))
                        .append(getKeyPairs("delay", providerConfig.getDelay()))
                        .append(getKeyPairs("id", providerConfig.getId()))
                        .append(getKeyPairs("dynamic", providerConfig.isDynamic()))
                        .append(getKeyPairs("weight", providerConfig.getWeight()))
                        .append(getKeyPairs("crossLang", providerConfig.getParameter("crossLang")))
                        .append(getKeyPairs("accepts", server.getAccepts()));
                addCommonAttrs(sb);
                urls.add(sb.toString());
            }
            return urls;
        }
        return null;
    }

    /**
     * Convert consumer to url.
     *
     * @param consumerConfig the ConsumerConfig
     * @return the JsfUrl list
     */
    static String convertConsumerToUrl(ConsumerConfig consumerConfig) {
        StringBuilder sb = new StringBuilder(200);
        String host = SystemInfo.getLocalHost();
        sb.append(consumerConfig.getProtocol()).append("://").append(host).append("/")
                .append(consumerConfig.getInterfaceId())
                .append("?tags=").append(consumerConfig.getTags())
                .append(getKeyPairs("pid", BsoaContext.PID))
                //.append(getKeyPairs("randomPort", server.isRandomPort()))
                .append(getKeyPairs("timeout", consumerConfig.getTimeout()))
                .append(getKeyPairs("id", consumerConfig.getId()))
                .append(getKeyPairs("crossLang", consumerConfig.getParameter("crossLang")))
                .append(getKeyPairs("generic", consumerConfig.isGeneric()))
                .append(getKeyPairs("serialization", consumerConfig.getSerialization()));
        addCommonAttrs(sb);
        return sb.toString();
    }

    /**
     * Gets key pairs.
     *
     * @param key   the key
     * @param value the value
     * @return the key pairs
     */
    private static String getKeyPairs(String key, Object value) {
        if (value != null) {
            return "&" + key + "=" + value.toString();
        } else {
            return "";
        }
    }

    /**
     * 加入一些公共的额外属性
     *
     * @param sb 属性
     */
    private static void addCommonAttrs(StringBuilder sb) {
        sb.append(getKeyPairs("startTime", BsoaContext.START_TIME));
        sb.append(getKeyPairs("pid", BsoaContext.PID));
        sb.append(getKeyPairs("language", "java"));
        sb.append(getKeyPairs("bsoaVersion", BsoaVersion.BSOA_VERSION));
        sb.append(getKeyPairs("apppath", (String) BsoaContext.get(BsoaContext.KEY_APPAPTH)));
        sb.append(getKeyPairs(BsoaConstants.CONFIG_KEY_BSOAVERSION, BsoaVersion.BSOA_VERSION + ""));
        if (BsoaContext.get("reg.backfile") != null || BsoaContext.get("provider.backfile") != null) {
            sb.append(getKeyPairs("backfile", "false"));
        }
        putIfContextNotEmpty(sb, "appId");
        putIfContextNotEmpty(sb, "appName");
        putIfContextNotEmpty(sb, "appInsId");
    }

    /**
     * 从上下文中拿到值，如果不为空，放入注册的属性列表中
     *
     * @param sb  属性
     * @param key 关键字
     */
    private static void putIfContextNotEmpty(StringBuilder sb, String key) {
        Object object = BsoaContext.get(key);
        if (object != null) {
            sb.append(getKeyPairs(key, object.toString()));
        }
    }

    /**
     * Convert url to provider list.
     *
     *
     * @param providerPath
     * @param currentData the current data
     * @return the list
     */
    static List<ProviderInfo> convertUrlsToProviders(String providerPath, List<ChildData> currentData)
            throws UnsupportedEncodingException {
        List<ProviderInfo> providerInfos = new ArrayList<>();
        if(CommonUtils.isEmpty(currentData)) {
            return providerInfos;
        }

        for (ChildData childData : currentData) {
            String url = childData.getPath().substring(providerPath.length() + 1); // 去掉头部
            url = URLDecoder.decode(url, "UTF-8");
            byte[] data = childData.getData();
            providerInfos.add(ProviderInfo.valueOf(url));
        }
        return providerInfos;
    }

    static ProviderInfo convertUrlToProvider(String providerPath, ChildData childData)
            throws UnsupportedEncodingException {
        String url = childData.getPath().substring(providerPath.length() + 1); // 去掉头部
        url = URLDecoder.decode(url, "UTF-8");
        byte[] data = childData.getData();
        return ProviderInfo.valueOf(url);
    }

    static String buildProviderPath(String rootPath, AbstractInterfaceConfig config) {
        return rootPath + "bsoa/" + config.getInterfaceId() + "/providers";
    }

    static String buildConsumerPath(String rootPath, AbstractInterfaceConfig config) {
        return rootPath + "bsoa/" + config.getInterfaceId() + "/consumers";
    }

    static String buildConfigPath(String rootPath, AbstractInterfaceConfig config) {
        return rootPath + "bsoa/" + config.getInterfaceId() + "/configs";
    }
}
