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
package io.bsoa.rpc.protocol.bsoa;

import io.bsoa.rpc.exception.BsoaRuntimeException;
import io.bsoa.rpc.message.NegotiationRequest;
import io.bsoa.rpc.transport.AbstractChannel;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/03/2017/3/5 18:37. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public interface BsoaNegotiationHandler {

    /**
     * 得到命令
     *
     * @return 命令
     */
    String command();

    /**
     * 处理命令
     *
     * @param request 请求内容
     * @param channel 长连接（带上下文）
     * @return 正常返回的响应
     * @throws BsoaRuntimeException 异常时一定要抛出
     */
    String handle(NegotiationRequest request, AbstractChannel channel) throws BsoaRuntimeException;
}
