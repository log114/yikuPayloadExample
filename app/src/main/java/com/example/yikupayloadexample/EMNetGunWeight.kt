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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.yiku.yikupayloadSDK.protocol.EMNETGUN_STATUS
import com.yiku.yikupayloadSDK.service.EMNetGunService
import com.yiku.yikupayloadSDK.util.MsgCallback
import com.yiku.yikupayloadSDK.util.byteArrayToInt16
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

class EMNetGunWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "EMNetGunWeight"
    private lateinit var mContentView: View
    var emNetGunService: EMNetGunService = EMNetGunService()
    private lateinit var mNetInstallState: TextView
    private lateinit var mChargeState: TextView
    private lateinit var mPitchState: TextView
    private lateinit var mLaserState: TextView
    private lateinit var mSafetySwitch: Switch
    private lateinit var mPitchSeekbar: SeekBar
    private lateinit var mLaserBtn: Button
    private lateinit var mLaunchBtn: Button
    private var isConnecting: Boolean = false
    private var isFirstConnect: Boolean = true
    private var isLaserOpened: Boolean = false
    private var isSettingPitch: Boolean = false
    private var isLaunching: Boolean = false
    private var updateTime = Date().time

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        val host = preferences?.getString("EMNetGunHost", "")
        if(host != null && "" != host) {
            emNetGunService.setIp(host)
        }
        initView(context)
        emNetGunService.registMsgCallback(object : MsgCallback {
            override fun getId(): String {
                return "EMNetGunServiceCallback"
            }

            override fun onMsg(msg: ByteArray) {
                Log.i(TAG, "msg:${msg.toHex()}")
                if (msg[0] != 0x8d.toByte()) {
                    return
                }
                if (msg[2] == EMNETGUN_STATUS.toByte()) {
                    updateTime = Date().time
                    updateState(msg)
                }
            }

        })
    }

    // 状态更新
    private fun updateState(msg: ByteArray) {
        isLaserOpened = msg[4].toInt() == 0x01
        val pitch = msg[5].toInt()
        val volatile = byteArrayToInt16(msg.slice(6 until 8).toByteArray())
        val isInstallNet = msg[8].toInt() == 0x00
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            mNetInstallState.text = if(isInstallNet) context.getString(R.string.installed) else context.getString(R.string.not_installed)
            if(isLaserOpened) {
                mLaserState.setText(R.string.opened)
                mLaserBtn.setText(R.string.laser_off)
            }
            else {
                mLaserState.setText(R.string.closed)
                mLaserBtn.setText(R.string.laser_on)
            }
//            if(!isSettingPitch) {
//                mPitchSeekbar.progress = pitch
//            }
            if(volatile > 400) {
                mLaunchBtn.isEnabled = true
                mChargeState.setText(R.string.fully_charged)
            }
            else {
                mLaunchBtn.isEnabled = false
                mChargeState.setText(R.string.not_charged)
            }
            mPitchState.text = context.getString(R.string.angle_value, pitch)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.em_net_gun_weight, this, true)
        mContentView = findViewById(R.id.emNetGun_view)
        mNetInstallState = findViewById(R.id.netInstallState)
        mChargeState = findViewById(R.id.chargeState)
        mPitchState = findViewById(R.id.pitchState)
        mLaserState = findViewById(R.id.laserState)
        mSafetySwitch = findViewById(R.id.safetySwitch)
        mPitchSeekbar = findViewById(R.id.pitch_seekbar)
        mLaserBtn = findViewById(R.id.laserBtn)
        mLaunchBtn = findViewById(R.id.launchBtn)
        setConnectState()

        mPitchSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                if (seekBar != null) {
//                    emNetGunService.setPitch(seekBar.progress)
//                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSettingPitch = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
//                mPitchSeekbar.isEnabled = false
//                thread {
//                    Thread.sleep(1000)
//                    Handler(Looper.getMainLooper()).post {
//                        mPitchSeekbar.isEnabled = true
//                    }
//                    isSettingPitch = false
//                }
                emNetGunService.setPitch(mPitchSeekbar.progress)
            }

        })
        // 激光开关
        mLaserBtn.setOnClickListener {
            mLaserBtn.isEnabled = false
            emNetGunService.laserSwitch(!isLaserOpened)
            thread {
                Thread.sleep(2000)
                Handler(Looper.getMainLooper()).post {
                    mLaserBtn.isEnabled = true
                }
            }
        }

        // 发射按键
        mLaunchBtn.setOnClickListener {
            if(mSafetySwitch.isChecked) {
                isLaunching = true
                mLaunchBtn.isEnabled = false
                emNetGunService.launch()
                thread {
                    Thread.sleep(2000)
                    isLaunching = false
                }
            }
            else {
                showToast(R.string.need_to_open_safety_switch)
            }
        }
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
        val connectText = findViewById<TextView>(R.id.emNetGunConnect)
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask(){
            override fun run() {
                if(emNetGunService.getIsConnected()){
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
                        emNetGunService.disConnect()
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
                        while (!emNetGunService.connect()) {
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