/*
 * Copyright 2016 The BSOA Project
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
package io.bsoa.rpc.server;

import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.struct.NamedThreadFactory;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.ThreadPoolUtils;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.transport.ServerTransportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhangg on 16-6-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class BusinessPool {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(BusinessPool.class);

    /**
     * 端口和业务线程池的对应缓存
     */
    private final static ConcurrentMap<Integer, ThreadPoolExecutor> poolMap = new ConcurrentHashMap<Integer, ThreadPoolExecutor>();

    /**
     * 构建业务线程池
     *
     * @param transportConfig ServerTransportConfig
     * @return 线程池
     */
    public static ThreadPoolExecutor getBusinessPool(ServerTransportConfig transportConfig) {
        int port = transportConfig.getPort();
        ThreadPoolExecutor pool = poolMap.get(port);
        if (pool == null) {
            pool = CommonUtils.putToConcurrentMap(poolMap, port, initPool(transportConfig));
        }
        return pool;
    }

    /**
     * 得到全部线程池
     *
     * @return 全部业务线程池
     */
    public static ConcurrentMap<Integer, ThreadPoolExecutor> getBusinessPools() {
        return poolMap;
    }

    /**
     * 按端口查询业务线程池
     *
     * @param port 端口
     * @return 线程池
     */
    public static ThreadPoolExecutor getBusinessPool(int port) {
        return poolMap.get(port);
    }

    private static synchronized ThreadPoolExecutor initPool(ServerTransportConfig transportConfig) {
        int port = transportConfig.getPort();
        // 计算线程池大小
        int minPoolSize; // TODO 最小值和存活时间是否可配？
        int aliveTime;
        int maxPoolSize = transportConfig.getBizMaxThreads();
        String poolType = transportConfig.getBizPoolType();
        if (BsoaConstants.THREADPOOL_TYPE_FIXED.equals(poolType)) {
            minPoolSize = maxPoolSize;
            aliveTime = 0;
        } else if (BsoaConstants.THREADPOOL_TYPE_CACHED.equals(poolType)) {
            minPoolSize = 20;
            maxPoolSize = Math.max(minPoolSize, maxPoolSize);
            aliveTime = 60000;
        } else {
            throw new BsoaRuntimeException(21401, "server.threadPoolType");
        }

        // 初始化队列
        String queueType = transportConfig.getBizPoolQueueType();
        int queueSize = transportConfig.getBizPoolQueues();
        boolean isPriority = BsoaConstants.QUEUE_TYPE_PRIORITY.equals(queueType);
        BlockingQueue<Runnable> configQueue = ThreadPoolUtils.buildQueue(queueSize, isPriority);

        NamedThreadFactory threadFactory = new NamedThreadFactory("BZ-" + port, true);
        RejectedExecutionHandler handler = new RejectedExecutionHandler() {
            private int i = 1;

            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                if (i++ % 7 == 0) {
                    i = 1;
                    LOGGER.warn("[23002]Task:{} has been reject for ThreadPool exhausted!" +
                                    " pool:{}, active:{}, queue:{}, taskcnt: {}",
                            new Object[]{
                                    r,
                                    executor.getPoolSize(),
                                    executor.getActiveCount(),
                                    executor.getQueue().size(),
                                    executor.getTaskCount()
                            });
                }
                throw new RejectedExecutionException("[23003]Biz thread pool of provider has bean exhausted");
            }
        };
        LOGGER.debug("Build " + poolType + " business pool for port " + port
                + " [min: " + minPoolSize
                + " max:" + maxPoolSize
                + " queueType:" + queueType
                + " queueSize:" + queueSize
                + " aliveTime:" + aliveTime
                + "]");
        return new ThreadPoolExecutor(minPoolSize, maxPoolSize,
                aliveTime, TimeUnit.MILLISECONDS,
                configQueue, threadFactory, handler);
    }

}
