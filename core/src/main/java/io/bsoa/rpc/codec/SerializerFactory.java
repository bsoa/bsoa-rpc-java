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
package io.bsoa.rpc.codec;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.ExtensionLoader;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;
import io.bsoa.rpc.protocol.ProtocolFactory;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/24 21:09. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public final class SerializerFactory {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(SerializerFactory.class);

    private final static ExtensionLoader<Serializer> extensionLoader
            = ExtensionLoaderFactory.getExtensionLoader(Serializer.class);

    private final static ConcurrentHashMap<Object, Serializer> SERIALIZER_MAP = new ConcurrentHashMap<>();

    /**
     * 按序列化名称返回协议对象
     *
     * @param alias
     * @return 序列化器
     */
    public static Serializer getSerializer(String alias) {
        // 工厂模式 自己维护不托管给ExtensionLoader
        Serializer serializer = SERIALIZER_MAP.get(alias);
        if (serializer == null) {
            synchronized (SerializerFactory.class) {
                serializer = SERIALIZER_MAP.get(alias);
                if (serializer == null) {
                    LOGGER.info("Init protocol : {}", alias);
                    serializer = extensionLoader.getExtension(alias);
                    byte code = serializer.getCode();
                    if (SERIALIZER_MAP.containsKey(code)) {
                        throw new BsoaRuntimeException(22222, "Duplicate protocol with same code!");
                    }
                    SERIALIZER_MAP.put(alias, serializer);
                    SERIALIZER_MAP.put(code, serializer);
                }
            }
        }
        return serializer;
    }

    public static Serializer getSerializer(byte type) {
        return SERIALIZER_MAP.get(type);
    }

}
