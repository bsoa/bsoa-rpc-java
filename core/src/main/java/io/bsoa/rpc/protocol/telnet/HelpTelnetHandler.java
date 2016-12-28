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
package io.bsoa.rpc.protocol.telnet;

import java.util.Map;

import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.protocol.TelnetHandler;
import io.bsoa.rpc.protocol.TelnetHandlerFactory;
import io.bsoa.rpc.transport.AbstractChannel;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/28 23:45. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("help")
public class HelpTelnetHandler implements TelnetHandler {

    @Override
    public String getCommand() {
        return "help";
    }

    @Override
    public String telnet(AbstractChannel channel, String message) {
        StringBuffer result = new StringBuffer();
        if (StringUtils.isNotBlank(message)) {
            TelnetHandler handler = TelnetHandlerFactory.getHandler(message);
            if (handler != null) {
                result.append(handler.getCommand()).append(line)
                        .append(handler.getDescription()).append(line);
            } else {
                result.append("Not found command : " + message);
            }
        } else {
            result.append("The supported command include:").append(line);
            for (Map.Entry<String, TelnetHandler> entry : TelnetHandlerFactory.getAllHandlers().entrySet()) {
                result.append(entry.getKey()).append(" ");
                //result.append(entry.getKey() + "\t : " + entry.getValue().getDescription() + "\r\n");
            }
            result.append(line);
        }
        return result.toString();
    }

    @Override
    public String getDescription() {
        return "show all commands infomation!" + line + "Usage:\thelp" + line + "\thelp [cmd]";
    }

}
