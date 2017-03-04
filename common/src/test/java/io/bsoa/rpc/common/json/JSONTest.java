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
package io.bsoa.rpc.common.json;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Created by zhangg on 2017/1/20 14:07.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class JSONTest {

    @Test
    public void getSerializeFields() throws Exception {
        List<Field> fields = JSON.getSerializeFields(TestJsonBean.class);
        Assert.assertNotNull(fields);
        Assert.assertEquals(fields.size(), 7);
    }

    @Test
    public void testToJSONString() {
        TestJsonBean bean = new TestJsonBean();
        bean.setName("zzzgg");
        bean.setSex(true);
        bean.setAge(111);
        bean.setStep(1234567890l);
        bean.setFriends(new ArrayList<>());
        bean.setStatus(TestJsonBean.Status.START);

        String json = JSON.toJSONString(bean);
        Assert.assertNotNull(json);
    }

    @Test
    public void testParseObject() {
        TestJsonBean bean = new TestJsonBean();
        bean.setName("zzzgg");
        bean.setSex(true);
        bean.setAge(111);
        bean.setStep(1234567890l);
        bean.setFriends(new ArrayList<>());
        bean.setStatus(TestJsonBean.Status.START);
        {
            String json = JSON.toJSONString(bean);
            TestJsonBean bean1 = JSON.parseObject(json, TestJsonBean.class);
            System.out.println(bean1);
        }
    }
}