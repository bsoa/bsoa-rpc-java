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
package io.bsoa.rpc.listener;

import io.bsoa.rpc.transport.AbstractChannel;

/**
 * Listener of channel event
 * <p>
 * Created by zhangg on 2016/7/14 23:02.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public interface ChannelListener {

    /**
     * Handle connect event on channel active
     *
     * @param channel Channel
     */
    public void onConnected(AbstractChannel channel);

    /**
     * Handle disconnect event on channel closed
     *
     * @param channel Channel
     */
    public void onDisconnected(AbstractChannel channel);
}
