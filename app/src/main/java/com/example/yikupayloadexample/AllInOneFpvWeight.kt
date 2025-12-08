package com.example.yikupayloadexample

import android.content.Context
import android.content.Intent
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
import com.example.yikupayloadexample.component.FullScreenVideoActivity
import com.example.yikupayloadexample.util.RtspPlayer
import com.yiku.yikupayloadSDK.protocol.ALLINONE_PITCH_STATE
import com.yiku.yikupayloadSDK.util.MsgCallback
import kotlin.concurrent.thread
import android.view.SurfaceView

class AllInOneFpvWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    private val TAG = "AllInOneFpvWeight"
    private lateinit var enlargeBtn: ImageView
    private lateinit var playerView: SurfaceView
    private lateinit var rtspPlayer: RtspPlayer
//    private var streamUrl = "rtsp://192.168.144.188:554/ch01_sub"
    private var streamUrl = "rtsp://192.168.144.108:554/stream=1"
    private lateinit var pitchSeekBar: SeekBar
    private lateinit var pitchText: TextView
    private var isSettingPitch: Boolean = false
    private var thisPayloadWeight: PayloadWeight? = null
    private var isInitPlayer: Boolean = false

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
                    val pitchValue = ((msg[3].toInt()  and 0xFF) shl 8) or (msg[4].toInt()  and 0xFF)
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        pitchSeekBar.progress = pitchValue
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
    }
    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.all_in_one_fpv_weight, this, true)
        enlargeBtn = findViewById(R.id.enlarge_btn)
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
            thisPayloadWeight?.allInOneFpvBtn?.performClick()

        } catch (e: Exception) {
            Log.e(TAG, "切换到全屏失败: ${e.message}")
            Toast.makeText(context, "全屏模式暂不可用", Toast.LENGTH_SHORT).show()
        }
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