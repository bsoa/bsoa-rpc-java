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
package io.bsoa.rpc.invoke;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

/**
 * Created by zhangg on 2017/2/11.
 */
public class StreamUtilsTest {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(StreamUtilsTest.class);

    @Test
    public void testScanMethod() throws Exception {

        Method[] methods = TestSteamClass.class.getMethods();
        HashMap<String, Method> methodMap = new HashMap<>();
        int cntOk = 0;
        int cntFail = 0;
        for (Method method : methods) {
            if (method.getName().startsWith("ok")) {
                cntOk++;
            } else if (method.getName().startsWith("fail")) {
                cntFail++;
            }
            methodMap.put(method.getName(), method);
        }
        for (int i = 0; i < cntOk; i++) {
            Assert.assertTrue(doTestScanMethod(methodMap.get("ok" + i)));
        }
        for (int i = 0; i < cntFail; i++) {
            LOGGER.warn("fail" + i);
            Assert.assertFalse(doTestScanMethod(methodMap.get("fail" + i)));
        }
        //Assert.assertTrue(StreamContext.getParamClassStreamMethod());
    }

    private boolean doTestScanMethod(Method method) {
        try {
            StreamUtils.scanMethod(TestSteamClass.class.getName(), method);
            return true;
        } catch (Exception e) {
            LOGGER.warn(e.getMessage(), e);
            return false;
        }
    }

    private interface TestSteamClass {
        void ok0(String s);

        String ok1();

        String ok2(String s);

        String ok3(StreamObserver<String> o);

        String ok4(StreamObserver<List<String>> o);

        String ok5(StreamObserver<List<?>> o);

        StreamObserver<String> ok6();

        StreamObserver<List<String>> ok7(int o);

        StreamObserver<List<?>> ok8(List o);

        StreamObserver<String> ok9(StreamObserver<String> o);

        XX<String> ok10();

        String ok11(XX<String> x1);

        String fail0(StreamObserver o);

        String fail1(StreamObserver<?> o);

        String fail2(StreamObserver<String> o, StreamObserver<String> o1);

        StreamObserver fail3();

        StreamObserver<?> fail4(String o);

        String fail5(XX<String> x1, XX<String> x2);

        String fail6(YY<String, String> x1);

        YY<String, String> fail7(YY<String, String> x1);

    }

    private interface XX<T> extends StreamObserver<T> {
    }

    private interface YY<T, V> extends StreamObserver<T> {
    }
}