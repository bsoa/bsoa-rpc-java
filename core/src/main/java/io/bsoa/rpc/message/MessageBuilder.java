package io.bsoa.rpc.message;

import io.bsoa.rpc.common.utils.ClassTypeUtils;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.exception.BsoaRpcException;

/**
 * Created by zhangg on 16-6-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class MessageBuilder {

    /**
     * 构建请求，常用于代理类拦截
     *
     * @param clazz    接口类
     * @param method   方法名
     * @param argTypes 方法参数类型
     * @param args     方法参数值
     * @return 远程调用请求
     */
    public static RpcRequest buildRpcRequest(Class<?> clazz, String method, Class[] argTypes, Object[] args) {
        RpcRequest request = new RpcRequest();
        request.setInterfaceName(ClassTypeUtils.getTypeStr(clazz));
        request.setMethodName(method);
        request.setArgClasses(argTypes);
        request.setArgs(args == null ? new Object[0] : args);
        return request;
    }

    /**
     * 构建心跳包
     *
     * @return 心跳请求
     */
    public static HeartbeatRequest buildHeartbeatRequest() {
        HeartbeatRequest request = new HeartbeatRequest();
        request.setTimestamp(BsoaContext.now());
        return request;
    }

    /**
     * 根据心跳请求构建心跳结果
     *
     * @param request 心跳请求
     * @return 心跳结果
     */
    public static HeartbeatResponse buildHeartbeatResponse(HeartbeatRequest request) {
        HeartbeatResponse response = new HeartbeatResponse();
        response.setMessageId(request.getMessageId());
        response.setTimestamp(BsoaContext.now());
        return response;
    }

    /**
     * 根据协商请求构建协商结果
     *
     * @param request 协商请求
     * @return 协商结果
     */
    public static NegotiationResponse buildNegotiationResponse(NegotiationRequest request) {
        NegotiationResponse response = new NegotiationResponse();
        response.setMessageId(request.getMessageId());
        return response;
    }

    /**
     * 根据rpc请求构建rpc结果
     *
     * @param request rpc请求
     * @return rpc结果
     */
    public static RpcResponse buildRpcResponse(RpcRequest request) {
        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setMessageId(request.getMessageId());
        rpcResponse.setProtocolType(request.getProtocolType());
        rpcResponse.setCompressType(request.getCompressType());
        rpcResponse.setSerializationType(request.getSerializationType());

        return rpcResponse;
    }

    public static BaseMessage buildMessage(byte messageType, int messageId) {
        BaseMessage baseMessage;
        switch (messageType) {
            case 1:
                baseMessage = new RpcRequest();
                break;
            case 2:
                baseMessage = new RpcResponse();
                break;
            case 3:
                baseMessage = new HeartbeatRequest();
                break;
            case 4:
                baseMessage = new HeartbeatResponse();
                break;
            case 5:
                baseMessage = new NegotiationRequest();
                break;
            case 6:
                baseMessage = new NegotiationResponse();
                break;
            default:
                throw new BsoaRpcException(22222, "Value of attrs in message header must be byte/short/int/string");
        }
        baseMessage.setMessageId(messageId);
        return baseMessage;
    }


}
