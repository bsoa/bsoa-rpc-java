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
package io.bsoa.rpc.protocol;

import io.bsoa.rpc.ext.ExtensionLoader;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/29 00:26. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class TelnetHandlerFactory {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(TelnetHandlerFactory.class);

    /**
     * 保存支持的全部命令，{命令：解析器}
     */
    private static Map<String, TelnetHandler> supportedCmds = new ConcurrentHashMap<String, TelnetHandler>();

    /**
     * 扩展器
     */
    private static ExtensionLoader<TelnetHandler> extensionLoader =
            ExtensionLoaderFactory.getExtensionLoader(TelnetHandler.class, extensionClass -> {
                // 自己维护支持列表，不托管给ExtensionLoaderFactory
                TelnetHandler handler = extensionClass.getExtInstance();
                supportedCmds.put(handler.getCommand(), handler);
            });

    public static TelnetHandler getHandler(String command) {
        return supportedCmds.get(command);
    }

    public static Map<String, TelnetHandler> getAllHandlers() {
        return supportedCmds;
    }
}
