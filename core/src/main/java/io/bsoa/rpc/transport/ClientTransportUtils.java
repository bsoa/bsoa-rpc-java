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
package io.bsoa.rpc.transport;

import java.net.InetSocketAddress;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/1/13 01:32. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ClientTransportUtils {

    public static InetSocketAddress remoteAddress(ClientTransport transport) {
        AbstractChannel channel = transport.getChannel();
        return channel == null ? null : channel.getRemoteAddress();
    }

    public static InetSocketAddress localAddress(ClientTransport transport) {
        AbstractChannel channel = transport.getChannel();
        return channel == null ? null : channel.getLocalAddress();
    }
}
