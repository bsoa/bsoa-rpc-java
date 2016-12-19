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
package io.bsoa.rpc.transport;

import java.util.List;

import io.bsoa.rpc.client.Provider;
import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.listener.ChannelListener;

import static io.bsoa.rpc.common.BsoaConfigs.CLIENT_CONNECT_TIMEOUT;
import static io.bsoa.rpc.common.BsoaConfigs.CLIENT_DISCONNECT_TIMEOUT;
import static io.bsoa.rpc.common.BsoaConfigs.CLIENT_INVOKE_TIMEOUT;
import static io.bsoa.rpc.common.BsoaConfigs.CONSUMER_CONNECTION_NUM;
import static io.bsoa.rpc.common.BsoaConfigs.DEFAULT_TRANSPORT;
import static io.bsoa.rpc.common.BsoaConfigs.TRANSPORT_PAYLOAD_MAX;
import static io.bsoa.rpc.common.BsoaConfigs.getIntValue;
import static io.bsoa.rpc.common.BsoaConfigs.getStringValue;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/15 23:07. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ClientTransportConfig {

    private Provider provider; // 对应的Provider信息

    private String container = getStringValue(DEFAULT_TRANSPORT);

    private int connectTimeout = getIntValue(CLIENT_CONNECT_TIMEOUT);// 默认连接超时时间

    private int disconnectTimeout = getIntValue(CLIENT_DISCONNECT_TIMEOUT);// 默认断开连接超时时间

    private int invokeTimeout = getIntValue(CLIENT_INVOKE_TIMEOUT); // 默认的调用超时时间（长连接调用时会被覆盖）

    private int connectionNum = getIntValue(CONSUMER_CONNECTION_NUM); // 默认一个地址建立长连接的数量

    private int payload = getIntValue(TRANSPORT_PAYLOAD_MAX); // 最大数据量

    private boolean useEpoll = BsoaConfigs.getBooleanValue(BsoaConfigs.TRANSPORT_USE_EPOLL);

    private List<ChannelListener> channelListeners; // 连接事件监听器

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getDisconnectTimeout() {
        return disconnectTimeout;
    }

    public void setDisconnectTimeout(int disconnectTimeout) {
        this.disconnectTimeout = disconnectTimeout;
    }

    public int getInvokeTimeout() {
        return invokeTimeout;
    }

    public void setInvokeTimeout(int invokeTimeout) {
        this.invokeTimeout = invokeTimeout;
    }

    public int getConnectionNum() {
        return connectionNum;
    }

    public void setConnectionNum(int connectionNum) {
        this.connectionNum = connectionNum;
    }

    public int getPayload() {
        return payload;
    }

    public void setPayload(int payload) {
        this.payload = payload;
    }

    public boolean isUseEpoll() {
        return useEpoll;
    }

    public void setUseEpoll(boolean useEpoll) {
        this.useEpoll = useEpoll;
    }

    public List<ChannelListener> getChannelListeners() {
        return channelListeners;
    }

    public void setChannelListeners(List<ChannelListener> channelListeners) {
        this.channelListeners = channelListeners;
    }
}
