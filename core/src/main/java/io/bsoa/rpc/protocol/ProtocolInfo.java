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
     * 协议名称
     */
    protected final String name;

    /**
     * 协议ID
     */
    protected final byte code;

    /**
     * 是否定长协议
     */
    protected final boolean lengthFixed;

    public ProtocolInfo(String name, byte code, boolean lengthFixed) {
        this.name = name;
        this.code = code;
        this.lengthFixed = lengthFixed;
    }

    /**
     * 返回协议名
     *
     * @return 协议名
     */
    public String getName() {
        return name;
    }

    /**
     * 返回协议ID
     *
     * @return 协议ID
     */
    public byte getCode() {
        return code;
    }

    /**
     * 协议是否固定长度，true定长，false变长
     *
     * @return true定长，false变长
     */
    public boolean isLengthFixed() {
        return lengthFixed;
    }

    /**
     * 最大帧长度，变长时使用
     *
     * @return 最大帧长度
     */
    public abstract int maxFrameLength();

    /**
     * 找到“保存长度的字段”的偏移位，变长时使用
     *
     * @return 长度字段的偏移位
     */
    public abstract int lengthFieldOffset();

    /**
     * “保存长度的字段”的长度，定长变长都适应
     *
     * @return 长度字段的长度
     */
    public abstract int lengthFieldLength();

    /**
     * 总长度调整位，变长时使用
     *
     * @return 总长度调整位
     */
    public abstract int lengthAdjustment();

    /**
     * 跳过读取的位数，变长时使用
     *
     * @return 跳过读取的位数
     */
    public abstract int initialBytesToStrip();

    /**
     * 魔术位字段长度，用于协议自适应
     *
     * @return 魔术位字段长度
     */
    public abstract int magicFieldLength();

    /**
     * 魔术位偏移量，用于协议自适应
     *
     * @return 魔术位偏移量
     */
    public abstract int magicFieldOffset();

    /**
     * 魔术位
     *
     * @return 魔术位的值
     */
    public abstract MagicCode getMagicCode();

    public static class MagicCode {
        /**
         * 魔术为字符串
         */
        private final byte[] bytes;

        public MagicCode(byte[] bytes) {
            this.bytes = bytes;
        }

        public static MagicCode valueOf(byte... codes) {
            return new MagicCode(codes);
        }

        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public int hashCode() {
            // null 和空数组一致
            int result = bytes != null ? bytes.length : 0;
            if (bytes != null) {
                for (int i = 0; i < bytes.length; i++) {
                    result = 31 * result + bytes[i];
                }
            }
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            return this.hashCode() == obj.hashCode();
        }
    }
}
