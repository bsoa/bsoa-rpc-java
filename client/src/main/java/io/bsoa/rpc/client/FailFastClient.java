/**
 * FailFastClient.java Created on 2014年4月9日 上午9:12:07
 * <p>
 * Copyright (c) 2014 by www.jd.com.
 */
package io.bsoa.rpc.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.transport.ClientTransport;

/**
 * Title: 快速失败<br>
 * <p>
 * Description: 不重试<br>
 * <p>
 * Company: <a href=www.jd.com>京东</a><br>
 *
 * @author <a href=mailto:zhanggeng@jd.com>章耿</a>
 */
@Extension("failfast")
public class FailFastClient extends AbstractClient {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(FailFastClient.class);

    @Override
    public RpcResponse doSendMsg(RpcRequest request) {
        ClientTransport connection = super.select(request);
        try {
            RpcResponse result = super.sendMsg0(connection, request);
            if (result != null) {
                return result;
            } else {
                throw new BsoaRpcException(22222, "Failed to call " + request.getInterfaceName() + "." + request.getMethodName()
                        + " on remote server " + connection.getConfig().getProvider() + ", return null");
            }
        } catch (Exception e) {
            throw new BsoaRpcException(22222, "[JSF-22103]Failed to call " + request.getInterfaceName() + "." + request.getMethodName()
                    + " on remote server: " + connection.getConfig().getProvider() + ", cause by: "
                    + e.getClass().getName() + ", message is: " + e.getMessage(), e);
        }
    }
}
