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
package io.bsoa.rpc.serialization.hessian;

import io.bsoa.rpc.common.utils.ClassLoaderUtils;
import io.bsoa.rpc.serialization.hessian.io.SerializerFactory;

/**
 * <p>保存一些新旧类的映射关系<br>
 * 例如旧的发来com.xxx.Obj，需要拿com.yyy.Obj去解析，则可以使用此类<br></p>
 * <p>
 * Created by zhangg on 2016/12/24 21:09. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class HessianSerializerFactory extends SerializerFactory {

    protected static final SerializerFactory SERIALIZER_FACTORY = new HessianSerializerFactory();

    @Override
    public ClassLoader getClassLoader() {
        return ClassLoaderUtils.getClassLoader(HessianSerializerFactory.class);
    }

}