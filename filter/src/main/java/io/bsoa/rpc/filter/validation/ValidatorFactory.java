/**
 * ValidatorFactory.java Created on 2014/5/7 16:50
 * <p>
 * Copyright (c) 2014 by www.jd.com.
 */
package io.bsoa.rpc.filter.validation;

import java.util.concurrent.ConcurrentHashMap;
import javax.validation.ValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.exception.BsoaRuntimeException;

/**
 * Title: 校验器生成工厂<br>
 * <p/>
 * Description: <br>
 * <p/>
 * Company: <a href=www.jd.com>京东</a><br>
 *
 * @author <a href=mailto:zhanggeng@jd.com>章耿</a>
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
     * @param className
     *         the interface name
     * @param customImpl
     *         the custom impl
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