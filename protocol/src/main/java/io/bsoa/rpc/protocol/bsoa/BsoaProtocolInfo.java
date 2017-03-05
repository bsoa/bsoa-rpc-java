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

import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.BsoaOptions;
import io.bsoa.rpc.common.utils.CodecUtils;
import io.bsoa.rpc.protocol.ProtocolInfo;

/**
 * <p>根据魔术位和总长度，自动截取剩下的一个完整数据帧（Frame）</p>
 * <p>
 * Created by zhangg on 2016/12/18 08:37. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class BsoaProtocolInfo extends ProtocolInfo {

    private int maxFrameLength = BsoaConfigs.getOrDefaultValue(
            BsoaOptions.TRANSPORT_PAYLOAD_MAX, 20 * 1024 * 1024); //  最大值默认20M

    private final int lengthFieldOffset = 2;  // 最前面是魔术位2B，然后是长度4B，所以偏移：2

    private final int lengthFieldLength = 4; // 长度占4B，所以长度是：4

    private final int lengthAdjustment = -6; // 总长度计算是包括自己和魔术位，剩下的长度=总长度-2B  所以调整值是：-2

    private final int initialBytesToStrip = 2; // 前面2位魔术位不要了，可以跳过，所以跳过的值是：2

    private final int magicFieldLength = 2; // 魔术位长度2位

    private final int magicFieldOffset = 0; // 魔术位0-2位

    private final byte[] magicCode = new byte[]{(byte) 0x08, (byte) 0x0a}; // 2个不可见字符

    public BsoaProtocolInfo() {
        super("bsoa", (byte) 10, false, NET_PROTOCOL_TCP);// 是一个变长协议
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
