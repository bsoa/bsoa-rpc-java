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
package io.bsoa.rpc.protocol;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/17 20:12. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public abstract class ProtocolInfo {

    /**
     * 底层通讯协议：TCP
     */
    public final static byte NET_PROTOCOL_TCP = 0;
    /**
     * 底层通讯协议：UDP
     */
    public final static byte NET_PROTOCOL_UDP = 1;
    /**
     * 底层通讯协议：HTTP
     */
    public final static byte NET_PROTOCOL_HTTP = 2;

    /**
     * 协议名称
     */
    protected final String name;

    /**
     * 协议ID
     */
    protected final byte code;

    /**
     *
     */
    protected final byte netProtocol;

    /**
     * 是否定长协议
     */
    protected final boolean lengthFixed;

    /**
     * Instantiates a new Protocol info.
     *
     * @param name        the name
     * @param code        the code
     * @param lengthFixed the length fixed
     */
    public ProtocolInfo(String name, byte code, boolean lengthFixed, byte netProtocol) {
        this.name = name;
        this.code = code;
        this.lengthFixed = lengthFixed;
        this.netProtocol = netProtocol;
    }

    /**
     * 返回协议名
     *
     * @return 协议名 name
     */
    public String getName() {
        return name;
    }

    /**
     * 返回协议ID
     *
     * @return 协议ID code
     */
    public byte getCode() {
        return code;
    }

    /**
     * 返回协议底层通讯协议
     *
     * @return
     */
    public byte getNetProtocol() {
        return netProtocol;
    }

    /**
     * 协议是否固定长度，true定长，false变长
     *
     * @return true定长 ，false变长
     */
    public boolean isLengthFixed() {
        return lengthFixed;
    }

    /**
     * 最大帧长度，变长时使用
     *
     * @return 最大帧长度 int
     */
    public abstract int maxFrameLength();

    /**
     * 找到“保存长度的字段”的偏移位，变长时使用
     *
     * @return 长度字段的偏移位 int
     */
    public abstract int lengthFieldOffset();

    /**
     * “保存长度的字段”的长度，定长变长都适应
     *
     * @return 长度字段的长度 int
     */
    public abstract int lengthFieldLength();

    /**
     * 总长度调整位，变长时使用
     *
     * @return 总长度调整位 int
     */
    public abstract int lengthAdjustment();

    /**
     * 跳过读取的位数，变长时使用
     *
     * @return 跳过读取的位数 int
     */
    public abstract int initialBytesToStrip();

    /**
     * 魔术位字段长度，用于协议自适应
     *
     * @return 魔术位字段长度 int
     */
    public abstract int magicFieldLength();

    /**
     * 魔术位偏移量，用于协议自适应
     *
     * @return 魔术位偏移量 int
     */
    public abstract int magicFieldOffset();

    /**
     * 返回魔术位信息
     *
     * @return 魔术位字节数组
     */
    public abstract byte[] magicCode();

    /**
     * 是否命中魔术位
     *
     * @return 魔术位的值 magic code
     */
    public abstract boolean isMatchMagic(byte[] bs);
}
