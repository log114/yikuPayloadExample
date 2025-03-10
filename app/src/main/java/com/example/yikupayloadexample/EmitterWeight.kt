package com.example.yikupayloadexample;

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.yiku.yikupayload_sdk.service.EmitterService
import com.yiku.yikupayload_sdk.util.MsgCallback
import java.lang.Exception
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

class EmitterWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "EmitterWeight"
    var emitterService: EmitterService
    private lateinit var mSafetySwitchSwitch: Switch
    private lateinit var mEmitterLaunch1Btn: Button
    private lateinit var mEmitterLaunch2Btn: Button
    private lateinit var mEmitterLaunch3Btn: Button
    private lateinit var mEmitterLaunch4Btn: Button
    private lateinit var mEmitterLaunch5Btn: Button
    private lateinit var mEmitterLaunch6Btn: Button
    private lateinit var mEmitterView: View
    private var isConnecting: Boolean = false
    private var isFirstConnect: Boolean = true

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        initView(context)
        emitterService = EmitterService()
        val host = preferences?.getString("EmitterHost", "")
        if(host != null && "" != host) {
            emitterService.setIp(host)
        }
        emitterService.msgCallbacks += object : MsgCallback {
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

        }
    }


    fun updateStatus(msg: ByteArray) {
        Log.i(TAG, "38mm发射器，更新状态msg:${msg.toHex()}")
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
            i++
        }
    }

    fun launch(index: Int) {
        Log.i(TAG, "mSafetySwitchSwitch.isChecked:${mSafetySwitchSwitch.isChecked}")
        try {
            if (!mSafetySwitchSwitch.isChecked) {
                showToast(R.string.need_to_open_safety_switch)
            } else {
                Log.i(TAG, "发射...")
                emitterService.launch(index)
                showToast(R.string.launch_command_executed)

                mSafetySwitchSwitch.isChecked = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(R.string.launch_failed)
        }
//        thread {
//            try {
//                Log.i(TAG, "获取状态")
//                Thread.sleep(1500)
//                emitterService.getStatus()
//            } catch (e: Exception) {
//                e.printStackTrace()
////                showToast("获取状态失败")
//            }
//        }
    }

    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.emitter_weight, this, true)
        mEmitterView = findViewById(R.id.emitter_view)
        mSafetySwitchSwitch = findViewById(R.id.emitterSafetySwitchSwitch)
        mEmitterLaunch1Btn = findViewById(R.id.emitterLaunch1Btn)
        mEmitterLaunch2Btn = findViewById(R.id.emitterLaunch2Btn)
        mEmitterLaunch3Btn = findViewById(R.id.emitterLaunch3Btn)
        mEmitterLaunch4Btn = findViewById(R.id.emitterLaunch4Btn)
        mEmitterLaunch5Btn = findViewById(R.id.emitterLaunch5Btn)
        mEmitterLaunch6Btn = findViewById(R.id.emitterLaunch6Btn)
        setConnectState()

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
        val connectText = findViewById<TextView>(R.id.emitterConnect)
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask() {
            override fun run() {
                if (emitterService.getIsConnected()) {
                    handler.post {
                        connectText.setText(R.string.connection_status_connected)
                    }
                    emitterService.getStatus()
                } else if(!isConnecting){
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
                        while (!emitterService.connect()) {
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