package io.bsoa.rpc.filter;

import java.io.Serializable;
import java.util.Map;

import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * 接口抽象类（Filter不对外，需实现自定义接口，继承此类）<br>
 * <p/>
 * Description: 1.实现invoke方法，可以对request和response进行处理，统计等<br>
 * 2.执行getNext().invoke(request)，调用链自动往下层执行<br>
 * 3.在getNext().invoke(request)前加的代码，将在远程方法调用前执行<br>
 * 4.在getNext().invoke(request)后加的代码，将在远程方法调用后执行<br>
 * <p/>
 *
 * Created by zhanggeng on 16-6-7.
 *
 * @author <a href=mailto:ujjboy@qq.com>Geng Zhang</a>
 */
public abstract class AbstractFilter implements Filter, Serializable, Cloneable {

    /**
     * 下一层过滤器
     */
    private Filter next;

    /**
     * <B>unmodifiable</B><br/>
     * 一些和请求无关的固定的上下文，这些内容从config里加载，和invocation无关<br/>
     * 例如是否开启validation配置，方法级是否开启配置。<br/>
     * 像请求ip端口这种和invocation有关的上下文不在此map中。
     */
    private Map<String, Object> configContext;

    /**
     * 调用远程服务
     *
     * @param request
     *         the request
     * @return the response message
     */
    abstract public RpcResponse invoke(RpcRequest request);

    /**
     * 下一级过滤器
     *
     * @return the next
     */
    protected Filter getNext() {
        return next;
    }

    /**
     * Sets next.
     *
     * @param next
     *         the next to set
     */
    protected void setNext(Filter next) {
        this.next = next;
    }

    /**
     * <B>unmodifiable</B><br/>
     * 一些和请求无关的固定的上下文，这些内容从config里加载，和invocation无关<br/>
     * 例如是否开启validation配置，方法级是否开启配置。<br/>
     * 像请求ip端口这种和invocation有关的上下文不在此map中。
     *
     * @return the configContext
     */
    protected Map<String, Object> getConfigContext() {
        return configContext;
    }

    /**
     * Sets configContext.
     *
     * @param configContext
     *         the configContext
     */
    protected void setConfigContext(Map<String, Object> configContext) {
        this.configContext = configContext;
    }

    /**
     * 取得方法的特殊参数配置
     *
     * @param methodName
     *         方法名
     * @param paramKey
     *         参数关键字
     * @param defaultValue
     *         默认值
     * @return 都找不到为false
     */
    protected boolean getBooleanMethodParam(String methodName, String paramKey, boolean defaultValue) {
        if (CommonUtils.isEmpty(configContext)) {
            return defaultValue;
        }
        Boolean o = (Boolean) configContext.get(buildmkey(methodName, paramKey));
        if (o == null) {
            o = (Boolean) configContext.get(paramKey);
            return o == null ? defaultValue : o;
        } else {
            return o;
        }
    }

    /**
     * 取得方法的特殊参数配置
     *
     * @param methodName
     *         方法名
     * @param paramKey
     *         参数关键字
     * @param defaultValue
     *         默认值
     * @return 都找不到为null
     */
    protected String getStringMethodParam(String methodName, String paramKey, String defaultValue) {
        if (CommonUtils.isEmpty(configContext)) {
            return defaultValue;
        }
        String o = (String) configContext.get(buildmkey(methodName, paramKey));
        if (o == null) {
            o = (String) configContext.get(paramKey);
            return o == null ? defaultValue : o;
        } else {
            return o;
        }
    }

    /**
     * 取得方法的特殊参数配置
     *
     * @param methodName
     *         方法名
     * @param paramKey
     *         参数关键字
     * @param defaultValue
     *         默认值
     * @return 都找不到为defaultValue
     */
    protected int getIntMethodParam(String methodName, String paramKey, int defaultValue) {
        if (CommonUtils.isEmpty(configContext)) {
            return defaultValue;
        }
        Integer o = (Integer) configContext.get(buildmkey(methodName, paramKey));
        if (o == null) {
            o = (Integer) configContext.get(paramKey);
            return o == null ? defaultValue : o;
        } else {
            return o;
        }
    }

    /**
     * 取得方法的特殊参数配置
     *
     * @param methodName
     *         方法名
     * @param paramKey
     *         参数关键字
     * @return 都找不到为null
     */
    protected Object getMethodParam(String methodName, String paramKey) {
        if (CommonUtils.isEmpty(configContext)) {
            return null;
        }
        Object o = configContext.get(buildmkey(methodName, paramKey));
        return o == null ? configContext.get(paramKey) : o;
    }

    /**
     * Buildmkey string.
     *
     * @param methodName
     *         the method name
     * @param key
     *         the key
     * @return the string
     */
    private String buildmkey(String methodName, String key) {
        return BsoaConstants.HIDE_KEY_PREFIX + methodName + BsoaConstants.HIDE_KEY_PREFIX + key;
    }

    /**
     * 浅复制（字段值指向同一个对象）
     *
     * @return 过滤器对象
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new BsoaRuntimeException(22222, "Filter clone not supported!", e);
        }
    }
}
