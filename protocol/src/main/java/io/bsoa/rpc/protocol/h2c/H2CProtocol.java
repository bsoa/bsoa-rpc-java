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
package io.bsoa.rpc.protocol.h2c;

import io.bsoa.rpc.protocol.Protocol;
import io.bsoa.rpc.protocol.ProtocolDecoder;
import io.bsoa.rpc.protocol.ProtocolEncoder;
import io.bsoa.rpc.protocol.ProtocolInfo;
import io.bsoa.rpc.protocol.ProtocolNegotiator;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/28 23:49. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
// TODO
// @Extension(value = "h2c", code = 10)
public class H2CProtocol implements Protocol {

    private H2CProtocolInfo protocolInfo = new H2CProtocolInfo();

    @Override
    public ProtocolInfo protocolInfo() {
        return protocolInfo;
    }

    @Override
    public ProtocolEncoder encoder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProtocolDecoder decoder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProtocolNegotiator negotiator() {
        return null; // 无需协商
    }
}
