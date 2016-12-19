/**
 *  ExcludeFilter.java Created on 2015/2/27 0027 17:34
 *
 *  Copyright (c) 2015 by www.jd.com.
 */
package io.bsoa.rpc.filter;

import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 *
 *
 * Created by zhanggeng on 16-6-7.
 *
 * @author <a href=mailto:ujjboy@qq.com>Geng Zhang</a>
 */
public class ExcludeFilter extends AbstractFilter {

    /**
     * 要排除的过滤器 -*和 -default表示不加载默认过滤器
     */
    private final String excludeFilterName;

    public ExcludeFilter(String excludeFilterName) {
        this.excludeFilterName = excludeFilterName;
    }

    public RpcResponse invoke(RpcRequest request) {
        throw new UnsupportedOperationException();
    }

    public String getExcludeFilterName() {
        return excludeFilterName;
    }
}