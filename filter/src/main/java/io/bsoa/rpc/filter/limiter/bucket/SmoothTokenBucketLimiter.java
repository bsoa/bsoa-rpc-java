/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bsoa.rpc.filter.limiter.bucket;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 *
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class SmoothTokenBucketLimiter extends AbstractTokenBucketLimiter {

    @Override
    public double getToken(double requiredToken) {
        long waitMicros;
        long sleepTime;
        long oldNextGenTokenMicros;
        long nowMicros = duration();
        synchronized (mutex) {
            syncAvailableToken(nowMicros);
            oldNextGenTokenMicros = nextGenTokenMicros;
            double tokenPermitted = Math.min(requiredToken, availableTokens);
            double needNewToken = requiredToken - tokenPermitted;
            waitMicros = (long) (needNewToken * stableIntervalTokenMicros);
            nextGenTokenMicros = nextGenTokenMicros + waitMicros;
            availableTokens -= tokenPermitted;
        }
        sleepTime = Math.max(oldNextGenTokenMicros - nowMicros, 0);
        uninterruptibleSleep(sleepTime, MICROSECONDS);
        return sleepTime;
    }


    private void uninterruptibleSleep(long sleepTime, TimeUnit unit) {
        boolean interrupted = false;
        try {
            long remainingNanos = unit.toNanos(sleepTime);
            long end = System.nanoTime() + remainingNanos;
            while (true) {
                try {
                    NANOSECONDS.sleep(remainingNanos);
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                    remainingNanos = end - System.nanoTime();
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
