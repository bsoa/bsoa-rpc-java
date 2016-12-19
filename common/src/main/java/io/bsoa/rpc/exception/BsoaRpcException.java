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
 * 调用时异常，
 *
 * Created by zhanggeng on 16-6-7.
 *
 * @author <a href=mailto:ujjboy@qq.com>Geng Zhang</a>
 */
public final class BsoaRpcException extends RuntimeException {

    private static final long serialVersionUID = 8951353731374236729L;

    private int code = ErrorCode.UNKNOW; // use code to case type

    private BsoaRpcException() {

    }

    public BsoaRpcException(String message) {
        super(message);
    }

    public BsoaRpcException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BsoaRpcException(int code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public BsoaRpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public BsoaRpcException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
