/**
 *  ConsumerInvokeFilter.java Created on 2013年10月29日 上午10:30:50
 *
 *  Copyright (c) 2013 by www.jd.com.
 */
package io.bsoa.rpc.filter;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;


/**
 * Title: 调用过滤器<br>
 * <p/>
 * Description: 执行真正的调用过程，使用client发送数据给server<br>
 * <p/>
 *
 * Created by zhanggeng on 16-6-7.
 *
 * @author <a href=mailto:ujjboy@qq.com>Geng Zhang</a>
 */
public class ProviderInvokeFilter<T> implements Filter {

    /**
     * slf4j logger for this class
     */
    private final static Logger LOGGER = LoggerFactory
            .getLogger(ProviderInvokeFilter.class);

    /**
     * The Provider config.
     */
    private final ProviderConfig<T> providerConfig;

    /**
     * The Ref.
     */
    private final T ref;

    /**
     * Instantiates a new Provider invoke filter.
     *
     * @param providerConfig
     *         the provider config
     */
    public ProviderInvokeFilter(ProviderConfig<T> providerConfig) {
        this.providerConfig = providerConfig;
        this.ref = providerConfig.getRef();
    }

    /**
     * Invoke response message.
     *
     * @param request
     *         the request
     * @return the response message
     */
    @Override
    public RpcResponse invoke(RpcRequest request) {
        return  null;
        //TODO

//        Invocation invocation = request.getInvocationBody();
//
//        // 将接口的<jsf:param />的配置复制到RpcContext
//        RpcContext context = RpcContext.getContext();
//        Map params = providerConfig.getParameters();
//        if (params != null) {
//            context.setAttachments(params);
//        }
//        // 将方法的<jsf:param />的配置复制到invocation
//        String methodName = invocation.getMethodName();
//        params = (Map) providerConfig.getMethodConfigValue(methodName, Constants.CONFIG_KEY_PARAMS);
//        if (params != null) {
//            context.setAttachments(params);
//        }
//
//        ResponseMessage responseMessage = MessageBuilder.buildResponse(request);
//
//        // 是否启动压缩
//        if (providerConfig.getCompress() != null) {
//            byte b = CompressType.valueOf(providerConfig.getCompress()).value();
//            responseMessage.getMsgHeader().setCompressType(b);
//        }
//
//        try {
//            // 反射 真正调用业务代码
//            Object result = reflectInvoke(ref, invocation);
//            responseMessage.setResponse(result);
//        } catch (IllegalArgumentException e){ // 非法参数，可能是实现类和接口类不对应
//            responseMessage.setException(e);
//        } catch (InvocationTargetException e) { // 业务代码抛出异常
//            responseMessage.setException(e.getCause());
//        } catch (ClassNotFoundException e) {
//            responseMessage.setException(e);
//        } catch (NoSuchMethodException e) {
//            responseMessage.setException(e);
//        } catch (IllegalAccessException e) {
//            responseMessage.setException(e);
//        }
//
//        return responseMessage;
    }


    /**
     * Reflect invoke.
     *
     * @param impl
     *         具体接口实现类
     * @param invocation
     *         调用方法+参数类型+参数值
     * @return the object 调用结果，void返回null
     * @throws NoSuchMethodException
     *         如果找不到匹配的方法
     * @throws ClassNotFoundException
     *         如果指定的类加载器无法定位该类
     * @throws InvocationTargetException
     *         如果底层方法抛出异常
     * @throws IllegalAccessException
     *         如果此 Method 对象强制执行 Java 语言访问控制，并且底层方法是不可访问的

    private Object reflectInvoke(Object impl, Invocation invocation) throws NoSuchMethodException,
            ClassNotFoundException, InvocationTargetException, IllegalAccessException {

        Method method = ReflectUtils.getMethod(invocation.getClassName(),
                invocation.getMethodName(), invocation.getArgsType());
        return method.invoke(impl, invocation.getArgs());
    }*/

}
