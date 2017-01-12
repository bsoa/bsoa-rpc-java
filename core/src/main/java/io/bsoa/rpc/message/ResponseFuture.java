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
package io.bsoa.rpc.message;

import java.util.List;
import java.util.concurrent.Future;

import io.bsoa.rpc.listener.ResponseListener;

/**
 * <p>响应Future，可以调用get方法进行获取响应，也可以注入监听器，有结果或者都会通知</p>
 *
 * Created by zhangg on 2016/12/15 23:09. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
//@Extensible(singleton = false)
public interface ResponseFuture<V> extends Future<V> {

    /**
     * 增加多个响应监听器
     *
     * @param responseListeners 多个响应监听器
     * @return 对象本身
     */
    ResponseFuture addListeners(List<ResponseListener> responseListeners);

    /**
     * 增加单个响应监听器
     *
     * @param responseListener 多个响应监听器
     * @return 对象本身
     */
    ResponseFuture addListener(ResponseListener responseListener);

}
