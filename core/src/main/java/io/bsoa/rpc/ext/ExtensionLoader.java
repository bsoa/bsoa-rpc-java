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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.utils.ClassLoaderUtils;
import io.bsoa.rpc.common.utils.ClassTypeUtils;
import io.bsoa.rpc.common.utils.ClassUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.exception.BsoaRuntimeException;

/**
 * Created by zhangg on 2016/7/14 21:57.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ExtensionLoader<T> {
    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ExtensionLoader.class);

    /**
     * 当前加载的接口类名
     */
    protected final Class<T> interfaceClass;

    /**
     * 接口名字
     */
    protected final String interfaceName;

    /**
     * 扩展点是否单例
     */
    protected final boolean singleton;

    /**
     * 扩展点使用的文件名
     */
    protected final String file;

    /**
     * 全部的加载的实现类
     */
    protected final ConcurrentHashMap<String, ExtensionClass<T>> all;

    /**
     * 自动激活的
     */
    protected final ConcurrentHashMap<String, ExtensionClass<T>> autoActives;

    /**
     * 如果是单例，那么factory不为空
     */
    protected final ConcurrentHashMap<String, T> factory;

    /**
     * 构造函数（自动加载）
     *
     * @param interfaceClass 接口类
     */
    public ExtensionLoader(Class<T> interfaceClass) {
        this(interfaceClass, true);
    }

    /**
     * 构造函数（主要测试用）
     *
     * @param interfaceClass 接口类
     * @param autoLoad       是否自动开始加载
     */
    protected ExtensionLoader(Class<T> interfaceClass, boolean autoLoad) {
        if (interfaceClass == null || !interfaceClass.isInterface()) {
            throw new IllegalArgumentException("Extensible class must be interface!");
        }
        this.interfaceClass = interfaceClass;
        this.interfaceName = ClassTypeUtils.getTypeStr(interfaceClass);
        Extensible extensible = interfaceClass.getAnnotation(Extensible.class);
        if (extensible == null) {
            throw new IllegalArgumentException("Error when load extensible interface " + interfaceName
                    + ", must add annotation @Extensible.");
        } else {
            file = StringUtils.isBlank(extensible.file()) ? interfaceName : extensible.file().trim();
            singleton = extensible.singleton();
        }

        factory = singleton ? new ConcurrentHashMap<>() : null;
        all = new ConcurrentHashMap<>();
        autoActives = new ConcurrentHashMap<>();
        if (autoLoad) {
            List<String> paths = BsoaConfigs.getListValue(BsoaConfigs.EXTENSION_LOAD_PATH);
            for (String path : paths) {
                loadFromFile(path);
            }
        }
    }

    /**
     * @param path path必须以/结尾
     */
    protected synchronized void loadFromFile(String path) {
        String fileName = path + file;
        try {
            ClassLoader classLoader = ClassLoaderUtils.getClassLoader(getClass());
            Enumeration<URL> urls = classLoader != null ? classLoader.getResources(fileName)
                    : ClassLoader.getSystemResources(fileName);
            // 可能存在多个文件。
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    // 读取一个文件
                    URL url = urls.nextElement();
                    LOGGER.debug("Loading extension of interface {} from file: {}", interfaceName, url);
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            readLine(url, line);
                        }
                    } catch (Throwable t) {
                        LOGGER.error("Failed to load extension of interface " + interfaceName
                                + " from file:" + url, t);
                    } finally {
                        if (reader != null) {
                            reader.close();
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to load extension of interface " + interfaceName
                    + " from path:" + fileName, t);
        }
    }

    protected void readLine(URL url, String line) throws Throwable {
        String[] aliasAndClassName = parseAliasAndClassName(line);
        if (aliasAndClassName == null || aliasAndClassName.length != 2) {
            return;
        }
        String alias = aliasAndClassName[0];
        String className = aliasAndClassName[1];
        // 读取配置的实现类
        Class<?> implClass = ClassLoaderUtils.forName(className, true);
        if (!interfaceClass.isAssignableFrom(implClass)) {
            throw new IllegalArgumentException("Error when load extension of interface " + interfaceName
                    + " from file:" + url + ", " + className + " is not subtype of interface.");
        }

        // 检查是否有可扩展标识
        Extension extension = implClass.getAnnotation(Extension.class);
        if (extension == null) {
            throw new IllegalArgumentException("Error when load extension of interface " + interfaceName
                    + " from file:" + url + ", " + className + " must add annotation @Extension.");
        } else {
            String aliasInCode = extension.value();
            if (StringUtils.isBlank(aliasInCode)) {
                throw new IllegalArgumentException("Error when load extension of interface "
                        + interfaceClass + " from file:" + url + ", " + className
                        + "'s alias of @extensible is blank");
            }
            if (alias == null) {
                alias = aliasInCode;
            } else {
                if (!aliasInCode.equals(alias)) {
                    throw new IllegalArgumentException("Error when load extension of interface "
                            + interfaceName + " from file:" + url + ", aliases of " + className + " are " +
                            "not equal between " + aliasInCode + "(code) and " + alias + "(file).");
                }
            }
        }
        // 提前试试能不能实例化，
        try {
            implClass.getConstructor(interfaceClass);
        } catch (NoSuchMethodException e) {
            implClass.getConstructor(); // 有没有默认的空的构造函数
        }
        // 检查是否有存在不对的
        ExtensionClass old = all.get(alias);
        if (old != null) {
            throw new IllegalStateException("Error when load extension of interface "
                    + interfaceClass + " from file:" + url + ", Duplicate class with same alias: "
                    + alias + ", " + old.getClazz() + " and " + implClass);
        } else {
            ExtensionClass extensionClass = new ExtensionClass();
            extensionClass.setAlias(alias);
            extensionClass.setClazz(implClass);
            extensionClass.setOrder(extension.order());
            // 读取自动加载的类列表。
            AutoActive autoActive = implClass.getAnnotation(AutoActive.class);
            if (autoActive != null) {
                extensionClass.setAutoActive(true);
                extensionClass.setProviderSide(autoActive.providerSide());
                extensionClass.setConsumerSide(autoActive.consumerSide());
                autoActives.put(alias, extensionClass);
                LOGGER.debug("Extension of interface " + interfaceName + " from file:" + url
                        + ", " + implClass + "(" + alias + ") will auto active");
            }
            all.put(alias, extensionClass);
        }
    }

    protected String[] parseAliasAndClassName(String line) {
        line = line.trim();
        int i0 = line.indexOf("#");
        if (i0 == 0 || line.length() == 0) {
            return null; // 当前行是注释 或者 空
        }
        if (i0 > 0) {
            line = line.substring(0, i0).trim();
        }

        String alias = null;
        String className = null;
        int i = line.indexOf('=');
        if (i > 0) {
            alias = line.substring(0, i).trim(); // 以代码里的为准
            className = line.substring(i + 1).trim();
        } else {
            className = line;
        }
        if (className.length() == 0) {
            return null;
        }
        return new String[]{alias, className};
    }

    /**
     * 得到服务端的全部自动激活扩展
     *
     * @return 自动激活扩展列表
     */
    public List<ExtensionClass<T>> getProviderSideAutoActives() {
        List<ExtensionClass<T>> extensionClasses = new ArrayList<ExtensionClass<T>>();
        for (ConcurrentHashMap.Entry<String, ExtensionClass<T>> entry : all.entrySet()) {
            ExtensionClass<T> extensionClass = entry.getValue();
            if (extensionClass.isAutoActive() && extensionClass.isProviderSide()) {
                extensionClasses.add(extensionClass);
            }
        }
        Collections.sort(extensionClasses, new OrderComparator());
        return extensionClasses;
    }

    /**
     * 得到调用端的全部自动激活扩展
     *
     * @return 自动激活扩展列表
     */
    public List<ExtensionClass<T>> getConsumerSideAutoActives() {
        List<ExtensionClass<T>> extensionClasses = new ArrayList<ExtensionClass<T>>();
        for (ConcurrentHashMap.Entry<String, ExtensionClass<T>> entry : all.entrySet()) {
            ExtensionClass<T> extensionClass = entry.getValue();
            if (extensionClass.isAutoActive() && extensionClass.isConsumerSide()) {
                extensionClasses.add(extensionClass);
            }
        }
        Collections.sort(extensionClasses, new OrderComparator());
        return extensionClasses;
    }

    /**
     * 根据服务别名查找扩展类
     *
     * @param alias 扩展别名
     * @return 扩展类对象
     */
    public ExtensionClass<T> getExtensionClass(String alias) {
        return all.get(alias);
    }

    /**
     * 得到实力
     *
     * @param alias 别名
     * @return 扩展实例（已判断是否单例）
     */
    public T getExtension(String alias) {
        ExtensionClass<T> extensionClass = getExtensionClass(alias);
        if (extensionClass == null) {
            throw new BsoaRuntimeException(22222, "Extension Not Found :\"" + alias + "\"!");
        } else {
            if (singleton && factory != null) {
                T t = factory.get(alias);
                if (t == null) {
                    synchronized (this) {
                        t = factory.get(alias);
                        if (t == null) {
                            t = ClassUtils.newInstance(extensionClass.getClazz());
                            factory.put(alias, t);
                        }
                    }
                }
                return t;
            } else {
                return ClassUtils.newInstance(extensionClass.getClazz());
            }
        }
    }

    protected static class OrderComparator implements Comparator<ExtensionClass> {
        public int compare(ExtensionClass o1, ExtensionClass o2) {
            // order一样的情况下，先加入的在前面
            return o2.getOrder() > o1.getOrder() ? -1 : 1;
        }
    }


}
