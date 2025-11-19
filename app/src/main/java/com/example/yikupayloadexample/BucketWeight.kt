package com.example.yikupayloadexample

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.yiku.yikupayloadSDK.protocol.BUCKET_BARREL_STATE
import com.yiku.yikupayloadSDK.service.BucketService
import com.yiku.yikupayloadSDK.util.MsgCallback
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

class BucketWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "BucketWeight"
    private lateinit var mBucketView: View
    private lateinit var mPromptView: View
    var bucketService: BucketService = BucketService()
    private lateinit var mSafetySwitchSwitch: Switch
    private lateinit var mHookSwitch: Switch
    private lateinit var mBarrelOpenBtn: Button
    private lateinit var mBarrelCloseBtn: Button
    private lateinit var mBarrelStopBtn: Button
    private lateinit var mOkBtn: Button
    private lateinit var mCancelBtn: Button
    private var isConnecting: Boolean = false
    private var isFirstConnect: Boolean = true
    private var updateTime = Date().time
    private var isFirstLoad = true
    private var safetySwitch = false
    private var barrelState = 0
    private var hookState = false

    private var isControlingSafetySwitch = false // 安全开关控制
    private var isControlingHookSwitch = false // 挂钩控制
    private var isControlingBarrel = false // 水桶控制

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        val host = preferences?.getString("BucketHost", "")
        if(host != null && "" != host) {
            bucketService.setIp(host)
        }
        initView(context)
        bucketService.registMsgCallback(object : MsgCallback {
            override fun getId(): String {
                return "BucketServiceCallback"
            }

            override fun onMsg(msg: ByteArray) {
                Log.i(TAG, "msg:${msg.toHex()}")
                if (msg[0] != 0x8d.toByte()) {
                    return
                }
                if (msg[2] == BUCKET_BARREL_STATE.toByte()) {
                    updateTime = Date().time
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        updateState(msg)
                    }
                }
            }

        })
    }

    private fun updateState(msg: ByteArray) {
        safetySwitch = msg[3].toInt() == 1
        barrelState = msg[4].toInt()
        hookState = msg[5].toInt() == 1
        if(isFirstLoad) {
            isFirstLoad = false
            mSafetySwitchSwitch.isChecked = safetySwitch
            mHookSwitch.isChecked = hookState
        }
        // 安全开关状态
        if(!isControlingSafetySwitch) {
            mSafetySwitchSwitch.isChecked = safetySwitch
            mSafetySwitchSwitch.isEnabled = true
        }
        // 挂钩开关状态
        if(!isControlingHookSwitch) {
            mHookSwitch.isChecked = hookState
            if(safetySwitch) {
                mHookSwitch.isEnabled = true
            }
        }
        // 水桶控制按键状态
        if(safetySwitch) {
            if(!isControlingBarrel) {
                mBarrelOpenBtn.isEnabled = true
                mBarrelCloseBtn.isEnabled = true
                mBarrelStopBtn.isEnabled = true
            }
        }
        else {
            mHookSwitch.isEnabled = false
            mBarrelOpenBtn.isEnabled = false
            mBarrelCloseBtn.isEnabled = false
            mBarrelStopBtn.isEnabled = false
        }
    }

    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.bucket_weight, this, true)
        mBucketView = findViewById(R.id.bucket_view)
        mPromptView = findViewById(R.id.prompt_view)
        mSafetySwitchSwitch = findViewById(R.id.safetySwitchSwitch)
        mHookSwitch = findViewById(R.id.hookSwitch)
        mBarrelOpenBtn = findViewById(R.id.barrelOpenBtn)
        mBarrelCloseBtn = findViewById(R.id.barrelCloseBtn)
        mBarrelStopBtn = findViewById(R.id.barrelStopBtn)
        mOkBtn = findViewById(R.id.okBtn)
        mCancelBtn = findViewById(R.id.cancelBtn)

        mBarrelOpenBtn.isEnabled = false
        mBarrelCloseBtn.isEnabled = false
        mBarrelStopBtn.isEnabled = false

        setConnectState()
        // 安全开关
        mSafetySwitchSwitch.setOnClickListener {
            var _switch = 0
            if( mSafetySwitchSwitch.isChecked) {
                _switch = 1
            }
            bucketService.safetySwitch(_switch)
            isControlingSafetySwitch = true
            mSafetySwitchSwitch.isEnabled = false
            thread {
                Thread.sleep(2000)
                isControlingSafetySwitch = false
            }
        }

        // 水桶开
        mBarrelOpenBtn.setOnClickListener {
            try {
                if(!mSafetySwitchSwitch.isChecked) {
                    showToast(R.string.need_to_open_safety_switch)
                    return@setOnClickListener
                }
                bucketService.barrelControl(1)
                isControlingBarrel = true
                mBarrelOpenBtn.isEnabled = false
                thread {
                    Thread.sleep(2000)
                    isControlingBarrel = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(R.string.operation_failed)
            }
        }
        // 水桶关
        mBarrelCloseBtn.setOnClickListener {
            try {
                bucketService.barrelControl(2)
                mBarrelCloseBtn.isEnabled = false
                isControlingBarrel = true
                thread {
                    Thread.sleep(2000)
                    isControlingBarrel = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(R.string.operation_failed)
            }
        }
        // 水桶停止
        mBarrelStopBtn.setOnClickListener {
            bucketService.barrelControl(0)
            mBarrelStopBtn.isEnabled = false
            isControlingBarrel = true
            thread {
                Thread.sleep(2000)
                isControlingBarrel = false
            }
        }
        // 挂钩开关
        mHookSwitch.setOnClickListener {
            isControlingHookSwitch = true
            if(mHookSwitch.isChecked) {
                mBucketView.visibility = GONE
                mPromptView.visibility = VISIBLE
            }
            else {
                bucketService.hookControl(0)
                mHookSwitch.isEnabled = false
                thread {
                    Thread.sleep(2000)
                    isControlingHookSwitch = false
                }
            }
        }
        // 确定打开挂钩
        mOkBtn.setOnClickListener {
            bucketService.hookControl(1)
            mBucketView.visibility = VISIBLE
            mPromptView.visibility = GONE
            mHookSwitch.isEnabled = false
            thread {
                Thread.sleep(2000)
                isControlingHookSwitch = false
            }
        }
        // 取消打开挂钩
        mCancelBtn.setOnClickListener {
            mHookSwitch.isChecked = false
            mBucketView.visibility = VISIBLE
            mPromptView.visibility = GONE
            isControlingHookSwitch = false
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
        val connectText = findViewById<TextView>(R.id.bucketConnect)
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask(){
            override fun run() {
                if(bucketService.getIsConnected()){
                    if (Date().time - updateTime < 3000) {
                        handler.post {
                            connectText.setText(R.string.connection_status_connected)
                        }
                    }
                    // 3秒没收到信息，显示未连接
                    if (Date().time - updateTime > 3000) {
                        handler.post {
                            connectText.setText(R.string.connection_status_notconnected)
                        }
                    }
                    // 如果超过10s没收到消息，主动断开连接，等待重连
                    if (Date().time - updateTime > 10000) {
                        // 断连
                        bucketService.disConnect()
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
                            Thread.sleep(5000)
                        }
                        isFirstConnect = false
                        while (!bucketService.connect()) {
                            Thread.sleep(1000)
                        }
                        isConnecting = false
                        updateTime = Date().time
                        handler.post {
                            connectText.setText(R.string.connection_status_connected)
                        }
                    }
                }
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.scheduleAtFixedRate(task, 100, 1000);
    }
}