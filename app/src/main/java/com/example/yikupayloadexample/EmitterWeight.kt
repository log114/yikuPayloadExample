package com.example.yikupayloadexample;

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.yiku.yikupayloadSDK.service.EmitterService
import com.yiku.yikupayloadSDK.util.MsgCallback
import java.lang.Exception
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

class EmitterWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "EmitterWeight"
    var emitterService: EmitterService
    private lateinit var connectText: TextView
    private lateinit var statusDot: View
    private lateinit var background: GradientDrawable
    private lateinit var mSafetySwitch: Switch
    private lateinit var mEmitterLaunch1Btn: Button
    private lateinit var mEmitterLaunch2Btn: Button
    private lateinit var mEmitterLaunch3Btn: Button
    private lateinit var mEmitterLaunch4Btn: Button
    private lateinit var mEmitterLaunch5Btn: Button
    private lateinit var mEmitterLaunch6Btn: Button
    private lateinit var mEmitterView: View
    private var isConnecting: Boolean = false
    private var isFirstConnect: Boolean = true
    private var updateTime = Date().time
    private var confirmPopup: PopupWindow? = null

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        initView(context)
        emitterService = EmitterService()
        val host = preferences?.getString("EmitterHost", "")
        val portStr = preferences?.getString("EmitterPort", "")
        if(host != null && "" != host) {
            emitterService.setIp(host)
        }
        if(portStr != null && "" != portStr) {
            val port = portStr.toIntOrNull()
            if(port != null) {
                emitterService.setPort(port)
            }
        }
        emitterService.registMsgCallback(object : MsgCallback {
            override fun getId(): String {
                return "EmitterWeightCallback"
            }

            override fun onMsg(msg: ByteArray) {
                Log.i(TAG, "38mm发射器msg:${msg.toHex()}")
                if (msg[0] != 0x8d.toByte()) {
                    return
                }
                if (msg[2] == 0x11.toByte()) {
                    Log.i(TAG, "recv 0x11!")
                    // 更新按钮状态
                    mEmitterView.post {
                        updateStatus(msg)
                    }
                }
            }

        })
    }


    fun updateStatus(msg: ByteArray) {
        Log.i(TAG, "38mm发射器，更新状态msg:${msg.toHex()}")
        updateTime = Date().time

        handler.post {
            connectText.setText(R.string.connection_status_connected)
            background.setColor(ContextCompat.getColor(context, R.color.green))
        }
        var i = 0
        while (i < 6) {
            val btn: Button = when (i) {
                0 -> mEmitterLaunch1Btn
                1 -> mEmitterLaunch4Btn
                2 -> mEmitterLaunch2Btn
                3 -> mEmitterLaunch5Btn
                4 -> mEmitterLaunch3Btn
                5 -> mEmitterLaunch6Btn
                else -> {
                    mEmitterLaunch1Btn
                }
            }
            if(!mSafetySwitch.isChecked) {
                btn.setText(R.string.not_detected)
                btn.isEnabled = false
            }
            else {
                if (msg[i + 3] == 0x00.toByte()) {
                    // 空仓
                    btn.setText(R.string.short_position)
                    btn.isEnabled = false
                }
                if (msg[i + 3] == 0x01.toByte()) {
                    // 在仓
                    btn.setText(R.string.launch)
                    btn.isEnabled = true
                }
                if (msg[i + 3] == 0x02.toByte()) {
                    // 发射中
                    btn.setText(R.string.launching)
                    btn.isEnabled = false
                }
                if (msg[i + 3] == 0x03.toByte()) {
                    // 卡住
                    btn.setText(R.string.stuck)
                    btn.isEnabled = false
                }
            }
            i++
        }
    }

    fun launch(index: Int) {
        Log.i(TAG, "mSafetySwitch.isChecked:${mSafetySwitch.isChecked}")
        try {
            if (!mSafetySwitch.isChecked) {
                showToast(R.string.need_to_open_safety_switch)
                return
            }
            showConfirmPopup(index)
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(R.string.launch_failed)
        }
    }

    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.emitter_weight, this, true)
        mEmitterView = findViewById(R.id.emitter_view)
        connectText = findViewById(R.id.emitterConnect)
        statusDot = findViewById(R.id.statusDot)
        mSafetySwitch = findViewById(R.id.emitterSafetySwitch)
        mEmitterLaunch1Btn = findViewById(R.id.emitterLaunch1Btn)
        mEmitterLaunch2Btn = findViewById(R.id.emitterLaunch2Btn)
        mEmitterLaunch3Btn = findViewById(R.id.emitterLaunch3Btn)
        mEmitterLaunch4Btn = findViewById(R.id.emitterLaunch4Btn)
        mEmitterLaunch5Btn = findViewById(R.id.emitterLaunch5Btn)
        mEmitterLaunch6Btn = findViewById(R.id.emitterLaunch6Btn)
        background = statusDot.background as GradientDrawable
        setConnectState()

        // 安全开关
        mSafetySwitch.setOnClickListener {
            emitterService.safetySwitch(mSafetySwitch.isChecked)
        }

        mEmitterLaunch1Btn.setOnClickListener {
            launch(0)
        }

        mEmitterLaunch2Btn.setOnClickListener {
            launch(2)
        }

        mEmitterLaunch3Btn.setOnClickListener {
            launch(4)

        }

        mEmitterLaunch4Btn.setOnClickListener {
            launch(1)

        }

        mEmitterLaunch5Btn.setOnClickListener {
            launch(3)

        }

        mEmitterLaunch6Btn.setOnClickListener {
            launch(5)
        }


    }

    private fun showConfirmPopup(index: Int) {
        // 如果已有弹窗，先关闭
        confirmPopup?.dismiss()

        val popupView = LayoutInflater.from(context).inflate(R.layout.popup_confirm, null)
        val confirmBtn = popupView.findViewById<Button>(R.id.popup_confirm)
        val cancelBtn = popupView.findViewById<Button>(R.id.popup_cancel)

        confirmBtn.setOnClickListener {
            Log.i(TAG, "发射...")
            emitterService.launch(index)
            showToast(R.string.launch_command_executed)
            confirmPopup?.dismiss()
        }
        cancelBtn.setOnClickListener {
            confirmPopup?.dismiss()
        }

        confirmPopup = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true // 可聚焦
        )
        // 设置背景模糊（可选）
        confirmPopup?.isOutsideTouchable = true
        confirmPopup?.isFocusable = true

        // 显示在 EmitterWeight 的中心
        confirmPopup?.showAtLocation(this, Gravity.CENTER, 0, 0)
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
    private fun setConnectState() {
        val timer = Timer();
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask() {
            override fun run() {
                if (emitterService.getIsConnected()) {
                    emitterService.getStatus()

                    if (Date().time - updateTime > 3000) {
                        val handler = Handler(Looper.getMainLooper())
                        handler.post {
                            connectText.setText(R.string.connection_status_notconnected)
                            background.setColor(ContextCompat.getColor(context, R.color.red))
                        }
                    }
                    // 如果超过5s没收到消息，主动断开连接，等待重连
                    if (Date().time - updateTime > 5000) {
                        // 断连
                        emitterService.disConnect()
                    }
                } else if(!isConnecting){
                    isConnecting = true
                    handler.post {
                        connectText.setText(R.string.connection_status_notconnected)
                        background.setColor(ContextCompat.getColor(context, R.color.red))
                    }
                    // 尝试重连
                    thread {
                        if(!isFirstConnect) {
                            Thread.sleep(5000)
                        }
                        isFirstConnect = false
                        while (!emitterService.connect()) {
                            Thread.sleep(1000)
                        }
                        updateTime = Date().time
                        isConnecting = false
                    }
                }
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.schedule(task, 100, 1000);
    }
}