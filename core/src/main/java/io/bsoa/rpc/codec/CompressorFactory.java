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

import io.bsoa.rpc.ext.ExtensionLoader;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/24 22:56. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public final class CompressorFactory {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(CompressorFactory.class);

    /**
     * 除了托管给扩展加载器的工厂模式（保留alias：实例）外<br>
     * 还需要额外保留编码和实例的映射：{编码：压缩器}
     */
    private final static ConcurrentHashMap<Byte, Compressor> TYPE_COMPRESSOR_MAP = new ConcurrentHashMap<>();
    /**
     * 除了托管给扩展加载器的工厂模式（保留alias：实例）外<br>
     * 还需要额外保留编码和实例的映射：{别名：编码}
     */
    private final static ConcurrentHashMap<String, Byte> TYPE_CODE_MAP = new ConcurrentHashMap<>();

    /**
     * 扩展加载器
     */
    private final static ExtensionLoader<Compressor> extensionLoader
            = ExtensionLoaderFactory.getExtensionLoader(Compressor.class, extensionClass -> {
        // 除了保留 tag：Compressor外， 需要保留 code：Compressor
        TYPE_COMPRESSOR_MAP.put(extensionClass.getCode(), extensionClass.getExtInstance());
        TYPE_CODE_MAP.put(extensionClass.getAlias(), extensionClass.getCode());
    });

    /**
     * 按压缩算法名称返回协议对象
     *
     * @param alias 压缩算法
     * @return Compressor
     */
    public static Compressor getCompressor(String alias) {
        // 工厂模式  托管给ExtensionLoader
        return extensionLoader.getExtension(alias);
    }

    /**
     * 按压缩编码返回协议对象
     *
     * @param code Compressor编码
     * @return Compressor
     */
    public static Compressor getCompressor(byte code) {
        return TYPE_COMPRESSOR_MAP.get(code);
    }

    /**
     * 通过别名获取Code
     *
     * @param compress 压缩名字
     * @return 压缩编码
     */
    public static byte getCodeByAlias(String compress) {
        return TYPE_CODE_MAP.get(compress);
    }
}
