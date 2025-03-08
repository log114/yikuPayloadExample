package com.yiku.yikupayload.service

import android.util.Log
import cn.ykuav.payload.service.c.EMITTER_LUNCH
import cn.ykuav.payload.service.c.EMITTER_STATUS
import com.yiku.yikupayload.util.EmitterHost
import com.yiku.yikupayload.util.Msg
import com.yiku.yikupayload.util.MsgCallback
import com.yiku.yikupayload.util.bytesToHex
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread


open class EmitterService {

    private val TAG = "EmitterService"
    var msgCallbacks: List<MsgCallback> = ArrayList()

    private val port = 8519
    private lateinit var client: Socket
    private var out: OutputStream? = null
    private var inputStream: InputStream? = null
    private var isConnected = false
    private var host = ""

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

    open fun setIp(ip: String) {
        host = ip
    }
    open fun getIp(): String {
        return host
    }

    open fun connect(): Boolean {
        if(host == ""){
            host = EmitterHost
        }
        //开启一个链接，需要指定地址和端口
        return try {
            client = Socket(host, port)
            out = client.getOutputStream()
            Log.i(TAG, "38mm发射器连接成功")
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
            Log.e(TAG, "38mm连接失败，ip:${host}，error：${e.message}")
//            e.printStackTrace()
//            showToast("连接失败")
            false
        }
    }


    open fun sendData2Payload(data: ByteArray): Int {
        thread {
            try {
                //向输出流中写入数据，传向服务端
                if (!getIsConnected()) {
                    return@thread
                }
                Log.i(TAG, "sendData:${bytesToHex(data)}")
                out?.write(data)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                Log.e(TAG, "38mm消息发送异常：$e")
                isConnected = false
                client.close()
            }
        }
        return 0
    }

    fun getStatus() {
        val msg = Msg()
        msg.msgId = EMITTER_STATUS.toByte()
        msg.payload = ByteArray(0)
        sendData2Payload(msg.getMsg())
    }

    fun launch(index: Int) {
        val msg = Msg()
        msg.msgId = EMITTER_LUNCH.toByte()
        msg.payload = ByteArray(16)
        msg.payload[index] = 0x01.toByte()
        sendData2Payload(msg.getMsg())
    }
}