/*
 * Copyright 2016 The BSOA Project
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
package io.bsoa.rpc.protocol.bsoa;

import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.protocol.Protocol;
import io.bsoa.rpc.protocol.ProtocolDecoder;
import io.bsoa.rpc.protocol.ProtocolEncoder;
import io.bsoa.rpc.protocol.ProtocolInfo;
import io.bsoa.rpc.protocol.ProtocolNegotiator;

import javax.annotation.concurrent.ThreadSafe;

import static io.bsoa.rpc.ext.ExtensionLoaderFactory.getExtensionLoader;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/17 18:37. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "bsoa", code = 10)
@ThreadSafe
public class BsoaProtocol implements Protocol {

    private final BsoaProtocolInfo protocolInfo;

    private final ProtocolEncoder encoder;

    private final ProtocolDecoder decoder;

    private final ProtocolNegotiator protocolNegotiator;

    public BsoaProtocol() {
        protocolInfo = new BsoaProtocolInfo();
        encoder = getExtensionLoader(ProtocolEncoder.class)
                .getExtension("bsoa", new Class[]{ProtocolInfo.class}, new Object[]{protocolInfo});
        decoder = getExtensionLoader(ProtocolDecoder.class)
                .getExtension("bsoa", new Class[]{ProtocolInfo.class}, new Object[]{protocolInfo});
        protocolNegotiator = getExtensionLoader(ProtocolNegotiator.class).getExtension("bsoa");
    }

    @Override
    public ProtocolInfo protocolInfo() {
        return protocolInfo;
    }

    @Override
    public ProtocolEncoder encoder() {
        return encoder;
    }

    @Override
    public ProtocolDecoder decoder() {
        return decoder;
    }

    @Override
    public ProtocolNegotiator negotiator() {
        return protocolNegotiator;
    }
}
