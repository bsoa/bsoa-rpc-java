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

import io.bsoa.rpc.common.struct.TwoWayMap;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.exception.BsoaRuntimeException;

/**
 * <p>每个长连接都对应一个上下文，例如客户端本地存着服务端版本，服务端存着客户端的APP信息等</p>
 * <p>
 * Created by zhangg on 2017/03/2017/3/4 18:13. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ChannelContext {

    /**
     * 每个长连接独立的一个缓存，最多256条
     * TODO 是否分将缓存 全局静态区和动态区
     */
    protected volatile TwoWayMap<Byte, String> headerCache;
    /**
     * 对方版本
     */
    protected int dstVersion;
    /**
     * 客户端应用Id
     */
    protected String clientAppId;
    /**
     * 客户端应用名称
     */
    protected String clientAppName;
    /**
     * 客户端应用实例id
     */
    protected String clientInstanceId;
    /**
     * 长连接的协议
     */
    private String protocol;

    /**
     * Put header cache
     *
     * @param key   the key
     * @param value the value
     */
    public void putHeadCache(Byte key, String value) {
        if (headerCache == null) {
            synchronized (this) {
                if (headerCache == null) {
                    headerCache = new TwoWayMap<>();
                }
            }
        }
        if (headerCache != null && !headerCache.containsKey(key)) {
            if (headerCache.size() >= 255) {
                throw new BsoaRuntimeException(22222, "Cache of channel is full! size >= 255");
            }
            headerCache.put(key, value);
        }
    }
    
    public Byte getAvailableRefIndex() {
        if (headerCache == null) {
            synchronized (this) {
                if (headerCache == null) {
                    headerCache = new TwoWayMap<>();
                }
            }
        }
        if (headerCache.size() >= 255) {
            return null;
        }
        for (byte i = Byte.MIN_VALUE; i < Byte.MAX_VALUE; i++) {
            if (!headerCache.containsKey(i)) {
                return i;
            }
        }
        return null;
    }

    /**
     * Invalidate head cache.
     *
     * @param key   the key
     * @param value the value
     */
    public void invalidateHeadCache(Byte key, String value) {
        if (headerCache!=null && headerCache.containsKey(key)) {
            String old = headerCache.get(key);
            if (!old.equals(value)) {
                throw new BsoaRuntimeException(22222, "Value of old is not match current");
            }
            headerCache.remove(key);
        }
    }

    /**
     * Gets header.
     *
     * @param key the key
     * @return the header
     */
    public String getHeader(Byte key) {
        if (key != null && headerCache!=null) {
            return headerCache.get(key);
        }
        return null;
    }

    /**
     * Gets header.
     *
     * @param value the value
     * @return the header
     */
    public Byte getHeaderKey(String value) {
        if (StringUtils.isNotEmpty(value) && headerCache!=null) {
            return headerCache.getKey(value);
        }
        return null;
    }

    /**
     * Gets dst version.
     *
     * @return the dst version
     */
    public int getDstVersion() {
        return dstVersion;
    }

    /**
     * Sets dst version.
     *
     * @param dstVersion the dst version
     * @return the dst version
     */
    public ChannelContext setDstVersion(int dstVersion) {
        this.dstVersion = dstVersion;
        return this;
    }

    /**
     * Gets client app id.
     *
     * @return the client app id
     */
    public String getClientAppId() {
        return clientAppId;
    }

    /**
     * Sets client app id.
     *
     * @param clientAppId the client app id
     * @return the client app id
     */
    public ChannelContext setClientAppId(String clientAppId) {
        this.clientAppId = clientAppId;
        return this;
    }

    /**
     * Gets client app name.
     *
     * @return the client app name
     */
    public String getClientAppName() {
        return clientAppName;
    }

    /**
     * Sets client app name.
     *
     * @param clientAppName the client app name
     * @return the client app name
     */
    public ChannelContext setClientAppName(String clientAppName) {
        this.clientAppName = clientAppName;
        return this;
    }

    /**
     * Gets client instance id.
     *
     * @return the client instance id
     */
    public String getClientInstanceId() {
        return clientInstanceId;
    }

    /**
     * Sets client instance id.
     *
     * @param clientInstanceId the client instance id
     * @return the client instance id
     */
    public ChannelContext setClientInstanceId(String clientInstanceId) {
        this.clientInstanceId = clientInstanceId;
        return this;
    }

    /**
     * Sets protocol.
     *
     * @param protocol the protocol
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Gets protocol.
     *
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }
}
