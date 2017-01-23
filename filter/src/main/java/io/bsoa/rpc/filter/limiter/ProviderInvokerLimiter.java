package io.bsoa.rpc.filter.limiter;

import com.jd.jsf.gd.filter.limiter.bucket.LimitedException;
import com.jd.jsf.gd.filter.limiter.bucket.RateLimiter;

/**
 * Title: <br>
 * <p>
 * Description: <br>
 *     服务端限流
 * </p>
 *
 * @author <a href=mailto:taobaorun@gmail.com>wutao</a>
 *
 * @since 2016/04/27 15:59
 */
public class ProviderInvokerLimiter implements Limiter {

    private RateLimiter rateLimiter;

    /**
     * 限制次数/s
     */
    private int limit;

    public ProviderInvokerLimiter(int limit) {
        this.limit = limit;
        rateLimiter = RateLimiter.builder()
                .withTokePerSecond(this.limit)
                .withType(RateLimiter.RateLimiterType.FFTB)
                .build();
    }

    public void updateLimit(int limit){
        this.limit = limit;
        rateLimiter.setRate(limit);
    }

    @Override
    public boolean isOverLimit(String interfaceName, String methodName, String alias, String appId) {
        try {
            rateLimiter.getToken(1);
        } catch (LimitedException e){
            return true;
        }
        return false;
    }

    @Override
    public String getDetails() {
        return "ProviderLimit:"+limit;
    }

    public int getLimit() {
        return limit;
    }

}
