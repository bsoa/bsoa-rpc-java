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
package io.bsoa.rpc.config.spring;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;

import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.config.RegistryConfig;
import io.bsoa.rpc.config.ServerConfig;
import io.bsoa.rpc.filter.Filter;

/**
 * Created by zhangg on 16-7-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class ProviderBean<T> extends ProviderConfig<T> implements
        InitializingBean, DisposableBean, ApplicationContextAware, ApplicationListener,
        BeanNameAware {

    /**
     *
     */
    private static final long serialVersionUID = -6685403797940153883L;

    /**
     * slf4j logger for this class
     */
    private final static Logger LOGGER = LoggerFactory
            .getLogger(ProviderBean.class);

    /**
     * 默认构造函数，不允许从外部new
     */
    protected ProviderBean() {

    }

    private transient ApplicationContext applicationContext;

    private transient String beanName;

    private transient boolean supportedApplicationListener;

    /**
     * @param name
     * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
     */
    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    /**
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
        if (applicationContext != null) {
            try {
                Method method = applicationContext.getClass().getMethod(
                        "addApplicationListener",
                        new Class<?>[]{ApplicationListener.class}); // 兼容Spring2.5.6和3.0
                method.invoke(applicationContext, new Object[]{this});
                supportedApplicationListener = true;
            } catch (Throwable t) {
                if (applicationContext instanceof AbstractApplicationContext) {
                    try {
                        Method method = AbstractApplicationContext.class.getDeclaredMethod("addListener",
                                new Class<?>[]{ApplicationListener.class}); // 兼容Spring2.0.1
                        if (!method.isAccessible()) {
                            method.setAccessible(true);
                        }
                        method.invoke(applicationContext, new Object[]{this});
                        supportedApplicationListener = true;
                    } catch (Throwable t2) {
                    }
                }
            }
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) { // spring加载完毕
            if (isDelay()) {
                LOGGER.info("JSF export provider with beanName {} after " +
                        "spring context refreshed.", beanName);
                if (delay < -1) { // 小于-1表示延迟更长时间加载
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(-delay);
                            } catch (Throwable e) {
                            }
                            export();
                        }
                    });
                    thread.setDaemon(true);
                    thread.setName("DelayExportThread");
                    thread.start();
                } else { // 等于-1表示延迟立即加载
                    export();
                }
            }
        }
    }

    private boolean isDelay() {
        return supportedApplicationListener && delay < 0;
    }

    /**
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (applicationContext != null) {
            // 如果没有配置协议，则默认发全部协议
            if (getServer() == null) {
                Map<String, ServerConfig> protocolMaps = applicationContext
                        .getBeansOfType(ServerConfig.class, false, false);
                List<ServerConfig> protocolLists = null;
                if (CommonUtils.isNotEmpty(protocolMaps)) {
                    Collection<ServerConfig> serverConfigs = protocolMaps.values();
                    if (CommonUtils.isNotEmpty(serverConfigs)) {
                        protocolLists = new ArrayList<ServerConfig>(serverConfigs);
                    }
                }
                super.setServer(protocolLists);
            }
            // 如果没有配置注册中心，则默认发布到全部注册中心
            if (getRegistry() == null) {
                Map<String, RegistryConfig> registryMaps = applicationContext
                        .getBeansOfType(RegistryConfig.class, false, false);
                List<RegistryConfig> registryLists = null;
                if (CommonUtils.isNotEmpty(registryMaps)) {
                    Collection<RegistryConfig> registryConfigs = registryMaps.values();
                    if (CommonUtils.isNotEmpty(registryConfigs)) {
                        registryLists = new ArrayList<RegistryConfig>(registryConfigs);
                    }
                }
                super.setRegistry(registryLists);
            }
            // 看有没有全局过滤器配置
            Map<String, FilterBean> registryMaps = applicationContext
                    .getBeansOfType(FilterBean.class, false, false);
            for (Map.Entry<String, FilterBean> entry : registryMaps.entrySet()) {
                FilterBean filterBean = entry.getValue();
                if (filterBean.containsProvider(beanName)) {
                    List<Filter> filters = getFilterRef();
                    if (filters == null) {
                        filters = new ArrayList<Filter>();
                        filters.add(filterBean.getRef());
                        setFilterRef(filters);
                    } else {
                        filters.add(filterBean.getRef());
                    }
                }
            }
        }
        if (!isDelay()) {
            export();
        }
    }

    @Override
    public void destroy() throws Exception {
        LOGGER.info("JSF destroy provider with beanName {}", beanName);
        super.unExport();
    }
}