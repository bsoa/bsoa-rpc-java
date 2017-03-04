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
package io.bsoa.rpc.config;

import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.ExceptionUtils;

import java.io.Serializable;

/**
 * Created by zhangg on 16-7-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class ParameterConfig implements Serializable {

    private static final long serialVersionUID = -8438415130253334898L;

    /**
     * 关键字
     */
    private String key;

    /**
     * 值
     */
    private String value;

    /**
     * 是否隐藏（是的话，业务代码不能获取到）
     */
    private boolean hide = false;

    /**
     * Gets key.
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets key.
     *
     * @param key the key
     */
    public void setKey(String key) {
        if (!isValidParamKey(key)) {
            throw ExceptionUtils.buildRuntime(21800, "param.key", key, "key can not start with "
                    + BsoaConstants.HIDE_KEY_PREFIX + " and " + BsoaConstants.INTERNAL_KEY_PREFIX);
        }
        this.key = key;
    }

    /**
     * Gets value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets value.
     *
     * @param value the value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Is hide.
     *
     * @return the boolean
     */
    public boolean isHide() {
        return hide;
    }

    /**
     * Sets hide.
     *
     * @param hide the hide
     */
    public void setHide(boolean hide) {
        this.hide = hide;
    }

    /**
     * 自定义的key是否合法
     *
     * @param paramkey 参数key
     * @return 是否合法
     */
    public static boolean isValidParamKey(String paramkey) {
        char c = paramkey.charAt(0);
        return c != BsoaConstants.HIDE_KEY_PREFIX && c != BsoaConstants.INTERNAL_KEY_PREFIX;
    }

    /**
     * 自定义的key是否内置key
     *
     * @param paramkey 参数key
     * @return 是否合法
     */
    public static boolean isInternalParamKey(String paramkey) {
        char c = paramkey.charAt(0);
        return c == BsoaConstants.INTERNAL_KEY_PREFIX;
    }
}
