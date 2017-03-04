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

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by zhangg on 2017/2/20.
 */
public class ConcurrentReferenceHashMapTest {
    @Test
    public void testAll() throws Exception {
        // 强引用： 默认，不回收
        // 弱引用： key没有强引用就回收
        // 软引用： 内存不足时才回收
        ConcurrentReferenceHashMap hashMap = new ConcurrentReferenceHashMap(8,
                ConcurrentReferenceHashMap.ReferenceType.WEAK);
        Assert.assertTrue(hashMap.size() == 0);
//        xxx(hashMap);
        String s = "xxx";
        hashMap.put(s, "xxx");
        s = null;

        Assert.assertTrue(hashMap.size() == 1);
        System.out.println("3:" + hashMap);


        System.gc();
//        Thread.sleep(5000);
//        Assert.assertTrue(hashMap.size() == 0);
        System.out.println("4:" + hashMap);
        System.out.println("4:" + hashMap.size());
        System.out.println(hashMap.containsKey("xxx"));
        System.out.println(hashMap.get("xxx"));
    }

    private void xxx(ConcurrentReferenceHashMap hashMap) {
        String s = "xxx";
        hashMap.put(s, "xxx");
        Assert.assertTrue(hashMap.size() == 1);
        System.out.println("2:" + hashMap);
        s = null;
    }

    @Test
    public void get() throws Exception {

    }

    @Test
    public void containsKey() throws Exception {

    }

    @Test
    public void put() throws Exception {

    }

}