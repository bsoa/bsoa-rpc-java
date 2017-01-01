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
package io.bsoa.rpc.context;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.struct.NamedThreadFactory;
import io.bsoa.rpc.common.utils.ThreadPoolUtils;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/1/1 16:37. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class CallbackContext {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(CallbackContext.class);

    /**
     * callback业务线程池（callback+async）
     */
    private static volatile ThreadPoolExecutor callbackThreadPool;

    /**
     * 得到callback用的线程池 默认开始创建
     *
     * @return callback用的线程池
     */
    public static ThreadPoolExecutor getCallbackThreadPool() {
        return getCallbackThreadPool(true);
    }

    /**
     * 得到callback用的线程池
     *
     * @param build 没有时是否构建
     * @return callback用的线程池
     */
    public static ThreadPoolExecutor getCallbackThreadPool(boolean build) {
        if (callbackThreadPool == null && build) {
            synchronized (CallbackContext.class) {
                if (callbackThreadPool == null && build) {
                    // 一些系统参数，可以从配置或者注册中心获取。
                    int coresize = BsoaConfigs.getIntValue(BsoaConfigs.CALLBACK_POOL_CORE);
                    int maxsize = BsoaConfigs.getIntValue(BsoaConfigs.CALLBACK_POOL_MAX);
                    int queuesize = BsoaConfigs.getIntValue(BsoaConfigs.CALLBACK_POOL_QUEUE);

                    BlockingQueue<Runnable> queue = ThreadPoolUtils.buildQueue(queuesize);
                    NamedThreadFactory threadFactory = new NamedThreadFactory("BSOA-CB", true);

                    RejectedExecutionHandler handler = new RejectedExecutionHandler() {
                        private int i = 1;

                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                            if (i++ % 7 == 0) {
                                i = 1;
                                LOGGER.warn("Task:{} has been reject for ThreadPool exhausted!" +
                                                " pool:{}, active:{}, queue:{}, taskcnt: {}",
                                        r,
                                        executor.getPoolSize(),
                                        executor.getActiveCount(),
                                        executor.getQueue().size(),
                                        executor.getTaskCount());
                            }
                            throw new RejectedExecutionException(
                                    "Callback handler thread pool has bean exhausted");
                        }
                    };
                    callbackThreadPool = ThreadPoolUtils.newCachedThreadPool(
                            coresize, maxsize, queue, threadFactory, handler);
                }
            }
        }
        return callbackThreadPool;
    }
}
