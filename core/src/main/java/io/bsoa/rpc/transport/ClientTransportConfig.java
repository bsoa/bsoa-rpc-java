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
package io.bsoa.rpc.transport;

import io.bsoa.rpc.client.ProviderInfo;
import io.bsoa.rpc.listener.ChannelListener;

import java.util.List;

import static io.bsoa.rpc.common.BsoaConfigs.getBooleanValue;
import static io.bsoa.rpc.common.BsoaConfigs.getIntValue;
import static io.bsoa.rpc.common.BsoaConfigs.getStringValue;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_CONNECTION;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_CONNECT_TIMEOUT;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_DISCONNECT_TIMEOUT;
import static io.bsoa.rpc.common.BsoaOptions.CONSUMER_INVOKE_TIMEOUT;
import static io.bsoa.rpc.common.BsoaOptions.DEFAULT_TRANSPORT;
import static io.bsoa.rpc.common.BsoaOptions.TRANSPORT_PAYLOAD_MAX;
import static io.bsoa.rpc.common.BsoaOptions.TRANSPORT_USE_EPOLL;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/15 23:07. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ClientTransportConfig {

    private ProviderInfo providerInfo; // 对应的Provider信息

    private String container = getStringValue(DEFAULT_TRANSPORT);

    private int connectTimeout = getIntValue(CONSUMER_CONNECT_TIMEOUT);// 默认连接超时时间

    private int disconnectTimeout = getIntValue(CONSUMER_DISCONNECT_TIMEOUT);// 默认断开连接超时时间

    private int invokeTimeout = getIntValue(CONSUMER_INVOKE_TIMEOUT); // 默认的调用超时时间（长连接调用时会被覆盖）

    private int connectionNum = getIntValue(CONSUMER_CONNECTION); // 默认一个地址建立长连接的数量

    private int payload = getIntValue(TRANSPORT_PAYLOAD_MAX); // 最大数据量

    private boolean useEpoll = getBooleanValue(TRANSPORT_USE_EPOLL);

    private List<ChannelListener> channelListeners; // 连接事件监听器

    public ProviderInfo getProviderInfo() {
        return providerInfo;
    }

    public ClientTransportConfig setProviderInfo(ProviderInfo providerInfo) {
        this.providerInfo = providerInfo;
        return this;
    }

    public String getContainer() {
        return container;
    }

    public ClientTransportConfig setContainer(String container) {
        this.container = container;
        return this;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public ClientTransportConfig setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public int getDisconnectTimeout() {
        return disconnectTimeout;
    }

    public ClientTransportConfig setDisconnectTimeout(int disconnectTimeout) {
        this.disconnectTimeout = disconnectTimeout;
        return this;
    }

    public int getInvokeTimeout() {
        return invokeTimeout;
    }

    public ClientTransportConfig setInvokeTimeout(int invokeTimeout) {
        this.invokeTimeout = invokeTimeout;
        return this;
    }

    public int getConnectionNum() {
        return connectionNum;
    }

    public ClientTransportConfig setConnectionNum(int connectionNum) {
        this.connectionNum = connectionNum;
        return this;
    }

    public int getPayload() {
        return payload;
    }

    public ClientTransportConfig setPayload(int payload) {
        this.payload = payload;
        return this;
    }

    public boolean isUseEpoll() {
        return useEpoll;
    }

    public ClientTransportConfig setUseEpoll(boolean useEpoll) {
        this.useEpoll = useEpoll;
        return this;
    }

    public List<ChannelListener> getChannelListeners() {
        return channelListeners;
    }

    public ClientTransportConfig setChannelListeners(List<ChannelListener> channelListeners) {
        this.channelListeners = channelListeners;
        return this;
    }
}
