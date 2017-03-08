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
package io.bsoa.rpc.context;

import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.config.ParameterConfig;
import io.bsoa.rpc.message.ResponseFuture;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by zhangg on 2016/7/14 21:02.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class RpcContext {

    /**
     * The constant LOCAL.
     */
    private static final ThreadLocal<RpcContext> LOCAL = new ThreadLocal<RpcContext>() {
        @Override
        protected RpcContext initialValue() {
            return new RpcContext();
        }
    };

    /**
     * get context.
     *
     * @return context context
     */
    public static RpcContext getContext() {
        return LOCAL.get();
    }

    /**
     * remove context.
     */
    public static void removeContext() {
        LOCAL.remove();
    }

    /**
     * The Future.
     */
    private ResponseFuture<?> future;

    /**
     * The Local address.
     */
    private InetSocketAddress localAddress;

    /**
     * The Remote address.
     */
    private InetSocketAddress remoteAddress;

    /**
     * The Attachments.
     */
    private final Map<String, Object> attachments = new HashMap<String, Object>();

    /**
     * Instantiates a new Rpc context.
     */
    protected RpcContext() {
    }

    /**
     * The Provider side.
     */
    private Boolean providerSide;

    /**
     * Is provider side.
     *
     * @return the boolean
     */
    public boolean isProviderSide() {
        return providerSide != null && providerSide;
    }

    /**
     * Sets provider side.
     *
     * @param isProviderSide the is provider side
     * @return the provider side
     */
    public RpcContext setProviderSide(boolean isProviderSide) {
        this.providerSide = isProviderSide;
        return this;
    }

    /**
     * Is consumer side.
     *
     * @return the boolean
     */
    public boolean isConsumerSide() {
        return providerSide != null && !providerSide;
    }

    /**
     * get future.
     *
     * @param <T>
     * @return future future
     */
    @SuppressWarnings("unchecked")
    public <T> ResponseFuture<T> getFuture() {
        return (ResponseFuture<T>) future;
    }

    /**
     * set future.
     *
     * @param future the future
     * @return RpcContext
     */
    public RpcContext setFuture(ResponseFuture<?> future) {
        this.future = future;
        return this;
    }

    /*
    private String methodName;

    private Class<?>[] parameterTypes;

    private Object[] arguments;
    */

    /**
     * get method name.
     *
     * @return method name.
    public String getMethodName() {
    return methodName;
    }

    public void setMethodName(String methodName) {
    this.methodName = methodName;
    }

     *
     * get parameter types.
     *
     * @serial public Class<?>[] getParameterTypes() {
    return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
    this.parameterTypes = parameterTypes;
    }

     *
     * get arguments.
     *
     * @return arguments.

    public Object[] getArguments() {
    return arguments;
    }

    public void setArguments(Object[] arguments) {
    this.arguments = arguments;
    }*/

    /**
     * set local address.
     *
     * @param address the address
     * @return context local address
     */
    public RpcContext setLocalAddress(InetSocketAddress address) {
        this.localAddress = address;
        return this;
    }

    /**
     * set local address.
     *
     * @param host the host
     * @param port the port
     * @return context local address
     */
    public RpcContext setLocalAddress(String host, int port) {
        if (port < 0) {
            port = 0;
        }
        this.localAddress = InetSocketAddress.createUnresolved(host, port);
        return this;
    }

    /**
     * 本地地址InetSocketAddress
     *
     * @return local address
     */
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    /**
     * set remote address.
     *
     * @param address the address
     * @return context remote address
     */
    public RpcContext setRemoteAddress(InetSocketAddress address) {
        this.remoteAddress = address;
        return this;
    }

    /**
     * set remote address.
     *
     * @param host the host
     * @param port the port
     * @return context remote address
     */
    public RpcContext setRemoteAddress(String host, int port) {
        if (host == null) {
            return this;
        }
        if (port < 0 || port > 0xFFFF) {
            port = 0;
        }
        // 提前检查是否为空，防止createUnresolved抛出异常，损耗性能
        this.remoteAddress = InetSocketAddress.createUnresolved(host, port);
        return this;
    }

    /**
     * 远程地址InetSocketAddress
     *
     * @return remote address
     */
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * 远程IP地址
     *
     * @return remote host name
     */
    public String getRemoteHostName() {
        return NetUtils.toIpString(remoteAddress);
    }


    /**
     * get attachment.
     *
     * @param key the key
     * @return attachment attachment
     */
    public Object getAttachment(String key) {
        return attachments.get(key);
    }

    /**
     * set attachment.
     *
     * @param key   the key
     * @param value the value
     * @return context attachment
     */
    public RpcContext setAttachment(String key, Object value) {
        if (ParameterConfig.isInternalParamKey(key)) {
            throw new IllegalArgumentException("key can not start with" + BsoaConstants.INTERNAL_KEY_PREFIX);
        }
        if (value == null) {
            attachments.remove(key);
        } else {
            attachments.put(key, value);
        }
        return this;
    }

    /**
     * remove attachment.
     *
     * @param key the key
     * @return context rpc context
     */
    public RpcContext removeAttachment(String key) {
        attachments.remove(key);
        return this;
    }

    /**
     * get attachments.
     *
     * @return attachments attachments
     */
    public Map<String, Object> getAttachments() {
        return attachments;
    }

    /**
     * key不能以点和下划线开头
     *
     * @param attachments the attachments
     * @return context attachments
     */
    public RpcContext setAttachments(Map<String, Object> attachments) {
        if (attachments != null && attachments.size() > 0) {
            for (Map.Entry<String, Object> entry : attachments.entrySet()) {
                String key = entry.getKey();
                if (ParameterConfig.isValidParamKey(key)) {
                    this.attachments.put(key, entry.getValue());
                }
            }
        }
        return this;
    }

    /**
     * Clear attachments.
     */
    public RpcContext clearAttachments() {
        if (attachments != null && attachments.size() > 0) {
            Iterator<Map.Entry<String, Object>> it = attachments.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> entry = it.next();
                String key = entry.getKey();
                if (!BsoaConstants.HIDDEN_KEY_SESSION.equals(key)) { // session不清理
                    it.remove();
                }
            }
        }
        return this;
    }

    /**
     * 当前组
     */
    private String tags;

    /**
     * Gets tags.
     *
     * @return the tags
     */
    public String getTags() {
        return tags;
    }

    /**
     * Sets tags.
     *
     * @param tags the tags
     * @return RpcContext
     */
    public RpcContext setTags(String tags) {
        this.tags = tags;
        return this;
    }

    /**
     * 设置Session的属性值<br>
     * 注：Session是一种特殊的 隐式传参，客户端线程不会主动删除，需要用户自己写代码清理
     *
     * @param key   属性
     * @param value 值
     * @return 本对象
     * @see #getSessionAttribute
     * @see #clearSession()
     */
    public RpcContext setSessionAttribute(String key, Object value) {
        Map<String, Object> session = (Map<String, Object>) attachments.get(BsoaConstants.HIDDEN_KEY_SESSION);
        if (session == null) {
            session = new HashMap<String, Object>();
            attachments.put(BsoaConstants.HIDDEN_KEY_SESSION, session);
        }
        session.put(key, value);
        return this;
    }

    /**
     * 查询Session的属性值<br>
     * 注：Session是一种特殊的 隐式传参，JSF客户端不会主动删除，需要用户自己写代码清理
     *
     * @param key 属性
     * @return 值
     * @see #setSessionAttribute
     * @see #clearSession()
     */
    public Object getSessionAttribute(String key) {
        Map<String, Object> session = (Map<String, Object>) attachments.get(BsoaConstants.HIDDEN_KEY_SESSION);
        return session == null ? null : session.get(key);
    }

    /**
     * 设置session<br>
     * 注：Session是一种特殊的 隐式传参，JSF客户端不会主动删除，需要用户自己写代码清理
     *
     * @param session session属性map
     * @return 本对象
     */
    public RpcContext setSession(Map<String, Object> session) {
        return setAttachment(BsoaConstants.HIDDEN_KEY_SESSION, session);
    }

    /**
     * 得到session<br>
     * 注：Session是一种特殊的 隐式传参，JSF客户端不会主动删除，需要用户自己写代码清理
     *
     * @return session属性map
     */
    public Map<String, Object> getSession() {
        return (Map<String, Object>) attachments.get(BsoaConstants.HIDDEN_KEY_SESSION);
    }

    /**
     * 删除session
     *
     * @return 本对象
     */
    public RpcContext clearSession() {
        return removeAttachment(BsoaConstants.HIDDEN_KEY_SESSION);
    }
}