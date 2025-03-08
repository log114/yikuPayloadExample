package com.yiku.yikupayload.service

import android.util.Log
import com.yiku.yikupayload.protocol.DESCENT_ACTION_CONTROL
import com.yiku.yikupayload.protocol.DESCENT_CONTROL
import com.yiku.yikupayload.protocol.DESCENT_URGENT_CONTROL
import com.yiku.yikupayload.util.Msg
import com.yiku.yikupayload.util.MsgCallback
import com.yiku.yikupayload.util.SlowDescentDeviceHost
import com.yiku.yikupayload.util.bytesToHex
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.ArrayList
import kotlin.concurrent.thread

open class SlowDescentDeviceService {
    private val TAG = "SlowDescentDeviceService"
    var msgCallbacks: List<MsgCallback> = ArrayList()
    private val port = 8519
    private lateinit var client: Socket
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

    fun connect(): Boolean {
        Log.i(TAG, "SlowDescentDeviceService   connect...")
        if(host == null || host == ""){
            host = SlowDescentDeviceHost
        }
        if (isConnected){
            return true
        }
        //开启一个链接，需要指定地址和端口
        return try {
            client = Socket(host, port)
            out = client.getOutputStream()
            Log.i(TAG, "缓降器连接成功")
            isConnected = true
            inputStream = client.getInputStream()
            thread {
                Log.i(TAG, "recv start...")
                try {
                    while (client.isConnected) {
                        val recv = ByteArray(1024)
                        val i = inputStream?.read(recv)
                        if (i == 0) {
                            continue
                        }
                        val data = recv.slice(0 until i!!).toByteArray()
//                    Log.i(TAG, "recv:${String()}")
                        data.forEach {
                            run {
                                if (parseByte(it)) {
                                    for (msgCallback in msgCallbacks) {
                                        msgCallback.onMsg(recvDataLast)
                                    }
                                }

                            }
                        }
                    }
                }
                catch (e: Exception) {
                    Log.e(TAG, e.toString())
                }
            }
            true
        } catch (e: Exception) {
            isConnected = false
            false
        }
    }


    open fun sendData2Payload(data: ByteArray): Int {
        thread {
            try {
                //向输出流中写入数据，传向服务端
                if (!isConnected || !client.isConnected) {
                    connect()
                }
                Log.i(TAG, "缓降器sendData:${bytesToHex(data)}")
                out?.write(data)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                Log.e(TAG, "传输失败，重试中...")
                sendData2Payload(data)
            }
        }
        return 0
    }
    // 缓降器使能控制
    fun descentControl(flag: Boolean) {
        val msg = Msg()
        msg.msgId = DESCENT_CONTROL.toByte()
        msg.payload = ByteArray(1)
        // Enable缓降器
        if(flag){
            msg.payload[0] = 0x01.toByte()
        }
        // Disable缓降器
        else {
            msg.payload[0] = 0x00.toByte()
        }
        sendData2Payload(msg.getMsg())
    }

    // 缓降器紧急控制
    fun emergencyControl(command: Int) {
        val msg = Msg()
        msg.msgId = DESCENT_URGENT_CONTROL.toByte()
        msg.payload = ByteArray(1)
        // 解除紧急状态（在发送了紧急停车或紧急熔断命令后，突然想取消停车和熔断的时候使用）
        when (command) {
            0 -> {
                msg.payload[0] = 0x00.toByte()
            }
            // 紧急停车
            1 -> {
                msg.payload[0] = 0x01.toByte()
            }
            // 紧急熔断
            2 -> {
                msg.payload[0] = 0x02.toByte()
            }
        }
        sendData2Payload(msg.getMsg())
    }

    /* 缓降器动作控制
    * mode: 0：按长度，1：按速度
    * speedOrLength: 速度或长度
    * */
    fun actionControl(mode: Int, speedOrLength: Int) {
        val msg = Msg()
        msg.msgId = DESCENT_ACTION_CONTROL.toByte()
        msg.payload = ByteArray(3)
        msg.payload[0] = mode.toByte()
        // 速度传值：0~40
        // 长度传值：0~300
        msg.payload[1] = (speedOrLength/256).toByte()
        msg.payload[2] = (speedOrLength%256).toByte()

        sendData2Payload(msg.getMsg())
    }
}