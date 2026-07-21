package com.example.yikupayloadexample

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.yikupayloadexample.component.ModeItem
import com.example.yikupayloadexample.component.ModeSelectionDialog
import com.yiku.yikupayloadSDK.protocol.THROWER_STATE
import com.yiku.yikupayloadSDK.service.ThrowerService
import com.yiku.yikupayloadSDK.util.MsgCallback
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.DecimalFormat
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread
import kotlin.math.roundToInt
import kotlin.system.exitProcess

class ThrowerWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "ThrowerWeight"
    var throwerService: ThrowerService
    private lateinit var mThrower_6_way: LinearLayout
    private lateinit var mThrower_4_way: LinearLayout
    private lateinit var mBombSettingRow2: LinearLayout
    private lateinit var mStatusRow2: LinearLayout
    private lateinit var mStatusRow3: LinearLayout
    private lateinit var mThrowerSafetySwitch: Switch
    private lateinit var mDetonationSettingsBtn: Button
    private lateinit var mThrowerAllowDetonationSwitch_1: Switch
    private lateinit var mThrowerAllowDetonationSwitch_2: Switch
    private lateinit var mThrowerAllowDetonationSwitch_3: Switch
    private lateinit var mThrowerAllowDetonationSwitch_4: Switch
    private lateinit var mBombState_1: TextView
    private lateinit var mBombState_2: TextView
    private lateinit var mBombState_3: TextView
    private lateinit var mBombState_4: TextView
    private lateinit var mWeight1: TextView
    private lateinit var mWeight2: TextView
//    private lateinit var mTemperature: TextView
    private lateinit var statusDot: View
    private lateinit var background: GradientDrawable
    private lateinit var mConnectState: TextView
    private lateinit var mHeight: TextView
    private var mBtnArr = arrayOfNulls<Button>(6)
    private lateinit var mOpenAll: Button
    private lateinit var mThrowerView: View
    private lateinit var mDetonationSettingsView: View
    private lateinit var mPromptView: View
    private lateinit var mOKBtn: Button
    private lateinit var mDetonateHeightEditText: EditText
    private lateinit var mPromptOkBtn: Button
    private lateinit var mPromptCancelBtn: Button
    private lateinit var mThrowerUpdate: Button
    private lateinit var mUpdateView: View
    private lateinit var mUpdateOkBtn: Button
    private lateinit var mUpdateCancelBtn: Button
    private lateinit var mCenterBtn: Button
    private lateinit var mLeftBtn: Button
    private lateinit var mRightBtn: Button
    private lateinit var mWeightSettingBtn: Button
    private lateinit var mWeightSettingsView: LinearLayout
    private lateinit var mCalibrationWeight: EditText
    private lateinit var mCalibration1Btn: Button
    private lateinit var mCalibration2Btn: Button
    private lateinit var mPeelBtn: Button
    private lateinit var mFactoryResetBtn: Button
    private lateinit var mWeightSettingBackBtn: Button

    private var updateTime = Date().time
    private var canDetonate_1 = false // 1号弹是否可以引爆
    private var canDetonate_2 = false // 2号弹是否可以引爆
    private var canDetonate_3 = false // 3号弹是否可以引爆
    private var canDetonate_4 = false // 4号弹是否可以引爆

    private var detonateHeight = 0;
    private var isInit = false
    private var isConnecting: Boolean = false
    private var isCharging_1: Boolean = false
    private var isCharging_2: Boolean = false
    private var isCharging_3: Boolean = false
    private var isCharging_4: Boolean = false

    private val modeItems = listOf(
        ModeItem("4", context.resources.getString(R.string.throw_4_way)),
        ModeItem("6", context.resources.getString(R.string.throw_6_way))
    )
    private var currentMode = modeItems[1]
    private lateinit var modeSelectorLayout: LinearLayout
    private lateinit var currentModeText: TextView
    private var throwerMode: Int = 6

    //
    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        throwerService = ThrowerService()
        val host = preferences?.getString("ThrowerHost", "")
        if(host != null && "" != host) {
            throwerService.setIp(host)
        }
        initView(context)
        throwerService.registMsgCallback(object : MsgCallback {
            override fun getId(): String {
                return "ThrowerWeightCallback"
            }

            override fun onMsg(msg: ByteArray) {
                Log.i(TAG, "msg:${msg.toHex()}")
                if (msg[0] != 0x8d.toByte()) {
                    return
                }
                if (msg[2] == THROWER_STATE.toByte()) {
                    updateTime = Date().time
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        mConnectState.setText(R.string.connection_status_connected)
                        background.setColor(ContextCompat.getColor(context, R.color.green))
                    }
                    // 更新状态
                    updateStatus(msg)
                    updateWeight(msg)
                }
            }

        })
        currentModeText.text = currentMode.name
        modeSelectorLayout.setOnClickListener {
            showModeSelectionDialog()
        }
    }


    fun updateStatus(msg: ByteArray) {
        // 总状态
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            // 高度
            mHeight.text = msg[0 + 3].toUByte().toString() + "m"
            // 起爆高度
            detonateHeight = msg[1 + 3].toInt()

            background.setColor(ContextCompat.getColor(context, R.color.green))
            updateBombStateText(1, msg.slice(11 until 19).toByteArray())
            updateBombStateText(2, msg.slice(19 until 27).toByteArray())
            updateBombStateText(3, msg.slice(27 until 35).toByteArray())
            updateBombStateText(4, msg.slice(35 until 43).toByteArray())
        }
    }

    fun updateWeight(msg: ByteArray) {
        val df = DecimalFormat("#0.#")   // 修改为 "#0.#" 避免 ".5" 这种显示
        val short1 = ByteBuffer.wrap(byteArrayOf(msg[5], msg[6]))
            .order(ByteOrder.LITTLE_ENDIAN)  // 注意你的高低字节顺序：msg[6]是高字节，msg[5]是低字节 → LITTLE_ENDIAN
            .short
        val weight1 = df.format(short1 / 10.0)

        val short2 = ByteBuffer.wrap(byteArrayOf(msg[7], msg[8]))
            .order(ByteOrder.LITTLE_ENDIAN)
            .short
        val weight2 = df.format(short2 / 10.0)
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            mWeight1.text = "$weight1 kg"
            mWeight2.text = "$weight2 kg"
            mStatusRow3.visibility = VISIBLE
        }
    }

    private fun showModeSelectionDialog() {
        val dialog = ModeSelectionDialog(
            title = context.resources.getString(R.string.select_the_type_of_thrower),
            context = context,
            modes = modeItems,
            currentMode = currentMode
        ) {
            selectedMode ->
                currentMode = selectedMode
                currentModeText.text = currentMode.name
                throwerMode = currentMode.id.toInt()
                initModeView()
        }
        dialog.show()
    }

    private fun updateBombStateText(bombIndex: Int, stateData: ByteArray) {
        val canDetonate = stateData[4] !== 0x00.toByte()
        val stateText: String
        val stateTextColor: Int
        val isCharging: Boolean

        if (canDetonate) { // 可以引爆
            stateText = resources.getString(R.string.can_detonate)
            isCharging = true
            stateTextColor = resources.getColor(R.color.green)
        } else { // 无法引爆
            stateTextColor = resources.getColor(R.color.red)
            // 未允许引爆
            if(stateData[0] == 0x00.toByte()) {
                stateText = resources.getString(R.string.cannot_detonate_notAllow)
                isCharging = false
            }
            // 正在充电
            else if(stateData[0] == 0x02.toByte()) {
                stateText = resources.getString(R.string.charging)
                isCharging = true
            }
            // 未连接
            else if(stateData[0] == 127.toByte()) {
                stateText = resources.getString(R.string.not_connected)
                isCharging = false
            }
            // 高度不够，飞机高度-引爆高度<=22米
            else if(stateData[1] == 0x00.toByte()) {
                stateText = resources.getString(R.string.cannot_detonate_tooLow)
                isCharging = true
            }
            else {
                stateText = resources.getString(R.string.cannot_detonate)
                isCharging = true
            }
        }

        when(bombIndex) {
            1 -> { // 1号弹
                canDetonate_1 = canDetonate
                mBombState_1.text = stateText
                mBombState_1.setTextColor(stateTextColor)
                isCharging_1 = isCharging
            }
            2 -> { // 2号弹
                canDetonate_2 = canDetonate
                mBombState_2.text = stateText
                mBombState_2.setTextColor(stateTextColor)
                isCharging_2 = isCharging
            }
            3 -> { // 3号弹
                canDetonate_3 = canDetonate
                mBombState_3.text = stateText
                mBombState_3.setTextColor(stateTextColor)
                isCharging_3 = isCharging
            }
            4 -> { // 4号弹
                canDetonate_4 = canDetonate
                mBombState_4.text = stateText
                mBombState_4.setTextColor(stateTextColor)
                isCharging_4 = isCharging
            }
        }
    }

    // 更新按键状态
    fun updateBtn(type: String, index: Int) {
        when (type) {
            "toOpen" -> {
                mBtnArr[index]?.text =
                    "${context.resources.getString(R.string.passageway)}${index + 1}:${
                        context.resources.getString(R.string.open)
                    }"
                mBtnArr[index]?.isEnabled = true
                mBtnArr[index]?.setOnClickListener {
                    toOpen(index)
                }
            }
            "toClose" -> {
                mBtnArr[index]?.text =
                    "${context.resources.getString(R.string.passageway)}${index + 1}:${
                        context.resources.getString(R.string.off)
                    }"
                mBtnArr[index]?.isEnabled = true
                mBtnArr[index]?.setOnClickListener {
                    close(index)
                }
            }
            "opening" -> {
                mBtnArr[index]?.setText(R.string.opening)
                mBtnArr[index]?.isEnabled = false
            }
            "closing" -> {
                mBtnArr[index]?.setText(R.string.closing)
                mBtnArr[index]?.isEnabled = false
            }
        }
    }

    // 打开前的判断
    fun toOpen(index: Int) {
        if (!mThrowerSafetySwitch.isChecked) {
            showToast(R.string.need_to_open_safety_switch)
        } else {
            val isCanDetonate = when(throwerMode) {
                6 -> canDetonate_1 && canDetonate_2 && canDetonate_3 && canDetonate_4
                4 -> canDetonate_1 && canDetonate_2
                else -> canDetonate_1 && canDetonate_2
            }
            // 如果可以引爆就直接打开，如果不可以引爆就弹提示窗口，确认后再打开
            if(isCanDetonate) {
                open(index)
            }
            else {
                mThrowerView.visibility = GONE
                mPromptView.visibility = VISIBLE
                // 确认框，确认
                mPromptOkBtn.setOnClickListener {
                    mThrowerView.visibility = VISIBLE
                    mPromptView.visibility = GONE
                    open(index)
                }
            }
        }
    }

    // 开
    fun open(index: Int) {
        throwerService.open(index)
        mBtnArr[index]?.setText(R.string.opening)
        mBtnArr[index]?.isEnabled = false

        val timer = Timer()
        val task = object : TimerTask() {
            override fun run() {
                mThrowerView.post {
                    mBtnArr[index]?.text =
                        "${context.resources.getString(R.string.passageway)}${index + 1}:${
                            context.resources.getString(R.string.off)
                        }"
                    mBtnArr[index]?.isEnabled = true
                    mBtnArr[index]?.setOnClickListener {
                        close(index)
                    }
                }
            }
        }
        timer.schedule(task, 2000)
    }


    // 关
    fun close(index: Int) {
        try {
            throwerService.close(index)
            mBtnArr[index]?.setText(R.string.closing)

            val timer = Timer()
            val task = object : TimerTask() {
                override fun run() {
                    mThrowerView.post {
                        mBtnArr[index]?.text =
                            "${context.resources.getString(R.string.passageway)}${index + 1}:${
                                context.resources.getString(R.string.open)
                            }"
                        mBtnArr[index]?.isEnabled = true
                        mBtnArr[index]?.setOnClickListener {
                            toOpen(index)
                        }
                    }
                }
            }
            timer.schedule(task, 2000)
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(R.string.operation_failed)
        }
    }

    private fun initView(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.thrower_weight, this, true)
        mThrower_6_way = findViewById(R.id.thrower_6_way)
        mThrower_4_way = findViewById(R.id.thrower_4_way)
        mBombSettingRow2 = findViewById(R.id.bombSettingRow2)
        mStatusRow2 = findViewById(R.id.statusRow2)
        mStatusRow3 = findViewById(R.id.statusRow3)
        mThrowerView = findViewById(R.id.throwerWeight)
        mDetonationSettingsView = findViewById(R.id.detonationSettingsView)
        mPromptView = findViewById(R.id.promptView)
        mThrowerSafetySwitch = findViewById(R.id.throwerSafetySwitch)
        mDetonationSettingsBtn = findViewById(R.id.detonationSettingsBtn)
        mThrowerAllowDetonationSwitch_1 = findViewById(R.id.throwerAllowDetonationSwitch_1)
        mThrowerAllowDetonationSwitch_2 = findViewById(R.id.throwerAllowDetonationSwitch_2)
        mThrowerAllowDetonationSwitch_3 = findViewById(R.id.throwerAllowDetonationSwitch_3)
        mThrowerAllowDetonationSwitch_4 = findViewById(R.id.throwerAllowDetonationSwitch_4)
        mBombState_1 = findViewById(R.id.bombState_1)
        mBombState_2 = findViewById(R.id.bombState_2)
        mBombState_3 = findViewById(R.id.bombState_3)
        mBombState_4 = findViewById(R.id.bombState_4)
        mWeight1 = findViewById(R.id.weight_1)
        mWeight2 = findViewById(R.id.weight_2)
//        mTemperature = findViewById(R.id.temperature)
        statusDot = findViewById(R.id.statusDot)
        background = statusDot.background as GradientDrawable
        mConnectState = findViewById(R.id.connectState)
        mHeight = findViewById(R.id.height)
        mDetonateHeightEditText = findViewById(R.id.detonateHeight)
        mOKBtn = findViewById(R.id.OKBtn)
        mPromptOkBtn = findViewById(R.id.promptOkBtn)
        mPromptCancelBtn = findViewById(R.id.promptCancelBtn)
        mCenterBtn = findViewById(R.id.switchCenter)
        mThrowerUpdate = findViewById(R.id.throwerUpdate)
        mUpdateView = findViewById(R.id.updateView)
        mUpdateOkBtn = findViewById(R.id.updateOkBtn)
        mUpdateCancelBtn = findViewById(R.id.updateCancelBtn)
        modeSelectorLayout = findViewById(R.id.mode_selector_layout)
        currentModeText = findViewById(R.id.current_mode_text)
        mWeightSettingBtn = findViewById(R.id.weightSettingBtn)
        mWeightSettingsView = findViewById(R.id.weightSettingsView)
        mCalibrationWeight = findViewById(R.id.calibrationWeight)
        mCalibration1Btn = findViewById(R.id.calibration1Btn)
        mCalibration2Btn = findViewById(R.id.calibration2Btn)
        mPeelBtn = findViewById(R.id.peelBtn)
        mFactoryResetBtn = findViewById(R.id.factoryResetBtn)
        mWeightSettingBackBtn = findViewById(R.id.weightSettingBackBtn)

        initModeView()

        // 中间双舵机
        mCenterBtn.text = "${context.resources.getString(R.string.center)}:${
            context.resources.getString(R.string.open)
        }"
        mCenterBtn.setOnClickListener {
            toOpenTwo("center")
        }
        // 打开起爆设置页面
        mDetonationSettingsBtn.setOnClickListener {
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                mDetonateHeightEditText.setText(detonateHeight.toString()) // 默认显示当前的起爆高度
                // 更新允许引爆开关状态
                mThrowerAllowDetonationSwitch_1.isChecked = isCharging_1
                mThrowerAllowDetonationSwitch_2.isChecked = isCharging_2
                mThrowerAllowDetonationSwitch_3.isChecked = isCharging_3
                mThrowerAllowDetonationSwitch_4.isChecked = isCharging_4

                mThrowerView.visibility = GONE
                mDetonationSettingsView.visibility = VISIBLE
            }
        }
        // 点击“确定”，设置起爆高度，然后关闭起爆设置页面
        mOKBtn.setOnClickListener {
            val heightStr = mDetonateHeightEditText.text.toString()
            var height = 0
            if ("" != heightStr) {
                height = heightStr.toInt()
            }
            throwerService.setDetonateHeight(height) // 设置起爆高度
            mThrowerView.visibility = VISIBLE
            mDetonationSettingsView.visibility = GONE
        }

        // 打开称重设置界面
        mWeightSettingBtn.setOnClickListener {
            mThrowerView.visibility = GONE
            mWeightSettingsView.visibility = VISIBLE
        }
        // 重量标定1
        mCalibration1Btn.setOnClickListener {
            var calibrationWeight = 0
            // 安全获取文本，避免空指针
            val calibrationWeightStr = mCalibrationWeight?.text?.toString().orEmpty()
            // 使用 toFloatOrNull 避免异常
            val weightValue = calibrationWeightStr.toFloatOrNull()

            if (weightValue != null && weightValue >= 0) { // 假设只接受非负数，根据业务调整
                // 使用 Math.round 实现四舍五入到十分位（乘以10后四舍五入）
                calibrationWeight = (weightValue * 10).roundToInt()
            }
            throwerService.weightCalibration(1, calibrationWeight)
        }
        // 重量标定2
        mCalibration2Btn.setOnClickListener {
            var calibrationWeight = 0
            // 安全获取文本，避免空指针
            val calibrationWeightStr = mCalibrationWeight?.text?.toString().orEmpty()
            // 使用 toFloatOrNull 避免异常
            val weightValue = calibrationWeightStr.toFloatOrNull()

            if (weightValue != null && weightValue >= 0) { // 假设只接受非负数，根据业务调整
                // 使用 Math.round 实现四舍五入到十分位（乘以10后四舍五入）
                calibrationWeight = (weightValue * 10).roundToInt()
            }
            throwerService.weightCalibration(2, calibrationWeight)
        }
        // 去皮
        mPeelBtn.setOnClickListener {
            throwerService.weightPeel()
        }
        // 恢复出厂设置
        mFactoryResetBtn.setOnClickListener {
            throwerService.factoryReset()
        }
        // 返回
        mWeightSettingBackBtn.setOnClickListener {
            mThrowerView.visibility = VISIBLE
            mWeightSettingsView.visibility = GONE
        }

        // 确认框，取消
        mPromptCancelBtn.setOnClickListener {
            mThrowerView.visibility = VISIBLE
            mPromptView.visibility = GONE
        }
        // 打开更新程序确认窗口
        mThrowerUpdate.setOnClickListener {
            mUpdateView.visibility = VISIBLE
            mThrowerView.visibility = GONE
        }
        // 确认更新程序
        mUpdateOkBtn.setOnClickListener {
            throwerService.throwerUpdate()
            mUpdateView.visibility = GONE
            mThrowerView.visibility = VISIBLE
        }
        // 取消更新程序
        mUpdateCancelBtn.setOnClickListener {
            mUpdateView.visibility = GONE
            mThrowerView.visibility = VISIBLE
        }
        // 1号弹允许起爆和充电
        mThrowerAllowDetonationSwitch_1.setOnClickListener {
            throwerService.chargingAndAllowDetonation(1, mThrowerAllowDetonationSwitch_1.isChecked)
            mThrowerAllowDetonationSwitch_1.isEnabled = false
            thread {
                Thread.sleep(8000)
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    if (mThrowerAllowDetonationSwitch_1.isChecked != isCharging_1) {
                        showToast(R.string.operation_failed)
                        mThrowerAllowDetonationSwitch_1.isChecked = isCharging_1
                    }
                    mThrowerAllowDetonationSwitch_1.isEnabled = true
                }
            }
        }
        // 2号弹允许起爆和充电
        mThrowerAllowDetonationSwitch_2.setOnClickListener {
            throwerService.chargingAndAllowDetonation(2, mThrowerAllowDetonationSwitch_2.isChecked)
            mThrowerAllowDetonationSwitch_2.isEnabled = false
            thread {
                Thread.sleep(8000)
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    if (mThrowerAllowDetonationSwitch_2.isChecked != isCharging_2) {
                        showToast(R.string.operation_failed)
                        mThrowerAllowDetonationSwitch_2.isChecked = isCharging_2
                    }
                    mThrowerAllowDetonationSwitch_2.isEnabled = true
                }
            }
        }
        // 3号弹允许起爆和充电
        mThrowerAllowDetonationSwitch_3.setOnClickListener {
            throwerService.chargingAndAllowDetonation(3, mThrowerAllowDetonationSwitch_3.isChecked)
            mThrowerAllowDetonationSwitch_3.isEnabled = false
            thread {
                Thread.sleep(8000)
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    if (mThrowerAllowDetonationSwitch_3.isChecked != isCharging_3) {
                        showToast(R.string.operation_failed)
                        mThrowerAllowDetonationSwitch_3.isChecked = isCharging_3
                    }
                    mThrowerAllowDetonationSwitch_3.isEnabled = true
                }
            }
        }
        // 4号弹允许起爆和充电
        mThrowerAllowDetonationSwitch_4.setOnClickListener {
            throwerService.chargingAndAllowDetonation(4, mThrowerAllowDetonationSwitch_4.isChecked)
            mThrowerAllowDetonationSwitch_4.isEnabled = false
            thread {
                Thread.sleep(8000)
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    if (mThrowerAllowDetonationSwitch_4.isChecked != isCharging_4) {
                        showToast(R.string.operation_failed)
                        mThrowerAllowDetonationSwitch_4.isChecked = isCharging_4
                    }
                    mThrowerAllowDetonationSwitch_4.isEnabled = true
                }
            }
        }
        setConnectState()
    }

    private fun initModeView() {
        // 6路抛投
        if(throwerMode == 6) {
            mThrower_6_way.visibility = VISIBLE
            mThrower_4_way.visibility = GONE
            mBombSettingRow2.visibility = VISIBLE
            mStatusRow2.visibility = VISIBLE
            mOpenAll = findViewById(R.id.openAll)
            mLeftBtn = findViewById(R.id.switchLeft)
            mRightBtn = findViewById(R.id.switchRight)
            mRightBtn.setOnClickListener {
                toOpenTwo("right")
            }

            // 按键
            for (btnIndex in 0 until throwerMode) {
                mBtnArr[btnIndex] = findViewById(
                    context.resources.getIdentifier(
                        "switch${btnIndex + 1}",
                        "id",
                        context.packageName
                    )
                )
                updateBtn("toOpen", btnIndex)
            }
        }
        // 4路抛投
        else if(throwerMode == 4) {
            mThrower_6_way.visibility = GONE
            mThrower_4_way.visibility = VISIBLE
            mBombSettingRow2.visibility = GONE
            mStatusRow2.visibility = GONE
            mOpenAll = findViewById(R.id.openAll_4_way)
            mLeftBtn = findViewById(R.id.switchLeft_4_way)
            mRightBtn = findViewById(R.id.switchRight_4_way)
            mRightBtn.setOnClickListener {
                toOpenTwo("center")
            }
            // 按键
            for (btnIndex in 0 until throwerMode) {
                mBtnArr[btnIndex] = findViewById(
                    context.resources.getIdentifier(
                        "switch${btnIndex + 1}_4_way",
                        "id",
                        context.packageName
                    )
                )
                updateBtn("toOpen", btnIndex)
            }
        }
        // 打开全部通道
        mOpenAll.setOnClickListener {
            toOpenAll()
        }
        // 左侧双舵机
        mLeftBtn.text = "${context.resources.getString(R.string.left)}:${
            context.resources.getString(R.string.open)
        }"
        mLeftBtn.setOnClickListener {
            toOpenTwo("left")
        }
        // 右侧双舵机
        mRightBtn.text = "${context.resources.getString(R.string.right)}:${
            context.resources.getString(R.string.open)
        }"
    }

    // 将所有按键重置
    private fun resetThrowerBtn() {
        throwerService.closeAll()
        for (btnIndex in mBtnArr.indices) {
            updateBtn("toOpen", btnIndex)
        }
        // 中间双舵机
        mCenterBtn.text = "${context.resources.getString(R.string.center)}:${
            context.resources.getString(R.string.open)
        }"
        mCenterBtn.setOnClickListener {
            toOpenTwo("center")
        }
        // 左侧双舵机
        mLeftBtn.text = "${context.resources.getString(R.string.left)}:${
            context.resources.getString(R.string.open)
        }"
        mLeftBtn.setOnClickListener {
            toOpenTwo("left")
        }
        // 右侧双舵机
        mRightBtn.text = "${context.resources.getString(R.string.right)}:${
            context.resources.getString(R.string.open)
        }"
        mRightBtn.setOnClickListener {
            toOpenTwo("right")
        }
    }
    // 点击打开双舵机时判断
    private fun toOpenTwo(type: String) {
        try {
            if (!mThrowerSafetySwitch.isChecked) {
                showToast(R.string.need_to_open_safety_switch)
            } else {
                val isCanDetonate = when(throwerMode) {
                    6 -> canDetonate_1 && canDetonate_2 && canDetonate_3 && canDetonate_4
                    4 -> canDetonate_1 && canDetonate_2
                    else -> canDetonate_1 && canDetonate_2
                }
                // 如果可以引爆就直接打开，如果不可以引爆就弹提示窗口，确认后再打开
                if(isCanDetonate) {
                    openTwo(type)
                }
                else {
                    mThrowerView.visibility = GONE
                    mPromptView.visibility = VISIBLE
                    // 确认框，确认
                    mPromptOkBtn.setOnClickListener {
                        mThrowerView.visibility = VISIBLE
                        mPromptView.visibility = GONE
                        openTwo(type)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(R.string.operation_failed)
        }
    }

    // 打开双舵机
    private fun openTwo(type: String) {
        try {
            when (type) {
                "center" -> {
                    throwerService.openCenter()
                    if(throwerMode == 6) {
                        mCenterBtn.setText(R.string.opening)
                        mCenterBtn.isEnabled = false
                    }
                    else if(throwerMode == 4) {
                        mRightBtn.setText(R.string.opening)
                        mRightBtn.isEnabled = false
                    }
                    // 第3通道
                    updateBtn("opening", 2)
                    // 第6通道
                    updateBtn("opening", 5-2)
                }
                "left" -> {
                    throwerService.openLeft()
                    mLeftBtn.setText(R.string.opening)
                    mLeftBtn.isEnabled = false
                    // 第1通道
                    updateBtn("opening", 0)
                    // 第2通道
                    updateBtn("opening", 1)
                }
                "right" -> {
                    throwerService.openRight()
                    mRightBtn.setText(R.string.opening)
                    mRightBtn.isEnabled = false
                    // 第7通道
                    updateBtn("opening", 6-2)
                    // 第8通道
                    updateBtn("opening", 7-2)
                }
            }

            val timer = Timer()
            val task = object : TimerTask() {
                override fun run() {
                    mThrowerView.post {
                        when (type) {
                            "center" -> {
                                if(throwerMode == 6) {
                                    mCenterBtn.isEnabled = true
                                    mCenterBtn.text ="${context.resources.getString(R.string.center)}:${ context.resources.getString(R.string.off)}"
                                    mCenterBtn.setOnClickListener {
                                        closeTwo("center")
                                    }
                                }
                                else if(throwerMode == 4) {
                                    mRightBtn.isEnabled = true
                                    mRightBtn.text ="${context.resources.getString(R.string.right)}:${ context.resources.getString(R.string.off)}"
                                    mRightBtn.setOnClickListener {
                                        closeTwo("center")
                                    }
                                }
                                // 第3通道
                                updateBtn("toClose", 2)
                                // 第6通道
                                updateBtn("toClose", 5-2)
                            }
                            "left" -> {
                                mLeftBtn.isEnabled = true
                                mLeftBtn.text ="${context.resources.getString(R.string.left)}:${ context.resources.getString(R.string.off)}"
                                mLeftBtn.setOnClickListener {
                                    closeTwo("left")
                                }
                                // 第1通道
                                updateBtn("toClose", 0)
                                // 第2通道
                                updateBtn("toClose", 1)
                            }
                            "right" -> {
                                mRightBtn.isEnabled = true
                                mRightBtn.text ="${context.resources.getString(R.string.right)}:${ context.resources.getString(R.string.off)}"
                                mRightBtn.setOnClickListener {
                                    closeTwo("right")
                                }
                                // 第7通道
                                updateBtn("toClose", 6-2)
                                // 第8通道
                                updateBtn("toClose", 7-2)
                            }
                        }
                    }
                }
            }
            timer.schedule(task, 2000)
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(R.string.operation_failed)
        }
    }

    // 关闭双舵机
    private fun closeTwo(type: String) {
        try {
            when (type) {
                "center" -> {
                    throwerService.closeCenter()
                    if(throwerMode == 6) {
                        mCenterBtn.setText(R.string.closing)
                        mCenterBtn.isEnabled = false
                    }
                    else if(throwerMode == 4) {
                        mRightBtn.setText(R.string.closing)
                        mRightBtn.isEnabled = false
                    }
                    // 第3通道
                    updateBtn("closing", 2)
                    // 第6通道
                    updateBtn("closing", 5-2)
                }
                "left" -> {
                    throwerService.closeLeft()
                    mLeftBtn.setText(R.string.closing)
                    mLeftBtn.isEnabled = false
                    // 第1通道
                    updateBtn("closing", 0)
                    // 第2通道
                    updateBtn("closing", 1)
                }
                "right" -> {
                    throwerService.closeRight()
                    mRightBtn.setText(R.string.closing)
                    mRightBtn.isEnabled = false
                    // 第7通道
                    updateBtn("closing", 6-2)
                    // 第8通道
                    updateBtn("closing", 7-2)
                }
            }

            val timer = Timer()
            val task = object : TimerTask() {
                override fun run() {
                    mThrowerView.post {
                        when (type) {
                            "center" -> {
                                if(throwerMode == 6) {
                                    mCenterBtn.isEnabled = true
                                    mCenterBtn?.text ="${context.resources.getString(R.string.center)}:${ context.resources.getString(R.string.open)}"
                                    mCenterBtn.setOnClickListener {
                                        toOpenTwo("center")
                                    }
                                }
                                else if(throwerMode == 4) {
                                    mRightBtn.isEnabled = true
                                    mRightBtn.text ="${context.resources.getString(R.string.right)}:${ context.resources.getString(R.string.open)}"
                                    mRightBtn.setOnClickListener {
                                        toOpenTwo("center")
                                    }
                                }
                                // 第3通道
                                updateBtn("toOpen", 2)
                                // 第6通道
                                updateBtn("toOpen", 5-2)
                            }
                            "left" -> {
                                mLeftBtn.isEnabled = true
                                mLeftBtn?.text ="${context.resources.getString(R.string.left)}:${ context.resources.getString(R.string.open)}"
                                mLeftBtn.setOnClickListener {
                                    toOpenTwo("left")
                                }
                                // 第1通道
                                updateBtn("toOpen", 0)
                                // 第2通道
                                updateBtn("toOpen", 1)
                            }
                            "right" -> {
                                mRightBtn.isEnabled = true
                                mRightBtn?.text ="${context.resources.getString(R.string.right)}:${ context.resources.getString(R.string.open)}"
                                mRightBtn.setOnClickListener {
                                    toOpenTwo("right")
                                }
                                // 第7通道
                                updateBtn("toOpen", 6-2)
                                // 第8通道
                                updateBtn("toOpen", 7-2)
                            }
                        }
                    }
                }
            }
            timer.schedule(task, 2000)
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(R.string.operation_failed)
        }
    }

    // 点击全开时判断
    private fun toOpenAll() {
        try {
            if (!mThrowerSafetySwitch.isChecked) {
                showToast(R.string.need_to_open_safety_switch)
            } else {
                val isCanDetonate = when(throwerMode) {
                    6 -> canDetonate_1 && canDetonate_2 && canDetonate_3 && canDetonate_4
                    4 -> canDetonate_1 && canDetonate_2
                    else -> canDetonate_1 && canDetonate_2
                }
                // 如果可以引爆就直接打开，如果不可以引爆就弹提示窗口，确认后再打开
                if(isCanDetonate) {
                    openAll()
                }
                else {
                    mThrowerView.visibility = GONE
                    mPromptView.visibility = VISIBLE
                    // 确认框，确认
                    mPromptOkBtn.setOnClickListener {
                        mThrowerView.visibility = VISIBLE
                        mPromptView.visibility = GONE
                        openAll()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(R.string.operation_failed)
        }
    }

    // 打开全部通道
    private fun openAll() {
        try {
            throwerService.openAll()
            mOpenAll.setText(R.string.opening)
            mOpenAll.isEnabled = false
            for (btnIndex in mBtnArr.indices) {
                mBtnArr[btnIndex]?.setText(R.string.opening)
                mBtnArr[btnIndex]?.isEnabled = false
            }
            // 中间双舵机
            mCenterBtn.setText(R.string.opening)
            mCenterBtn.isEnabled = false
            // 左侧双舵机
            mLeftBtn.setText(R.string.opening)
            mLeftBtn.isEnabled = false
            // 右侧双舵机
            mRightBtn.setText(R.string.opening)
            mRightBtn.isEnabled = false

            val timer = Timer()
            val task = object : TimerTask() {
                override fun run() {
                    mThrowerView.post {
                        for (btnIndex in mBtnArr.indices) {
                            mBtnArr[btnIndex]?.text =
                                "${context.resources.getString(R.string.passageway)}${btnIndex + 1}:${
                                    context.resources.getString(R.string.off)
                                }"
                            mBtnArr[btnIndex]?.isEnabled = true
                            mBtnArr[btnIndex]?.setOnClickListener {
                                close(btnIndex)
                            }
                        }

                        // 中间双舵机
                        mCenterBtn.text = "${context.resources.getString(R.string.center)}:${
                            context.resources.getString(R.string.off)
                        }"
                        mCenterBtn.setOnClickListener {
                            closeTwo("center")
                        }
                        mCenterBtn.isEnabled = true
                        // 左侧双舵机
                        mLeftBtn.text = "${context.resources.getString(R.string.left)}:${
                            context.resources.getString(R.string.off)
                        }"
                        mLeftBtn.setOnClickListener {
                            closeTwo("left")
                        }
                        mLeftBtn.isEnabled = true
                        // 右侧双舵机
                        mRightBtn.text = "${context.resources.getString(R.string.right)}:${
                            context.resources.getString(R.string.off)
                        }"
                        if(throwerMode == 6) {
                            mRightBtn.setOnClickListener {
                                closeTwo("right")
                            }
                        }
                        else {
                            mRightBtn.setOnClickListener {
                                closeTwo("center")
                            }
                        }
                        mRightBtn.isEnabled = true
                        // 全开、全关
                        mOpenAll.isEnabled = true
                        mOpenAll.setText(R.string.off_all)
                        mOpenAll.setOnClickListener {
                            closeAll()
                        }
                    }
                }
            }
            timer.schedule(task, 2000)
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(R.string.operation_failed)
        }
    }

    // 关闭全部通道
    private fun closeAll() {
        try {
            throwerService.closeAll()
            mOpenAll.setText(R.string.closing)
            mOpenAll.isEnabled = false
            for (btnIndex in mBtnArr.indices) {
                mBtnArr[btnIndex]?.setText(R.string.closing)
                mBtnArr[btnIndex]?.isEnabled = false
            }
            // 中间双舵机
            mCenterBtn.setText(R.string.closing)
            mCenterBtn.isEnabled = false
            // 左侧双舵机
            mLeftBtn.setText(R.string.closing)
            mLeftBtn.isEnabled = false
            // 右侧双舵机
            mRightBtn.setText(R.string.closing)
            mRightBtn.isEnabled = false

            val timer = Timer()
            val task = object : TimerTask() {
                override fun run() {
                    mThrowerView.post {
                        for (btnIndex in mBtnArr.indices) {
                            mBtnArr[btnIndex]?.text =
                                "${context.resources.getString(R.string.passageway)}${btnIndex + 1}:${
                                    context.resources.getString(R.string.open)
                                }"
                            mBtnArr[btnIndex]?.isEnabled = true
                            mBtnArr[btnIndex]?.setOnClickListener {
                                toOpen(btnIndex)
                            }
                        }

                        // 中间双舵机
                        mCenterBtn.text = "${context.resources.getString(R.string.center)}:${
                            context.resources.getString(R.string.open)
                        }"
                        mCenterBtn.setOnClickListener {
                            toOpenTwo("center")
                        }
                        mCenterBtn.isEnabled = true
                        // 左侧双舵机
                        mLeftBtn.text = "${context.resources.getString(R.string.left)}:${
                            context.resources.getString(R.string.open)
                        }"
                        mLeftBtn.setOnClickListener {
                            toOpenTwo("left")
                        }
                        mLeftBtn.isEnabled = true
                        // 右侧双舵机
                        mRightBtn.text = "${context.resources.getString(R.string.right)}:${
                            context.resources.getString(R.string.open)
                        }"

                        if(throwerMode == 6) {
                            mRightBtn.setOnClickListener {
                                toOpenTwo("right")
                            }
                        }
                        else {
                            mRightBtn.setOnClickListener {
                                toOpenTwo("center")
                            }
                        }
                        mRightBtn.isEnabled = true
                        // 全开、全关
                        mOpenAll.isEnabled = true
                        mOpenAll.setText(R.string.open_all)
                        mOpenAll.setOnClickListener {
                            toOpenAll()
                        }
                    }
                }
            }
            timer.schedule(task, 2000)
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(R.string.operation_failed)
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

    private fun showToast(msg: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(
                MApplication.applicationContext, msg, Toast.LENGTH_LONG
            ).show()
        }
    }

    // 定时器，判断连接状态
    private fun setConnectState() {
        isConnecting = true
        thread {
            while (!throwerService.connect()) {
                Thread.sleep(1000)
            }
            isConnecting = false
            // 打开app后，第一次连接成功时，重置所有舵机
            resetThrowerBtn()
            updateTime = Date().time
            getMessageTime()
        }
    }

    // 定时器，判断消息接收情况
    private fun getMessageTime() {
        val timer = Timer();
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask() {
            override fun run() {
                // 已连接
                if (throwerService.getIsConnected()) {
                    // 定时发送消息，心跳包
                    throwerService.connectionTesting()
                    // 3秒没收到信息，显示未连接
                    if (Date().time - updateTime > 3000) {
                        handler.post {
                            mThrowerAllowDetonationSwitch_1.isChecked = false
                            mThrowerAllowDetonationSwitch_2.isChecked = false
                            mThrowerAllowDetonationSwitch_3.isChecked = false
                            mThrowerAllowDetonationSwitch_4.isChecked = false
//                        mTemperature.text = "0°C"
                            mConnectState.setText(R.string.connection_status_notconnected)
                            background.setColor(ContextCompat.getColor(context, R.color.red))
                            mHeight.text = "0m"
                        }
                    }
                    // 如果超过10s没收到消息，主动断开连接，等待重连
                    if (Date().time - updateTime > 10000) {
                        // 断连
                        throwerService.disConnect()
                    }
                }
                else{// 未连接
                    if(!isConnecting){
                        isConnecting = true
                        thread {
                            Thread.sleep(5000)// 先等待5s，防止刚断连就重连，报错
                            while (!throwerService.connect()) {
                                Thread.sleep(1000)
                            }
                            isConnecting = false
                            updateTime = Date().time
                            handler.post {
                                mConnectState.setText(R.string.connection_status_connected)
                                background.setColor(ContextCompat.getColor(context, R.color.green))
                            }
                        }
                    }
                }
            }
        }
        // 定时器，100毫秒后开始执行，每2秒执行一次
        timer.schedule(task, 100, 2000);
    }
}