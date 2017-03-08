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
package io.bsoa.rpc.common;

/**
 * <p>配置项关键字</p>
 * <p>
 * Created by zhangg on 2017/2/11 13:15. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class BsoaOptions {
    /**
     * 决定本配置文件的加载顺序，越大越往后加载
     */
    public static final String BSOA_CFG_ORDER = "bsoa.config.order";
    /**
     * 应用Id
     */
    public static final String APP_ID = "app.id";
    /**
     * 应用名称
     */
    public static final String APP_NAME = "app.name";
    /**
     * 应用实例Id
     */
    public static final String INSTANCE_ID = "instance.id";
    /**
     * 系统cpu核数
     */
    public static final String SYSTEM_CPU_CORES = "system.cpu.cores";
    /**
     * 扩展点加载的路径
     */
    public static final String EXTENSION_LOAD_PATH = "extension.load.path";
    /**
     * Consumer共享心跳重连线程？ FIXME
     */
    public static final String CONSUMER_SHARE_RECONNECT_THREAD = "consumer.share.reconnect.thread";
    /**
     * 是否跨接口的长连接复用
     */
    public static final String TRANSPORT_CONNECTION_REUSE = "transport.connection.reuse";
    /**
     * 默认服务提供者启动器
     */
    public static final String DEFAULT_PROVIDER_BOOTSTRAP = "default.provider.bootstrap";
    /**
     * 默认服务端调用者启动器
     */
    public static final String DEFAULT_CONSUMER_BOOTSTRAP = "default.consumer.bootstrap";
    /**
     * 默认服务tag
     */
    public static final String DEFAULT_TAGS = "default.tags";
    /**
     * 默认注册中心
     */
    public static final String DEFAULT_REGISTRY = "default.registry";
    /**
     * 默认协议
     */
    public static final String DEFAULT_PROTOCOL = "default.protocol";
    /**
     * 默认序列化
     */
    public static final String DEFAULT_SERIALIZATION = "default.serialization";
    /**
     * 默认代理类型
     */
    public static final String DEFAULT_PROXY = "default.proxy";
    /**
     * 默认字符集 utf-8
     */
    public static final String DEFAULT_CHARSET = "default.charset";
    /**
     * 默认传输层
     */
    public static final String DEFAULT_TRANSPORT = "default.transport";
    /**
     * 默认压缩算法
     */
    public static final String DEFAULT_COMPRESS = "default.compress";

    /**
     * bsoa协议是否使用头部映射
     */
    public static final String BSOA_NEGOTIATION_ENABLE = "bsoa.negotiation.enable";
    /**
     * bsoa协议是否使用头部映射
     */
    public static final String BSOA_HEAD_REF_ENABLE = "bsoa.head.ref.enable";

    /**
     * 默认连注册中心的超时时间
     */
    public static final String REGISTRY_CONNECT_TIMEOUT = "registry.connect.timeout";
    /**
     * 注册中心等待结果的超时时间
     */
    public static final String REGISTRY_DISCONNECT_TIMEOUT = "registry.disconnect.timeout";
    /**
     * 注册中心调用超时时间
     */
    public static final String REGISTRY_INVOKE_TIMEOUT = "registry.invoke.timeout";
    /**
     * 注册中心心跳发送间隔
     */
    public static final String REGISTRY_HEARTBEAT_PERIOD = "registry.heartbeat.period";
    /**
     * 注册中心重建连接的间隔
     */
    public static final String REGISTRY_RECONNECT_PERIOD = "registry.reconnect.period";
    /**
     * 是否批量操作
     */
    public static final String REGISTRY_BATCH = "registry.batch";
    /**
     * 批量注册的大小
     */
    public static final String REGISTRY_BATCH_SIZE = "registry.batch.size";
    /**
     * 默认绑定网卡
     */
    public static final String SERVER_HOST = "server.host";
    /**
     * 默认启动端口，包括不配置或者随机，都从此端口开始计算
     */
    public static final String SERVER_PORT_START = "server.port.start";
    /**
     * 默认启动端口，包括不配置或者随机，都从此端口开始计算
     */
    public static final String SERVER_PORT_END = "server.port.end";
    /**
     * 默认发布路径 "/"
     */
    public static final String SERVER_CONTEXT_PATH = "server.context.path";
    /**
     * 默认io线程大小，推荐自动设置
     */
    public static final String SERVER_IOTHREADS = "server.ioThreads";
    /**
     * 默认服务端业务线程池类型
     */
    public static final String SERVER_POOL_TYPE = "server.pool.type";
    /**
     * 默认服务端业务线程池最小
     */
    public static final String SERVER_POOL_CORE = "server.pool.core";
    /**
     * 默认服务端业务线程池最大
     */
    public static final String SERVER_POOL_MAX = "server.pool.max";
    /**
     * 是否允许telnet，针对自定义协议
     */
    public static final String SERVER_TELNET = "server.telnet";
    /**
     * 默认服务端业务线程池队列类型
     */
    public static final String SERVER_POOL_QUEUE_TYPE = "server.pool.queue.type";
    /**
     * 默认服务端业务线程池队列
     */
    public static final String SERVER_POOL_QUEUE = "server.pool.queue";
    /**
     * 默认服务端业务线程池回收时间
     */
    public static final String SERVER_POOL_ALIVETIME = "server.pool.aliveTime";
    /**
     * 最大支持长连接
     */
    public static final String SERVER_ACCEPTS = "server.accepts";
    /**
     * 是否启动epoll
     */
    public static final String SERVER_EPOLL = "server.epoll";
    /**
     * 是否hold住端口，true的话随主线程退出而退出，false的话则要主动退出
     */
    public static final String SERVER_DAEMON = "server.daemon";
    /**
     * 默认服务是否注册
     */
    public static final String SERVICE_REGISTER = "service.register";
    /**
     * 默认服务是否订阅
     */
    public static final String SERVICE_SUBSCRIBE = "service.subscribe";
    /**
     * 默认权重
     */
    public static final String PROVIDER_WEIGHT = "provider.weight";
    /**
     * 默认服务启动延迟
     */
    public static final String PROVIDER_DELAY = "provider.delay";
    /**
     * 默认发布方法
     */
    public static final String PROVIDER_INCLUDE = "provider.include";
    /**
     * 默认不发布方法
     */
    public static final String PROVIDER_EXCLUDE = "provider.exclude";
    /**
     * 是否动态注册
     */
    public static final String PROVIDER_DYNAMIC = "provider.dynamic";
    /**
     * 接口优先级
     */
    public static final String PROVIDER_PRIORITY = "provider.priority";
    /**
     * 服务端调用超时（不打断执行）
     */
    public static final String PROVIDER_INVOKE_TIMEOUT = "provider.invoke.timeout";
    /**
     * 接口下每方法的最大可并行执行请求数
     */
    public static final String PROVIDER_CONCURRENTS = "provider.concurrents";
    /**
     * 默认集群策略
     */
    public static final String CONSUMER_CLUSTER = "consumer.cluster";
    /**
     * 默认负载均衡算法
     */
    public static final String CONSUMER_CONNECTION_HOLDER = "consumer.connectionHolder";
    /**
     * 默认负载均衡算法
     */
    public static final String CONSUMER_LOAD_BALANCER = "consumer.loadBalancer";
    /**
     * 默认失败重试次数
     */
    public static final String CONSUMER_RETRIES = "consumer.retries";
    /**
     * 默认是否异步
     */
    public static final String CONSUMER_ASYNC = "consumer.async";
    /**
     * 默认不延迟加载
     */
    public static final String CONSUMER_LAZY = "consumer.lazy";
    /**
     * 默认粘滞连接
     */
    public static final String CONSUMER_STICKY = "consumer.sticky";
    /**
     * 是否jvm内部调用（provider和consumer配置在同一个jvm内，则走本地jvm内部，不走远程）
     */
    public static final String CONSUMER_INJVM = "consumer.inJVM";
    /**
     * 是否强依赖（即没有服务节点就启动失败）
     */
    public static final String CONSUMER_CHECK = "consumer.check";
    /**
     * 是否单向调用（不关心结果，服务端不响应）
     */
    public static final String CONSUMER_ONEWAY = "consumer.oneWay";
    /**
     * 接口下每方法的最大可并行执行请求数
     */
    public static final String CONSUMER_CONCURRENTS = "consumer.concurrents";
    /**
     * 默认一个ip端口建立的长连接数量
     */
    public static final String CONSUMER_CONNECTION = "consumer.connection";
    /**
     * 默认consumer连provider超时时间
     */
    public static final String CONSUMER_CONNECT_TIMEOUT = "consumer.connect.timeout";
    /**
     * 默认consumer断开时等待结果的超时时间
     */
    public static final String CONSUMER_DISCONNECT_TIMEOUT = "consumer.disconnect.timeout";
    /**
     * 默认consumer调用provider超时时间
     */
    public static final String CONSUMER_INVOKE_TIMEOUT = "consumer.invoke.timeout";
    /**
     * Consumer给Provider发心跳的间隔
     */
    public static final String CONSUMER_HEARTBEAT_PERIOD = "consumer.heartbeat.period";
    /**
     * Consumer给Provider重连的间隔
     */
    public static final String CONSUMER_RECONNECT_PERIOD = "consumer.reconnect.period";
    /**
     * 本地缓存的StreamObserver最大实例数
     */
    public static final String STREAM_OBSERVER_MAX_SIZE = "stream.observer.max.size";
    /**
     * 本地缓存的Callback最大实例数
     */
    public static final String CALLBACK_MAX_SIZE = "callback.max.size";
    /**
     * 默认回调线程池类型
     */
    public static final String ASYNC_POOL_TYPE = "async.pool.type";
    /**
     * 默认回调线程池最小
     */
    public static final String ASYNC_POOL_CORE = "async.pool.core";
    /**
     * 默认回调线程池最大
     */
    public static final String ASYNC_POOL_MAX = "async.pool.max";
    /**
     * 默认回调线程池队列
     */
    public static final String ASYNC_POOL_QUEUE = "async.pool.queue";
    /**
     * 默认回调线程池回收时间
     */
    public static final String ASYNC_POOL_TIME = "async.pool.time";
    /**
     * 默认开启epoll？
     */
    public static final String TRANSPORT_USE_EPOLL = "transport.use.epoll";
    /**
     * 默认服务端 数据包限制
     */
    public static final String TRANSPORT_PAYLOAD_MAX = "transport.payload.max";
    /**
     * 默认IO的buffer大小
     */
    public static final String TRANSPORT_BUFFER_SIZE = "transport.buffer.size";
    /**
     * 最大IO的buffer大小
     */
    public static final String TRANSPORT_BUFFER_MAX = "transport.buffer.max";
    /**
     * 最小IO的buffer大小
     */
    public static final String TRANSPORT_BUFFER_MIN = "transport.buffer.min";
    /**
     * 客户端IO线程池
     */
    public static final String TRANSPORT_CLIENT_IO_THREADS = "transport.client.io.threads";
    /**
     * 客户端IO 比例：用户在代码中使用到了Runnable和ScheduledFutureTask，请合理设置ioRatio的比例，
     * 通过NioEventLoop的setIoRatio(int ioRatio)方法可以设置该值，默认值为50，即I/O操作和用户自定义任务的执行时间比为1：1
     */
    public static final String TRANSPORT_CLIENT_IO_RATIO = "transport.client.io.ratio";
    /**
     * 客户端IO 比例：用户在代码中使用到了Runnable和ScheduledFutureTask，请合理设置ioRatio的比例，
     * 通过NioEventLoop的setIoRatio(int ioRatio)方法可以设置该值，默认值为50，即I/O操作和用户自定义任务的执行时间比为1：1
     */
    public static final String TRANSPORT_SERVER_IO_RATIO = "transport.server.io.ratio";
    /**
     * 连接重用
     */
    public static final String TRANSPORT_SERVER_BACKLOG = "transport.server.backlog";
    /**
     * 连接重用
     */
    public static final String TRANSPORT_SERVER_REUSE_ADDR = "transport.server.reuseAddr";
    /**
     * 保存长连接
     */
    public static final String TRANSPORT_SERVER_KEEPALIVE = "transport.server.keepAlive";
    /**
     * 无延迟
     */
    public static final String TRANSPORT_SERVER_TCPNODELAY = "transport.server.tcpNoDelay";
    /**
     * 服务端boss线程数
     */
    public static final String TRANSPORT_SERVER_BOSS_THREADS = "transport.server.boss.threads";
    /**
     * 服务端IO线程数
     */
    public static final String TRANSPORT_SERVER_IO_THREADS = "transport.server.io.threads";
    /**
     * 线程方法模型
     */
    public static final String TRANSPORT_SERVER_DISPATCHER = "transport.server.dispatcher";
    /**
     * 是否一个端口支持多协议
     */
    public static final String TRANSPORT_SERVER_PROTOCOL_ADAPTIVE = "transport.server.protocol.adaptive";
    /**
     * 是否开启压缩
     */
    public static final String COMPRESS_OPEN = "compress.open";
    /**
     * 开启压缩的大小基线
     */
    public static final String COMPRESS_SIZE_BASELINE = "compress.size.baseline";
}
