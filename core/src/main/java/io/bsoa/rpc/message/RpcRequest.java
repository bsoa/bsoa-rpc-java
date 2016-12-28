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
package io.bsoa.rpc.message;

import java.io.Serializable;

/**
 * Created by zhanggeng on 16-6-6.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class RpcRequest extends RPCMessage implements Serializable {

    private static final long serialVersionUID = -7300288646979013314L;

    public RpcRequest() {
        super(MessageConstants.RPC_REQUEST);
    }

    private transient String interfaceName;

    private transient String methodName;

    private transient String group;

    private String[] argsType; //考虑优化class？

    private transient Class[] argClasses;

    private Object[] args;

    public String getInterfaceName() {
        return interfaceName;
    }

    public RpcRequest setInterfaceName(String interfaceName) {
        super.addHeadKey(HeadKey.INTERFACE_NAME, interfaceName);
        this.interfaceName = interfaceName;
        return this;
    }

    public String getMethodName() {
        return methodName;
    }

    public RpcRequest setMethodName(String methodName) {
        super.addHeadKey(HeadKey.METHOD_NAME, interfaceName);
        this.methodName = methodName;
        return this;
    }

    public String getGroup() {
        return group;
    }

    public RpcRequest setGroup(String group) {
        super.addHeadKey(HeadKey.GROUP, interfaceName);
        this.group = group;
        return this;
    }

    public String[] getArgsType() {
        return argsType;
    }

    public RpcRequest setArgsType(String[] argsType) {
        this.argsType = argsType;
        return this;
    }

    public Class[] getArgClasses() {
        return argClasses;
    }

    public RpcRequest setArgClasses(Class[] argClasses) {
        this.argClasses = argClasses;
        return this;
    }

    public Object[] getArgs() {
        return args;
    }

    public RpcRequest setArgs(Object[] args) {
        this.args = args;
        return this;
    }
}
