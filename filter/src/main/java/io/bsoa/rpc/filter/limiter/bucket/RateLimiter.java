/*
 * Copyright © 2016-2017 The BSOA Project
 *
 * The BSOA Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.bsoa.rpc.filter.limiter.bucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public abstract class RateLimiter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimiter.class);

    protected double maxTokens;

    protected double availableTokens;

    protected volatile long startNanos;

    protected long nextGenTokenMicros;

    protected double stableIntervalTokenMicros;

    protected final Object mutex = new Object();

    public void setRate(double tokenPerSecond) {
        if (tokenPerSecond < 0) {
            throw new IllegalArgumentException("tokenPerSecond must be positive.");
        }
        synchronized (mutex) {
            doSetRate(tokenPerSecond);
        }
    }

    protected abstract void doSetRate(double tokenPerSecond);

    public abstract void syncAvailableToken(long nowMicros);

    public abstract double getToken(double requiredToken);

    public long duration() {
        return MICROSECONDS.convert(System.nanoTime() - startNanos, NANOSECONDS);
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder<T extends RateLimiter> {

        private double tokenPerSecond;

        private RateLimiterType type;

        public Builder withType(RateLimiterType type) {
            this.type = type;
            return this;
        }

        public Builder withTokePerSecond(double tokenPerSecond) {
            this.tokenPerSecond = tokenPerSecond;
            return this;
        }

        public T build() {
            switch (type) {
                case TB:
                    return (T) buildSmoothTokenBucketLimiter();
                case LB:
                    return null;
                case FFTB:
                    return (T) buildFailFastTokenBucketLimiter();
                default:
                    return (T) buildSmoothTokenBucketLimiter();
            }
        }


        private SmoothTokenBucketLimiter buildSmoothTokenBucketLimiter() {
            SmoothTokenBucketLimiter limiter = new SmoothTokenBucketLimiter();
            limiter.startNanos = System.nanoTime();
            limiter.setRate(tokenPerSecond);
            return limiter;
        }

        private FailFastTokenBucketLimiter buildFailFastTokenBucketLimiter() {
            FailFastTokenBucketLimiter limiter = new FailFastTokenBucketLimiter();
            limiter.startNanos = System.nanoTime();
            limiter.setRate(tokenPerSecond);
            return limiter;
        }
    }

    public static enum RateLimiterType {

        /**
         * token bucket
         */
        TB,


        /**
         * leaky bucket
         */
        LB,


        /**
         * 没有可用token 抛出异常的token bucket
         */
        FFTB;
    }


}
