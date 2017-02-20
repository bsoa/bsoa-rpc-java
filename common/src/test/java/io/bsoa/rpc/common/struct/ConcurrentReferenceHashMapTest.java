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

    private void xxx( ConcurrentReferenceHashMap hashMap) {
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