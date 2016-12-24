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
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhanggeng on 16-6-6.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class RpcRequest extends RPCMessage implements Serializable {

    public RpcRequest() {
        super(MessageType.RPC_REQUEST);
    }

    private transient String ifaceId; // 接口id， 有的时候不传递className和argsType

    private String className;

    private String methodName;

    private String alias;

    private String[] argsType; //考虑优化class？

    private transient Class[] argClasses;

    private Object[] args;

    private Map<String,Object> attachments = new HashMap<String,Object>();

    public String getIfaceId() {
        return ifaceId;
    }

    public void setIfaceId(String ifaceId) {
        this.ifaceId = ifaceId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String[] getArgsType() {
        return argsType;
    }

    public void setArgsType(String[] argsType) {
        this.argsType = argsType;
    }

    public Class[] getArgClasses() {
        return argClasses;
    }

    public void setArgClasses(Class[] argClasses) {
        this.argClasses = argClasses;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Map<String, Object> getAttachments() {
        return attachments;
    }

    public void setAttachments(Map<String, Object> attachments) {
        this.attachments = attachments;
    }

    public void addAttachments(Map<String,Object> sourceMap){
        this.attachments.putAll(sourceMap);
    }
}
