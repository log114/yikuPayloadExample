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
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.yiku.yikupayloadSDK.protocol.DESCENT200_STATE_GET
import com.yiku.yikupayloadSDK.protocol.DESCENT_STATE_GET
import com.yiku.yikupayloadSDK.service.SlowDescentDevice200Service
import com.yiku.yikupayloadSDK.service.SlowDescentDeviceService
import com.yiku.yikupayloadSDK.util.MaxFValueInputFilter
import com.yiku.yikupayloadSDK.util.MaxValueInputFilter
import com.yiku.yikupayloadSDK.util.MsgCallback
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class SlowDescentDevice200Weight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "SlowDescentDevice200Weight"
    var slowDescentDevice200Service: SlowDescentDevice200Service
    private lateinit var mSlowDescentDevice200View: LinearLayout
    private lateinit var mPromptBox: LinearLayout
    private lateinit var mConnectState: TextView
    private lateinit var mSafetySwitch: Switch
    private lateinit var mPeakedText: TextView
    private lateinit var mWeightText: TextView
    private lateinit var mResetToZeroBtn: ImageButton
    private lateinit var mSpeedText: TextView
    private lateinit var mRopeLengthText: TextView
    private lateinit var mSwingAngleText: TextView
    private lateinit var mHookStatusText: TextView
    private lateinit var mHookBatteryLevelText: TextView
    private lateinit var mUpSpeedInput: EditText
    private lateinit var mDownSpeedInput: EditText
    private lateinit var mUpBtn: ImageButton
    private lateinit var mDownBtn: ImageButton
    private lateinit var mLengthInput: EditText
    private lateinit var mStartBtn: ImageButton
    private lateinit var mWarningLightBtn: Button
    private lateinit var mHookBtn: Button
    private lateinit var mStopBtn: Button
    private lateinit var mUrgentResetBtn: Button
    private lateinit var mUrgentStopBtn: Button
    private lateinit var mUrgentFuseBtn: Button
    private lateinit var mOkBtn: Button
    private lateinit var mCancelBtn:Button

    private var updateTime = Date().time
    private var isConnecting = false;
    private var isControling_safetySwitch = false
    private var isControling_warningLight = false
    private var isControling_hook = false

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        initView(context)
        slowDescentDevice200Service = SlowDescentDevice200Service()
        val host = preferences?.getString("SlowDescentDevice200Host", "")
        if(host != null && "" != host) {
            slowDescentDevice200Service.setIp(host)
        }
        slowDescentDevice200Service.msgCallbacks += object : MsgCallback {
            override fun getId(): String {
                return "SlowDescentDevice200WeightCallback"
            }

            override fun onMsg(msg: ByteArray) {
                Log.i(TAG, "200kg缓降器msg:${msg.toHex()}")
                if (msg[0] != 0x8d.toByte()) {
                    return
                }
                if (msg[2] == DESCENT200_STATE_GET.toByte()) {
                    Log.i(TAG, "recv 0x90!")
                    // 更新200kg缓降器状态
                    mSlowDescentDevice200View.post {
                        updateStatus(msg)
                    }
                }
            }

        }
    }

    // 更新缓降器状态
    fun updateStatus(msg: ByteArray) {
        Log.i(TAG, "缓降器msg:${msg.toHex()}")
        updateTime = Date().time
        mConnectState.setText(R.string.connection_status_connected)
        // 安全开关状态
        val isSafetySwitchOpen = (msg[3].toInt() == 1)
        if(!isControling_safetySwitch) {
            mSafetySwitch.isChecked = isSafetySwitchOpen
            mSafetySwitch.isEnabled = true
        }
        enableButton(isSafetySwitchOpen)
        // 触顶状态
        val peakedTextId = when(msg[4].toInt()) {
            0 -> R.string.not_peaked
            1 -> R.string.peaked
            else -> R.string.not_peaked
        }
        mPeakedText.setText(peakedTextId)
        // 红蓝灯警示状态
        if(!isControling_warningLight) {
            mWarningLightBtn.isSelected = (msg[5].toInt() == 1)
        }
        // 载重
        mWeightText.text =  "${bytesToInt(msg[6], msg[7])} kg"
        // 水平摆角
        mSwingAngleText.text = "${bytesToInt(msg[8], msg[9])}°"
        // 速度，返回的是cm/s，转为m/min显示
        mSpeedText.text = "${(msg[10].toInt() * 60).toDouble() /100} m/min"
        // 释放绳长，单位是0.1m，转为m
        mRopeLengthText.text = "${bytesToInt(msg[11], msg[12]).toDouble()/10} m"
        // 挂钩开关状态
        if(!isControling_hook) {
            mHookBtn.isSelected = (msg[13].toInt() == 1)
        }
        // 挂钩通信状态
        val hookStatusTextId = when(msg[14].toInt()) {
            0 -> R.string.connected
            1 -> R.string.not_connected
            else -> R.string.not_connected
        }
        mHookStatusText.setText(hookStatusTextId)
        // 挂钩电压，单位是0.01V，转为V
        val hookVoltage = bytesToInt(msg[15], msg[16]).toDouble() / 100
        Log.d(TAG, "电压：${hookVoltage}")
        // 根据电压计算挂钩电量
        mHookBatteryLevelText.text = "${calculateCapacity(hookVoltage)}%"
        if(hookVoltage <= 3.5) {
            mHookBatteryLevelText.setTextColor(
                ContextCompat.getColor(context, R.color.red)
            )
        }
        else {
            mHookBatteryLevelText.setTextColor(
                ContextCompat.getColor(context, R.color.white)
            )
        }
        // 主板温度msg[17]，不显示
    }

    fun enableButton (enable: Boolean) {
        mResetToZeroBtn.isEnabled = enable
        mUpBtn.isEnabled = enable
        mDownBtn.isEnabled = enable
        mStartBtn.isEnabled = enable
        if(!isControling_warningLight) {
            mWarningLightBtn.isEnabled = enable
        }
        if(!isControling_hook) {
            mHookBtn.isEnabled = enable
        }
        mStopBtn.isEnabled = enable
        mUrgentResetBtn.isEnabled = enable
        mUrgentStopBtn.isEnabled = enable
        mUrgentFuseBtn.isEnabled = enable
    }

    fun bytesToInt(byteH: Byte, byteL: Byte): Int {
        // 高位左移8位，低位保留原值，并用 0xFF 屏蔽符号位
        return ((byteH.toInt() and 0xFF) shl 8) or
                (byteL.toInt() and 0xFF)
    }

    fun ByteArray.toHex(): String =
        joinToString(separator = "") { eachByte -> "%02x ".format(eachByte) }

    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.slow_descent_device_200_weight, this, true)
        mSlowDescentDevice200View = findViewById(R.id.slowDescentDevice200Weight)
        mPromptBox = findViewById(R.id.prompt_box)
        mConnectState = findViewById(R.id.connectState)
        mSafetySwitch = findViewById(R.id.safetySwitch)
        mPeakedText = findViewById(R.id.peakedText)
        mWeightText = findViewById(R.id.weightText)
        mResetToZeroBtn = findViewById(R.id.resetToZeroBtn)
        mSpeedText = findViewById(R.id.speedText)
        mRopeLengthText = findViewById(R.id.ropeLengthText)
        mSwingAngleText = findViewById(R.id.swingAngleText)
        mHookStatusText = findViewById(R.id.hookStatusText)
        mHookBatteryLevelText = findViewById(R.id.hookBatteryLevelText)
        mUpSpeedInput = findViewById(R.id.upSpeedInput)
        mDownSpeedInput = findViewById(R.id.downSpeedInput)
        mUpBtn = findViewById(R.id.upBtn)
        mDownBtn = findViewById(R.id.downBtn)
        mLengthInput = findViewById(R.id.lengthInput)
        mStartBtn = findViewById(R.id.startBtn)
        mWarningLightBtn = findViewById(R.id.warningLightBtn)
        mHookBtn = findViewById(R.id.hookBtn)
        mStopBtn = findViewById(R.id.stopBtn)
        mUrgentResetBtn = findViewById(R.id.urgentResetBtn)
        mUrgentStopBtn = findViewById(R.id.urgentStopBtn)
        mUrgentFuseBtn = findViewById(R.id.urgentFuseBtn)
        mOkBtn = findViewById(R.id.btn_ok)
        mCancelBtn = findViewById(R.id.btn_cancel)
        setConnectState()

        mUpSpeedInput.filters = arrayOf<InputFilter>(MaxValueInputFilter(60));
        mDownSpeedInput.filters = arrayOf<InputFilter>(MaxValueInputFilter(60));
        mLengthInput.filters = arrayOf<InputFilter>(MaxFValueInputFilter(100));

        enableButton(false) // 最初所有除安全开关外的按键都不可用

        // 安全开关
        mSafetySwitch.setOnClickListener {
            isControling_safetySwitch = true
            mSafetySwitch.isEnabled = false
            slowDescentDevice200Service.descentControl(mSafetySwitch.isChecked)
            thread {
                Thread.sleep(2000)
                isControling_safetySwitch = false
            }
        }
        // 重量清零
        mResetToZeroBtn.setOnClickListener {
            slowDescentDevice200Service.resetWeight()
        }
        // 按速度上升（负值上升）
        mUpBtn.setOnClickListener {
            mDownBtn.isSelected = false
            mStartBtn.isSelected = false
            mUpBtn.isSelected = !mUpBtn.isSelected
            if(mUpBtn.isSelected) {
                val speed = mUpSpeedInput.text.toString().toInt() * 100 / 60 // 输入是m/min，要转为cm/s
                slowDescentDevice200Service.controlBySpeed(-speed)
            }
            else {
                slowDescentDevice200Service.controlBySpeed(0)
            }
        }
        // 按速度下降（正值下降）
        mDownBtn.setOnClickListener {
            mUpBtn.isSelected = false
            mStartBtn.isSelected = false
            mDownBtn.isSelected = !mDownBtn.isSelected
            if(mDownBtn.isSelected) {
                val speed = mDownSpeedInput.text.toString().toInt() * 100 / 60 // 输入是m/min，要转为cm/s
                slowDescentDevice200Service.controlBySpeed(speed)
            }
            else {
                slowDescentDevice200Service.controlBySpeed(0)
            }
        }
        // 按释放绳长操作
        mStartBtn.setOnClickListener {
            mUpBtn.isSelected = false
            mDownBtn.isSelected = false
            mStartBtn.isSelected = !mStartBtn.isSelected
            if(mStartBtn.isSelected) {
                val length = mLengthInput.text.toString().toInt()
                slowDescentDevice200Service.controlByLength(length)
            }
            else {
                slowDescentDevice200Service.controlBySpeed(0)
            }
        }
        // 红蓝警示灯开关
        mWarningLightBtn.setOnClickListener {
            isControling_warningLight = true
            mWarningLightBtn.isEnabled = false
            mWarningLightBtn.isSelected = !mWarningLightBtn.isSelected
            slowDescentDevice200Service.warningLightControl(mWarningLightBtn.isSelected)
            thread {
                Thread.sleep(2000)
                isControling_warningLight = false
            }
        }
        // 挂钩开关
        mHookBtn.setOnClickListener {
            isControling_hook = true
            mHookBtn.isEnabled = false
            mHookBtn.isSelected = !mHookBtn.isSelected
            slowDescentDevice200Service.hookControl(mHookBtn.isSelected)
            thread {
                Thread.sleep(2000)
                isControling_hook = false
            }
        }
        // 停止
        mStopBtn.setOnClickListener {
            mUpBtn.isSelected = false
            mDownBtn.isSelected = false
            mStartBtn.isSelected = false
            slowDescentDevice200Service.controlBySpeed(0)
        }
        // 复位
        mUrgentResetBtn.setOnClickListener {
            slowDescentDevice200Service.emergencyControl(0)
        }
        // 急停
        mUrgentStopBtn.setOnClickListener {
            mUpBtn.isSelected = false
            mDownBtn.isSelected = false
            mStartBtn.isSelected = false
            slowDescentDevice200Service.emergencyControl(1)
        }
        // 熔断
        mUrgentFuseBtn.setOnClickListener {
            mSlowDescentDevice200View.visibility = GONE
            mPromptBox.visibility = VISIBLE
        }
        // 确定熔断
        mOkBtn.setOnClickListener {
            slowDescentDevice200Service.emergencyControl(2)
            mSlowDescentDevice200View.visibility = VISIBLE
            mPromptBox.visibility = GONE
        }
        // 取消
        mCancelBtn.setOnClickListener {
            mSlowDescentDevice200View.visibility = VISIBLE
            mPromptBox.visibility = GONE
        }
    }

    // 电压与电量映射表（按电压从高到低排序）
    private val voltageMap = listOf(
        VoltageCapacity(4.20, 100),
        VoltageCapacity(4.10, 90),
        VoltageCapacity(4.00, 80),
        VoltageCapacity(3.90, 70),
        VoltageCapacity(3.80, 50),
        VoltageCapacity(3.70, 30),
        VoltageCapacity(3.60, 20),
        VoltageCapacity(3.50, 10),
        VoltageCapacity(3.30, 5),
        VoltageCapacity(3.00, 0)
    )
    /**
     * 根据输入电压计算剩余电量百分比
     * @param voltage 当前电压值 (单位: V)
     * @return 估算的剩余电量百分比 (0-100)
     */
    fun calculateCapacity(voltage: Double): Int {
        // 1. 边界处理：如果电压高于满电电压，直接返回100%
        if (voltage >= voltageMap.first().voltage) {
            return voltageMap.first().capacity
        }

        // 2. 边界处理：如果电压低于截止电压，直接返回0%
        if (voltage <= voltageMap.last().voltage) {
            return voltageMap.last().capacity
        }

        // 3. 遍历查找所在的区间
        for (i in 0 until voltageMap.size - 1) {
            val upperPoint = voltageMap[i]     // 高电压点
            val lowerPoint = voltageMap[i + 1] // 低电压点

            // 如果当前电压正好等于表中的某个点，直接返回对应电量
            if (voltage == upperPoint.voltage) return upperPoint.capacity
            if (voltage == lowerPoint.voltage) return lowerPoint.capacity

            // 4. 判断是否落在区间内 (upperPoint.voltage > current > lowerPoint.voltage)
            if (voltage < upperPoint.voltage && voltage > lowerPoint.voltage) {
                // 执行分段线性插值计算
                val ratio = (voltage - upperPoint.voltage) / (lowerPoint.voltage - upperPoint.voltage)
                val capacityRange = upperPoint.capacity - lowerPoint.capacity

                val calculatedCapacity = upperPoint.capacity - (capacityRange * ratio)
                return calculatedCapacity.roundToInt()
            }
        }

        // 兜底返回（理论上不会执行到这里）
        return 0
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
                if (!slowDescentDevice200Service.getIsConnected()) {
                    if(!isConnecting){
                        isConnecting = true
                        thread {
                            Thread.sleep(5000)// 先等待5s，防止刚断连就重连，报错
                            while (!slowDescentDevice200Service.connect()) {
                                Thread.sleep(1000)
                            }
                            isConnecting = false
                            updateTime = Date().time
                        }
                    }
                }
                else {
                    // 3秒没收到信息，显示未连接
                    if (Date().time - updateTime > 3000) {
                        val handler = Handler(Looper.getMainLooper())
                        handler.post {
                            mConnectState.setText(R.string.connection_status_notconnected)
                        }
                    }
                    // 如果超过5s没收到消息，主动断开连接，等待重连
                    if (Date().time - updateTime > 5000) {
                        // 断连
                        slowDescentDevice200Service.disConnect()
                    }
                }
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.schedule(task, 100, 1000);
    }
}

data class VoltageCapacity(val voltage: Double, val capacity: Int)