package com.example.yikupayloadexample.util;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.LongByReference;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

public interface TradioLibrary extends Library {
    public static final String JNA_LIBRARY_NAME = "TradioCp";
    public static final NativeLibrary JNA_NATIVE_LIB = NativeLibrary.getInstance(TradioLibrary.JNA_LIBRARY_NAME);
    public static final TradioLibrary INSTANCE = (TradioLibrary)Native.loadLibrary(TradioLibrary.JNA_LIBRARY_NAME, TradioLibrary.class);
    /**
     * NET_TRADIO_SetChannelOption opt define<br>
     * <i>native declaration : Tradio.h:100</i><br>
     * enum values
     */
    public static interface OptionType {
        /** <i>native declaration : Tradio.h:100</i> */
        public static final int TRADIO_CHANNEL_OPT_ELIMINATE_TVVOICE = 0x0001;
    };
    /** <i>native declaration : Tradio.h</i> */
    public interface PRtpCallback extends Callback {
        void apply(Pointer data, int size, int channel, int db, int sample_rate, short seq, long cookie);
    };
    /** <i>native declaration : Tradio.h</i> */
    public interface PKeywordAlarmCallback extends Callback {
        void apply(Pointer keyword, int size, int channel, short fraction, short rtp_seq, long cookie);
    };
    /**
     * Original signature : <code>int NET_TRADIO_Init()</code><br>
     * <i>native declaration : Tradio.h:108</i>
     */
    int NET_TRADIO_Init();
    /**
     * Original signature : <code>int NET_TRADIO_DeviceReboot(const char*, int)</code><br>
     * <i>native declaration : Tradio.h:109</i><br>
     * @deprecated use the safer methods {@link #NET_TRADIO_DeviceReboot(String, int)} and {@link #NET_TRADIO_DeviceReboot(Pointer, int)} instead
     */
    @Deprecated
    int NET_TRADIO_DeviceReboot(Pointer ip, int port);
    /**
     * Original signature : <code>int NET_TRADIO_DeviceReboot(const char*, int)</code><br>
     * <i>native declaration : Tradio.h:109</i>
     */
    int NET_TRADIO_DeviceReboot(String ip, int port);
    /**
     * Original signature : <code>int NET_TRADIO_TestDevice(const char*, int, int, NET_TRADIO_DEVICEINFO*)</code><br>
     * <i>native declaration : Tradio.h:110</i><br>
     * @deprecated use the safer methods {@link #NET_TRADIO_TestDevice(String, int, int, tradio.NET_TRADIO_DEVICEINFO)} and {@link #NET_TRADIO_TestDevice(Pointer, int, int, tradio.NET_TRADIO_DEVICEINFO)} instead
     */
    @Deprecated
    int NET_TRADIO_TestDevice(Pointer ip, int port, int timeout, NET_TRADIO_DEVICEINFO info);
    /**
     * Original signature : <code>int NET_TRADIO_TestDevice(const char*, int, int, NET_TRADIO_DEVICEINFO*)</code><br>
     * <i>native declaration : Tradio.h:110</i>
     */
    int NET_TRADIO_TestDevice(String ip, int port, int timeout, NET_TRADIO_DEVICEINFO info);
    /**
     * Original signature : <code>int NET_TRADIO_CreateDevice(unsigned long long*)</code><br>
     * <i>native declaration : Tradio.h:111</i><br>
     * @deprecated use the safer methods {@link #NET_TRADIO_CreateDevice(LongBuffer)} and {@link #NET_TRADIO_CreateDevice(LongByReference)} instead
     */
    @Deprecated
    int NET_TRADIO_CreateDevice(LongByReference hd);
    /**
     * Original signature : <code>int NET_TRADIO_CreateDevice(unsigned long long*)</code><br>
     * <i>native declaration : Tradio.h:11 1</i>
     */
    int NET_TRADIO_CreateDevice(LongBuffer hd);
    /**
     * Original signature : <code>int NET_TRADIO_SetRtpCallback(unsigned long long, PRtpCallback, long long)</code><br>
     * <i>native declaration : Tradio.h:112</i>
     */
    int NET_TRADIO_SetRtpCallback(long hd, PRtpCallback cb, long cookie);
    /**
     * Original signature : <code>int NET_TRADIO_Login(unsigned long long, const char*, int, const char*, const char*, NET_TRADIO_DEVICEINFO*)</code><br>
     * <i>native declaration : Tradio.h:113</i><br>
     * @deprecated use the safer methods {@link #NET_TRADIO_Login(long, String, int, String, String, tradio.NET_TRADIO_DEVICEINFO)} and {@link #NET_TRADIO_Login(long, Pointer, int, Pointer, Pointer, tradio.NET_TRADIO_DEVICEINFO)} instead
     */
    @Deprecated
    int NET_TRADIO_Login(long hd, Pointer ip, int port, Pointer usr, Pointer pwd, NET_TRADIO_DEVICEINFO info);
    /**
     * Original signature : <code>int NET_TRADIO_Login(unsigned long long, const char*, int, const char*, const char*, NET_TRADIO_DEVICEINFO*)</code><br>
     * <i>native declaration : Tradio.h:113</i>
     */
    int NET_TRADIO_Login(long hd, String ip, int port, String usr, String pwd, NET_TRADIO_DEVICEINFO info);
    /**
     * Original signature : <code>int NET_TRADIO_SetKeywordAlarmCallback(unsigned long long, PKeywordAlarmCallback, long long)</code><br>
     * <i>native declaration : Tradio.h:114</i>
     */
    int NET_TRADIO_SetKeywordAlarmCallback(long hd, PKeywordAlarmCallback cb, long cookie);
    /**
     * Original signature : <code>int NET_TRADIO_SetDeviceTime(unsigned long long, long)</code><br>
     * <i>native declaration : Tradio.h:115</i>
     */
    int NET_TRADIO_SetDeviceTime(long hd, NativeLong now_time);
    /**
     * Original signature : <code>int NET_TRADIO_SetAdVolume(unsigned long long, int)</code><br>
     * <i>native declaration : Tradio.h:116</i>
     */
    int NET_TRADIO_SetAdVolume(long hd, int volume);
    /**
     * Original signature : <code>int NET_TRADIO_SetChannelOption(unsigned long long, int, int, int)</code><br>
     * <i>native declaration : Tradio.h:117</i>
     */
    int NET_TRADIO_SetChannelOption(long hd, int channelNo, int opt, int val);
    /**
     * Original signature : <code>int NET_TRADIO_StartRemoteRecordFile(unsigned long long, int, const char*)</code><br>
     * <i>native declaration : Tradio.h:118</i><br>
     * @deprecated use the safer methods {@link #NET_TRADIO_StartRemoteRecordFile(long, int, String)} and {@link #NET_TRADIO_StartRemoteRecordFile(long, int, Pointer)} instead
     */
    @Deprecated
    int NET_TRADIO_StartRemoteRecordFile(long hd, int channelNo, Pointer filename);
    /**
     * Original signature : <code>int NET_TRADIO_StartRemoteRecordFile(unsigned long long, int, const char*)</code><br>
     * <i>native declaration : Tradio.h:118</i>
     */
    int NET_TRADIO_StartRemoteRecordFile(long hd, int channelNo, String filename);
    /**
     * Original signature : <code>int NET_TRADIO_StopRemoteRecordFile(unsigned long long, int, char*, int)</code><br>
     * <i>native declaration : Tradio.h:119</i><br>
     * @deprecated use the safer methods {@link #NET_TRADIO_StopRemoteRecordFile(long, int, ByteBuffer, int)} and {@link #NET_TRADIO_StopRemoteRecordFile(long, int, Pointer, int)} instead
     */
    @Deprecated
    int NET_TRADIO_StopRemoteRecordFile(long hd, int channelNo, Pointer filename, int size);
    /**
     * Original signature : <code>int NET_TRADIO_StopRemoteRecordFile(unsigned long long, int, char*, int)</code><br>
     * <i>native declaration : Tradio.h:119</i>
     */
    int NET_TRADIO_StopRemoteRecordFile(long hd, int channelNo, ByteBuffer filename, int size);
    /**
     * Original signature : <code>int NET_TRADIO_RemoteRecordFileName(unsigned long long, int, char*, int)</code><br>
     * <i>native declaration : Tradio.h:120</i><br>
     * @deprecated use the safer methods {@link #NET_TRADIO_RemoteRecordFileName(long, int, ByteBuffer, int)} and {@link #NET_TRADIO_RemoteRecordFileName(long, int, Pointer, int)} instead
     */
    @Deprecated
    int NET_TRADIO_RemoteRecordFileName(long hd, int channelNo, Pointer filename, int size);
    /**
     * Original signature : <code>int NET_TRADIO_RemoteRecordFileName(unsigned long long, int, char*, int)</code><br>
     * <i>native declaration : Tradio.h:120</i>
     */
    int NET_TRADIO_RemoteRecordFileName(long hd, int channelNo, ByteBuffer filename, int size);
    /**
     * Original signature : <code>int NET_TRADIO_Logout(unsigned long long)</code><br>
     * <i>native declaration : Tradio.h:121</i>
     */
    int NET_TRADIO_Logout(long hd);
    /**
     * Original signature : <code>int NET_TRADIO_ReleaseDevice(unsigned long long)</code><br>
     * <i>native declaration : Tradio.h:122</i>
     */
    int NET_TRADIO_ReleaseDevice(long hd);
    /**
     * Original signature : <code>int NET_TRADIO_Clear()</code><br>
     * <i>native declaration : Tradio.h:123</i>
     */
    int NET_TRADIO_Clear();
}
