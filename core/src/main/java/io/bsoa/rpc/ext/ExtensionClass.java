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
package io.bsoa.rpc.ext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.utils.ClassUtils;

/**
 * Created by zhangg on 2016/7/14 21:57.
 *
 * @param <T> the type parameter
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 * @see Extension
 * @see Extensible
 */
public class ExtensionClass<T> {

    /**
     * slf4j Logger for this class
     */
    private static final Logger logger = LoggerFactory.getLogger(ExtensionClass.class);

    /**
     * The Alias.
     * 扩展别名,不是provider tags
     */
    protected String alias;
    /**
     * 扩展编码，必须唯一
     */
    protected byte code;
    /**
     * 是否单例
     */
    protected boolean singleton;
    /**
     * 扩展接口实现类名
     */
    protected Class<? extends T> clazz;
    /**
     * 扩展点排序值
     */
    protected int order;
//    /**
//     * 是否自动激活该扩展
//     */
//    protected boolean autoActive;
//    /**
//     * 服务提供者端是否自动激活
//     */
//    protected boolean providerSide;
//    /**
//     * 服务调用端是否自动激活
//     */
//    protected boolean consumerSide;

    /**
     * 服务端实例对象（只在是单例的时候保留）
     */
    private volatile transient T instance;

    /**
     * 得到服务端实例对象，如果是单例则返回单例对象，如果不是则返回新创建的实例对象
     *
     * @return 扩展点对象实例
     */
    public T getExtInstance() {
        if (clazz != null) {
            try {
                if (singleton) { // 如果是单例
                    if (instance == null) {
                        synchronized (this) {
                            if (instance == null) {
                                instance = ClassUtils.newInstance(clazz);
                            }
                        }
                    }
                    return instance; // 保留单例
                } else {
                    return ClassUtils.newInstance(clazz);
                }
            } catch (Exception e) {
                logger.error("create " + clazz.getCanonicalName() + "instance error", e);
            }
        }
        return null;
    }


    /**
     * Gets tag.
     *
     * @return the tag
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Sets tag.
     *
     * @param alias the tag
     * @return the tag
     */
    public ExtensionClass setAlias(String alias) {
        this.alias = alias;
        return this;
    }

    /**
     * Gets code.
     *
     * @return the code
     */
    public byte getCode() {
        return code;
    }

    /**
     * Sets code.
     *
     * @param code the code
     * @return the code
     */
    public ExtensionClass setCode(byte code) {
        this.code = code;
        return this;
    }

    /**
     * Is singleton boolean.
     *
     * @return the boolean
     */
    public boolean isSingleton() {
        return singleton;
    }

    /**
     * Sets singleton.
     *
     * @param singleton the singleton
     */
    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    /**
     * Gets clazz.
     *
     * @return the clazz
     */
    public Class<? extends T> getClazz() {
        return clazz;
    }

    /**
     * Sets clazz.
     *
     * @param clazz the clazz
     * @return the clazz
     */
    public ExtensionClass setClazz(Class<? extends T> clazz) {
        this.clazz = clazz;
        return this;
    }

    /**
     * Gets order.
     *
     * @return the order
     */
    public int getOrder() {
        return order;
    }

    /**
     * Sets order.
     *
     * @param order the order
     * @return the order
     */
    public ExtensionClass setOrder(int order) {
        this.order = order;
        return this;
    }

//    /**
//     * Is provider side boolean.
//     *
//     * @return the boolean
//     */
//    public boolean isProviderSide() {
//        return providerSide;
//    }
//
//    /**
//     * Sets provider side.
//     *
//     * @param providerSide the provider side
//     * @return the provider side
//     */
//    public ExtensionClass setProviderSide(boolean providerSide) {
//        this.providerSide = providerSide;
//        return this;
//    }
//
//    /**
//     * Is consumer side boolean.
//     *
//     * @return the boolean
//     */
//    public boolean isConsumerSide() {
//        return consumerSide;
//    }
//
//    /**
//     * Sets consumer side.
//     *
//     * @param consumerSide the consumer side
//     * @return the consumer side
//     */
//    public ExtensionClass setConsumerSide(boolean consumerSide) {
//        this.consumerSide = consumerSide;
//        return this;
//    }
//
//    /**
//     * Is auto active boolean.
//     *
//     * @return the boolean
//     */
//    public boolean isAutoActive() {
//        return autoActive;
//    }
//
//    /**
//     * Sets auto active.
//     *
//     * @param autoActive the auto active
//     * @return the auto active
//     */
//    public ExtensionClass setAutoActive(boolean autoActive) {
//        this.autoActive = autoActive;
//        return this;
//    }

    @Override
    public String toString() {
        return "ExtensibleClass{" +
                "alias='" + alias + '\'' +
                ", code=" + code +
                ", clazz=" + clazz +
                ", order=" + order +
//                ", providerSide=" + providerSide +
//                ", consumerSide=" + consumerSide +
//                ", autoActive=" + autoActive +
                '}';
    }
}
