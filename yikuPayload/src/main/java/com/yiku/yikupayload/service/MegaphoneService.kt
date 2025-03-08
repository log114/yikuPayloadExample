package com.yiku.yikupayload.service

import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import kotlin.concurrent.thread
import com.yiku.yikupayload.util.ShoutHost
import com.yiku.yikupayload.util.VehiclePlatform


class MegaphoneService : BaseMegaphoneService() {
    private val TAG = "MegaphoneService";


    //    private val ip = "192.168.144.23"


    private val port = 8519

    //    private val ip = "10.10.62.61"
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
        if (host == "") {
            host = ShoutHost
        }
        setHost(host)

    }

    var parseIndex = 0;
    var recvData = ByteArray(128)
    var recvDataLast = ByteArray(128)
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
            Log.i(TAG, "喊话器连接成功")
            isConnected = true
            inputStream = client!!.getInputStream()
            thread {
                Log.i(TAG, "recv start...")
                while (client!!.isConnected) {
                    val recv = ByteArray(1024)
                    val i = inputStream?.read(recv)
                    if (i == 0) {
                        continue
                    }
                    Log.i(TAG, "recv:${String(recv)}")
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
                    if (tmp.isNotEmpty()) {
                        for (msgCallback in msgCallbacks) {
                            msgCallback.onMsg(tmp)
                        }

                    }
//                    data.forEach {
//                        run {
//                            if (parseByte(it)) {
//                                for (msgCallback in msgCallbacks) {
//                                    msgCallback.onMsg(recvDataLast)
//                                }
//                            }
//
//                        }
//                    }
                    if (String(recv).startsWith("GAF")) {
                        getAudioFilesCallback?.onResult(String(recv).substring(3))
                    }
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

    override fun sendData2Payload(data: ByteArray): Int {
        thread {
            try {
                //向输出流中写入数据，传向服务端
                if (client == null || !client!!.isConnected) {
                    if (!isConnected) {
                        connect()
                    }
                }
                Log.i(TAG, "SEND size:${data.size} data:${data.asList()}")
//                Log.i(TAG, "sendDataBeginTime:${Date()}")
                out?.write(data)
//                Log.i(TAG, "sendDataEndTime:${Date()}")
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                Log.e(TAG, "传输失败，重试中...")
                sendData2Payload(data)
            }
        }
        return 0

    }

    override fun getIsConnected(): Boolean {
        return if (client == null) {
            false
        } else (client!!.isConnected && isConnected)
    }
}