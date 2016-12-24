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

/**
 * <p></p>
 *
 * Created by zhangg on 2016/12/24 22:56. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public final class CompressorFactory {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(CompressorFactory.class);

    private final static ExtensionLoader<Compressor> extensionLoader
            = ExtensionLoaderFactory.getExtensionLoader(Compressor.class);

    private final static ConcurrentHashMap<Object, Compressor> COMPRESSOR_MAP = new ConcurrentHashMap<>();

    /**
     * 按序列化名称返回协议对象
     *
     * @param alias
     * @return 序列化器
     */
    public static Compressor getCompressor(String alias) {
        // 工厂模式 自己维护不托管给ExtensionLoader
        Compressor compressor = COMPRESSOR_MAP.get(alias);
        if (compressor == null) {
            synchronized (CompressorFactory.class) {
                compressor = COMPRESSOR_MAP.get(alias);
                if (compressor == null) {
                    LOGGER.info("Init compressor : {}", alias);
                    compressor = extensionLoader.getExtension(alias);
                    byte code = compressor.getCode();
                    if (COMPRESSOR_MAP.containsKey(code)) {
                        throw new BsoaRuntimeException(22222, "Duplicate compressor with same code!");
                    }
                    COMPRESSOR_MAP.put(alias, compressor);
                    COMPRESSOR_MAP.put(code, compressor);
                }
            }
        }
        return compressor;
    }

    public static Compressor getCompressor(byte type) {
        return COMPRESSOR_MAP.get(type);
    }

}
