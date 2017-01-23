/**
 * InvokeLimiter.java Created on 2015/3/9 0009 13:55
 * <p/>
 * Copyright (c) 2015 by www.jd.com.
 */
package io.bsoa.rpc.filter.limiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Title: 基于监控服务的限制过滤器<br>
 * <p/>
 * Description: <br>
 * <p/>
 * Company: <a href=www.jd.com>京东</a><br>
 *
 * @author <a href=mailto:zhanggeng@jd.com>章耿</a>
 */
public class MonitorInvokeLimiter implements Limiter {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(MonitorInvokeLimiter.class);

    /**
     * 是否可调用，默认是
     */
    private boolean canInvoke = true;

    /**
     * 关键字
     */
    private String key;

    /**
     *
     * @param key
     */
    public MonitorInvokeLimiter(String key) {
        this.key = key;
    }

    @Override
    public boolean isOverLimit(String interfaceId, String methodName, String alias, String appId) {
        return !canInvoke;
    }

    @Override
    public String getDetails() {
        return "MonitorInvokeLimiter:" + canInvoke;
    }

    /**
     * 设置是否可以调用
     *
     * @param canInvoke
     */
    public void setCanInvoke(boolean canInvoke) {
        if (this.canInvoke != canInvoke) {
            LOGGER.info("Monitor limiter of " + key + ": canInvoke changed from {} to {}",
                    this.canInvoke, canInvoke);
            this.canInvoke = canInvoke;
        }
    }
}