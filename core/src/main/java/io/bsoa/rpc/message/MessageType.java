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
package io.bsoa.rpc.message;

/**
 * <p></p>
 *
 * Created by zhangg on 2016/12/25 01:28. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class MessageType {

    public final static byte RPC_REQUEST = 1;
    public final static byte RPC_RESPONSE = 2;
    public final static byte HEARTBEAT_REQUEST = 3;
    public final static byte HEARTBEAT_RESPONSE = 4;
    public final static byte NEGOTIATOR_REQUEST = 5;
    public final static byte NEGOTIATOR_RESPONSE = 6;
    public final static byte STREAM_REQUEST = 7;
    public final static byte STREAM_RESPONSE = 8;
}
