package com.example.yikupayloadexample.component

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yikupayloadexample.R
import com.example.yikupayloadexample.util.RtspPlayer
import com.example.yikupayloadexample.MApplication

class FullScreenVideoActivity : AppCompatActivity() {
    private val TAG = "FullScreenVideoActivity"
    private lateinit var playerView: SurfaceView
    private lateinit var exitFullScreenBtn: ImageView
    private lateinit var controlsLayout: LinearLayout

    private var rtspPlayer: RtspPlayer? = null
    private var streamUrl: String? = null

    companion object {
        const val EXTRA_STREAM_URL = "extra_stream_url"
    }

    @SuppressLint("SourceLockedOrientationPortrait")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置全屏
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_fullscreen_video)

        initViews()
        handleIntent()
        setupPlayer()
        setupListeners()
    }

    private fun initViews() {
        playerView = findViewById(R.id.fullscreen_video_layout)
        exitFullScreenBtn = findViewById(R.id.exit_fullscreen_btn)
        controlsLayout = findViewById(R.id.fullscreen_controls_layout)
    }

    private fun handleIntent() {
        streamUrl = intent.getStringExtra(EXTRA_STREAM_URL)
        if (streamUrl.isNullOrEmpty()) {
            finish()
            return
        }
    }

    private fun setupPlayer() {
        // 创建播放器（使用简化的事件监听）
        rtspPlayer = RtspPlayer(streamUrl!!, playerView, object : RtspPlayer.RtspPlayerEventListener {
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

    private fun setupListeners() {
        // 退出全屏
        exitFullScreenBtn.setOnClickListener {
            this.moveTaskToBack(true)
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rtspPlayer?.release()
    }

    override fun onBackPressed() {
        this.moveTaskToBack(true)
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun showToast(toastMsg: Int) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(
                MApplication.applicationContext,
                toastMsg,
                Toast.LENGTH_SHORT
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