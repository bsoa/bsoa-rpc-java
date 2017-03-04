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
package io.bsoa.rpc.common.struct;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 计数器，从0开始，保证正数。<br/>
 * 有人问为什么不用JDK8的LongAdder，这个根本就没有incrementAndGet()方法，然并卵
 * <p>
 * Created by zhangg on 2016/7/16 00:18.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class PositiveAtomicCounter {
    private final AtomicInteger atom;
    private static final int mask = 0x7FFFFFFF;

    public PositiveAtomicCounter() {
        atom = new AtomicInteger(0);
    }

    public final int incrementAndGet() {
        return atom.incrementAndGet() & mask;
    }

    public final int getAndIncrement() {
        return atom.getAndIncrement() & mask;
    }

    public int get() {
        return atom.get();
    }

}