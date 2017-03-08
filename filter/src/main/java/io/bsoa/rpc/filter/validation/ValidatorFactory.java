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
package io.bsoa.rpc.filter.validation;

import io.bsoa.rpc.exception.BsoaRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ValidationException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 校验器生成工厂<br>
 * <p>
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ValidatorFactory {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ValidatorFactory.class);

    /**
     * The validator cache.
     */
    private static ConcurrentHashMap<String, Validator> validatorMap = new ConcurrentHashMap<String, Validator>();

    /**
     * Gets validator.
     *
     * @param className  the interface name
     * @param customImpl the custom impl
     * @return the validator
     */
    public static Validator getValidator(String className, String customImpl) {

        Validator validator = validatorMap.get(className);
        if (validator == null) {
            try {
                LOGGER.info("build validator for {}", className);
                validator = new Jsr303Validator(className, customImpl);
                Validator vd = validatorMap.putIfAbsent(className, validator);
                if (vd != null) {
                    validator = vd;
                }
            } catch (ValidationException e) {
                throw new BsoaRuntimeException(22222, "The ValidatorFactory cannot be built", e);
            } catch (ClassNotFoundException e) {
                throw new BsoaRuntimeException(2222, e.getMessage(), e);
            }
        }
        return validator;
    }
}