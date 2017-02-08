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
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.annotation.JustForTest;
import io.bsoa.rpc.common.utils.ClassLoaderUtils;
import io.bsoa.rpc.common.utils.ClassTypeUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.exception.BsoaRuntimeException;

/**
 * <p>一个可扩展接口类，对应一个加载器</p>
 * <p>
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
    protected final Extensible extensible;

    /**
     * 全部的加载的实现类 {"alias":ExtensionClass}
     */
    protected final ConcurrentHashMap<String, ExtensionClass<T>> all;

    /**
     * 如果是单例，那么factory不为空
     */
    protected final ConcurrentHashMap<String, T> factory;

    /**
     * 加载监听器
     */
    protected final ExtensionLoaderListener<T> listener;

    /**
     * 构造函数（自动加载）
     *
     * @param interfaceClass 接口类
     * @param listener       加载后的监听器
     */
    public ExtensionLoader(Class<T> interfaceClass, ExtensionLoaderListener<T> listener) {
        this(interfaceClass, true, listener);
    }

    /**
     * 构造函数（自动加载）
     *
     * @param interfaceClass 接口类
     */
    protected ExtensionLoader(Class<T> interfaceClass) {
        this(interfaceClass, true, null);
    }

    /**
     * 构造函数（主要测试用）
     *
     * @param interfaceClass 接口类
     * @param autoLoad       是否自动开始加载
     */
    @JustForTest
    protected ExtensionLoader(Class<T> interfaceClass, boolean autoLoad, ExtensionLoaderListener<T> listener) {
        if (BsoaContext.IS_SHUTTING_DOWN) {
            this.interfaceClass = null;
            this.interfaceName = null;
            this.listener = null;
            this.factory = null;
            this.extensible = null;
            this.all = null;
            return;
        }
        // 接口为空，既不是接口，也不是抽象类
        if (interfaceClass == null ||
                !(interfaceClass.isInterface() || Modifier.isAbstract(interfaceClass.getModifiers()))) {
            throw new IllegalArgumentException("Extensible class must be interface or abstract class!");
        }
        this.interfaceClass = interfaceClass;
        this.interfaceName = ClassTypeUtils.getTypeStr(interfaceClass);
        this.listener = listener;
        Extensible extensible = interfaceClass.getAnnotation(Extensible.class);
        if (extensible == null) {
            throw new IllegalArgumentException("Error when load extensible interface " + interfaceName
                    + ", must add annotation @Extensible.");
        } else {
            this.extensible = extensible;
        }

        this.factory = extensible.singleton() ? new ConcurrentHashMap<>() : null;
        this.all = new ConcurrentHashMap<>();
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
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Loading extension of extensible {} from path: {}", interfaceName, path);
        }
        // 默认如果不指定文件名字，就是接口名
        String file = StringUtils.isBlank(extensible.file()) ? interfaceName : extensible.file().trim();
        String fullFileName = path + file;
        try {
            ClassLoader classLoader = ClassLoaderUtils.getClassLoader(getClass());
            Enumeration<URL> urls = classLoader != null ? classLoader.getResources(fullFileName)
                    : ClassLoader.getSystemResources(fullFileName);
            // 可能存在多个文件。
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    // 读取一个文件
                    URL url = urls.nextElement();
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Loading extension of extensible {} from file: {}", interfaceName, url);
                    }
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            readLine(url, line);
                        }
                    } catch (Throwable t) {
                        LOGGER.error("Failed to load extension of extensible " + interfaceName
                                + " from file:" + url, t);
                    } finally {
                        if (reader != null) {
                            reader.close();
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to load extension of extensible " + interfaceName
                    + " from path:" + fullFileName, t);
        }
    }

    @JustForTest
    protected void readLine(URL url, String line) throws Throwable {
        String[] aliasAndClassName = parseAliasAndClassName(line);
        if (aliasAndClassName == null || aliasAndClassName.length != 2) {
            return;
        }
        String alias = aliasAndClassName[0];
        String className = aliasAndClassName[1];
        // 读取配置的实现类
        Class tmp = ClassLoaderUtils.forName(className, true);
        if (!interfaceClass.isAssignableFrom(tmp)) {
            throw new IllegalArgumentException("Error when load extension of extensible " + interfaceName
                    + " from file:" + url + ", " + className + " is not subtype of interface.");
        }
        Class<? extends T> implClass = (Class<? extends T>) tmp;

        // 检查是否有可扩展标识
        Extension extension = implClass.getAnnotation(Extension.class);
        if (extension == null) {
            throw new IllegalArgumentException("Error when load extension of extensible " + interfaceName
                    + " from file:" + url + ", " + className + " must add annotation @Extension.");
        } else {
            String aliasInCode = extension.value();
            if (StringUtils.isBlank(aliasInCode)) {
                throw new IllegalArgumentException("Error when load extension of extensible "
                        + interfaceClass + " from file:" + url + ", " + className
                        + "'s alias of @Extension is blank");
            }
            if (alias == null) {
                alias = aliasInCode;
            } else {
                if (!aliasInCode.equals(alias)) {
                    throw new IllegalArgumentException("Error when load extension of extensible "
                            + interfaceName + " from file:" + url + ", aliases of " + className + " are " +
                            "not equal between " + aliasInCode + "(code) and " + alias + "(file).");
                }
            }
            if (extensible.coded() && extension.code() < 0) {
                throw new IllegalArgumentException("Error when load extension of extensible "
                        + interfaceName + " from file:" + url + ", code of @Extension must >=0 at "
                        + className + ".");
            }
        }
        // 不可以是default和*
        if (alias.equals("default") || alias.equals("*")) {
            throw new IllegalArgumentException("Error when load extension of extensible "
                    + interfaceName + " from file:" + url
                    + ", alias of @Extension must not \"default\" and \"*\" at " + className + ".");
        }
        // 检查是否有存在不对的
        ExtensionClass old = all.get(alias);
        if (old != null) {
            throw new IllegalStateException("Error when load extension of extensible "
                    + interfaceClass + " from file:" + url + ", Duplicate class with same alias: "
                    + alias + ", " + old.getClazz() + " and " + implClass);
        } else {
            ExtensionClass<T> extensionClass = new ExtensionClass<T>();
            extensionClass.setAlias(alias);
            extensionClass.setCode(extension.code());
            extensionClass.setSingleton(extensible.singleton());
            extensionClass.setClazz(implClass);
            extensionClass.setOrder(extension.order());
            all.put(alias, extensionClass);
            if (listener != null) {
                listener.onLoad(extensionClass); // 加载完毕，通知监听器
            }
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

//    /**
//     * 得到服务端的全部自动激活扩展
//     *
//     * @return 自动激活扩展列表
//     */
//    @JustForTest
//    protected List<ExtensionClass<T>> getProviderSideAutoActives() {
//        List<ExtensionClass<T>> extensionClasses = new ArrayList<>();
//        for (ConcurrentHashMap.Entry<String, ExtensionClass<T>> entry : all.entrySet()) {
//            ExtensionClass<T> extensionClass = entry.getValue();
//            if (extensionClass.isAutoActive() && extensionClass.isProviderSide()) {
//                extensionClasses.add(extensionClass);
//            }
//        }
//        Collections.sort(extensionClasses, new OrderComparator());
//        return extensionClasses;
//    }
//
//    /**
//     * 得到调用端的全部自动激活扩展
//     *
//     * @return 自动激活扩展列表
//     */
//    @JustForTest
//    protected List<ExtensionClass<T>> getConsumerSideAutoActives() {
//        List<ExtensionClass<T>> extensionClasses = new ArrayList<>();
//        for (ConcurrentHashMap.Entry<String, ExtensionClass<T>> entry : all.entrySet()) {
//            ExtensionClass<T> extensionClass = entry.getValue();
//            if (extensionClass.isAutoActive() && extensionClass.isConsumerSide()) {
//                extensionClasses.add(extensionClass);
//            }
//        }
//        Collections.sort(extensionClasses, new OrderComparator());
//        return extensionClasses;
//    }


    /**
     * 返回全部扩展类
     *
     * @return 扩展类对象
     */
    @JustForTest
    protected ConcurrentHashMap<String, ExtensionClass<T>> getAllExtensions() {
        return all;
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
     * 得到实例
     *
     * @param alias 别名
     * @return 扩展实例（已判断是否单例）
     */
    public T getExtension(String alias) {
        ExtensionClass<T> extensionClass = getExtensionClass(alias);
        if (extensionClass == null) {
            throw new BsoaRuntimeException(22222, "Extension Not Found :\"" + alias + "\"!");
        } else {
            if (extensible.singleton() && factory != null) {
                T t = factory.get(alias);
                if (t == null) {
                    synchronized (this) {
                        t = factory.get(alias);
                        if (t == null) {
                            t = extensionClass.getExtInstance();
                            factory.put(alias, t);
                        }
                    }
                }
                return t;
            } else {
                return extensionClass.getExtInstance();
            }
        }
    }

    /**
     * 得到实例
     *
     * @param alias    别名
     * @param argTypes 扩展初始化需要的参数类型
     * @param args     扩展初始化需要的参数
     * @return 扩展实例（已判断是否单例）
     */
    public T getExtension(String alias, Class[] argTypes, Object[] args) {
        ExtensionClass<T> extensionClass = getExtensionClass(alias);
        if (extensionClass == null) {
            throw new BsoaRuntimeException(22222, "Extension Not Found :\"" + alias + "\"!");
        } else {
            if (extensible.singleton() && factory != null) {
                T t = factory.get(alias);
                if (t == null) {
                    synchronized (this) {
                        t = factory.get(alias);
                        if (t == null) {
                            t = extensionClass.getExtInstance(argTypes, args);
                            factory.put(alias, t);
                        }
                    }
                }
                return t;
            } else {
                return extensionClass.getExtInstance(argTypes, args);
            }
        }
    }


    /**
     * 从小到大排列
     */
    public static class OrderComparator implements Comparator<ExtensionClass> {
        public int compare(ExtensionClass o1, ExtensionClass o2) {
            // order一样的情况下，先加入的在前面
            return o1.getOrder() - o2.getOrder();
        }
    }
}
