package com.example.yikupayloadexample.util

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import com.jxj.ffmpegrtsp.lib.FFmpegCallbacks
import com.jxj.ffmpegrtsp.lib.FFmpegRTSPLibrary
import com.jxj.ffmpegrtsp.lib.VideoInfo

/**
 * RTSP播放器类
 * 封装了RTSP流播放功能，支持H.264和H.265编码
 */
class RtspPlayer(
    private val url: String,
    private val textureView: TextureView,  // 改为TextureView
    private val eventListener: RtspPlayerEventListener? = null
) : TextureView.SurfaceTextureListener {  // 改为实现SurfaceTextureListener

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
    private var surface: Surface? = null

    // 添加视频原始分辨率属性
    private var originalVideoWidth = 0
    private var originalVideoHeight = 0

    // 添加获取方法
    fun getVideoResolution(): Pair<Int, Int> = Pair(originalVideoWidth, originalVideoHeight)

    init {
        // 设置Surface回调
        textureView.surfaceTextureListener = this
    }

    fun getStreamId(): Int {
        return streamId
    }

    /**
     * 开始播放RTSP流
     * @param rtspUrl RTSP流地址
     */
    fun startPlayback(rtspUrl: String) {
        if (streamId < 0) {
            streamId = FFmpegRTSPLibrary.createStreamWithDecodeMode(rtspUrl, false)
            if (streamId < 0) {
                eventListener?.onError("创建流失败，streamId=$streamId")
                return
            }
        }

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
            if (surface == null) {
                eventListener?.onError("Surface未创建")
                return
            }
            val result = FFmpegRTSPLibrary.setSurface(streamId, surface!!)
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
                        eventListener?.onPlaying()
                    }

                    override fun onPlaybackError(
                        streamId: Int,
                        errorCode: Int,
                        errorMessage: String
                    ) {
                        release()
                        eventListener?.onError("播放错误: $errorMessage")
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
                    eventListener?.onStopped()
                }

                override fun onPlaybackError(streamId: Int, errorCode: Int, errorMessage: String) {
                    isPlaying = false
                    eventListener?.onError("停止播放错误: $errorMessage")
                }
            })
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        if (streamId >= 0) {
            try{
                stopPlayback()
                FFmpegRTSPLibrary.destroyStream(streamId)
            }
            catch (e: Exception) {
                eventListener?.onError("释放资源时出错：${e.message}")
            }
        }
        surface?.release()
        surface = null
        streamId = -1
        isPlaying = false
        eventListener?.onLogMessage("RTSP播放器资源已释放")
    }

    /**
     * 获取播放状态
     */
    fun isPlaying(): Boolean = isPlaying

    // SurfaceTextureListener实现
    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        surfaceReady = true
        surface = Surface(surfaceTexture)
        eventListener?.onLogMessage("SurfaceTexture已创建")
        startPlayback(url)
    }

    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        eventListener?.onLogMessage("SurfaceTexture尺寸变化: ${width}x$height")
        eventListener?.onVideoSizeChanged(width, height)
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        surfaceReady = false
        eventListener?.onLogMessage("SurfaceTexture被销毁")
        if (streamId >= 0) {
            FFmpegRTSPLibrary.onSurfaceDestroyed(streamId)
        }
        surface?.release()
        surface = null
        return true
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        // 每一帧更新时调用
    }
}