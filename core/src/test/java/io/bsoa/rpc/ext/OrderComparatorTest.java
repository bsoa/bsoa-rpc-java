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
package io.bsoa.rpc.ext;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangg on 17-01-20.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class OrderComparatorTest {
    @Test
    public void compare() throws Exception {

        List<ExtensionClass> list = new ArrayList<>();
        list.add(new ExtensionClass(null, "a").setOrder(10));
        list.add(new ExtensionClass(null, "b").setOrder(2));
        list.add(new ExtensionClass(null, "c").setOrder(6));
        list.add(new ExtensionClass(null, "d").setOrder(6));
        list.add(new ExtensionClass(null, "e").setOrder(0));
        list.add(new ExtensionClass(null, "f").setOrder(-1));
        list.add(new ExtensionClass(null, "g").setOrder(10));
        list.sort(new ExtensionLoader.OrderComparator());

        for (ExtensionClass extensionClass : list) {
            System.out.println(extensionClass);
        }
    }
}