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
package io.bsoa.rpc.protocol.bsoa.handler;

import io.bsoa.rpc.common.json.JSON;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.message.NegotiationRequest;
import io.bsoa.rpc.protocol.bsoa.BsoaNegotiationHandler;
import io.bsoa.rpc.transport.AbstractChannel;
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
public class HeaderKeyNegotiationHandler implements BsoaNegotiationHandler {
    /**
     * slf4j Logger for this class
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(HeaderKeyNegotiationHandler.class);

    @Override
    public String command() {
        return "headerKey";
    }

    @Override
    public String handle(NegotiationRequest request, AbstractChannel channel) throws BsoaRuntimeException {
        ChannelContext context = channel.context();
        String data = request.getData();
        Map<String, String> map = JSON.parseObject(data, Map.class);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Receive client header cache negotiation info : {}", map);
        }
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            try {
                String refValue = entry.getKey();
                Short refIndex = context.getHeaderKey(refValue);
                if (refIndex == null) {
                    // 客户端发来的true,服务端发来的是false
                    boolean consumerToProvider = CommonUtils.isTrue(entry.getValue());
                    refIndex = context.getAvailableRefIndex(consumerToProvider);
                    if (refValue != null) { // 服务端生成key，返回给客户端
                        context.putHeadCache(refIndex, refValue);
                    }
                }
                result.put(refValue, refIndex + "");
            } catch (Exception e) {
                LOGGER.warn("", e);
            }
        }
        return JSON.toJSONString(result);
    }
}
