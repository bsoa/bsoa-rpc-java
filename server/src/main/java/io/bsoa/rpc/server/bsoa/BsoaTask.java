/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bsoa.rpc.server.bsoa;

import java.util.PriorityQueue;

import io.bsoa.rpc.message.BaseMessage;
import io.bsoa.rpc.message.NegotiatorResponse;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.server.AbstractTask;
import io.bsoa.rpc.transport.AbstractChannel;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/1/1 20:30. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class BsoaTask extends AbstractTask {

    private final BaseMessage msg;

    private final AbstractChannel channel;

    protected BsoaTask(BaseMessage msg, AbstractChannel channel) {
        this.msg = msg;
        this.channel = channel;
    }

    protected BsoaTask(BaseMessage msg, AbstractChannel channel, int priority) {
        this.msg = msg;
        this.channel = channel;
        this.priority = priority;
    }

    @Override
    public void run() {

    }

    @Override
    public String toString() {
        return "BsoaTask[m:" + msg + ",p:" + priority + "]";
    }

    public static void main(String[] args) {
        PriorityQueue set = new PriorityQueue();
        set.add(new BsoaTask(null, null, 10));
        set.add(new BsoaTask(null, null, -99));
        set.add(new BsoaTask(null, null, 99));
        set.add(new BsoaTask(new NegotiatorResponse(), null, 1));
        set.add(new BsoaTask(new RpcResponse(), null, 1));
        set.add(new BsoaTask(new RpcRequest(), null, 1));
        set.add(new BsoaTask(null, null, -10));
        set.add(new BsoaTask(null, null, 0));
        set.add(new BsoaTask(null, null, 0));
        set.add(new BsoaTask(null, null, -1));

        Object o;
        while ((o = set.poll()) != null) {
            System.out.println(o);
        }
    }
}
