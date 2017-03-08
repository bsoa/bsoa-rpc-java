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

import io.bsoa.rpc.exception.BsoaRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class FailFastTokenBucketLimiter extends AbstractTokenBucketLimiter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FailFastTokenBucketLimiter.class);

    @Override
    public double getToken(double requiredToken) {
        long nowMicros = duration();
        synchronized (mutex) {
            syncAvailableToken(nowMicros);
            double tokenPermitted = Math.min(requiredToken, availableTokens);
            double needNewToken = requiredToken - tokenPermitted;
            if (needNewToken > 0) {
                throw new BsoaRpcException(22222, "[22211]Invoked exceed the provider limit[" + this.maxTokens + "]");
            }
            availableTokens -= tokenPermitted;
        }
        return 0;
    }
}
