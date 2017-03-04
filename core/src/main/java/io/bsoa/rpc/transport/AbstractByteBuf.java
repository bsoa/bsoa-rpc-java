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
package io.bsoa.rpc.transport;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/23 23:50. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public interface AbstractByteBuf {

    /* public int capacity();

     public AbstractByteBuf capacity(int newCapacity);

     public int readerIndex();

     public AbstractByteBuf readerIndex(int readerIndex);

     public int writerIndex();
 */
    public AbstractByteBuf writerIndex(int writerIndex);
/*
    public AbstractByteBuf setIndex(int readerIndex, int writerIndex);

    public int readableBytes();

    public int writableBytes();

    public int maxWritableBytes();

    public boolean isReadable();

    public boolean isReadable(int size);

    public boolean isWritable();

    public boolean isWritable(int size);

    public AbstractByteBuf clear();

    public AbstractByteBuf markReaderIndex();

    public AbstractByteBuf resetReaderIndex();

    public AbstractByteBuf markWriterIndex();

    public AbstractByteBuf resetWriterIndex();

    public AbstractByteBuf discardReadBytes();

    public AbstractByteBuf discardSomeReadBytes();

    public AbstractByteBuf ensureWritable(int minWritableBytes);

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

    public AbstractByteBuf getBytes(int index, AbstractByteBuf dst);

    public AbstractByteBuf getBytes(int index, AbstractByteBuf dst, int length);

    public AbstractByteBuf getBytes(int index, AbstractByteBuf dst, int dstIndex, int length);

    public AbstractByteBuf getBytes(int index, byte[] dst);

    public AbstractByteBuf getBytes(int index, byte[] dst, int dstIndex, int length);

    public AbstractByteBuf getBytes(int index, OutputStream out, int length) throws IOException;

    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException;

    public int getBytes(int index, FileChannel out, long position, int length) throws IOException;

    public CharSequence getCharSequence(int index, int length, Charset charset);

    public AbstractByteBuf setBoolean(int index, boolean value);

    public AbstractByteBuf setByte(int index, int value);

    public AbstractByteBuf setShort(int index, int value);

    public AbstractByteBuf setShortLE(int index, int value);

    public AbstractByteBuf setMedium(int index, int value);

    public AbstractByteBuf setMediumLE(int index, int value);

    public AbstractByteBuf setInt(int index, int value);

    public AbstractByteBuf setIntLE(int index, int value);

    public AbstractByteBuf setLong(int index, long value);

    public AbstractByteBuf setLongLE(int index, long value);

    public AbstractByteBuf setChar(int index, int value);

    public AbstractByteBuf setFloat(int index, float value);

    public AbstractByteBuf setDouble(int index, double value);

    public AbstractByteBuf setBytes(int index, AbstractByteBuf src, int length);

    public AbstractByteBuf setBytes(int index, AbstractByteBuf src, int srcIndex, int length);

    public AbstractByteBuf setBytes(int index, byte[] src);

    public AbstractByteBuf setBytes(int index, byte[] src, int srcIndex, int length);

    public AbstractByteBuf setBytes(int index, AbstractByteBuf src);

    public int setBytes(int index, InputStream in, int length) throws IOException;

    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException;

    public int setBytes(int index, FileChannel in, long position, int length) throws IOException;

    public AbstractByteBuf setZero(int index, int length);

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

    public AbstractByteBuf readBytes(int length);

    public AbstractByteBuf readSlice(int length);

    public AbstractByteBuf readRetainedSlice(int length);

    public AbstractByteBuf readBytes(AbstractByteBuf dst, int length);

    public AbstractByteBuf readBytes(AbstractByteBuf dst, int dstIndex, int length);

    public AbstractByteBuf readBytes(byte[] dst);

    public AbstractByteBuf readBytes(byte[] dst, int dstIndex, int length);

    public AbstractByteBuf readBytes(AbstractByteBuf dst);

    public AbstractByteBuf readBytes(OutputStream out, int length) throws IOException;

    public int readBytes(GatheringByteChannel out, int length) throws IOException;

    public CharSequence readCharSequence(int length, Charset charset);

    public int readBytes(FileChannel out, long position, int length) throws IOException;

    public AbstractByteBuf skipBytes(int length);

    public AbstractByteBuf writeBoolean(boolean value);

    public AbstractByteBuf writeByte(int value);

    public AbstractByteBuf writeShort(int value);

    public AbstractByteBuf writeShortLE(int value);

    public AbstractByteBuf writeMedium(int value);

    public AbstractByteBuf writeMediumLE(int value);

    public AbstractByteBuf writeInt(int value);

    public AbstractByteBuf writeIntLE(int value);

    public AbstractByteBuf writeLong(long value);

    public AbstractByteBuf writeLongLE(long value);

    public AbstractByteBuf writeChar(int value);

    public AbstractByteBuf writeFloat(float value);

    public AbstractByteBuf writeDouble(double value);

    public AbstractByteBuf writeBytes(AbstractByteBuf src, int length);

    public AbstractByteBuf writeBytes(AbstractByteBuf src, int srcIndex, int length);

    public AbstractByteBuf writeBytes(byte[] src);

    public AbstractByteBuf writeBytes(byte[] src, int srcIndex, int length);

    public AbstractByteBuf writeBytes(AbstractByteBuf src);

    public int writeBytes(InputStream in, int length) throws IOException;

    public int writeBytes(ScatteringByteChannel in, int length) throws IOException;

    public int writeBytes(FileChannel in, long position, int length) throws IOException;

    public AbstractByteBuf writeZero(int length);

    public int writeCharSequence(CharSequence sequence, Charset charset);

    public int indexOf(int fromIndex, int toIndex, byte value);

    public int bytesBefore(byte value);

    public int bytesBefore(int length, byte value);

    public int bytesBefore(int index, int length, byte value);

    public AbstractByteBuf copy();

    public AbstractByteBuf copy(int index, int length);

    public AbstractByteBuf slice();

    public AbstractByteBuf retainedSlice();

    public AbstractByteBuf slice(int index, int length);

    public AbstractByteBuf retainedSlice(int index, int length);

    public AbstractByteBuf duplicate();

    public AbstractByteBuf retainedDuplicate();

    public int nioBufferCount();

    public AbstractByteBuf nioBuffer();

    public AbstractByteBuf nioBuffer(int index, int length);

    public AbstractByteBuf internalNioBuffer(int index, int length);

    public AbstractByteBuf[] nioBuffers();

    public AbstractByteBuf[] nioBuffers(int index, int length);

    public boolean hasArray(); */

    public byte[] array();

    /**
     * public int arrayOffset();
     * <p>
     * public boolean hasMemoryAddress();
     * <p>
     * public long memoryAddress();
     * <p>
     * public String toString(Charset charset);
     * <p>
     * public String toString(int index, int length, Charset charset);
     * <p>
     * public int hashCode();
     * <p>
     * public boolean equals(Object obj);
     * <p>
     * public int compareTo(AbstractByteBuf buffer);
     * <p>
     * public String toString();
     * <p>
     * public AbstractByteBuf retain(int increment);
     * <p>
     * public int refCnt();
     * <p>
     * public AbstractByteBuf retain();
     * <p>
     * public AbstractByteBuf touch();
     * <p>
     * public AbstractByteBuf touch(Object hint);
     * <p>
     * public boolean release(int decrement);
     */

    public boolean release();
}
