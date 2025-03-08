package com.example.yikupayloadexample

import android.content.Context
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
import com.yiku.yikupayload.protocol.THROWER_STATE
import com.yiku.yikupayload.service.ThrowerService
import com.yiku.yikupayload.util.MsgCallback
import java.lang.Exception
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class ThrowerWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "ThrowerWeight"
    var throwerService: ThrowerService
    private lateinit var mThrowerSafetySwitch: Switch
    private lateinit var mDetonationSettingsBtn: Button
    private lateinit var mThrowerChargingSwitch: Switch
    private lateinit var mThrowerAllowDetonationSwitch: Switch
//    private lateinit var mTemperature: TextView
    private lateinit var mConnectState: TextView
    private lateinit var mHeight: TextView
    private lateinit var mBombState1: TextView
    private lateinit var mBombState2: TextView
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
    private var mPassagewayBoxArr = arrayOfNulls<LinearLayout>(6)
    private var updateTime = Date().time
    private var canDetonate = false // 是否可以引爆

    private var detonateHeight = 0;
    private var isInit = false
    private var isConnecting: Boolean = false

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
        throwerService.msgCallbacks += object : MsgCallback {
            override fun getId(): String {
                return "ThrowerWeightCallback"
            }

            override fun onMsg(msg: ByteArray) {
                Log.i(TAG, "msg:${msg.toHex()}")
                if (msg[0] != 0x8d.toByte()) {
                    return
                }
                if (msg[2] == THROWER_STATE.toByte()) {
//                    Log.i(TAG, "recv 0x25!")
                    // 更新状态
                    updateStatus(msg)
                }
            }

        }
    }


    fun updateStatus(msg: ByteArray) {
        Log.i(TAG, "抛投0x25msg:${msg.toHex()}")
        // 总状态
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            // 高度
            mHeight.text = msg[0 + 3].toUByte().toString() + "m"
            // 温度
//        mTemperature.text = msg[3 + 3].toUByte().toString() + "°C"

            mConnectState.setText(R.string.connection_status_connected)

            mThrowerAllowDetonationSwitch.isChecked = msg[1 + 3] !== 0x00.toByte()// 起爆状态
            mThrowerChargingSwitch.isChecked = msg[2 + 3] !== 0x00.toByte()// 充电状态
            if (msg[4 + 3] !== 0x00.toByte()) { // 可以引爆
                canDetonate = true
                mBombState1.setText(R.string.can_detonate)
                mBombState1.setTextColor(resources.getColor(R.color.green))
                mBombState2.setText(R.string.can_detonate)
                mBombState2.setTextColor(resources.getColor(R.color.green))
            } else { // 无法引爆
                canDetonate = false
                mBombState1.setTextColor(resources.getColor(R.color.red))
                mBombState2.setTextColor(resources.getColor(R.color.red))
                // 未允许引爆
                if(msg[1 + 3] == 0x00.toByte()) {
                    mBombState1.setText(R.string.cannot_detonate_notAllow)
                    mBombState2.setText(R.string.cannot_detonate_notAllow)
                }
                // 未充电
                else if(msg[2 + 3] == 0x00.toByte()) {
                    mBombState1.setText(R.string.cannot_detonate_uncharged)
                    mBombState2.setText(R.string.cannot_detonate_uncharged)
                }
                // 高度不够，飞机高度-引爆高度<=22米
                else if(msg[0 + 3].toUByte() - msg[5 + 3].toUByte() <= 22u) {
                    mBombState1.setText(R.string.cannot_detonate_tooLow)
                    mBombState2.setText(R.string.cannot_detonate_tooLow)
                }
                else {
                    mBombState1.setText(R.string.cannot_detonate)
                    mBombState2.setText(R.string.cannot_detonate)
                }
            }
        }
        // 起爆高度
        detonateHeight = msg[5 + 3].toInt()
        updateTime = Date().time
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
                        context.resources.getString(R.string.close)
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
            if(canDetonate) {
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

        val timer = Timer()
        val task = object : TimerTask() {
            override fun run() {
                mThrowerView.post {
                    mBtnArr[index]?.text =
                        "${context.resources.getString(R.string.passageway)}${index + 1}:${
                            context.resources.getString(R.string.close)
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

    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.thrower_weight, this, true)
        mThrowerView = findViewById(R.id.throwerWeight)
        mDetonationSettingsView = findViewById(R.id.detonationSettingsView)
        mPromptView = findViewById(R.id.promptView)
        mThrowerSafetySwitch = findViewById(R.id.throwerSafetySwitch)
        mDetonationSettingsBtn = findViewById(R.id.detonationSettingsBtn)
        mThrowerChargingSwitch = findViewById(R.id.throwerChargingSwitch)
        mThrowerAllowDetonationSwitch = findViewById(R.id.throwerAllowDetonationSwitch)
//        mTemperature = findViewById(R.id.temperature)
        mConnectState = findViewById(R.id.connectState)
        mHeight = findViewById(R.id.height)
        mBombState1 = findViewById(R.id.bombState_1)
        mBombState2 = findViewById(R.id.bombState_2)
        mOpenAll = findViewById(R.id.openAll)
        mDetonateHeightEditText = findViewById(R.id.detonateHeight)
        mOKBtn = findViewById(R.id.OKBtn)
        mPromptOkBtn = findViewById(R.id.promptOkBtn)
        mPromptCancelBtn = findViewById(R.id.promptCancelBtn)
        mCenterBtn = findViewById(R.id.switchCenter)
        mLeftBtn = findViewById(R.id.switchLeft)
        mRightBtn = findViewById(R.id.switchRight)
        mThrowerUpdate = findViewById(R.id.throwerUpdate)
        mUpdateView = findViewById(R.id.updateView)
        mUpdateOkBtn = findViewById(R.id.updateOkBtn)
        mUpdateCancelBtn = findViewById(R.id.updateCancelBtn)
        if (context != null) {
            // 按键
            for (btnIndex in mBtnArr.indices) {
                mBtnArr[btnIndex] = findViewById(
                    context.resources.getIdentifier(
                        "switch${btnIndex + 1}",
                        "id",
                        context.packageName
                    )
                )
                updateBtn("toOpen", btnIndex)
            }

            // 控制按键的显示与隐藏
            for (passagewayBoxIndex in mPassagewayBoxArr.indices) {
                mPassagewayBoxArr[passagewayBoxIndex] = findViewById(
                    context.resources.getIdentifier(
                        "passagewayBox${passagewayBoxIndex + 1}",
                        "id",
                        context.packageName
                    )
                )
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
        // 打开起爆设置页面
        mDetonationSettingsBtn.setOnClickListener {
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                mDetonateHeightEditText.setText(detonateHeight.toString()) // 默认显示当前的起爆高度
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
        // 确认框，取消
        mPromptCancelBtn.setOnClickListener {
            mThrowerView.visibility = VISIBLE
            mPromptView.visibility = GONE
        }
        // 打开全部通道
        mOpenAll.setOnClickListener {
            toOpenAll()
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
            val timer = Timer();
            val task = object : TimerTask() {
                override fun run() {
                    timer.cancel()
                    exitProcess(-1)
                }
            }
            // 定时器，100毫秒后开始执行，每2秒执行一次
            timer.scheduleAtFixedRate(task, 2000, 1000);
        }
        // 取消更新程序
        mUpdateCancelBtn.setOnClickListener {
            mUpdateView.visibility = GONE
            mThrowerView.visibility = VISIBLE
        }
        // 充电放电
        mThrowerChargingSwitch.setOnClickListener {
            throwerService.charging(mThrowerChargingSwitch.isChecked)
        }
        // 允许起爆
        mThrowerAllowDetonationSwitch.setOnClickListener {
            throwerService.allowDetonation(mThrowerAllowDetonationSwitch.isChecked)
        }
        setConnectState()
    }

    // 将所有按键重置
    fun resetThrowerBtn() {
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
                // 如果可以引爆就直接打开，如果不可以引爆就弹提示窗口，确认后再打开
                if(canDetonate) {
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
                    mCenterBtn.setText(R.string.opening)
                    mCenterBtn.isEnabled = false
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
                                mCenterBtn.isEnabled = true
                                mCenterBtn?.text ="${context.resources.getString(R.string.center)}:${ context.resources.getString(R.string.close)}"
                                mCenterBtn.setOnClickListener {
                                    closeTwo("center")
                                }
                                // 第3通道
                                updateBtn("toClose", 2)
                                // 第6通道
                                updateBtn("toClose", 5-2)
                            }
                            "left" -> {
                                mLeftBtn.isEnabled = true
                                mLeftBtn?.text ="${context.resources.getString(R.string.left)}:${ context.resources.getString(R.string.close)}"
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
                                mRightBtn?.text ="${context.resources.getString(R.string.right)}:${ context.resources.getString(R.string.close)}"
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
                    mCenterBtn.setText(R.string.closing)
                    mCenterBtn.isEnabled = false
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
                                mCenterBtn.isEnabled = true
                                mCenterBtn?.text ="${context.resources.getString(R.string.center)}:${ context.resources.getString(R.string.open)}"
                                mCenterBtn.setOnClickListener {
                                    toOpenTwo("center")
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
                // 如果可以引爆就直接打开，如果不可以引爆就弹提示窗口，确认后再打开
                if(canDetonate) {
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
                                    context.resources.getString(R.string.close)
                                }"
                            mBtnArr[btnIndex]?.isEnabled = true
                            mBtnArr[btnIndex]?.setOnClickListener {
                                close(btnIndex)
                            }
                        }

                        // 中间双舵机
                        mCenterBtn.text = "${context.resources.getString(R.string.center)}:${
                            context.resources.getString(R.string.close)
                        }"
                        mCenterBtn.setOnClickListener {
                            closeTwo("center")
                        }
                        mCenterBtn.isEnabled = true
                        // 左侧双舵机
                        mLeftBtn.text = "${context.resources.getString(R.string.left)}:${
                            context.resources.getString(R.string.close)
                        }"
                        mLeftBtn.setOnClickListener {
                            closeTwo("left")
                        }
                        mLeftBtn.isEnabled = true
                        // 右侧双舵机
                        mRightBtn.text = "${context.resources.getString(R.string.right)}:${
                            context.resources.getString(R.string.close)
                        }"
                        mRightBtn.setOnClickListener {
                            closeTwo("right")
                        }
                        mRightBtn.isEnabled = true
                        // 全开、全关
                        mOpenAll.isEnabled = true
                        mOpenAll.setText(R.string.close_all)
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
                        mRightBtn.setOnClickListener {
                            toOpenTwo("right")
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
        val task = object : TimerTask() {
            override fun run() {
                // 已连接
                if (throwerService.getIsConnected()) {
                    // 定时发送消息，心跳包
                    throwerService.connectionTesting()
                    // 3秒没收到信息，显示未连接
                    if (Date().time - updateTime > 3000) {
                        val handler = Handler(Looper.getMainLooper())
                        handler.post {
                            mThrowerChargingSwitch.isChecked = false
                            mThrowerAllowDetonationSwitch.isChecked = false
//                        mTemperature.text = "0°C"
                            mConnectState.setText(R.string.connection_status_notconnected)
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
                            Thread.sleep(10000)// 先等待10s，防止刚断连就重连，报错
                            while (!throwerService.connect()) {
                                Thread.sleep(1000)
                            }
                            isConnecting = false
                            updateTime = Date().time
                        }
                    }
                }
            }
        }
        // 定时器，100毫秒后开始执行，每2秒执行一次
        timer.scheduleAtFixedRate(task, 100, 2000);
    }
}