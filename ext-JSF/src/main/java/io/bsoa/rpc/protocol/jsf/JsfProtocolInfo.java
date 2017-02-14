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
package io.bsoa.rpc.protocol.jsf;

import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.BsoaOptions;
import io.bsoa.rpc.common.utils.CodecUtils;
import io.bsoa.rpc.protocol.ProtocolInfo;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/17 21:35. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class JsfProtocolInfo extends ProtocolInfo {

    private int maxFrameLength = BsoaConfigs.getOrDefaultValue(
            BsoaOptions.TRANSPORT_PAYLOAD_MAX, 8 * 1024 * 1024); //  最大值默认8M

    private final int lengthFieldOffset = 2;  // 最前面是魔术位2B，然后是长度4B，所以偏移：2

    private final int lengthFieldLength = 4; // 长度占4B，所以长度是：4

    private final int lengthAdjustment = -4; // 总长度计算是包括自己，剩下的长度=总长度-4B 所以调整值是：-4

    private final int initialBytesToStrip = 6; // 前面6位不用再读取了，可以跳过，所以跳过的值是：6

    private final int magicFieldLength = 2; // 魔术位长度2位

    private final int magicFieldOffset = 0; // 魔术位0-2位

    private final byte[] magicCode = new byte[]{(byte) 0xAD, (byte) 0xCF};

    public JsfProtocolInfo() {
        super("jsf", (byte) 1, false, ProtocolInfo.NET_PROTOCOL_TCP); // 是一个变长协议
    }

    @Override
    public int maxFrameLength() {
        return maxFrameLength;
    }

    @Override
    public int lengthFieldOffset() {
        return lengthFieldOffset;
    }

    @Override
    public int lengthFieldLength() {
        return lengthFieldLength;
    }

    @Override
    public int lengthAdjustment() {
        return lengthAdjustment;
    }

    @Override
    public int initialBytesToStrip() {
        return initialBytesToStrip;
    }

    @Override
    public int magicFieldLength() {
        return magicFieldLength;
    }

    @Override
    public int magicFieldOffset() {
        return magicFieldOffset;
    }

    @Override
    public byte[] magicCode() {
        return magicCode;
    }

    @Override
    public boolean isMatchMagic(byte[] bs) {
        return CodecUtils.startsWith(bs, magicCode);
    }
}
