package com.yiku.yikupayload.util;

public class Short2ByteArray {

    public static Byte[] short2Bytes(Short x) {
        Byte[] bytes = new Byte[2];
        bytes[0] = (byte) (x & 0xff);
        bytes[1] = (byte) ((x >> 8) & 0xff);
        return bytes;
    }

}
