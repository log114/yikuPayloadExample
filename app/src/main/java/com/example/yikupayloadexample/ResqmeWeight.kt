package com.example.yikupayloadexample

import android.content.Context
import android.graphics.Canvas
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
import com.yiku.yikupayload_sdk.protocol.RESQME_STATUS
import com.yiku.yikupayload_sdk.service.ResqmeService
import com.yiku.yikupayload_sdk.util.MsgCallback
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

class ResqmeWeight (context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {

    private val TAG = "ResqmeWeight"
    var resqmeService: ResqmeService = ResqmeService()
    private lateinit var mResqmeView: View
    private lateinit var connectText: TextView
    private lateinit var mSafetySwitchSwitch: Switch
    private lateinit var mResqmeLaunchBtn1: Button
    private lateinit var mResqmeLaunchBtn2: Button
    private lateinit var mResqmeLaunchAllBtn: Button
    private lateinit var mResqmeState1: TextView
    private lateinit var mResqmeState2: TextView
    private var isConnecting: Boolean = false
    private var isFirstConnect: Boolean = true
    private var updateTime = Date().time

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        val host = preferences?.getString("ResqmeHost", "")
        if(host != null && "" != host) {
            resqmeService.setIp(host)
        }
        initView(context)
        resqmeService.msgCallbacks += object : MsgCallback {
            override fun getId(): String {
                return "ResqmeWeightCallback"
            }

            override fun onMsg(msg: ByteArray) {
                Log.i(TAG, "破窗器msg:${msg.toHex()}")
                if (msg[0] != 0x8d.toByte()) {
                    return
                }
                if (msg[2] == RESQME_STATUS.toByte()) {
                    Log.i(TAG, "recv 0x31!")
                    updateTime = Date().time
                    // 更新破窗器状态
                    mResqmeView.post {
                        updateStatus(msg)
                    }
                }
            }

        }
    }

    fun updateStatus(msg: ByteArray) {
        // 如果没打开安全开关，状态显示为未检测
        if(!mSafetySwitchSwitch.isChecked) {
            mResqmeState1.setText(R.string.not_detected)
            mResqmeState2.setText(R.string.not_detected)
        }
        else {
            var i = 0
            while (i < 2) {

                val btn: Button = when (i) {
                    0 -> mResqmeLaunchBtn1
                    1 -> mResqmeLaunchBtn2
                    else -> {
                        mResqmeLaunchBtn1
                    }
                }
                val stateTextView: TextView = when (i) {
                    0 -> mResqmeState1
                    1 -> mResqmeState2
                    else -> {
                        mResqmeState1
                    }
                }
                if (msg[i + 3] == 0x00.toByte()) {
                    // 在仓
                    stateTextView.setText(R.string.can_be_launched)
                    btn.isEnabled = true
                }
                if (msg[i + 3] == 0x01.toByte()) {
                    // 空仓
                    stateTextView.setText(R.string.short_position)
                    btn.isEnabled = false
                }
                if (msg[i + 3] == 0x02.toByte()) {
                    // 卡住了
                    stateTextView.setText(R.string.stuck)
                    btn.isEnabled = false
                }
                i++
            }
        }
        handler.post {
            connectText.setText(R.string.connection_status_connected)
        }
    }

    fun ByteArray.toHex(): String =
        joinToString(separator = "") { eachByte -> "%02x ".format(eachByte) }

    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.resqme_weight, this, true)
        mResqmeView = findViewById(R.id.resqme_view)
        connectText = findViewById(R.id.resqmeConnect)
        mSafetySwitchSwitch = findViewById(R.id.safetySwitchSwitch)
        mResqmeLaunchBtn1 = findViewById(R.id.resqmeLaunchBtn1)
        mResqmeLaunchBtn2 = findViewById(R.id.resqmeLaunchBtn2)
        mResqmeLaunchAllBtn = findViewById(R.id.resqmeLaunchAllBtn)
        mResqmeState1 = findViewById(R.id.resqmeState1)
        mResqmeState2 = findViewById(R.id.resqmeState2)

        mSafetySwitchSwitch.setOnClickListener {
            resqmeService.safetySwitch(mSafetySwitchSwitch.isChecked)
        }
        mResqmeLaunchBtn1.setOnClickListener {
            try {
                if (!mSafetySwitchSwitch.isChecked) {
                    showToast(R.string.need_to_open_safety_switch)
                } else {
                    Log.i(TAG, "发射...")
                    resqmeService.launch(1)
                    showToast(R.string.launch_command_executed)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(R.string.launch_failed)
            }
        }
        mResqmeLaunchBtn2.setOnClickListener {
            try {
                if (!mSafetySwitchSwitch.isChecked) {
                    showToast(R.string.need_to_open_safety_switch)
                } else {
                    Log.i(TAG, "发射...")
                    resqmeService.launch(2)
                    showToast(R.string.launch_command_executed)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(R.string.launch_failed)
            }
        }
        mResqmeLaunchAllBtn.setOnClickListener {
            try {
                if (!mSafetySwitchSwitch.isChecked) {
                    showToast(R.string.need_to_open_safety_switch)
                } else {
                    Log.i(TAG, "发射...")
                    resqmeService.launch(3)
                    showToast(R.string.launch_command_executed)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(R.string.launch_failed)
            }
        }

        setConnectState()
    }

    private fun showToast(msg: Int) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(
                MApplication.applicationContext, msg, Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }

    // 定时器，判断连接状态
    private fun setConnectState() {
        val timer = Timer();
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask() {
            override fun run() {
                if(resqmeService.getIsConnected()){
                    if(Date().time - updateTime > 3000) {
                        handler.post {
                            connectText.setText(R.string.connection_status_notconnected)
                        }
                        resqmeService.disConnect()
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
                        while (!resqmeService.connect()) {
                            Thread.sleep(1000)
                        }
                        isConnecting = false
                        updateTime = Date().time
                    }
                }
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.scheduleAtFixedRate(task, 100, 1000);
    }
}