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
package io.bsoa.rpc.listener;

import io.bsoa.rpc.message.NegotiatorRequest;
import io.bsoa.rpc.message.NegotiatorResponse;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/22 22:43. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public interface NegotiatorListener {

    /**
     * 处理握手请求（服务端和客户端都可以互发）
     *
     * @param negotiatorRequest 协商请求
     * @return 协商响应
     */
    NegotiatorResponse handshake(NegotiatorRequest negotiatorRequest);
}
