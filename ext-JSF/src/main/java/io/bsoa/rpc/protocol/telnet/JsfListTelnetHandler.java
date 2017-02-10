/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bsoa.rpc.protocol.telnet;

import java.lang.reflect.Method;
import java.util.List;

import io.bsoa.rpc.bootstrap.ProviderBootstrap;
import io.bsoa.rpc.common.utils.ClassLoaderUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.protocol.TelnetHandler;
import io.bsoa.rpc.transport.AbstractChannel;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/2/7 00:48. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("jsfList")
public class JsfListTelnetHandler implements TelnetHandler {
    @Override
    public String getCommand() {
        return "ls";
    }

    @Override
    public String getDescription() {
        return "Display all service interface and included methods. " + line +
                "Usage:\tls \t\t\tshow interfaces." + line +
                "\tls -l\t\t\tshow interfaces detail." + line +
                "\tls io.bsoa.XXX\t\tshow methods." + line +
                "\tls -l io.bsoa.XXX\tshow methods detail." + line;
    }

    @Override
    public String telnet(AbstractChannel channel, String message) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isBlank(message)) {// 普通的显示全部接口+alias
            //for (Map.Entry<String, Invoker> entry : BaseServerHandler.getAllInvoker().entrySet()) {
            //    String key = entry.getKey();
            //   sb.append(key).append(line);
            //}
            List<ProviderBootstrap> bootstraps = BsoaContext.getProviderConfigs();
            for (ProviderBootstrap bootstrap : bootstraps) {
                ProviderConfig config = bootstrap.getProviderConfig();
                sb.append(config.getInterfaceId()).append(line);
            }
        } else if ("?".equals(message) || "/?".equals(message)) { // 显示帮助
            return getDescription();
        } else if ("-l".equals(message)) { // 显示详细alias等其它参数？
            List<ProviderBootstrap> bootstraps = BsoaContext.getProviderConfigs();
            for (ProviderBootstrap bootstrap : bootstraps) {
                ProviderConfig config = bootstrap.getProviderConfig();
                List<String> urls = bootstrap.buildUrls();
                if (urls != null) {
                    for (String url : urls) {
                        url = url.replace("tags=", "alias=");
                        sb.append(config.getInterfaceId()).append(" -> ").append(url).append(line);
                    }
                }
            }
        } else { // 显示接口下信息
            if (message.startsWith("-l")) {
                String interfaceId = message.substring(2).trim();
                Class clazz = ClassLoaderUtils.forName(interfaceId);
                sb.append(clazz.getCanonicalName()).append(line);
                Method ms[] = clazz.getMethods();
                for (Method m : ms) {
                    sb.append(m.getReturnType().getCanonicalName()).append(" ").append(m.getName()).append("(");
                    Class[] params = m.getParameterTypes();
                    for (Class p : params) {
                        sb.append(p.getCanonicalName()).append(", ");
                    }
                    if (params.length > 0) {
                        sb.delete(sb.length() - 2, sb.length());
                    }
                    sb.append(")").append(line);
                }
            } else {
                String interfaceId = message.trim();
                Class clazz = ClassLoaderUtils.forName(interfaceId);
                sb.append(clazz.getCanonicalName()).append(line);
                Method ms[] = clazz.getMethods();
                for (Method m : ms) {
                    sb.append(m.getName()).append(line);
                }
            }
        }
        return sb.toString();
    }
}
