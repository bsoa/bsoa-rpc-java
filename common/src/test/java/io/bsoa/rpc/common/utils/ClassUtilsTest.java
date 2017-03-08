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
package io.bsoa.rpc.common.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by zhangg on 2017/2/8.
 */
public class ClassUtilsTest {
    @Test
    public void testNewInstance() throws Exception {
        int i = ClassUtils.newInstance(int.class);
        Assert.assertEquals(i, 0);
        Integer integer = ClassUtils.newInstance(Integer.class);
        Assert.assertEquals(integer.intValue(), 0);
        Assert.assertNotNull(ClassUtils.newInstance(TestMemberClass1.class));
        Assert.assertNotNull(ClassUtils.newInstance(TestMemberClass2.class));
        Assert.assertNotNull(ClassUtils.newInstance(TestMemberClass3.class));
        Assert.assertNotNull(ClassUtils.newInstance(TestMemberClass4.class));
        Assert.assertNotNull(ClassUtils.newInstance(TestMemberClass5.class));
        Assert.assertNotNull(ClassUtils.newInstance(TestMemberClass6.class));
        Assert.assertNotNull(ClassUtils.newInstance(TestClass1.class));
        Assert.assertNotNull(ClassUtils.newInstance(TestClass2.class));
        TestClass3 class3 = ClassUtils.newInstance(TestClass3.class);
        Assert.assertNotNull(class3);
        Assert.assertNull(class3.getName());
        Assert.assertEquals(class3.getAge(), 0);

    }

    @Test
    public void testNewInstanceWithArgs() throws Exception {
        Assert.assertNotNull(ClassUtils.newInstanceWithArgs(TestMemberClass3.class, null, null));
        Assert.assertNotNull(ClassUtils.newInstanceWithArgs(TestMemberClass3.class,
                new Class[]{String.class}, new Object[]{"2222"}));
        Assert.assertNotNull(ClassUtils.newInstanceWithArgs(TestMemberClass6.class, null, null));
        Assert.assertNotNull(ClassUtils.newInstanceWithArgs(TestMemberClass6.class,
                new Class[]{int.class}, new Object[]{222}));
        Assert.assertNotNull(ClassUtils.newInstanceWithArgs(TestClass3.class, null, null));
        Assert.assertNotNull(ClassUtils.newInstanceWithArgs(TestClass3.class,
                new Class[]{String.class, int.class}, new Object[]{"xxx", 222}));
    }

    @Test
    public void testGetParamArg() throws Exception {
        Assert.assertNull(ClassUtils.getParamArg(String.class));
    }


    private static class TestMemberClass1 {

    }

    private static class TestMemberClass2 {
        private TestMemberClass2() {
            System.out.println("init TestMemberClass2 ");
        }
    }

    private static class TestMemberClass3 {
        private TestMemberClass3(String s) {
            System.out.println("init TestMemberClass3 ");
        }

        private TestMemberClass3(String s, int i) {
            System.out.println("init TestMemberClass3 with 2 arg");
        }
    }

    private class TestMemberClass4 {

    }

    private class TestMemberClass5 {
        private TestMemberClass5() {
            System.out.println("init TestMemberClass5 ");
        }
    }

    private class TestMemberClass6 {
        private TestMemberClass6(int s) {
            System.out.println("init TestMemberClass6 ");
        }

        private TestMemberClass6(String s, int i) {
            System.out.println("init TestMemberClass6 with 2 arg");
        }
    }

}