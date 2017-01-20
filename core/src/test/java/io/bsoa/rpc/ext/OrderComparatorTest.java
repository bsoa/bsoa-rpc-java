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
package io.bsoa.rpc.ext;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Created by zhanggeng on 17-01-20.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class OrderComparatorTest {
    @Test
    public void compare() throws Exception {

        List<ExtensionClass> list = new ArrayList<>();
        list.add(new ExtensionClass().setAlias("a").setOrder(10));
        list.add(new ExtensionClass().setAlias("b").setOrder(2));
        list.add(new ExtensionClass().setAlias("c").setOrder(6));
        list.add(new ExtensionClass().setAlias("d").setOrder(6));
        list.add(new ExtensionClass().setAlias("e").setOrder(0));
        list.add(new ExtensionClass().setAlias("f").setOrder(-1));
        list.add(new ExtensionClass().setAlias("g").setOrder(10));
        list.sort(new ExtensionLoader.OrderComparator());

        for (ExtensionClass extensionClass : list) {
            System.out.println(extensionClass);
        }
    }
}