package com.example.yikupayloadexample

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import com.example.yikupayloadexample.util.PlayerCallback
import com.example.yikupayloadexample.util.RtspPlayer
import com.google.common.io.Resources
import com.yiku.yikupayloadSDK.protocol.ALLINONE_STATE
import com.yiku.yikupayloadSDK.util.MsgCallback
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

class AllInOneThrowerWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    private val TAG = "AllInOneThrowerWeight"
    private lateinit var throwerContent: LinearLayout
    private lateinit var fpvContent: LinearLayout
    private lateinit var throwerSafetySwitch: Switch
    private lateinit var connectState: TextView
    private lateinit var thrower1Btn: Button
    private lateinit var thrower2Btn: Button
    private lateinit var throwerAllBtn: Button
    private lateinit var bomb1ConnectStateText: TextView
    private lateinit var bomb2ConnectStateText: TextView
    private lateinit var heightText: TextView
    private lateinit var detonateHeightEditText: EditText
    private lateinit var allowDetonationSwitch1: Switch
    private lateinit var allowDetonationSwitch2: Switch
    private lateinit var fpvOpenBtn: Button
    private lateinit var backBtn: ImageView
    private lateinit var enlargeBtn: ImageView
    private lateinit var playerView: VLCVideoLayout
    private lateinit var playerParentView: LinearLayout
    private lateinit var thrower1FpvBtn: Button
    private lateinit var thrower2FpvBtn: Button
    private lateinit var throwerAllFpvBtn: Button
    private var streamUrl = "rtsp://192.168.144.188:554/ch01_sub"
//    private var streamUrl = "rtsp://192.168.144.108:554/stream=1"
    private var rtspPlayer: RtspPlayer = RtspPlayer(context)

    private var isOpenSafetySwitch: Boolean = false
    private var isSettingDetonateHeight: Boolean = false
    private var isOpenThrower1: Boolean = false
    private var isOpenThrower2: Boolean = false
    private var isOpenThrowerAll: Boolean = false

    // 当窗口被加载时，加载视频
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (streamUrl != "" && fpvContent.isVisible) {
            Log.d(TAG, "onAttachedToWindow，播放视频")
            rtspPlayer.registPlayerCallback(object : PlayerCallback {
                override fun onPlaying(index: Int, mediaPlayer: MediaPlayer) {
                    Log.i(TAG, "视频播放成功")
                }

                override fun onError(index: Int) {
                    Log.e(TAG, "视频播放失败")
                    Toast.makeText(context, "视频播放失败", Toast.LENGTH_SHORT).show()
                }
            })
            rtspPlayer.createPlayer(3, playerView, streamUrl)
        }
    }
    // 当窗口被移除时，释放视频资源
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 释放视频资源
        rtspPlayer.release()
    }

    init {
        initView(context)
        fpvOpenBtn.setOnClickListener {
            throwerContent.visibility = GONE
            fpvContent.visibility = VISIBLE
            if(playerView.parent == null) {
                playerParentView.addView(playerView)
            }
            if (streamUrl != "") {
                Log.d(TAG, "按键播放视频")
                rtspPlayer.registPlayerCallback(object : PlayerCallback {
                    override fun onPlaying(index: Int, mediaPlayer: MediaPlayer) {
                        Log.i(TAG, "视频播放成功")
                    }

                    override fun onError(index: Int) {
                        Log.e(TAG, "视频播放失败")
                        Toast.makeText(context, "视频播放失败", Toast.LENGTH_SHORT).show()
                    }
                })
                rtspPlayer.createPlayer(3, playerView, streamUrl)
            }
        }
        backBtn.setOnClickListener {
            throwerContent.visibility = VISIBLE
            fpvContent.visibility = GONE
            rtspPlayer.release() // 释放视频资源
            playerParentView.removeView(playerView)
        }

        // 消息订阅
        allInOneService.registMsgCallback(object : MsgCallback {
            override fun getId(): String {
                return "AllInOneThrowerWeightCallback"
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

        // 打开、关闭安全开关
        throwerSafetySwitch.setOnClickListener {
            throwerSafetySwitch.isEnabled = false
            allInOneService.safetySwitch(throwerSafetySwitch.isChecked)
            thread {
                Thread.sleep(1500)
                throwerSafetySwitch.isEnabled = true
            }
        }
        // 1号开关
        thrower1Btn.setOnClickListener {
            thrower1Btn.isEnabled = false
            if(isOpenThrower1) {
                thrower1Btn.setText(R.string.closing)
            }
            else {
                thrower1Btn.setText(R.string.opening)
            }
            allInOneService.throwerSwitch(1, !isOpenThrower1)
            thread {
                Thread.sleep(2000)
                if(isOpenThrower1) {
                    thrower1Btn.text = "${resources.getString(R.string.number_1)}${resources.getString(R.string.open)}"
                }
                else {
                    thrower1Btn.text = "${resources.getString(R.string.number_1)}${resources.getString(R.string.close)}"
                }
                thrower1Btn.isEnabled = true
            }
        }
        // fpv页面，1号开关
        thrower1FpvBtn.setOnClickListener {
            thrower1FpvBtn.isEnabled = false
            if(isOpenThrower1) {
                thrower1FpvBtn.setText(R.string.closing)
            }
            else {
                thrower1FpvBtn.setText(R.string.opening)
            }
            allInOneService.throwerSwitch(1, !isOpenThrower1)
            thread {
                Thread.sleep(2000)
                thrower1FpvBtn.isEnabled = true
                if(isOpenThrower1) {
                    thrower1FpvBtn.text = "${resources.getString(R.string.number_1)}${resources.getString(R.string.open)}"
                }
                else {
                    thrower1FpvBtn.text = "${resources.getString(R.string.number_1)}${resources.getString(R.string.close)}"
                }
            }
        }
        // 2号开关
        thrower2Btn.setOnClickListener {
            thrower2Btn.isEnabled = false
            if(isOpenThrower2) {
                thrower2Btn.setText(R.string.closing)
            }
            else {
                thrower2Btn.setText(R.string.opening)
            }
            allInOneService.throwerSwitch(2, !isOpenThrower2)
            thread {
                Thread.sleep(2000)
                if(isOpenThrower2) {
                    thrower2Btn.text = "${resources.getString(R.string.number_2)}${resources.getString(R.string.open)}"
                }
                else {
                    thrower2Btn.text = "${resources.getString(R.string.number_2)}${resources.getString(R.string.close)}"
                }
                thrower2Btn.isEnabled = true
            }
        }
        // fpv页面，2号开关
        thrower2FpvBtn.setOnClickListener {
            thrower2FpvBtn.isEnabled = false
            if(isOpenThrower2) {
                thrower2FpvBtn.setText(R.string.closing)
            }
            else {
                thrower2FpvBtn.setText(R.string.opening)
            }
            allInOneService.throwerSwitch(2, !isOpenThrower2)
            thread {
                Thread.sleep(2000)
                thrower2FpvBtn.isEnabled = true
                if(isOpenThrower2) {
                    thrower2FpvBtn.text = "${resources.getString(R.string.number_2)}${resources.getString(R.string.open)}"
                }
                else {
                    thrower2FpvBtn.text = "${resources.getString(R.string.number_2)}${resources.getString(R.string.close)}"
                }
            }
        }
        // 全开全关
        throwerAllBtn.setOnClickListener {
            throwerAllBtn.isEnabled = false
            if(isOpenThrowerAll) {
                throwerAllBtn.setText(R.string.closing)
            }
            else {
                throwerAllBtn.setText(R.string.opening)
            }
            allInOneService.throwerSwitch(0, !isOpenThrowerAll)
            thread {
                Thread.sleep(2000)
                if(isOpenThrowerAll) {
                    throwerAllBtn.setText(R.string.close_all)
                }
                else {
                    throwerAllBtn.setText(R.string.open_all)
                }
                throwerAllBtn.isEnabled = true
            }
        }
        // fpv页面，全开全关
        thrower2FpvBtn.setOnClickListener {
            thrower2FpvBtn.isEnabled = false
            if(isOpenThrower2) {
                thrower2FpvBtn.setText(R.string.closing)
            }
            else {
                thrower2FpvBtn.setText(R.string.opening)
            }
            allInOneService.throwerSwitch(1, isOpenThrower2)
            thread {
                Thread.sleep(2000)
                thrower2FpvBtn.isEnabled = true
                if(isOpenThrower2) {
                    thrower2FpvBtn.text = "${resources.getString(R.string.number_2)}${resources.getString(R.string.open)}"
                }
                else {
                    thrower2FpvBtn.text = "${resources.getString(R.string.number_2)}${resources.getString(R.string.close)}"
                }
            }
        }

        // 1号充电放电
        allowDetonationSwitch1.setOnClickListener {
            allowDetonationSwitch1.isEnabled = false
            allInOneService.allowDetonate(1, allowDetonationSwitch1.isChecked)
            thread {
                Thread.sleep(1500)
                allowDetonationSwitch1.isEnabled = true
            }
        }

        // 2号充电放电
        allowDetonationSwitch2.setOnClickListener {
            allowDetonationSwitch2.isEnabled = false
            allInOneService.allowDetonate(2, allowDetonationSwitch2.isChecked)
            thread {
                Thread.sleep(1500)
                allowDetonationSwitch2.isEnabled = true
            }
        }

        detonateHeightEditText.setOnClickListener {
            if(it.hasFocus()) {
                isSettingDetonateHeight = true
            }
        }
        detonateHeightEditText.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE,
                EditorInfo.IME_ACTION_SEARCH,
                EditorInfo.IME_ACTION_GO -> {
                    var detonateHeight = detonateHeightEditText.text.toString().toInt()
                    if(detonateHeight > 20) {
                        detonateHeight = 20
                        detonateHeightEditText.setText("$detonateHeight")
                    }
                    allInOneService.setDetonateHeight(detonateHeight)
                    thread {
                        Thread.sleep(1500)
                        isSettingDetonateHeight = false
                    }
                    true // 消费事件
                }
                else -> false
            }
        }
        setConnectState()
    }
    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.all_in_one_thrower_weight, this, true)
        throwerContent = findViewById(R.id.thrower_content)
        fpvContent = findViewById(R.id.fpv_content)
        throwerSafetySwitch = findViewById(R.id.throwerSafetySwitch)
        connectState = findViewById(R.id.connectState)
        thrower1Btn = findViewById(R.id.thrower1_btn)
        thrower2Btn = findViewById(R.id.thrower2_btn)
        throwerAllBtn = findViewById(R.id.throwerAll_btn)
        bomb1ConnectStateText = findViewById(R.id.bomb1ConnectStateText)
        bomb2ConnectStateText = findViewById(R.id.bomb2ConnectStateText)
        heightText = findViewById(R.id.heightText)
        detonateHeightEditText = findViewById(R.id.detonateHeightEditText)
        allowDetonationSwitch1 = findViewById(R.id.allow_detonation_1)
        allowDetonationSwitch2 = findViewById(R.id.allow_detonation_2)
        fpvOpenBtn = findViewById(R.id.fpv_open_btn)
        backBtn = findViewById(R.id.back_btn)
        enlargeBtn = findViewById(R.id.enlarge_btn)
        playerView = findViewById(R.id.playerView)
        playerParentView = findViewById(R.id.playerParentView)
        thrower1FpvBtn = findViewById(R.id.thrower1_fpv_btn)
        thrower2FpvBtn = findViewById(R.id.thrower2_fpv_btn)
        throwerAllFpvBtn = findViewById(R.id.throwerAll_fpv_btn)
    }

    // 更新状态
    private fun updateState(msg: ByteArray) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            isOpenSafetySwitch = (msg[10].toInt() == 1)
            if(throwerSafetySwitch.isEnabled) {
                throwerSafetySwitch.isChecked = isOpenSafetySwitch
            }
            heightText.text = "${msg[3].toInt()}m"
            if(!isSettingDetonateHeight) {
                detonateHeightEditText.setText("${msg[4].toInt()}")
            }
        }
        isOpenThrower1 = getBitState(msg[5].toInt(), 0)
        isOpenThrower2 = getBitState(msg[5].toInt(), 1)
        isOpenThrowerAll = isOpenThrower1 && isOpenThrower2
        handler.post {
            if (thrower1Btn.isEnabled) {
                if (isOpenThrower1) {
                    thrower1Btn.text = "${resources.getString(R.string.number_1)}${resources.getString(R.string.open)}"
                }
                else {
                    thrower1Btn.text = "${resources.getString(R.string.number_1)}${resources.getString(R.string.close)}"
                }
            }
            if (thrower1FpvBtn.isEnabled) {
                if (isOpenThrower1) {
                    thrower1FpvBtn.text = "${resources.getString(R.string.number_1)}${resources.getString(R.string.open)}"
                }
                else {
                    thrower1FpvBtn.text = "${resources.getString(R.string.number_1)}${resources.getString(R.string.close)}"
                }
            }
            if (thrower2Btn.isEnabled) {
                if (isOpenThrower2) {
                    thrower2Btn.text = "${resources.getString(R.string.number_1)}${resources.getString(R.string.open)}"
                }
                else {
                    thrower2Btn.text = "${resources.getString(R.string.number_1)}${resources.getString(R.string.close)}"
                }
            }
            if (thrower2FpvBtn.isEnabled) {
                if (isOpenThrower2) {
                    thrower2FpvBtn.text = "${resources.getString(R.string.number_1)}${resources.getString(R.string.open)}"
                }
                else {
                    thrower2FpvBtn.text = "${resources.getString(R.string.number_1)}${resources.getString(R.string.close)}"
                }
            }
            if(throwerAllBtn.isEnabled) {
                if(isOpenThrowerAll) {
                    throwerAllBtn.setText(R.string.close_all)
                }
                else {
                    throwerAllBtn.setText(R.string.open_all)
                }
            }
            if(throwerAllFpvBtn.isEnabled) {
                if(isOpenThrowerAll) {
                    throwerAllFpvBtn.setText(R.string.close_all)
                }
                else {
                    throwerAllFpvBtn.setText(R.string.open_all)
                }
            }
        }
        updateThrowerState(1, msg.slice(11 until 19).toByteArray())
        updateThrowerState(2, msg.slice(19 until 27).toByteArray())
    }
    // 更新抛投和灭火弹状态
    private fun updateThrowerState(index: Int, stateMsg: ByteArray) {
        var bombStateText: TextView
        var allowSwitch: Switch
        val stateText: String
        val stateTextColor: Int
        when(index) {
            1 -> {
                bombStateText = bomb1ConnectStateText
                allowSwitch = allowDetonationSwitch1
            }
            2 -> {
                bombStateText = bomb2ConnectStateText
                allowSwitch = allowDetonationSwitch2
            }
            else -> {
                bombStateText = bomb1ConnectStateText
                allowSwitch = allowDetonationSwitch1
            }
        }
        if(stateMsg[0].toInt() == 0 || stateMsg[0].toInt() == 127) {
            if(allowSwitch.isEnabled) {
                allowSwitch.isChecked = false
            }
        }
        else {
            if(allowSwitch.isEnabled) {
                allowSwitch.isChecked = true
            }
        }
        // 无法起爆
        if(stateMsg[4].toInt() == 0) {
            stateTextColor = resources.getColor(R.color.red)
            // 未允许引爆
            if(stateMsg[0] == 0x00.toByte()) {
                stateText = resources.getString(R.string.cannot_detonate_notAllow)
            }
            // 正在充电
            else if(stateMsg[0] == 0x02.toByte()) {
                stateText = resources.getString(R.string.charging)
            }
            // 未连接
            else if(stateMsg[0] == 127.toByte()) {
                stateText = resources.getString(R.string.not_connected)
            }
            // 高度不够，飞机高度-引爆高度<=22米
            else if(stateMsg[1] == 0x00.toByte()) {
                stateText = resources.getString(R.string.cannot_detonate_tooLow)
            }
            else {
                stateText = resources.getString(R.string.cannot_detonate)
            }
        }
        else {
            stateText = resources.getString(R.string.can_detonate)
            stateTextColor = resources.getColor(R.color.green)
        }
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            bombStateText.text = stateText
            bombStateText.setTextColor(stateTextColor)
        }
    }

    /**
     * 检查特定位的状态
     * @param byteValue 无符号字节值
     * @param bitPosition 位位置（0-7，0是最低位）
     */
    private fun getBitState(byteValue: Int, bitPosition: Int): Boolean {
        require(bitPosition in 0..7) { "位位置必须在0-7范围内" }
        return (byteValue and (1 shl bitPosition)) != 0
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