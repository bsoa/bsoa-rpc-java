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
 * Created by zhangg on 2016/7/14 21:26.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Deprecated
public enum ProtocolType {

    bsoa(0),
    @Deprecated saf(1),
    jsf(1),
    rest(2),
    dubbo(3),
    webservice(4),
    jaxws(5),
    @Deprecated jaxrs(6),
    @Deprecated hessian(7),
    @Deprecated thrift(8),
    http(9);

    private byte value;

    private ProtocolType(int mvalue) {
        this.value = (byte) mvalue;
    }

    public byte value() {
        return value;
    }

    public static ProtocolType valueOf(int value) {
        ProtocolType p;
        switch (value) {
            case 0:
                p = bsoa;
                break;
            case 1:
                p = jsf;
                break;
            case 2:
                p = rest;
                break;
            case 9:
                p = http;
                break;
            case 3:
                p = dubbo;
                break;
            case 4:
                p = webservice;
                break;
            case 5:
                p = jaxws;
                break;
            case 6:
                p = jaxrs;
                break;
            case 7:
                p = hessian;
                break;
            case 8:
                p = thrift;
                break;
            default:
                throw new IllegalArgumentException("Unknown protocol type value: " + value);
        }
        return p;
    }
}
