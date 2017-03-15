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
import io.bsoa.rpc.common.utils.ClassUtils;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.transport.AbstractChannel;
import io.bsoa.rpc.transport.ClientTransport;
import io.bsoa.rpc.transport.ClientTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/2/10 22:22. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class CallbackUtils {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(CallbackUtils.class);

    /**
     * 判断全局是否使用了streamObserver功能，如果没有，则减少StreamContext的加载。
     */
    private static volatile boolean callbackFeatureUsed = false;
    /**
     * 允许同时的最大的Callback数
     */
    private static final int maxSize = BsoaConfigs.getIntValue(BsoaOptions.CALLBACK_MAX_SIZE);

    private static ConcurrentHashMap<Class, AtomicInteger> callbackCountMap = new ConcurrentHashMap<Class, AtomicInteger>();

    private static ConcurrentHashMap<Callback, Integer> instancesNumMap = new ConcurrentHashMap<Callback, Integer>();//instance number


    /**
     * 检查时候有流式方法，如果有记录下来,Callback<Q,S>里的Q,S一定要指定类型
     *
     * @param clazz 接口类
     */
    public static void scanAndRegisterCallBack(Class clazz) {
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
            if (Callback.class.isAssignableFrom(paramClazz)) {
                if (++cnt > 1) { // 只能有一个Callback参数
                    throw new BsoaRuntimeException(22222, "Illegal Callback parameter at method " + method.getName()
                            + ", just allow one Callback parameter");
                }
                // 需要解析出Callback<Q,S>里的Q,S的实际类型
                Type paramType = paramTypes[i];
                Class[] reqAndResActualClass = getActualClass(paramClazz, paramType);
                if (reqAndResActualClass == null || reqAndResActualClass.length != 2) {
                    throw new BsoaRuntimeException(22222,
                            "Must set actual type of Callback, Can not use <?>");
                }
                key = key == null ? ClassUtils.getMethodKey(interfaceId, method.getName()) : key;
                registryParamOfCallbackMethod(key, reqAndResActualClass[0]);
            }
        }
//        callbackRegister(interfaceId, method.getName(), reqRealClass);
//                HashSet set = new HashSet<Class<?>>();
//                CodecUtils.checkAndRegistryClass(reqRealClass, set);
//                CodecUtils.checkAndRegistryClass(resRealClass, set);
    }

    /**
     * 得到泛型的实际类型（必须知道实际类型）
     *
     * @param clazz 参数类名
     * @param type  参数泛化类型
     * @return 实际类
     */
    private static Class[] getActualClass(Class<? extends Callback> clazz, Type type) {
        if (type instanceof Class) {
            // 例如直接 Callback 不行
            throw new BsoaRuntimeException(22222, "[24300]Must set actual type of Callback");
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Type[] actualTypes = pt.getActualTypeArguments();
            if (actualTypes.length == 2) {
                Class[] cs = new Class[2];
                for (int i = 0; i < 2; i++) {
                    Type actualType = actualTypes[0];
                    try {
                        if (actualType instanceof ParameterizedType) {
                            // 例如 Callback<List<String>,List<String>>
                            cs[i] = (Class) ((ParameterizedType) actualType).getRawType();
                        } else if (actualType instanceof Class) {
                            // 普通的 Callback<String,String>
                            cs[1] = (Class) actualType;
                        }
                    } catch (ClassCastException e) {
                        // 抛出转换异常 表示为"?"泛化类型， java.lang.ClassCastException:
                        // sun.reflect.generics.reflectiveObjects.WildcardTypeImpl cannot be cast to java.lang.Class
                        throw new BsoaRuntimeException(22222,
                                "[24300]Must set actual type of Callback, Can not use <?>");
                    }
                }
                return cs;
            } else {
                throw new BsoaRuntimeException(22222,
                        "[24300]Must set only one actual type of Callback!");
            }
        } else {
            throw new BsoaRuntimeException(22222,
                    "[24300]Must set actual type of Callback!");
        }
    }

    /**
     * 注册Callback方法参数
     *
     * @param key         接口名#方法名
     * @param actualClass 实际类型
     */
    protected static void registryParamOfCallbackMethod(String key, Class actualClass) {
        CallbackContext.registryParamOfCallbackMethod(key, actualClass);
        callbackFeatureUsed = true;
    }

    /**
     * 是否为Callback方法
     *
     * @param key 接口名#方法名
     * @return 是否Callback方法
     */
    public static boolean hasCallbackParameter(String key) {
        return callbackFeatureUsed && CallbackContext.hasCallbackParameter(key);
    }

    /*
     *max default is 1000;
     *
     */
    public static String cacheLocalCallback(String interfaceId, String method, Callback impl, int port) {

        if (impl == null) {
            throw new RuntimeException("Callback Ins can't be null!");
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

        int currentSize = CallbackContext.getInsMapSize();
        if (currentSize > maxSize) {
            // 同时存在的SteamObserver太多，可能是没有调用onCompleted或者onError
            LOGGER.warn("There is to much Callback in local, " +
                    "currentSize is {} > {}, Please check it!", currentSize, maxSize);
        }
        CallbackContext.putCallbackIns(key, impl);
        callbackFeatureUsed = true;
        return key;

    }

    private static Integer getInstanceNumber(Object impl, String interfaceId, String method, int port) {
        Class clazz = impl.getClass();
        AtomicInteger num = callbackCountMap.get(clazz);
        if (num == null) {
            num = initNum(clazz);
        }

        if (num.intValue() >= 2000) {
            throw new RuntimeException("[24301]ServerCallback instance have exceeding 2000 for type:" + clazz + " interfaceId " + interfaceId + " method " + method + " port" + port);
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
     * 消息发送前预处理（客户端Callback发送前，保存本地实例映射）
     *
     * @param request 请求值
     */
    public static void preMsgSend(RpcRequest request, AbstractChannel channel) {
        Class[] classes = request.getArgClasses();
        int i = 0;
        for (Class clazz : classes) {
            if (Callback.class.isAssignableFrom(clazz)) {
                Callback callbackIns = (Callback) request.getArgs()[i];
                if (callbackIns == null) {
                    continue;
                } else {
                    String interfaceId = request.getInterfaceName();
                    String methodName = request.getMethodName();
                    // 生成callbackInsKey
                    String callbackInsKey = CallbackUtils.cacheLocalCallback(interfaceId,
                            methodName, callbackIns, channel.localAddress().getPort());
                    Class reqClass = CallbackContext.getParamTypeOfCallbackMethod(
                            ClassUtils.getMethodKey(interfaceId, methodName));
                    // 如果是Callback本地实例 则置为一个包装类
                    request.getArgs()[i] = new CallbackStub<>(callbackInsKey, reqClass);
                    break;
                }
            }
            i++;
        }
    }

    /**
     * 消息调用前预处理（服务端收到CallbackStub请求，例如生成本地CallbackStub代理类等）
     *
     * @param request 请求对象
     * @param channel 长连接
     */
    public static void preMsgHandle(RpcRequest request, AbstractChannel channel) {
        Class[] types = request.getArgClasses();
        for (int i = 0; i < types.length; i++) {
            Class type = types[i];
            if (Callback.class.isAssignableFrom(type)) {
                CallbackStub stub = (CallbackStub) request.getArgs()[i];
                callbackFeatureUsed = true;
                // 使用一个已有的channel虚拟化一个反向长连接
                ClientTransport clientTransport = ClientTransportFactory.getReverseClientTransport(channel);
                stub.setClientTransport(clientTransport).initByMessage(request);
                break;
            }
        }
    }
}