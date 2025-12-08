package com.example.yikupayloadexample

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import com.example.yikupayloadexample.component.FullScreenVideoActivity
import com.example.yikupayloadexample.util.RtspPlayer
import com.yiku.yikupayloadSDK.protocol.ALLINONE_STATE
import com.yiku.yikupayloadSDK.util.MsgCallback
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
    private lateinit var playerView: SurfaceView
    private lateinit var playerParentView: LinearLayout
    private lateinit var thrower1FpvBtn: Button
    private lateinit var thrower2FpvBtn: Button
    private lateinit var throwerAllFpvBtn: Button
//    private var streamUrl = "rtsp://192.168.144.188:554/ch01_sub"
    private var streamUrl = "rtsp://192.168.144.108:554/stream=1"
    private lateinit var rtspPlayer: RtspPlayer

    private var isOpenSafetySwitch: Boolean = false
    private var isSettingDetonateHeight: Boolean = false
    private var isOpenThrower1: Boolean = false
    private var isOpenThrower2: Boolean = false
    private var isOpenThrowerAll: Boolean = false
    private var thisPayloadWeight: PayloadWeight? = null
    private var isInitPlayer: Boolean = false

    // 当窗口被加载时，加载视频
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (streamUrl != "" && fpvContent.isVisible) {
            if(isInitPlayer) {
                rtspPlayer.startPlayback(streamUrl)
            }
            else {
                initPlayer()
                isInitPlayer = true
            }
        }
    }
    // 当窗口被移除时，释放视频资源
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 释放视频资源
        if(isInitPlayer) {
            rtspPlayer.stopPlayback()
        }
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
                initPlayer()
            }
        }
        backBtn.setOnClickListener {
            throwerContent.visibility = VISIBLE
            fpvContent.visibility = GONE
            rtspPlayer.release() // 释放视频资源
            playerParentView.removeView(playerView)
        }

        // 消息订阅
        allInOneService.registMainMsgCallback(object : MsgCallback {
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
                Thread.sleep(2000)
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    throwerSafetySwitch.isEnabled = true
                }
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
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    if (isOpenThrower1) {
                        thrower1Btn.setText(R.string.close_1)
                    } else {
                        thrower1Btn.setText(R.string.open_1)
                    }
                    thrower1Btn.isEnabled = true
                }
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
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    thrower1FpvBtn.isEnabled = true
                    if (isOpenThrower1) {
                        thrower1FpvBtn.setText(R.string.close_1)
                    } else {
                        thrower1FpvBtn.setText(R.string.open_1)
                    }
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
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    if (isOpenThrower2) {
                        thrower2Btn.setText(R.string.close_2)
                    } else {
                        thrower2Btn.setText(R.string.open_2)
                    }
                    thrower2Btn.isEnabled = true
                }
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
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    thrower2FpvBtn.isEnabled = true
                    if (isOpenThrower2) {
                        thrower2FpvBtn.setText(R.string.close_2)
                    } else {
                        thrower2FpvBtn.setText(R.string.open_2)
                    }
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
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    if (isOpenThrowerAll) {
                        throwerAllBtn.setText(R.string.close_all)
                    } else {
                        throwerAllBtn.setText(R.string.openAll)
                    }
                    throwerAllBtn.isEnabled = true
                }
            }
        }
        // fpv页面，全开全关
        throwerAllFpvBtn.setOnClickListener {
            throwerAllFpvBtn.isEnabled = false
            if(isOpenThrowerAll) {
                throwerAllFpvBtn.setText(R.string.closing)
            }
            else {
                throwerAllFpvBtn.setText(R.string.opening)
            }
            allInOneService.throwerSwitch(0, isOpenThrowerAll)
            thread {
                Thread.sleep(2000)
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    throwerAllFpvBtn.isEnabled = true
                    if (isOpenThrowerAll) {
                        throwerAllFpvBtn.setText(R.string.close_all)
                    } else {
                        throwerAllFpvBtn.setText(R.string.openAll)
                    }
                }
            }
        }

        // 1号充电放电
        allowDetonationSwitch1.setOnClickListener {
            allowDetonationSwitch1.isEnabled = false
            allInOneService.allowDetonate(1, allowDetonationSwitch1.isChecked)
            thread {
                Thread.sleep(2000)
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    allowDetonationSwitch1.isEnabled = true
                }
            }
        }

        // 2号充电放电
        allowDetonationSwitch2.setOnClickListener {
            allowDetonationSwitch2.isEnabled = false
            allInOneService.allowDetonate(2, allowDetonationSwitch2.isChecked)
            thread {
                Thread.sleep(2000)
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    allowDetonationSwitch2.isEnabled = true
                }
            }
        }

        detonateHeightEditText.setOnClickListener {
            if(it.hasFocus()) {
                isSettingDetonateHeight = true
            }
        }
        detonateHeightEditText.setOnEditorActionListener { view, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE,
                EditorInfo.IME_ACTION_SEARCH,
                EditorInfo.IME_ACTION_GO -> {
                    // 获取输入法管理器
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    // 隐藏软键盘，使用当前视图的 windowToken
                    imm.hideSoftInputFromWindow(view.windowToken, 0)

                    var detonateHeight = detonateHeightEditText.text.toString().toIntOrNull() ?: 0 // 使用 toIntOrNull 避免转换异常
                    if(detonateHeight > 20) {
                        detonateHeight = 20
                        detonateHeightEditText.setText("$detonateHeight")
                    }
                    allInOneService.setDetonateHeight(detonateHeight)
                    thread {
                        Thread.sleep(2000)
                        isSettingDetonateHeight = false
                    }
                    true // 消费事件
                }
                else -> false
            }
        }
        // 打开全屏
        enlargeBtn.setOnClickListener {
            switchToFullScreen()
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

    // 创建播放器
    private fun initPlayer() {
        // 创建播放器（使用简化的事件监听）
        rtspPlayer = RtspPlayer(streamUrl, playerView, object : RtspPlayer.RtspPlayerEventListener {
            override fun onPlaying() {
//                showToast("开始播放")
            }

            override fun onStopped() {
//                showToast("播放停止")
            }

            override fun onError(errorMessage: String) {
//                showToast("错误: $errorMessage")
            }

            override fun onLogMessage(message: String) {
//                Log.d("RtspPlayer", message)
            }

            override fun onVideoSizeChanged(width: Int, height: Int) {
                // 可选的视频尺寸变化处理
            }

            override fun onFrameRendered(frameCount: Int) {
                // 可选的帧渲染回调
            }
        })
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
                    thrower1Btn.setText(R.string.close_1)
                }
                else {
                    thrower1Btn.setText(R.string.open_1)
                }
            }
            if (thrower1FpvBtn.isEnabled) {
                if (isOpenThrower1) {
                    thrower1FpvBtn.setText(R.string.close_1)
                }
                else {
                    thrower1FpvBtn.setText(R.string.open_1)
                }
            }
            if (thrower2Btn.isEnabled) {
                if (isOpenThrower2) {
                    thrower2Btn.setText(R.string.close_2)
                }
                else {
                    thrower2Btn.setText(R.string.open_2)
                }
            }
            if (thrower2FpvBtn.isEnabled) {
                if (isOpenThrower2) {
                    thrower2FpvBtn.setText(R.string.close_2)
                }
                else {
                    thrower2FpvBtn.setText(R.string.open_2)
                }
            }
            if(throwerAllBtn.isEnabled) {
                if(isOpenThrowerAll) {
                    throwerAllBtn.setText(R.string.close_all)
                }
                else {
                    throwerAllBtn.setText(R.string.openAll)
                }
            }
            if(throwerAllFpvBtn.isEnabled) {
                if(isOpenThrowerAll) {
                    throwerAllFpvBtn.setText(R.string.close_all)
                }
                else {
                    throwerAllFpvBtn.setText(R.string.openAll)
                }
            }
        }
        updateThrowerState(1, msg.slice(11 until 19).toByteArray())
        updateThrowerState(2, msg.slice(19 until 27).toByteArray())
    }
    // 更新抛投和灭火弹状态
    private fun updateThrowerState(index: Int, stateMsg: ByteArray) {
        val handler = Handler(Looper.getMainLooper())
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
        handler.post {
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


    fun attachFloatingWindow(service: PayloadWeight) {
        this.thisPayloadWeight = service
    }

    /**
     * 切换到全屏模式
     */
    private fun switchToFullScreen() {
        try {
            val intent = Intent(context, FullScreenVideoActivity::class.java).apply {
                putExtra(FullScreenVideoActivity.EXTRA_STREAM_URL, streamUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            thisPayloadWeight?.allInOneThrowerBtn?.performClick()

        } catch (e: Exception) {
            Log.e(TAG, "切换到全屏失败: ${e.message}")
            Toast.makeText(context, "全屏模式暂不可用", Toast.LENGTH_SHORT).show()
        }
    }

    // 定时器，判断连接状态
    private fun setConnectState() {
        val timer = Timer();
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask() {
            override fun run() {
                // 已连接
                if (allInOneService.getMainIsConnected()) {
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

    private fun showToast(msg: Int) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(
                MApplication.applicationContext, msg, Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showToast(msg: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(
                MApplication.applicationContext, msg, Toast.LENGTH_SHORT
            ).show()
        }
    }
}