package com.example.yikupayloadexample.util;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class NET_TRADIO_DEVICEINFO extends Structure {
    /**
     * \u8bbe\u5907\u53f7<br>
     * C type : char[16]
     */
    public byte[] SerialNumber = new byte[16];
    /** \u8bbe\u5907\u7c7b\u578b, 1:\u5355\u58f0\u9053 2:\u7acb\u4f53\u58f0    3:8\u8def  4\uff1a\u5e26\u5b58\u50a88\u8def ...... */
    public short DeviceType;
    /** \u58f0\u97f3\u901a\u9053\u6570\u91cf 1 8 16 */
    public short ChannelNum;
    /** \u58f0\u97f3\u901a\u9053\u91cc\u97f3\u9891\u901a\u9053\u6570\u91cf */
    public short ChannelSubNum;
    /** \u8d77\u59cb\u901a\u9053\u53f7 1 */
    public short StartChannelNo;
    /** \u7f16\u7801\u7c7b\u578b  1:OGG    2:OGG_CELT  3:AAC */
    public short CodeType;
    /** \u91c7\u6837\u9891\u7387 */
    public int Frequency;
    /** \u91c7\u6837\u4f4d\u6570 */
    public short Bitrate;
    /** \u538b\u7f29\u6bd4 */
    public int Compressionratio;
    /** \u7801\u6d41\u5927\u5c0f */
    public int StreamSize;
    /** AD\u91c7\u6837\u97f3\u91cf */
    public int AD_Volume;
    /** \u786c\u76d8\u5bb9\u91cf */
    public int StorageCapacity;
    /** \u5269\u4f59\u5bb9\u91cf */
    public int StorageRemaining;
    /** \u652f\u6301\u964d\u566a */
    public short NoiseReductionEnable;
    /** \u652f\u6301EQ */
    public short EQEnable;
    /** \u964d\u566a\u5e45\u503c */
    public short NoiseReductionValume;
    /**
     * EQ\u503c<br>
     * C type : char[32]
     */
    public byte[] EQVal = new byte[32];
    /**
     * \u4fdd\u7559<br>
     * C type : char[16]
     */
    public byte[] Retention = new byte[16];
    public NET_TRADIO_DEVICEINFO() {
        super();
    }
    protected List<String> getFieldOrder() {
        return Arrays.asList("SerialNumber", "DeviceType", "ChannelNum", "ChannelSubNum", "StartChannelNo", "CodeType", "Frequency", "Bitrate", "Compressionratio", "StreamSize", "AD_Volume", "StorageCapacity", "StorageRemaining", "NoiseReductionEnable", "EQEnable", "NoiseReductionValume", "EQVal", "Retention");
    }
    public NET_TRADIO_DEVICEINFO(Pointer peer) {
        super(peer);
    }
    public static class ByReference extends NET_TRADIO_DEVICEINFO implements Structure.ByReference {

    };
    public static class ByValue extends NET_TRADIO_DEVICEINFO implements Structure.ByValue {

    };
}
