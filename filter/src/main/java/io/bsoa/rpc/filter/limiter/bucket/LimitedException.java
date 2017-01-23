package io.bsoa.rpc.filter.limiter.bucket;

import com.jd.jsf.gd.error.RpcException;

/**
 * Title: <br>
 * <p>
 * Description: <br>
 *     没有可用token,抛出此异常
 * </p>
 * <br>
 *
 * @author <a href=mailto:wutao@jd.com>wutao</a>
 *         <br>
 * @since 2016/04/26 22:40
 */
public class LimitedException extends RpcException {

    public LimitedException() {
    }

    public LimitedException(String message) {
        super(message);
    }

    public LimitedException(String message, Throwable cause) {
        super(message, cause);
    }

    public LimitedException(Throwable cause) {
        super(cause);
    }

}
