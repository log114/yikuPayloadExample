package com.example.yikupayloadexample

import android.content.Context
import android.databinding.tool.ext.T
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.example.yikupayloadexample.util.RtspPlayer
import com.yiku.yikupayloadSDK.protocol.ALLINONE_PITCH_STATE
import com.yiku.yikupayloadSDK.util.MsgCallback
import kotlin.concurrent.thread
import android.view.SurfaceView
import android.widget.RelativeLayout
import java.util.Date
import java.util.Timer
import java.util.TimerTask

class AllInOneFpvWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    private val TAG = "AllInOneFpvWeight"
    private lateinit var pageLayout: RelativeLayout
    private lateinit var enlargeBtn: ImageView
    private lateinit var playerParentView: LinearLayout
    private lateinit var playerView: SurfaceView
    private lateinit var rtspPlayer: RtspPlayer
    private var streamUrl = "rtsp://192.168.144.188:554/ch01_sub"
//    private var streamUrl = "rtsp://192.168.144.108:554/stream=1"
    private lateinit var pitchSeekBar: SeekBar
    private lateinit var pitchText: TextView
    private var isSettingPitch: Boolean = false
    private var thisPayloadWeight: PayloadWeight? = null
    private var isInitPlayer: Boolean = false
    private var isAdjusting = false
    private var isFullScreen = false
    private val interval = 50 // 限制两次俯仰控制间隔时间不得小于50ms
    private var lastTime = Date().time // 上一次控制俯仰的时间
    private var settingTimer: Timer? = null

    // 当窗口被加载时，加载视频
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (streamUrl != "") {
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
        Log.d(TAG, "onDetachedFromWindow: 视频View被移出窗口层级")

        // 释放视频资源
        if(isInitPlayer) {
            rtspPlayer.stopPlayback()
        }
    }

    init {
        initView(context)
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
                    var pitchValue = ((msg[3].toInt()  and 0xFF) shl 8) or (msg[4].toInt()  and 0xFF)
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        pitchSeekBar.progress = pitchValue
                        // 俯仰小于3°的时候，显示为0°
                        if(pitchValue < 3) {
                            pitchValue = 0
                        }
                        pitchText.text = "${pitchValue/10}°"
                    }
                }
            }
        })
        // 放大窗口
        enlargeBtn.setOnClickListener {
            switchToFullScreen()
        }
        // 俯仰控制
        pitchSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 如果不是手动拖动导致的更改，不重新发命令
                if(!isSettingPitch) {
                    return
                }
                pitchText.text = "${seekBar.progress/10}°"
                if(Date().time - lastTime > interval) {
                    lastTime = Date().time
                    allInOneService.pitchControl(seekBar.progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isSettingPitch = true
                cancelResume()
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                Log.i(TAG, "俯仰控制，当前俯仰值：${seekBar.progress}")
                allInOneService.pitchControl(seekBar.progress) // 结束的时候也发一下，避免不统一
                resumeMonitor()
            }
        })
    }
    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.all_in_one_fpv_weight, this, true)
        pageLayout = findViewById(R.id.allInOneFpvWeight)
        enlargeBtn = findViewById(R.id.enlarge_btn)
        playerParentView = findViewById(R.id.playerParentView)
        playerView = findViewById(R.id.playerView)
        pitchSeekBar = findViewById(R.id.pitch_seek_bar)
        pitchText = findViewById(R.id.pitch_text)
    }

    fun attachFloatingWindow(service: PayloadWeight) {
        this.thisPayloadWeight = service
    }

    // 创建播放器
    private fun initPlayer() {
        // 创建播放器（使用简化的事件监听）
        rtspPlayer = RtspPlayer(streamUrl, playerView, object : RtspPlayer.RtspPlayerEventListener {
            override fun onPlaying() {
//                showToast("开始播放")
                // 获取并保存原始分辨率
                val (width, height) = rtspPlayer.getVideoResolution()
                if (width > 0 && height > 0) {
                    Log.d(TAG, "播放开始，原始分辨率: ${width}x${height}")
//                    adjustContainerAspectRatio()
                }
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

            }

            override fun onFrameRendered(frameCount: Int) {
                // 可选的帧渲染回调
            }
        })
    }

    /**
     * 根据视频宽高比调整 video_container (FrameLayout) 的高度
     */
    private fun adjustContainerAspectRatio() {
        if (isAdjusting) return
        isAdjusting = true
        var (maxWidth, maxHeight) = thisPayloadWeight!!.getScreenSize(includeSystemBars = false)
        maxHeight = maxHeight - 84 // 去掉头部标题和内边距，才是这里可以使用的最大高度
        val bottomControlHeight = 76 // 底部控件高度
        val pagePadding = 8
//        val aspectRatio = rtspPlayer.getOriginalAspectRatio() // 视频原始宽高比
        val aspectRatio: Float = (1280.00/720.00).toFloat() // 视频宽高比固定为1280*720
        Log.d(TAG, "宽高比：${aspectRatio}")
        var targetPageWidthPx = 0
        var targetPageHeightPx = 0
        var targetPlayerWidthPx = 0
        var targetPlayerHeightPx = 0
        // 全屏时，按照最大宽度和高度计算
        if(isFullScreen) {
            // 先基于最大高度计算目标宽度
            targetPageHeightPx = maxHeight
            targetPlayerHeightPx = targetPageHeightPx - bottomControlHeight - pagePadding*2
            targetPlayerWidthPx = (targetPlayerHeightPx * aspectRatio).toInt()
            targetPageWidthPx = targetPlayerWidthPx + pagePadding*2
            // 如果计算结果超出最大限值，那改为基于最大宽度计算
            if(targetPageWidthPx > maxWidth) {
                targetPageWidthPx = maxWidth
                targetPlayerWidthPx = targetPageWidthPx - pagePadding*2
                targetPlayerHeightPx = (targetPlayerWidthPx / aspectRatio).toInt()
                targetPageHeightPx = targetPlayerHeightPx + bottomControlHeight + pagePadding*2
            }
        }
        else { // 非全屏时，先将高度定为414计算高度
            targetPageWidthPx = 900
            targetPageHeightPx = 490
            targetPlayerHeightPx = 414
            targetPlayerWidthPx = (targetPlayerHeightPx * aspectRatio).toInt()
            if(targetPlayerWidthPx > targetPageWidthPx - pagePadding*2) {
                targetPlayerWidthPx = targetPageWidthPx - pagePadding*2
                targetPlayerHeightPx = (targetPlayerWidthPx / aspectRatio).toInt()
            }
        }
        Log.d(TAG, "targetPageWidthPx=${targetPageWidthPx}, targetPageHeightPx=${targetPageHeightPx}, targetPlayerWidthPx=${targetPlayerWidthPx}, targetPlayerHeightPx=${targetPlayerHeightPx}")
        // 更新UI：调整 video_container 的高度
        updateVideoContainerHeight(targetPageWidthPx, targetPageHeightPx, targetPlayerWidthPx, targetPlayerHeightPx)
    }

    private fun updateVideoContainerHeight(targetPageWidthPx: Int, targetPageHeightPx: Int, targetPlayerWidthPx: Int, targetPlayerHeightPx: Int) {
        Handler(Looper.getMainLooper()).post {
            pageLayout.layoutParams = pageLayout.layoutParams.apply {
                width = targetPageWidthPx
                height = targetPageHeightPx
            }
            playerParentView.layoutParams = playerParentView.layoutParams.apply {
                width = targetPlayerWidthPx
                height = targetPlayerHeightPx
            }
        }
    }

    /**
     * 切换到全屏模式
     */
    private fun switchToFullScreen() {
        if(!isFullScreen) {
            thisPayloadWeight?.fullScreen()
        }
        else {
            thisPayloadWeight?.recoverSize()
        }
        isFullScreen = !isFullScreen
        // 延时一下，确保布局已应用
        Handler(Looper.getMainLooper()).postDelayed({
            isAdjusting = false
            adjustContainerAspectRatio()
        }, 50)
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

    // 恢复俯仰状态读取
    private fun resumeMonitor() {
        settingTimer = Timer();
        val task = object : TimerTask() {
            override fun run() {
                isSettingPitch = false
            }
        }
        settingTimer?.schedule(task, 4000);
    }
    // 取消恢复
    private fun cancelResume() {
        settingTimer?.cancel()
        settingTimer?.purge()
        settingTimer = null
    }
}