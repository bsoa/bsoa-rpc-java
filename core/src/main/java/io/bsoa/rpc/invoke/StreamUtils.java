/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bsoa.rpc.invoke;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.base.Invoker;
import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.SystemInfo;
import io.bsoa.rpc.common.annotation.JustForTest;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.context.StreamContext;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.message.HeadKey;
import io.bsoa.rpc.message.RPCMessage;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.proxy.ProxyFactory;
import io.bsoa.rpc.transport.AbstractChannel;
import io.bsoa.rpc.transport.ClientTransport;
import io.bsoa.rpc.transport.ClientTransportFactory;

/**
 * <p></p>
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

    private static ConcurrentHashMap<Class, AtomicInteger> callbackCountMap = new ConcurrentHashMap<Class, AtomicInteger>();
    /**
     * 接口+方法 ： 实际的StreamObserver数据类型
     */
    private static ConcurrentHashMap<String, Class> callbackNames = new ConcurrentHashMap<String, Class>();

    private static ConcurrentHashMap<String, ClientTransport> clientTransportMap = new ConcurrentHashMap<String, ClientTransport>();

    private static ConcurrentHashMap<StreamObserver, Integer> instancesNumMap = new ConcurrentHashMap<StreamObserver, Integer>();//instance number

    private static Integer getInstanceNumber(Object impl, String interfaceId, String method, int port) {
        Class clazz = impl.getClass();
        AtomicInteger num = callbackCountMap.get(clazz);
        if (num == null) {
            num = initNum(clazz);
        }

        if (num.intValue() >= 2000) {
            throw new RuntimeException("[JSF-24301]Callback instance have exceeding 2000 for type:" + clazz + " interfaceId " + interfaceId + " method " + method + " port" + port);
        }

        return num.getAndIncrement();
    }

    private static AtomicInteger initNum(Class clazz) {
        AtomicInteger num = callbackCountMap.get(clazz);
        if (num == null) {
            num = new AtomicInteger(0);
            AtomicInteger nu = callbackCountMap.putIfAbsent(clazz, num);
            if (nu != null) {
                num = nu;
            }
        }
        return num;
    }

    /*
    *max default is 1000;
    *
    */
    public static String clientRegisterStream(String interfaceId, String method, StreamObserver impl, int port) {

        if (impl == null) {
            throw new RuntimeException("Stream Ins cann't be null!");
        }
        String key = null;

        Integer insNumber = instancesNumMap.get(impl);
        if (insNumber == null) {
            insNumber = getInstanceNumber(impl, interfaceId, method, port);
            Integer num = instancesNumMap.putIfAbsent(impl, insNumber);
            if (num != null) {
                insNumber = num;
            }
        }

        Class clazz = impl.getClass();

        String ip = SystemInfo.getLocalHost();
        String pid = BsoaContext.PID;
        if (clazz.getCanonicalName() != null) {
            key = ip + "_" + port + "_" + pid + "_" + clazz.getCanonicalName() + "_" + insNumber;
        } else {
            key = ip + "_" + port + "_" + pid + "_" + clazz.getName() + "_" + insNumber;
        }

        StreamContext.putStreamIns(key, impl);
        streamFeatureUsed = true;
        return key;
    }

    private static String getName(String interfaceId, String methodName) {
        return interfaceId + "::" + methodName;
    }

    /**
     * 注册Stream方法参数
     *
     * @param interfaceName 接口名
     * @param methodName    方法名
     * @param actualClass   实际类型
     */
    protected static void registryParamOfStreamMethod(String interfaceName, String methodName, Class actualClass) {
        String key = getName(interfaceName, methodName);
        StreamContext.registryParamOfStreamMethod(key, actualClass);
        streamFeatureUsed = true;
    }

    /**
     * 注册Stream方法返回值
     *
     * @param interfaceName 接口名
     * @param methodName    方法名
     * @param actualClass   实际类型
     */
    protected static void registryReturnOfStreamMethod(String interfaceName, String methodName, Class actualClass) {
        String key = getName(interfaceName, methodName);
        StreamContext.registryReturnOfStreamMethod(key, actualClass);
        streamFeatureUsed = true;
    }

    /**
     * 是否为Stream方法
     *
     * @param interfaceName 接口名
     * @param methodName    方法名
     * @return 是否Stream方法
     */
    public static boolean hasStreamObserverParameter(String interfaceName, String methodName) {
        if (streamFeatureUsed) {
            String key = getName(interfaceName, methodName);
            return StreamContext.hasStreamObserverParameter(key);
        }
        return false;
    }

    /**
     * 是否为Stream方法
     *
     * @param interfaceName 接口名
     * @param methodName    方法名
     * @return 是否Stream方法
     */
    public static boolean hasStreamObserverReturn(String interfaceName, String methodName) {
        if (streamFeatureUsed) {
            String key = getName(interfaceName, methodName);
            return StreamContext.hasStreamObserverReturn(key);
        }
        return false;
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
    protected static void scanMethod(String interfaceId, Method method) {
        Class[] paramClasses = method.getParameterTypes();
        Type[] paramTypes = method.getGenericParameterTypes();
        int cnt = 0;
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
                registryParamOfStreamMethod(interfaceId, method.getName(), reqActualClass);
            }
        }
        Class returnType = method.getReturnType();
        if (StreamObserver.class.isAssignableFrom(returnType)) {
            Class resActualClass = getActualClass(returnType, method.getGenericReturnType());
            if (resActualClass == null) {
                throw new BsoaRuntimeException(22222,
                        "Must set actual type of StreamObserver, Can not use <?>");
            }
            registryReturnOfStreamMethod(interfaceId, method.getName(), resActualClass);
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
                    "[JSF-24300]Must set actual type of StreamObserver");
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
                            "[JSF-24300]Must set actual type of StreamObserver, Can not use <?>");
                }
            } else {
                throw new BsoaRuntimeException(22222,
                        "[JSF-24300]Must set only one actual type of StreamObserver!");
            }
        } else {
            throw new BsoaRuntimeException(22222,
                    "[JSF-24300]Must set actual type of StreamObserver!");
        }
    }

    /**
     * 消息发送前预处理（客户端StreamObserver发送前，保存本地实例映射，设置头部）
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
                    String streamInsKey = StreamUtils.clientRegisterStream(interfaceId,
                            methodName, streamIns, channel.getLocalAddress().getPort());
                    // 在Header加上streamInsKey关键字，客户端特殊处理
                    request.addHeadKey(HeadKey.STREAM_INS_KEY, streamInsKey);
                    // 如果是StreamObserver本地实例 则置为null再发给服务端
                    request.getArgs()[i] = null;
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
        String streamInsKey = (String) request.getHeadKey(HeadKey.STREAM_INS_KEY);
        if (streamInsKey == null) {
            // 参数里有，但是客户端没传，忽略
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{}.{} has StreamObserver param, but receive null!",
                        request.getInterfaceName(), request.getMethodName());
            }
        } else {
            Class[] types = request.getArgClasses();
            int i = 0;
            for (Class type : types) {
                if (StreamObserver.class.isAssignableFrom(type)) {
                    Object old = request.getArgs()[i];
                    if (old != null) {
                        throw new BsoaRpcException(22222, "stream observer in message must be null");
                    }
                    String key = getName(request.getInterfaceName(), request.getMethodName());
                    streamFeatureUsed = true;
                    //Class actualType = StreamContext.getParamTypeOfStreamMethod(key);
                    // 使用一个已有的channel虚拟化一个反向长连接
                    ClientTransport clientTransport = ClientTransportFactory.getReverseClientTransport(channel);
                    StreamObserver proxy = buildStreamObserverProxy(clientTransport, streamInsKey, request);
                    request.getArgs()[i] = proxy; // 重新设置回参数列表
                    break;
                }
                i++;
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
    public static void postMsgHandle(RpcRequest request, RpcResponse response, AbstractChannel channel) {
        if (response.hasError()) {
            return;
        }
        Object returnData = response.getReturnData();
        if (returnData instanceof StreamObserver) {
            StreamObserver streamIns = (StreamObserver) returnData;
            String interfaceId = request.getInterfaceName();
            String methodName = request.getMethodName();
            // 生成streamInsKey
            String streamInsKey = StreamUtils.clientRegisterStream(interfaceId,
                    methodName, streamIns, channel.getLocalAddress().getPort());
            // 在Header加上streamInsKey关键字，服务端特殊处理
            response.addHeadKey(HeadKey.STREAM_INS_KEY, streamInsKey);
            // 如果是StreamObserver本地实例 则置为null再发给服务端
            response.setReturnData(null);
        }
    }

    /**
     * 客户端收到请求前预处理（调用端收到StreamObserver请求，例如生成本地Stream代理类等）
     *
     * @param response 响应
     * @param clientTransport  客户端连接
     */
    public static void preMsgReceive(RpcResponse response, ClientTransport clientTransport) {
        if (response.hasError()) {
            return;
        }
        String streamInsKey = (String) response.getHeadKey(HeadKey.STREAM_INS_KEY);
        if (response.getReturnData() != null) {
            throw new BsoaRpcException(22222, "stream observer in message must be null");
        }
        streamFeatureUsed = true;
        StreamObserver proxy = buildStreamObserverProxy(clientTransport, streamInsKey, response);
        response.setReturnData(proxy); // 重新设置回参数列表
    }

    /**
     * 构建StreamObserver代理类
     *
     * @param clientTransport 长连接
     * @param streamInsKey    StreamObserver关键字
     * @param actualType      实际的类型
     * @param rpcMessage      当前请求
     * @return StreamObserver
     */
    public static StreamObserver buildStreamObserverProxy(
            ClientTransport clientTransport, String streamInsKey, RPCMessage rpcMessage) {
        // 看看之前有没有本地代理类
        StreamObserver observer = StreamContext.getStreamProxy(streamInsKey);
        if (observer != null) { //if channel failed remember to remove the proxy instance from the proxyMap
            return observer;
        }
        Invoker invoker = new StreamInvoker(clientTransport, streamInsKey, rpcMessage);
        try {
            // 生成一个本地代理类
            StreamObserver proxy = ProxyFactory.buildProxy(BsoaConfigs.getStringValue(BsoaConfigs.DEFAULT_PROXY),
                    StreamObserver.class, invoker);
            StreamContext.putStreamProxy(streamInsKey, proxy);
            return proxy;
        } catch (Exception e) {
            throw new BsoaRpcException(22222,
                    "error when create the proxy of StreamObserver with streamInsKey: " + streamInsKey, e);
        }
    }
}
