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
package io.bsoa.rpc.message;

/**
 * 头部关键字以及类型
 * <p>
 * Created by zhangg on 2016/7/14 21:29.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public enum HeadKey {
    /**
     * 请求超时时间
     */
    TIMEOUT((byte) 1, Integer.class),
    /**
     * 接口名：实际名称
     */
    INTERFACE_NAME((byte) 2, String.class),
    /**
     * 分组名称：实际名称
     */
    TAGS((byte) 3, String.class),
    /**
     * 方法名称：实际名称
     */
    METHOD_NAME((byte) 4, String.class),
    /**
     * 回调函数实例
     */
    CALLBACK_INS_KEY((byte) 5, String.class), // 回调函数对应的实例id
    /**
     * 方法参数类型：实际名称
     */
    @Deprecated
    METHOD_ARG_TYPES((byte) 6, String.class),
    /**
     * rpc客户端版本，协商阶段可以决定，无需重复传递
     */
    @Deprecated
    BSOA_VERSION((byte) 7, Short.class),
    /**
     * 请求的语言（针对跨语言 1c++ 2lua）
     */
    srcLanguage((byte) 8, Byte.class),
    /**
     * 返回结果（针对跨语言 0成功 1失败）
     */
    responseCode((byte) 9, Byte.class),

    /**
     * 接口名：映射值
     */
    INTERFACE_NAME_REF((byte) 12, Void.class),
    /**
     * 分组名称：映射值
     */
    GROUP_REF((byte) 13, Byte.class),
    /**
     * 方法名称：映射值
     */
    METHOD_NAME_REF((byte) 14, Byte.class),
    /**
     * 方法参数类型：映射值
     */
    @Deprecated
    METHOD_ARG_TYPES_REF((byte) 15, String.class),
    /**
     * 回调函数实例
     */
    STREAM_INS_KEY((byte) 16, String.class), // 回调函数对应的实例id
    ;

    /**
     * 编码
     */
    private final byte code;

    /**
     * <p>支持 byte/short/long/boolean/(代表是个映射）</p>
     * <p>bsoa协议中如下定义：<br/>
     * void   : 1位key+1位标识(0)+1位ref值<br/>
     * int    : 1位key+1位标识(1)+4位值<br/>
     * String : 1位key+1位标识(2)+2位长度+N位值<br/>
     * byte   : 1位key+1位标识(3)+1位值<br/>
     * short  : 1位key+1位标识(4)+2位值<br/>
     * long   : 1位key+1位标识(5)+8位值<br/>
     * boolean: 1位key+1位标识(6)+1位值<br/>
     * </p>
     */
    private final Class valueType;

    HeadKey(byte code, Class valueType) {
        this.code = code;
        this.valueType = valueType;
    }

    /**
     * Head编码
     *
     * @return 编码
     */
    public byte getCode() {
        return this.code;
    }

    /**
     * Head值类型
     *
     * @return 值类型
     */
    public Class getValueType() {
        return this.valueType;
    }

    /**
     * 通过code获取HeadKey
     *
     * @param code 编码
     * @return HeadKey
     */
    public static HeadKey valueOf(byte code) {
        HeadKey key = null;
        switch (code) {
            case 1:
                key = TIMEOUT;
                break;
            case 2:
                key = INTERFACE_NAME;
                break;
            case 3:
                key = TAGS;
                break;
            case 4:
                key = METHOD_NAME;
                break;
            case 6:
                key = METHOD_ARG_TYPES;
                break;
            case 5:
                key = CALLBACK_INS_KEY;
                break;
            case 7:
                key = BSOA_VERSION;
                break;
            case 8:
                key = srcLanguage;
                break;
            case 9:
                key = responseCode;
                break;
            case 12:
                key = INTERFACE_NAME_REF;
                break;
            case 13:
                key = GROUP_REF;
                break;
            case 14:
                key = METHOD_NAME_REF;
                break;
            case 15:
                key = METHOD_ARG_TYPES_REF;
                break;
            default:
                throw new IllegalArgumentException("Unknown head key code: " + code);
        }
        return key;
    }
}
