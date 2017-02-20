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
package io.bsoa.rpc.protocol.bsoa;

import io.bsoa.rpc.client.ProviderInfo;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.listener.NegotiationListener;
import io.bsoa.rpc.protocol.ProtocolNegotiator;
import io.bsoa.rpc.transport.ClientTransport;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/2/20 21:44. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("bsoa")
public class BsoaProtocolNegotiator implements ProtocolNegotiator{
    @Override
    public boolean handshake(ProviderInfo providerInfo, ClientTransport clientTransport) {
        return false;
    }

    @Override
    public boolean addListener(NegotiationListener negotiationListener) {
        return false;
    }
}
