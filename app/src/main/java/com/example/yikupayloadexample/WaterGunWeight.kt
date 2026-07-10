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
import com.yiku.yikupayloadSDK.protocol.WATERGUN_STATE_RECEIVE
import com.yiku.yikupayloadSDK.service.WaterGunService
import com.yiku.yikupayloadSDK.util.MsgCallback
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread
import kotlin.time.Duration

class WaterGunWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
        private val TAG = "WaterGunWeight"
        private lateinit var mContentView: View
        private lateinit var mPromptView: View
        var waterGunService: WaterGunService = WaterGunService()
        private lateinit var mSafetySwitch: Switch
        private lateinit var mState: TextView
        private lateinit var mOperateBtn: Button
        private lateinit var mSwitchBtn: Button
        private lateinit var mSwitchState: TextView
        private lateinit var mToLeftBtn: Button
        private lateinit var mToRightBtn: Button
        private lateinit var mOkBtn: Button
        private lateinit var mCancelBtn: Button
        private var isConnecting: Boolean = false
        private var isFirstConnect: Boolean = true
        private var updateTime = Date().time
        private var timerToLeft: Timer? = null
        private var timerToRight: Timer? = null
        private var isSwitchingNozzle = false
        private var currentNozzleType = 0

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
            state = msg[0 + 3].toInt() // 水枪状态
            val locationStatus = msg[0 + 4].toInt() // 限位状态
            currentNozzleType = msg[5].toInt()
            val nozzleType = if(currentNozzleType == 0) { // 当前喷头类型
                R.string.clear_water
            }
            else {
                R.string.foam
            }
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                when(state) {
                    0 -> {
                        mState.setText(R.string.preparing)
                        mState.setTextColor(resources.getColor(R.color.red))
                        mOperateBtn.isEnabled = false
                        mSwitchBtn.isEnabled = false
                        mToLeftBtn.isEnabled = false
                        mToRightBtn.isEnabled = false
                    }
                    1 -> {
                        mState.setText(R.string.static_mode)
                        mState.setTextColor(resources.getColor(R.color.green))
                        mOperateBtn.isEnabled = mSafetySwitch.isChecked
                        mOperateBtn.setText(R.string.shake_head)
                        mSwitchBtn.isEnabled = mSafetySwitch.isChecked && !isSwitchingNozzle
                        mToLeftBtn.isEnabled = false
                        mToRightBtn.isEnabled = false
                    }
                    2 -> {
                        mState.setText(R.string.autoMode)
                        mState.setTextColor(resources.getColor(R.color.green))
                        mOperateBtn.isEnabled = mSafetySwitch.isChecked
                        mOperateBtn.setText(R.string.switching_modes)
                        mSwitchBtn.isEnabled = mSafetySwitch.isChecked && !isSwitchingNozzle
                        mToLeftBtn.isEnabled = false
                        mToRightBtn.isEnabled = false
                    }
                    3 -> {
                        mState.setText(R.string.switching)
                        mState.setTextColor(resources.getColor(R.color.green))
                        mOperateBtn.isEnabled = false
                        mOperateBtn.setText(R.string.switching_modes)
                        mSwitchBtn.isEnabled = mSafetySwitch.isChecked && !isSwitchingNozzle
                        mToLeftBtn.isEnabled = false
                        mToRightBtn.isEnabled = false
                    }
                    4 -> {
                        mState.setText(R.string.manualMode)
                        mState.setTextColor(resources.getColor(R.color.green))
                        mOperateBtn.isEnabled = mSafetySwitch.isChecked
                        mOperateBtn.setText(R.string.switching_modes)
                        mSwitchBtn.isEnabled = mSafetySwitch.isChecked && !isSwitchingNozzle
                        mToLeftBtn.isEnabled = mSafetySwitch.isChecked
                        mToRightBtn.isEnabled = mSafetySwitch.isChecked
                    }
                    5 -> {
                        mState.setText(R.string.static_mode)
                        mState.setTextColor(resources.getColor(R.color.green))
                        mOperateBtn.isEnabled = mSafetySwitch.isChecked
                        mOperateBtn.setText(R.string.shake_head)
                        mSwitchBtn.isEnabled = mSafetySwitch.isChecked && !isSwitchingNozzle
                        mToLeftBtn.isEnabled = false
                        mToRightBtn.isEnabled = false
                    }
                }

                Log.i(TAG, "locationStatus=${locationStatus}")
                when(locationStatus) {
                    1 -> {
                        if(mToRightBtn.isPressed && timerToRight != null) {
                            showToast(R.string.reached_rightmost_side)
                        }
                        stopToRight()
                    }
                    2 -> {
                        if(mToLeftBtn.isPressed && timerToLeft != null) {
                            showToast(R.string.reached_leftmost_side)
                        }
                        stopToLeft()
                    }
                }
                mSwitchState.setText(nozzleType)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun initView(context: Context?) {
            LayoutInflater.from(context).inflate(R.layout.water_gun_weight, this, true)
            mContentView = findViewById(R.id.waterGun_view)
            mPromptView = findViewById(R.id.prompt_view)
            mSafetySwitch = findViewById(R.id.safetySwitch)
            mState = findViewById(R.id.state)
            mOperateBtn = findViewById(R.id.operateBtn)
            mSwitchBtn = findViewById(R.id.switchBtn)
            mSwitchState = findViewById(R.id.switch_state)
            mToLeftBtn = findViewById(R.id.toLeftBtn)
            mToRightBtn = findViewById(R.id.toRightBtn)
            mOkBtn = findViewById(R.id.okBtn)
            mCancelBtn = findViewById(R.id.cancelBtn)
            setConnectState()

            // 切换模式
            mOperateBtn.setOnClickListener {
                try {
                    waterGunService.modeSwitch()
                    mOperateBtn.isEnabled = false
                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast(R.string.operation_failed)
                }

            }
            // 切换喷头
            mSwitchBtn.setOnClickListener {
                mContentView.visibility = GONE
                mPromptView.visibility = VISIBLE
            }
            mOkBtn.setOnClickListener {
                isSwitchingNozzle = true
                mSwitchBtn.isEnabled = false
                if(currentNozzleType == 0) {
                    waterGunService.nozzleSwitch(1)
                }
                else {
                    waterGunService.nozzleSwitch(0)
                }
                thread {
                    Thread.sleep(3000)
                    isSwitchingNozzle = false
                }
                mPromptView.visibility = GONE
                mContentView.visibility = VISIBLE
            }
            mCancelBtn.setOnClickListener {
                mPromptView.visibility = GONE
                mContentView.visibility = VISIBLE
            }
            // 手动向左
            mToLeftBtn.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        mToLeftBtn.isPressed = true
                        if(timerToLeft == null) {
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
                        mToLeftBtn.isPressed = false
                        stopToLeft()
                    }
                }
                true
            }
            // 手动向右
            mToRightBtn.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        mToRightBtn.isPressed = true
                        if(timerToRight == null) {
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
                        mToRightBtn.isPressed = false
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


        private fun showToast(msg: Int, duration: Int = Toast.LENGTH_LONG) {
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
            val connectText = findViewById<TextView>(R.id.waterGunConnect)
            val handler = Handler(Looper.getMainLooper())
            val task = object : TimerTask(){
                override fun run() {
                    if(waterGunService.getIsConnected()){
                        if (Date().time - updateTime < 3000) {
                            handler.post {
                                connectText.setText(R.string.connection_status_connected)
                                background.setColor(ContextCompat.getColor(context, R.color.green))
                            }
                        }
                        waterGunService.heartbeat()
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
                            waterGunService.disConnect()
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
                            while (!waterGunService.connect()) {
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