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
package io.bsoa.rpc.base;

/**
 * <p></p>
 *
 * Created by zhangg on 2017/2/23 19:02. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public interface Initializable {
    /**
     * 初始化
     */
    public void init();

    /**
     * 初始化动作
     * @param hook 初始化钩子
     */
    public default void init(InitializeHook hook) {
        if (hook != null) {
            hook.preInitialize();
        }
        init();
        if (hook != null) {
            hook.postInitialize();
        }
    }

    /**
     * 初始化钩子
     */
    interface InitializeHook {
        /**
         * 加载前要做的事情
         */
        public default void preInitialize(){};

        /**
         *  加载后要做的事情
         */
        public void postInitialize();
    }
}
