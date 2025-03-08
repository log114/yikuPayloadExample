package com.yiku.yikupayload.service

import android.util.Log
import com.yiku.yikupayload.protocol.FETCH_TEMPERATURE
import com.yiku.yikupayload.protocol.LUMINANCE_CHANGE
import com.yiku.yikupayload.protocol.OPEN_CLOSE_LIGHT
import com.yiku.yikupayload.protocol.RED_BLUE_FLASHES
import com.yiku.yikupayload.protocol.SEROV_CONTROL
import com.yiku.yikupayload.protocol.SHARP_FLASH
import com.yiku.yikupayload.util.Msg
import com.yiku.yikupayload.util.VehiclePlatform
import com.yiku.yikupayload.util.YA3Host
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import kotlin.concurrent.thread

class FourInOneService : BaseMegaphoneService() {
    private val TAG = "FourInOneService";

    private val port = 8519

    private var client: Socket? = null
    private var out: OutputStream? = null
    private var inputStream: InputStream? = null
    private var isConnected = false
    private var host = ""

    override fun setIp(ip: String) {
        host = ip
    }
    override fun getIp(): String {
        return host
    }
    init {
        platform = VehiclePlatform.H16
        if(host == ""){
            host = YA3Host
        }
        setHost(host)
    }

    var parseIndex = 0;
    var recvData = ByteArray(128)
    var recvDataLast =  ByteArray(128)
    private fun parseByte(b: Byte): Boolean {
        when (parseIndex) {
            0 -> { // header
                if (b != 0x8d.toByte()) {
                    parseIndex = 0
                    recvData = ByteArray(128)
                    return false
                }
                recvData[0] = b
                parseIndex++
                return false

            }

            1 -> { //LEN
                recvData[1] = b
                parseIndex++
                return false

            }

            2 -> { // MSG_ID
                recvData[2] = b
                parseIndex++
                return false
            }

            else -> {
                return if (parseIndex >= recvData[1].toInt() + 4) {
                    parseIndex = 0
                    recvDataLast = recvData
                    recvData = ByteArray(128)
                    true
                } else {
                    recvData[parseIndex] = b
                    parseIndex++
                    false
                }
            }

        }
    }
    override fun connect(): Boolean {
        //开启一个链接，需要指定地址和端口
        return try {
            client = Socket(host, port)
            out = client!!.getOutputStream()
            Log.i(TAG, "四合一连接成功")
            isConnected = true
            inputStream = client!!.getInputStream()
            thread {
                try {
                    Log.i(TAG, "recv start...")
                    while (client!!.isConnected) {
                        val recv = ByteArray(1024)
                        val i = inputStream?.read(recv)
                        Log.i(TAG, "i:${i}")
                        if (i == 0) {
                            continue
                        }
                        Log.i(TAG, "===YA3Service recv:${String(recv)}")
                        val data = recv.slice(0 until i!!).toByteArray()
                        var tmp = ByteArray(0);

                        var n = 0;
                        data.forEach {
                            if (it.toInt().toChar() == '[' && tmp.isNotEmpty()) {
                                for (msgCallback in msgCallbacks) {
                                    msgCallback.onMsg(tmp)
                                }
                                tmp = ByteArray(0);
//                            n++
                                n = 0;
                            }
                            tmp += it
                            n++
                        }
                        if (String(recv).startsWith("GAF")) {
                            getAudioFilesCallback?.onResult(String(recv).substring(3))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            true
        } catch (e: Exception) {
            isConnected = false
//            Log.e(TAG, "connect error:${e.message}")
//            e.printStackTrace()
            false
        }
    }

    override fun sendData2Payload(data: ByteArray): Int {
        thread {
            try {
                //向输出流中写入数据，传向服务端
                if (client == null || !client!!.isConnected || !isConnected) {
                    connect()
                }
                Log.i(TAG, "SEND size:${data.size} data:${data.asList()}")
                out?.write(data)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                Log.e(TAG, "传输失败，重试中...")
                isConnected = false
                sendData2Payload(data)
            }
        }
        return 0

    }

    /**
     * 开关灯控制/查询
     * open 开关状态 0关 1开
     * query 是否查询状态
     */
    override fun openLight(open: Int, query: Boolean) {

        val msg = Msg()
        msg.msgId = OPEN_CLOSE_LIGHT
        if (!query) {
            msg.payload = byteArrayOf(open.toByte())
        } else {
            msg.payload = byteArrayOf()
        }
        sendData2Payload(msg.getMsg())
    }

    /**
     * 亮度调整
     *
     * lum 亮度值 0-100
     * query 是否为查询
     */
    override fun luminanceChange(lum: Int, query: Boolean) {
        val msg = Msg()
        msg.msgId = LUMINANCE_CHANGE
        if (!query) {
            msg.payload = byteArrayOf(lum.toByte())
        } else {
            msg.payload = byteArrayOf()
        }
        sendData2Payload(msg.getMsg())
    }

    /**
     * 爆闪
     * open 1 开 0 关
     */
    override fun sharpFlash(open: Int, query: Boolean) {
        val msg = Msg()
        msg.msgId = SHARP_FLASH
        if (!query) {
            msg.payload = byteArrayOf(open.toByte())
        } else {
            msg.payload = byteArrayOf()
        }
        sendData2Payload(msg.getMsg())
    }

    /**
     * 获取温度
     *
     */
    override fun fetchTemperature() {
        val msg = Msg()
        msg.msgId = FETCH_TEMPERATURE
        msg.payload = byteArrayOf()
        sendData2Payload(msg.getMsg())
    }

    override fun redBlueLedControl(model: Byte) {
        val msg = Msg()
        msg.msgId = RED_BLUE_FLASHES
        msg.payload = byteArrayOf(model)
        sendData2Payload(msg.getMsg())
    }

    /**
     * 探照灯舵机控制 舵机值范围100-200
     */
    override fun controlServo(cval: Int) {
        val msg = Msg();
        // 控制舵机
        msg.msgId = SEROV_CONTROL.toByte()
        msg.payload = ByteArray(2)

        msg.payload[0] = 0xFF.toByte()
        msg.payload[1] = cval.toByte()
        sendData2Payload(msg.getMsg())
    }

    override fun getIsConnectedYA3(): Boolean {
        return if (client == null) {
            false
        } else (client!!.isConnected && isConnected)
    }
}