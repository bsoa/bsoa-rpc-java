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
package io.bsoa.rpc.compress.snappy;

import io.bsoa.rpc.codec.Compressor;
import io.bsoa.rpc.ext.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/12/24 21:09. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension(value = "snappy", code = 2)
public final class Snappy implements Compressor {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(Snappy.class);

    public Snappy() {
        LOGGER.info("Init Snappy compressor");
    }

    @Override
    public byte[] deCompress(byte[] src) {
        return uncompress(src, 0, src.length);
    }

    public int getUncompressedLength(byte[] compressed, int compressedOffset)
            throws CorruptionException {
        return SnappyDecompressor.getUncompressedLength(compressed, compressedOffset);
    }

    public byte[] uncompress(byte[] compressed, int compressedOffset, int compressedSize)
            throws CorruptionException {
        return SnappyDecompressor.uncompress(compressed, compressedOffset, compressedSize);
    }

    public int uncompress(byte[] compressed, int compressedOffset, int compressedSize, byte[] uncompressed, int uncompressedOffset)
            throws CorruptionException {
        return SnappyDecompressor.uncompress(compressed, compressedOffset, compressedSize, uncompressed, uncompressedOffset);
    }

    public int maxCompressedLength(int sourceLength) {
        return SnappyCompressor.maxCompressedLength(sourceLength);
    }

    public int compress(
            byte[] uncompressed,
            int uncompressedOffset,
            int uncompressedLength,
            byte[] compressed,
            int compressedOffset) {
        return SnappyCompressor.compress(uncompressed,
                uncompressedOffset,
                uncompressedLength,
                compressed,
                compressedOffset);
    }

    @Override
    public byte[] compress(byte[] data) {
        byte[] compressedOut = new byte[maxCompressedLength(data.length)];
        int compressedSize = compress(data, 0, data.length, compressedOut, 0);
        byte[] trimmedBuffer = Arrays.copyOf(compressedOut, compressedSize);
        return trimmedBuffer;
    }

    static final int LITERAL = 0;
    static final int COPY_1_BYTE_OFFSET = 1;  // 3 bit length + 3 bits of offset in opcode
    static final int COPY_2_BYTE_OFFSET = 2;
    static final int COPY_4_BYTE_OFFSET = 3;
}
