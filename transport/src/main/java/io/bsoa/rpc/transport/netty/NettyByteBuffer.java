/*
 * *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  * <p>
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  * <p>
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package io.bsoa.rpc.transport.netty;

import io.bsoa.rpc.transport.ByteBufferHolder;
import io.netty.buffer.ByteBuf;

/**
 * <p></p>
 *
 * Created by zhangg on 2016/12/18 15:37. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class NettyByteBuffer implements ByteBufferHolder {

   ByteBuf byteBuf;

    public NettyByteBuffer(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    /*
    @Override
    public int capacity() {
        return byteBuf.capacity();
    }

    @Override
    public NettyByteBuffer capacity(int newCapacity) {
        return new NettyByteBuffer(byteBuf.capacity(newCapacity));
    }

    @Override
    public int maxCapacity() {
        return byteBuf.maxCapacity();
    }

    @Override
    public ByteBufAllocator alloc() {
        return byteBuf.alloc();
    }

    @Override
    public ByteOrder order() {
        return byteBuf.order();
    }

    @Override
    public ByteBuf order(ByteOrder endianness) {
        return null;
    }

    @Override
    public ByteBuf unwrap() {
        return null;
    }

    @Override
    public boolean isDirect() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public ByteBuf asReadOnly() {
        return null;
    }

    @Override
    public int readerIndex() {
        return 0;
    }

    @Override
    public ByteBuf readerIndex(int readerIndex) {
        return null;
    }

    @Override
    public int writerIndex() {
        return 0;
    }

    @Override
    public ByteBuf writerIndex(int writerIndex) {
        return null;
    }

    @Override
    public ByteBuf setIndex(int readerIndex, int writerIndex) {
        return null;
    }

    @Override
    public int readableBytes() {
        return 0;
    }

    @Override
    public int writableBytes() {
        return 0;
    }

    @Override
    public int maxWritableBytes() {
        return 0;
    }

    @Override
    public boolean isReadable() {
        return false;
    }

    @Override
    public boolean isReadable(int size) {
        return false;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean isWritable(int size) {
        return false;
    }

    @Override
    public ByteBuf clear() {
        return null;
    }

    @Override
    public ByteBuf markReaderIndex() {
        return null;
    }

    @Override
    public ByteBuf resetReaderIndex() {
        return null;
    }

    @Override
    public ByteBuf markWriterIndex() {
        return null;
    }

    @Override
    public ByteBuf resetWriterIndex() {
        return null;
    }

    @Override
    public ByteBuf discardReadBytes() {
        return null;
    }

    @Override
    public ByteBuf discardSomeReadBytes() {
        return null;
    }

    @Override
    public ByteBuf ensureWritable(int minWritableBytes) {
        return null;
    }

    @Override
    public int ensureWritable(int minWritableBytes, boolean force) {
        return 0;
    }

    @Override
    public boolean getBoolean(int index) {
        return false;
    }

    @Override
    public byte getByte(int index) {
        return 0;
    }

    @Override
    public short getUnsignedByte(int index) {
        return 0;
    }

    @Override
    public short getShort(int index) {
        return 0;
    }

    @Override
    public short getShortLE(int index) {
        return 0;
    }

    @Override
    public int getUnsignedShort(int index) {
        return 0;
    }

    @Override
    public int getUnsignedShortLE(int index) {
        return 0;
    }

    @Override
    public int getMedium(int index) {
        return 0;
    }

    @Override
    public int getMediumLE(int index) {
        return 0;
    }

    @Override
    public int getUnsignedMedium(int index) {
        return 0;
    }

    @Override
    public int getUnsignedMediumLE(int index) {
        return 0;
    }

    @Override
    public int getInt(int index) {
        return 0;
    }

    @Override
    public int getIntLE(int index) {
        return 0;
    }

    @Override
    public long getUnsignedInt(int index) {
        return 0;
    }

    @Override
    public long getUnsignedIntLE(int index) {
        return 0;
    }

    @Override
    public long getLong(int index) {
        return 0;
    }

    @Override
    public long getLongLE(int index) {
        return 0;
    }

    @Override
    public char getChar(int index) {
        return 0;
    }

    @Override
    public float getFloat(int index) {
        return 0;
    }

    @Override
    public double getDouble(int index) {
        return 0;
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst) {
        return null;
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst, int length) {
        return null;
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
        return null;
    }

    @Override
    public ByteBuf getBytes(int index, byte[] dst) {
        return null;
    }

    @Override
    public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
        return null;
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuffer dst) {
        return null;
    }

    @Override
    public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
        return null;
    }

    @Override
    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
        return 0;
    }

    @Override
    public int getBytes(int index, FileChannel out, long position, int length) throws IOException {
        return 0;
    }

    @Override
    public CharSequence getCharSequence(int index, int length, Charset charset) {
        return null;
    }

    @Override
    public ByteBuf setBoolean(int index, boolean value) {
        return null;
    }

    @Override
    public ByteBuf setByte(int index, int value) {
        return null;
    }

    @Override
    public ByteBuf setShort(int index, int value) {
        return null;
    }

    @Override
    public ByteBuf setShortLE(int index, int value) {
        return null;
    }

    @Override
    public ByteBuf setMedium(int index, int value) {
        return null;
    }

    @Override
    public ByteBuf setMediumLE(int index, int value) {
        return null;
    }

    @Override
    public ByteBuf setInt(int index, int value) {
        return null;
    }

    @Override
    public ByteBuf setIntLE(int index, int value) {
        return null;
    }

    @Override
    public ByteBuf setLong(int index, long value) {
        return null;
    }

    @Override
    public ByteBuf setLongLE(int index, long value) {
        return null;
    }

    @Override
    public ByteBuf setChar(int index, int value) {
        return null;
    }

    @Override
    public ByteBuf setFloat(int index, float value) {
        return null;
    }

    @Override
    public ByteBuf setDouble(int index, double value) {
        return null;
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src) {
        return null;
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src, int length) {
        return null;
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
        return null;
    }

    @Override
    public ByteBuf setBytes(int index, byte[] src) {
        return null;
    }

    @Override
    public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
        return null;
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuffer src) {
        return null;
    }

    @Override
    public int setBytes(int index, InputStream in, int length) throws IOException {
        return 0;
    }

    @Override
    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
        return 0;
    }

    @Override
    public int setBytes(int index, FileChannel in, long position, int length) throws IOException {
        return 0;
    }

    @Override
    public ByteBuf setZero(int index, int length) {
        return null;
    }

    @Override
    public int setCharSequence(int index, CharSequence sequence, Charset charset) {
        return 0;
    }

    @Override
    public boolean readBoolean() {
        return false;
    }

    @Override
    public byte readByte() {
        return 0;
    }

    @Override
    public short readUnsignedByte() {
        return 0;
    }

    @Override
    public short readShort() {
        return 0;
    }

    @Override
    public short readShortLE() {
        return 0;
    }

    @Override
    public int readUnsignedShort() {
        return 0;
    }

    @Override
    public int readUnsignedShortLE() {
        return 0;
    }

    @Override
    public int readMedium() {
        return 0;
    }

    @Override
    public int readMediumLE() {
        return 0;
    }

    @Override
    public int readUnsignedMedium() {
        return 0;
    }

    @Override
    public int readUnsignedMediumLE() {
        return 0;
    }

    @Override
    public int readInt() {
        return 0;
    }

    @Override
    public int readIntLE() {
        return 0;
    }

    @Override
    public long readUnsignedInt() {
        return 0;
    }

    @Override
    public long readUnsignedIntLE() {
        return 0;
    }

    @Override
    public long readLong() {
        return 0;
    }

    @Override
    public long readLongLE() {
        return 0;
    }

    @Override
    public char readChar() {
        return 0;
    }

    @Override
    public float readFloat() {
        return 0;
    }

    @Override
    public double readDouble() {
        return 0;
    }

    @Override
    public ByteBuf readBytes(int length) {
        return null;
    }

    @Override
    public ByteBuf readSlice(int length) {
        return null;
    }

    @Override
    public ByteBuf readRetainedSlice(int length) {
        return null;
    }

    @Override
    public ByteBuf readBytes(ByteBuf dst) {
        return null;
    }

    @Override
    public ByteBuf readBytes(ByteBuf dst, int length) {
        return null;
    }

    @Override
    public ByteBuf readBytes(ByteBuf dst, int dstIndex, int length) {
        return null;
    }

    @Override
    public ByteBuf readBytes(byte[] dst) {
        return null;
    }

    @Override
    public ByteBuf readBytes(byte[] dst, int dstIndex, int length) {
        return null;
    }

    @Override
    public ByteBuf readBytes(ByteBuffer dst) {
        return null;
    }

    @Override
    public ByteBuf readBytes(OutputStream out, int length) throws IOException {
        return null;
    }

    @Override
    public int readBytes(GatheringByteChannel out, int length) throws IOException {
        return 0;
    }

    @Override
    public CharSequence readCharSequence(int length, Charset charset) {
        return null;
    }

    @Override
    public int readBytes(FileChannel out, long position, int length) throws IOException {
        return 0;
    }

    @Override
    public ByteBuf skipBytes(int length) {
        return null;
    }

    @Override
    public ByteBuf writeBoolean(boolean value) {
        return null;
    }

    @Override
    public ByteBuf writeByte(int value) {
        return null;
    }

    @Override
    public ByteBuf writeShort(int value) {
        return null;
    }

    @Override
    public ByteBuf writeShortLE(int value) {
        return null;
    }

    @Override
    public ByteBuf writeMedium(int value) {
        return null;
    }

    @Override
    public ByteBuf writeMediumLE(int value) {
        return null;
    }

    @Override
    public ByteBuf writeInt(int value) {
        return null;
    }

    @Override
    public ByteBuf writeIntLE(int value) {
        return null;
    }

    @Override
    public ByteBuf writeLong(long value) {
        return null;
    }

    @Override
    public ByteBuf writeLongLE(long value) {
        return null;
    }

    @Override
    public ByteBuf writeChar(int value) {
        return null;
    }

    @Override
    public ByteBuf writeFloat(float value) {
        return null;
    }

    @Override
    public ByteBuf writeDouble(double value) {
        return null;
    }

    @Override
    public ByteBuf writeBytes(ByteBuf src) {
        return null;
    }

    @Override
    public ByteBuf writeBytes(ByteBuf src, int length) {
        return null;
    }

    @Override
    public ByteBuf writeBytes(ByteBuf src, int srcIndex, int length) {
        return null;
    }

    @Override
    public ByteBuf writeBytes(byte[] src) {
        return null;
    }

    @Override
    public ByteBuf writeBytes(byte[] src, int srcIndex, int length) {
        return null;
    }

    @Override
    public ByteBuf writeBytes(ByteBuffer src) {
        return null;
    }

    @Override
    public int writeBytes(InputStream in, int length) throws IOException {
        return 0;
    }

    @Override
    public int writeBytes(ScatteringByteChannel in, int length) throws IOException {
        return 0;
    }

    @Override
    public int writeBytes(FileChannel in, long position, int length) throws IOException {
        return 0;
    }

    @Override
    public ByteBuf writeZero(int length) {
        return null;
    }

    @Override
    public int writeCharSequence(CharSequence sequence, Charset charset) {
        return 0;
    }

    @Override
    public int indexOf(int fromIndex, int toIndex, byte value) {
        return 0;
    }

    @Override
    public int bytesBefore(byte value) {
        return 0;
    }

    @Override
    public int bytesBefore(int length, byte value) {
        return 0;
    }

    @Override
    public int bytesBefore(int index, int length, byte value) {
        return 0;
    }

    @Override
    public ByteBuf copy() {
        return null;
    }

    @Override
    public ByteBuf copy(int index, int length) {
        return null;
    }

    @Override
    public ByteBuf slice() {
        return null;
    }

    @Override
    public ByteBuf retainedSlice() {
        return null;
    }

    @Override
    public ByteBuf slice(int index, int length) {
        return null;
    }

    @Override
    public ByteBuf retainedSlice(int index, int length) {
        return null;
    }

    @Override
    public ByteBuf duplicate() {
        return byteBuf.duplicate();
    }

    @Override
    public ByteBuf retainedDuplicate() {
        return byteBuf.retainedDuplicate();
    }

    @Override
    public int nioBufferCount() {
        return byteBuf.nioBufferCount();
    }

    @Override
    public boolean hasArray() {
        return byteBuf.hasArray();
    }

    @Override
    public byte[] array() {
        return byteBuf.array();
    }

    @Override
    public int arrayOffset() {
        return byteBuf.arrayOffset();
    }

    @Override
    public boolean hasMemoryAddress() {
        return byteBuf.hasMemoryAddress();
    }

    @Override
    public long memoryAddress() {
        return byteBuf.memoryAddress();
    }

    @Override
    public String toString(Charset charset) {
        return byteBuf.toString(charset);
    }

    @Override
    public String toString(int index, int length, Charset charset) {
        return byteBuf.toString(index, length, charset);
    }

    @Override
    public ByteBufferHolder retain(int increment) {
        return new NettyByteBuffer(byteBuf.retain(increment));
    }

    @Override
    public int refCnt() {
        return byteBuf.refCnt();
    }

    @Override
    public ByteBufferHolder retain() {
        return new NettyByteBuffer(byteBuf.retain());
    }

    @Override
    public ByteBufferHolder touch() {
        return new NettyByteBuffer(byteBuf.touch());
    }

    @Override
    public ByteBufferHolder touch(Object hint) {
        return new NettyByteBuffer(byteBuf.touch(hint));
    }

    @Override
    public boolean release() {
        return byteBuf.release();
    }

    @Override
    public boolean release(int decrement) {
        return byteBuf.release(decrement);
    }*/
}
