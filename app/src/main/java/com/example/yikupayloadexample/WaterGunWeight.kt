package com.example.yikupayloadexample

import android.annotation.SuppressLint
import android.content.Context
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
import com.yiku.yikupayloadSDK.protocol.WATERGUN_STATE_RECEIVE
import com.yiku.yikupayloadSDK.service.WaterGunService
import com.yiku.yikupayloadSDK.util.MsgCallback
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

class WaterGunWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
        private val TAG = "WaterGunWeight"
        private lateinit var mLightView: View
        var waterGunService: WaterGunService = WaterGunService()
        private lateinit var mSafetySwitchSwitch: Switch
        private lateinit var mState: TextView
        private lateinit var mOperateBtn: Button
        private lateinit var mToLeftBtn: Button
        private lateinit var mToRightBtn: Button
        private var isConnecting: Boolean = false
        private var isFirstConnect: Boolean = true
        private var updateTime = Date().time
        private var timerToLeft: Timer? = null
        private var timerToRight: Timer? = null

        // 0关，1开
        private var state: Int = 0

        constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
        constructor(context: Context) : this(context, null, 0)

        init {
            val host = preferences?.getString("WaterGunHost", "")
            if(host != null && "" != host) {
                waterGunService.setIp(host)
            }
            initView(context)
            waterGunService.registMsgCallback(object : MsgCallback {
                override fun getId(): String {
                    return "WaterGunServiceCallback"
                }

                override fun onMsg(msg: ByteArray) {
                    Log.i(TAG, "msg:${msg.toHex()}")
                    if (msg[0] != 0x8d.toByte()) {
                        return
                    }
                    if (msg[2] == WATERGUN_STATE_RECEIVE.toByte()) {
                        updateTime = Date().time
                        updateState(msg)
                    }
                }

            })
        }

        private fun updateState(msg: ByteArray) {
            state = msg[0 + 3].toInt()
            var locationStatus = msg[0 + 4].toInt()
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                when(state) {
                    0 -> {
                        mState.setText(R.string.preparing)
                        mState.setTextColor(resources.getColor(R.color.red))
                        mOperateBtn.isEnabled = false
                        mToLeftBtn.isEnabled = false
                        mToRightBtn.isEnabled = false
                    }
                    1 -> {
                        mState.setText(R.string.beReady)
                        mState.setTextColor(resources.getColor(R.color.green))
                        mOperateBtn.isEnabled = true
                        mOperateBtn.setText(R.string.pulse_on)
                        mToLeftBtn.isEnabled = false
                        mToRightBtn.isEnabled = false
                    }
                    2 -> {
                        mState.setText(R.string.autoMode)
                        mState.setTextColor(resources.getColor(R.color.green))
                        mOperateBtn.isEnabled = true
                        mOperateBtn.setText(R.string.switching_modes)
                        mToLeftBtn.isEnabled = false
                        mToRightBtn.isEnabled = false
                    }
                    3 -> {
                        mState.setText(R.string.switching)
                        mState.setTextColor(resources.getColor(R.color.green))
                        mOperateBtn.isEnabled = false
                        mToLeftBtn.isEnabled = false
                        mToRightBtn.isEnabled = false
                    }
                    4 -> {
                        mState.setText(R.string.manualMode)
                        mState.setTextColor(resources.getColor(R.color.green))
                        mOperateBtn.isEnabled = true
                        mOperateBtn.setText(R.string.stop)
                        mToLeftBtn.isEnabled = true
                        mToRightBtn.isEnabled = true
                    }
                    5 -> {
                        mState.setText(R.string.stopped)
                        mState.setTextColor(resources.getColor(R.color.green))
                        mOperateBtn.isEnabled = true
                        mOperateBtn.setText(R.string.pulse_on)
                        mToLeftBtn.isEnabled = false
                        mToRightBtn.isEnabled = false
                    }
                }

                Log.i(TAG, "locationStatus=${locationStatus}")
                when(locationStatus) {
                    1 -> {
                        stopToLeft()
                    }
                    2 -> {
                        stopToRight()
                    }
                }
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun initView(context: Context?) {
            LayoutInflater.from(context).inflate(R.layout.water_gun_weight, this, true)
            mLightView = findViewById(R.id.waterGun_view)
            mSafetySwitchSwitch = findViewById(R.id.safetySwitchSwitch)
            mState = findViewById(R.id.state)
            mOperateBtn = findViewById<Button>(R.id.operateBtn)
            mToLeftBtn = findViewById<Button>(R.id.toLeftBtn)
            mToRightBtn = findViewById<Button>(R.id.toRightBtn)
            setConnectState()

            mOperateBtn.setOnClickListener {
                try {
                    if(!mSafetySwitchSwitch.isChecked) {
                        showToast(R.string.need_to_open_safety_switch)
                        return@setOnClickListener
                    }
                    waterGunService.modeSwitch()
                    mOperateBtn.setText( R.string.executing )
                    mOperateBtn.isEnabled = false
                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast(R.string.operation_failed)
                }

            }
            mToLeftBtn.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if(timerToLeft == null) {
                            if(!mSafetySwitchSwitch.isChecked) {
                                showToast(R.string.need_to_open_safety_switch)
                                return@setOnTouchListener false
                            }
                            if(state != 4) {
                                showToast(R.string.notManualMode)
                                return@setOnTouchListener false
                            }
                            stopToRight()
                            timerToLeft = Timer()
                            val task = object : TimerTask(){
                                override fun run() {
                                    Log.i(TAG, "向左")
                                    waterGunService.toLeft()
                                }
                            }
                            // 定时器
                            timerToLeft?.schedule(task, 0, 100)
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        Log.i(TAG, "向左松开")
                        stopToLeft()
                    }
                }
                true
            }
            mToRightBtn.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if(timerToRight == null) {
                            if(!mSafetySwitchSwitch.isChecked) {
                                showToast(R.string.need_to_open_safety_switch)
                                return@setOnTouchListener false
                            }
                            if(state != 4) {
                                showToast(R.string.notManualMode)
                                return@setOnTouchListener false
                            }
                            stopToLeft()
                            timerToRight = Timer();
                            val task = object : TimerTask(){
                                override fun run() {
                                    Log.i(TAG, "向右")
                                    waterGunService.toRight()
                                }
                            }
                            // 定时器
                            timerToRight?.schedule(task, 0, 100)
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        Log.i(TAG, "向右松开")
                        stopToRight()
                    }
                }
                true
            }
        }

        private fun stopToLeft() {
            timerToLeft?.cancel()
            timerToLeft?.purge()
            timerToLeft = null
        }
        private fun stopToRight() {
            timerToRight?.cancel()
            timerToRight?.purge()
            timerToRight = null
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
            val connectText = findViewById<TextView>(R.id.waterGunConnect)
            val handler = Handler(Looper.getMainLooper())
            val task = object : TimerTask(){
                override fun run() {
                    if(waterGunService.getIsConnected()){
                        if (Date().time - updateTime < 3000) {
                            handler.post {
                                connectText.setText(R.string.connection_status_connected)
                            }
                        }
                        waterGunService.heartbeat()
                        // 3秒没收到信息，显示未连接
                        if (Date().time - updateTime > 3000) {
                            handler.post {
                                connectText.setText(R.string.connection_status_notconnected)
                            }
                        }
                        // 如果超过10s没收到消息，主动断开连接，等待重连
                        if (Date().time - updateTime > 10000) {
                            // 断连
                            waterGunService.disConnect()
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
                            while (!waterGunService.connect()) {
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