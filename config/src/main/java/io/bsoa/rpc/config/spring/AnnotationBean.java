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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;

import io.bsoa.rpc.bootstrap.Bootstraps;
import io.bsoa.rpc.common.struct.ConcurrentHashSet;
import io.bsoa.rpc.common.utils.ClassLoaderUtils;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.config.RegistryConfig;
import io.bsoa.rpc.config.ServerConfig;
import io.bsoa.rpc.config.annotation.Consumer;
import io.bsoa.rpc.config.annotation.Provider;
import io.bsoa.rpc.config.annotation.Server;
import io.bsoa.rpc.exception.BsoaRuntimeException;

/**
 * Created by zhangg on 16-7-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class AnnotationBean implements InitializingBean, DisposableBean,
        BeanFactoryPostProcessor, BeanPostProcessor, ApplicationContextAware {
    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(AnnotationBean.class);

    /**
     * The Application context.
     */
    private ApplicationContext applicationContext;// 如果用了spring，则会注入

    private ConfigurableListableBeanFactory beanFactory;

    /**
     * 解析包路径下class类
     */
    private final String resourcePattern = "**/*.class";
    /**
     * The Resource pattern resolver.
     */
    private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    /**
     * The Metadata reader factory.
     */
    private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(
            this.resourcePatternResolver);


    /*--------------- 缓存的数据开始 ------------------*/
    /**
     * 外部配置的注册中心（例如spring）
     */
    private final List<RegistryConfig> registryConfigs;

    /**
     * 外部配置的server（例如spring）
     */
    private final List<ServerConfig> serverConfigs;

    /**
     * The Provider configs.
     */
    private final ConcurrentMap<String, ProviderConfig> providerCache = new ConcurrentHashMap<String, ProviderConfig>();
    /**
     * The Server map.
     */
    private final ConcurrentMap<String, ServerConfig> serverCache = new ConcurrentHashMap<String, ServerConfig>();
    /**
     * The Consumer configs.
     */
    private final ConcurrentMap<String, ConsumerConfig> consumerCache = new ConcurrentHashMap<String, ConsumerConfig>();
    /**
     * The Reference object.
     */
    private final ConcurrentMap<String, Object> referenceObject = new ConcurrentHashMap<String, Object>();

    /*--------------- 缓存的数据结束 ------------------*/

    /**
     * 构造函数
     */
    private AnnotationBean() {
        LOGGER.debug("JSF annotation from spring");
        this.registryConfigs = new ArrayList<RegistryConfig>();
        this.serverConfigs = new ArrayList<ServerConfig>();
    }

    /**
     * 构造函数 API方式配置annotation
     *
     * @param registryConfigs          注册中心地址
     * @param serverConfigs          配置协议
     * @param basePackage          扫描包路径, 多个用英文逗号分隔
     * @throws org.springframework.beans.BeansException          the beans exception
     */
    public AnnotationBean(List<RegistryConfig> registryConfigs,
                          List<ServerConfig> serverConfigs, String basePackage)
            throws BeansException {

        this.registryConfigs = registryConfigs;
        this.serverConfigs = serverConfigs;
        if (serverConfigs != null && serverConfigs.size() != 0) {
            for (ServerConfig serverConfig : serverConfigs) {
                serverCache.put(serverConfig.getProtocol() + ":" + serverConfig.getPort(), serverConfig);
            }
        }
        setBasePackage(basePackage);
        scanAndExport();
    }

    /**
     * 构造函数 API方式配置annotation  reference应使用该构造方法
     *
     * @param registryConfigs          注册中心地址
     * @param basePackage          扫描包路径, 多个用英文逗号分隔
     * @throws org.springframework.beans.BeansException          the beans exception
     */
    public AnnotationBean(List<RegistryConfig> registryConfigs, String basePackage) throws BeansException {
        this.registryConfigs = registryConfigs;
        this.serverConfigs = new ArrayList<ServerConfig>();
        setBasePackage(basePackage);
        scanAndExport();
    }

    /**
     * Sets application context.
     *
     * @param applicationContext the application context
     * @throws BeansException the beans exception
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;

        // 初始化spring配置到已发布
        if (applicationContext != null) {
            Map<String, RegistryConfig> registryMaps = applicationContext
                    .getBeansOfType(RegistryConfig.class, false, false);
            if (registryMaps != null && !registryMaps.isEmpty()) {
                registryConfigs.addAll(registryMaps.values());
            }

            Map<String, ServerConfig> serverMaps = applicationContext
                    .getBeansOfType(ServerConfig.class, false, false);
            if (serverMaps != null && !serverMaps.isEmpty()) {
                serverConfigs.addAll(serverMaps.values());
                for (ServerConfig serverConfig : serverConfigs) {
                    serverCache.put(serverConfig.getProtocol() + ":" + serverConfig.getPort(), serverConfig);
                }
            }
        }
    }

    /**
     * After properties set.
     *
     * @throws Exception the exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (StringUtils.isBlank(basePackage)) {
            throw ExceptionUtils.buildRuntime(21801, "annotation.basePackage", basePackage);
        }
        //scanAndExport();
    }

    /**
     * Destroy void.
     *
     * @throws Exception the exception
     */
    @Override
    public void destroy() throws Exception {
//        for (ProviderConfig<?> providerConfig : providerCache.values()) {
//            try {
//                //serviceConfig.unexport();
//                //providerConfig.destroy();
//            } catch (Throwable e) {
//                LOGGER.error(e.getMessage(), e);
//            }
//        }
//        for (ConsumerConfig<?> consumerConfig : consumerCache.values()) {
//            try {
//                //consumerConfig.destroy();
//            } catch (Throwable e) {
//                LOGGER.error(e.getMessage(), e);
//            }
//        }
    }

    /**
     * Post process bean factory.
     *
     * @param beanFactory the bean factory
     * @throws BeansException the beans exception
     */
    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
        if (StringUtils.isBlank(basePackage)) {
            throw ExceptionUtils.buildRuntime(21801, "annotation.basePackage", basePackage);
        }
        scanAndExport();
    }

    /**
     * Post process before initialization.
     *
     * @param bean the bean
     * @param beanName the bean name
     * @return the object
     * @throws BeansException the beans exception
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName)
            throws BeansException {
        if (isMatchPackage(bean) && isConsumer()) { // 检查是否匹配包路径
            return scanConsumer(bean, beanName);
        }
        return bean;
    }

    /**
     * Post process after initialization.
     *
     * @param bean the bean
     * @param beanName the bean name
     * @return the object
     * @throws BeansException the beans exception
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        /*if (isMatchPackage(bean) && isProvider()) { // 检查是否匹配包路径
            return scanProvider(bean, beanName);
        }*/
        return bean;
    }

    /**
     * Scan and export.
     */
    private void scanAndExport() {
        LOGGER.info("Scaning jsf annotation of package: {} ", basePackage);
        for (String basePackage : annotationPackages) {
            try {
                findCandidateComponents(basePackage);
            } catch (Throwable e) {
                LOGGER.error("Scan class of " + basePackage + " error!", e);
            }
        }
    }

    /**
     * Scan the class path for candidate components.
     *
     * @param basePackage          the package to check for annotated classes
     * @return a corresponding Set of autodetected bean definitions
     */
    private Set<BeanDefinition> findCandidateComponents(String basePackage) {
        Set<BeanDefinition> candidates = new LinkedHashSet<BeanDefinition>();
        try {
            String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                    + resolveBasePackage(basePackage)
                    + "/"
                    + this.resourcePattern;
            Resource[] resources = this.resourcePatternResolver.getResources(packageSearchPath);
            boolean traceEnabled = LOGGER.isTraceEnabled();
            boolean debugEnabled = LOGGER.isDebugEnabled();
            for (Resource resource : resources) {
                if (traceEnabled) {
                    LOGGER.trace("Scanning " + resource);
                }
                if (resource.isReadable()) {
                    try {
                        MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(resource);
                        ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
                        sbd.setResource(resource);
                        sbd.setSource(resource);
                        if (debugEnabled) {
                            LOGGER.debug("Identified candidate component class: " + resource);
                        }

                        // 如果是接口或者不是public类型, 跳过
                        Class<?> clz = ClassLoaderUtils.forName(sbd.getBeanClassName());
                        if (clz.isInterface() || clz.isAnnotation() || clz.isEnum()
                                || !Modifier.isPublic(clz.getModifiers())
                                || Modifier.isStatic(clz.getModifiers())) {
                            continue;
                        }

                        Object bean = null;
                        try {
                            bean = ClassLoaderUtils.newInstance(clz);
                        } catch(Exception e) {
                            continue;
                        }

                        String beanName = sbd.getBeanClassName();
                        if (isMatchPackage(bean)) { // 检查是否匹配包路径
                            if (isConsumer()) {
                                scanConsumer(bean, beanName); // consumer
                            }
                            if (isProvider()) {
                                scanProvider(bean, beanName); // provider
                            }
                            candidates.add(sbd);
                        }
//                        postProcessAfterInitialization(bean, beanName); // provider
//                        postProcessBeforeInitialization(bean, beanName); // consumer
//                        candidates.add(sbd);
                    } catch (Throwable ex) {
                        LOGGER.error(ex.getMessage(), ex);
                        throw new BeanDefinitionStoreException("Failed to read candidate component class: " + resource, ex);
                    }
                } else {
                    if (traceEnabled) {
                        LOGGER.trace("Ignored because not readable: " + resource);
                    }
                }
            }
        } catch (IOException ex) {
            LOGGER.error("I/O failure during classpath scanning" + ex.getMessage(), ex);
//			throw new BeanDefinitionStoreException(
//					"I/O failure during classpath scanning", ex);
        }
        return candidates;
    }

    /**
     * Scan provider.
     *
     * @param bean the bean
     * @param beanName the bean name
     * @return the object
     */
    private Object scanProvider(Object bean, String beanName) {
        Class<?> beanClass = bean.getClass();
        Provider provider = beanClass.getAnnotation(Provider.class);
        if (provider != null) {
            Class interfaceClass;
            Class[] interfaces = beanClass.getInterfaces();
            if (interfaces.length > 0) { // 实现类未继承接口
                interfaceClass = interfaces[0];
            } else {
                throw new IllegalStateException("Failed to export remote service class " + beanClass.getName()
                        + ", cause: The @Provider undefined interfaceClass or interfaceName, "
                        + "and the service class unimplemented any interfaces.");
            }
            ProviderConfig providerConfig = parseAnnotationService(provider, interfaceClass, bean);
            Bootstraps.from(providerConfig).export();// 发布服务
        }
        return bean;
    }

    /**
     * Scan consumer.
     *
     * @param bean the bean
     * @param beanName the bean name
     * @return the object
     */
    private Object scanConsumer(Object bean, String beanName) {
        Class<?> beanClass = bean.getClass();
        /*
        Method[] methods = bean.getClass().getMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (name.length() > 3 && name.startsWith("set")
                    && method.getParameterTypes().length == 1
                    && Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers())) {
                try {
                    Consumer consumer = method.getAnnotation(Consumer.class);
                    if (consumer != null) {
                        Object value = refer(consumer, method.getParameterTypes()[0]);
                        if (value != null) {
                            method.invoke(bean, new Object[]{value});
                            referenceObject.put(method.getParameterTypes()[0].getName(), value);
                        }
                    }
                } catch (Throwable e) {
                    LOGGER.error("Failed to init remote service reference at method " + name
                                    + " in class " + bean.getClass().getName() + "!", e);
                }
            }
        }*/
        Field[] fields = beanClass.getDeclaredFields();
        for (Field field : fields) {
            boolean accessible = field.isAccessible();
            try {
                if (!accessible) {
                    field.setAccessible(true);
                }
                Consumer consumer = field.getAnnotation(Consumer.class);
                if (consumer != null) {
                    Class fieldClass = field.getType();
                    if (!fieldClass.isInterface()) { // 返回值不是接口
                        throw new IllegalStateException(
                                "Failed to refer remote service class " + fieldClass.getName()
                                        + ", cause: The @Consumer need defined base on interfaces.");
                    }
                    ConsumerConfig consumerConfig = parseAnnotationCosumer(consumer, fieldClass);
                    Object ref = Bootstraps.from(consumerConfig).refer(); // 引用服务
                    if (ref != null) {
                        try {
                            field.set(bean, ref); // 注入到字段中
                        } catch (IllegalAccessException e) {
                            throw new BsoaRuntimeException(22222, "Set proxy to field error", e);
                        }
                        if (!beanFactory.containsSingleton(field.getName())) {
                            beanFactory.registerSingleton(field.getName(), ref);
                        }
                        referenceObject.put(field.getType().getName(), ref);
                    }
                }
            } finally {
                if (!accessible) {
                    field.setAccessible(accessible);
                }
            }
        }
        return bean;
    }

    /**
     * 取xml和annotation配置的交集
     *
     * @param provider          the provider
     * @param interfaceClass the interface class
     * @param ref the ref
     */
    private ProviderConfig parseAnnotationService(Provider provider, Class interfaceClass, Object ref) {

        String interfaceName = interfaceClass.getName();
        String key = interfaceName + "@" + provider.alias() + "@" + ref.getClass().getName();

        ProviderConfig providerConfig = providerCache.get(key);
        if (providerConfig == null) {
            providerConfig = new ProviderBean();
            providerConfig.setInterfaceId(interfaceClass.getName());
            providerConfig.setRef(ref);
            if (applicationContext != null) {
                Set<ProviderConfig<?>> configs = getXMLProviderConfig();
                if (configs != null && configs.size() > 0) {// 既配置了xml，又配置了annotation, 报错
                    for (ProviderConfig<?> xmlProvider : configs) {
                        if (xmlProvider.getRef().getClass().getName().equals(providerConfig.getRef().getClass().getName())
                                && xmlProvider.getTags().equals(provider.alias())) {
                            throw new BsoaRuntimeException(22222, "Duplicate providers with same alias " +
                                    "are specified in annotation and spring-xml");
                        }
                    }
                }
            }
            providerConfig.setRegistry(registryConfigs);
            providerConfig.setTags(provider.alias());
            // 解析服务端
            List<ServerConfig> serverConfigs = parseAnnotationServers(provider.server());
            providerConfig.setServer(serverConfigs);
            // 其它选填属性
            providerConfig.setRegister(provider.register());
            providerConfig.setDynamic(provider.dynamic());
            providerConfig.setWeight(provider.weight());

            providerCache.put(key, providerConfig);
        }

        return providerConfig;
    }

    /**
     * 从annotation中获取Server的配置
     *
     * @param servers          the servers
     * @return list list
     */
    private List<ServerConfig> parseAnnotationServers(Server[] servers) {
        List<ServerConfig> serverConfigs = new ArrayList<ServerConfig>();
        for (Server server : servers) {
            String protocol = server.protocol();
            int port = server.port();
            // 先从缓存里取
            String key = protocol + "@" +port;
            ServerConfig serverConfig = serverCache.get(key);
            if (serverConfig == null) {
                // 复制属性
                serverConfig = new ServerConfig();
                serverConfig.setProtocol(protocol);
                // 其它选填属性
                serverConfig.setHost(server.host());
                serverConfig.setPort(port);
                serverCache.put(key, serverConfig);
            }
            serverConfigs.add(serverConfig);
        }
        return serverConfigs;
    }

    /**
     * Refer object.
     *
     * @param consumer          the consumer
     * @param consumerClass          the consumer class
     * @return the object
     */
    private ConsumerConfig parseAnnotationCosumer(Consumer consumer, Class<?> consumerClass) {
        String interfaceName = consumerClass.getName();
        String key = interfaceName + "@" + consumer.alias() + "@" + consumer.protocol();
        ConsumerConfig consumerConfig = consumerCache.get(key);
        if (consumerConfig == null) {
            consumerConfig = new ConsumerBean();
            consumerConfig.setInterfaceId(interfaceName);
            consumerConfig.setRegistry(registryConfigs);
            consumerConfig.setTags(consumer.alias());
            consumerConfig.setProtocol(consumer.protocol());
            // 其它属性，选填
            consumerConfig.setCluster(consumer.cluster());
            consumerConfig.setRetries(consumer.retries());
            consumerConfig.setTimeout(consumer.timeout());
            consumerConfig.setUrl(consumer.url());
            consumerConfig.setLoadBalancer(consumer.loadBalancer());
            consumerConfig.setSerialization(consumer.serialization());
            consumerConfig.setLazy(consumer.lazy());

            consumerCache.put(key, consumerConfig);
        }
        return consumerConfig;
    }

    /**
     * Resolve base package.
     *
     * @param basePackage          the base package
     * @return the string
     */
    protected String resolveBasePackage(String basePackage) {
        return ClassUtils.convertClassNameToResourcePath(SystemPropertyUtils
                .resolvePlaceholders(basePackage));
    }

    /**
     * Gets reference object.
     *
     * @param type          the type
     * @return the reference object
     */
    public Object getReferenceObject(Class<?> type) {
        return referenceObject.get(type.getName());
    }


    /**
     * Description: 取得xml中所配置的Config
     * Date: 2013-5-22
     *
     * @return void xML provider config
     * @author changhongqiang
     */
    private Set<ProviderConfig<?>> getXMLProviderConfig() {
        Set<ProviderConfig<?>> configs = new ConcurrentHashSet<ProviderConfig<?>>();
        if (applicationContext != null) {
            // 得到spring配置文件中配置的ServiceConfig
            @SuppressWarnings("rawtypes")
            Map<String, ProviderConfig> providers = applicationContext.getBeansOfType(ProviderConfig.class);
            if (providers != null && providers.size() > 0) {
                for (ProviderConfig<?> config : providers.values()) {
                    configs.add(config);
                }
            }
        }

        return configs;
    }


    /*---------- 参数配置项开始 ------------*/

    /**
     * 包的基本路径（前缀）
     */
    protected String basePackage;

    /**
     * 是否扫描provider
     */
    protected boolean provider = true;

    /**
     * 是否扫描consumer
     */
    protected boolean consumer = true;

    /*---------- 参数配置项结束 ------------*/

    /**
     * 解析出来的各个包
     */
    protected transient String[] annotationPackages;

    /**
     * Gets basePackage.
     *
     * @return the basePackage
     */
    public String getBasePackage() {
        return basePackage;
    }

    /**
     * Sets basePackage.
     *
     * @param basePackage          the basePackage
     */
    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
        this.annotationPackages = StringUtils.isBlank(basePackage) ? null
                : StringUtils.splitWithCommaOrSemicolon(this.basePackage);
    }

    /**
     * Is provider.
     *
     * @return the boolean
     */
    public boolean isProvider() {
        return provider;
    }

    /**
     * Sets provider.
     *
     * @param provider the provider
     */
    public void setProvider(boolean provider) {
        this.provider = provider;
    }

    /**
     * Is consumer.
     *
     * @return the boolean
     */
    public boolean isConsumer() {
        return consumer;
    }

    /**
     * Sets consumer.
     *
     * @param consumer the consumer
     */
    public void setConsumer(boolean consumer) {
        this.consumer = consumer;
    }


    /**
     * Is match package.
     *
     * @param bean
     *         the bean
     * @return the boolean
     */
    protected boolean isMatchPackage(Object bean) {
        if (annotationPackages == null || annotationPackages.length == 0) {
            return true;
        }
        String beanClassName = bean.getClass().getName();
        for (String pkg : annotationPackages) {
            if (beanClassName.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }
}
