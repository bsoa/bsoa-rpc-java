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
package io.bsoa.rpc.common.type;

/**
 *
 *
 * Created by zhangg on 2016/7/14 21:29.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public enum HeadKey {

    timeout((byte) 1, Integer.class), // 请求超时时间
    //interfaceId((byte) 2, String.class),
    //alias((byte) 3, String.class),
    //methodName((byte) 4, String.class),
    callbackInsId((byte) 5, String.class), // 回调函数对应的实例id
    //compress((byte) 6, String.class),
    jsfVersion((byte) 7, Short.class), // 客户端的JSF版本
    srcLanguage((byte) 8, Byte.class), // 请求的语言（针对跨语言 1c++ 2lua）
    responseCode((byte) 9, Byte.class), // 返回结果（针对跨语言 0成功 1失败）
    ;

    private byte keyNum;
    private Class type;


    private HeadKey(byte b, Class clazz) {
        this.keyNum = b;
        this.type = clazz;
    }

    public byte getNum() {
        return this.keyNum;
    }

    public Class getType() {
        return this.type;
    }

    public static HeadKey getKey(byte num) {
        HeadKey key = null;
        switch (num) {
            case 1:
                key = timeout;
                break;
                /*case 2:
                    key = interfaceId;
                    break;
                case 3:
                    key = alias;
                    break;
                case 4:
                    key = methodName;
                    break;*/
            case 5:
                key = callbackInsId;
                break;
                /*case 6:
                    key = compress;
                    break;*/
            case 7:
                key = jsfVersion;
                break;
            case 8:
                key = srcLanguage;
                break;
            case 9:
                key = responseCode;
                break;
            default:
                throw new IllegalArgumentException("Unknown head key value: " + num);
        }
        return key;

    }

}
