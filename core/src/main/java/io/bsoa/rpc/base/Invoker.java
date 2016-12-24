package io.bsoa.rpc.base;

import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 *
 *
 * Created by zhanggeng on 16-6-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public interface Invoker {
    RpcResponse invoke(RpcRequest requestMessage);
}
