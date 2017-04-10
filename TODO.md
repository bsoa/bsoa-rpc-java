bulkheads  线程隔离
ReferenceCounted ??
自定义线程池
provider增加 uuid


=====transport========
建立长链接后进行匹配(包括版本,服务等)
是否跨接口长链接复用   ----->
长连接数 是否增加 -----》
是否开启调用优化(发ID代替接口方法)
 loghandler


========服务端======
1. 是否允许一个端口多个服务
2. 线程池可配置
3. 服务端是否检测tag


===异常=====
异常码, 异常区分来自服务端客户端等
多国语言



==== 请求内容=====
请求上线文RPCContext
隐式传参  HiddenParam
RPCSession 请求
Head扩展map


===filter====
是否检测服务端耗时

===client=====
可以配置是否一个线程池重试所有consumer的死亡节点


====task======
服务端是否检查请求超时时间（超过超时时间则丢弃不再执行）
服务端是否检查返回超时时间（超过超时时间则丢弃不再返回）

========telnet=======
是否独立管理端口
telnet命令扩展 telnet增加trace功能，debug功能
检测class类加载信息
debug模式
tp可以指定 监控 次数。 类似 jstat -gcutil

