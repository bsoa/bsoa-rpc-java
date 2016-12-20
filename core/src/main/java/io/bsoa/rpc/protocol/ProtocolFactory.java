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
package io.bsoa.rpc.protocol;

import java.util.concurrent.ConcurrentHashMap;

import io.bsoa.rpc.common.type.ProtocolType;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.ExtensionLoader;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;
import io.bsoa.rpc.protocol.ProtocolInfo.MagicCode;

/**
 * Created by zhangg on 2016/7/17 14:39.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ProtocolFactory {

    private final static ExtensionLoader<Protocol> extensionLoader
            = ExtensionLoaderFactory.getExtensionLoader(Protocol.class);

    /**
     * 按协议名称返回协议对象
     *
     * @param alias
     * @return
     */
    public static Protocol getProtocol(String alias) {
        // 工厂模式 托管给ExtensionLoader
        return extensionLoader.getExtension(alias);
    }

    public static Protocol getProtocol(ProtocolType type) {
        return extensionLoader.getExtension(type.name());
    }

    private static int maxMagicOffset = 2; // 最大偏移量，用于一个端口支持多协议时使用

    private static ConcurrentHashMap<MagicCode, String> MAGIC_PROTOCOL_MAP = new ConcurrentHashMap<>();

    public static synchronized void registerAdaptive(ProtocolInfo protocolInfo) {
        if (MAGIC_PROTOCOL_MAP == null) {
            MAGIC_PROTOCOL_MAP = new ConcurrentHashMap<>();
        }
        // 取最大偏移量
        maxMagicOffset = Math.max(protocolInfo.magicFieldOffset(), maxMagicOffset + protocolInfo.magicFieldOffset());
        String old = MAGIC_PROTOCOL_MAP.putIfAbsent(protocolInfo.magicCode(), protocolInfo.name());
        if (old != null && !old.equals(protocolInfo.name())) {
            throw new BsoaRuntimeException(22222, "Same magic code with different protocol!");
        }
    }
}
