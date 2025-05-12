package com.example.yikupayloadexample

import android.annotation.SuppressLint
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
import com.yiku.yikupayloadSDK.protocol.WATERBRANCH_STATE_RECEIVE
import com.yiku.yikupayloadSDK.service.WaterBranchService
import com.yiku.yikupayloadSDK.util.MsgCallback
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

class WaterBranchWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "WaterBranchWeight"
    private lateinit var mLightView: View
    var waterBranchService: WaterBranchService = WaterBranchService()
    private lateinit var mSafetySwitchSwitch: Switch
    private lateinit var mOpenState: TextView
    private lateinit var mOperateBtn: Button
    private lateinit var mHoseReleaseSwitch: Switch
    private lateinit var mHoseDetachmentSwitch: Switch
    private var isConnecting: Boolean = false
    private var isFirstConnect: Boolean = true
    private var updateTime = Date().time
    // 0关，1开
    private var operate: Int = 1
    private var isHoseRelease: Boolean = false
    private var isHoseDetachment: Boolean = false

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
            if (0x00 == msg[0 + 3].toInt()) {
                mOpenState.setText(R.string.closed)
                operate = 1
            } else {
                mOpenState.setText(R.string.opened)
                operate = 0
            }
            isHoseRelease = 0x01 == msg[0 + 4].toInt()
            mHoseReleaseSwitch.isChecked = isHoseRelease
            isHoseDetachment = 0x01 == msg[0 + 5].toInt()
            mHoseDetachmentSwitch.isChecked = isHoseDetachment
        }
    }

    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.water_branch_weight, this, true)
        mLightView = findViewById(R.id.waterBranch_view)
        mSafetySwitchSwitch = findViewById(R.id.safetySwitchSwitch)
        mOpenState = findViewById(R.id.openState)
        mOperateBtn = findViewById(R.id.operateBtn)
        mHoseReleaseSwitch = findViewById(R.id.hoseReleaseSwitch)
        mHoseDetachmentSwitch = findViewById(R.id.hoseDetachmentSwitch)
        setConnectState()

        mSafetySwitchSwitch.setOnClickListener {
            mHoseReleaseSwitch.isEnabled = mSafetySwitchSwitch.isChecked
            mHoseDetachmentSwitch.isEnabled = mSafetySwitchSwitch.isChecked
        }

        mOperateBtn.setOnClickListener {
            try {
                if(operate==1 && !mSafetySwitchSwitch.isChecked) {
                    showToast(R.string.need_to_open_safety_switch)
                    return@setOnClickListener
                }
                waterBranchService.operate(operate)
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
        // 释放水带
        mHoseReleaseSwitch.setOnClickListener {
            if(!mSafetySwitchSwitch.isChecked) {
                showToast(R.string.need_to_open_safety_switch)
                return@setOnClickListener
            }
            // 如果当前是打开状态，关闭
            if(isHoseRelease) {
                waterBranchService.hoseRelease(0)
            }
            else {
                waterBranchService.hoseRelease(1)
            }
        }
        // 水带脱困
        mHoseDetachmentSwitch.setOnClickListener {
            if(!mSafetySwitchSwitch.isChecked) {
                showToast(R.string.need_to_open_safety_switch)
                return@setOnClickListener
            }
            // 如果当前水带已脱困，关闭
            if(isHoseDetachment) {
                waterBranchService.hoseDetachment(0)
            }
            else {
                waterBranchService.hoseDetachment(1)
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
        val connectText = findViewById<TextView>(R.id.waterBranchConnect)
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask(){
            override fun run() {
                if(waterBranchService.getIsConnected()){
                    if (Date().time - updateTime < 3000) {
                        handler.post {
                            connectText.setText(R.string.connection_status_connected)
                        }
                    }
                    waterBranchService.heartbeat()
                    // 3秒没收到信息，显示未连接
                    if (Date().time - updateTime > 3000) {
                        handler.post {
                            connectText.setText(R.string.connection_status_notconnected)
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
                    }
                    // 尝试重连
                    thread {
                        if(!isFirstConnect) {
                            Thread.sleep(10000)
                        }
                        isFirstConnect = false
                        while (!waterBranchService.connect()) {
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