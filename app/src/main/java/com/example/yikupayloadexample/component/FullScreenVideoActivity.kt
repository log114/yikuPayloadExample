package com.example.yikupayloadexample.component

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yikupayloadexample.R
import com.example.yikupayloadexample.util.PlayerCallback
import com.example.yikupayloadexample.util.RtspPlayer
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import com.example.yikupayloadexample.MApplication
import java.security.Provider

class FullScreenVideoActivity : AppCompatActivity() {
    private val TAG = "FullScreenVideoActivity"
    private lateinit var fullScreenVideoLayout: VLCVideoLayout
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
        fullScreenVideoLayout = findViewById(R.id.fullscreen_video_layout)
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
        rtspPlayer = RtspPlayer(this)
        rtspPlayer?.registPlayerCallback(object : PlayerCallback {
            override fun onPlaying(index: Int, mediaPlayer: MediaPlayer) {
                // 视频播放成功
            }

            override fun onError(index: Int) {
                // 处理播放错误
                Log.e(TAG, "播放失败")
//                finish()
            }
        })

        rtspPlayer?.createPlayer(0, fullScreenVideoLayout, streamUrl!!)
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
                Toast.LENGTH_LONG
            ).show()
        }
    }
}