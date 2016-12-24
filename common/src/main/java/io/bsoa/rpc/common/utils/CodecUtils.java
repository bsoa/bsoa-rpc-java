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
package io.bsoa.rpc.common.utils;

/**
 * <p></p>
 *
 * Created by zhangg on 2016/12/24 13:02. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class CodecUtils {


    /**
     * int 转 byte数组
     *
     * @param num
     *         int值
     * @return byte[4]
     */
    public static byte[] intToBytes(int num) {
        byte[] result = new byte[4];
        result[0] = (byte) (num >>> 24);//取最高8位放到0下标
        result[1] = (byte) (num >>> 16);//取次高8为放到1下标
        result[2] = (byte) (num >>> 8); //取次低8位放到2下标
        result[3] = (byte) (num);      //取最低8位放到3下标
        return result;
    }

    /**
     * byte数组转int
     *
     * @param ary
     *         byte[4]
     * @return int值
     */
    public static int bytesToInt(byte[] ary) {
        return (ary[3] & 0xFF)
                | ((ary[2] << 8) & 0xFF00)
                | ((ary[1] << 16) & 0xFF0000)
                | ((ary[0] << 24) & 0xFF000000);
    }

    /**
     * short 转 byte数组
     *
     * @param num
     *         short值
     * @return byte[2]
     */
    public static byte[] short2bytes(short num) {
        byte[] result = new byte[2];
        result[0] = (byte) (num >>> 8); //取次低8位放到0下标
        result[1] = (byte) (num);      //取最低8位放到1下标
        return result;
    }


    /**
     * byte array copy.
     *
     * @param src
     *         src.
     * @param length
     *         new length.
     * @return new byte array.
     */
    public static byte[] copyOf(byte[] src, int length) {
        byte[] dest = new byte[length];
        System.arraycopy(src, 0, dest, 0, Math.min(src.length, length));
        return dest;
    }

}
