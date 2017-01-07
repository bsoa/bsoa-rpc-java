/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bsoa.rpc.client;

import java.io.Serializable;

import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.StringUtils;

/**
 *
 *
 * Created by zhangg on 2016/7/14 22:42.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class Provider implements Serializable {

    private static final long serialVersionUID = -6438690329875954051L;

    /**
     * The Ip.
     */
    private String ip;

    /**
     * The Port.
     */
    private int port = 80;

    /**
     * The Protocol type.
     */
    private String protocolType = BsoaConfigs.getStringValue(BsoaConfigs.DEFAULT_PROTOCOL);

    /**
     * 判断服务端codec兼容性，以服务端的为准
     */
    private String serializationType;

    /**
     * The Weight.
     */
    private int weight = BsoaConstants.DEFAULT_PROVIDER_WEIGHT;

    /**
     * The Saf version.
     */
    private int safVersion;

    /**
     * The Jsf Version
     */
    @Deprecated
    private int jsfVersion;

    /**
     * The tags.
     */
    private String tags;

    /**
     * The path
     */
    private String path;

    /**
     * The Interface id.
     */
    private String interfaceId;

    /**
     * 启用invocation优化？
     */
    private transient volatile boolean invocationOptimizing;

    /**
     * 重连周期系数：1-5（即5次才真正调一次）
     */
    private transient int reconnectPeriodCoefficient = 1;

    /**
     * Instantiates a new Provider.
     */
    public Provider() {

    }

    /**
     * Instantiates a new Provider.
     *
     * @param host the host
     * @param port the port
     */
    private Provider(String host, int port){
        this.ip = host;
        this.port = port;
    }

    /**
     * Get provider.
     *
     * @param host the host
     * @param port the port
     * @return the provider
     */
    public static Provider getProvider(String host, int port){
        return new Provider(host,port);
    }

    /**
     * Instantiates a new Provider.
     *
     * @param url the url
     */
    private Provider(final String url) {
        try {
            int protocolIndex = url.indexOf("://");
            String remainUrl;
            if (protocolIndex > -1) {
                String protocol = url.substring(0, protocolIndex).toLowerCase();
                this.setProtocolType(protocol);
                remainUrl = url.substring(protocolIndex + 3);
            } else { // 默认
//                this.setProtocolType(BsoaConstants.DEFAULT_PROTOCOL_TYPE);
                remainUrl = url;
            }

            int addressIndex = remainUrl.indexOf("/");
            String address;
            if (addressIndex > -1) {
                address = remainUrl.substring(0, addressIndex);
                remainUrl = remainUrl.substring(addressIndex + 1);
            } else {
                int itfIndex = remainUrl.indexOf("?");
                if (itfIndex > -1) {
                    address = remainUrl.substring(0, itfIndex);
                    remainUrl = remainUrl.substring(itfIndex);
                } else {
                    address = remainUrl;
                    remainUrl = "";
                }
            }
            String[] ipport = address.split(":", -1);
            this.setIp(ipport[0]);
            this.setPort(Integer.valueOf(ipport[1]));

            // 后面可以解析remainUrl得到interface等 /xxx?a=1&b=2
            if (remainUrl.length() > 0) {
                int itfIndex = remainUrl.indexOf("?");
                if (itfIndex > -1) {
                    String itf = remainUrl.substring(0, itfIndex);
                    this.setPath(itf);
                    // 剩下是params,例如a=1&b=2
                    remainUrl = remainUrl.substring(itfIndex + 1);
                    String[] params = remainUrl.split("&", -1);
                    for(String parm: params){
                        String[] kvpair = parm.split("=", -1);
                        if (BsoaConstants.CONFIG_KEY_WEIGHT.equals(kvpair[0]) && StringUtils.isNotEmpty(kvpair[1])) {
                            this.setWeight(Integer.valueOf(kvpair[1]));
                        }
                        if (BsoaConstants.CONFIG_KEY_SAFVERSION.equals(kvpair[0]) && StringUtils.isNotEmpty(kvpair[1])) {
                            this.setSafVersion(Integer.valueOf(kvpair[1]));
                        }
                        if (BsoaConstants.CONFIG_KEY_JSFVERSION.equals(kvpair[0]) && StringUtils.isNotEmpty(kvpair[1])) {
                            this.setJsfVersion(Integer.valueOf(kvpair[1]));
                        }
                        if (BsoaConstants.CONFIG_KEY_INTERFACE.equals(kvpair[0]) && StringUtils.isNotEmpty(kvpair[1])) {
                            this.setInterfaceId(kvpair[1]);
                        }
                        if (BsoaConstants.CONFIG_KEY_ALIAS.equals(kvpair[0]) && StringUtils.isNotEmpty(kvpair[1])) {
                            this.setTags(kvpair[1]);
                        }
                        if (BsoaConstants.CONFIG_KEY_SERIALIZATION.equals(kvpair[0]) && StringUtils.isNotEmpty(kvpair[1])) {
                            this.setSerializationType(kvpair[1]);
                        }
                    }
                } else {
                    String itf = remainUrl;
                    this.setPath(itf);
                }
            } else {
                this.setPath(StringUtils.EMPTY);
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert url to provider, the wrong url is:" + url, e);
        }
    }

    /**
     * 从thrift://10.12.120.121:9090 得到Provider
     *
     * @param url url地址
     * @return Provider对象 provider
     */
    public static Provider valueOf(String url) {
        return new Provider(url);
    }

    /**
     * Gets protocol type.
     *
     * @return the protocol type
     */
    public String getProtocolType() {
        return protocolType;
    }

    /**
     * Sets protocol type.
     *
     * @param protocolType the protocol type
     */
    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    /**
     * Gets codec type.
     *
     * @return the serializationType
     */
    public String getSerializationType() {
        return serializationType;
    }

    /**
     * Sets codec type.
     *
     * @param serializationType the serializationType to set
     */
    public void setSerializationType(String serializationType) {
        this.serializationType = serializationType;
    }

    /**
     * Gets saf version.
     *
     * @return the saf version
     */
    public int getSafVersion() {
        return safVersion;
    }

    /**
     * Sets saf version.
     *
     * @param safVersion the saf version
     */
    public void setSafVersion(int safVersion) {
        this.safVersion = safVersion;
    }

    /**
     * Gets jsf version.
     *
     * @return the jsf version
     */
    public int getJsfVersion() {
        return jsfVersion;
    }

    /**
     * Sets jsf version.
     *
     * @param jsfVersion the jsf version
     */
    public void setJsfVersion(int jsfVersion) {
        this.jsfVersion = jsfVersion;
    }

    /**
     * Gets ip.
     *
     * @return the ip
     */
    public String getIp() {
        return ip;
    }

    /**
     * Sets ip.
     *
     * @param ip the ip
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * Gets port.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets port.
     *
     * @param port the port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Gets weight.
     *
     * @return the weight
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Sets weight.
     *
     * @param weight the weight to set
     */
    public void setWeight(int weight) {
        this.weight = weight;
    }

    /**
     * Gets tags.
     *
     * @return the tags
     */
    public String getTags() {
        return tags;
    }

    /**
     * Sets tags.
     *
     * @param tags the tags
     */
    public void setTags(String tags) {
        this.tags = tags;
    }

    /**
     * Gets interface id.
     *
     * @return the interface id
     */
    public String getInterfaceId() {
        return interfaceId;
    }

    /**
     * Sets interface id.
     *
     * @param interfaceId the interface id
     */
    public void setInterfaceId(String interfaceId) {
        this.interfaceId = interfaceId;
    }

    /**
     * Gets path.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets path.
     *
     * @param path the path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * 序列化到url.
     *
     * @return the string
     */
    public String toUrl(){
        String uri = protocolType + "://" + ip + ":" + port + "/" + StringUtils.trimToEmpty(path);
        StringBuilder sb = new StringBuilder();
        if (weight != BsoaConstants.DEFAULT_PROVIDER_WEIGHT) {
            sb.append("&").append(BsoaConstants.CONFIG_KEY_WEIGHT).append("=").append(weight);
        }
        if (safVersion > 0) {
            sb.append("&").append(BsoaConstants.CONFIG_KEY_SAFVERSION).append("=").append(safVersion);
        }
        if (jsfVersion > 0) {
            sb.append("&").append(BsoaConstants.CONFIG_KEY_JSFVERSION).append("=").append(jsfVersion);
        }
        if (interfaceId != null) {
            sb.append("&").append(BsoaConstants.CONFIG_KEY_INTERFACE).append("=").append(interfaceId);
        }
        if (tags != null) {
            sb.append("&").append(BsoaConstants.CONFIG_KEY_ALIAS).append("=").append(tags);
        }
        if (serializationType != null) {
            sb.append("&").append(BsoaConstants.CONFIG_KEY_SERIALIZATION).append("=").append(serializationType);
        }
        if(sb.length() > 0){
            uri += sb.replace(0, 1, "?").toString();
        }
        return uri;
    }

    /**
     * 重写toString方法
     *
     * @return 字符串 string
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return toUrl();
    }

    /**
     * Equals boolean.
     *
     * @param o the o
     * @return the boolean
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Provider)) return false;

        Provider provider = (Provider) o;

        if (port != provider.port) return false;
        if (tags != null ? !tags.equals(provider.tags) : provider.tags != null) return false;
        if (ip != null ? !ip.equals(provider.ip) : provider.ip != null) return false;
        if (interfaceId != null ? !interfaceId.equals(provider.interfaceId) : provider.interfaceId != null) return false;
        if (path != null ? !path.equals(provider.path) : provider.path != null) return false;
        if (protocolType != provider.protocolType) return false;
        if (weight != provider.weight) return false;

        return true;
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        int result = ip != null ? ip.hashCode() : 0;
        result = 31 * result + port;
        result = 31 * result + (protocolType != null ? protocolType.hashCode() : 0);
        result = 31 * result + (interfaceId != null ? interfaceId.hashCode() : 0);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + weight;
        return result;
    }

    /**
     * Open invocation optimizing.
     *
     * @return the boolean
     */
    public boolean openInvocationOptimizing() {
        return invocationOptimizing;
    }

    /**
     * Sets invocation optimizing.
     *
     * @param invocationOptimizing  the invocation optimizing
     */
    public void setInvocationOptimizing(boolean invocationOptimizing) {
        this.invocationOptimizing = invocationOptimizing;
    }

    /**
     * Gets reconnect period coefficient.
     *
     * @return the reconnect period coefficient
     */
    public int getReconnectPeriodCoefficient() {
        // 最大是5
        reconnectPeriodCoefficient = Math.min(5, reconnectPeriodCoefficient);
        return reconnectPeriodCoefficient;
    }

    /**
     * Sets reconnect period coefficient.
     *
     * @param reconnectPeriodCoefficient  the reconnect period coefficient
     */
    public void setReconnectPeriodCoefficient(int reconnectPeriodCoefficient) {
        // 最小是1
        reconnectPeriodCoefficient = Math.max(1, reconnectPeriodCoefficient);
        this.reconnectPeriodCoefficient = reconnectPeriodCoefficient;
    }
}
