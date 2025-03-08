package com.yiku.yikupayload.service

import android.util.Log
import com.yiku.yikupayload.protocol.EMITTER_GRAB_OR_RELEASE
import com.yiku.yikupayload.protocol.GRIPPER_RISE_OR_DECLINE
import com.yiku.yikupayload.protocol.GRIPPER_STOP
import com.yiku.yikupayload.util.GripperHost
import com.yiku.yikupayload.util.Msg
import com.yiku.yikupayload.util.MsgCallback
import com.yiku.yikupayload.util.bytesToHex
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.ArrayList
import kotlin.concurrent.thread

class GripperService {
    private val TAG = "GripperService"
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
        if(host == null || host == ""){
            host = GripperHost
        }
        //开启一个链接，需要指定地址和端口
        return try {
            client = Socket(host, port)
            out = client.getOutputStream()
            Log.i(TAG, "机械爪连接成功")
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
                Log.e(TAG, "机械爪消息发送异常：$e")
                isConnected = false
                client.close()
            }
        }
    }

    // 上升
    fun gripperRise() {
        val msg = Msg()
        msg.msgId = GRIPPER_RISE_OR_DECLINE.toByte()
        msg.payload = ByteArray(1)
        msg.payload[0] = 0x00.toByte()
        sendData2Payload(msg.getMsg())
    }
    // 下降
    fun gripperDecline() {
        val msg = Msg()
        msg.msgId = GRIPPER_RISE_OR_DECLINE.toByte()
        msg.payload = ByteArray(1)
        msg.payload[0] = 0x01.toByte()
        sendData2Payload(msg.getMsg())
    }
    // 抓取
    fun gripperGrab() {
        val msg = Msg()
        msg.msgId = EMITTER_GRAB_OR_RELEASE.toByte()
        msg.payload = ByteArray(1)
        msg.payload[0] = 0x01.toByte()
        sendData2Payload(msg.getMsg())
    }
    // 紧急制动
    fun gripperStop() {
        val msg = Msg()
        msg.msgId = GRIPPER_STOP.toByte()
        msg.payload = ByteArray(1)
        msg.payload[0] = 0x01.toByte()
        sendData2Payload(msg.getMsg())
    }
    // 松开
    fun gripperRelease() {
        val msg = Msg()
        msg.msgId = EMITTER_GRAB_OR_RELEASE.toByte()
        msg.payload = ByteArray(1)
        msg.payload[0] = 0x00.toByte()
        sendData2Payload(msg.getMsg())
    }
}