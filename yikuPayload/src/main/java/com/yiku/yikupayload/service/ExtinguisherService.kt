package com.yiku.yikupayload.service

import android.util.Log
import com.yiku.yikupayload.protocol.EXTINGUISHER_OPERATE
import com.yiku.yikupayload.protocol.EXTINGUISHER_STATE_SEND
import com.yiku.yikupayload.util.ExtinguisherHost
import com.yiku.yikupayload.util.Msg
import com.yiku.yikupayload.util.MsgCallback
import com.yiku.yikupayload.util.bytesToHex
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.ArrayList
import kotlin.concurrent.thread

class ExtinguisherService {
    private val TAG = "ExtinguisherService"
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
        return isConnected && client.isConnected
    }
    open fun disConnect() {
        if(getIsConnected()) {
            isConnected = false
            client.close()
        }
    }

    open fun registMsgCallback(msgCallback: MsgCallback) {
        this.msgCallbacks += msgCallback
    }

    open fun connect(): Boolean {
        if(host == null || host == ""){
            host = ExtinguisherHost
        }
        //开启一个链接，需要指定地址和端口
        return try {
            Log.i(TAG, "灭火罐连接：$host")
            client = Socket(host, port)
            out = client.getOutputStream()
            Log.i(TAG, "灭火罐连接成功")
            isConnected = true
            inputStream = client.getInputStream()
            thread {
                Log.i(TAG, "recv start...")
                while (client.isConnected) {
                    val recv = ByteArray(1024)
                    inputStream?.read(recv)
                    if (recv.isEmpty()) {
                        continue
                    }
                    for (msgCallback in msgCallbacks) {
                        msgCallback.onMsg(recv)
                    }
                }
            }
            true
        } catch (e: Exception) {
            isConnected = false
            false
        }
    }


    open fun sendData2Payload(data: ByteArray) {
        thread {
            try {
                //向输出流中写入数据，传向服务端
                if (!getIsConnected()) {
                    return@thread
                }
                Log.i(TAG, "sendData:${bytesToHex(data)}")
                out?.write(data)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "灭火罐消息发送异常：$e")
//                sendData2Payload(data)
                isConnected = false
                client.close()
            }
        }
    }

    // 操作灭火罐开关，0关，1开
    fun operate(operateType: Int) {
        val msg = Msg();
        msg.msgId = EXTINGUISHER_OPERATE.toByte()
        msg.payload = ByteArray(4)
        msg.payload[0] = operateType.toByte()
        sendData2Payload(msg.getMsg())
    }

    // 发送心跳包
    fun heartbeat() {
        val msg = Msg();
        msg.msgId = EXTINGUISHER_STATE_SEND.toByte()
        msg.payload = ByteArray(4)
        sendData2Payload(msg.getMsg())
    }
}