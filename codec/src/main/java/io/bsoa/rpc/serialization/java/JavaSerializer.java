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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.codec.Serializer;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.message.RpcRequest;
import io.bsoa.rpc.message.RpcResponse;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/24 21:09. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("java")
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
        } catch (IOException e) {
            LOGGER.error("Encoder error:", e);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    LOGGER.warn("", e);
                }
            }
        }
        return new byte[0];
    }

    private void encodeRequest(RpcRequest req, ObjectOutputStream out) throws IOException {
        writeObject(req, out);
    }

    private void encodeResponse(RpcResponse res, ObjectOutputStream out) throws IOException {
        writeObject(res, out);
    }


    private void writeUTF(String v, ObjectOutputStream oos) throws IOException {
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

    private String readUTF(ObjectInputStream ois) throws IOException {
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
    public Object decode(byte[] datas, Class clazz) {
        UnsafeByteArrayInputStream bais = new UnsafeByteArrayInputStream(datas);
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(bais);
            if (RpcRequest.class == clazz) {
                return this.readObject(ois);
            } else if (RpcResponse.class == clazz) {
                return this.readObject(ois);
            } else {
                return this.readObject(ois);
            }
        } catch (Exception e) {
            LOGGER.error("Decode error", e);
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    LOGGER.warn("", e);
                }
            }
        }
        return null;
    }

    @Override
    public byte getCode() {
        return 3;
    }
}
