package io.bsoa.rpc.common.json;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Title: <br>
 * <p>
 * Description: <br>
 * <p>
 * Company: <a href=www.jd.com>京东</a><br>
 *
 * @author <a href=mailto:zhanggeng@jd.com>章耿</a>
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