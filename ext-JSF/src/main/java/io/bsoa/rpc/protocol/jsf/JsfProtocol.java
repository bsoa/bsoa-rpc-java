/*
 * *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  * <p>
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  * <p>
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package io.bsoa.rpc.protocol.jsf;

import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;
import io.bsoa.rpc.protocol.Protocol;
import io.bsoa.rpc.protocol.ProtocolDecoder;
import io.bsoa.rpc.protocol.ProtocolEncoder;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/18 09:09. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "jsf", code = 1)
public class JsfProtocol implements Protocol {

    private JsfProtocolInfo protocolInfo = new JsfProtocolInfo();

    @Override
    public JsfProtocolInfo protocolInfo() {
        return protocolInfo;
    }


    @Override
    public ProtocolEncoder encoder() {
        ProtocolEncoder encoder = ExtensionLoaderFactory.getExtensionLoader(ProtocolEncoder.class)
                .getExtension("jsf");
        encoder.setProtocolInfo(protocolInfo);
        return encoder;
    }

    @Override
    public ProtocolDecoder decoder() {
        ProtocolDecoder decoder = ExtensionLoaderFactory.getExtensionLoader(ProtocolDecoder.class)
                .getExtension("jsf");
        decoder.setProtocolInfo(protocolInfo);
        return decoder;
    }
}
