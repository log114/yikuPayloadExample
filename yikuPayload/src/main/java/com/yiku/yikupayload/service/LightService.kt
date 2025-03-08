package com.yiku.yikupayload.service

import android.util.Log
import com.yiku.yikupayload.protocol.FETCH_TEMPERATURE
import com.yiku.yikupayload.protocol.LUMINANCE_CHANGE
import com.yiku.yikupayload.protocol.OPEN_CLOSE_LIGHT
import com.yiku.yikupayload.protocol.RED_BLUE_FLASHES
import com.yiku.yikupayload.protocol.SHARP_FLASH
import com.yiku.yikupayload.protocol.TRIPOD_HEAD
import com.yiku.yikupayload.util.LightHost
import com.yiku.yikupayload.util.Msg
import com.yiku.yikupayload.util.MsgCallback
import com.yiku.yikupayload.util.Short2ByteArray
import com.yiku.yikupayload.util.bytesToHex
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.ArrayList
import java.util.Date
import kotlin.concurrent.thread

class LightService {
    private val TAG = "LightService"

    var msgCallbacks: List<MsgCallback> = ArrayList()

    private var globalTid = 0L

    private val port = 8519
    private var client: Socket? = null
    private var out: OutputStream? = null
    private var inputStream: InputStream? = null
    private var isConnected = false
    private var host = ""

    open fun setIp(ip: String) {
        host = ip
    }
    open fun getIp(): String {
        return host
    }

    open fun getIsConnected(): Boolean {
        return isConnected
    }

    open fun registMsgCallback(msgCallback: MsgCallback) {
        this.msgCallbacks += msgCallback
    }

    open fun reConnect(): Boolean {
        Log.i(TAG, "Reconnect....")
        isConnected = false
        if (out != null) {
            out?.close()
        }
        if (inputStream != null) {
            inputStream?.close()
        }
        if (client != null && client?.isConnected == true) {
            client?.close()
        }
        return connect()
    }

    open fun connect(): Boolean {
        //开启一个链接，需要指定地址和端口
        return try {
            if(host == null || host == ""){
                host = LightHost
            }
            client = Socket(host, port)
            out = client?.getOutputStream()
            Log.i(TAG, "探照灯连接成功")
            isConnected = true
            inputStream = client?.getInputStream()
            globalTid = Date().time
            thread {
                try {
                    val vTid = globalTid

                    Log.i(TAG, "recv start...")
                    isConnected = true
                    while (client?.isConnected == true) {
                        if (vTid != globalTid) {
                            break
                        }
                        val recv = ByteArray(1024)
                        inputStream?.read(recv)
                        if (recv.isEmpty()) {
                            continue
                        }
//                    Log.i(TAG, "recv:${String(recv)}")
                        for (msgCallback in msgCallbacks) {
                            msgCallback.onMsg(recv)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
            true
        } catch (e: Exception) {
            isConnected = false
//            Log.i(TAG, "灯连接失败，ip: ${host}，端口:${port}")
//            Log.e(TAG, "connect error:${e.message}")
//            e.printStackTrace()
//            showToast("连接失败")
            false
        }
    }

    open fun sendData2Payload(data: ByteArray): Int {
        thread {
            try {
                //向输出流中写入数据，传向服务端
                val firstTime = Date().time
                if (!isConnected || !client?.isConnected!!) {
                    Log.i(
                        TAG,
                        "重新连接: isConnected:${isConnected}"
                    )
                    reConnect()
                }
                Log.i(TAG, "耗时: ${Date().time - firstTime}")
                Log.i(TAG, "sendData:${bytesToHex(data)}")
                if (out == null) {
                    Log.i(TAG, "out is null")
                    return@thread
                }
                out?.write(data)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                Log.e(TAG, "传输失败，重试中...")
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
    open fun openLight(open: Int, query: Boolean) {
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
    open fun luminanceChange(lum: Int, query: Boolean) {
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
    open fun sharpFlash(open: Int, query: Boolean) {
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
    open fun fetchTemperature() {
        val msg = Msg()
        msg.msgId = FETCH_TEMPERATURE
        msg.payload = byteArrayOf()
        sendData2Payload(msg.getMsg())
    }

    open fun redBlueLedControl(model: Byte) {
        val msg = Msg()
        msg.msgId = RED_BLUE_FLASHES
        msg.payload = byteArrayOf(model)
        sendData2Payload(msg.getMsg())
    }

    /**
     * 云台控制
     */
    open fun gimbalControl(picth: Short, roll: Short, yaw: Short) {
        val msg = Msg()
        msg.msgId = TRIPOD_HEAD
        val sendData = ByteArray(6)
        val picthData = Short2ByteArray.short2Bytes(picth)
        val rollData = Short2ByteArray.short2Bytes(roll)
        val yawData = Short2ByteArray.short2Bytes(yaw)
        sendData[0] = picthData[0]
        sendData[1] = picthData[1]
        sendData[2] = rollData[0]
        sendData[3] = rollData[1]
        sendData[4] = yawData[0]
        sendData[5] = yawData[1]

        msg.payload = sendData
        sendData2Payload(msg.getMsg())
    }
}