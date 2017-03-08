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
package io.bsoa.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/03/2017/3/4 15:50. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class EchoServiceImpl implements EchoService {

    /**
     * slf4j Logger for this class
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(EchoServiceImpl.class);

    @Override
    public String echoStr(String arg) {
        LOGGER.info("server receive {}", arg);
        return arg;
    }
}
