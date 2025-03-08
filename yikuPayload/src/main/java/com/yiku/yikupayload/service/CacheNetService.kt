package com.yiku.yikupayload.service

import android.util.Log
import com.yiku.yikupayload.protocol.CACHENET_SEROV_CONTROL
import com.yiku.yikupayload.util.CacheNetHost
import com.yiku.yikupayload.util.Msg
import com.yiku.yikupayload.util.MsgCallback
import com.yiku.yikupayload.util.bytesToHex
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread

open class BaseCacheNetService {

    private val TAG = "BaseCacheNetService"
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

    open fun registMsgCallback(msgCallback: MsgCallback) {
        this.msgCallbacks += msgCallback
    }

    open fun connect(): Boolean {
        if(host == ""){
            host = CacheNetHost
        }
        //开启一个链接，需要指定地址和端口
        return try {
            client = Socket(host, port)
            out = client.getOutputStream()
            Log.i(TAG, "捕捉网连接成功")
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
//                    Log.i(TAG, "recv:${String(recv)}")
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
                Log.e(TAG, "网枪消息发送异常：$e")
                isConnected = false
                client.close()
            }
        }
    }

    fun launch() {
        val msg = Msg();
        // 控制舵机
        msg.msgId = CACHENET_SEROV_CONTROL.toByte()
        msg.payload = ByteArray(1)
        msg.payload[0] = 0x01.toByte()
        sendData2Payload(msg.getMsg())
        Thread.sleep(100)
    }
}