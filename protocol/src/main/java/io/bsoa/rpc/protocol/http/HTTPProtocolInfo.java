/*
 * Copyright © 2016-2017 The BSOA Project
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
package io.bsoa.rpc.protocol.http;

import io.bsoa.rpc.protocol.ProtocolInfo;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/28 23:45. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class HTTPProtocolInfo extends ProtocolInfo {
    /**
     * Instantiates a new Protocol info.
     */
    public HTTPProtocolInfo() {
        super("http", (byte) 9, false, NET_PROTOCOL_HTTP);
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
        return 3;
    }

    @Override
    public int magicFieldOffset() {
        return 0;
    }

    @Override
    public byte[] magicCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMatchMagic(byte[] bs) {
        byte magic1 = bs[0];
        byte magic2 = bs[1];
        return (magic1 == 'G' && magic2 == 'E') || // GET
                (magic1 == 'P' && magic2 == 'O') || // POST
                (magic1 == 'P' && magic2 == 'U') || // PUT
                (magic1 == 'H' && magic2 == 'E') || // HEAD
                (magic1 == 'O' && magic2 == 'P') || // OPTIONS
                (magic1 == 'P' && magic2 == 'A') || // PATCH
                (magic1 == 'D' && magic2 == 'E') || // DELETE
                (magic1 == 'T' && magic2 == 'R') || // TRACE
                (magic1 == 'C' && magic2 == 'O');   // CONNECT
    }
}
