package com.example.yikupayloadexample

import android.content.Context
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import com.yiku.yikupayload.protocol.DESCENT_STATE_GET
import com.yiku.yikupayload.service.SlowDescentDeviceService
import com.yiku.yikupayload.util.MaxFValueInputFilter
import com.yiku.yikupayload.util.MaxValueInputFilter
import com.yiku.yikupayload.util.MsgCallback
import java.util.Timer
import java.util.TimerTask

class SlowDescentDeviceWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "SlowDescentDeviceWeight"
    var slowDescentDeviceService: SlowDescentDeviceService

    //    private lateinit var mDescentSafetySwitchSwitch: Switch
    private lateinit var mBtnOpen: Button
    private lateinit var mBtnClose: Button

    //    private lateinit var mIsEnable: TextView
    private lateinit var mSlowDescentDeviceView: View
    private lateinit var mPromptBox: View
    private lateinit var mCurrentLineLength: TextView

    //    private lateinit var mWeight: TextView
    private lateinit var mCurrentLocation: TextView
    private lateinit var mBtnBySpeed: RadioButton
    private lateinit var mBtnByLength: RadioButton
    private lateinit var mGroupBySpeed: RadioGroup
    private lateinit var mGruopByLength: RadioGroup
    private lateinit var mSpeedEditText: EditText
    private lateinit var mLengthEditText: EditText
    private lateinit var mBtnSpeedContrl: LinearLayout
    private lateinit var mBtnRise: Button
    private lateinit var mBtnDecline: Button
    private lateinit var mBtnStop: Button
    private lateinit var mBtnLengthContrl: LinearLayout
    private lateinit var mBtnPulseOn: Button
    private lateinit var mBtnEmergencyStop: Button
    private lateinit var mBtnFusing: Button
    private lateinit var mActionContrl: LinearLayout
    private lateinit var mEmergentContrl: LinearLayout
    private lateinit var mReleaseEmergency: Button

    private lateinit var mBtnOk: Button
    private lateinit var mBtnCancel: Button

    private var isEnable = false
    private var isEmergency = false
    private var liftingMethod: Int = 1

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        initView(context)
        slowDescentDeviceService = SlowDescentDeviceService()
        val host = preferences?.getString("SlowDescentDeviceHost", "")
        if(host != null && "" != host) {
            slowDescentDeviceService.setIp(host)
        }
        slowDescentDeviceService.msgCallbacks += object : MsgCallback {
            override fun getId(): String {
                return "SlowDescentDeviceWeightCallback"
            }

            override fun onMsg(msg: ByteArray) {
                Log.i(TAG, "缓降器msg:${msg.toHex()}")
                if (msg[0] != 0x8d.toByte()) {
                    return
                }
                if (msg[2] == DESCENT_STATE_GET.toByte()) {
                    Log.i(TAG, "recv 0x15!")
                    // 更新缓降器状态
                    mSlowDescentDeviceView.post {
                        updateStatus(msg)
                    }
                }
            }

        }
    }

    // 更新缓降器状态
    fun updateStatus(msg: ByteArray) {
        Log.i(TAG, "缓降器msg:${msg.toHex()}")
        val enableType = msg[0 + 3]                 // 0: 缓降器已Disable, 1: 缓降器已Enable
        val mode = msg[1 + 3]                       // 0: 长度控制模式, 1: 速度控制模式
        val speed = msg[2 + 3]                      // 当前速度 m/min
        val length = ((msg[3 + 3]).toUByte() * (256).toUInt()) + (msg[4 + 3]).toUByte()    // 已释放长度
        val state = msg[5 + 3]                      // 0: 缓降器未到限位, 1: 缓降器已到顶, 2: 缓降器已到底
        val weight = msg[6 + 3]                     // 载重 kg/LSB
        val emergencyState = msg[7 + 3]             // 0: 已解除紧急状态 1: 紧急刹车 2: 紧急熔断 3: 紧急刹车+熔断
        Log.i(
            TAG,
            "缓降器，isEnable:${enableType}, mode:${mode}, speed:${speed}, length:${length}, state:${state}, 载重:${weight}"
        )
        // 安全开关状态
        isEnable = (enableType != 0x00.toByte())
        if (isEnable) {
            mBtnOpen.visibility = View.GONE
            mBtnClose.visibility = VISIBLE
        } else {
            mBtnOpen.visibility = VISIBLE
            mBtnClose.visibility = View.GONE
        }
        // 当前放线长度
        Log.i(TAG, "length:${length}")
        if (length > (0).toUByte()) {
            mCurrentLineLength.text = "${String.format("%.1f", length.toDouble() / 10f)}m"
        }
        // 载重
//        mWeight.text = "${weight}kg"
        // 当前位置
        when (state) {
            0x00.toByte() -> {
                mCurrentLocation.setText(R.string.not_reaching_the_limit)
            }

            0x01.toByte() -> {
                mCurrentLocation.setText(R.string.reached_the_top)
            }

            else -> {
                mCurrentLocation.setText(R.string.reached_the_bottom)
            }
        }
        isEmergency = (emergencyState != 0x00.toByte())
        // 处于紧急状态
        if (isEmergency) {
            mActionContrl.visibility = View.GONE
            mEmergentContrl.visibility = VISIBLE
        } else {
            mActionContrl.visibility = VISIBLE
            mEmergentContrl.visibility = View.GONE
        }
    }

    fun ByteArray.toHex(): String =
        joinToString(separator = "") { eachByte -> "%02x ".format(eachByte) }

    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.slow_descent_device_weight, this, true)
        mSlowDescentDeviceView = findViewById(R.id.slowDescentDeviceWeight)
        mPromptBox = findViewById(R.id.prompt_box)
//        mDescentSafetySwitchSwitch = findViewById(R.id.descentSafetySwitchSwitch)
        mBtnOpen = findViewById(R.id.btn_open)
        mBtnClose = findViewById(R.id.btn_close)
//        mIsEnable = findViewById(R.id.isEnable)
        mCurrentLineLength = findViewById(R.id.currentLineLength)
//        mWeight = findViewById(R.id.weight)
        mCurrentLocation = findViewById(R.id.currentLocation)
        mBtnBySpeed = findViewById(R.id.btn_by_speed)
        mBtnByLength = findViewById(R.id.btn_by_length)
        mGroupBySpeed = findViewById(R.id.by_speed_group)
        mGruopByLength = findViewById(R.id.by_length_group)
        mSpeedEditText = findViewById(R.id.speed)
        mLengthEditText = findViewById(R.id.length)
        mBtnSpeedContrl = findViewById(R.id.btn_speed_contrl)
        mBtnRise = findViewById(R.id.btn_rise)
        mBtnDecline = findViewById(R.id.btn_decline)
        mBtnStop = findViewById(R.id.btn_stop)
        mBtnLengthContrl = findViewById(R.id.btn_length_contrl)
        mBtnPulseOn = findViewById(R.id.btn_pulse_on)
        mBtnEmergencyStop = findViewById(R.id.btn_emergency_stop)
        mBtnFusing = findViewById(R.id.btn_fusing)
        mActionContrl = findViewById(R.id.action_contrl)
        mEmergentContrl = findViewById(R.id.emergent_contrl)
        mReleaseEmergency = findViewById(R.id.btn_release_emergency)
        mBtnOk = findViewById(R.id.btn_ok)
        mBtnCancel = findViewById(R.id.btn_cancel)

        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

        mSpeedEditText.filters = arrayOf<InputFilter>(MaxValueInputFilter(20));
        mLengthEditText.filters = arrayOf<InputFilter>(MaxFValueInputFilter(30));

        // 安全开关
//        mDescentSafetySwitchSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
//            // 打开/关闭安全开关
//            slowDescentDeviceService.descentControl(isChecked)
//        }
        // 打开安全开关
        mBtnOpen.setOnClickListener {
            slowDescentDeviceService.descentControl(true)
        }
        // 关闭安全开关
        mBtnClose.setOnClickListener {
            slowDescentDeviceService.descentControl(false)
        }

        // 按速度
        mBtnBySpeed.setOnCheckedChangeListener { _, checked ->
            run {
                if (checked) {
                    liftingMethod = 1
                    mGruopByLength.clearCheck()
                    mBtnSpeedContrl.visibility = VISIBLE
                    mBtnLengthContrl.visibility = View.GONE
                }

            }
        }
        // 按长度
        mBtnByLength.setOnCheckedChangeListener { _, checked ->
            run {
                if (checked) {
                    liftingMethod = 0
                    mGroupBySpeed.clearCheck()
                    mBtnSpeedContrl.visibility = View.GONE
                    mBtnLengthContrl.visibility = VISIBLE
                }

            }
        }
        // 按速度，上升
        mBtnRise.setOnClickListener {
            if (isEnable) {
                val speedStr = mSpeedEditText.text.toString()
                Log.i(TAG, "缓降器，上升速度${speedStr}")
                var speed = 0
                if ("" != speedStr) {
                    speed = speedStr.toInt()
                }
                if (speed > 20) {
                    speed = 20
                }
                slowDescentDeviceService.actionControl(liftingMethod, 20 - speed)
            } else {
                showToast(R.string.need_to_open_safety_switch)
            }
        }
        // 按速度，下降
        mBtnDecline.setOnClickListener {
            if (isEnable) {
                val speedStr = mSpeedEditText.text.toString()
                var speed = 0
                if ("" != speedStr) {
                    speed = speedStr.toInt()
                }
                Log.i(TAG, "缓降器，下降速度${mSpeedEditText.text}")
                if (speed > 20){
                    speed = 20
                }
                speed += 20
                slowDescentDeviceService.actionControl(liftingMethod, speed)
            } else {
                showToast(R.string.need_to_open_safety_switch)
            }
        }
        // 按速度，停止，即将速度下降到0
        mBtnStop.setOnClickListener {
            if (isEnable) {
                Log.i(TAG, "=================停止================")
                slowDescentDeviceService.actionControl(liftingMethod, 20)
                Log.i(TAG, "=================停止END================")

            } else {
                showToast(R.string.need_to_open_safety_switch)
            }
        }
        // 按长度，启动
        mBtnPulseOn.setOnClickListener {
            if (isEnable) {
                val lengthStr = mLengthEditText.text.toString()
                Log.i(TAG, "缓降器，长度${mLengthEditText.text}")
                var length = 0
                if ("" != lengthStr) {
                    length = (lengthStr.toFloat() * 10).toInt()
                }
                slowDescentDeviceService.actionControl(liftingMethod, length)
            } else {
                showToast(R.string.need_to_open_safety_switch)
            }
        }
        // 紧急停止
        mBtnEmergencyStop.setOnClickListener {
            if (isEnable) {
                slowDescentDeviceService.emergencyControl(1)
            } else {
                showToast(R.string.need_to_open_safety_switch)
            }
        }

        // 紧急熔断
        mBtnFusing.setOnClickListener {
            if (isEnable) {
                mSlowDescentDeviceView.visibility = View.GONE // 隐藏内容
                mPromptBox.visibility = VISIBLE // 显示确认框
            } else {
                showToast(R.string.need_to_open_safety_switch)
            }
        }
        // 确定熔断
        mBtnOk.setOnClickListener {
            slowDescentDeviceService.emergencyControl(2)
            mSlowDescentDeviceView.visibility = VISIBLE // 显示内容
            mPromptBox.visibility = View.GONE // 隐藏确认框
        }
        // 取消熔断
        mBtnCancel.setOnClickListener {
            mSlowDescentDeviceView.visibility = VISIBLE // 显示内容
            mPromptBox.visibility = View.GONE // 隐藏确认框
        }

        // 解除紧急状态
        mReleaseEmergency.setOnClickListener {
            if (isEnable) {
                slowDescentDeviceService.emergencyControl(0)
            } else {
                showToast(R.string.need_to_open_safety_switch)
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
        val task = object : TimerTask() {
            override fun run() {
                if (!slowDescentDeviceService.getIsConnected()) {
                    // 尝试重连
                    slowDescentDeviceService.connect()
                }
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.scheduleAtFixedRate(task, 100, 1000);
    }
}