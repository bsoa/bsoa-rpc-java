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
package io.bsoa.rpc.common;

import io.bsoa.rpc.common.json.JSON;
import io.bsoa.rpc.common.utils.ClassLoaderUtils;
import io.bsoa.rpc.common.utils.FileUtils;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>配置加载器和操作入口</p>
 * <p>
 * Created by zhangg on 2016/12/10 22:22. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class BsoaConfigs {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(BsoaConfigs.class);
    /**
     * 全部配置
     */
    private final static ConcurrentHashMap<String, Object> CFG = new ConcurrentHashMap<>();
    /**
     * 配置变化监听器
     */
    private final static ConcurrentHashMap<String, List<BsoaConfigListener>> CFG_LISTENER 
            = new ConcurrentHashMap<>();

    static {
        init(); // 加载配置文件
    }

    private static void init() {
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Load config from file start!");
            }
            // loadDefault
            String json = FileUtils.file2String(BsoaConfigs.class, "bsoa_default.json", "UTF-8");
            Map map = JSON.parseObject(json, Map.class);
            CFG.putAll(map);

            // loadCustom
            loadCustom("bsoa.json");
            loadCustom("META-INF/bsoa.json");

            // load system properties
            CFG.putAll(new HashMap(System.getProperties()));

            // print if debug
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Load config from file end!");
                for (Map.Entry<String, Object> entry : CFG.entrySet()) {
                    LOGGER.debug("{}: {}", entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, "", e);
        }
    }

    /**
     * 加载自定义配置文件
     *
     * @param fileName 文件名
     * @throws IOException 加载异常
     */
    private static void loadCustom(String fileName) throws IOException {
        ClassLoader classLoader = ClassLoaderUtils.getClassLoader(BsoaConfigs.class);
        Enumeration<URL> urls = classLoader != null ? classLoader.getResources(fileName)
                : ClassLoader.getSystemResources(fileName);
        if (urls != null) { // 可能存在多个文件
            List<CfgFile> allFile = new ArrayList<>();
            while (urls.hasMoreElements()) {
                // 读取每一个文件
                URL url = urls.nextElement();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Loading custom config from file: {}", url);
                }
                try (InputStreamReader input = new InputStreamReader(url.openStream(), "utf-8");
                     BufferedReader reader = new BufferedReader(input)) {
                    StringBuilder context = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        context.append(line).append("\n");
                    }
                    Map map = JSON.parseObject(context.toString(), Map.class);
                    Integer order = (Integer) map.get(BsoaOptions.BSOA_CFG_ORDER);
                    allFile.add(new CfgFile(url, order == null ? 0 : order, map));
                }
            }
            Collections.sort(allFile, (o1, o2) -> o1.getOrder() - o2.getOrder());  // 从小到大排下序
            for (CfgFile file : allFile) {
                CFG.putAll(file.getMap()); // 顺序加载，越大越后加载
            }
        }
    }

    public static void putValue(String key, Object newValue) {
        Object oldValue = CFG.get(key);
        if (changed(oldValue, newValue)) {
            CFG.put(key, newValue);
            List<BsoaConfigListener> bsoaConfigListeners = CFG_LISTENER.get(key);
            for (BsoaConfigListener bsoaConfigListener : bsoaConfigListeners) {
                bsoaConfigListener.onChange(oldValue, newValue);
            }
        }
    }

    public static boolean getBooleanValue(String primaryKey) {
        Boolean val = (Boolean) CFG.get(primaryKey);
        if (val == null) {
            throw new BsoaRuntimeException(22222, "Not found key: " + primaryKey);
        } else {
            return val;
        }
    }

    public static boolean getBooleanValue(String primaryKey, String secondaryKey) {
        Boolean val = (Boolean) CFG.get(primaryKey);
        if (val == null) {
            val = (Boolean) CFG.get(secondaryKey);
            if (val == null) {
                throw new BsoaRuntimeException(22222, "Not found key: " + primaryKey + "/" + secondaryKey);
            } else {
                return val;
            }
        } else {
            return val;
        }
    }

    public static int getIntValue(String primaryKey) {
        Integer val = (Integer) CFG.get(primaryKey);
        if (val == null) {
            throw new BsoaRuntimeException(22222, "Not found key: " + primaryKey);
        } else {
            return val;
        }
    }

    public static <T> T getOrDefaultValue(String primaryKey, T defaultValue) {
        Object val = CFG.get(primaryKey);
        return val == null ? defaultValue : (T) val;
    }

    public static int getIntValue(String primaryKey, String secondaryKey) {
        Integer val = (Integer) CFG.get(primaryKey);
        if (val == null) {
            val = (Integer) CFG.get(secondaryKey);
            if (val == null) {
                throw new BsoaRuntimeException(22222, "Not found key: " + primaryKey + "/" + secondaryKey);
            } else {
                return val;
            }
        } else {
            return val;
        }
    }

    public static <T extends Enum<T>> T getEnumValue(String primaryKey, Class<T> enumClazz) {
        String val = (String) CFG.get(primaryKey);
        if (val == null) {
            throw new BsoaRuntimeException(22222, "Not Found Key: " + primaryKey);
        } else {
            return Enum.valueOf(enumClazz, val);
        }
    }

    public static String getStringValue(String primaryKey) {
        String val = (String) CFG.get(primaryKey);
        if (val == null) {
            throw new BsoaRuntimeException(22222, "Not Found Key: " + primaryKey);
        } else {
            return val;
        }
    }

    public static String getStringValue(String primaryKey, String secondaryKey) {
        String val = (String) CFG.get(primaryKey);
        if (val == null) {
            val = (String) CFG.get(secondaryKey);
            if (val == null) {
                throw new BsoaRuntimeException(22222, "Not found key: " + primaryKey + "/" + secondaryKey);
            } else {
                return val;
            }
        } else {
            return val;
        }
    }

    public static List getListValue(String primaryKey) {
        List val = (List) CFG.get(primaryKey);
        if (val == null) {
            throw new BsoaRuntimeException(22222, "Not found key: " + primaryKey);
        } else {
            return val;
        }
    }

    /**
     * 订阅配置变化
     *
     * @param key            关键字
     * @param bsoaConfigListener 配置监听器
     * @see BsoaOptions
     */
    public static synchronized void subscribe(String key, BsoaConfigListener bsoaConfigListener) {
        List<BsoaConfigListener> listeners = CFG_LISTENER.get(key);
        if (listeners == null) {
            listeners = new ArrayList<>();
            CFG_LISTENER.put(key, listeners);
        }
        listeners.add(bsoaConfigListener);
    }

    /**
     * 取消订阅配置变化
     *
     * @param key            关键字
     * @param bsoaConfigListener 配置监听器
     * @see BsoaOptions
     */
    public static synchronized void unSubscribe(String key, BsoaConfigListener bsoaConfigListener) {
        List<BsoaConfigListener> listeners = CFG_LISTENER.get(key);
        if (listeners != null) {
            listeners.remove(bsoaConfigListener);
            if (listeners.size() == 0) {
                CFG_LISTENER.remove(key);
            }
        }
    }

    /**
     * 值是否发生变化
     *
     * @param oldObj 旧值
     * @param newObj 新值
     * @return 是否变化
     */
    protected static boolean changed(Object oldObj, Object newObj) {
        return oldObj == null ?
                newObj != null :
                !oldObj.equals(newObj);
    }

    /**
     * 用于排序的一个类
     */
    private static class CfgFile {
        private final URL url;
        private final int order;
        private final Map map;

        public CfgFile(URL url, int order, Map map) {
            this.url = url;
            this.order = order;
            this.map = map;
        }

        public URL getUrl() {
            return url;
        }

        public int getOrder() {
            return order;
        }

        public Map getMap() {
            return map;
        }
    }

    /**
     * 配置变更会拿到通知
     */
    public interface BsoaConfigListener<T> {
        public void onChange(T oldValue, T newValue);
    }

}