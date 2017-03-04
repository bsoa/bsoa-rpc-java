/*
 * Copyright 2016 The BSOA Project
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
package io.bsoa.rpc.serialization.hessian;

/**
 * 修改记录：
 * 1. 增加io.bsoa.rpc.serialization.hessian.HessianConstants类， 保存映射关系
 * 2. 修改io.bsoa.rpc.serialization.hessian.io.Hessian2Input 的 readObjectDefinition 方法读取映射关系
 * 3. 优化io.bsoa.rpc.serialization.hessian.io.SerializerFactory里的HashMap 变成 ConcurrentHashMap
 * 4. 修改io.bsoa.rpc.serialization.hessian.io.Hessian2Input 的 parseChar方法有bug  修复
 */