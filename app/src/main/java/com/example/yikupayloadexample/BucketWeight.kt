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
import com.yiku.yikupayloadSDK.protocol.BUCKET_STATE_RECEIVE
import com.yiku.yikupayloadSDK.service.BucketService
import com.yiku.yikupayloadSDK.util.MsgCallback
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

class BucketWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "BucketWeight"
    private lateinit var mLightView: View
    var bucketService: BucketService = BucketService()
    private lateinit var mSafetySwitchSwitch: Switch
    private lateinit var mBarrelOpenBtn: Button
    private lateinit var mBarrelCloseBtn: Button
    private lateinit var mBarrelStopBtn: Button
    private lateinit var mHookOpenBtn: Button
    private lateinit var mHookCloseBtn: Button
    private var isConnecting: Boolean = false
    private var isFirstConnect: Boolean = true
    private var updateTime = Date().time

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
                if (msg[2] == BUCKET_STATE_RECEIVE.toByte()) {
                    updateTime = Date().time
                }
            }

        })
    }

    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.bucket_weight, this, true)
        mLightView = findViewById(R.id.bucket_view)
        mSafetySwitchSwitch = findViewById(R.id.safetySwitchSwitch)
        mBarrelOpenBtn = findViewById(R.id.barrelOpenBtn)
        mBarrelCloseBtn = findViewById(R.id.barrelCloseBtn)
        mBarrelStopBtn = findViewById(R.id.barrelStopBtn)
        mHookOpenBtn = findViewById(R.id.hookOpenBtn)
        mHookCloseBtn = findViewById(R.id.hookCloseBtn)
        setConnectState()
        // 水桶开
        mBarrelOpenBtn.setOnClickListener {
            try {
                if(!mSafetySwitchSwitch.isChecked) {
                    showToast(R.string.need_to_open_safety_switch)
                    return@setOnClickListener
                }
                bucketService.barrelControl(1)
                mBarrelOpenBtn.setText(R.string.opening)
                mBarrelOpenBtn.isEnabled = false
                thread {
                    Thread.sleep(2000)
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        mBarrelOpenBtn.isEnabled = true
                        mBarrelOpenBtn.setText(R.string.open)
                    }
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
                mBarrelCloseBtn.setText(R.string.closing)
                mBarrelCloseBtn.isEnabled = false
                thread {
                    Thread.sleep(2000)
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        mBarrelCloseBtn.isEnabled = true
                        mBarrelCloseBtn.setText(R.string.close)
                    }
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
            mBarrelStopBtn.setText(R.string.executing )
            thread {
                Thread.sleep(2000)
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    mBarrelStopBtn.isEnabled = true
                    mBarrelStopBtn.setText(R.string.stop )
                }
            }
        }
        // 挂钩开
        mHookOpenBtn.setOnClickListener {
            try {
                if(!mSafetySwitchSwitch.isChecked) {
                    showToast(R.string.need_to_open_safety_switch)
                    return@setOnClickListener
                }
                bucketService.hookControl(1)
                mHookOpenBtn.setText(R.string.opening)
                mHookOpenBtn.isEnabled = false
                thread {
                    Thread.sleep(2000)
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        mHookOpenBtn.isEnabled = true
                        mHookOpenBtn.setText(R.string.open)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(R.string.operation_failed)
            }
        }
        // 挂钩关
        mHookCloseBtn.setOnClickListener {
            try {
                bucketService.hookControl(0)
                mHookCloseBtn.setText(R.string.closing)
                mHookCloseBtn.isEnabled = false
                thread {
                    Thread.sleep(2000)
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        mHookCloseBtn.isEnabled = true
                        mHookCloseBtn.setText(R.string.close)
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
                    bucketService.heartbeat()
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