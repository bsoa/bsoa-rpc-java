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

import io.bsoa.rpc.common.utils.ClassLoaderUtils;
import io.bsoa.rpc.common.utils.ReflectUtils;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtNewConstructor;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.ByteMemberValue;
import javassist.bytecode.annotation.CharMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.DoubleMemberValue;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.FloatMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.LongMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.ShortMemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Constraint;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.groups.Default;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于javax.validation.Validator的校验器，需要引入官方实现hibernate包
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class Jsr303Validator implements Validator {


    /**
     * slf4j logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(Jsr303Validator.class);

    /**
     * 当前接口
     */
    private final Class clazz;

    /**
     * jsr303实现的Validator类
     */
    private final javax.validation.Validator validator;

    /**
     * 构造函数
     *
     * @param className       当前调用的接口类
     * @param customValidator Validator的实现类，传null默认为javax.validation自动查找，否则按指定来
     * @throws ClassNotFoundException 自定义实现类找不到
     */
    @SuppressWarnings("unchecked")
    public Jsr303Validator(String className, String customValidator) throws ClassNotFoundException {
        this.clazz = ClassLoaderUtils.forName(className);
        javax.validation.ValidatorFactory factory;
        if (customValidator != null && customValidator.length() > 0) {
            factory = Validation.byProvider(ClassLoaderUtils.forName(customValidator))
                    .configure().buildValidatorFactory();
        } else {
            factory = Validation.buildDefaultValidatorFactory();
        }
        this.validator = factory.getValidator();
    }


    @Override
    public void validate(String methodName, String[] parameterTypes, Object[] arguments) throws Exception {
        String methodClassName = clazz.getName() + "_" + toUpperMethodName(methodName);
        Class<?> methodClass = ClassLoaderUtils.forName(methodClassName, false);
        Set<ConstraintViolation<?>> violations = new HashSet<ConstraintViolation<?>>();
        Method method = ReflectUtils.getMethod(clazz, methodName, parameterTypes);
        //clazz.getMethod(methodName, parameterTypes);
        Object parameterBean = getMethodParameterBean(clazz, method, arguments);
        if (parameterBean != null) {
            if (methodClass != null) {
                violations.addAll(validator.validate(parameterBean, Default.class, clazz, methodClass));
            } else {
                violations.addAll(validator.validate(parameterBean, Default.class, clazz));
            }
        }
        for (Object arg : arguments) {
            validate(violations, arg, clazz, methodClass);
        }
        if (violations.size() > 0) {
            throw new ConstraintViolationException("Failed to validate service: " + clazz.getName()
                    + ", method: " + methodName + ", cause: " + violations, violations);
        }
    }

    private void validate(Set<ConstraintViolation<?>> violations, Object arg, Class<?> clazz, Class<?> methodClass) {
        if (arg != null && !ReflectUtils.isPrimitives(arg.getClass())) {
            if (Object[].class.isInstance(arg)) {
                for (Object item : (Object[]) arg) {
                    validate(violations, item, clazz, methodClass);
                }
            } else if (Collection.class.isInstance(arg)) {
                for (Object item : (Collection<?>) arg) {
                    validate(violations, item, clazz, methodClass);
                }
            } else if (Map.class.isInstance(arg)) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) arg).entrySet()) {
                    validate(violations, entry.getKey(), clazz, methodClass);
                    validate(violations, entry.getValue(), clazz, methodClass);
                }
            } else {
                if (methodClass != null) {
                    violations.addAll(validator.validate(arg, Default.class, clazz, methodClass));
                } else {
                    violations.addAll(validator.validate(arg, Default.class, clazz));
                }
            }
        }
    }

    private static final Map<ClassLoader, ClassPool> POOL_MAP = new ConcurrentHashMap<ClassLoader, ClassPool>(); //ClassLoader - ClassPool

    public static ClassPool getClassPool(ClassLoader loader) {
        if (loader == null)
            return ClassPool.getDefault();

        ClassPool pool = POOL_MAP.get(loader);
        if (pool == null) {
            pool = new ClassPool(true);
            pool.appendClassPath(new LoaderClassPath(loader));
            POOL_MAP.put(loader, pool);
        }
        return pool;
    }

    private static Object getMethodParameterBean(Class<?> clazz, Method method, Object[] args) {
        if (!hasConstraintParameter(method)) {
            return null;
        }
        try {
            String upperName = toUpperMethodName(method.getName());
            String parameterSimpleName = upperName + "Parameter";
            String parameterClassName = clazz.getName() + "_" + parameterSimpleName;
            Class<?> parameterClass;
            try {
                parameterClass = ClassLoaderUtils.forName(parameterClassName);
            } catch (BsoaRuntimeException e) {
                ClassPool pool = getClassPool(clazz.getClassLoader());
                CtClass ctClass = pool.makeClass(parameterClassName);
                ClassFile classFile = ctClass.getClassFile();
                classFile.setVersionToJava5();
                ctClass.addConstructor(CtNewConstructor.defaultConstructor(pool.getCtClass(parameterClassName)));
                // parameter fields
                Class<?>[] parameterTypes = method.getParameterTypes();
                Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                for (int i = 0; i < parameterTypes.length; i++) {
                    Class<?> type = parameterTypes[i];
                    Annotation[] annotations = parameterAnnotations[i];
                    AnnotationsAttribute attribute = new AnnotationsAttribute(classFile.getConstPool(),
                            AnnotationsAttribute.visibleTag);
                    for (Annotation annotation : annotations) {
                        if (annotation.annotationType().isAnnotationPresent(Constraint.class)) {
                            javassist.bytecode.annotation.Annotation ja = new javassist.bytecode.annotation.Annotation(
                                    classFile.getConstPool(), pool.getCtClass(annotation.annotationType().getName()));
                            Method[] members = annotation.annotationType().getMethods();
                            for (Method member : members) {
                                if (Modifier.isPublic(member.getModifiers())
                                        && member.getParameterTypes().length == 0
                                        && member.getDeclaringClass() == annotation.annotationType()) {
                                    Object value = member.invoke(annotation, new Object[0]);
                                    if (value != null && !value.equals(member.getDefaultValue())) {
                                        MemberValue memberValue = createMemberValue(
                                                classFile.getConstPool(), pool.get(member.getReturnType().getName()), value);
                                        ja.addMemberValue(member.getName(), memberValue);
                                    }
                                }
                            }
                            attribute.addAnnotation(ja);
                        }
                    }
                    String fieldName = method.getName() + "Argument" + i;
                    CtField ctField = CtField.make("public " + type.getCanonicalName() + " " + fieldName + ";",
                            pool.getCtClass(parameterClassName));
                    ctField.getFieldInfo().addAttribute(attribute);
                    ctClass.addField(ctField);
                }
                parameterClass = ctClass.toClass();
            }
            Object parameterBean = parameterClass.newInstance();
            for (int i = 0; i < args.length; i++) {
                Field field = parameterClass.getField(method.getName() + "Argument" + i);
                field.set(parameterBean, args[i]);
            }
            return parameterBean;
        } catch (Throwable e) {
            LOGGER.warn(e.getMessage(), e);
            return null;
        }
    }

    private static boolean hasConstraintParameter(Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        if (parameterAnnotations != null && parameterAnnotations.length > 0) {
            for (Annotation[] annotations : parameterAnnotations) {
                for (Annotation annotation : annotations) {
                    if (annotation.annotationType().isAnnotationPresent(Constraint.class)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String toUpperMethodName(String methodName) {
        return methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
    }

    // Copy from javassist.bytecode.annotation.Annotation.createMemberValue(ConstPool, CtClass);
    private static MemberValue createMemberValue(ConstPool cp, CtClass type, Object value) throws NotFoundException {
        MemberValue memberValue = javassist.bytecode.annotation.Annotation.createMemberValue(cp, type);
        if (memberValue instanceof BooleanMemberValue)
            ((BooleanMemberValue) memberValue).setValue((Boolean) value);
        else if (memberValue instanceof ByteMemberValue)
            ((ByteMemberValue) memberValue).setValue((Byte) value);
        else if (memberValue instanceof CharMemberValue)
            ((CharMemberValue) memberValue).setValue((Character) value);
        else if (memberValue instanceof ShortMemberValue)
            ((ShortMemberValue) memberValue).setValue((Short) value);
        else if (memberValue instanceof IntegerMemberValue)
            ((IntegerMemberValue) memberValue).setValue((Integer) value);
        else if (memberValue instanceof LongMemberValue)
            ((LongMemberValue) memberValue).setValue((Long) value);
        else if (memberValue instanceof FloatMemberValue)
            ((FloatMemberValue) memberValue).setValue((Float) value);
        else if (memberValue instanceof DoubleMemberValue)
            ((DoubleMemberValue) memberValue).setValue((Double) value);
        else if (memberValue instanceof ClassMemberValue)
            ((ClassMemberValue) memberValue).setValue(((Class<?>) value).getName());
        else if (memberValue instanceof StringMemberValue)
            ((StringMemberValue) memberValue).setValue((String) value);
        else if (memberValue instanceof EnumMemberValue)
            ((EnumMemberValue) memberValue).setValue(((Enum<?>) value).name());
        /* else if (memberValue instanceof AnnotationMemberValue) */
        else if (memberValue instanceof ArrayMemberValue) {
            CtClass arrayType = type.getComponentType();
            int len = Array.getLength(value);
            MemberValue[] members = new MemberValue[len];
            for (int i = 0; i < len; i++) {
                members[i] = createMemberValue(cp, arrayType, Array.get(value, i));
            }
            ((ArrayMemberValue) memberValue).setValue(members);
        }
        return memberValue;
    }
}