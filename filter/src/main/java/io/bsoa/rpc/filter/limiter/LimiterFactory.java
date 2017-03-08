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
package io.bsoa.rpc.filter.limiter;

import io.bsoa.rpc.common.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限制器接口
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class LimiterFactory {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(LimiterFactory.class);

    /**
     * 基于监控服务计数限制
     */
    private final static String LIMITER_TYPE_MONITOR = "0";
    /**
     * 基于计数器服务计数限制
     */
    private final static String LIMITER_TYPE_COUNTER = "1";

    /**
     * 是否开启监控统计限流，默认为关闭，当有收到下发时间后会开启此功能
     */
    private static boolean open = false;

    /**
     * 服务端是否开启限制流量功能,全局的一个配置
     */
    private static boolean providerLimitOpen = false;

    /**
     * InvokeLimiter缓存 一个接口一个
     */
    private final static ConcurrentHashMap<String, Limiter> INVOKE_LIMITER_CACHE
            = new ConcurrentHashMap<String, Limiter>();


    /**
     * 服务端限流
     * <p>
     * key:interfaceName#method#alias
     * <p>
     * value:limiter
     */
    private final static ConcurrentHashMap<String, Limiter> INVOKE_PROVIDER_LIMITER_CACHE
            = new ConcurrentHashMap<String, Limiter>();

    /**
     * 清空限制
     */
    public static void clearRules() {
        INVOKE_LIMITER_CACHE.clear();
        LimiterFactory.open = false;
    }

    /**
     * 是否有限制
     *
     * @param interfaceId 接口名
     * @param methodName  方法名
     * @param alias       服务别名
     * @param appId       appId
     * @return boolean 返回true，不让调用
     */
    public static boolean isOverLimit(String interfaceId, String methodName, String alias, String appId) {
        if (INVOKE_LIMITER_CACHE.isEmpty()) {
            return false;
        }
        String key = buildKey(interfaceId, methodName, alias, appId);
        Limiter limiter = INVOKE_LIMITER_CACHE.get(key);
        return limiter != null && limiter.isOverLimit(interfaceId, methodName, alias, appId);
    }


    public static ProviderInvokerLimiter getProviderLimiter(String interfaceId, String methodName, String alias, String appId) {
        if (INVOKE_PROVIDER_LIMITER_CACHE.isEmpty()) {
            return null;
        }
        String key = buildProviderLimiterKey(interfaceId, methodName, alias, appId);
        ProviderInvokerLimiter limiter = (ProviderInvokerLimiter) INVOKE_PROVIDER_LIMITER_CACHE.get(key);
        if (limiter == null) {
            //没有限制此appid的 看是否有限制所有app的,appid 为空代表限制所有 app
            key = buildProviderLimiterKey(interfaceId, methodName, alias, "");
        }
        limiter = (ProviderInvokerLimiter) INVOKE_PROVIDER_LIMITER_CACHE.get(key);
        return limiter;
    }

    private static String buildKey(String interfaceId, String methodName, String alias, String appId) {
        return interfaceId + "#" + methodName + "#" + alias + "#" + appId;
    }

    private static String buildProviderLimiterKey(String interfaceId, String methodName, String alias, String appId) {
        return interfaceId + "#" + methodName + "#" + alias + "#" + appId;
    }

    /**
     * 得到json格式的app限制规则
     *
     * @return app限制规则
     */
    public static String getAllLimitDetails() {
        if (INVOKE_LIMITER_CACHE.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder(1024);
        sb.append("{");
        for (Map.Entry<String, Limiter> entry : INVOKE_LIMITER_CACHE.entrySet()) {
            sb.append("\"").append(entry.getKey()).append("\":\"")
                    .append(entry.getValue().getDetails()).append("\"").append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

    /**
     * 得到json格式的限制规则
     *
     * @return 限制规则
     */
    public static String getLimitDetails(String interfaceId, boolean isProvider) {
        Map<String, Limiter> limiterMap = null;
        if (isProvider) {
            limiterMap = INVOKE_PROVIDER_LIMITER_CACHE;
        } else {
            limiterMap = INVOKE_LIMITER_CACHE;
        }
        if (limiterMap.isEmpty()) {
            return null;
        }
        List<Map.Entry<String, Limiter>> matches = new ArrayList<Map.Entry<String, Limiter>>();
        for (Map.Entry<String, Limiter> entry : limiterMap.entrySet()) {
            if (entry.getKey().startsWith(interfaceId + "#")) {
                matches.add(entry);
            }
        }
        if (CommonUtils.isNotEmpty(matches)) {
            StringBuilder sb = new StringBuilder(1024);
            sb.append("[");
            for (Map.Entry<String, Limiter> entry : matches) {
                sb.append("\"").append(entry.getKey()).append("\":\"")
                        .append(entry.getValue().getDetails()).append("\"").append(", ");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append("]");
            return sb.toString();
        }
        return null;
    }

    public static boolean isFunctionOpen() {
        return open;
    }


    public static boolean isGlobalProviderLimitOpen() {
        return providerLimitOpen;
    }

}