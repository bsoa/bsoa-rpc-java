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
package io.bsoa.rpc.invoke;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.base.Invoker;
import io.bsoa.rpc.common.SystemInfo;
import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.message.HeadKey;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.proxy.ProxyFactory;
import io.bsoa.rpc.transport.AbstractChannel;
import io.bsoa.rpc.transport.ClientTransport;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/2/10 22:22. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class CallbackUtils {

    private final static Logger logger = LoggerFactory.getLogger(CallbackUtils.class);

    private static ConcurrentHashMap<Class, AtomicInteger> callbackCountMap = new ConcurrentHashMap<Class, AtomicInteger>();

    private static ConcurrentHashMap<String, ClientTransport> clientTransportMap = new ConcurrentHashMap<String, ClientTransport>();

    private static ConcurrentHashMap<String, Callback> proxyMap = new ConcurrentHashMap<String, Callback>();//cache the ServerCallback stub proxy instance in serverside.

    private static ConcurrentHashMap<Callback, Integer> instancesNumMap = new ConcurrentHashMap<Callback, Integer>();//instance number

    private static boolean hasServerCallbackParam(Method method) {
        Class[] clazzList = method.getParameterTypes();
        int cnt = 0;
        for (Class clazz : clazzList) {
            logger.trace("clazz - {}", clazz);
            if (Callback.class.isAssignableFrom(clazz)) {
                cnt++;
            }
        }
        if (cnt > 1) {
            throw new BsoaRuntimeException(22222, "Illegal ServerCallback parameter at method " + method.getName()
                    + ",just allow one ServerCallback parameter");
        }
        return cnt == 1;
    }

    /**
     * 服务端需要提前注册ServerCallback事件，ServerCallback<T>里的T一定要指定类型
     *
     * @param clazz 接口类
     */
    public static void scanAndRegisterCallBack(Class clazz) {
        String interfaceId = clazz.getCanonicalName();
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (hasServerCallbackParam(method)) {
                // 需要解析出ServerCallback<T>里的T的实际类型
                Class reqRealClass = null;
                Class resRealClass = null;
                Type[] tps = method.getGenericParameterTypes();
                for (Type tp : tps) {
                    if (tp instanceof Class) {
                        Class cls = (Class) tp;
                        if (cls.equals(Callback.class)) {
                            throw new BsoaRuntimeException(22222, "[JSF-24300]Must set actual type of ServerCallback");
                        } else {
                            continue;
                        }
                    }
                    if (tp instanceof ParameterizedType) {
                        ParameterizedType pt = (ParameterizedType) tp;
                        if (pt.getRawType().equals(Callback.class)) {
                            Type[] actualTypes = pt.getActualTypeArguments();
                            if (actualTypes.length == 2) {
                                reqRealClass = checkClass(actualTypes[0]);
                                resRealClass = checkClass(actualTypes[1]);
                                break;
                            }
                        }
                    }
                }
                if (reqRealClass == null) {
                    throw new BsoaRuntimeException(22222, "[JSF-24300]Must set actual type of ServerCallback, Can not use <?>");
                }

                callbackRegister(interfaceId, method.getName(), reqRealClass);
//                HashSet set = new HashSet<Class<?>>();
//                CodecUtils.checkAndRegistryClass(reqRealClass, set);
//                CodecUtils.checkAndRegistryClass(resRealClass, set);
            }
        }
    }

    private static Class checkClass(Type actualType) {
        Class realclass;
        try {
            if (actualType instanceof ParameterizedType) {
                // 例如 ServerCallback<List<String>>
                realclass = (Class) ((ParameterizedType) actualType).getRawType();
            } else {
                // 普通的 ServerCallback<String>
                realclass = (Class) actualType;
            }
            return realclass;
        } catch (ClassCastException e) {
            // 抛出转换异常 表示为"?"泛化类型， java.lang.ClassCastException:
            // sun.reflect.generics.reflectiveObjects.WildcardTypeImpl cannot be cast to java.lang.Class
            throw new BsoaRuntimeException(22222, "[JSF-24300]Must set actual type of ServerCallback, Can not use <?>");
        }
    }

    /*
     *max default is 1000;
     *
     */
    public static String clientRegisterCallback(String interfaceId, String method, Object impl, int port) {

        if (impl == null) {
            throw new RuntimeException("ServerCallback Ins cann't be null!");
        }
        String key = null;

        Integer insNumber = instancesNumMap.get(impl);
        if (insNumber == null) {
            insNumber = getInstanceNumber(impl, interfaceId, method, port);
            Integer num = instancesNumMap.putIfAbsent((Callback) impl, insNumber);
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

        // FIXME ClientCallbackHandler.registerServerCallback(key, (ServerCallback) impl);
        return key;

    }

    private static Integer getInstanceNumber(Object impl, String interfaceId, String method, int port) {
        Class clazz = impl.getClass();
        AtomicInteger num = callbackCountMap.get(clazz);
        if (num == null) {
            num = initNum(clazz);
        }

        if (num.intValue() >= 2000) {
            throw new RuntimeException("[JSF-24301]ServerCallback instance have exceeding 2000 for type:" + clazz + " interfaceId " + interfaceId + " method " + method + " port" + port);
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

    /**
     * 服务端接收带有ServerCallback的请求
     *
     * @param msg     请求
     * @param channel 连接
     * @return
     */
    public static RpcRequest msgHandle(RpcRequest msg, ClientTransport channel) {
        Class[] types = msg.getArgClasses();
        Callback callback = null;
        String callbackInsId = (String) msg.getHeadKey(HeadKey.CALLBACK_INS_KEY);
        if (callbackInsId == null) {
            throw new RuntimeException(" Server side handle RpcRequest ServerCallbackInsId can not be null! ");
        }
        int i = 0;
        for (Class type : types) {
            if (Callback.class.isAssignableFrom(type)) {
                Class actualType = callbackNames.get(getName(msg.getInterfaceName(), msg.getMethodName()));
                callback = buildCallbackProxy(channel, callbackInsId, actualType, msg.getSerializationType());
                break;
            }
            i++;
        }
        Object[] objArr = msg.getArgs();
        objArr[i] = callback;
        msg.setArgs(objArr);
        return msg;
    }

    /*
     *1.
     */
    public static Invoker callbackInvoker(ClientTransport clientTransport, String callbackInsId, Class actualType, int codecType) {
//        ChannelWapperedInvoker callbackInvoker = new ChannelWapperedInvoker(clientTransport, callbackInsId, codecType);
//        // 设置实际的参数类型 ActualType TODO
//       callbackInvoker.setArgTypes(new String[]{ClassTypeUtils.getTypeStr(actualType)});
//        return callbackInvoker;
//
        return null;
    }

    /**
     * 服务端构建回调客户端的代理类
     * need another param for ServerCallback instanceId
     */
    public static Callback buildCallbackProxy(ClientTransport clientTransport, String ServerCallbackInsId, Class actualType, int codecType) {

        if (proxyMap.containsKey(ServerCallbackInsId)) {
            //if channel failed remember to remove the proxy instance from the proxyMap
            return proxyMap.get(ServerCallbackInsId);
        }

        Invoker invoker = callbackInvoker(clientTransport, ServerCallbackInsId, actualType, codecType);
        Callback proxy = null;
        try {
            proxy = ProxyFactory.buildProxy("jdk", Callback.class, invoker);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new BsoaRpcException(22222, "error when create the ServerCallbackProxy for ServerCallbackInsId.." + ServerCallbackInsId, e);
        }
        proxyMap.put(ServerCallbackInsId, proxy);
        return proxy;

    }

    public static void removeFromProxyMap(String ServerCallbackInsId) {
        proxyMap.remove(ServerCallbackInsId);
    }

    public static ClientTransport getTransportByKey(String transportKey) {
        return clientTransportMap.get(transportKey);

    }


    private static String getName(String interfaceId, String methodName) {

        return interfaceId + "::" + methodName;

    }

    public static String callbackRegister(String interfaceId, String methodName, Class realClass) {
        String ServerCallbackKey = getName(interfaceId, methodName);
        logger.debug("register ServerCallback method key:{}", ServerCallbackKey);
        callbackNames.put(ServerCallbackKey, realClass);
        return ServerCallbackKey;
    }


    public static boolean isCallbackRegister(String interfaceId, String methodName) {
        boolean flag = Boolean.FALSE;
        if (callbackNames.containsKey(getName(interfaceId, methodName))) flag = Boolean.TRUE;
        return flag;
    }

    public static void checkTransportFutureMap() {

        for (Map.Entry<String, ClientTransport> entrySet : clientTransportMap.entrySet()) {
            try {
                ClientTransport clientTransport = entrySet.getValue();
//                clientTransport.checkFutureMap();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public static void removeTransport(AbstractChannel channel) {
        String key = getTransportKey(channel);
        clientTransportMap.remove(key);
    }


    public static String getTransportKey(AbstractChannel channel) {
        InetSocketAddress address = channel.getRemoteAddress();
        String remoteIp = NetUtils.toIpString(address);
        int port = address.getPort();
        return getTransportKey(remoteIp, port);
    }

    public static String getTransportKey(String ip, int port) {
        return ip + "::" + port;
    }



    /**
     * 处理 stream callback的情况
     *
     * @param request 请求值
     * @return RpcRequest
     */
    public static void preMsgSend(RpcRequest request, AbstractChannel channel) {
        Class[] classes = request.getArgClasses();
        Object[] objs = request.getArgs();
        //find and replace the callback
        int port = channel.getLocalAddress().getPort();
        int i = 0;
        for (Class clazz : classes) {
            if (Callback.class.isAssignableFrom(clazz)) {
                Callback callbackIns = (Callback) objs[i];
                if (callbackIns == null) {
                    continue;
                }
                String interfaceId = request.getInterfaceName();
                String methodName = request.getMethodName();
                // 如果是callback本地实例 则置为null再发给服务端
                objs[i] = null;
                // 在Header加上callbackInsId关键字，服务端特殊处理
                String callbackInsKey = CallbackUtils.clientRegisterCallback(interfaceId, methodName, callbackIns, port);
                request.addHeadKey(HeadKey.CALLBACK_INS_KEY, callbackInsKey);
                break;
            }
            i++;
        }
        request.setArgs(objs);
    }
}
