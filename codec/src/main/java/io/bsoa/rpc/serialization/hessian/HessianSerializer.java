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
package io.bsoa.rpc.serialization.hessian;

import io.bsoa.rpc.codec.Serializer;
import io.bsoa.rpc.common.struct.UnsafeByteArrayInputStream;
import io.bsoa.rpc.common.struct.UnsafeByteArrayOutputStream;
import io.bsoa.rpc.common.utils.ReflectUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.exception.BsoaRpcException;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;
import io.bsoa.rpc.serialization.hessian.io.AbstractHessianInput;
import io.bsoa.rpc.serialization.hessian.io.AbstractHessianOutput;
import io.bsoa.rpc.serialization.hessian.io.Hessian2Input;
import io.bsoa.rpc.serialization.hessian.io.Hessian2Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static io.bsoa.rpc.serialization.hessian.HessianConstants.EMPTY_CLASS_ARRAY;
import static io.bsoa.rpc.serialization.hessian.HessianConstants.EMPTY_OBJECT_ARRAY;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/24 21:09. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "hessian", code = 2)
public class HessianSerializer implements Serializer {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(HessianSerializer.class);


    /**
     * Instantiates a new Hessian codec.
     */
    public HessianSerializer() {
//        HessianObjectMapping.registryMapping("com.alibaba.dubbo.rpc.service.GenericException"
//                                            ,"io.bsoa.service.rpc.service.GenericException");
//        HessianObjectMapping.registryMapping("com.alibaba.dubbo.rpc.RpcException"
//                                            ,"io.bsoa.service.rpc.RpcException");
    }

    /**
     * Encode byte [ ].
     *
     * @param obj the obj
     * @return the byte [ ]
     */
    @Override
    public byte[] encode(Object obj) {
        UnsafeByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(1024);
        Hessian2Output mH2o = new Hessian2Output(bos);
        mH2o.setSerializerFactory(HessianSerializerFactory.SERIALIZER_FACTORY);
        try {
            if (obj instanceof RpcRequest) {

                encodeRequest((RpcRequest) obj, mH2o);
                mH2o.flushBuffer();
            } else if (obj instanceof RpcResponse) {
                encodeResponse((RpcResponse) obj, mH2o);
                mH2o.flushBuffer();
            } else {
                mH2o.writeObject(obj);
                mH2o.flushBuffer();
            }
        } catch (BsoaRpcException e) {
            throw e;
        } catch (Exception e) {
            throw new BsoaRpcException(22222, "Encode request error", e);
        }
        return bos.toByteArray();
    }

    /**
     * 编码发送给服务端的Request
     *
     * @param req the RpcRequest
     * @param out the Hessian2Output
     */
    private void encodeRequest(RpcRequest req, Hessian2Output out) throws IOException {
        Object[] args = req.getArgs();
        Class<?>[] pts = req.getArgClasses();
        out.writeString(ReflectUtils.getDesc(pts));
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                out.writeObject(args[i]);
            }
        }
        out.writeObject(req.getAttachments());
    }

    /**
     * 编码返回给客户端的response
     *
     * @param res the res
     * @param out the out
     * @throws IOException the iO exception
     */
    private void encodeResponse(RpcResponse res, AbstractHessianOutput out) throws IOException {
        // encode response data or error message.
        Throwable th = res.getException();
        if (th == null) {
            Object ret = res.getReturnData();
            if (ret == null) {
                out.writeInt(HessianConstants.RESPONSE_NULL);
            } else {
                out.writeInt(HessianConstants.RESPONSE_DATA);
                out.writeObject(ret);
            }
        } else {
            out.writeInt(HessianConstants.RESPONSE_EXCEPTION);
            out.writeObject(th);
        }
    }

    @Override
    public Object decode(byte[] data, Class clazz) {
        InputStream is = new UnsafeByteArrayInputStream(data);
        AbstractHessianInput mH2i = new Hessian2Input(is);
        mH2i.setSerializerFactory(HessianSerializerFactory.SERIALIZER_FACTORY);

        Object obj;
        try {
            if (clazz == null) {
                obj = mH2i.readObject(); // 无需依赖class
            } else if (clazz == RpcRequest.class) {
                obj = decodeRequest(mH2i, new RpcRequest());
            } else if (clazz == RpcResponse.class) {
                obj = decodeResponse(mH2i, new RpcResponse());
            } else {
                obj = mH2i.readObject(); // 无需依赖class
            }
        } catch (Exception e) {
            throw new BsoaRpcException(22222, "Decode error", e);
        }
        return obj;
    }


    @Override
    public Object decode(byte[] data, Object template) {
        InputStream is = new UnsafeByteArrayInputStream(data);
        AbstractHessianInput mH2i = new Hessian2Input(is);
        mH2i.setSerializerFactory(HessianSerializerFactory.SERIALIZER_FACTORY);

        Object obj;
        try {
            if (template == null) {
                obj = mH2i.readObject(); // 无需依赖class
            } else if (template instanceof RpcRequest) {
                obj = decodeRequest(mH2i, (RpcRequest) template);
            } else if (template instanceof RpcResponse) {
                obj = decodeResponse(mH2i, (RpcResponse) template);
            } else {
                obj = mH2i.readObject(); // 无需依赖class
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
    private RpcRequest decodeRequest(AbstractHessianInput input, RpcRequest req) throws IOException {
        try {
            Object[] args;
            Class<?>[] pts;
            String desc = input.readString();
            if (StringUtils.isEmpty(desc)) {
                pts = EMPTY_CLASS_ARRAY;
                args = EMPTY_OBJECT_ARRAY;
            } else {
                pts = ReflectUtils.desc2classArray(desc);
                args = new Object[pts.length];
                for (int i = 0; i < args.length; i++) {
                    try {
                        args[i] = input.readObject(pts[i]);
                    } catch (Exception e) {
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn("Decode argument failed: " + e.getMessage(), e);
                        }
                    }
                }
            }
            Map<String, Object> map = (Map<String, Object>) input.readObject(Map.class);
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
    private RpcResponse decodeResponse(AbstractHessianInput in, RpcResponse res) throws IOException {
        byte code = (byte) in.readInt();
        switch (code) {
            case HessianConstants.RESPONSE_NULL:
                break;
            case HessianConstants.RESPONSE_DATA:
                res.setReturnData(in.readObject());
                break;
            case HessianConstants.RESPONSE_EXCEPTION:
                res.setException((Throwable) in.readObject());
                break;
            default:
                break;
        }
        return res;
    }
}
