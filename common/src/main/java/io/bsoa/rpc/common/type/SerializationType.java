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
package io.bsoa.rpc.common.type;

/**
 * Created by zhangg on 2016/7/14 21:27.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Deprecated
public enum SerializationType {
    @Deprecated dubbo(1),
    hessian(2),
    java(3),
    @Deprecated compactedjava(4),
    json(5),
    @Deprecated fastjson(6),
    @Deprecated nativejava(7),
    @Deprecated kryo(8),
    msgpack(10),
    @Deprecated nativemsgpack(11),
    protobuf(12);

    private byte value;

    private SerializationType(int mvalue) {
        this.value = (byte) mvalue;
    }

    public byte value() {
        return value;
    }

    public static SerializationType valueOf(int value) {
        SerializationType p;
        switch (value) {
            case 10:
                p = msgpack;
                break;
            case 2:
                p = hessian;
                break;
            case 3:
                p = java;
                break;
            case 12:
                p = protobuf;
                break;
            case 5:
                p = json;
                break;
            case 1:
                p = dubbo;
                break;
            case 11:
                p = nativemsgpack;
                break;
            case 4:
                p = compactedjava;
                break;
            case 6:
                p = fastjson;
                break;
            case 7:
                p = nativejava;
                break;
            case 8:
                p = kryo;
                break;
            default:
                throw new IllegalArgumentException("Unknown codec type value: " + value);
        }
        return p;
    }
}
