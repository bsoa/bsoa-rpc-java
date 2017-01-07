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
package io.bsoa.rpc.client;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.context.CallbackContext;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.listener.ResponseFuture;
import io.bsoa.rpc.listener.ResponseListener;
import io.bsoa.rpc.listener.ResultListener;
import io.bsoa.rpc.message.RpcResponse;

/**
 *
 *
 * Created by zhangg on 2016/7/17 15:32.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class AsyncResultListener implements ResultListener {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(AsyncResultListener.class);

    /**
     * The Listeners.
     */
    private List<ResponseListener> listeners;

    /**
     * Operation complete.
     *
     * @param future
     *         the future
     * @return the boolean
     * @see ClientProxyInvoker#notifyResponseListener(String, io.bsoa.rpc.message.RpcResponse)
     */
    @Override
    public boolean operationComplete(ResponseFuture future) {
        if (listeners == null || listeners.isEmpty()) {
            return false;
        }
        RpcResponse response = null;
        try {
            response = (RpcResponse) future.get();
        } catch (Exception e) {
            throw new BsoaRpcException(22222, e);
        }
        if (response == null) {
            return true;
        }
        RpcResponse finalResponse = response;
        CallbackContext.getCallbackThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("call async listener");
                }
                if (finalResponse.hasError()) {
                    Throwable responseException = finalResponse.getException();
                    for (ResponseListener responseListener : listeners) {
                        try {
                            responseListener.catchException(responseException);
                        } catch (Exception e) {
                            LOGGER.warn("notify response listener error", e);
                        }
                    }
                } else {
                    Object result = finalResponse.getReturnData();
                    for (ResponseListener responseListener : listeners) {
                        try {
                            responseListener.handleResult(result);
                        } catch (Exception e) {
                            LOGGER.warn("notify response listener error", e);
                        }
                    }
                }
            }
        });
        return true;
    }

    /**
     * Add response listener.
     *
     * @param responseListener
     *         the response listener
     */
    public void addResponseListener(ResponseListener responseListener) {
        if (responseListener == null) {
            return;
        }
        if (listeners == null) {
            listeners = new ArrayList<ResponseListener>();
        }
        listeners.add(responseListener);
    }

    /**
     * Add response listeners.
     *
     * @param responseListeners
     *         the response listeners
     */
    public void addResponseListeners(List<ResponseListener> responseListeners) {
        if (responseListeners == null) {
            return;
        }
        if (listeners == null) {
            listeners = new ArrayList<ResponseListener>();
        }
        listeners.addAll(responseListeners);
    }

}