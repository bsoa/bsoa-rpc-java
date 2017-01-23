/**
 * CounterInvokeLimiter.java Created on 2015/4/15 11:01
 * <p/>
 * Copyright (c) 2015 by www.jd.com.
 */
package io.bsoa.rpc.filter.limiter;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jd.jsf.Counter;
import com.jd.jsf.gd.config.ConsumerConfig;
import com.jd.jsf.gd.registry.RegistryFactory;
import com.jd.jsf.gd.util.CommonUtils;
import com.jd.jsf.gd.util.JSFContext;
import io.bsoa.rpc.common.BsoaConstants;

/**
 * Title: 基于计数器服务的限制器<br>
 * <p/>
 * Description: <br>
 * <p/>
 * Company: <a href=www.jd.com>京东</a><br>
 *
 * @author <a href=mailto:zhanggeng@jd.com>章耿</a>
 */
public class CounterInvokeLimiter implements Limiter {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(CounterInvokeLimiter.class);

    /**
     * 本地计数器
     */
    private AtomicInteger localCnt = new AtomicInteger();

    /**
     * 批量调用次数
     */
    private int batchSize;

    /**
     * 上次返回结果
     */
    private volatile int lastResult = 0;

    public CounterInvokeLimiter() {
        // 批量调用次数
        batchSize = CommonUtils.parseInt(JSFContext.getGlobalVal("counter.batch", "1"), 1);
    }

    @Override
    public boolean isOverLimit(String interfaceName, String methodName, String alias, String appId) {

        if (localCnt.incrementAndGet() % batchSize == 0) {
            try {
                lastResult = getCounter().count(interfaceName, alias, methodName, batchSize, appId);
            } catch (Throwable e) {
                LOGGER.warn("Failed to invoke counter service", e);
                lastResult = 1;
            }
        }
        if (lastResult == -1) {
            return true;
        } else if (lastResult >= 100) {
            LOGGER.warn("Counter service return exception code {}", lastResult);
        }

        return false;
    }

    @Override
    public String getDetails() {
        return "CounterInvokeLimiter:" + lastResult;
    }

    /**
     * 远程计数器配置
     */
    private static ConsumerConfig<Counter> counterConfig;
    /**
     * 远程计数器
     */
    private static Counter counter;

    private static Counter getCounter() {
        if (counter == null) {
            synchronized (LOGGER) {
                if (counter == null) {
                    // CounterService调用超时时间
                    int timeout = CommonUtils.parseInt(JSFContext.getGlobalVal("counter.timeout", "50"), 50);

                    counterConfig = new ConsumerConfig<Counter>();
                    counterConfig.setRegistry(RegistryFactory.getRegistryConfigs());
                    counterConfig.setInterfaceId(Counter.class.getName());
                    counterConfig.setProtocol(BsoaConstants.DEFAULT_PROTOCOL);
                    counterConfig.setAlias(BsoaConstants.DEFAULT_PROTOCOL);
                    counterConfig.setTimeout(timeout);
                    counterConfig.setParameter(BsoaConstants.HIDDEN_KEY_MONITOR, "false");
                    counter = counterConfig.refer();
                }
            }
        }
        return counter;
    }

    /**
     * 释放资源
     */
    protected static void unrefer() {
        ConsumerConfig tmp = counterConfig;
        counterConfig = null;
        counter = null;
        try {
            tmp.unrefer();
        } catch (Exception e) {
            LOGGER.error("Exception when unrefer consumer config of Counter", e);
        }
    }
}