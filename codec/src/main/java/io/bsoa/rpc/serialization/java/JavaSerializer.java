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
package io.bsoa.rpc.serialization.java;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.codec.Serializer;
import io.bsoa.rpc.common.struct.UnsafeByteArrayInputStream;
import io.bsoa.rpc.common.struct.UnsafeByteArrayOutputStream;
import io.bsoa.rpc.common.utils.ReflectUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.serialization.hessian.HessianConstants;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/24 21:09. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "java", code = 3)
public class JavaSerializer implements Serializer {

    private final static Logger LOGGER = LoggerFactory.getLogger(JavaSerializer.class);

    @Override
    public byte[] encode(Object obj) {
        UnsafeByteArrayOutputStream baos = new UnsafeByteArrayOutputStream(1024);
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(baos);
            if (obj instanceof RpcRequest) {
                RpcRequest req = (RpcRequest) obj;
                encodeRequest(req, oos);
            } else if (obj instanceof RpcResponse) {
                RpcResponse res = (RpcResponse) obj;
                encodeResponse(res, oos);
            } else {
                writeObject(obj, oos);
            }
            oos.flush();
            return baos.toByteArray();
        } catch (BsoaRpcException e) {
            throw e;
        } catch (Exception e) {
            throw new BsoaRpcException(22222, "Encode request error", e);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    LOGGER.warn("", e);
                }
            }
        }

    }

    private void encodeRequest(RpcRequest req, ObjectOutputStream out) throws IOException {
        Object[] args = req.getArgs();
        Class<?>[] pts = req.getArgClasses();
        writeString(ReflectUtils.getDesc(pts), out);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                writeObject(args[i], out);
            }
        }
        writeObject(req.getAttachments(), out);
    }

    private void encodeResponse(RpcResponse res, ObjectOutputStream out) throws IOException {
        // encode response data or error message.
        Throwable th = res.getException();
        if (th == null) {
            Object ret = res.getReturnData();
            if (ret == null) {
                out.writeInt(HessianConstants.RESPONSE_NULL);
            } else {
                out.writeInt(HessianConstants.RESPONSE_DATA);
                writeObject(ret, out);
            }
        } else {
            out.writeInt(HessianConstants.RESPONSE_EXCEPTION);
            writeObject(th, out);
        }
    }

    private void writeString(String v, ObjectOutputStream oos) throws IOException {
        if (v == null) {
            oos.writeInt(-1);
        } else {
            oos.writeInt(v.length());
            oos.writeUTF(v);
        }
    }

    private void writeObject(Object obj, ObjectOutputStream oos) throws IOException {
        if (obj == null) {
            oos.writeByte(0);
        } else {
            oos.writeByte(1);
            oos.writeObject(obj);
        }
    }

    private String readString(ObjectInputStream ois) throws IOException {
        int len = ois.readInt();
        if (len < 0)
            return null;

        return ois.readUTF();
    }

    private Object readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        byte b = ois.readByte();
        if (b == 0) {
            return null;
        }
        return ois.readObject();
    }

    @Override
    public Object decode(byte[] data, Class clazz) {
        UnsafeByteArrayInputStream bais = new UnsafeByteArrayInputStream(data);
        ObjectInputStream ois = null;
        Object obj;
        try {
            ois = new ObjectInputStream(bais);
            if (clazz == null) {
                obj = readObject(ois); // 无需依赖class
            } else if (clazz == RpcRequest.class) {
                obj = decodeRequest(ois, new RpcRequest());
            } else if (clazz == RpcResponse.class) {
                obj = decodeResponse(ois, new RpcResponse());
            } else {
                obj = readObject(ois); // 无需依赖class
            }
        } catch (Exception e) {
            throw new BsoaRpcException(22222, "Decode error", e);
        }
        return obj;
    }

    @Override
    public Object decode(byte[] data, Object template) {
        UnsafeByteArrayInputStream bais = new UnsafeByteArrayInputStream(data);
        ObjectInputStream ois = null;
        Object obj;
        try {
            ois = new ObjectInputStream(bais);
            if (template == null) {
                obj = readObject(ois); // 无需依赖class
            } else if (template instanceof RpcRequest) {
                obj = decodeRequest(ois, (RpcRequest) template);
            } else if (template instanceof RpcResponse) {
                obj = decodeResponse(ois, (RpcResponse) template);
            } else {
                obj = readObject(ois); // 无需依赖class
            }
        } catch (Exception e) {
            throw new BsoaRpcException(22222, "Decode error", e);
        }
        return obj;
    }

    /**
     * 解码客户端发来的RpcRequest.
     *
     * @param input the input
     * @param req   请求
     * @return the request message
     * @throws IOException the iO exception
     */
    private RpcRequest decodeRequest(ObjectInputStream input, RpcRequest req) throws IOException {
        try {
            Object[] args;
            Class<?>[] pts;
            String desc = readString(input);
            if (StringUtils.isEmpty(desc)) {
                pts = EMPTY_CLASS_ARRAY;
                args = EMPTY_OBJECT_ARRAY;
            } else {
                pts = ReflectUtils.desc2classArray(desc);
                args = new Object[pts.length];
                for (int i = 0; i < args.length; i++) {
                    try {
                        args[i] = readObject(input);
                    } catch (Exception e) {
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn("Decode argument failed: " + e.getMessage(), e);
                        }
                    }
                }
            }
            Map<String, Object> map = (Map<String, Object>) input.readObject();
            req.setArgClasses(pts);
            req.setArgs(args);
            req.setAttachments(map);
        } catch (ClassNotFoundException e) {
            throw new IOException("Read invocation data failed.", e);
        }

        return req;
    }


    /**
     * 解码服务端返回的Response
     *
     * @param in  the in
     * @param res 响应
     * @return the response message
     * @throws IOException the iO exception
     */
    private RpcResponse decodeResponse(ObjectInputStream in, RpcResponse res) throws IOException, ClassNotFoundException {
        byte code = (byte) in.readInt();
        switch (code) {
            case HessianConstants.RESPONSE_NULL:
                break;
            case HessianConstants.RESPONSE_DATA:
                res.setReturnData(readObject(in));
                break;
            case HessianConstants.RESPONSE_EXCEPTION:
                res.setException((Throwable) readObject(in));
                break;
            default:
                break;
        }
        return res;
    }

    /**
     * 空的Object数组，无参方法
     */
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    /**
     * 空的Class数组，无参方法
     */
    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
}
