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
package io.bsoa.rpc.codec;

import io.bsoa.rpc.ext.Extensible;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/24 22:13. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extensible(coded = true)
public interface Compressor {
    /**
     * 字节数组压缩
     *
     * @param src 未压缩的字节数组
     * @return 压缩后的字节数组
     */
    public byte[] compress(byte[] src);

    /**
     * 字节数组解压缩
     *
     * @param src 压缩后的源字节数组
     * @return 解压缩后的字节数组
     */
    public byte[] deCompress(byte[] src);
}
