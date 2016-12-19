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
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 * @see Extension
 * @see AutoActive
 */
public class ExtensionClass<T> {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionClass.class);

    protected String alias; // 扩展别名,不是provider alias
    protected Class<T> clazz; // 扩展接口类
    protected int order; // 扩展点排序
    protected boolean autoActive; // 是否自动激活
    protected boolean providerSide; // 服务端是否激活
    protected boolean consumerSide; // 调用端是否激活

    /**
     * @return instance of clazz
     */
    public T getExtInstance() {
        if (clazz != null) {
            try {
                return ClassUtils.newInstance(clazz);
            } catch (Exception e) {
                logger.error("create {} instance error", clazz.getCanonicalName(), e);
                return null;
            }
        }
        return null;
    }


    public String getAlias() {
        return alias;
    }

    public ExtensionClass setAlias(String alias) {
        this.alias = alias;
        return this;
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public ExtensionClass setClazz(Class<T> clazz) {
        this.clazz = clazz;
        return this;
    }

    public int getOrder() {
        return order;
    }

    public ExtensionClass setOrder(int order) {
        this.order = order;
        return this;
    }

    public boolean isProviderSide() {
        return providerSide;
    }

    public ExtensionClass setProviderSide(boolean providerSide) {
        this.providerSide = providerSide;
        return this;
    }

    public boolean isConsumerSide() {
        return consumerSide;
    }

    public ExtensionClass setConsumerSide(boolean consumerSide) {
        this.consumerSide = consumerSide;
        return this;
    }

    public boolean isAutoActive() {
        return autoActive;
    }

    public ExtensionClass setAutoActive(boolean autoActive) {
        this.autoActive = autoActive;
        return this;
    }

    @Override
    public String toString() {
        return "ExtensibleClass{" +
                "alias='" + alias + '\'' +
                ", clazz=" + clazz +
                ", order=" + order +
                ", providerSide=" + providerSide +
                ", consumerSide=" + consumerSide +
                ", autoActive=" + autoActive +
                '}';
    }
}
