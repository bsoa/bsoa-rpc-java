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
 *
 * Created by zhangg on 2016/12/15 23:19. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public interface BsoaChannel {

    /**
     * 得到连接的远端地址
     *
     * @return the remote address
     */
    InetSocketAddress getRemoteAddress();

    /**
     * 得到连接的本地地址（如果是短连接，可能不准）
     *
     * @return the local address
     */
    InetSocketAddress getLocalAddress();

}
