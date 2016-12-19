/**
 * ExtensionFilter.java Created on 2015/11/18 16:09
 * <p/>
 * Copyright (c) 2015 by www.jd.com.
 */
package io.bsoa.rpc.ext;

import io.bsoa.rpc.filter.Filter;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * Title: <br>
 *
 * Description: <br>
 *
 * Company: <a href=www.jd.com>京东</a><br>
 *
 * @author <a href=mailto:zhanggeng@jd.com>章耿</a>
 */
@Extension(value = "testext",order = 2)
@AutoActive(providerSide = true,consumerSide = false)
public class ExtensionFilter implements Filter {

    @Override
    public RpcResponse invoke(RpcRequest request) {
        return null;
    }
}
