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

import io.bsoa.rpc.transport.AbstractByteBuf;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/20 21:45. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public abstract class StreamMessage extends BaseMessage {

    /**
     * 帧Id
     */
    protected short frameId;

    /**
     * 是否最后一个
     */
    protected boolean EndOfStream;

    /**
     * 流的内容
     */
    protected AbstractByteBuf byteBuf;

    /**
     * Instantiates a new Stream message.
     *
     * @param messageType the message type
     */
    protected StreamMessage(byte messageType) {
        super(messageType);
    }

    /**
     * Gets frame id.
     *
     * @return the frame id
     */
    public short getFrameId() {
        return frameId;
    }

    /**
     * Sets frame id.
     *
     * @param frameId the frame id
     * @return the frame id
     */
    public StreamMessage setFrameId(short frameId) {
        this.frameId = frameId;
        return this;
    }

    /**
     * Is end of stream boolean.
     *
     * @return the boolean
     */
    public boolean isEndOfStream() {
        return EndOfStream;
    }

    /**
     * Sets end of stream.
     *
     * @param endOfStream the end of stream
     * @return the end of stream
     */
    public StreamMessage setEndOfStream(boolean endOfStream) {
        EndOfStream = endOfStream;
        return this;
    }

    /**
     * Gets byte buf.
     *
     * @return the byte buf
     */
    public AbstractByteBuf getByteBuf() {
        return byteBuf;
    }

    /**
     * Sets byte buf.
     *
     * @param byteBuf the byte buf
     * @return the byte buf
     */
    public StreamMessage setByteBuf(AbstractByteBuf byteBuf) {
        this.byteBuf = byteBuf;
        return this;
    }
}
