package com.example.yikupayloadexample

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.example.yikupayloadexample.util.PlayerCallback
import com.example.yikupayloadexample.util.RtspPlayer
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class AllInOneFpvWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    private val TAG = "AllInOneFpvWeight"
    private lateinit var enlargeBtn: ImageView
    private var playerView: VLCVideoLayout? = null
//    private var streamUrl = "rtsp://192.168.144.188:554/ch01_sub"
    private var streamUrl = "rtsp://192.168.144.108:554/stream=1"
    private var rtspPlayer: RtspPlayer = RtspPlayer(context)

    // 当窗口被加载时，加载视频
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (streamUrl != "") {
            rtspPlayer.registPlayerCallback(object : PlayerCallback {
                override fun onPlaying(index: Int, mediaPlayer: MediaPlayer) {
                    Log.i(TAG, "视频播放成功")
                }

                override fun onError(index: Int) {
                    Log.e(TAG, "视频播放失败")
                    Toast.makeText(context, "视频播放失败", Toast.LENGTH_SHORT).show()
                }
            })
            rtspPlayer.createPlayer(3, playerView!!, streamUrl)
        }
    }
    // 当窗口被移除时，释放视频资源
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d(TAG, "onDetachedFromWindow: 视频View被移出窗口层级")

        // 释放视频资源
        rtspPlayer.release()
    }
    init {
        initView(context)
        enlargeBtn.setOnClickListener {

        }
    }
    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.all_in_one_fpv_weight, this, true)
        enlargeBtn = findViewById(R.id.enlarge_btn)
        playerView = findViewById(R.id.playerView)
    }
}