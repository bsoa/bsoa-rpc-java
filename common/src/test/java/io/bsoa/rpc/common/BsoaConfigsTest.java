package io.bsoa.rpc.common;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhangg on 2016/12/16.
 */
public class BsoaConfigsTest {
    
    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(BsoaConfigsTest.class);
    
    @Test
    public void getStringValue() throws Exception {
        String s = BsoaConfigs.CALLBACK_POOL_CORE;
        Assert.assertEquals(BsoaConfigs.getStringValue(BsoaConfigs.SERVER_CONTEXT_PATH), "/");
        System.out.println(BsoaConfigs.getListValue(BsoaConfigs.EXTENSION_LOAD_PATH));
    }

}