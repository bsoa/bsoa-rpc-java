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
package io.bsoa.rpc.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.ExtensionClass;
import io.bsoa.rpc.ext.ExtensionLoader;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 *
 *
 * Created by zhangg on 2016/7/16 00:56.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class FilterChain {

    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(FilterChain.class);

    /**
     * 调用链
     */
    private Filter chain;

    /**
     * 配置
     */
    private Map<String, Object> configContext;

    /**
     * 构造执行链
     *
     * @param filters
     *         包装过滤器列表
     * @param lastFilter
     *         最终过滤器
     * @param context
     *         上下文
     */
    protected FilterChain(List<AbstractFilter> filters, Filter lastFilter, Map<String, Object> context) {
        chain = lastFilter;
        configContext = context;
        // 调用过程外面包装多层自定义filter
        // 前面的过滤器在最外层
        if (CommonUtils.isNotEmpty(filters)) {
            for (int i = filters.size() - 1; i >= 0; i--) {
                try {
                    AbstractFilter filter = filters.get(i);
                    if (filter.getNext() != null) {
                        LOGGER.warn("[JSF-22000]Filter {} has been already used, maybe it's singleton, " +
                                "jsf will try to clone new instance instead of it", filter);
                        filter = (AbstractFilter) filter.clone(); // 复制生成一个新对象
                    }
                    filter.setNext(chain);
                    filter.setConfigContext(configContext);
                    chain = filter;
                } catch (Exception e) {
                    LOGGER.error("加载filter列表异常", e);
                    throw new BsoaRuntimeException(22222,"加载filter列表异常", e);
                }
            }
        }
    }

    /**
     * 构造服务端的执行链
     *
     * @param providerConfig
     *         provider配置
     * @param lastFilter
     *         最后一个filter
     * @return filter执行链
     */
    public static FilterChain buildProviderChain(ProviderConfig providerConfig, Filter lastFilter) {

        Map<String, Object> context = providerConfig.getConfigValueCache(true);
        // 自定义的filter处理，如果通过spring加载，可以设置scope来设置是单例还是多例

        List<AbstractFilter> customFilters = providerConfig.getFilter() == null ?
                null : new CopyOnWriteArrayList(providerConfig.getFilter());
        // 先解析是否有特殊处理
        HashSet<String> excludes = parseExcludeFilter(customFilters);

        // 构造执行链
        List<AbstractFilter> filters = new ArrayList<AbstractFilter>();
//        if (!excludes.contains("*") && !excludes.contains("default")) {
//            if (CommonUtils.isLinux()
//                    && !CommonUtils.isFalse(JSFContext.getGlobalVal(Constants.SETTING_CHECK_SYSTEM_TIME, "true"))) {
//                filters.add(new SystemTimeCheckFilter()); // 系统时间检查过滤器
//            }
//            if (!excludes.contains("exception")) {
//                filters.add(new ExceptionFilter()); // 异常过滤器
//            }
//            filters.add(new ProviderContextFilter()); // 上下文过滤器
//            if (!excludes.contains("providerGeneric")) {
//                filters.add(new ProviderGenericFilter()); // 泛化调用过滤器
//            }
//            if (!excludes.contains("providerHttpGW")) {
//                filters.add(new ProviderHttpGWFilter());
//            }
//            if (!excludes.contains("providerLimiter")){
//                filters.add(new ProviderInvokeLimitFilter());
//            }
//            if (providerConfig.hasToken() && !excludes.contains("token")) {
//                filters.add(new TokenFilter()); // Token认证过滤器
//            }
//            if(!excludes.contains("providerMethodCheck")) {
//                filters.add(new ProviderMethodCheckFilter(providerConfig)); // 检查方法是否调用
//            }
//            if(!excludes.contains("providerTimeout")) {
//                filters.add(new ProviderTimeoutFilter()); // 超时过滤器
//            }
//            if (providerConfig.hasValidation() && !excludes.contains("validation")) {
//                filters.add(new ValidationFilter()); // 参数校验过滤器
//            }
//            if (providerConfig.hasCache() && !excludes.contains("cache")) {
//                filters.add(new CacheFilter(providerConfig)); // 缓存过滤器
//            }
//            //if (!excludes.contains("mock")) {
//            //    filters.add(new MockFilter(providerConfig));
//            //}
//            if (providerConfig.hasConcurrents() && !excludes.contains("providerConcurrents")) { // 并发控制过滤器
//                filters.add(new ProviderConcurrentsFilter(providerConfig));
//            }
//        }
        if (CommonUtils.isNotEmpty(customFilters)) {
            filters.addAll(customFilters);
        }
        //加载META-INF目录下的自定义filter
        ExtensionLoader<Filter> extensionLoader = ExtensionLoaderFactory.getExtensionLoader(Filter.class);
        if (extensionLoader != null) {
            for (ExtensionClass<Filter> extensionClass : extensionLoader.getProviderSideAutoActives()) {
                Filter filter = extensionClass.getExtInstance();
                if (filter != null && filter instanceof AbstractFilter) {
                    LOGGER.info("load provider extension filter:{}", filter.getClass().getCanonicalName());
                    filters.add((AbstractFilter) filter);
                }
            }
        }
        return new FilterChain(filters, lastFilter, context);
    }

    /**
     * 构造调用端的执行链
     *
     * @param consumerConfig
     *         consumer配置
     * @param lastFilter
     *         最后一个filter
     * @return filter执行链
     */
    public static FilterChain buildConsumerChain(ConsumerConfig consumerConfig, Filter lastFilter) {

        Map<String, Object> context = consumerConfig.getConfigValueCache(true);
        // 自定义的filter处理，如果通过spring加载，可以设置scope来设置是单例还是多例
        List<AbstractFilter> customFilters = consumerConfig.getFilter() == null ?
                null : new CopyOnWriteArrayList(consumerConfig.getFilter());
        // 先解析是否有特殊处理
        HashSet<String> excludes = parseExcludeFilter(customFilters);

        // 构造执行链
        List<AbstractFilter> filters = new ArrayList<AbstractFilter>();
//        if (!excludes.contains("*") && !excludes.contains("default")) {
//            if (CommonUtils.isLinux()
//                    && !excludes.contains("systemTimeCheck")
//                    && !CommonUtils.isFalse(JSFContext.getGlobalVal(Constants.SETTING_CHECK_SYSTEM_TIME, "true"))) {
//                filters.add(new SystemTimeCheckFilter()); // 系统时间检查过滤器
//            }
//            if (!excludes.contains("exception")) {
//                filters.add(new ExceptionFilter()); // 异常过滤器
//            }
//            if (consumerConfig.isGeneric() && !excludes.contains("consumerGeneric")) { // 泛化调用过滤器
//                filters.add(new ConsumerGenericFilter());
//            }
//            filters.add(new ConsumerContextFilter()); // 上下文过滤器
//            if (consumerConfig.hasCache() && !excludes.contains("cache")) {
//                filters.add(new CacheFilter(consumerConfig)); // 缓存过滤器
//            }
//            if (!excludes.contains("mock")) { // 模拟调用过滤器
//                filters.add(new MockFilter(consumerConfig));
//            }
//            if (JSFContext.get(JSFContext.KEY_APPID) != null && !excludes.contains("consumerInvokeLimit")) {
//                filters.add(new ConsumerInvokeLimitFilter()); // 调用次数限制过滤器
//            }
//            if (consumerConfig.hasValidation() && !excludes.contains("validation")) {
//                filters.add(new ValidationFilter()); // 参数校验过滤器
//            }
//
//            if (consumerConfig.hasConcurrents() && !excludes.contains("consumerConcurrents")) { // 并发控制过滤器
//                filters.add(new ConsumerConcurrentsFilter(consumerConfig));
//            }
//            filters.add(new ConsumerMonitorFilter());
//        }

        if (CommonUtils.isNotEmpty(customFilters)) {
            filters.addAll(customFilters);
        }
        //加载META-INF目录下的自定义filter
        ExtensionLoader<Filter> extensionLoader = ExtensionLoaderFactory.getExtensionLoader(Filter.class);
        if ( extensionLoader != null ){
            for (ExtensionClass<Filter> extensionClass : extensionLoader.getConsumerSideAutoActives()){
                Filter filter = extensionClass.getExtInstance();
                if ( filter != null && filter instanceof AbstractFilter){
                    LOGGER.info("load consumer extension filter:{}",filter.getClass().getCanonicalName());
                    filters.add((AbstractFilter)filter);
                }
            }
        }
        return new FilterChain(filters, lastFilter, context);
    }

    /**
     * 判断是否需要排除系统过滤器
     *
     * @param customFilters
     *         自定义filter
     * @return 是否排除
     */
    private static HashSet<String> parseExcludeFilter(List<AbstractFilter> customFilters) {
        HashSet<String> excludeKeys = new HashSet<String>();
        if (CommonUtils.isNotEmpty(customFilters)) {
            for (AbstractFilter filter : customFilters) {
                if (filter instanceof ExcludeFilter) {
                    // 存在需要排除的过滤器
                    ExcludeFilter excludeFilter = (ExcludeFilter) filter;
                    String excludeFilterName = excludeFilter.getExcludeFilterName().substring(1);
                    if (StringUtils.isNotEmpty(excludeFilterName)) {
                        excludeKeys.add(excludeFilterName);
                    }
                    customFilters.remove(filter);
                }
            }
        }
        if (!excludeKeys.isEmpty()) {
            LOGGER.info("Find exclude filters: {}", excludeKeys);
        }
        return excludeKeys;
    }

    /**
     * proxy拦截的调用
     *
     * @param requestMessage
     *         请求支持
     * @return 调用结果 response message
     */
    public RpcResponse invoke(RpcRequest requestMessage) {
        return getChain().invoke(requestMessage);
    }

    /**
     * 得到执行链
     *
     * @return chain
     */
    protected Filter getChain() {
        return chain;
    }

}
