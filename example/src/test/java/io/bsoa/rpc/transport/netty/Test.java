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
package io.bsoa.rpc.transport.netty;

import java.util.Arrays;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/25 01:54. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class Test {

    public static void printbytebuf() {
//        byte[] bytes = new byte[out.readableBytes()];
//        out.readBytes(bytes);
//        LOGGER.debug(Arrays.toString(bytes));
//        out.readerIndex(0);
    }

    /**
     * 将byte转换为一个长度为8的byte数组，数组每个值代表bit
     */
    public static byte[] getBooleanArray(byte b) {
        byte[] array = new byte[8];
        for (int i = 7; i >= 0; i--) {
            array[i] = (byte) (b & 1);
            b = (byte) (b >> 1);
        }
        return array;
    }

    /**
     * 把byte转为字符串的bit
     */
    public static String byteToBit(byte b) {
        return ""
                + (byte) ((b >> 7) & 0x01) + (byte) ((b >> 6) & 0x1)
                + (byte) ((b >> 5) & 0x01) + (byte) ((b >> 4) & 0x1)
                + (byte) ((b >> 3) & 0x01) + (byte) ((b >> 2) & 0x1)
                + (byte) ((b >> 1) & 0x01) + (byte) ((b >> 0) & 0x1);
    }

    public static String byteToBit2(byte b) {
        return ""
                + (byte) ((b & 0x8f)) + " "
                + (byte) ((b & 0x80)) + " "
                + (byte) ((b & 0x0f)) + " "
                + (byte) (b % 0x08);
    }

    public static String byteToBit3(byte b) {
        return "11 00 00 00 "
                + (byte) ((b >> 6)) + " "
                + (byte) ((b & 0x3f)) + " "
                + (byte) ((b & 0xf0)) + " "
                + (byte) ((((b << 2) & 0x01 )>> 2));

    }

    public static void main(String[] args) {
        byte b = 117; // 0011 0101  53  -- 1 3 1 1  - 1  53
        // 输出 [0, 1, 1, 1, 0, 1, 0, 1]
        System.out.println(Arrays.toString(getBooleanArray(b)));
        // 输出 00110101
        System.out.println(byteToBit(b));
        System.out.println();
        System.out.println(byteToBit2(b));
        System.out.println();
        System.out.println(byteToBit3(b));
        // JDK自带的方法，会忽略前面的 0
        System.out.println(Integer.toBinaryString(0x35));
    }
}
