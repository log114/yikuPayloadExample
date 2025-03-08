package com.yiku.yikupayload.service

import android.util.Log
import com.yiku.yikupayload.protocol.RESQME_LUNCH
import com.yiku.yikupayload.protocol.RESQME_SAFETY_SWITCH
import com.yiku.yikupayload.util.Msg
import com.yiku.yikupayload.util.MsgCallback
import com.yiku.yikupayload.util.ResqmeHost
import com.yiku.yikupayload.util.bytesToHex
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.ArrayList
import kotlin.concurrent.thread

class ResqmeService {
    private val TAG = "ResqmeService"
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
        isConnected = false
        client.close()
    }

    open fun registMsgCallback(msgCallback: MsgCallback) {
        this.msgCallbacks += msgCallback
    }

    open fun connect(): Boolean {
        if(host == null || host == ""){
            host = ResqmeHost
        }
        //开启一个链接，需要指定地址和端口
        return try {
            client = Socket(host, port)
            out = client.getOutputStream()
            Log.i(TAG, "破窗器连接成功")
            isConnected = true
            inputStream = client.getInputStream()
            thread {
                Log.i(TAG, "recv start...")
                try{
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
                catch (e: Exception) {
                    Log.e(TAG, e.toString());
                }
            }
            true
        } catch (e: Exception) {
            isConnected = false
//            showToast("连接失败")
            false
        }
    }

    open fun sendData2Payload(data: ByteArray) {
        thread {
            if (!getIsConnected()) {
                return@thread
            }
            try {
                //向输出流中写入数据，传向服务端
                Log.i(TAG, "sendData:${bytesToHex(data)}")
                out?.write(data)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "破窗器消息发送异常：$e")
//                sendData2Payload(data)
                isConnected = false
                client.close()
            }
        }
    }

    // index: 1：1号口，2：2号口，3：全部
    fun launch(index: Int) {
        val msg = Msg();
        msg.msgId = RESQME_LUNCH.toByte()
        msg.payload = ByteArray(4)
        msg.payload[0] = index.toByte()
        msg.payload[1] = 0x01.toByte()
        sendData2Payload(msg.getMsg())
        Thread.sleep(100)
    }

    // 打开、关闭安全开关
    fun safetySwitch(state: Boolean) {
        val msg = Msg();
        msg.msgId = RESQME_SAFETY_SWITCH.toByte()
        msg.payload = ByteArray(4)
        if(state) {
            msg.payload[0] = 0x01.toByte()
        }
        else {
            msg.payload[0] = 0x00.toByte()
        }
        sendData2Payload(msg.getMsg())
    }
}