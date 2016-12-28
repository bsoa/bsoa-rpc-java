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
package io.bsoa.rpc.protocol.h2c;

import io.bsoa.rpc.protocol.ProtocolInfo;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/28 23:51. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class H2CProtocolInfo extends ProtocolInfo {

    /**
     * Instantiates a new Protocol info.
     */
    public H2CProtocolInfo() {
        super("h2c", (byte) 12, false, NET_PROTOCOL_TCP);
    }

    @Override
    public int maxFrameLength() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int lengthFieldOffset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int lengthFieldLength() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int lengthAdjustment() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int initialBytesToStrip() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int magicFieldLength() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int magicFieldOffset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] magicCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMatchMagic(byte[] bs) {
        throw new UnsupportedOperationException();
    }
}
