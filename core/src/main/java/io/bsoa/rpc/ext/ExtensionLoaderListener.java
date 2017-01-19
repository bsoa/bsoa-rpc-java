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
package io.bsoa.rpc.ext;

/**
 * <p>当扩展点加载时，可以做一些事情，例如解析code，初始化等动作</p>
 * <p>
 * Created by zhangg on 2016/12/24 23:47. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public interface ExtensionLoaderListener<T> {

    /**
     * 当扩展点加载时，触发的事件
     *
     * @param extensionClass 扩展点类对象
     */
    public void onLoad(ExtensionClass<T> extensionClass);
}
