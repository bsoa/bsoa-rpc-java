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
package io.bsoa.rpc.exception;

/**
 * 除调用异常外的运行时异常，包括配置加载异常，初始化异常等
 * <p>
 * Created by zhangg on 2016/7/13 20:57.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public final class BsoaRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 8050652255280202869L;

    private int code = ErrorCode.UNKNOW;

    private BsoaRuntimeException() {

    }

    public BsoaRuntimeException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BsoaRuntimeException(int code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public BsoaRuntimeException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
    
    public String getMessage() {
        return "[bsoa-" + code + "]" + super.getMessage();
    }
}
