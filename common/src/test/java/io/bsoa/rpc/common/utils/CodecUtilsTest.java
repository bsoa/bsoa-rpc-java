package io.bsoa.rpc.common.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by zhangg on 2016/12/25.
 */
public class CodecUtilsTest {
    @Test
    public void intToBytes() throws Exception {
        int s = 16777218; // =1*256*256*256+ 0*256*256 +  0*256 + 2
        byte[] bs = CodecUtils.intToBytes(s);
        Assert.assertEquals(bs[0], 1);
        Assert.assertEquals(bs[1], 0);
        Assert.assertEquals(bs[2], 0);
        Assert.assertEquals(bs[3], 2);
    }

    @Test
    public void bytesToInt() throws Exception {
        int s = CodecUtils.bytesToInt(new byte[]{1, 0, 0, 2});
        Assert.assertEquals(s, 16777218);
    }

    @Test
    public void short2bytes() throws Exception {
        short s = 258; // =1*256+2
        byte[] bs = CodecUtils.short2bytes(s);
        Assert.assertEquals(bs[0], 1);
        Assert.assertEquals(bs[1], 2);
    }

    @Test
    public void copyOf() throws Exception {
        byte[] bs = new byte[]{1, 2, 3, 5};
        byte[] cp = CodecUtils.copyOf(bs, 3);
        Assert.assertArrayEquals(cp, new byte[]{1, 2, 3});

        cp = CodecUtils.copyOf(bs, 5);
        Assert.assertArrayEquals(cp, new byte[]{1, 2, 3, 5, 0});
    }

    @Test
    public void parseHigh4Low4Bytes() throws Exception {
        byte b = 117; // = 7*16+5
        byte[] bs = CodecUtils.parseHigh4Low4Bytes(b);
        Assert.assertEquals(bs[0], 7);
        Assert.assertEquals(bs[1], 5);
    }

    @Test
    public void buildHigh4Low4Bytes() throws Exception {
        byte bs = CodecUtils.buildHigh4Low4Bytes((byte) 7, (byte) 5);
        Assert.assertEquals(bs, (byte) 117);
    }

    @Test
    public void parseHigh2Low6Bytes() throws Exception {
        byte b = 117; // = 1*64 + 53
        byte[] bs = CodecUtils.parseHigh2Low6Bytes(b);
        Assert.assertEquals(bs[0], 1);
        Assert.assertEquals(bs[1], 53);
    }

    @Test
    public void buildHigh2Low6Bytes() throws Exception {
        byte bs = CodecUtils.buildHigh2Low6Bytes((byte) 1, (byte) 53);
        Assert.assertEquals(bs, (byte) 117);
    }

}