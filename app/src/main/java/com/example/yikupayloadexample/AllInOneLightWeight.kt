package com.example.yikupayloadexample

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.example.yikupayloadexample.component.ModeItem
import com.example.yikupayloadexample.component.ModeSelectionDialog
import com.yiku.yikupayloadSDK.protocol.ALLINONE_PITCH_STATE
import com.yiku.yikupayloadSDK.protocol.ALLINONE_STATE
import com.yiku.yikupayloadSDK.util.MsgCallback
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

// 灯光状态数据类
data class LightStatus(
    val brightness: Int,    // 亮度值 (0-63)
    val strobeEnabled: Boolean, // 爆闪开关
    val lightEnabled: Boolean   // 灯开关
)

class AllInOneLightWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    private val TAG = "AllInOneLightWeight"
    private lateinit var connectState: TextView
    private lateinit var lightOpenBtn: Button
    private lateinit var lightCloseBtn: Button
    private lateinit var flashOpenBtn: Button
    private lateinit var flashCloseBtn: Button
    private lateinit var luminanceSeekBar: SeekBar
    private lateinit var luminanceText: TextView
    private lateinit var redAndBlueOpenBtn: Button
    private lateinit var redAndBlueCloseBtn: Button
    private lateinit var modeSelectorLayout: LinearLayout
    private lateinit var currentModeText: TextView
    private lateinit var pitchSeekBar: SeekBar
    private lateinit var pitchText: TextView
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
    private var isSettingLuminance: Boolean = false
    private var isOpenLight: Boolean = false
    private var isFlashing: Boolean = false
    private var isSettingPitch: Boolean = false
    private var redAndBlueMode: Int = 1
    private var isOpenRedAndBlue: Boolean = false
    private var isSettingRedAndBlueMode: Boolean = false

    init {
        initView(context)
        // 消息订阅
        allInOneService.registMsgCallback(object : MsgCallback {
            override fun getId(): String {
                return "AllInOneLightWeightCallback"
            }
            override fun onMsg(msg: ByteArray) {
                if (msg[0] != 0x8d.toByte()) {
                    return
                }
                if (msg[2] == ALLINONE_STATE.toByte()) {
                    // 更新状态
                    updateState(msg)
                }
            }
        })

        // 云台消息订阅
        allInOneService.registPtzMsgCallback(object : MsgCallback {
            override fun getId(): String {
                return "AllInOneLightWeightPTZCallback"
            }
            override fun onMsg(msg: ByteArray) {
                if (msg[0] != 0x8d.toByte()) {
                    return
                }
                if (msg[2] == ALLINONE_PITCH_STATE.toByte()) {
                    if(isSettingPitch) {
                        return
                    }
                    // 俯仰值，0-900
                    val pitchValue = ((msg[3].toInt()  and 0xFF) shl 8) or (msg[4].toInt()  and 0xFF)
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        pitchSeekBar.progress = pitchValue
                        pitchText.text = "${pitchValue/10}°"
                    }
                }
            }
        })
        currentModeText.text = currentMode.name
        modeSelectorLayout.setOnClickListener {
            isSettingRedAndBlueMode = true
            showModeSelectionDialog()
        }

        // 开灯
        lightOpenBtn.setOnClickListener {
            allInOneService.openLight(true)
        }
        // 关灯
        lightCloseBtn.setOnClickListener {
            allInOneService.openLight(false)
        }
        // 开爆闪
        flashOpenBtn.setOnClickListener {
            allInOneService.flashSwitch(true)
        }
        // 关爆闪
        flashCloseBtn.setOnClickListener {
            allInOneService.flashSwitch(false)
        }

        // 亮度控制
        luminanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                luminanceText.text = "${seekBar.progress}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isSettingLuminance = true
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                allInOneService.luminanceChange(seekBar.progress)
                // 延迟一下，避免设置还未生效，导致滑条往回跳
                thread {
                    Thread.sleep(1000)
                    isSettingLuminance = false
                }
            }
        })
        // 开红蓝
        redAndBlueOpenBtn.setOnClickListener {
            allInOneService.redBlueLedControl(redAndBlueMode.toByte())
            isOpenRedAndBlue = true
            isSettingRedAndBlueMode = false
        }
        // 关红蓝
        redAndBlueCloseBtn.setOnClickListener {
            allInOneService.redBlueLedControl(0)
            isOpenRedAndBlue = false
            isSettingRedAndBlueMode = false
        }
        // 俯仰控制
        pitchSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                pitchText.text = "${seekBar.progress/10}°"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isSettingPitch = true
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                allInOneService.pitchControl(seekBar.progress)
                Log.i(TAG, "音量设置，当前音量：${seekBar.progress}")
                // 延迟一下，避免设置还未生效，导致滑条往回跳
                thread {
                    Thread.sleep(1000)
                    isSettingPitch = false
                }
            }
        })
        setConnectState()
    }
    private fun updateState(msg: ByteArray) {
        // 灯状态
        val lightStatus = parseLightStatus(msg[6])
        isOpenLight = lightStatus.lightEnabled
        isFlashing = lightStatus.strobeEnabled
        val handler = Handler(Looper.getMainLooper())
        Log.d(TAG, "模式：${msg[7].toInt()}, isSettingRedAndBlueMode:$isSettingRedAndBlueMode")
        // 红蓝模式
        if(msg[7].toInt() != 0 && !isSettingRedAndBlueMode) {
            Log.d(TAG, "自动更新红蓝模式")
            redAndBlueMode = msg[7].toInt()
            currentMode = modeItems[redAndBlueMode - 1]
            handler.post {
                currentModeText.text = currentMode.name
            }
        }
        handler.post {
            if(!isSettingLuminance) {
                luminanceSeekBar.progress = lightStatus.brightness
                luminanceText.text = "${lightStatus.brightness}"
            }
        }
    }

    /**
     * 解析字节的灯光控制状态
     * @param byte 要解析的字节
     * @return LightStatus对象包含解析出的亮度、爆闪开关和灯开关状态
     */
    fun parseLightStatus(byte: Byte): LightStatus {
        // 将字节转换为无符号整数处理[6](@ref)
        val unsignedByte = byte.toInt() and 0xFF
        // 解析亮度值 (0-5位，低6位)
        val brightness = unsignedByte and 0x3F  // 掩码 00111111
        // 解析爆闪开关 (第6位)
        val strobeEnabled = (unsignedByte and 0x40) != 0 // 掩码 01000000
        // 解析灯开关 (第7位)
        val lightEnabled = (unsignedByte and 0x80) != 0 // 掩码 10000000
        return LightStatus(brightness, strobeEnabled, lightEnabled)
    }

    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.all_in_one_light_weight, this, true)
        connectState = findViewById(R.id.connectState)
        lightOpenBtn = findViewById(R.id.light_open_btn)
        lightCloseBtn = findViewById(R.id.light_close_btn)
        flashOpenBtn = findViewById(R.id.flash_open_btn)
        flashCloseBtn = findViewById(R.id.flash_close_btn)
        luminanceSeekBar = findViewById(R.id.luminance_seek_bar)
        luminanceText = findViewById(R.id.luminance_text)
        redAndBlueOpenBtn = findViewById(R.id.redAndBlue_open_btn)
        redAndBlueCloseBtn = findViewById(R.id.redAndBlue_close_btn)
        modeSelectorLayout = findViewById(R.id.mode_selector_layout)
        currentModeText = findViewById(R.id.current_mode_text)
        pitchSeekBar = findViewById(R.id.pitch_seek_bar)
        pitchText = findViewById(R.id.pitch_text)
    }
    private fun showModeSelectionDialog() {
        val dialog = ModeSelectionDialog(
            context = context,
            modes = modeItems,
            currentMode = currentMode
        ) {
            selectedMode ->
                currentMode = selectedMode
                currentModeText.text = currentMode.name
                redAndBlueMode = currentMode.id.toInt()
                if(isOpenRedAndBlue) {
                    allInOneService.redBlueLedControl(redAndBlueMode.toByte())
                    thread {
                        Thread.sleep(1000)
                        isSettingRedAndBlueMode = false
                    }
                }
                else {
                    isSettingRedAndBlueMode = false
                }
        }
        dialog.show()
    }

    // 定时器，判断连接状态
    private fun setConnectState() {
        val timer = Timer();
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask() {
            override fun run() {
                // 已连接
                if (allInOneService.getIsConnected()) {
                    handler.post {
                        connectState.setText(R.string.connection_status_connected)
                    }
                }
                else{// 未连接
                    handler.post {
                        connectState.setText(R.string.connection_status_notconnected)
                    }
                }
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.scheduleAtFixedRate(task, 100, 2000);
    }
}