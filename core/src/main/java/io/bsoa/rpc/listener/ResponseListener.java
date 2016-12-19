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
package io.bsoa.rpc.listener;

/**
 * 客户端拿到结果的Listener
 *
 * Created by zhangg on 2016/7/15 23:59.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public interface ResponseListener {

    /**
     * 得到正常返回的结果
     *
     * @param result
     *         the result 正常返回结果
     */
    public void handleResult(Object result);

    /**
     * 捕获到异常后
     *
     * @param e
     *         the e 异常
     */
    public void catchException(Throwable e);
}
