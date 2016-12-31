/**
 * 为防止包冲突，将hessian打包进来，基于hessian-3.2.1
 */
package io.bsoa.rpc.serialization.hessian;

/**
修改记录：
1. 增加io.bsoa.rpc.serialization.hessian.HessianObjectMapping 类， 保存映射关系
2. 修改io.bsoa.rpc.serialization.hessian.io.Hessian2Input 的 readObjectDefinition 方法读取映射关系
3. 优化io.bsoa.rpc.serialization.hessian.io.SerializerFactory里的HashMap 变成 ConcurrentHashMap
4. 修改io.bsoa.rpc.serialization.hessian.io.Hessian2Input 的 parseChar方法有bug  修复
*/