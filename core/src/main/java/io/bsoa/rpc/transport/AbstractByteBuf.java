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
package io.bsoa.rpc.transport;

/**
 * <p></p>
 *
 * Created by zhangg on 2016/12/23 23:50. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public interface AbstractByteBuf {

   /* public int capacity();

    public ByteBufferHolder capacity(int newCapacity);

    public int readerIndex();

    public ByteBufferHolder readerIndex(int readerIndex);

    public int writerIndex();

    public ByteBufferHolder writerIndex(int writerIndex);

    public ByteBufferHolder setIndex(int readerIndex, int writerIndex);

    public int readableBytes();

    public int writableBytes();

    public int maxWritableBytes();

    public boolean isReadable();

    public boolean isReadable(int size);

    public boolean isWritable();

    public boolean isWritable(int size);

    public ByteBufferHolder clear();

    public ByteBufferHolder markReaderIndex();

    public ByteBufferHolder resetReaderIndex();

    public ByteBufferHolder markWriterIndex();

    public ByteBufferHolder resetWriterIndex();

    public ByteBufferHolder discardReadBytes();

    public ByteBufferHolder discardSomeReadBytes();

    public ByteBufferHolder ensureWritable(int minWritableBytes);

    public int ensureWritable(int minWritableBytes, boolean force);

    public boolean getBoolean(int index);

    public byte getByte(int index);

    public short getUnsignedByte(int index);

    public short getShort(int index);

    public short getShortLE(int index);

    public int getUnsignedShort(int index);

    public int getUnsignedShortLE(int index);

    public int getMedium(int index);

    public int getMediumLE(int index);

    public int getUnsignedMedium(int index);

    public int getUnsignedMediumLE(int index);

    public int getInt(int index);

    public int getIntLE(int index);

    public long getUnsignedInt(int index);

    public long getUnsignedIntLE(int index);

    public long getLong(int index);

    public long getLongLE(int index);

    public char getChar(int index);

    public float getFloat(int index);

    public double getDouble(int index);

    public ByteBufferHolder getBytes(int index, ByteBufferHolder dst);

    public ByteBufferHolder getBytes(int index, ByteBufferHolder dst, int length);

    public ByteBufferHolder getBytes(int index, ByteBufferHolder dst, int dstIndex, int length);

    public ByteBufferHolder getBytes(int index, byte[] dst);

    public ByteBufferHolder getBytes(int index, byte[] dst, int dstIndex, int length);

    public ByteBufferHolder getBytes(int index, OutputStream out, int length) throws IOException;

    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException;

    public int getBytes(int index, FileChannel out, long position, int length) throws IOException;

    public CharSequence getCharSequence(int index, int length, Charset charset);

    public ByteBufferHolder setBoolean(int index, boolean value);

    public ByteBufferHolder setByte(int index, int value);

    public ByteBufferHolder setShort(int index, int value);

    public ByteBufferHolder setShortLE(int index, int value);

    public ByteBufferHolder setMedium(int index, int value);

    public ByteBufferHolder setMediumLE(int index, int value);

    public ByteBufferHolder setInt(int index, int value);

    public ByteBufferHolder setIntLE(int index, int value);

    public ByteBufferHolder setLong(int index, long value);

    public ByteBufferHolder setLongLE(int index, long value);

    public ByteBufferHolder setChar(int index, int value);

    public ByteBufferHolder setFloat(int index, float value);

    public ByteBufferHolder setDouble(int index, double value);

    public ByteBufferHolder setBytes(int index, ByteBufferHolder src, int length);

    public ByteBufferHolder setBytes(int index, ByteBufferHolder src, int srcIndex, int length);

    public ByteBufferHolder setBytes(int index, byte[] src);

    public ByteBufferHolder setBytes(int index, byte[] src, int srcIndex, int length);

    public ByteBufferHolder setBytes(int index, ByteBufferHolder src);

    public int setBytes(int index, InputStream in, int length) throws IOException;

    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException;

    public int setBytes(int index, FileChannel in, long position, int length) throws IOException;

    public ByteBufferHolder setZero(int index, int length);

    public int setCharSequence(int index, CharSequence sequence, Charset charset);

    public boolean readBoolean();

    public byte readByte();

    public short readUnsignedByte();

    public short readShort();

    public short readShortLE();

    public int readUnsignedShort();

    public int readUnsignedShortLE();

    public int readMedium();

    public int readMediumLE();

    public int readUnsignedMedium();

    public int readUnsignedMediumLE();

    public int readInt();

    public int readIntLE();

    public long readUnsignedInt();

    public long readUnsignedIntLE();

    public long readLong();

    public long readLongLE();

    public char readChar();

    public float readFloat();

    public double readDouble();

    public ByteBufferHolder readBytes(int length);

    public ByteBufferHolder readSlice(int length);

    public ByteBufferHolder readRetainedSlice(int length);

    public ByteBufferHolder readBytes(ByteBufferHolder dst, int length);

    public ByteBufferHolder readBytes(ByteBufferHolder dst, int dstIndex, int length);

    public ByteBufferHolder readBytes(byte[] dst);

    public ByteBufferHolder readBytes(byte[] dst, int dstIndex, int length);

    public ByteBufferHolder readBytes(ByteBufferHolder dst);

    public ByteBufferHolder readBytes(OutputStream out, int length) throws IOException;

    public int readBytes(GatheringByteChannel out, int length) throws IOException;

    public CharSequence readCharSequence(int length, Charset charset);

    public int readBytes(FileChannel out, long position, int length) throws IOException;

    public ByteBufferHolder skipBytes(int length);

    public ByteBufferHolder writeBoolean(boolean value);

    public ByteBufferHolder writeByte(int value);

    public ByteBufferHolder writeShort(int value);

    public ByteBufferHolder writeShortLE(int value);

    public ByteBufferHolder writeMedium(int value);

    public ByteBufferHolder writeMediumLE(int value);

    public ByteBufferHolder writeInt(int value);

    public ByteBufferHolder writeIntLE(int value);

    public ByteBufferHolder writeLong(long value);

    public ByteBufferHolder writeLongLE(long value);

    public ByteBufferHolder writeChar(int value);

    public ByteBufferHolder writeFloat(float value);

    public ByteBufferHolder writeDouble(double value);

    public ByteBufferHolder writeBytes(ByteBufferHolder src, int length);

    public ByteBufferHolder writeBytes(ByteBufferHolder src, int srcIndex, int length);

    public ByteBufferHolder writeBytes(byte[] src);

    public ByteBufferHolder writeBytes(byte[] src, int srcIndex, int length);

    public ByteBufferHolder writeBytes(ByteBufferHolder src);

    public int writeBytes(InputStream in, int length) throws IOException;

    public int writeBytes(ScatteringByteChannel in, int length) throws IOException;

    public int writeBytes(FileChannel in, long position, int length) throws IOException;

    public ByteBufferHolder writeZero(int length);

    public int writeCharSequence(CharSequence sequence, Charset charset);

    public int indexOf(int fromIndex, int toIndex, byte value);

    public int bytesBefore(byte value);

    public int bytesBefore(int length, byte value);

    public int bytesBefore(int index, int length, byte value);

    public ByteBufferHolder copy();

    public ByteBufferHolder copy(int index, int length);

    public ByteBufferHolder slice();

    public ByteBufferHolder retainedSlice();

    public ByteBufferHolder slice(int index, int length);

    public ByteBufferHolder retainedSlice(int index, int length);

    public ByteBufferHolder duplicate();

    public ByteBufferHolder retainedDuplicate();

    public int nioBufferCount();

    public ByteBufferHolder nioBuffer();

    public ByteBufferHolder nioBuffer(int index, int length);

    public ByteBufferHolder internalNioBuffer(int index, int length);

    public ByteBufferHolder[] nioBuffers();

    public ByteBufferHolder[] nioBuffers(int index, int length);

    public boolean hasArray(); */

    public byte[] array();

    /**
    public int arrayOffset();

    public boolean hasMemoryAddress();

    public long memoryAddress();

    public String toString(Charset charset);

    public String toString(int index, int length, Charset charset);

    public int hashCode();

    public boolean equals(Object obj);

    public int compareTo(ByteBufferHolder buffer);

    public String toString();

    public ByteBufferHolder retain(int increment);

    public int refCnt();

    public ByteBufferHolder retain();

    public ByteBufferHolder touch();

    public ByteBufferHolder touch(Object hint);

    public boolean release(int decrement);*/

    public boolean release();
}
