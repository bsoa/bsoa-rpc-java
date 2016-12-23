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
package io.bsoa.rpc.config;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.exception.BsoaRuntimeException;

/**
 * 配置检查器
 *
 * Created by zhangg on 2016/7/13 20:51.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ConfigValueHelper {
    /**
     * 可用的字符串为：英文大小写，数字，横杆-，下划线_，点.
     * !@#$*,;:有特殊含义
     */
    protected final static Pattern NORMAL = Pattern.compile("^[a-zA-Z0-9\\-\\_\\.]+$");

    /**
     * 可用的字符串为：英文大小写，数字，横杆-，下划线_，点. 逗号,
     * !@#$*;:有特殊含义
     */
    protected final static Pattern NORMAL_COMMA = Pattern.compile("^[a-zA-Z0-9\\-\\_\\.,]+$");

    /**
     * 可用的字符串为：英文大小写，数字，横杆-，下划线_，点. 冒号:
     * !@#$*,;有特殊含义
     */
    protected final static Pattern NORMAL_COLON = Pattern.compile("^[a-zA-Z0-9\\-\\_\\.:]+$");

    /**
     * 可用的字符串为：英文大小写，数字，横杆-，下划线_，点. 分号;
     * !@#$*,;有特殊含义
     */
    protected final static Pattern NORMAL_SEMICOLON = Pattern.compile("^[a-zA-Z0-9\\-\\_\\.;]+$");

    /**
     * 可用的字符串为：英文大小写，数字，横杆-，下划线_，点. 逗号, 冒号:
     * !@#$*,;有特殊含义
     */
    protected final static Pattern NORMAL_COMMA_COLON = Pattern.compile("^[a-zA-Z0-9\\-\\_\\.,:]+$");

    /**
     * 可用的字符串为：英文大小写，数字，横杆-，下划线_，点. 分号; 冒号:
     * !@#$*,;有特殊含义
     */
    protected final static Pattern NORMAL_SEMICOLON_COLON = Pattern.compile("^[a-zA-Z0-9\\-\\_\\.;:]+$");

    /**
     * 判断字符串是否为空或"false"或"null"
     *
     * @param string
     *         字符串
     * @return 是否为空或"false"或"null"
     */
    protected static boolean assertFalse(String string) {
        return string == null || "".equals(string)
                || "false".equalsIgnoreCase(string) || "null".equals(string);
    }

    /**
     * 匹配正常字符串
     *
     * @param configValue
     *         配置项
     * @return 是否匹配，否表示有其他字符
     */
    protected static boolean match(Pattern pattern, String configValue) {
        return pattern.matcher(configValue).find();
    }

    /**
     * 检查字符串是否是正常值，不是则抛出异常
     *
     * @param configKey
     *         配置项
     * @param configValue
     *         配置值
     * @throws BsoaRuntimeException
     *         非法异常
     */
    protected static void checkNormal(String configKey, String configValue) throws BsoaRuntimeException {
        checkPattern(21003, configKey, configValue, NORMAL, "only allow a-zA-Z0-9 '-' '_' '.'");
    }

    /**
     * 检查字符串是否是正常值（含逗号），不是则抛出异常
     *
     * @param configKey
     *         配置项
     * @param configValue
     *         配置值
     * @throws BsoaRuntimeException
     *         非法异常
     */
    protected static void checkNormalWithComma(String configKey, String configValue) throws BsoaRuntimeException {
        checkPattern(21004, configKey, configValue, NORMAL_COMMA, "only allow a-zA-Z0-9 '-' '_' '.' ','");
    }

    /**
     * 检查字符串是否是正常值（含冒号），不是则抛出异常
     *
     * @param configKey
     *         配置项
     * @param configValue
     *         配置值
     * @throws BsoaRuntimeException
     *         非法异常
     */
    protected static void checkNormalWithColon(String configKey, String configValue) throws BsoaRuntimeException {
        checkPattern(21005, configKey, configValue, NORMAL_COLON, "only allow a-zA-Z0-9 '-' '_' '.' ':'");
    }

    /**
     * 检查字符串是否是正常值（含冒号），不是则抛出异常
     *
     * @param configKey
     *         配置项
     * @param configValue
     *         配置值
     * @throws BsoaRuntimeException
     *         非法异常
     */
    protected static void checkNormalWithCommaColon(String configKey, String configValue) throws BsoaRuntimeException {
        checkPattern(21006, configKey, configValue, NORMAL_COMMA_COLON, "only allow a-zA-Z0-9 '-' '_' '.' ':' ','");
    }

    /**
     * 根据正则表达式检查字符串是否是正常值（含冒号），不是则抛出异常
     *
     * @param errCode
     *         错误码
     * @param configKey
     *         配置项
     * @param configValue
     *         配置值
     * @param pattern
     *         正则表达式
     * @param message
     *         消息
     * @throws BsoaRuntimeException
     */
    protected static void checkPattern(int errCode, String configKey, String configValue, Pattern pattern, String message)
            throws BsoaRuntimeException {
        if (configValue != null && !match(pattern, configValue)) {
            throw ExceptionUtils.buildRuntime(errCode, configKey, configValue, message);
        }
    }

    /**
     * 检查数字是否为正整数（>0)
     *
     * @param configKey
     *         配置项
     * @param configValue
     *         配置值
     * @throws BsoaRuntimeException
     *         非法异常
     */
    protected static void checkPositiveInteger(String configKey, int configValue) throws BsoaRuntimeException {
        if (configValue <= 0) {
            throw ExceptionUtils.buildRuntime(21001, configKey, configValue + "", "must > 0");
        }
    }

    /**
     * 检查数字是否为非负数（>=0)
     *
     * @param configKey
     *         配置项
     * @param configValue
     *         配置值
     * @throws BsoaRuntimeException
     *         非法异常
     */
    protected static void checkNonnegativeInteger(String configKey, int configValue) throws BsoaRuntimeException {
        if (configValue < 0) {
            throw ExceptionUtils.buildRuntime(21002, configKey, configValue + "", "must >= 0");
        }
    }

    /**
     * Id生成器
     */
    private final static AtomicInteger ID_GENERATOR = new AtomicInteger(0);

//    public static void check(ProtocolType protocolType, SerializationType serializationType) {
//
//    }
}
