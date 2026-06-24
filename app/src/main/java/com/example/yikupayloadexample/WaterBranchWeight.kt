package com.example.yikupayloadexample

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.yiku.yikupayloadSDK.protocol.WATERBRANCH_STATE_RECEIVE
import com.yiku.yikupayloadSDK.service.WaterBranchService
import com.yiku.yikupayloadSDK.util.MsgCallback
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class WaterBranchWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "WaterBranchWeight"
    private lateinit var mLightView: View
    var waterBranchService: WaterBranchService = WaterBranchService()
    private lateinit var mSafetySwitchSwitch: Switch
    private lateinit var mHoseReleaseBtn: Button
    private lateinit var mHoseDetachmentBtn: Button
    private var isConnecting: Boolean = false
    private var isFirstConnect: Boolean = true
    private var updateTime = Date().time
    private var isHoseRelease: Boolean = false
    private var isHoseDetachment: Boolean = false
    private val hoseThread: AtomicReference<Thread> = AtomicReference()
    private val isButtonDown = AtomicBoolean(false)

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        val host = preferences?.getString("WaterBranchHost", "")
        if(host != null && "" != host) {
            waterBranchService.setIp(host)
        }
        initView(context)
        waterBranchService.registMsgCallback(object : MsgCallback {
            override fun getId(): String {
                return "WaterBranchServiceCallback"
            }

            override fun onMsg(msg: ByteArray) {
                Log.i(TAG, "msg:${msg.toHex()}")
                if (msg[0] != 0x8d.toByte()) {
                    return
                }
                if (msg[2] == WATERBRANCH_STATE_RECEIVE.toByte()) {
                    updateTime = Date().time
                    updateState(msg)
                }
            }

        })
    }

    private fun updateState(msg: ByteArray) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            isHoseRelease = 0x01 == msg[0 + 4].toInt()
//            isHoseDetachment = 0x01 == msg[0 + 5].toInt()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.water_branch_weight, this, true)
        mLightView = findViewById(R.id.waterBranch_view)
        mSafetySwitchSwitch = findViewById(R.id.safetySwitchSwitch)
        mHoseReleaseBtn = findViewById(R.id.hoseReleaseBtn)
        mHoseDetachmentBtn = findViewById(R.id.hoseDetachmentBtn)
        setConnectState()

        mSafetySwitchSwitch.setOnClickListener {
            mHoseReleaseBtn.isEnabled = mSafetySwitchSwitch.isChecked
            mHoseDetachmentBtn.isEnabled = mSafetySwitchSwitch.isChecked
        }
        // 释放水带
        mHoseReleaseBtn.setOnClickListener {
            if(!mSafetySwitchSwitch.isChecked) {
                showToast(R.string.need_to_open_safety_switch)
                return@setOnClickListener
            }
            // 如果当前是打开状态，关闭
//            if(isHoseRelease) {
//                waterBranchService.hoseRelease(0)
//            }
//            else {
//                waterBranchService.hoseRelease(1)
//            }
            waterBranchService.hoseRelease(1)
            mHoseReleaseBtn.setText( R.string.executing )
            mHoseReleaseBtn.isEnabled = false
            thread {
                Thread.sleep(3000)
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    mHoseReleaseBtn.isEnabled = true
                    mHoseReleaseBtn.setText(R.string.hoseRelease )
                }
            }
        }
        // 水带脱困
        mHoseDetachmentBtn.setOnClickListener {
            if(!mSafetySwitchSwitch.isChecked) {
                showToast(R.string.need_to_open_safety_switch)
                return@setOnClickListener
            }
            if(isHoseDetachment) {
                waterBranchService.hoseDetachment(0) // 复位
            }
            else {
                waterBranchService.hoseDetachment(1) // 水带脱困
            }
            isHoseDetachment = !isHoseDetachment

            mHoseDetachmentBtn.setText( R.string.executing )
            mHoseDetachmentBtn.isEnabled = false
            thread {
                Thread.sleep(5000)
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    mHoseDetachmentBtn.isEnabled = true
                    if(isHoseDetachment) {
                        mHoseDetachmentBtn.setText(R.string.reset )
                    }
                    else {
                        mHoseDetachmentBtn.setText(R.string.hoseDetachment )
                    }
                }
            }
        }
    }

    private fun releaseButton(v: View) {
        isButtonDown.set(false)
        // 中断工作线程
        hoseThread.get()?.interrupt()
        hoseThread.set(null)
        v.isPressed = false
        // 恢复父View的拦截权限
        v.parent.requestDisallowInterceptTouchEvent(false)
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
        val statusDot = findViewById<View>(R.id.statusDot)
        val background = statusDot.background as GradientDrawable
        val connectText = findViewById<TextView>(R.id.waterBranchConnect)
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask(){
            override fun run() {
                if(waterBranchService.getIsConnected()){
                    if (Date().time - updateTime < 3000) {
                        handler.post {
                            connectText.setText(R.string.connection_status_connected)
                            background.setColor(ContextCompat.getColor(context, R.color.green))
                        }
                    }
                    waterBranchService.heartbeat()
                    // 3秒没收到信息，显示未连接
                    if (Date().time - updateTime > 3000) {
                        handler.post {
                            connectText.setText(R.string.connection_status_notconnected)
                            background.setColor(ContextCompat.getColor(context, R.color.red))
                        }
                    }
                    // 如果超过10s没收到消息，主动断开连接，等待重连
                    if (Date().time - updateTime > 10000) {
                        // 断连
                        waterBranchService.disConnect()
                    }
                }
                else if(!isConnecting){
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
                        while (!waterBranchService.connect()) {
                            Thread.sleep(1000)
                        }
                        isConnecting = false
                        updateTime = Date().time
                        handler.post {
                            connectText.setText(R.string.connection_status_connected)
                            background.setColor(ContextCompat.getColor(context, R.color.green))
                        }
                    }
                }
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.schedule(task, 100, 1000);
    }
}