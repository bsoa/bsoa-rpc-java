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
package io.bsoa.rpc.common.struct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhangg on 2016/7/16 00:17.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ScheduledService {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ScheduledService.class);

    /**
     * 固定频率执行，按执行开始时间计算间隔
     */
    public static final int MODE_FIXEDRATE = 0;
    /**
     * 固定间隔执行，执行完成后才计算间隔
     */
    public static final int MODE_FIXEDDELAY = 1;

    /**
     * The Scheduled executor service.
     */
    private volatile ScheduledExecutorService scheduledExecutorService;

    /**
     * The Thread name
     */
    private String threadName;

    /**
     * The Runnable.
     */
    private final Runnable runnable;

    /**
     * The Initial delay.
     */
    private final long initialDelay;

    /**
     * The Period.
     */
    private final long period;

    /**
     * The Unit.
     */
    private final TimeUnit unit;

    /**
     * 0:scheduleAtFixedRate
     * 1:scheduleWithFixedDelay
     */
    private final int mode;

    /**
     * The Future.
     */
    private volatile ScheduledFuture future;

    /**
     * The Started.
     */
    private volatile boolean started;

    /**
     * Instantiates a new Scheduled service.
     *
     * @param threadName   the thread name
     * @param mode         the mode
     * @param runnable     the runnable
     * @param initialDelay the initial delay
     * @param period       the period
     * @param unit         the unit
     */
    public ScheduledService(String threadName,
                            int mode,
                            Runnable runnable,
                            long initialDelay,
                            long period,
                            TimeUnit unit) {
        this.threadName = threadName;
        this.runnable = runnable;
        this.initialDelay = initialDelay;
        this.period = period;
        this.unit = unit;
        this.mode = mode;
    }

    /**
     * 开始执行定时任务
     *
     * @return the boolean
     * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, java.util.concurrent.TimeUnit)
     * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, java.util.concurrent.TimeUnit)
     */
    public synchronized ScheduledService start() {
        if (started) {
            return this;
        }
        if (scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
                    new NamedThreadFactory(threadName, true));
        }
        ScheduledFuture future = null;
        switch (mode) {
            case MODE_FIXEDRATE:
                future = scheduledExecutorService.scheduleAtFixedRate(runnable, initialDelay,
                        period,
                        unit);
                break;
            case MODE_FIXEDDELAY:
                future = scheduledExecutorService.scheduleWithFixedDelay(runnable, initialDelay, period,
                        unit);
                break;
            default:
                break;
        }
        if (future != null) {
            this.future = future;
            // 缓存一下
            scheduledServiceMap.put(this, System.currentTimeMillis());
            started = true;
        } else {
            started = false;
        }
        return this;
    }

    /**
     * 停止执行定时任务，还可以重新start
     */
    public synchronized void stop() {
        if (!started) {
            return;
        }
        try {
            if (future != null) {
                future.cancel(true);
                future = null;
            }
            if (scheduledExecutorService != null) {
                scheduledExecutorService.shutdownNow();
                scheduledExecutorService = null;
            }
        } catch (Throwable t) {
            LOGGER.warn(t.getMessage(), t);
        } finally {
            scheduledServiceMap.remove(this);
            started = false;
        }
    }

    /**
     * 停止执行定时任务，还可以重新start
     */
    public void shutdown() {
        stop();
    }

    /**
     * 是否已经启动
     *
     * @return the boolean
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * 缓存了目前全部的定时任务， 用于重建
     */
    protected final static Map<ScheduledService, Long> scheduledServiceMap = new ConcurrentHashMap<ScheduledService,
            Long>();

    /**
     * 重建定时任务，用于特殊情况
     */
    public static synchronized void reset() {
        resetting = true;
        LOGGER.warn("Start resetting all {} schedule executor service.", scheduledServiceMap.size());
        for (Map.Entry<ScheduledService, Long> entry : scheduledServiceMap.entrySet()) {
            try {
                ScheduledService service = entry.getKey();
                if (service.isStarted()) {
                    service.stop();
                    service.start();
                }
            } catch (Exception e) {
                LOGGER.error("Error when restart schedule service", e);
            }
        }
        LOGGER.warn("Already reset all {} schedule executor service.", scheduledServiceMap.size());
        resetting = false;
    }

    /**
     * 正在重置标识
     */
    protected static volatile boolean resetting;

    /**
     * 是否正在重置
     *
     * @return the boolean
     */
    public static boolean isResetting() {
        return resetting;
    }
}
