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
package io.bsoa.rpc.protocol.bsoa.handler;

import io.bsoa.rpc.common.BsoaVersion;
import io.bsoa.rpc.common.json.JSON;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.message.NegotiationRequest;
import io.bsoa.rpc.protocol.bsoa.BsoaNegotiationHandler;
import io.bsoa.rpc.transport.ChannelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>协商处理器</p>
 * <p>
 * Created by zhangg on 2017/2/20 21:44. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class VersionNegotiationHandler implements BsoaNegotiationHandler {

    /**
     * slf4j Logger for this class
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(VersionNegotiationHandler.class);

    @Override
    public String command() {
        return "version";
    }

    @Override
    public String handle(NegotiationRequest request, ChannelContext context) throws BsoaRuntimeException {
        String data = request.getData();
        Map<String, String> map = JSON.parseObject(data, Map.class);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Receive client version negotiation info : {}", map);
        }
        context.setDstVersion(Integer.parseInt(map.get("version")));

        Map<String, String> tmp = new HashMap<>();
        tmp.put("version", BsoaVersion.BSOA_VERSION + "");
        tmp.put("build", BsoaVersion.BUILD_VERSION);
        return JSON.toJSONString(tmp); // 把自己版本发给对方
    }
}
