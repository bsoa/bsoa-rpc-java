package io.bsoa.rpc.message;

/**
 *
 *
 * Created by zhanggeng on 16-6-7.
 *
 * @author <a href=mailto:ujjboy@qq.com>Geng Zhang</a>
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
}
