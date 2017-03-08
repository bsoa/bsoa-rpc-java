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
package io.bsoa.rpc.message;

import io.bsoa.rpc.common.utils.ClassTypeUtils;

import java.io.Serializable;

/**
 * Created by zhangg on 16-6-6.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class RpcRequest extends RPCMessage implements Serializable {

    private static final long serialVersionUID = -7300288646979013314L;

    /**
     * Instantiates a new Rpc request.
     */
    public RpcRequest() {
        super(MessageConstants.RPC_REQUEST);
    }

    private transient String interfaceName;

    private transient String methodName;

    private transient String tags;

    private String[] argsType; //考虑优化class？

    private transient Class[] argClasses; // 考虑classes不传？

    private Object[] args;

    /**
     * Gets interface name.
     *
     * @return the interface name
     */
    public String getInterfaceName() {
        if (interfaceName == null) {
            interfaceName = (String) super.getHeadKey(HeadKey.INTERFACE_NAME);
        }
        return interfaceName;
    }

    /**
     * Sets interface name.
     *
     * @param interfaceName the interface name
     * @return the interface name
     */
    public RpcRequest setInterfaceName(String interfaceName) {
        super.addHeadKey(HeadKey.INTERFACE_NAME, interfaceName);
        this.interfaceName = interfaceName;
        return this;
    }

    /**
     * Gets method name.
     *
     * @return the method name
     */
    public String getMethodName() {
        if (methodName == null) {
            methodName = (String) super.getHeadKey(HeadKey.METHOD_NAME);
        }
        return methodName;
    }

    /**
     * Sets method name.
     *
     * @param methodName the method name
     * @return the method name
     */
    public RpcRequest setMethodName(String methodName) {
        super.addHeadKey(HeadKey.METHOD_NAME, methodName);
        this.methodName = methodName;
        return this;
    }

    /**
     * Gets tags.
     *
     * @return the tags
     */
    public String getTags() {
        if (tags == null) {
            tags = (String) super.getHeadKey(HeadKey.TAGS);
        }
        return tags;
    }

    /**
     * Sets tags.
     *
     * @param tags the tags
     * @return the tags
     */
    public RpcRequest setTags(String tags) {
        super.addHeadKey(HeadKey.TAGS, tags);
        this.tags = tags;
        return this;
    }

    /**
     * Get args type string [ ].
     *
     * @return the string [ ]
     */
    public String[] getArgsType() {
        return argsType;
    }

    /**
     * Sets args type.
     *
     * @param argsType the args type
     * @return the args type
     */
    public RpcRequest setArgsType(String[] argsType) {
        this.argsType = argsType;
        this.argClasses = ClassTypeUtils.getClasses(argsType);
        return this;
    }

    /**
     * Get arg classes class [ ].
     *
     * @return the class [ ]
     */
    public Class[] getArgClasses() {
        return argClasses;
    }

    /**
     * Sets arg classes.
     *
     * @param argClasses the arg classes
     * @return the arg classes
     */
    public RpcRequest setArgClasses(Class<?>[] argClasses) {
        this.argClasses = argClasses;
        this.argsType = ClassTypeUtils.getTypeStrs(argClasses);
        return this;
    }

    /**
     * Get args object [ ].
     *
     * @return the object [ ]
     */
    public Object[] getArgs() {
        return args;
    }

    /**
     * Sets args.
     *
     * @param args the args
     * @return the args
     */
    public RpcRequest setArgs(Object[] args) {
        this.args = args;
        return this;
    }
}
