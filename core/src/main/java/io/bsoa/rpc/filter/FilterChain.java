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
package io.bsoa.rpc.filter;

import io.bsoa.rpc.base.Invoker;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.config.AbstractInterfaceConfig;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.config.ProviderConfig;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.ext.ExtensionClass;
import io.bsoa.rpc.ext.ExtensionLoader;
import io.bsoa.rpc.ext.ExtensionLoaderFactory;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by zhangg on 2016/7/16 00:56.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class FilterChain {

    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FilterChain.class);

    /**
     * 服务端自动激活的 {"alias":ExtensionClass}
     */
    private final static ConcurrentHashMap<String, ExtensionClass<Filter>> providerAutoActives
            = new ConcurrentHashMap<>();

    /**
     * 调用端自动激活的 {"alias":ExtensionClass}
     */
    private final static ConcurrentHashMap<String, ExtensionClass<Filter>> consumerAutoActives
            = new ConcurrentHashMap<>();

    /**
     * 扩展加载器
     */
    private final static ExtensionLoader<Filter> EXTENSION_LOADER
            = ExtensionLoaderFactory.getExtensionLoader(Filter.class, extensionClass -> {
        Class<? extends Filter> implClass = extensionClass.getClazz();
        // 读取自动加载的类列表。
        AutoActive autoActive = implClass.getAnnotation(AutoActive.class);
        if (autoActive != null) {
            String alias = extensionClass.getAlias();
//            extensionClass.setAutoActive(true);
            if (autoActive.providerSide()) {
                providerAutoActives.put(alias, extensionClass);
            } else if (autoActive.consumerSide()) {
                consumerAutoActives.put(alias, extensionClass);
            }
//            extensionClass.setProviderSide(autoActive.providerSide());
//            extensionClass.setConsumerSide(autoActive.consumerSide());
//            autoActives.put(alias, extensionClass);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Extension of interface " + Filter.class
                        + ", " + implClass + "(" + alias + ") will auto active");
            }
        }
    });

    /**
     * 调用链
     */
    private FilterInvoker invokerChain;

    /**
     * 构造执行链
     *
     * @param filters     包装过滤器列表
     * @param lastInvoker 最终过滤器
     * @param config      接口配置
     */
    protected FilterChain(List<Filter> filters, FilterInvoker lastInvoker, AbstractInterfaceConfig config) {
        // 调用过程外面包装多层自定义filter
        // 前面的过滤器在最外层
        invokerChain = lastInvoker;
        if (CommonUtils.isNotEmpty(filters)) {
            for (int i = filters.size() - 1; i >= 0; i--) {
                try {
                    Filter filter = filters.get(i);
                    if (filter.needToLoad(invokerChain)) {
                        invokerChain = new FilterInvoker(filter, invokerChain, config);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error when build filter chain", e);
                    throw new BsoaRuntimeException(22222, "加载filter列表异常", e);
                }
            }
        }
    }

    /**
     * 构造服务端的执行链
     *
     * @param providerConfig provider配置
     * @param lastFilter     最后一个filter
     * @return filter执行链
     */
    public static FilterChain buildProviderChain(ProviderConfig providerConfig, FilterInvoker lastFilter) {
        /*
         * 例如自动装载扩展 A(a),B(b),C(c)  filter=[-a,d]  filterRef=[new E, new Exclude(b)]
         * 逻辑如下：
         * 1.解析config.getFilterRef()，记录E和-b
         * 2.解析config.getFilter()字符串，记录 d 和 -a,-b
         * 3.再解析自动装载扩展，a,b被排除了，所以拿到c,d
         * 4.对c d进行排序
         * 5.拿到C、D实现类
         * 6.加上自定义，返回C、D、E
         */
        // 用户通过自己new实例的方式注入的filter，优先级高
        List<Filter> customFilters = providerConfig.getFilterRef() == null ?
                new ArrayList<>() : new CopyOnWriteArrayList<>(providerConfig.getFilterRef());
        // 先解析是否有特殊处理
        HashSet<String> excludes = parseExcludeFilter(customFilters);

        // 准备数据：用户通过别名的方式注入的filter，需要解析
        List<ExtensionClass<Filter>> extensionFilters = new ArrayList<>();
        List<String> filterAliases = providerConfig.getFilter(); //
        if (CommonUtils.isNotEmpty(filterAliases)) {
            for (String filterAlias : filterAliases) {
                if (filterAlias.startsWith("-")) { // 排除用的特殊字符
                    excludes.add(filterAlias.substring(1));
                } else {
                    extensionFilters.add(EXTENSION_LOADER.getExtensionClass(filterAlias));
                }
            }
        }
        // 解析自动加载的过滤器
        if (!excludes.contains("*") && !excludes.contains("default")) {  // 配了-*和-default表示不加载内置
            for (Map.Entry<String, ExtensionClass<Filter>> entry : providerAutoActives.entrySet()) {
                if (!excludes.contains(entry.getKey())) {
                    extensionFilters.add(entry.getValue());
                }
            }
        }
        excludes = null; // 不需要了
        // 按order从小到大排序
        if (extensionFilters.size() > 1) {
            extensionFilters.sort(new ExtensionLoader.OrderComparator());
        }
        List<Filter> actualFilters = new ArrayList<>();
        for (ExtensionClass<Filter> extensionFilter : extensionFilters) {
            actualFilters.add(extensionFilter.getExtInstance());
        }
        // 加入自定义的过滤器
        actualFilters.addAll(customFilters);
        return new FilterChain(actualFilters, lastFilter, providerConfig);
    }

    /**
     * 构造调用端的执行链
     *
     * @param consumerConfig consumer配置
     * @param lastFilter     最后一个filter
     * @return filter执行链
     */
    public static FilterChain buildConsumerChain(ConsumerConfig consumerConfig, FilterInvoker lastFilter) {

         /*
         * 例如自动装载扩展 A(a),B(b),C(c)  filter=[-a,d]  filterRef=[new E, new Exclude(b)]
         * 逻辑如下：
         * 1.解析config.getFilterRef()，记录E和-b
         * 2.解析config.getFilter()字符串，记录 d 和 -a,-b
         * 3.再解析自动装载扩展，a,b被排除了，所以拿到c,d
         * 4.对c d进行排序
         * 5.拿到C、D实现类
         * 6.加上自定义，返回C、D、E
         */
        // 用户通过自己new实例的方式注入的filter，优先级高
        List<Filter> customFilters = consumerConfig.getFilterRef() == null ?
                new ArrayList<>() : new CopyOnWriteArrayList<>(consumerConfig.getFilterRef());
        // 先解析是否有特殊处理
        HashSet<String> excludes = parseExcludeFilter(customFilters);

        // 准备数据：用户通过别名的方式注入的filter，需要解析
        List<ExtensionClass<Filter>> extensionFilters = new ArrayList<>();
        List<String> filterAliases = consumerConfig.getFilter(); //
        if (CommonUtils.isNotEmpty(filterAliases)) {
            for (String filterAlias : filterAliases) {
                if (filterAlias.startsWith("-")) { // 排除用的特殊字符
                    excludes.add(filterAlias.substring(1));
                } else {
                    extensionFilters.add(EXTENSION_LOADER.getExtensionClass(filterAlias));
                }
            }
        }
        // 解析自动加载的过滤器
        if (!excludes.contains("*") && !excludes.contains("default")) {  // 配了-*和-default表示不加载内置
            for (Map.Entry<String, ExtensionClass<Filter>> entry : consumerAutoActives.entrySet()) {
                if (!excludes.contains(entry.getKey())) {
                    extensionFilters.add(entry.getValue());
                }
            }
        }
        excludes = null; // 不需要了
        // 按order从小到大排序
        if (extensionFilters.size() > 1) {
            extensionFilters.sort(new ExtensionLoader.OrderComparator());
        }
        List<Filter> actualFilters = new ArrayList<>();
        for (ExtensionClass<Filter> extensionFilter : extensionFilters) {
            actualFilters.add(extensionFilter.getExtInstance());
        }
        // 加入自定义的过滤器
        actualFilters.addAll(customFilters);
        return new FilterChain(actualFilters, lastFilter, consumerConfig);
    }

    /**
     * 判断是否需要排除系统过滤器
     *
     * @param customFilters 自定义filter
     * @return 是否排除
     */
    private static HashSet<String> parseExcludeFilter(List<Filter> customFilters) {
        HashSet<String> excludeKeys = new HashSet<String>();
        if (CommonUtils.isNotEmpty(customFilters)) {
            for (Filter filter : customFilters) {
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
     * @param rpcRequest 请求支持
     * @return 调用结果 response message
     */
    public RpcResponse invoke(RpcRequest rpcRequest) {
        return getChain().invoke(rpcRequest);
    }

    /**
     * 得到执行链
     *
     * @return chain
     */
    protected Invoker getChain() {
        return invokerChain;
    }

}
