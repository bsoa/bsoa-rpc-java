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

import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.bsoa.rpc.client.Provider;
import io.bsoa.rpc.config.AbstractInterfaceConfig;

/**
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class RpcStatus {

    /**
     * 接口级统计
     */
    private static final ConcurrentMap<AbstractInterfaceConfig, RpcStatus> INTERFACE_STATISTICS
            = new ConcurrentHashMap<>();

    /**
     * 方法级统计
     */
    private static final ConcurrentMap<AbstractInterfaceConfig, ConcurrentMap<String, RpcStatus>>
            METHOD_STATISTICS = new ConcurrentHashMap<>();

    /**
     * 方法+provider级统计
     */
    private static final ConcurrentMap<String, RpcStatus> METHOD_PROVIDER_STATISTICS = new ConcurrentHashMap<>();

    /**
     * 接口级的状态统计数据，服务端和客户端计算并发的时候用
     *
     * @param config 接口配置
     * @return RpcStatus对象
     */
    public static RpcStatus getInterfaceStatus(AbstractInterfaceConfig config) {
        RpcStatus status = INTERFACE_STATISTICS.get(config);
        if (status == null) {
            INTERFACE_STATISTICS.putIfAbsent(config, new RpcStatus());
            status = INTERFACE_STATISTICS.get(config);
        }
        return status;
    }

    /**
     * 得到接口级的统计数据
     *
     * @return
     */
    public static Map<AbstractInterfaceConfig, RpcStatus> getInterfaceStatuses() {
        return Collections.unmodifiableMap(INTERFACE_STATISTICS);
    }

    /**
     * 接口+方法级的状态统计数据，服务端和客户端计算并发的时候用
     *
     * @param config     接口配置
     * @param methodName 方法
     * @return RpcStatus对象
     */
    public static RpcStatus getMethodStatus(AbstractInterfaceConfig config, String methodName) {
        // 一个接口多引用怎么计算
        ConcurrentMap<String, RpcStatus> map = METHOD_STATISTICS.get(config);
        if (map == null) {
            METHOD_STATISTICS.putIfAbsent(config, new ConcurrentHashMap<String, RpcStatus>());
            map = METHOD_STATISTICS.get(config);
        }
        RpcStatus status = map.get(methodName);
        if (status == null) {
            map.putIfAbsent(methodName, new RpcStatus());
            status = map.get(methodName);
        }
        return status;
    }

    /**
     * 得到接口下方法级的统计数据
     *
     * @return
     */
    public static Map<AbstractInterfaceConfig, ConcurrentMap<String, RpcStatus>> getMethodStatuses() {
        return Collections.unmodifiableMap(METHOD_STATISTICS);
    }

    /**
     * 删除接口级+方法级的状态统计
     *
     * @param config 接口配置
     */
    public static void removeStatus(AbstractInterfaceConfig config) {
        INTERFACE_STATISTICS.remove(config);
        METHOD_STATISTICS.remove(config);
    }

    /**
     * 接口+方法+provider级的状态统计数据，leastactive用
     *
     * @param interfaceId 接口
     * @param methodName  方法
     * @param provider    连接
     * @return RpcStatus对象
     */
    public static RpcStatus getStatus(String interfaceId, String methodName, Provider provider) {
        String key = getIdentityString(interfaceId, methodName, provider);
        RpcStatus status = METHOD_PROVIDER_STATISTICS.get(key);
        if (status == null) {
            METHOD_PROVIDER_STATISTICS.putIfAbsent(key, new RpcStatus());
            status = METHOD_PROVIDER_STATISTICS.get(key);
        }
        return status;
    }

    /**
     * Remove status.
     *
     * @param interfaceId 接口
     * @param methodName  方法
     * @param provider    连接
     */
    public static void removeStatus(String interfaceId, String methodName, Provider provider) {
        String key = getIdentityString(interfaceId, methodName, provider);
        METHOD_PROVIDER_STATISTICS.remove(key);
    }

    /**
     * 唯一的关键字
     *
     * @param interfaceId 接口
     * @param methodName  方法
     * @param provider    连接
     * @return 关键字
     */
    private static String getIdentityString(String interfaceId, String methodName, Provider provider) {
        // 完成唯一字符串的拼接
        return interfaceId + ":" + methodName + ":" + provider.getIp() + ":" + provider.getPort();
    }

    /**
     * 开始统计
     *
     * @param config     配置
     * @param methodName 方法
     */
    public static void beginCount(AbstractInterfaceConfig config, String methodName) {
        beginCount(getInterfaceStatus(config));
        beginCount(getMethodStatus(config, methodName));
    }


    /**
     * 开始统计
     *
     * @param interfaceId 接口
     * @param methodName  方法
     * @param provider    连接
     * @return 关键字
     */
    public static void beginCount(String interfaceId, String methodName, Provider provider) {
        beginCount(getStatus(interfaceId, methodName, provider));
    }

    /**
     * Begin count.
     *
     * @param status the status
     */
    private static void beginCount(RpcStatus status) {
        status.active.incrementAndGet();
    }

    /**
     * End count.
     *
     * @param config     接口配置
     * @param methodName 方法
     * @param elapsed    the elapsed
     * @param succeeded  the succeeded
     */
    public static void endCount(AbstractInterfaceConfig config, String methodName, long elapsed, boolean succeeded) {
        endCount(getInterfaceStatus(config), elapsed, succeeded);
        endCount(getMethodStatus(config, methodName), elapsed, succeeded);
    }

    /**
     * End count.
     *
     * @param interfaceId 接口
     * @param methodName  方法
     * @param provider    the provider
     * @param elapsed     the elapsed
     * @param succeeded   the succeeded
     */
    public static void endCount(String interfaceId, String methodName, Provider provider, long elapsed, boolean succeeded) {
        endCount(getStatus(interfaceId, methodName, provider), elapsed, succeeded);
    }

    /**
     * End count.
     *
     * @param status    the status
     * @param elapsed   the elapsed
     * @param succeeded the succeeded
     */
    private static void endCount(RpcStatus status, long elapsed, boolean succeeded) {
        status.active.decrementAndGet();
        status.total.incrementAndGet();
        status.totalElapsed.addAndGet(elapsed);
        if (status.maxElapsed.get() < elapsed) {
            status.maxElapsed.set(elapsed);
        }
        if (succeeded) {
            if (status.succeededMaxElapsed.get() < elapsed) {
                status.succeededMaxElapsed.set(elapsed);
            }
            status.failedOfLast100.add(LastResultCounter.success);
        } else {
            status.failed.incrementAndGet();
            status.failedElapsed.addAndGet(elapsed);
            if (status.failedMaxElapsed.get() < elapsed) {
                status.failedMaxElapsed.set(elapsed);
            }
            status.failedOfLast100.add(LastResultCounter.failure);
        }
    }

    /**
     * The Attrs.
     */
    private final ConcurrentMap<String, Object> attrs = new ConcurrentHashMap<String, Object>();

    /**
     * The Active.
     */
    private final AtomicInteger active = new AtomicInteger();

    /**
     * The Total.
     */
    private final AtomicLong total = new AtomicLong();

    /**
     * The Failed.
     */
    private final AtomicInteger failed = new AtomicInteger();

    /**
     * The Failed of last 100.
     */
    private final transient LastResultCounter failedOfLast100 = new LastResultCounter(100);

    /**
     * The Total elapsed.
     */
    private final AtomicLong totalElapsed = new AtomicLong();

    /**
     * The Failed elapsed.
     */
    private final AtomicLong failedElapsed = new AtomicLong();

    /**
     * The Max elapsed.
     */
    private final AtomicLong maxElapsed = new AtomicLong();

    /**
     * The Failed max elapsed.
     */
    private final AtomicLong failedMaxElapsed = new AtomicLong();

    /**
     * The Succeeded max elapsed.
     */
    private final AtomicLong succeededMaxElapsed = new AtomicLong();

    /**
     * Instantiates a new Rpc status.
     */
    private RpcStatus() {
    }

    /**
     * set value.
     *
     * @param key   the key
     * @param value the value
     */
    public void set(String key, Object value) {
        attrs.put(key, value);
    }

    /**
     * get value.
     *
     * @param key the key
     * @return value object
     */
    public Object get(String key) {
        return attrs.get(key);
    }

    /**
     * get active.
     *
     * @return active active
     */
    public int getActive() {
        return active.get();
    }

    /**
     * The Random.
     */
    private final Random random = new Random();

    /**
     * 结合最近100次的调用情况的并发数<br>
     * 如果有失败，则有概率返回超大并发数，让这个节点不被选中
     *
     * @return random active
     */
    public int randomActive() {
        int failPrecent = failedOfLast100.get();
        if (failPrecent == 0) { // 最近无失败
            return active.get();
        }
        if (failPrecent > 90) { // 最近失败太多，预留10%概率供恢复
            failPrecent = 90;
        }
        if (random.nextInt(100) < failPrecent) { //随机
            return Integer.MAX_VALUE; // 返回超大并发数，就不会被选中
        } else {
            return active.get(); //返回实际并发数
        }
    }

    /**
     * get total.
     *
     * @return total total
     */
    public long getTotal() {
        return total.longValue();
    }

    /**
     * get total elapsed.
     *
     * @return total elapsed
     */
    public long getTotalElapsed() {
        return totalElapsed.get();
    }

    /**
     * get average elapsed.
     *
     * @return average elapsed
     */
    public long getAverageElapsed() {
        long total = getTotal();
        if (total == 0) {
            return 0;
        }
        return getTotalElapsed() / total;
    }

    /**
     * get max elapsed.
     *
     * @return max elapsed
     */
    public long getMaxElapsed() {
        return maxElapsed.get();
    }

    /**
     * get failed.
     *
     * @return failed failed
     */
    public int getFailed() {
        return failed.get();
    }

    /**
     * get failed elapsed.
     *
     * @return failed elapsed
     */
    public long getFailedElapsed() {
        return failedElapsed.get();
    }

    /**
     * get failed average elapsed.
     *
     * @return failed average elapsed
     */
    public long getFailedAverageElapsed() {
        long failed = getFailed();
        if (failed == 0) {
            return 0;
        }
        return getFailedElapsed() / failed;
    }

    /**
     * get failed max elapsed.
     *
     * @return failed max elapsed
     */
    public long getFailedMaxElapsed() {
        return failedMaxElapsed.get();
    }

    /**
     * get succeeded.
     *
     * @return succeeded succeeded
     */
    public long getSucceeded() {
        return getTotal() - getFailed();
    }

    /**
     * get succeeded elapsed.
     *
     * @return succeeded elapsed
     */
    public long getSucceededElapsed() {
        return getTotalElapsed() - getFailedElapsed();
    }

    /**
     * get succeeded average elapsed.
     *
     * @return succeeded average elapsed
     */
    public long getSucceededAverageElapsed() {
        long succeeded = getSucceeded();
        if (succeeded == 0) {
            return 0;
        }
        return getSucceededElapsed() / succeeded;
    }

    /**
     * get succeeded max elapsed.
     *
     * @return succeeded max elapsed.
     */
    public long getSucceededMaxElapsed() {
        return succeededMaxElapsed.get();
    }

    /**
     * Calculate average TPS (Transaction per second).
     *
     * @return tps average tps
     */
    public long getAverageTps() {
        if (getTotalElapsed() >= 1000L) {
            return getTotal() / (getTotalElapsed() / 1000L);
        }
        return getTotal();
    }

    /**
     * Gets failed of last 100.
     *
     * @return the failed of last 100
     */
    public LastResultCounter getFailedOfLast100() {
        return failedOfLast100;
    }


    static class LastResultCounter {
        /**
         * 成功记0
         */
        private static int success = 0;
        /**
         * 失败记1
         */
        private static int failure = 1;
        /**
         * The Total.
         */
        private int total = 0; // 总计数
        /**
         * The Capacity.
         */
        private final int capacity; // 容量
        /**
         * The Last.
         */
        private final int[] last; // 全部值
        /**
         * The Index.
         */
        private int index = 0; // 当前索引

        /**
         * Instantiates a new Last result counter.
         *
         * @param capacity the capacity
         */
        public LastResultCounter(int capacity) {
            this.capacity = capacity;
            this.last = new int[capacity];
        }

        /**
         * Add void.
         *
         * @param val the val
         */
        public final synchronized void add(int val) {
            int old = last[index];
            last[index] = val;

            total -= old;
            total += val;

            if (index == capacity - 1) {
                index = 0;
            } else {
                index++;
            }
        }

        /**
         * Get int.
         *
         * @return the int
         */
        public final int get() {
            return total;
        }
    }
}

