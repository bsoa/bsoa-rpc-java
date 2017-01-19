package io.bsoa.rpc.ext;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.filter.Filter;

/**
 * Created by zhangg on 2016/12/17.
 */
public class ExtensionLoaderTest {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ExtensionLoaderTest.class);


    @Test
    public void testCompare() throws Exception {
        List<ExtensionClass> list = new ArrayList<>();
        list.add(new ExtensionClass().setAlias("1").setOrder(10));
        list.add(new ExtensionClass().setAlias("2").setOrder(0));
        list.add(new ExtensionClass().setAlias("3").setOrder(0));
        list.add(new ExtensionClass().setAlias("4").setOrder(5));
        list.add(new ExtensionClass().setAlias("5").setOrder(12));
        list.add(new ExtensionClass().setAlias("6").setOrder(2));

        Collections.sort(list, new ExtensionLoader.OrderComparator());

        int i = 0; // 2,3,6,4,1,5
        Assert.assertEquals(list.get(i++).getAlias(),"2");
        Assert.assertEquals(list.get(i++).getAlias(),"3");
        Assert.assertEquals(list.get(i++).getAlias(),"6");
        Assert.assertEquals(list.get(i++).getAlias(),"4");
        Assert.assertEquals(list.get(i++).getAlias(),"1");
        Assert.assertEquals(list.get(i++).getAlias(),"5");
    }


    @Test
    public void testLoadFromFile() throws Exception {
        ExtensionLoader loader = new ExtensionLoader<Filter>(Filter.class, false, null);
        loader.loadFromFile("META-INF/exttest/");
        Assert.assertTrue(!loader.all.isEmpty());

        ExtensionClass extensibleClass = loader.getExtensionClass("testext");
        Assert.assertNotNull(extensibleClass);
        Assert.assertTrue(extensibleClass.getOrder() == 2);
        Assert.assertTrue(extensibleClass.isProviderSide());
        Assert.assertFalse(extensibleClass.isConsumerSide());
        Assert.assertTrue(CommonUtils.isNotEmpty(loader.getProviderSideAutoActives()));
        Assert.assertTrue(CommonUtils.isEmpty(loader.getConsumerSideAutoActives()));
    }

    @Test
    public void testReadLine() throws Exception {
        ExtensionLoader loader = new ExtensionLoader<Filter>(Filter.class, false, null);
        URL url = Filter.class.getResource("/META-INF/bsoa/" + Filter.class.getName());
        try {
            loader.readLine(url, "io.bsoa.rpc.test.HelloServiceImpl");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        Assert.assertTrue(loader.all.isEmpty());

        loader.all.clear();
        try {
            loader.readLine(url,  "io.bsoa.rpc.ext.WrongFilter0");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        try {
            loader.readLine(url,  "io.bsoa.rpc.ext.WrongFilter1");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        try {
            loader.readLine(url,  "io.bsoa.rpc.ext.WrongFilter2");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }

        try {
            loader.readLine(url, "echo1=io.bsoa.rpc.ext.ExtensionFilter");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        Assert.assertTrue(loader.all.isEmpty());

        try {
            loader.readLine(url, "io.bsoa.rpc.ext.ExtensionFilter");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        Assert.assertFalse(loader.all.isEmpty());

        loader.all.clear();
        try {
            loader.readLine(url, "testext=io.bsoa.rpc.ext.ExtensionFilter");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        Assert.assertFalse(loader.all.isEmpty());

        boolean isOk = true;
        try {
            loader.readLine(url, "io.bsoa.rpc.ext.ExtensionFilter");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
            isOk = false;
        }
        Assert.assertTrue(!loader.all.isEmpty());
        Assert.assertFalse(isOk);
    }

    @Test
    public void testParseAliasAndClassName() throws Exception {
        ExtensionLoader loader = new ExtensionLoader<Filter>(Filter.class);
        String[] ss = loader.parseAliasAndClassName("    # xxxx");
        Assert.assertNull(loader.parseAliasAndClassName("    # xxxx"));
        Assert.assertNull(loader.parseAliasAndClassName("# xxxx"));

        Assert.assertArrayEquals(loader.parseAliasAndClassName("1111"), new String[]{null, "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  1111"), new String[]{null, "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  1111   "), new String[]{null, "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("1111   "), new String[]{null, "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("1111#aa"), new String[]{null, "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  1111#aa"), new String[]{null, "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("1111#aa   "), new String[]{null, "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  1111#aa  "), new String[]{null, "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("1111 #aa"), new String[]{null, "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  1111 #aa"), new String[]{null, "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("1111 #aa   "), new String[]{null, "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  1111 #aa  "), new String[]{null, "1111"});

        Assert.assertArrayEquals(loader.parseAliasAndClassName("aa=1111"), new String[]{"aa", "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  aa=1111"), new String[]{"aa", "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("aa=1111  "), new String[]{"aa", "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  aa=1111  "), new String[]{"aa", "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("aa=1111#aa"), new String[]{"aa", "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  aa=1111#aa"), new String[]{"aa", "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("aa=1111#aa  "), new String[]{"aa", "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  aa=1111#aa  "), new String[]{"aa", "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("aa=1111  #aa"), new String[]{"aa", "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  aa=1111  #aa"), new String[]{"aa", "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("aa=1111  #aa  "), new String[]{"aa", "1111"});
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  aa=1111  #aa  "), new String[]{"aa", "1111"});
    }
}