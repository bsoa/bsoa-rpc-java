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
package io.bsoa.rpc.invoke;

import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.BsoaOptions;
import io.bsoa.rpc.common.SystemInfo;
import io.bsoa.rpc.common.annotation.JustForTest;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.transport.AbstractChannel;
import io.bsoa.rpc.transport.ClientTransport;
import io.bsoa.rpc.transport.ClientTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static io.bsoa.rpc.common.utils.ClassUtils.getMethodKey;

/**
 * <p>Stream相关工具类。<br>
 * 注意：如果是开启Stream才加载的功能，例如缓存等，全部放入StreamContext</p>
 * <p>
 * Created by zhangg on 2017/2/11 01:16. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class StreamUtils {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(StreamUtils.class);
    /**
     * 判断全局是否使用了streamObserver功能，如果没有，则减少StreamContext的加载。
     */
    private static volatile boolean streamFeatureUsed = false;
    /**
     * 允许同时的最大的SteamObserver数
     */
    private static final int maxSize = BsoaConfigs.getIntValue(BsoaOptions.STREAM_OBSERVER_MAX_SIZE);

    /**
     * @param interfaceId 接口名
     * @param method      方法名
     * @param impl        实现类
     * @param port        端口
     * @return streamInsKey
     */
    public static String cacheLocalStreamObserver(String interfaceId, String method, StreamObserver impl, int port) {
        if (impl == null) {
            throw new RuntimeException("StreamObserver instance can't be null!");
        }
        int insNumber = StreamContext.STREAM_ID_GEN.incrementAndGet();
        Class clazz = impl.getClass();
        String ip = SystemInfo.getLocalHost();
        String pid = BsoaContext.PID;
        String key;
        if (clazz.getCanonicalName() != null) { // TODO
            key = ip + "_" + port + "_" + pid + "_" + clazz.getCanonicalName() + "_" + Integer.toHexString(insNumber);
        } else {
            key = ip + "_" + port + "_" + pid + "_" + clazz.getName() + "_" + Integer.toHexString(insNumber);
        }
        int currentSize = StreamContext.getInsMapSize();
        if (currentSize > maxSize) {
            // 同时存在的SteamObserver太多，可能是没有调用onCompleted或者onError
            LOGGER.warn("There is to much StreamObserver in local, " +
                    "currentSize is {} > {}, Please check it!", currentSize, maxSize);
        }
        StreamContext.putStreamIns(key, impl);
        streamFeatureUsed = true;
        return key;
    }

    /**
     * 注册Stream方法参数
     *
     * @param key         接口名#方法名
     * @param actualClass 实际类型
     */
    protected static void registryParamOfStreamMethod(String key, Class actualClass) {
        StreamContext.registryParamOfStreamMethod(key, actualClass);
        streamFeatureUsed = true;
    }

    /**
     * 注册Stream方法返回值
     *
     * @param key         接口名#方法名
     * @param actualClass 实际类型
     */
    protected static void registryReturnOfStreamMethod(String key, Class actualClass) {
        StreamContext.registryReturnOfStreamMethod(key, actualClass);
        streamFeatureUsed = true;
    }

    /**
     * 是否为Stream方法
     *
     * @param key 接口名#方法名
     * @return 是否Stream方法
     */
    public static boolean hasStreamObserverParameter(String key) {
        return streamFeatureUsed && StreamContext.hasStreamObserverParameter(key);
    }

    /**
     * 是否为Stream方法
     *
     * @param key 接口名#方法名
     * @return 是否Stream方法
     */
    public static boolean hasStreamObserverReturn(String key) {
        return streamFeatureUsed && StreamContext.hasStreamObserverReturn(key);
    }

    /**
     * 检查时候有流式方法，如果有记录下来
     *
     * @param clazz 接口类
     */
    public static void scanAndRegisterStream(Class clazz) {
        String interfaceId = clazz.getCanonicalName();
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            scanMethod(interfaceId, method);
        }
    }

    @JustForTest
    static void scanMethod(String interfaceId, Method method) {
        Class[] paramClasses = method.getParameterTypes();
        Type[] paramTypes = method.getGenericParameterTypes();
        int cnt = 0;
        String key = null;
        for (int i = 0; i < paramClasses.length; i++) {
            Class paramClazz = paramClasses[i];
            if (StreamObserver.class.isAssignableFrom(paramClazz)) {
                if (++cnt > 1) { // 只能有一个StreamObserver参数
                    throw new BsoaRuntimeException(22222, "Illegal StreamObserver parameter at method " + method.getName()
                            + ", just allow one StreamObserver parameter");
                }
                // 需要解析出StreamObserver<T>里的T的实际类型
                Type paramType = paramTypes[i];
                Class reqActualClass = getActualClass(paramClazz, paramType);
                if (reqActualClass == null) {
                    throw new BsoaRuntimeException(22222,
                            "Must set actual type of StreamObserver, Can not use <?>");
                }
                key = key == null ? getMethodKey(interfaceId, method.getName()) : key;
                registryParamOfStreamMethod(key, reqActualClass);
            }
        }
        Class returnType = method.getReturnType();
        if (StreamObserver.class.isAssignableFrom(returnType)) {
            Class resActualClass = getActualClass(returnType, method.getGenericReturnType());
            if (resActualClass == null) {
                throw new BsoaRuntimeException(22222,
                        "Must set actual type of StreamObserver, Can not use <?>");
            }
            key = key == null ? getMethodKey(interfaceId, method.getName()) : key;
            registryReturnOfStreamMethod(key, resActualClass);
        }
    }

    /**
     * 得到泛型的实际类型（必须知道实际类型）
     *
     * @param clazz 参数类名
     * @param type  参数泛化类型
     * @return 实际类
     */
    private static Class getActualClass(Class<? extends StreamObserver> clazz, Type type) {
        if (type instanceof Class) {
            // 例如直接 StreamObserver 不行
            throw new BsoaRuntimeException(22222,
                    "[24300]Must set actual type of StreamObserver");
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Type[] actualTypes = pt.getActualTypeArguments();
            if (actualTypes.length == 1) {
                Type actualType = actualTypes[0];
                try {
                    if (actualType instanceof ParameterizedType) {
                        // 例如 StreamObserver<List<String>>
                        return (Class) ((ParameterizedType) actualType).getRawType();
                    } else {
                        // 普通的 StreamObserver<String>
                        return (Class) actualType;
                    }
                } catch (ClassCastException e) {
                    // 抛出转换异常 表示为"?"泛化类型， java.lang.ClassCastException:
                    // sun.reflect.generics.reflectiveObjects.WildcardTypeImpl cannot be cast to java.lang.Class
                    throw new BsoaRuntimeException(22222,
                            "[24300]Must set actual type of StreamObserver, Can not use <?>");
                }
            } else {
                throw new BsoaRuntimeException(22222,
                        "[24300]Must set only one actual type of StreamObserver!");
            }
        } else {
            throw new BsoaRuntimeException(22222,
                    "[24300]Must set actual type of StreamObserver!");
        }
    }

    /**
     * 消息发送前预处理（客户端StreamObserver发送前，保存本地实例映射）
     *
     * @param request 请求值
     */
    public static void preMsgSend(RpcRequest request, AbstractChannel channel) {
        Class[] classes = request.getArgClasses();
        int i = 0;
        for (Class clazz : classes) {
            if (StreamObserver.class.isAssignableFrom(clazz)) {
                StreamObserver streamIns = (StreamObserver) request.getArgs()[i];
                if (streamIns == null) {
                    continue;
                } else {
                    String interfaceId = request.getInterfaceName();
                    String methodName = request.getMethodName();
                    // 生成streamInsKey
                    String streamInsKey = StreamUtils.cacheLocalStreamObserver(interfaceId,
                            methodName, streamIns, channel.localAddress().getPort());
                    // 如果是StreamObserver本地实例 则置为一个包装类
                    request.getArgs()[i] = new StreamObserverStub<>(streamInsKey);
                    break;
                }
            }
            i++;
        }
    }

    /**
     * 消息调用前预处理（服务端收到StreamObserver请求，例如生成本地Stream代理类等）
     *
     * @param request 请求对象
     * @param channel 长连接
     */
    public static void preMsgHandle(RpcRequest request, AbstractChannel channel) {
        Class[] types = request.getArgClasses();
        for (int i = 0; i < types.length; i++) {
            Class type = types[i];
            if (StreamObserver.class.isAssignableFrom(type)) {
                StreamObserverStub stub = (StreamObserverStub) request.getArgs()[i];
                streamFeatureUsed = true;
                // 使用一个已有的channel虚拟化一个反向长连接
                ClientTransport clientTransport = ClientTransportFactory.getReverseClientTransport(channel);
                stub.setClientTransport(clientTransport).initByMessage(request);
                break;
            }
        }
    }

    /**
     * 消息调用后处理（StreamObserver返回前）
     *
     * @param request  请求
     * @param response 返回值
     * @param channel  连接
     * @return RpcResponse返回值
     */
    public static void preMessageReturn(RpcRequest request, RpcResponse response, AbstractChannel channel) {
        if (response.hasError()) {
            return;
        }
        Object returnData = response.getReturnData();
        if (returnData instanceof StreamObserver) {
            StreamObserver streamIns = (StreamObserver) returnData;
            // 生成streamInsKey
            String streamInsKey = StreamUtils.cacheLocalStreamObserver(request.getInterfaceName(),
                    request.getMethodName(), streamIns, channel.localAddress().getPort());
            // 如果是StreamObserver本地实例 则置为StreamObserverStub再发给服务端
            response.setReturnData(new StreamObserverStub<>(streamInsKey));
        }
    }

    /**
     * 客户端收到请求前预处理（调用端收到StreamObserver请求，例如生成本地Stream代理类等）
     *
     * @param response        响应
     * @param clientTransport 客户端连接
     */
    public static void preMsgReceive(RpcResponse response, ClientTransport clientTransport) {
        if (response.hasError()) {
            return;
        }
        Object returnData = response.getReturnData();
        if (returnData instanceof StreamObserverStub) {
            StreamObserverStub stub = (StreamObserverStub) returnData;
            streamFeatureUsed = true;
            // 使用一个已有的长连接
            stub.setClientTransport(clientTransport).initByMessage(response);
        }
    }
}
