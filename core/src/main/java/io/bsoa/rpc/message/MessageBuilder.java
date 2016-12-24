package io.bsoa.rpc.message;

import io.bsoa.rpc.exception.BsoaRpcException;

/**
 *
 *
 * Created by zhanggeng on 16-6-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class MessageBuilder {
    public static RpcRequest buildRequest(Class<?> declaringClass, String methodName, Class[] paramTypes, Object[] paramValues) {
        // TODO
        return null;
    }

    public static HeartbeatResponse buildHeartbeatResponse(HeartbeatRequest request) {
        HeartbeatResponse response = new HeartbeatResponse();
        response.setMessageId(request.getMessageId());
        return response;
    }

    public static RpcResponse buildRpcResponse(RpcRequest rpcRequest) {
        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setMessageId(rpcRequest.getMessageId());
        rpcResponse.setProtocolType(rpcRequest.getProtocolType());
        rpcResponse.setCompressType(rpcRequest.getCompressType());
        rpcResponse.setSerializationType(rpcRequest.getSerializationType());

        return rpcResponse;
    }

    public static BaseMessage buildMessage(byte messageType, int messageId) {
        BaseMessage baseMessage;
        switch (messageType) {
            case 1 :
                baseMessage = new RpcRequest();
                break;
            case 2 :
                baseMessage = new RpcResponse();
                break;
            case 3 :
                baseMessage = new HeartbeatRequest();
                break;
            case 4 :
                baseMessage = new HeartbeatResponse();
                break;
            case 5 :
                baseMessage = new NegotiatorRequest();
                break;
            case 6 :
                baseMessage = new NegotiatorResponse();
                break;
            case 7 :
                baseMessage = new StreamRequest();
                break;
            case 8 :
                baseMessage = new StreamResponse();
                break;
            default:
                throw new BsoaRpcException(22222, "Value of attrs in message header must be byte/short/int/string");
        }
        baseMessage.setMessageType(messageType);
        baseMessage.setMessageId(messageId);
        return baseMessage;
    }
}
