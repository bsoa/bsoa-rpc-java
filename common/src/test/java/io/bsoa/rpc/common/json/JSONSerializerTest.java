package io.bsoa.rpc.common.json;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Title: <br>
 * <p>
 * Description: <br>
 * <p>
 * Company: <a href=www.jd.com>京东</a><br>
 *
 * @author <a href=mailto:zhanggeng@jd.com>章耿</a>
 */
public class JSONSerializerTest {

    @Test
    public void testSerialize() {
        Assert.assertEquals(JSONSerializer.serialize(null), "null");
        Assert.assertEquals(JSONSerializer.serialize((byte)1), "1");
        Assert.assertEquals(JSONSerializer.serialize((short)1), "1");
        Assert.assertEquals(JSONSerializer.serialize(-1), "-1");
        Assert.assertEquals(JSONSerializer.serialize(1l), "1");
        Assert.assertEquals(JSONSerializer.serialize(1.0d), "1.0");
        Assert.assertEquals(JSONSerializer.serialize(1.0f), "1.0");
        Assert.assertEquals(JSONSerializer.serialize(true), "true");
        Assert.assertEquals(JSONSerializer.serialize(false), "false");
        Assert.assertEquals(JSONSerializer.serialize('c'), "\"c\"");
        Assert.assertEquals(JSONSerializer.serialize("c"), "\"c\"");
        Assert.assertEquals(JSONSerializer.serialize("\"c\""), "\"\\\"c\\\"\"");

        Assert.assertEquals(JSONSerializer.serialize(new String[]{}), "[]");
        Assert.assertEquals(JSONSerializer.serialize(new String[]{"1","2"}), "[\"1\",\"2\"]");
        List list = new ArrayList();
        Assert.assertEquals(JSONSerializer.serialize(list), "[]");
        list.add("1");
        list.add("2");
        Assert.assertEquals(JSONSerializer.serialize(list), "[\"1\",\"2\"]");
        Map map = new HashMap();
        Assert.assertEquals(JSONSerializer.serialize(map), "{}");
        map.put("1","2");
        Assert.assertEquals(JSONSerializer.serialize(map), "{\"1\":\"2\"}");
    }

    @Test
    public void testDeserialize() {

        Assert.assertEquals(JSONSerializer.deserialize("\"c\""), "c");
        Assert.assertEquals(JSONSerializer.deserialize("'c'"), "c");
        List list = (List) JSONSerializer.deserialize("[]");
        Assert.assertEquals(list.size(), 0);
        Map map = (Map) JSONSerializer.deserialize("{}");
        Assert.assertEquals(map.size(), 0);

        String s = "{" +
                "\"a\": null," +
                "        \"b\":1," +
                "        \"c\":9999999999," +
                "        \"d\":1.0," +
                "        \"e\":false," +
                "        \"f\":\"c\"," +
                "        \"g\":[]," +
                "        \"h\":[1,2]," +
                "        \"i\":[\"11\",\"22\"]," +
                "        \"j\":{}," +
                "        \"k\":{" +
                "            \"11\":\"22\"" +
                "        }" +
                "}";
        Map json = (Map) JSONSerializer.deserialize(s);
        Assert.assertNotNull(json);
        Assert.assertEquals(json.get("a"), null);
        Assert.assertEquals(json.get("b"), 1);
        Assert.assertEquals(json.get("c"), 9999999999l);
        Assert.assertEquals(json.get("d"), 1.0);
        Assert.assertEquals(json.get("e"), false);
        Assert.assertEquals(json.get("f"), "c");
        Assert.assertEquals(((List)json.get("g")).size(), 0);
        Assert.assertEquals(((List)json.get("h")).get(0), 1);
        Assert.assertEquals(((List)json.get("i")).get(1), "22");
        Assert.assertEquals(((Map) json.get("j")).size(), 0);
        Assert.assertEquals(((Map) json.get("k")).get("11"), "22");
    }

    @Test
    public void testDeserializeWithComment() {

        String s = "{" +
                "\"a\": null, // 111\n" +
                "        \"b\":1, /*2   // asdsad / das */\n" +
                "        \"c\":1, /*2   // asdsad \n \r / das */\n" +
                "        \"d\":9999999999" +
                "}";
        System.out.println(s);
        Map json = (Map) JSONSerializer.deserialize(s);
        Assert.assertNotNull(json);
        Assert.assertEquals(json.get("a"), null);
        Assert.assertEquals(json.get("b"), 1);
        Assert.assertEquals(json.get("c"), 1);
        Assert.assertEquals(json.get("d"), 9999999999l);
    }
}
