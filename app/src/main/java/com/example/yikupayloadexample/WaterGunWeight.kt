package com.example.yikupayloadexample

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.yiku.yikupayloadSDK.protocol.WATERGUN_STATE_RECEIVE
import com.yiku.yikupayloadSDK.service.WaterGunService
import com.yiku.yikupayloadSDK.util.MsgCallback
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

class WaterGunWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
        private val TAG = "ExtinguisherWeight"
        private lateinit var mLightView: View
        var waterGunService: WaterGunService
        private lateinit var mSafetySwitchSwitch: Switch
        private lateinit var mOpenState: TextView
        private lateinit var mOperateBtn: Button
        private var isConnecting: Boolean = false
        private var isFirstConnect: Boolean = true
        private var updateTime = Date().time
        // 0关，1开
        private var operate: Int = 1

        constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
        constructor(context: Context) : this(context, null, 0)

        init {
            waterGunService = WaterGunService()
            val host = preferences?.getString("WaterGunHost", "")
            if(host != null && "" != host) {
                waterGunService.setIp(host)
            }
            initView(context)
            waterGunService.registMsgCallback(object : MsgCallback {
                override fun getId(): String {
                    return "WaterGunServiceCallback"
                }

                override fun onMsg(msg: ByteArray) {
                    Log.i(TAG, "msg:${msg.toHex()}")
                    if (msg[0] != 0x8d.toByte()) {
                        return
                    }
                    if (msg[2] == WATERGUN_STATE_RECEIVE.toByte()) {
                        updateTime = Date().time
                        updateState(msg)
                    }
                }

            })
        }

        private fun updateState(msg: ByteArray) {
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                if (0x00 == msg[0 + 3].toInt()) {
                    mOpenState.setText(R.string.closed)
                    operate = 1
                } else {
                    mOpenState.setText(R.string.opened)
                    operate = 0
                }
            }
        }

        private fun initView(context: Context?) {
            LayoutInflater.from(context).inflate(R.layout.water_gun_weight, this, true)
            mLightView = findViewById(R.id.waterGun_view)
            mSafetySwitchSwitch = findViewById(R.id.safetySwitchSwitch)
            mOpenState = findViewById(R.id.openState)
            mOperateBtn = findViewById<Button>(R.id.operateBtn)
            setConnectState()

            mOperateBtn.setOnClickListener {
                try {
                    if(operate==1 && !mSafetySwitchSwitch.isChecked) {
                        showToast(R.string.need_to_open_safety_switch)
                        return@setOnClickListener
                    }
                    waterGunService.operate(operate)
                    mOperateBtn.setText( if(operate==1) R.string.opening else R.string.closing )
                    mOperateBtn.isEnabled = false
                    thread {
                        Thread.sleep(2000)
                        val handler = Handler(Looper.getMainLooper())
                        handler.post {
                            mOperateBtn.isEnabled = true
                            mOperateBtn.setText( if(operate==1) R.string.open else R.string.close )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast(R.string.operation_failed)
                }

            }
        }

        fun ByteArray.toHex(): String =
            joinToString(separator = "") { eachByte -> "%02x ".format(eachByte) }


        private fun showToast(msg: Int) {
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                Toast.makeText(
                    MApplication.applicationContext, msg, Toast.LENGTH_LONG
                ).show()
            }
        }

        // 定时器，判断连接状态
        private fun setConnectState(){
            val timer = Timer();
            val connectText = findViewById<TextView>(R.id.waterGunConnect)
            val handler = Handler(Looper.getMainLooper())
            val task = object : TimerTask(){
                override fun run() {
                    if(waterGunService.getIsConnected()){
                        if (Date().time - updateTime < 3000) {
                            handler.post {
                                connectText.setText(R.string.connection_status_connected)
                            }
                        }
                        waterGunService.heartbeat()
                        // 3秒没收到信息，显示未连接
                        if (Date().time - updateTime > 3000) {
                            handler.post {
                                connectText.setText(R.string.connection_status_notconnected)
                            }
                        }
                        // 如果超过10s没收到消息，主动断开连接，等待重连
                        if (Date().time - updateTime > 10000) {
                            // 断连
                            waterGunService.disConnect()
                        }
                    }
                    else if(!isConnecting){
                        isConnecting = true
                        handler.post {
                            connectText.setText(R.string.connection_status_notconnected)
                        }
                        // 尝试重连
                        thread {
                            if(!isFirstConnect) {
                                Thread.sleep(10000)
                            }
                            isFirstConnect = false
                            while (!waterGunService.connect()) {
                                Thread.sleep(1000)
                            }
                            isConnecting = false
                        }
                    }
                }
            }
            // 定时器，100毫秒后开始执行，每1秒执行一次
            timer.scheduleAtFixedRate(task, 100, 1000);
        }
}