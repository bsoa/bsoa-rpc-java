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
package io.bsoa.rpc;

import io.bsoa.rpc.listener.ResponseListener;

/**
 * Created by zhangg on 2016/7/17 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public interface GenericService {

    /**
     * 泛化调用
     *
     * @param method         方法名
     * @param parameterTypes 参数类型
     * @param args           参数列表
     * @return 返回值
     */
    public Object $invoke(String method, String[] parameterTypes, Object[] args);

    /**
     * 异步回调的泛化调用
     *
     * @param method         方法名
     * @param parameterTypes 参数类型
     * @param args           参数列表
     * @param listener       结果listener
     */
    public void $asyncInvoke(String method, String[] parameterTypes, Object[] args, ResponseListener listener);
}
