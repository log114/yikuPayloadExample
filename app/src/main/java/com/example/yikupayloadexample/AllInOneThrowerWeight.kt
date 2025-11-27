package com.example.yikupayloadexample

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
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
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

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
    //    private var streamUrl = "rtsp://192.168.144.188:554/ch01_sub"
    private var streamUrl = "rtsp://192.168.144.108:554/stream=1"
    private var rtspPlayer: RtspPlayer = RtspPlayer(context)

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
    }
}