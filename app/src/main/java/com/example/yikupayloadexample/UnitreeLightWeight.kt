package com.example.yikupayloadexample;

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.yiku.yikupayloadSDK.util.MsgCallback
import org.json.JSONException
import org.json.JSONObject

import java.util.Timer
import java.util.TimerTask
import androidx.core.view.isVisible
import com.example.yikupayloadexample.component.ModeItem
import com.example.yikupayloadexample.component.ModeSelectionDialog

class UnitreeLightWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "UnitreeLightWeight"
    private lateinit var mLightView: View
    private var openLightStatus = 0
    private var sharpFlashStatus = 0
    private val timer: Timer = Timer()
    private lateinit var timerTask: TimerTask;
    private lateinit var mLuminanceText: TextView
    private lateinit var mLuminanceChangePitch: SeekBar // 舵机控制
    private lateinit var mOnOrOffRbBtn: Button
    private lateinit var mModeSelectorLayout: LinearLayout
    private lateinit var mCurrentModeText: TextView
    private val modeItems = listOf(
        ModeItem("1", "${context.resources.getString(R.string.mode)}1"),
        ModeItem("2", "${context.resources.getString(R.string.mode)}2"),
        ModeItem("3", "${context.resources.getString(R.string.mode)}3"),
        ModeItem("4", "${context.resources.getString(R.string.mode)}4"),
        ModeItem("5", "${context.resources.getString(R.string.mode)}5"),
        ModeItem("6", "${context.resources.getString(R.string.mode)}6"),
        ModeItem("7", "${context.resources.getString(R.string.mode)}7"),
        ModeItem("8", "${context.resources.getString(R.string.mode)}8"),
        ModeItem("9", "${context.resources.getString(R.string.mode)}9"),
        ModeItem("10", "${context.resources.getString(R.string.mode)}10"),
        ModeItem("11", "${context.resources.getString(R.string.mode)}11"),
        ModeItem("12", "${context.resources.getString(R.string.mode)}12"),
        ModeItem("13", "${context.resources.getString(R.string.mode)}13"),
        ModeItem("14", "${context.resources.getString(R.string.mode)}14"),
        ModeItem("15", "${context.resources.getString(R.string.mode)}15"),
        ModeItem("16", "${context.resources.getString(R.string.mode)}16")
    )
    private var currentMode = modeItems[0]
    private var isOpenRedAndBlue: Boolean = false
    private var redAndBlueMode: Int = 1
    private var isSettingRedAndBlueMode: Boolean = false
    private var isIntCallBack: Boolean = false

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        initView(context)
    }

    fun changeLuminance(driverLuminance: Int, ledLuminance: Int) {
        mLuminanceText.text = "驱动温度: ${driverLuminance}℃ \n" + "灯头温度: ${ledLuminance}℃"
    }

    inner

    class LightMsgCallback : MsgCallback {
        private val TAG = "LightMsgCallback"
        override fun getId(): String {
            return "LightMsgCallback"
        }

        override fun onMsg(msg: ByteArray) {
            if(msg[0] == 0x8d.toByte()) {
                if (msg[2] == 0x04.toByte()) {
//                Log.i(TAG, "查询温度: ${msg.asList()} ")
//                val ledLuminance = msg[3].toInt() - 50
//                val driverLuminance = msg[4].toInt() - 50
//                    mLightView.post(Runnable {
//                        changeLuminance(msg[4].toInt() - 50, msg[3].toInt() - 50)
//                    })
                }
            }
            else if (msg.size > 6 && String(msg.slice(0..3).toByteArray()) == "[99]") {
                // 假设 msg 是一个 ByteArray
                val dataLength = msg.size - 2 - 4
                // 使用 Kotlin 的 sliceArray 方法提取子数组，更简洁
                val valueBytes = msg.sliceArray(5 until 5 + dataLength)
                // 将字节数组（ASCII字符）转换为字符串
                val jsonString = valueBytes.toString(Charsets.US_ASCII)
                try {
                    // 使用 Kotlin 标准库的 JSONObject 进行解析
                    val jsonObject = JSONObject(jsonString)
                    // 从JSON对象中提取数据
                    val mode = jsonObject.getInt("rbMode")
                    mLightView.post {
                        if (!isSettingRedAndBlueMode) {
                            if (mode == 0) {
                                // 设备关闭了红蓝
                                isOpenRedAndBlue = false
                                mOnOrOffRbBtn.setText(R.string.open)
                            } else {
                                // 设备切换了模式
                                isOpenRedAndBlue = true
                                redAndBlueMode = mode
                                currentMode = modeItems[mode - 1]
                                mCurrentModeText.text = currentMode.name
                                mOnOrOffRbBtn.setText(R.string.close)
                            }
                        }
                    }
                } catch (e: JSONException) {
                    // 处理JSON解析错误（如键不存在、类型不匹配、格式错误等）
                    Log.e(TAG, "JSON解析失败: ${e.message}")
                } catch (e: Exception) {
                    // 处理其他潜在异常
                    Log.e(TAG, "处理消息时发生未知错误: ${e.message}")
                }
            }
        }

    }

    private fun initView(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.light_weight_ya3, this, true)
        mLightView = findViewById(R.id.ya3_light_view)
        val mOpenLightBtn = findViewById<Button>(R.id.open_light_btn)
        val mSharpFlashBtn = findViewById<Button>(R.id.sharp_flash_btn)
        val mLuminanceChangeSeekbar = findViewById<SeekBar>(R.id.luminance_change_seekbar)
        mOnOrOffRbBtn = findViewById(R.id.on_off_rb_btn)
        mModeSelectorLayout = findViewById(R.id.mode_selector_layout)
        mCurrentModeText = findViewById(R.id.current_mode_text)
        mCurrentModeText.text = currentMode.name
        mLuminanceChangePitch = findViewById(R.id.luminance_change_pitch)

        // 模式选择点击 -> 弹出 ModeSelectionDialog
        mModeSelectorLayout.setOnClickListener {
            isSettingRedAndBlueMode = true
            showModeSelectionDialog()
        }
        // 打开/关闭红蓝按钮
        mOnOrOffRbBtn.setOnClickListener {
            isSettingRedAndBlueMode = true
            if (isOpenRedAndBlue) {
                // 已打开 -> 关闭
                megaphoneService?.redBlueLedControl(0x00)
                mOnOrOffRbBtn.setText(R.string.open)
            } else {
                // 未打开 -> 打开到当前模式
                megaphoneService?.redBlueLedControl(redAndBlueMode.toByte())
                mOnOrOffRbBtn.setText(R.string.close)
            }
            isOpenRedAndBlue = !isOpenRedAndBlue
            // 延迟一下避免状态冲突
            Thread {
                Thread.sleep(2000)
                isSettingRedAndBlueMode = false
            }.start()
        }

        mLuminanceChangePitch.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (seekBar != null) {
                    megaphoneService?.controlServo(seekBar.progress + 100)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
//                if (seekBar != null) {
//                    megaphoneService.setVolume(seekBar.progress)
//                }
            }

        })


        // 查询初始化状态
        megaphoneService?.openLight(0, true) // 开灯状态
        megaphoneService?.sharpFlash(0, true) // 爆闪状态
        megaphoneService?.fetchTemperature() // 温度
        setConnectState()
        fun close_light() {
            megaphoneService?.openLight(0, false)
            megaphoneService?.sharpFlash(0, false)
            openLightStatus = 0
            mOpenLightBtn.setText(R.string.turn_on_the_light)
            sharpFlashStatus = 0
            mSharpFlashBtn.setText(R.string.explosive_flashing)
        }
        mOpenLightBtn.setOnClickListener {
//            close_light()
            megaphoneService?.openLight(if (openLightStatus == 0) 1 else 0, false)
            if (openLightStatus == 0) {
                openLightStatus = 1
                mOpenLightBtn.setText(R.string.turn_off_the_light)
            } else {
                openLightStatus = 0
                mOpenLightBtn.setText(R.string.turn_on_the_light)
                // 关灯时自动关爆闪
                megaphoneService?.sharpFlash(0, false)
                sharpFlashStatus = 0
                mSharpFlashBtn.setText(R.string.explosive_flashing)
            }
        }
        mSharpFlashBtn.setOnClickListener {
            megaphoneService?.sharpFlash(if (sharpFlashStatus == 0) 1 else 0, false)
            // 如果是打开爆闪
            if (sharpFlashStatus == 0) {
                sharpFlashStatus = 1
                mSharpFlashBtn.setText(R.string.turn_off_explosion_flash)
            } else {// 如果是关闭爆闪
                sharpFlashStatus = 0
                mSharpFlashBtn.setText(R.string.explosive_flashing)
                // 关爆闪时，自动关灯
                megaphoneService?.openLight(0, false)
                openLightStatus = 0
                mOpenLightBtn.setText(R.string.turn_on_the_light)
            }
        }
        mLuminanceChangeSeekbar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?, progress: Int, fromUser: Boolean
            ) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    megaphoneService?.luminanceChange(seekBar.progress, false)

                }
            }

        })

        // 定时获取温度
        timerTask = object : TimerTask() {
            override fun run() {
                if (megaphoneService != null && megaphoneService?.getIsConnectedYA3() == true) {
                    megaphoneService?.fetchTemperature()
                }
            }
        }
        timer.schedule(timerTask, 0, 2000)
        isFocusable = true
    }

    private fun showModeSelectionDialog() {
        val dialog = ModeSelectionDialog(
            context = context,
            modes = modeItems,
            currentMode = currentMode
        ) { selectedMode ->
            currentMode = selectedMode
            mCurrentModeText.text = currentMode.name
            redAndBlueMode = currentMode.id.toInt()
            if (isOpenRedAndBlue) {
                megaphoneService?.redBlueLedControl(redAndBlueMode.toByte())
                // 延迟一下避免状态冲突
                Thread {
                    Thread.sleep(1000)
                    isSettingRedAndBlueMode = false
                }.start()
            } else {
                isSettingRedAndBlueMode = false
            }
        }
        dialog.show()
    }

    fun ByteArray.toHex(): String =
        joinToString(separator = "") { eachByte -> "%02x ".format(eachByte) }


    private fun showToast(msg: String) {
        (context as Activity).runOnUiThread {
            Toast.makeText(
                context, msg, Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 定时器，判断连接状态
    private fun setConnectState(){
        val timer = Timer();
        val statusDot = findViewById<View>(R.id.statusDot)
        val background = statusDot.background as GradientDrawable
        val connectText = findViewById<TextView>(R.id.lightConnectYA3)
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask(){
            override fun run() {
                if(megaphoneService?.getIsConnectedYA3() == true){
                    handler.post {
                        connectText.setText(R.string.connection_status_connected)
                        background.setColor(ContextCompat.getColor(context, R.color.green))
                    }

                    if(!isIntCallBack) {
                        // 初始化通讯回调函数
                        megaphoneService?.registMsgCallback(LightMsgCallback())
                        isIntCallBack = true
                    }
                }
                else{
                    handler.post {
                        connectText.setText(R.string.connection_status_notconnected)
                        background.setColor(ContextCompat.getColor(context, R.color.red))
                    }
                    // 此处不重连，因为是四合一，重连部分在RealTimeShoutWeight
                }
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.schedule(task, 100, 1000);
    }
}