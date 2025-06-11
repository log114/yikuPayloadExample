package com.example.yikupayloadexample.service

import android.os.Build
import android.util.Log
import com.yiku.yikupayloadSDK.util.MsgCallback
import com.yiku.yikupayloadSDK.util.WaterGunHost
import com.yiku.yikupayloadSDK.util.bytesToHex
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.ArrayList
import kotlin.concurrent.thread

open class GasMonitoringService {
    private val TAG = "GasMonitoringService";

    private val port = 8519

    private lateinit var client: Socket
    private var out: OutputStream? = null
    private var inputStream: InputStream? = null
    private var isConnected = false
    private var host = ""
    var msgCallbacks: List<MsgCallback> = ArrayList()

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
        if(host == ""){
            host = WaterGunHost
        }
        //开启一个链接，需要指定地址和端口
        return try {
            Log.i(TAG, "气体监测设备连接：$host")
            client = Socket(host, port)
            out = client.getOutputStream()
            Log.i(TAG, "气体监测设备连接成功")
            isConnected = true
            inputStream = client.getInputStream()
            thread {
                Log.i(TAG, "recv start...")
                try {
                    while (client.isConnected) {
                        val recv = ByteArray(1024)
                        val dataLength = inputStream?.read(recv)
                        if (dataLength == 0) {
                            continue
                        }
                        val data = recv.slice(0 until dataLength!!).toByteArray()
                        //                    Log.i(TAG, "recv:${String(recv)}")
                        for (msgCallback in msgCallbacks) {
                            msgCallback.onMsg(data)
                        }
                    }
                } catch (e: Exception) {
                    Log.i(TAG, "气体监测设备信息获取失败：$e")
                    e.printStackTrace()
                }
            }
            true
        } catch (e: Exception) {
            isConnected = false
//            Log.e(TAG, "connect error:${e.message}")
//            e.printStackTrace()
//            showToast("连接失败")
            false
        }
    }


    open fun sendData2Payload(data: ByteArray) {
        thread {
            try {
                Log.i(TAG, "气体监测设备，sendData:${bytesToHex(data)}")
                //向输出流中写入数据，传向服务端
                if (!getIsConnected()) {
                    return@thread
                }
//                Log.i(TAG, "sendData:${data.asList()}")
                Log.i(TAG, "sendData:${bytesToHex(data)}")
                out?.write(data)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "气体监测设备消息发送异常：$e")
//                sendData2Payload(data)
                isConnected = false
                client.close()
            }
        }
    }
}