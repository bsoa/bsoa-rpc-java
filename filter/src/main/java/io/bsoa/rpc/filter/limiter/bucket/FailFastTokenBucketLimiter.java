package io.bsoa.rpc.filter.limiter.bucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Title: <br>
 * <p>
 * Description: <br>
 * </p>
 * <br>
 *
 * @author <a href=mailto:wutao@jd.com>wutao</a>
 *         <br>
 * @since 2016/04/26 21:31
 */
public class FailFastTokenBucketLimiter extends AbstractTokenBucketLimiter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FailFastTokenBucketLimiter.class);

    @Override
    public double getToken(double requiredToken) {
        long nowMicros = duration();
        synchronized (mutex){
            syncAvailableToken(nowMicros);
            double tokenPermitted = Math.min(requiredToken, availableTokens);
            double needNewToken = requiredToken - tokenPermitted;
            if (needNewToken > 0){
                LOGGER.trace("no token.needNewToken:{},tokenPermitted:{}",needNewToken,tokenPermitted);
                throw new LimitedException(String.format("[JSF-22211]Invoked exceed the provider limit[%s]",this.maxTokens));
            }
            availableTokens -= tokenPermitted;
        }
        return 0;
    }
}
