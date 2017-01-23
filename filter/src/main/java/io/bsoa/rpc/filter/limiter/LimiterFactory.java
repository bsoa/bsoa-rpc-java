/**
 * LimiterFactroy.java Created on 2015/4/15 11:08
 * <p/>
 * Copyright (c) 2015 by www.jd.com.
 */
package io.bsoa.rpc.filter.limiter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jd.fastjson.JSONObject;
import com.jd.jsf.gd.util.*;

/**
 * Title: <br>
 * <p/>
 * Description: <br>
 * <p/>
 * Company: <a href=www.jd.com>京东</a><br>
 *
 * @author <a href=mailto:zhanggeng@jd.com>章耿</a>
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
     *
     * key:interfaceName#method#alias
     *
     * value:limiter
     */
    private final static ConcurrentHashMap<String, Limiter> INVOKE_PROVIDER_LIMITER_CACHE
            = new ConcurrentHashMap<String, Limiter>();

    /**
     * 根据注册中心回传的参数进行限制
     *
     * @param interfaceId
     *         the interface id
     * @param methodName
     *         the method name
     * @param alias
     *         the alias
     * @param appId
     *         the app id
     * @param canInvoke
     *         the can invoke
     */
    public static synchronized void addMonitorLimiterRule(String interfaceId, String methodName, String alias,
                                                          String appId, boolean canInvoke) {
        try {
            String key = buildKey(interfaceId, methodName, alias, appId);
            if (canInvoke) { // 可以调用
                Limiter limiter = INVOKE_LIMITER_CACHE.get(key);
                if (limiter != null && limiter instanceof MonitorInvokeLimiter) {
                    ((MonitorInvokeLimiter) limiter).setCanInvoke(true); // 从不可用到可用
                }
            } else { //变成不能调用
                Limiter limiter = INVOKE_LIMITER_CACHE.get(key);
                if (limiter == null) { // 新增
                    limiter = new MonitorInvokeLimiter(key);
                    INVOKE_LIMITER_CACHE.put(key, limiter);
                }
                ((MonitorInvokeLimiter) limiter).setCanInvoke(canInvoke);
            }
        } catch (Exception e) {
            LOGGER.error("Error when update app limit data of " + interfaceId + "." + methodName + "/" + alias
                    + ", the error data is " + appId + ":" + canInvoke, e);
        }
        LimiterFactory.open = !INVOKE_LIMITER_CACHE.isEmpty();
    }

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
     * @param interfaceId
     *         接口名
     * @param methodName
     *         方法名
     * @param alias
     *         服务别名
     * @param appId
     *         appId
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



    public static ProviderInvokerLimiter getProviderLimiter(String interfaceId, String methodName, String alias, String appId){
        if (INVOKE_PROVIDER_LIMITER_CACHE.isEmpty()) {
            return null;
        }
        String key = buildProviderLimiterKey(interfaceId, methodName, alias, appId);
        ProviderInvokerLimiter limiter = (ProviderInvokerLimiter)INVOKE_PROVIDER_LIMITER_CACHE.get(key);
        if (limiter == null){
            //没有限制此appid的 看是否有限制所有app的,appid 为空代表限制所有 app
            key = buildProviderLimiterKey(interfaceId,methodName,alias,"");
        }
        limiter = (ProviderInvokerLimiter)INVOKE_PROVIDER_LIMITER_CACHE.get(key);
        return limiter;
    }

    private static String buildKey(String interfaceId, String methodName, String alias, String appId) {
        return interfaceId + "#" + methodName + "#" + alias + "#" + appId;
    }

    private static String buildProviderLimiterKey(String interfaceId, String methodName, String alias, String appId) {
        return interfaceId + "#" + methodName + "#" + alias+"#"+appId;
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
        Map<String,Limiter> limiterMap = null;
        if (isProvider){
            limiterMap = INVOKE_PROVIDER_LIMITER_CACHE;
        } else {
            limiterMap = INVOKE_LIMITER_CACHE;
        }
        if (limiterMap == null || limiterMap.isEmpty()) {
            return null;
        }
        List<Map.Entry<String, Limiter>> matches = new ArrayList<Map.Entry<String, Limiter>>();
        for (Map.Entry<String, Limiter> entry : limiterMap.entrySet()) {
            if (entry.getKey().startsWith(interfaceId + "#")) {
                matches.add(entry);
            }
        }
        if(CommonUtils.isNotEmpty(matches)) {
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

    /**
     * 更新
     * @param interfaceId
     */
    public static void updateCache(String interfaceId) {
        String limitjson = JSFContext.getInterfaceVal(interfaceId, Constants.SETTING_INVOKE_APPLIMIT, null);
        if (StringUtils.isNotEmpty(limitjson)) {
            /* 注册中心有配置
            [{
                "method": "echoStr",
                "alias": "ZG",
                "appId": 11111,
                "type": 0
            }]
            */
            List<JSONObject> applimits = null;
            try {
                applimits = JsonUtils.parseObject(limitjson, List.class);
            } catch (Exception e) {
                LOGGER.error("[JSF-21602]Failed to parse app limit data of " + interfaceId
                        + ", the error app limit json is " + limitjson, e);
            }
            List<String> keys = new ArrayList<String>(); // 目前所有的key

            // 新增或者更新 配置
            if (applimits != null) {
                for (JSONObject applimit : applimits) {
                    String appId = StringUtils.toString(applimit.get("appId"));
                    if (!StringUtils.defaultString((String) JSFContext.get(JSFContext.KEY_APPID)).equals(appId)) {
                        // 不是当前appId的，忽略。
                        continue;
                    }
                    String method = (String) applimit.get("method");
                    String alias = (String) applimit.get("alias");
                    String type = StringUtils.toString(applimit.get("type"));
                    String key = buildKey(interfaceId, method, alias, appId);
                    try {
                        // 基于monitor的
                        if (LIMITER_TYPE_MONITOR.equals(type)) { // app限流
                            Limiter limiter = INVOKE_LIMITER_CACHE.get(key);
                            if (limiter != null) {
                                if (!(limiter instanceof MonitorInvokeLimiter)) { // 原来不是基于monitor的
                                    limiter = new MonitorInvokeLimiter(key);
                                    INVOKE_LIMITER_CACHE.put(key, limiter);
                                    LOGGER.info("Replace to app limit by monitor config : {}, {}", key,
                                            limiter.getDetails());
                                }
                            } else {
                                limiter = new MonitorInvokeLimiter(key);
                                INVOKE_LIMITER_CACHE.put(key, limiter);
                                LOGGER.info("Add app limit by monitor config : {}, {}", key,
                                        limiter.getDetails());
                            }
                        }
                        // 基于counter的
                        else if (LIMITER_TYPE_COUNTER.equals(type)) {
                            Limiter limiter = INVOKE_LIMITER_CACHE.get(key);
                            if (limiter != null) {
                                if (!(limiter instanceof CounterInvokeLimiter)) { // 原来不是基于counter的
                                    limiter = new CounterInvokeLimiter();
                                    INVOKE_LIMITER_CACHE.put(key, limiter);
                                    LOGGER.info("Replace to app limit by counter config : {}, {}", key,
                                            limiter.getDetails());
                                }
                            } else {
                                limiter = new CounterInvokeLimiter();
                                INVOKE_LIMITER_CACHE.put(key, limiter);
                                LOGGER.info("Add app limit by counter config : {}, {}", key,
                                        limiter.getDetails());
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to update limiter of " + interfaceId + "." + method
                                + ", alias : " + alias + ", type : " + type, e);
                    } finally {
                        keys.add(key); // 目前的key
                    }
                }
            }
            // 从缓存中干掉 已删除的配置
            for (String key : INVOKE_LIMITER_CACHE.keySet()) {
                if (key.startsWith(interfaceId + "#") && !keys.contains(key)) {
                    LOGGER.info("Remove deleted app limit config : {}", key);
                    INVOKE_LIMITER_CACHE.remove(key);
                }
            }
        }

        boolean old = open;
        open = !INVOKE_LIMITER_CACHE.isEmpty();
        if (!old && open) {
            LOGGER.info("App Limiter function changed to opened");
        } else if (old && !open) {
            LOGGER.info("App Limiter function changed to closed");
            CounterInvokeLimiter.unrefer(); // 功能关闭，销毁资源
        }
    }



    public static boolean isFunctionOpen() {
        return open;
    }


    public static boolean isGlobalProviderLimitOpen(){
        return providerLimitOpen;
    }

    public static void updateProviderLimitCache(String interfaceId) {
        String limitJson = JSFContext.getInterfaceVal(interfaceId, Constants.SETTING_INVOKE_PROVIDER_LIMIT, null);
        if (StringUtils.isNotEmpty(limitJson)) {
            /* 注册中心有配置
            [{
                "method": "echoStr",
                "alias": "ZG",
                "limit": 100,
                "appId":7941,
                "open":1//代表开启
            }]
            */
            List<JSONObject> providerLimits = null;
            try {
                providerLimits = JsonUtils.parseObject(limitJson, List.class);
            } catch (Exception e) {
                LOGGER.error("[JSF-21603]Failed to parse provider limit data of " + interfaceId
                        + ", the error provider limit json is " + limitJson, e);
            }
            List<String> keys = new ArrayList<String>(); // 目前所有的key
            // 新增或者更新 配置
            if (providerLimits != null) {
                for (JSONObject providerLimit : providerLimits) {
                    //如果appId为空说明是对所有的app限制流量
                    String appId = "";
                    //limit 为0 代表不限制
                    int limit = 0;
                    //是否开启
                    boolean open = false;
                    if ( providerLimit.containsKey("appId")){
                       appId = providerLimit.getString("appId");
                    }
                    if ( providerLimit.containsKey("limit")){
                        limit = providerLimit.getIntValue("limit");
                    }
                    if ( providerLimit.containsKey("open")){
                        open = providerLimit.getIntValue("open") == 1 ? true : false;
                    }
                    String method = (String) providerLimit.get("method");
                    String alias = (String) providerLimit.get("alias");
                    String key = buildProviderLimiterKey(interfaceId, method, alias,appId);
                    try {
                        ProviderInvokerLimiter limiter = (ProviderInvokerLimiter) INVOKE_PROVIDER_LIMITER_CACHE.get(key);
                        if (limiter != null) {
                            if (limit == 0 || !open){
                                INVOKE_PROVIDER_LIMITER_CACHE.remove(key);
                                LOGGER.info("Remove provider limit by config : {}, {}", key,
                                        limiter.getDetails());
                            } else {
                                limiter.updateLimit(limit);
                            }
                        } else {
                            if (open && limit > 0){
                                limiter = new ProviderInvokerLimiter(limit);
                                INVOKE_PROVIDER_LIMITER_CACHE.put(key, limiter);
                                LOGGER.info("Add provider limit by config : {}, {}", key,
                                        limiter.getDetails());
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to update limiter of " + interfaceId + "." + method
                                + ", alias : " + alias, e);
                    } finally {
                        keys.add(key); // 目前的key
                    }
                }
            }
            // 从缓存中干掉 已删除的配置
            for (String key : INVOKE_PROVIDER_LIMITER_CACHE.keySet()) {
                if (key.startsWith(interfaceId + "#") && !keys.contains(key)) {
                    LOGGER.info("Remove deleted provider limit config : {}", key);
                    INVOKE_PROVIDER_LIMITER_CACHE.remove(key);
                }
            }
        }

        boolean old = providerLimitOpen;
        providerLimitOpen = !INVOKE_PROVIDER_LIMITER_CACHE.isEmpty();
        if (!old && providerLimitOpen) {
            LOGGER.info("Provider Limiter function changed to opened");
        } else if (old && !providerLimitOpen) {
            LOGGER.info("Provider Limiter function changed to closed");
        }
    }

}