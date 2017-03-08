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
package io.bsoa.rpc.client.router;

import io.bsoa.rpc.client.ProviderInfo;
import io.bsoa.rpc.client.Router;
import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.message.RpcRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 参数化的路由基类: 路由是基于一个参数，例如本地ip，方法名，方法参数值
 * <p>
 * Created by zhangg on 2017/01/07 15:32.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public abstract class ParameterizedRouter implements Router {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ParameterizedRouter.class);

    /**
     * 规则( "method==sayHello" )
     */
    protected ParameterizedRule rule;

    /**
     * 命中规则后，服务端列表的匹配规则，例如 "10.12.113.*"
     */
    protected String matchRule;

    public ParameterizedRouter() {
        // FIXME
    }

    /**
     * 构造路由器
     *
     * @param routerKey  关键字：method==sayHello
     * @param routerRule 路由地址：10.12.113.11*
     * @return Router
     */
    public ParameterizedRouter(String routerKey, String routerRule) {
        rule = new ParameterizedRule(routerKey); // 解析规则
        this.matchRule = routerRule;
    }

    @Override
    public void setRule(String ruleJson) {
        this.matchRule = ruleJson;
    }

    @Override
    public List<ProviderInfo> route(RpcRequest request, List<ProviderInfo> providerInfos) {
        if (matchRule(rule, request)) {
            List<ProviderInfo> matchedProviderInfos = new ArrayList<ProviderInfo>();
            // 匹配规则，根据这个规则筛选Provider列表
            for (ProviderInfo providerInfo : providerInfos) {
                if (matchProvider(matchRule, providerInfo)) {
                    matchedProviderInfos.add(providerInfo);
                }
            }
            return matchedProviderInfos;
        }
        return providerInfos; //TODO 没有任何匹配，返回全部的??
    }

    /**
     * 是否匹配规则
     *
     * @param rule  规则（例如 v==0)
     * @param value 参数值
     * @return
     */
    public abstract boolean matchRule(ParameterizedRule rule, RpcRequest value);

    /**
     * 匹配数字
     *
     * @param rule  规则对象
     * @param value 值（不为空）
     * @return
     */
    protected boolean matchInteger(ParameterizedRule rule, Integer value) {
        int cfg = Integer.parseInt(rule.getRight());
        RelationalOperator op = rule.getRelationalOperator();
        switch (op) {
            case eq:
                return value != null && value == cfg;
            case neq:
                return value != null && value != cfg;
            case gteq:
                return value != null && value >= cfg;
            case lteq:
                return value != null && value <= cfg;
            case gt:
                return value != null && value > cfg;
            case lt:
                return value != null && value < cfg;
            case nil:
                return value == null;
            default:
                break;
        }
        return false;
    }

    /**
     * 匹配数字
     *
     * @param rule  规则对象
     * @param value 值（不为空）
     * @return
     */
    protected boolean matchLong(ParameterizedRule rule, Long value) {
        long cfg = Long.parseLong(rule.getRight());
        RelationalOperator op = rule.getRelationalOperator();
        switch (op) {
            case eq:
                return value != null && value == cfg;
            case neq:
                return value != null && value != cfg;
            case gteq:
                return value != null && value >= cfg;
            case lteq:
                return value != null && value <= cfg;
            case gt:
                return value != null && value > cfg;
            case lt:
                return value != null && value < cfg;
            case nil:
                return value == null;
            default:
                break;
        }
        return false;
    }

    /**
     * 匹配对象(toString)支持 == 和 != 和空
     *
     * @param rule  规则（例如 v=0)
     * @param value 值
     * @return（不为空）
     */
    protected boolean matchObject(ParameterizedRule rule, Object value) {
        RelationalOperator op = rule.getRelationalOperator();
        switch (op) {
            case eq:
                return value != null && rule.getRight().equals(value.toString());
            case neq:
                return value != null && !rule.getRight().equals(value.toString());
            case nil:
                return value == null;
            default:
                throw new BsoaRpcException(22222, "Unsupported relational operator [" + op + "] of Object");
        }
    }

    /**
     * 匹配字符串 支持== 和 != 和空
     *
     * @param rule  规则对象
     * @param value 值（不为空）
     * @return
     */
    protected boolean matchString(ParameterizedRule rule, String value) {
        RelationalOperator op = rule.getRelationalOperator();
        switch (op) {
            case eq:
                return rule.getRight().equals(value);
            case neq:
                return !rule.getRight().equals(value);
            case nil:
                return value == null;
            default:
                throw new BsoaRpcException(22222, "Unsupported relational operator [\" + op + \"] of String");
        }
    }

    /**
     * 匹配服务提供者
     *
     * @param ruleDst      规则指向
     * @param providerInfo Provider对象
     * @return 是否匹配
     */
    protected boolean matchProvider(String ruleDst, ProviderInfo providerInfo) {
        // 根据ip匹配
        return NetUtils.isMatchIPByPattern(ruleDst, providerInfo.getIp());
    }

    /**
     * 关系运算符
     */
    enum RelationalOperator {
        nil, // is null
        eq, // ==
        neq, // !=
        gteq, // >=
        lteq,// <=
        gt,// >
        lt;// <
    }

    class ParameterizedRule {
        /**
         * 运算符左侧
         */
        private final String left;
        /**
         * 运算符右侧
         */
        private final String right;
        /**
         * 关系运算符 == != >= <= > <
         */
        private final RelationalOperator relationalOperator;

        ParameterizedRule(String rule) {
            int idx;
            if ((idx = rule.indexOf("==")) > 0) {
                left = rule.substring(0, idx).trim();
                right = rule.substring(idx + 2).trim();
                relationalOperator = RelationalOperator.eq;
            } else if ((idx = rule.indexOf("!=")) > 0) {
                left = rule.substring(0, idx).trim();
                right = rule.substring(idx + 2).trim();
                relationalOperator = RelationalOperator.neq;
            } else if ((idx = rule.indexOf(">=")) > 0) {
                left = rule.substring(0, idx).trim();
                right = rule.substring(idx + 2).trim();
                relationalOperator = RelationalOperator.gteq;
            } else if ((idx = rule.indexOf("<=")) > 0) {
                left = rule.substring(0, idx).trim();
                right = rule.substring(idx + 2).trim();
                relationalOperator = RelationalOperator.lteq;
            } else if ((idx = rule.indexOf(">")) > 0) {
                left = rule.substring(0, idx).trim();
                right = rule.substring(idx + 1).trim();
                relationalOperator = RelationalOperator.gt;
            } else if ((idx = rule.indexOf("<")) > 0) {
                left = rule.substring(0, idx).trim();
                right = rule.substring(idx + 1).trim();
                relationalOperator = RelationalOperator.lt;
            } else if ((idx = rule.indexOf("is null")) > 0) {
                left = rule.substring(0, idx).trim();
                right = null;
                relationalOperator = RelationalOperator.nil;
            } else {
                throw new BsoaRpcException(22222, "[21600]Illegal route rule :" + rule);
            }
        }

        public String getLeft() {
            return left;
        }

        public String getRight() {
            return right;
        }

        public RelationalOperator getRelationalOperator() {
            return relationalOperator;
        }

    }

}

