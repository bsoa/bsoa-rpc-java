/**
 * Limiter.java Created on 2015/4/15 11:38
 * <p/>
 * Copyright (c) 2015 by www.jd.com.
 */
package io.bsoa.rpc.filter.limiter;

/**
 * Title: 限制器接口 <br>
 * <p/>
 * Description: <br>
 * <p/>
 * Company: <a href=www.jd.com>京东</a><br>
 *
 * @author <a href=mailto:zhanggeng@jd.com>章耿</a>
 */
public interface Limiter {

    /**
     * 是否超过限制
     *
     * @param interfaceName
     *         接口
     * @param methodName
     *         方法
     * @param alias
     *         别名
     * @param appId
     *         appId
     * @return true不可以调用 false可以调用
     */
    public boolean isOverLimit(String interfaceName, String methodName, String alias, String appId);

    /**
     * 得到明细
     *
     * @return 详细描述
     */
    public String getDetails();
}
