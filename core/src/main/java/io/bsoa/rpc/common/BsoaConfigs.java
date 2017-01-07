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
package io.bsoa.rpc.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.json.JSON;
import io.bsoa.rpc.common.utils.ClassLoaderUtils;
import io.bsoa.rpc.common.utils.FileUtils;
import io.bsoa.rpc.exception.BsoaRuntimeException;

/**
 * <p></p>
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

    private final static ConcurrentHashMap<String, List<ConfigListener>> CFG_LISTENER = new ConcurrentHashMap<>();

    static {
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Load config from file start!");
            }
            loadDefault();
            loadCustom();
            // FIXME 读取system.properties
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Load config from file ok!");
                for (Map.Entry<String, Object> entry : CFG.entrySet()) {
                    LOGGER.debug("{}: {}", entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            throw new BsoaRuntimeException(22222, "", e);
        }
    }

    private static void loadDefault() throws IOException {
        String json = FileUtils.file2String(BsoaConfigs.class, "bsoa_default.json", "UTF-8");
        Map map = JSON.parseObject(json, Map.class);
        CFG.putAll(map);
    }

    private static void loadCustom() throws IOException {
        loadCustom("bsoa.json");
        loadCustom("META-INF/bsoa.json");
    }

    private static void loadCustom(String fileName) throws IOException {
        ClassLoader classLoader = ClassLoaderUtils.getClassLoader(BsoaConfigs.class);
        Enumeration<URL> urls = classLoader != null ? classLoader.getResources(fileName)
                : ClassLoader.getSystemResources(fileName);
        // 可能存在多个文件。
        if (urls != null) {
            while (urls.hasMoreElements()) {
                // 读取一个文件
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
                    CFG.putAll(map);
                } catch (IOException e) {
                    throw e;
                }
            }
        }
    }

    public static void putValue(String key, Object newValue) {
        Object oldValue = CFG.get(key);
        if (oldValue != null && oldValue.equals(newValue)) {
            // No onChange
        } else {
            CFG.put(key, newValue);
            List<ConfigListener> configListeners = CFG_LISTENER.get(key);
            for (ConfigListener configListener : configListeners) {
                configListener.onChange(oldValue, newValue);
            }
        }
    }

    public static boolean getBooleanValue(String primaryKey) {
        Boolean val = (Boolean) CFG.get(primaryKey);
        if (val == null) {
            throw new BsoaRuntimeException(22222, "");
        } else {
            return val;
        }
    }

    public static boolean getBooleanValue(String primaryKey, String secondaryKey) {
        Boolean val = (Boolean) CFG.get(primaryKey);
        if (val == null) {
            val = (Boolean) CFG.get(secondaryKey);
            if (val == null) {
                throw new BsoaRuntimeException(22222, "");
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
            throw new BsoaRuntimeException(22222, "Not found key:" + primaryKey);
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
                throw new BsoaRuntimeException(22222, "");
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
            throw new BsoaRuntimeException(22222, "Not Found");
        } else {
            return Enum.valueOf(enumClazz, val);
        }
    }

    public static String getStringValue(String primaryKey) {
        String val = (String) CFG.get(primaryKey);
        if (val == null) {
            throw new BsoaRuntimeException(22222, "Not Found");
        } else {
            return val;
        }
    }

    public static String getStringValue(String primaryKey, String secondaryKey) {
        String val = (String) CFG.get(primaryKey);
        if (val == null) {
            val = (String) CFG.get(secondaryKey);
            if (val == null) {
                throw new BsoaRuntimeException(22222, "");
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
            throw new BsoaRuntimeException(22222, "Not Found");
        } else {
            return val;
        }
    }

    /**
     * 扩展点加载的路径
     */
    public static final String EXTENSION_LOAD_PATH = "extension.load.path";

    /**
     * 默认协议
     */
    public final static String DEFAULT_PROTOCOL = "default.protocol";
    /**
     * 默认序列化
     */
    public final static String DEFAULT_SERIALIZATION = "default.serialization";
    /**
     * 默认负载均衡算法
     */
    public final static String DEFAULT_LOADBALANCER = "default.loadbalancer";
    /**
     * 默认代理类型
     */
    public final static String DEFAULT_PROXY = "default.proxy";
    /**
     * 默认字符集 utf-8
     */
    public final static String DEFAULT_CHARSET = "default.charset";
    /**
     * 默认传输层
     */
    public final static String DEFAULT_TRANSPORT = "default.transport";
    /**
     * 默认压缩算法
     */
    public final static String DEFAULT_COMPRESS = "default.compress";
    /**
     * 默认集群策略
     */
    public final static String DEFAULT_CLUSTER = "default.cluster";

    /**
     * 默认连注册中心的超时时间
     */
    public static final String REGISTRY_CONNECT_TIMEOUT = "registry.connect.timeout";
    /**
     * 默认连注册中心的超时时间
     */
    public static final String REGISTRY_REGISTER_BATCH = "registry.register.batch";

    /**
     * 默认服务端业务线程池类型
     */
    public final static String SERVER_POOL_TYPE = "server.pool.type";
    /**
     * 默认服务端业务线程池最小
     */
    public final static String SERVER_POOL_CORE = "server.pool.core";
    /**
     * 默认服务端业务线程池最大
     */
    public final static String SERVER_POOL_MAX = "server.pool.max";
    /**
     * 默认服务端业务线程池队列类型
     */
    public final static String SERVER_POOL_QUEUE_TYPE = "server.pool.queue.type";
    /**
     * 默认服务端业务线程池队列
     */
    public final static String SERVER_POOL_QUEUE = "server.pool.queue";
    /**
     * 默认服务端业务线程池回收时间
     */
    public final static String SERVER_POOL_TIME = "server.pool.time";
    /**
     * 默认启动端口，包括不配置或者随机，都从此端口开始计算
     */
    public static final String SERVER_PORT_START = "server.port.start";
    /**
     * 默认启动端口，包括不配置或者随机，都从此端口开始计算
     */
    public static final String SERVER_PORT_END = "server.port.end";
    /**
     * 默认发布路径 "/"
     */
    public static final String SERVER_CONTEXT_PATH = "server.context.path";
    /**
     * 默认权重
     */
    public final static String PROVIDER_WEIGHT = "provider.weight";
    /**
     * 默认服务启动延迟
     */
    public static final String PROVIDER_DELAY = "provider.delay";
    /**
     * 默认失败重试次数
     */
    public static final String CONSUMER_RETRIES = "consumer.retries";
    /**
     * 默认一个ip端口建立的长连接数量
     */
    public static final String CONSUMER_CONNECTION_NUM = "consumer.connection.num";
    /**
     * Consumer给Provider发心跳的间隔
     */
    public final static String CONSUMER_HEARTBEAT_PERIOD = "consumer.heartbeat.period";
    /**
     * Consumer给Provider重连的间隔
     */
    public final static String CONSUMER_RECONNECT_PERIOD = "consumer.reconnect.period";
    /**
     * Consumer共享心跳重连线程？
     */
    public final static String CONSUMER_SHARE_RECONNECT_THREAD = "consumer.share.reconnect.thread";
    /**
     * 默认consumer连provider超时时间
     */
    public static final String CLIENT_CONNECT_TIMEOUT = "client.connect.timeout";
    /**
     * 默认consumer断开时等待结果的超时时间
     */
    public static final String CLIENT_DISCONNECT_TIMEOUT = "client.disconnect.timeout";
    /**
     * 默认consumer调用provider超时时间
     */
    public static final String CLIENT_INVOKE_TIMEOUT = "client.invoke.timeout";
    /**
     * 默认回调线程池类型
     */
    public final static String CALLBACK_POOL_TYPE = "callback.pool.type";
    /**
     * 默认回调线程池最小
     */
    public final static String CALLBACK_POOL_CORE = "callback.pool.core";
    /**
     * 默认回调线程池最大
     */
    public final static String CALLBACK_POOL_MAX = "callback.pool.max";
    /**
     * 默认回调线程池队列
     */
    public final static String CALLBACK_POOL_QUEUE = "callback.pool.queue";
    /**
     * 默认回调线程池回收时间
     */
    public final static String CALLBACK_POOL_TIME = "callback.pool.time";
    /**
     * 默认开启epoll？
     */
    public final static String TRANSPORT_USE_EPOLL = "transport.use.epoll";
    /**
     * 默认服务端 数据包限制
     */
    public final static String TRANSPORT_PAYLOAD_MAX = "transport.payload.max";
    /**
     * 默认IO的buffer大小
     */
    public final static String TRANSPORT_BUFFER_SIZE = "transport.buffer.size";
    /**
     * 最大IO的buffer大小
     */
    public final static String TRANSPORT_BUFFER_MAX = "transport.buffer.max";
    /**
     * 最小IO的buffer大小
     */
    public final static String TRANSPORT_BUFFER_MIN = "transport.buffer.min";
    /**
     * 是否跨接口的长连接复用
     */
    public final static String TRANSPORT_CONNECTION_REUSE = "transport.connection.reuse";
    /**
     * 客户端IO线程池
     */
    public final static String TRANSPORT_CLIENT_IO_THREADS = "transport.client.io.threads";
    /**
     * 客户端IO 比例：用户在代码中使用到了Runnable和ScheduledFutureTask，请合理设置ioRatio的比例，
     * 通过NioEventLoop的setIoRatio(int ioRatio)方法可以设置该值，默认值为50，即I/O操作和用户自定义任务的执行时间比为1：1
     */
    public final static String TRANSPORT_CLIENT_IO_RATIO = "transport.client.io.ratio";
    /**
     * 客户端IO 比例：用户在代码中使用到了Runnable和ScheduledFutureTask，请合理设置ioRatio的比例，
     * 通过NioEventLoop的setIoRatio(int ioRatio)方法可以设置该值，默认值为50，即I/O操作和用户自定义任务的执行时间比为1：1
     */
    public final static String TRANSPORT_SERVER_IO_RATIO = "transport.server.io.ratio";
    /**
     * 连接重用
     */
    public final static String TRANSPORT_SERVER_BACKLOG = "transport.server.backlog";
    /**
     * 连接重用
     */
    public final static String TRANSPORT_SERVER_REUSE_ADDR = "transport.server.reuseAddr";
    /**
     * 保存长连接
     */
    public final static String TRANSPORT_SERVER_KEEPALIVE = "transport.server.keepAlive";
    /**
     * 无延迟
     */
    public final static String TRANSPORT_SERVER_TCPNODELAY = "transport.server.tcpNoDelay";
    /**
     * 服务端boss线程数
     */
    public final static String TRANSPORT_SERVER_BOSS_THREADS = "transport.server.boss.threads";
    /**
     * 服务端IO线程数
     */
    public final static String TRANSPORT_SERVER_IO_THREADS = "transport.server.io.threads";
    /**
     * 最大长连接数
     */
    public final static String TRANSPORT_SERVER_MAX_CONNECTION = "transport.server.max.connection";
    /**
     * 是否允许telnet
     */
    public final static String TRANSPORT_SERVER_TELNET = "transport.server.telnet";
    /**
     * 是否守护线程，true随主线程退出而退出，false需要主动退出
     */
    public final static String TRANSPORT_SERVER_DAEMON = "transport.server.daemon";
    /**
     * 线程方法模型
     */
    public final static String TRANSPORT_SERVER_DISPATCHER = "transport.server.dispatcher";
    /**
     * 是否一个端口支持多协议
     */
    public final static String TRANSPORT_SERVER_PROTOCOL_ADAPTIVE = "transport.server.protocol.adaptive";

    /**
     * 是否开启压缩
     */
    public final static String COMPRESS_OPEN = "compress.open";
    /**
     * 开启压缩的大小基线
     */
    public final static String COMPRESS_SIZE_BASELINE = "compress.size.baseline";

    public static synchronized void subscribe(String key, ConfigListener configListener) {
        List<ConfigListener> listeners = CFG_LISTENER.get(key);
        if (listeners == null) {
            listeners = new ArrayList<>();
            CFG_LISTENER.put(key, listeners);
        }
        listeners.add(configListener);
    }

    public static synchronized void unSubscribe(String key, ConfigListener configListener) {
        List<ConfigListener> listeners = CFG_LISTENER.get(key);
        if (listeners != null) {
            listeners.remove(configListener);
            if (listeners.size() == 0) {
                CFG_LISTENER.remove(key);
            }
        }
    }

    /**
     *
     */
    public interface ConfigListener<T> {
        public void onChange(T oldValue, T newValue);
    }

}