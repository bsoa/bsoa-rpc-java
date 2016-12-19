package io.bsoa.rpc;

import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 *
 *
 * Created by zhanggeng on 16-6-7.
 *
 * @author <a href=mailto:ujjboy@qq.com>Geng Zhang</a>
 */
public interface Invoker {
    RpcResponse invoke(RpcRequest requestMessage);
}
