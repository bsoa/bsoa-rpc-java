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
package io.bsoa.rpc.server.bsoa;

import java.net.InetSocketAddress;
import java.util.PriorityQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.base.Invoker;
import io.bsoa.rpc.common.utils.NetUtils;
import io.bsoa.rpc.context.BsoaContext;
import io.bsoa.rpc.context.RpcContext;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.message.HeadKey;
import io.bsoa.rpc.message.MessageBuilder;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.protocol.Protocol;
import io.bsoa.rpc.protocol.ProtocolFactory;
import io.bsoa.rpc.server.AbstractTask;
import io.bsoa.rpc.server.InvokerHolder;
import io.bsoa.rpc.transport.AbstractByteBuf;
import io.bsoa.rpc.transport.AbstractChannel;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/1/1 20:30. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class BsoaTask extends AbstractTask {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(BsoaTask.class);

    private final BsoaServerHandler serverHandler;

    private final RpcRequest msg;

    private final AbstractChannel channel;

    protected BsoaTask(BsoaServerHandler serverHandler, RpcRequest msg, AbstractChannel channel, int priority) {
        this.serverHandler = serverHandler;
        this.msg = msg;
        this.channel = channel;
        this.priority = priority;
    }

    @Override
    public void run() {

        AbstractByteBuf byteBuf = msg.getByteBuf();
        String interfaceName = null;
        String methodName = null;
        String tags = null;
        InetSocketAddress remoteAddress = null;
        InetSocketAddress localAddress = null;
        try {
            long now = BsoaContext.now();
            Integer timeout = (Integer) msg.getHeader(HeadKey.TIMEOUT.getCode());
            if (timeout != null && BsoaContext.now() - msg.getReceiveTime() > timeout) { // 客户端已经超时的请求直接丢弃
                LOGGER.warn("[JSF-23008]Discard request cause by timeout after receive the msg: {}", msg.getMessageId());
                return;
            }

            remoteAddress = channel.getRemoteAddress();
            localAddress = channel.getLocalAddress();

            // decode body
            Protocol protocol = ProtocolFactory.getProtocol(msg.getProtocolType());
            protocol.decoder().decodeBody(byteBuf, msg);

            interfaceName = msg.getInterfaceName();
            methodName = msg.getMethodName();
            tags = msg.getTags();

//            //AUTH check for blacklist/whitelist
//            if (!ServerAuthHelper.isValid(className, aliasName, NetUtils.toIpString(remoteAddress))) {
//                throw new RpcException(msg.getMsgHeader(),
//                        "[JSF-23007]Fail to pass the server auth check in server: " + localAddress
//                                + ", May be your host in blacklist of server");
//            }

//            if (CallbackUtil.isCallbackRegister(className, methodName)) {
//                CallbackUtil.msgHandle(msg, channel);
//            }
            String key = InvokerHolder.buildKey(interfaceName, tags);
            Invoker invoker = serverHandler.getInvoker(key);
            if (invoker == null) {
                throw new BsoaRpcException(222222, "[JSF-23006]Cannot found invoker of "
                        + key
                        + " in channel:" + NetUtils.channelToString(remoteAddress, localAddress)
                        + ", current invokers is " + serverHandler.getAllOwnInvoker().keySet());
            }
            RpcContext.getContext().setRemoteAddress(remoteAddress);
            RpcContext.getContext().setLocalAddress(localAddress);
            RpcResponse response = invoker.invoke(msg); // 执行调用，包括过滤器链

            // 如果是
            methodName = msg.getMethodName(); // generic的方法名为$invoke已经变成了真正方法名

            AbstractByteBuf responseByteBuf = channel.getByteBuf();
            protocol.encoder().encodeAll(response, responseByteBuf);

//            // 判断是否启动监控，如果启动则运行
//            if (!CommonUtils.isFalse((String) invocation.getAttachment(BsoaConstants.INTERNAL_KEY_MONITOR))
//                    && MonitorFactory.isMonitorOpen(className, methodName)) {
//                String ip = NetUtils.toIpString(localAddress);
//                int port = localAddress.getPort();
//                Monitor monitor = MonitorFactory.getMonitor(MonitorFactory.MONITOR_PROVIDER_METRIC,
//                        className, methodName, ip, port);
//                if (monitor != null) { // 需要记录日志
//                    boolean iserror = rpcResponse.isError();
//                    invocation.addAttachment(BsoaConstants.INTERNAL_KEY_INPUT, msg.getMsgHeader().getLength());
//                    // 报文长度+magiccode(2) + totallength(4)
//                    invocation.addAttachment(BsoaConstants.INTERNAL_KEY_OUTPUT, buf.readableBytes() + 6);
//                    invocation.addAttachment(BsoaConstants.INTERNAL_KEY_RESULT, !iserror);
//                    invocation.addAttachment(BsoaConstants.INTERNAL_KEY_PROTOCOL, ProtocolType.jsf.value() + "");
//                    if (iserror) { // 失败
//                        monitor.recordException(invocation, rpcResponse.getException());
//                    } else { // 成功
//                        monitor.recordInvoked(invocation);
//                    }
//                }
//            }

            if (timeout != null && BsoaContext.now() - msg.getReceiveTime() > timeout) { // 客户端已经超时的响应直接丢弃
                LOGGER.warn("[JSF-23008]Discard send response cause by " +
                        "timeout after receive the msg: {}", msg.getMessageId());
                responseByteBuf.release();
                return;
            }
            channel.writeAndFlush(responseByteBuf);
        } catch (Throwable e) {
            LOGGER.error("[JSF-23011]Error when run JSFTask, request to " + interfaceName
                    + "/" + methodName + "/" + tags + ", error: " + e.getMessage()
                    + (channel != null ? ", channel: "
                    + NetUtils.channelToString(remoteAddress, localAddress) : ""), e);
            RpcResponse response = MessageBuilder.buildRpcResponse(msg);
            response.setException(e);
            channel.writeAndFlush(response);
        } finally {
            //release the byteBuf here
            if (byteBuf != null) {
                byteBuf.release();
            }
        }
    }

    @Override
    public String toString() {
        return "BsoaTask[m:" + msg + ",p:" + priority + "]";
    }

    public static void main(String[] args) {
        PriorityQueue set = new PriorityQueue();
        set.add(new BsoaTask(null, null, null, 10));
        set.add(new BsoaTask(null, null, null, -99));
        set.add(new BsoaTask(null, null, null, 99));
        set.add(new BsoaTask(null, new RpcRequest(), null, 1));
        set.add(new BsoaTask(null, null, null, -10));
        set.add(new BsoaTask(null, null, null, 0));
        set.add(new BsoaTask(null, null, null, 0));
        set.add(new BsoaTask(null, null, null, -1));

        Object o;
        while ((o = set.poll()) != null) {
            System.out.println(o);
        }
    }
}
