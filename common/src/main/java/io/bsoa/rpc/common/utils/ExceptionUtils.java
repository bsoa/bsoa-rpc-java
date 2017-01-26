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
package io.bsoa.rpc.common.utils;

import io.bsoa.rpc.exception.BsoaRuntimeException;

/**
 * 异常构造器
 *
 * Created by zhangg on 16-6-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public final class ExceptionUtils {

    public static BsoaRuntimeException buildRuntime(int code, String configKey, String configValue) {
        String msg = "The value of config " + configKey + " [" + configValue + "] is illegal, please check it";
        return new BsoaRuntimeException(code, msg);
    }

    public static BsoaRuntimeException buildRuntime(int code, String configKey, String configValue, String message) {
        String msg = "The value of config " + configKey + " [" + configValue + "] is illegal, " + message;
        return new BsoaRuntimeException(code, msg);
    }

    /**
     * 返回堆栈信息（e.printStackTrace()的内容）
     *
     * @param e
     *         Throwable
     * @return 异常堆栈信息
     */
    public static String toString(Throwable e) {
        StackTraceElement[] traces = e.getStackTrace();
        StringBuilder sb = new StringBuilder(1024);
        sb.append(e.toString()).append("\n");
        if (traces != null) {
            for (StackTraceElement trace : traces) {
                sb.append("\tat ").append(trace).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 返回消息+简短堆栈信息（e.printStackTrace()的内容）
     *
     * @param e
     *         Throwable
     * @param stackLevel
     *         堆栈层级
     * @return 异常堆栈信息
     */
    public static String toShortString(Throwable e, int stackLevel) {
        StackTraceElement[] traces = e.getStackTrace();
        StringBuilder sb = new StringBuilder(1024);
        sb.append(e.toString()).append("\t");
        if (traces != null) {
            for (int i = 0; i < traces.length; i++) {
                if (i < stackLevel) {
                    sb.append("\tat ").append(traces[i]).append("\t");
                } else {
                    break;
                }
            }
        }
        return sb.toString();
    }

//    /**
//     * 封装RpcException
//     *
//     * @param header
//     *         消息头
//     * @param throwable
//     *         异常
//     * @return RpcException
//     */
//    public static BsoaRpcException handlerException(MessageHeader header,Throwable throwable){
//        BsoaRpcException exception = null;
//        if(throwable instanceof BsoaRpcException){
//            exception = (BsoaRpcException) throwable;
//            if(header != null) exception.setMsgHeader(header);
//
//        }else{
//            exception = new BsoaRpcException(header,throwable);
//
//        }
//        return  exception;
//    }

}
