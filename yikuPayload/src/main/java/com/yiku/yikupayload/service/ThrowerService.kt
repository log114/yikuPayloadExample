package com.yiku.yikupayload.service

import android.util.Log
import com.yiku.yikupayload.protocol.THROWER_ALLOW_DETONATION
import com.yiku.yikupayload.protocol.THROWER_CHARGING
import com.yiku.yikupayload.protocol.THROWER_CONNECT_TEST
import com.yiku.yikupayload.protocol.THROWER_CONTROL_ALL
import com.yiku.yikupayload.protocol.THROWER_CONTROL_ONE
import com.yiku.yikupayload.protocol.THROWER_CONTROL_TWO_CENTER
import com.yiku.yikupayload.protocol.THROWER_CONTROL_TWO_LEFT
import com.yiku.yikupayload.protocol.THROWER_CONTROL_TWO_RIGHT
import com.yiku.yikupayload.protocol.THROWER_DETONATE_HEIGHT
import com.yiku.yikupayload.protocol.THROWER_UPDATE
import com.yiku.yikupayload.util.Msg
import com.yiku.yikupayload.util.MsgCallback
import com.yiku.yikupayload.util.ThrowerHost
import com.yiku.yikupayload.util.bytesToHex
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.ArrayList
import kotlin.concurrent.thread

class ThrowerService {
    private val TAG = "ThrowerService"
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

    open fun connect(): Boolean {
        if (host == null || host == "") {
            host = ThrowerHost
        }
        if (isConnected){
            return true
        }
        //开启一个链接，需要指定地址和端口
        return try {
            Log.i(TAG, "抛投连接")
            client = Socket(host, port)
            out = client.getOutputStream()
            Log.i(TAG, "抛投连接成功")
            isConnected = true
            inputStream = client.getInputStream()
            thread {
                Log.i(TAG, "recv start...")
                while (getIsConnected()) {
                    try {
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
                    } catch (e: Exception) {
                        Log.i(TAG, "抛投信息获取失败：$e")
                        e.printStackTrace()
                        false
                    }
                }
            }
            true
        } catch (e: Exception) {
            isConnected = false
            Log.i(TAG, "抛投连接失败，ip:${host}，端口：${port}，error:${e.message}")
            e.printStackTrace()
//            showToast("连接失败")
            false
        }
    }

    // 断连
    open fun disConnect() {
        if(getIsConnected()) {
            isConnected = false
            client.close()
        }
    }

    open fun sendData2Payload(data: ByteArray) {
        thread {
            try {
                //向输出流中写入数据，传向服务端
                if (!getIsConnected()) {
//                    throw Exception("未连接")
                    return@thread
                }
                Log.i(TAG, "sendData:${bytesToHex(data)}，时间：${System.currentTimeMillis()}")
                out?.write(data)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "抛投消息发送异常：$e")
                isConnected = false
                client.close()
            }
        }
    }

    // 舵机打开
    fun open(index: Int) {
        var _index = index
        if(_index >= 3) {
            _index += 2;
        }
        val msg = Msg()
        msg.msgId = THROWER_CONTROL_ONE.toByte()
        msg.payload = ByteArray(4)
        msg.payload[0] = _index.toByte()
        msg.payload[1] = 0x01.toByte()
        sendData2Payload(msg.getMsg())
    }

    // 舵机复位
    fun close(index: Int) {
        var _index = index
        if(_index >= 3) {
            _index += 2;
        }
        val msg = Msg()
        msg.msgId = THROWER_CONTROL_ONE.toByte()
        msg.payload = ByteArray(4)
        msg.payload[0] = _index.toByte()
        msg.payload[1] = 0x00.toByte()
        sendData2Payload(msg.getMsg())
    }

    // 打开中间俩舵机
    fun openCenter() {
        val msg = Msg()
        msg.msgId = THROWER_CONTROL_TWO_CENTER.toByte()
        msg.payload = ByteArray(4)
        msg.payload[0] = 0x01.toByte()
        sendData2Payload(msg.getMsg())
    }
    // 关闭中间俩舵机
    fun closeCenter() {
        val msg = Msg()
        msg.msgId = THROWER_CONTROL_TWO_CENTER.toByte()
        msg.payload = ByteArray(4)
        msg.payload[0] = 0x00.toByte()
        sendData2Payload(msg.getMsg())
    }

    // 打开左侧俩舵机
    fun openLeft() {
        val msg = Msg()
        msg.msgId = THROWER_CONTROL_TWO_LEFT.toByte()
        msg.payload = ByteArray(4)
        msg.payload[0] = 0x01.toByte()
        sendData2Payload(msg.getMsg())
    }
    // 关闭左侧俩舵机
    fun closeLeft() {
        val msg = Msg()
        msg.msgId = THROWER_CONTROL_TWO_LEFT.toByte()
        msg.payload = ByteArray(4)
        msg.payload[0] = 0x00.toByte()
        sendData2Payload(msg.getMsg())
    }

    // 打开右侧俩舵机
    fun openRight() {
        val msg = Msg()
        msg.msgId = THROWER_CONTROL_TWO_RIGHT.toByte()
        msg.payload = ByteArray(4)
        msg.payload[0] = 0x01.toByte()
        sendData2Payload(msg.getMsg())
    }
    // 关闭右侧俩舵机
    fun closeRight() {
        val msg = Msg()
        msg.msgId = THROWER_CONTROL_TWO_RIGHT.toByte()
        msg.payload = ByteArray(4)
        msg.payload[0] = 0x00.toByte()
        sendData2Payload(msg.getMsg())
    }

    // 关闭全部通道，每次关闭2个通道，间隔200毫秒
    fun closeAll() {
        val msg = Msg()
        msg.msgId = THROWER_CONTROL_ALL.toByte()
        msg.payload = ByteArray(4)
        msg.payload[0] = 0x00.toByte()
        sendData2Payload(msg.getMsg())
    }

    // 打开全部通道，每次打开2个通道，间隔200毫秒
    fun openAll() {
        val msg = Msg()
        msg.msgId = THROWER_CONTROL_ALL.toByte()
        msg.payload = ByteArray(4)
        msg.payload[0] = 0x01.toByte()
        sendData2Payload(msg.getMsg())

    }

    // 充电放电
    fun charging(switch: Boolean) {
        val msg = Msg()
        msg.msgId = THROWER_CHARGING.toByte()
        msg.payload = ByteArray(4)
        if(switch) {
            msg.payload[0] = 0x01.toByte()
        }
        else {
            msg.payload[0] = 0x00.toByte()
        }
        sendData2Payload(msg.getMsg())
    }

    // 允许起爆
    fun allowDetonation(switch: Boolean) {
        val msg = Msg()
        msg.msgId = THROWER_ALLOW_DETONATION.toByte()
        msg.payload = ByteArray(4)
        if(switch) {
            msg.payload[0] = 0x01.toByte()
        }
        else {
            msg.payload[0] = 0x00.toByte()
        }
        sendData2Payload(msg.getMsg())
    }

    // 连接测试
    fun connectionTesting() {
        val msg = Msg()
        msg.msgId = THROWER_CONNECT_TEST.toByte()
        msg.payload = ByteArray(4)
        sendData2Payload(msg.getMsg())
    }

    // 设置起爆高度
    fun setDetonateHeight(height: Int) {
        Log.i(TAG, "设置起爆高度：$height")
        val msg = Msg()
        msg.msgId = THROWER_DETONATE_HEIGHT.toByte()
        msg.payload = ByteArray(4)
        msg.payload[0] = height.toByte()
        sendData2Payload(msg.getMsg())
    }

    // 更新抛投板子程序
    fun throwerUpdate() {
        val msg = Msg()
        msg.msgId = THROWER_UPDATE.toByte()
        msg.payload = ByteArray(4)
        sendData2Payload(msg.getMsg())
    }
}