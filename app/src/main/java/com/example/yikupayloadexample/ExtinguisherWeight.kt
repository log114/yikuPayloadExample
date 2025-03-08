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
import com.yiku.yikupayload.protocol.EXTINGUISHER_STATE_RECEIVE
import com.yiku.yikupayload.service.ExtinguisherService
import com.yiku.yikupayload.util.MsgCallback
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

class ExtinguisherWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "ExtinguisherWeight"
    private lateinit var mLightView: View
    var extinguisherService: ExtinguisherService
    private lateinit var mSafetySwitchSwitch: Switch
    private lateinit var mOpenState: TextView
    private var isConnecting: Boolean = false
    private var isFirstConnect: Boolean = true
    private var updateTime = Date().time

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        extinguisherService = ExtinguisherService()
        val host = preferences?.getString("ExtinguisherHost", "")
        if(host != null && "" != host) {
            extinguisherService.setIp(host)
        }
        initView(context)
        extinguisherService.msgCallbacks += object : MsgCallback {
            override fun getId(): String {
                return "ExtinguisherServiceCallback"
            }

            override fun onMsg(msg: ByteArray) {
                Log.i(TAG, "msg:${msg.toHex()}")
                if (msg[0] != 0x8d.toByte()) {
                    return
                }
                if (msg[2] == EXTINGUISHER_STATE_RECEIVE.toByte()) {
                    updateTime = Date().time
                    updateState(msg)
                }
            }

        }
    }

    private fun updateState(msg: ByteArray) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            if (0x00 == msg[0 + 3].toInt()) {
                mOpenState.setText(R.string.closed)
            } else {
                mOpenState.setText(R.string.opened)
            }
        }
    }

    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.extinguisher_weight, this, true)
        mLightView = findViewById(R.id.extinguisher_view)
        mSafetySwitchSwitch = findViewById(R.id.safetySwitchSwitch)
        mOpenState = findViewById(R.id.openState)
        val mOperateBtn = findViewById<Button>(R.id.operateBtn)
        setConnectState()


        mOperateBtn.setOnClickListener {
            try {
                if (!mSafetySwitchSwitch.isChecked) {
                    showToast(R.string.need_to_open_safety_switch)
                } else {
                    extinguisherService.operate(1)// 开
                    mOperateBtn.setText(R.string.opening)
                    mOperateBtn.isEnabled = false
                    thread {
                        Thread.sleep(2000)
                        val handler = Handler(Looper.getMainLooper())
                        handler.post {
                            mOperateBtn.setText(R.string.open)
                            mOperateBtn.isEnabled = true
                        }
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
        val connectText = findViewById<TextView>(R.id.extinguisherConnect)
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask(){
            override fun run() {
                if(extinguisherService.getIsConnected()){
                    handler.post {
                        connectText.setText(R.string.connection_status_connected)
                    }
                    extinguisherService.heartbeat()
                    // 3秒没收到信息，显示未连接
                    if (Date().time - updateTime > 3000) {
                        handler.post {
                            connectText.setText(R.string.connection_status_notconnected)
                        }
                    }
                    // 如果超过10s没收到消息，主动断开连接，等待重连
                    if (Date().time - updateTime > 10000) {
                        // 断连
                        extinguisherService.disConnect()
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
                        while (!extinguisherService.connect()) {
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