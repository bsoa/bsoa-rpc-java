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
package io.bsoa.rpc.transport.netty;

import io.bsoa.rpc.transport.AbstractByteBuf;
import io.netty.buffer.ByteBuf;

/**
 * <p>包装Netty的ByteBuf为Bsoa的AbstractByteBuf</p>
 * <p>
 * Created by zhangg on 2016/12/23 23:51. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class NettyByteBuf implements AbstractByteBuf {

    private final ByteBuf byteBuf;

    public NettyByteBuf(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    @Override
    public int readerIndex() {
        return byteBuf.readerIndex();
    }

    @Override
    public AbstractByteBuf readerIndex(int readerIndex) {
        byteBuf.readerIndex(readerIndex);
        return this;
    }

    @Override
    public int writerIndex() {
        return byteBuf.writerIndex();
    }

    @Override
    public AbstractByteBuf writerIndex(int writerIndex) {
        byteBuf.writerIndex(writerIndex);
        return this;
    }

    @Override
    public int readableBytes() {
        return byteBuf.readableBytes();
    }

    @Override
    public AbstractByteBuf setByte(int index, int value) {
        byteBuf.setByte(index, value);
        return this;
    }

    @Override
    public AbstractByteBuf setBytes(int index, byte[] src) {
        byteBuf.setBytes(index, src);
        return this;
    }

    @Override
    public boolean readBoolean() {
        return byteBuf.readBoolean();
    }

    @Override
    public byte readByte() {
        return byteBuf.readByte();
    }

    @Override
    public short readShort() {
        return byteBuf.readShort();
    }

    @Override
    public int readInt() {
        return byteBuf.readInt();
    }

    @Override
    public long readLong() {
        return byteBuf.readLong();
    }

    @Override
    public char readChar() {
        return byteBuf.readChar();
    }

    @Override
    public float readFloat() {
        return byteBuf.readFloat();
    }

    @Override
    public double readDouble() {
        return byteBuf.readDouble();
    }

    @Override
    public AbstractByteBuf readBytes(byte[] dst) {
        byteBuf.readBytes(dst);
        return this;
    }

    @Override
    public AbstractByteBuf writeBoolean(boolean value) {
        byteBuf.writeBoolean(value);
        return this;
    }

    @Override
    public AbstractByteBuf writeByte(int value) {
        byteBuf.writeByte(value);
        return this;
    }

    @Override
    public AbstractByteBuf writeShort(int value) {
        byteBuf.writeShort(value);
        return this;
    }

    @Override
    public AbstractByteBuf writeInt(int value) {
        byteBuf.writeInt(value);
        return this;
    }

    @Override
    public AbstractByteBuf writeLong(long value) {
        byteBuf.writeLong(value);
        return this;
    }

    @Override
    public AbstractByteBuf writeChar(int value) {
        byteBuf.writeChar(value);
        return this;
    }

    @Override
    public AbstractByteBuf writeFloat(float value) {
        byteBuf.writeFloat(value);
        return this;
    }

    @Override
    public AbstractByteBuf writeDouble(double value) {
        byteBuf.writeDouble(value);
        return this;
    }

    @Override
    public AbstractByteBuf writeBytes(byte[] src) {
        byteBuf.writeBytes(src);
        return this;
    }

    @Override
    public AbstractByteBuf slice(int index, int length) {
        ByteBuf newByteBuf = byteBuf.slice(index, length);
        return new NettyByteBuf(newByteBuf);
    }

    @Override
    public byte[] array() {
        return byteBuf.array();
    }

    @Override
    public AbstractByteBuf retain(int increment) {
        byteBuf.retain(increment);
        return this;
    }

    @Override
    public int refCnt() {
        return byteBuf.refCnt();
    }

    @Override
    public AbstractByteBuf retain() {
        byteBuf.retain();
        return this;
    }

    @Override
    public boolean release(int decrement) {
        return byteBuf.release(decrement);
    }

    @Override
    public boolean release() {
        return byteBuf.release();
    }
}
