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
package io.bsoa.rpc.invoke;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * <p>面向用户的流式请求监听器，使用结束请务必关闭Stream</p>
 * <p>
 * Created by zhangg on 2017/2/10 23:29. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@NotThreadSafe
public interface StreamObserver<V> {

    /**
     * 传输一段流对象（可调用多次）
     *
     * @param value 值
     */
    public void onValue(V value);

    /**
     * 传输完毕（和onError方法两者只可调一次）
     */
    public void onCompleted();

    /**
     * 传输出现异常，需要终止等（和onCompleted方法两者只可调一次）
     *
     * @param t 异常
     */
    public void onError(Throwable t);
}
