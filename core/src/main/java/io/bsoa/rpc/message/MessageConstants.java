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
package io.bsoa.rpc.message;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/25 01:28. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class MessageConstants {

    /**
     * 消息类型：RPC调用请求
     */
    public static final byte RPC_REQUEST = 1;
    /**
     * 消息类型：RPC调用请求
     */
    public static final byte RPC_RESPONSE = 2;
    /**
     * 消息类型：心跳调用请求
     */
    public static final byte HEARTBEAT_REQUEST = 3;
    /**
     * 消息类型：心跳调用响应
     */
    public static final byte HEARTBEAT_RESPONSE = 4;
    /**
     * 消息类型：协商调用请求
     */
    public static final byte NEGOTIATOR_REQUEST = 5;
    /**
     * 消息类型：协商调用响应
     */
    public static final byte NEGOTIATOR_RESPONSE = 6;

    /**
     * 调用方向：双向，需要对方回应
     */
    public static final byte DIRECTION_FORWARD = 0;
    /**
     * 调用方向：单向，无需对方回应
     */
    public static final byte DIRECTION_ONEWAY = 1;

    /**
     * 默认无协议
     */
    public static final byte PROTOCOL_NONE = 0;

    /**
     * 默认无协议
     */
    public static final byte SERIALIZATION_NONE = 0;

    /**
     * 压缩默认不开启
     */
    public static final byte COMPRESS_NONE = 0;
}
