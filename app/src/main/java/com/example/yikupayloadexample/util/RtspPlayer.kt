package com.example.yikupayloadexample.util

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import com.jxj.ffmpegrtsp.lib.FFmpegCallbacks
import com.jxj.ffmpegrtsp.lib.FFmpegRTSPLibrary
import com.jxj.ffmpegrtsp.lib.VideoInfo
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.ProcessBuilder
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * RTSP播放器类
 * 封装了RTSP流播放功能，支持H.264和H.265编码
 */
class RtspPlayer(
    private val url: String,
    private val surfaceView: SurfaceView,
    private val eventListener: RtspPlayerEventListener? = null
) : SurfaceHolder.Callback {

    /**
     * RTSP播放器事件监听接口
     */
    interface RtspPlayerEventListener {
        fun onPlaying()
        fun onStopped()
        fun onError(errorMessage: String)
        fun onLogMessage(message: String)
        fun onVideoSizeChanged(width: Int, height: Int)
        fun onFrameRendered(frameCount: Int)
    }

    // 播放状态变量
    private var isPlaying = false
    private var surfaceReady = false
    private var streamId = -1

    // 添加视频原始分辨率属性
    private var originalVideoWidth = 0
    private var originalVideoHeight = 0

    // 添加获取方法
    fun getVideoResolution(): Pair<Int, Int> = Pair(originalVideoWidth, originalVideoHeight)

    init {
        // 设置Surface回调
        surfaceView.holder.addCallback(this)
    }

    /**
     * 开始播放RTSP流
     * @param rtspUrl RTSP流地址
     */
    fun startPlayback(rtspUrl: String) {
        streamId = FFmpegRTSPLibrary.createStream(rtspUrl)
        if (isPlaying) {
            eventListener?.onLogMessage("正在播放中，请先停止当前播放")
            return
        }

        if (!surfaceReady) {
            eventListener?.onLogMessage("Surface尚未准备就绪")
            return
        }

        if (rtspUrl.isEmpty()) {
            eventListener?.onError("RTSP流地址不能为空")
            return
        }

        if(!isPlaying) {
            val result = FFmpegRTSPLibrary.setSurface(streamId, surfaceView.holder.surface)
            if (result == 0) {
                eventListener?.onLogMessage("Surface设置成功")
            } else {
                eventListener?.onError("Surface设置失败")
                return
            }
            FFmpegRTSPLibrary.startPlayAsync(
                streamId,
                object : FFmpegCallbacks.PlaybackStartCallback {
                    override fun onPlaybackStarted(streamId: Int, videoInfo: VideoInfo?) {
                        isPlaying = true
                    }

                    override fun onPlaybackError(
                        streamId: Int,
                        errorCode: Int,
                        errorMessage: String
                    ) {
                        isPlaying = false
                    }
                }
            )
        }

    }

    /**
     * 停止播放
     */
    fun stopPlayback() {
        if (isPlaying) {
            FFmpegRTSPLibrary.stopPlayAsync(streamId, object : FFmpegCallbacks.PlaybackStopCallback {
                override fun onPlaybackStopped(streamId: Int) {
                    isPlaying = false
                }

                override fun onPlaybackError(streamId: Int, errorCode: Int, errorMessage: String) {
                    isPlaying = false
                }
            })
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        if (streamId >= 0) {
            stopPlayback()
            FFmpegRTSPLibrary.destroyAllStreamsAsync()
        }
        eventListener?.onLogMessage("RTSP播放器资源已释放")
    }

    /**
     * 获取播放状态
     */
    fun isPlaying(): Boolean = isPlaying

    // SurfaceHolder.Callback实现
    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceReady = true
        eventListener?.onLogMessage("Surface已创建")
        startPlayback(url)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        eventListener?.onLogMessage("Surface尺寸变化: ${width}x$height")
        eventListener?.onVideoSizeChanged(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
        eventListener?.onLogMessage("Surface被销毁")
        if (streamId >= 0) {
            FFmpegRTSPLibrary.onSurfaceDestroyed(streamId)
        }
    }

}