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
package io.bsoa.rpc.invoke;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.BsoaConfigs;
import io.bsoa.rpc.common.BsoaOptions;
import io.bsoa.rpc.common.struct.NamedThreadFactory;
import io.bsoa.rpc.common.utils.ThreadPoolUtils;
import io.bsoa.rpc.context.CallbackContext;

/**
 * <p></p>
 *
 * Created by zhangg on 2017/2/15 20:27. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class AsyncContext {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(AsyncContext.class);
    

    /**
     * callback业务线程池（callback+async）
     */
    private static volatile ThreadPoolExecutor asyncThreadPool;

    /**
     * 得到callback用的线程池 默认开始创建
     *
     * @return callback用的线程池
     */
    public static ThreadPoolExecutor getAsyncThreadPool() {
        return getAsyncThreadPool(true);
    }

    /**
     * 得到callback用的线程池
     *
     * @param build 没有时是否构建
     * @return callback用的线程池
     */
    public static ThreadPoolExecutor getAsyncThreadPool(boolean build) {
        if (asyncThreadPool == null && build) {
            synchronized (CallbackContext.class) {
                if (asyncThreadPool == null && build) {
                    // 一些系统参数，可以从配置或者注册中心获取。
                    int coresize = BsoaConfigs.getIntValue(BsoaOptions.CALLBACK_POOL_CORE);
                    int maxsize = BsoaConfigs.getIntValue(BsoaOptions.CALLBACK_POOL_MAX);
                    int queuesize = BsoaConfigs.getIntValue(BsoaOptions.CALLBACK_POOL_QUEUE);

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
                    asyncThreadPool = ThreadPoolUtils.newCachedThreadPool(
                            coresize, maxsize, queue, threadFactory, handler);
                }
            }
        }
        return asyncThreadPool;
    }
}
