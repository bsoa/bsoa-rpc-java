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
package io.bsoa.rpc.transport.netty;

import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.DateUtils;
import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.context.AsyncContext;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.invoke.StreamUtils;
import io.bsoa.rpc.listener.ResponseListener;
import io.bsoa.rpc.message.DecodableMessage;
import io.bsoa.rpc.message.ResponseFuture;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.protocol.Protocol;
import io.bsoa.rpc.protocol.ProtocolFactory;
import io.bsoa.rpc.transport.ClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/20 23:05. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class NettyMessageFuture<V> implements ResponseFuture<V> {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ResponseFuture.class);

    private short waiters;

    private static final String UNCANCELLABLE = "UNCANCELLABLE";

    private static final CauseHolder CANCELLATION_CAUSE_HOLDER = new CauseHolder(new CancellationException());

    /**
     * 结果监听器，在返回成功或者异常的时候需要通知
     */
    private volatile List<ResponseListener> listeners;

    private volatile Object result;

    private final int msgId;

    private final ClientTransport clientTransport;

    /**
     * 用户设置的超时时间
     */
    private final int timeout;
    /**
     * Future生成时间
     */
    private final long genTime = BsoaContext.now();
    /**
     * Future已发送时间
     */
    private volatile long sentTime;
    /**
     * Future完成的时间
     */
    private volatile long doneTime = 0L;
    /**
     * 是否同步调用，默认是
     */
    private boolean asyncCall;
    /**
     * 返回值是否为流式
     */
    private boolean streamReturn;

    public NettyMessageFuture(ClientTransport clientTransport, int msgId, int timeout) {
        this.clientTransport = clientTransport;
        this.msgId = msgId;
        this.timeout = timeout;
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean res = this.cancle0(mayInterruptIfRunning);
        notifyListeners(this.listeners);
        return res;
    }

    private boolean cancle0(boolean mayInterruptIfRunning) {
        Object result = this.result;
        if (isDone0(result) || result == UNCANCELLABLE) {
            return false;
        }
        synchronized (this) {
            // Allow only once.
            result = this.result;
            if (isDone0(result) || result == UNCANCELLABLE) {
                return false;
            }
            this.result = CANCELLATION_CAUSE_HOLDER;
            this.setDoneTime();
            if (hasWaiters()) {
                notifyAll();
            }
        }
        return true;
    }

    @Override
    public boolean isCancelled() {
        return this.result == CANCELLATION_CAUSE_HOLDER;
    }

    @Override
    public boolean isDone() {
        return isDone0(result);
    }

    private static boolean isDone0(Object result) {
        return result != null && result != UNCANCELLABLE;
    }

    @Override
    public V get() throws InterruptedException {
        return get(timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException {
        timeout = unit.toMillis(timeout); // 转为毫秒
        long remaintime = timeout - (sentTime - genTime); // 剩余时间
        if (remaintime <= 0) { // 没有剩余时间不等待
            if (isDone()) { // 直接看是否已经返回
                return getNow();
            }
        } else { // 等待剩余时间
            if (await(remaintime, TimeUnit.MILLISECONDS)) {
                return getNow();
            }
        }
        this.setDoneTime();
        throw clientTimeoutException(false);
    }

    /**
     * 构建超时异常
     *
     * @param scan 是否扫描线程
     * @return 异常ClientTimeoutException
     */
    protected BsoaRpcException clientTimeoutException(boolean scan) {
        Date now = new Date();
        String errorMsg = (sentTime > 0 ? "[22110]Waiting provider return response timeout"
                : "[22111]Consumer send request timeout")
                + ". Start time: " + DateUtils.dateToMillisStr(new Date(genTime))
                + ", End time: " + DateUtils.dateToMillisStr(now)
                + ((sentTime > 0 ?
                ", Client elapsed: " + (sentTime - genTime)
                        + "ms, Server elapsed: " + (now.getTime() - sentTime)
                : ", Client elapsed: " + (now.getTime() - genTime))
                + "ms, Timeout: " + timeout
                + "ms, MsgId: " + this.msgId
                + ", Channel: " + NetUtils.channelToString(
                        clientTransport.getChannel().localAddress(), clientTransport.getChannel().remoteAddress()))
                + (scan ? ", throws by scan thread" : ".");
        return new BsoaRpcException(22222, errorMsg);
    }

    public boolean isSuccess() {
        Object result = this.result;
        return result != null && !(result instanceof CauseHolder);
    }

    public Throwable cause() {
        Object result = this.result;
        if (result instanceof CauseHolder) {
            return ((CauseHolder) result).cause;
        }
        return null;
    }

    public V getNow() {
        Object result = this.result;
        if (result instanceof DecodableMessage) {
            DecodableMessage msg = (DecodableMessage) result;
            if (msg.getByteBuf() != null) {
                synchronized (this) {
                    if (msg.getByteBuf() != null) {
                        Protocol protocol = ProtocolFactory.getProtocol(msg.getProtocolType());
                        protocol.decoder().decodeBody(msg.getByteBuf(), msg);
                        msg.getByteBuf().release();
                        msg.setByteBuf(null);
                    }
                }
            }
            if (isStreamReturn() && msg instanceof RpcResponse) {
                StreamUtils.preMsgReceive((RpcResponse) msg, clientTransport);
            }
        }
//        } else if (result instanceof CauseHolder) { // 本地异常
//            Throwable e = ((CauseHolder) result).cause;
//            if (e instanceof RpcException) {
//                RpcException rpcException = (RpcException) e;
//                rpcException.setMsgHeader(header);
//                throw rpcException;
//            } else {
//                throw new RpcException(this.header, ((CauseHolder) result).cause);
//            }
//        }
        return (V) result;
    }

    public void releaseIfNeed() {
        this.releaseIfNeed0((V) result);
    }

    private synchronized void releaseIfNeed0(V result) {
//        if(result instanceof RpcResponse){
//            ByteBuf byteBuf = ((RpcResponse) result).getMsgBody();
//            if(byteBuf != null && byteBuf.refCnt() > 0 ){
//                byteBuf.release();
//            }
//        }
    }

    public void setStreamReturn(boolean streamReturn) {
        this.streamReturn = streamReturn;
    }

    public boolean isStreamReturn() {
        return streamReturn;
    }

    private static final class CauseHolder {
        final Throwable cause;

        private CauseHolder(Throwable cause) {
            this.cause = cause;
        }
    }

    public boolean await(long timeout, TimeUnit unit)
            throws InterruptedException {
        return await0(unit.toNanos(timeout), true);
    }

    private boolean await0(long timeoutNanos, boolean interruptable) throws InterruptedException {
        if (isDone()) {
            return true;
        }

        if (timeoutNanos <= 0) {
            return isDone();
        }

        if (interruptable && Thread.interrupted()) {
            throw new InterruptedException(toString());
        }

        long startTime = System.nanoTime();
        long waitTime = timeoutNanos;
        boolean interrupted = false;

        try {
            synchronized (this) {
                if (isDone()) {
                    return true;
                }

                if (waitTime <= 0) {
                    return isDone();
                }

                //checkDeadLock(); need this check?
                incWaiters();
                try {
                    for (; ; ) {
                        try {
                            wait(waitTime / 1000000, (int) (waitTime % 1000000));
                        } catch (InterruptedException e) {
                            if (interruptable) {
                                throw e;
                            } else {
                                interrupted = true;
                            }
                        }

                        if (isDone()) {
                            return true;
                        } else {
                            waitTime = timeoutNanos - (System.nanoTime() - startTime);
                            if (waitTime <= 0) {
                                return isDone();
                            }
                        }
                    }
                } finally {
                    decWaiters();
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }


    public NettyMessageFuture<V> setSuccess(V result) {
        if (this.isCancelled()) {
            this.releaseIfNeed0(result);
            return this;
        }
        if (setSuccess0(result)) {
            notifyListeners(listeners);
            return this;
        }
        throw new IllegalStateException("complete already: " + this);
    }

    private boolean setSuccess0(V result) {
        if (isDone()) {
            return false;
        }
        synchronized (this) {
            // Allow only once.
            if (isDone()) {
                return false;
            }
            if (this.result == null) {
                this.result = result;
                this.setDoneTime();
            }
            if (hasWaiters()) {
                notifyAll();
            }
        }
        return true;
    }


    public NettyMessageFuture<V> setFailure(Throwable cause) {
        if (this.isCancelled()) {
            this.releaseIfNeed();
            return this;
        }
        if (setFailure0(cause)) {
            notifyListeners(listeners);
            return this;
        }
        throw new IllegalStateException("complete already: " + this, cause);
    }

    private boolean setFailure0(Throwable cause) {
        if (isDone()) {
            return false;
        }

        synchronized (this) {
            // Allow only once.
            if (isDone()) {
                return false;
            }
            result = new CauseHolder(cause);
            this.setDoneTime();
            if (hasWaiters()) {
                notifyAll();
            }
        }
        return true;
    }

    @Override
    public ResponseFuture addListeners(List<ResponseListener> listeners) {
        if (CommonUtils.isEmpty(listeners)) {
            return this;
        }
        if (isDone()) { // 如过加的时候 直接已经执行完了，则立即触发结果
            notifyListeners(listeners);
            return this;
        }
        synchronized (this) {
            if (!isDone()) { // 如果没有，则排队（加锁）加入listener
                if (this.listeners == null) {
                    this.listeners = new ArrayList<>();
                }
                this.listeners.addAll(listeners);
                return this;
            }
        }
        // 可能排队过程中执行完了，立即触发结果
        notifyListeners(listeners);
        return this;
    }

    @Override
    public ResponseFuture addListener(ResponseListener listener) {
        if (listener == null) {
            return this;
        }
        if (isDone()) { // 如过加的时候 直接已经执行完了，则立即触发结果
            notifyListener(listener);
            return this;
        }
        synchronized (this) {
            if (!isDone()) { // 如果没有，则排队（加锁）加入listener
                if (this.listeners == null) {
                    this.listeners = new ArrayList<>();
                }
                this.listeners.add(listener);
                return this;
            }
        }
        // 可能排队过程中执行完了，立即触发结果
        notifyListener(listener);
        return this;
    }

    /*
     * 批量通知全部Listener
     */
    private void notifyListeners(List<ResponseListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            if (isAsyncCall() && !this.isCancelled()) {
                //主要是释放掉msgBody 的buf
                getNow();
            }
        } else {
            RpcResponse response = (RpcResponse) getNow();
            AsyncContext.getAsyncThreadPool().execute(() -> {
                if (response.hasError()) {
                    Throwable responseException = response.getException();
                    for (ResponseListener responseListener : listeners) {
                        try {
                            responseListener.onException(responseException);
                        } catch (Exception e) {
                            LOGGER.warn("notify response listener error", e);
                        }
                    }
                } else {
                    Object result1 = response.getReturnData();
                    for (ResponseListener responseListener : listeners) {
                        try {
                            responseListener.onResult(result1);
                        } catch (Exception e) {
                            LOGGER.warn("notify response listener error", e);
                        }
                    }
                }
            });
        }
    }

    /**
     * 单个通知Listener
     *
     * @param responseListener 单个Listener
     */
    private void notifyListener(final ResponseListener responseListener) {
        RpcResponse response = (RpcResponse) getNow();
        AsyncContext.getAsyncThreadPool().execute(() -> {
            if (response.hasError()) {
                Throwable responseException = response.getException();
                try {
                    responseListener.onException(responseException);
                } catch (Exception e) {
                    LOGGER.warn("notify response listener error", e);
                }
            } else {
                Object result1 = response.getReturnData();
                try {
                    responseListener.onResult(result1);
                } catch (Exception e) {
                    LOGGER.warn("notify response listener error", e);
                }
            }
        });
    }

    private boolean hasWaiters() {
        return waiters > 0;
    }

    private void incWaiters() {
        if (waiters == Short.MAX_VALUE) {
            throw new IllegalStateException("too many waiters: " + this);
        }
        waiters++;
    }

    private void decWaiters() {
        waiters--;
    }

    /**
     * 查看生成时间
     *
     * @return 生成时间
     */
    public long getGenTime() {
        return genTime;
    }

    /**
     * 查看超时时间
     *
     * @return 超时时间
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * 查看完成时间
     *
     * @return 完成时间
     */
    public long getDoneTime() {
        if (doneTime == 0l) {
            long remaintime = timeout - (sentTime - genTime); // 剩余时间
            if (remaintime <= 0) { // 没有剩余时间
                doneTime = BsoaContext.now();
            }
        }
        return doneTime;
    }

    private void setDoneTime() {
        if (doneTime == 0l) {
            doneTime = BsoaContext.now();
        }
    }

    /**
     * 设置已发送时间
     *
     * @param sentTime 已发送时间
     */
    public void setSentTime(long sentTime) {
        this.sentTime = sentTime;
    }

    /**
     * 是否异步调用
     *
     * @return 是否异步调用
     */
    public boolean isAsyncCall() {
        return asyncCall;
    }

    /**
     * 标记为异步调用
     *
     * @param asyncCall 是否异步调用
     */
    public void setAsyncCall(boolean asyncCall) {
        this.asyncCall = asyncCall;
    }

}
