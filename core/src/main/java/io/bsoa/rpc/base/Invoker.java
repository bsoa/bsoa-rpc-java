package io.bsoa.rpc.base;

import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * 调用器
 * <p>
 * Created by zhangg on 16-6-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public interface Invoker {

    /**
     * 执行调用
     *
     * @param request 请求
     * @return RpcResponse 响应
     */
    RpcResponse invoke(RpcRequest request);
}
