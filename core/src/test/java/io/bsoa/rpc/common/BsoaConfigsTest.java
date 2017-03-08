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
package io.bsoa.rpc.common;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhangg on 17-01-20.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class BsoaConfigsTest {
    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(BsoaConfigsTest.class);

    @Test
    public void putValue() throws Exception {

    }

    @Test
    public void getBooleanValue() throws Exception {

    }

    @Test
    public void getBooleanValue1() throws Exception {

    }

    @Test
    public void getIntValue() throws Exception {

    }

    @Test
    public void getOrDefaultValue() throws Exception {

    }

    @Test
    public void getIntValue1() throws Exception {

    }

    @Test
    public void getEnumValue() throws Exception {

    }

    @Test
    public void getStringValue() throws Exception {
        String s = BsoaOptions.ASYNC_POOL_CORE;
        Assert.assertEquals(BsoaConfigs.getStringValue(BsoaOptions.SERVER_CONTEXT_PATH), "/");
        System.out.println(BsoaConfigs.getListValue(BsoaOptions.EXTENSION_LOAD_PATH));
    }

    @Test
    public void getStringValue1() throws Exception {

    }

    @Test
    public void getStringValue2() throws Exception {

    }

    @Test
    public void getListValue() throws Exception {

    }

    @Test
    public void subscribe() throws Exception {

    }

    @Test
    public void unSubscribe() throws Exception {

    }

    public void changed() throws Exception {
        Assert.assertTrue(BsoaConfigs.changed(null, "aaa"));
        Assert.assertTrue(BsoaConfigs.changed("aa", "aaa"));
        Assert.assertTrue(BsoaConfigs.changed("aa", null));

        Assert.assertFalse(BsoaConfigs.changed(null, null));
        Assert.assertFalse(BsoaConfigs.changed("aaa", "aaa"));
    }

}