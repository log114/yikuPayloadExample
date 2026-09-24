package com.example.yikupayloadexample

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.yiku.yikupayloadSDK.protocol.PAINTSPRAYING_STATUS
import com.yiku.yikupayloadSDK.service.PaintSprayingService
import com.yiku.yikupayloadSDK.util.MsgCallback
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

class PaintSprayingWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "PaintSprayingWeight"
    private lateinit var mContentView: View
    var paintSprayingService: PaintSprayingService = PaintSprayingService()
    private lateinit var mDistanceGear: TextView
    private lateinit var mFlowRateGear: TextView
    private lateinit var mSafetySwitch: Switch
    private lateinit var mDistanceSeekbar: SeekBar
    private lateinit var mFlowRateSeekbar: SeekBar
    private var isConnecting: Boolean = false
    private var isFirstConnect: Boolean = true
    private var curDistanceGear: Int = 0
    private var curFlowRateGear: Int = 0
    private var isSettingDistanceGear: Boolean = false
    private var isSettingFlowRateGear: Boolean = false
    private var updateTime = Date().time

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        val host = preferences?.getString("PaintSprayingHost", "")
        if(host != null && "" != host) {
            paintSprayingService.setIp(host)
        }
        initView(context)
        paintSprayingService.registMsgCallback(object : MsgCallback {
            override fun getId(): String {
                return "PaintSprayingServiceCallback"
            }

            override fun onMsg(msg: ByteArray) {
                Log.i(TAG, "msg:${msg.toHex()}")
                if (msg[0] != 0x8d.toByte()) {
                    return
                }
                if (msg[2] == PAINTSPRAYING_STATUS.toByte()) {
                    updateTime = Date().time
                    updateState(msg)
                }
            }

        })
    }

    // 状态更新
    private fun updateState(msg: ByteArray) {
        curDistanceGear = msg[3].toInt()
        curFlowRateGear = msg[4].toInt()
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            mDistanceGear.text = context.getString(R.string.gear, curDistanceGear)
            mFlowRateGear.text = context.getString(R.string.gear, curFlowRateGear)
            if(!isSettingDistanceGear) {
                mDistanceSeekbar.progress = curDistanceGear
                if(mSafetySwitch.isChecked) {
                    mDistanceSeekbar.isEnabled = true
                }
            }
            if(!isSettingFlowRateGear) {
                mFlowRateSeekbar.progress = curFlowRateGear
                if(mSafetySwitch.isChecked) {
                    mFlowRateSeekbar.isEnabled = true
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.paint_spraying_weight, this, true)
        mContentView = findViewById(R.id.paintSpraying_view)
        mDistanceGear = findViewById(R.id.distanceGear)
        mFlowRateGear = findViewById(R.id.flowRateGear)
        mSafetySwitch = findViewById(R.id.safetySwitch)
        mDistanceSeekbar = findViewById(R.id.distance_seekbar)
        mFlowRateSeekbar = findViewById(R.id.flow_rate_seekbar)
        setConnectState()

        mDistanceSeekbar.isEnabled = false
        mFlowRateSeekbar.isEnabled = false

        mSafetySwitch.setOnClickListener {
            if(!isSettingDistanceGear) {
                mDistanceSeekbar.isEnabled = mSafetySwitch.isChecked
            }
            if(!isSettingFlowRateGear) {
                mFlowRateSeekbar.isEnabled = mSafetySwitch.isChecked
            }
        }

        // 距离挡位设置
        mDistanceSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSettingDistanceGear = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                mDistanceSeekbar.isEnabled = false
                paintSprayingService.gearControl(mDistanceSeekbar.progress, curFlowRateGear)
                thread {
                    Thread.sleep(1000)
                    isSettingDistanceGear = false
                }
            }

        })
        // 流量挡位设置
        mFlowRateSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSettingFlowRateGear = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                mFlowRateSeekbar.isEnabled = false
                paintSprayingService.gearControl(curDistanceGear, mFlowRateSeekbar.progress)
                thread {
                    Thread.sleep(1000)
                    isSettingFlowRateGear = false
                }
            }

        })
    }

    fun ByteArray.toHex(): String =
        joinToString(separator = "") { eachByte -> "%02x ".format(eachByte) }

    private fun showToast(msg: Int, duration: Int = Toast.LENGTH_SHORT) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(
                MApplication.applicationContext, msg, duration
            ).show()
        }
    }

    private fun showToast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(
                MApplication.applicationContext, msg, duration
            ).show()
        }
    }

    // 定时器，判断连接状态
    private fun setConnectState(){
        val timer = Timer();
        val statusDot = findViewById<View>(R.id.statusDot)
        val background = statusDot.background as GradientDrawable
        val connectText = findViewById<TextView>(R.id.paintSprayingConnect)
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask(){
            override fun run() {
                if(paintSprayingService.getIsConnected()){
                    if (Date().time - updateTime < 3000) {
                        handler.post {
                            connectText.setText(R.string.connection_status_connected)
                            background.setColor(ContextCompat.getColor(context, R.color.green))
                        }
                    }
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
                        paintSprayingService.disConnect()
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
                        while (!paintSprayingService.connect()) {
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