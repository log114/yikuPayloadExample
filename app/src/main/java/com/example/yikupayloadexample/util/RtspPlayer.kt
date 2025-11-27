package com.example.yikupayloadexample.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import androidx.core.net.toUri
import java.util.ArrayList
import kotlin.collections.plus
import kotlin.concurrent.thread

interface PlayerCallback {
    fun onPlaying(index: Int, mediaPlayer: MediaPlayer)
    fun onError(index: Int)
}

class RtspPlayer(context: Context) {
    private val TAG = "RtspPlayer"
    private val lock = Any()
    private val _context: Context = context
    private val args = mutableListOf(
        "--network-caching=0",
        "--clock-jitter=0",
        "--clock-synchro=0",
        "--live-caching=0",
        "--tcp-caching=0",
        "--rtsp-tcp",
        "--avcodec-fast"
    )

    private var libVLC: LibVLC? = LibVLC(context, args)
    private var currentMediaPlayer: MediaPlayer? = null  // 添加当前播放器引用
    private var playerCallbacks: MutableList<PlayerCallback> = ArrayList()
    private var isReleasing = false  // 防止重复释放标志

    fun createPlayer(index: Int, videoLayout: VLCVideoLayout, url: String) {
        // 先释放之前的资源
        cleanupCurrentPlayer()

        if (libVLC == null) {
            try {
                libVLC = LibVLC(_context, args)
            } catch (e: Exception) {
                Log.e(TAG, "LibVLC init failed", e)
                return
            }
        }

        val mediaPlayer = MediaPlayer(libVLC).apply {
            // 存储当前播放器引用
            currentMediaPlayer = this

            // 绑定渲染视图
            attachViews(videoLayout, null, false, false)

            // 动态设置窗口尺寸
            videoLayout.post {
                val width = videoLayout.width
                val height = videoLayout.height
                vlcVout.setWindowSize(width, height)
                Log.d(TAG, "width: $width, height: $height")
            }

            scale = 0f
            aspectRatio = ""
        }

        // 设置媒体源
        val media = Media(libVLC, url.toUri()).apply {
            addOption(":network-caching=0")
            addOption(":rtsp-timeout=300")
        }

        mediaPlayer.media = media
        mediaPlayer.play()

        // 设置事件监听器
        mediaPlayer.setEventListener(object : MediaPlayer.EventListener {
            override fun onEvent(event: MediaPlayer.Event) {
                // 检查是否正在释放资源，避免在释放过程中处理事件
                if (isReleasing) return

                when (event.type) {
                    MediaPlayer.Event.Playing -> {
                        Log.d(TAG, "播放成功")
                        playerCallbacks.forEach { it.onPlaying(index, mediaPlayer) }
                    }
                    MediaPlayer.Event.EncounteredError -> {
                        Log.e(TAG, "播放失败")
                        playerCallbacks.forEach { it.onError(index) }

                        if (!isReleasing) {
                            // 异步重连，避免阻塞主线程
                            thread {
                                Thread.sleep(1000)
                                Handler(Looper.getMainLooper()).post {
                                    if (!isReleasing) {
                                        createPlayer(index, videoLayout, url)
                                    }
                                }
                            }
                        }
                    }
                    MediaPlayer.Event.Buffering -> {
                        if (event.buffering == 100f) {
                            Log.d(TAG, "缓冲完成")
                        }
                    }
                    // 添加更多事件处理...
                    MediaPlayer.Event.Stopped -> {
                        Log.d(TAG, "播放停止")
                    }
                    MediaPlayer.Event.EndReached -> {
                        Log.d(TAG, "播放结束")
                    }
                }
            }
        })
    }

    /**
     * 完全释放资源的方法
     */
    fun release() {
        synchronized(lock) {
            if (isReleasing) return  // 防止重复释放
            isReleasing = true

            Log.d(TAG, "开始释放VLC资源...")

            // 1. 先停止并释放当前MediaPlayer
            cleanupCurrentPlayer()

            // 2. 释放LibVLC实例
            libVLC?.let { vlc ->
                try {
                    vlc.release()
                    libVLC = null
                    Log.d(TAG, "LibVLC资源已释放")
                } catch (e: Exception) {
                    Log.e(TAG, "释放LibVLC时出错", e)
                }
            }

            // 3. 清空回调列表
            playerCallbacks.clear()

            isReleasing = false
            Log.d(TAG, "VLC资源释放完成")
        }
    }

    /**
     * 专门清理当前MediaPlayer的方法
     */
    private fun cleanupCurrentPlayer() {
        synchronized(lock) {
            currentMediaPlayer?.let { player ->
                try {
                    // 移除事件监听器
                    player.setEventListener(null)

                    // 停止播放
                    player.stop()

                    // 分离视图
                    player.detachViews()

                    // 释放媒体资源
                    player.media?.release()

                    // 释放播放器实例
                    player.release()

                    Log.d(TAG, "MediaPlayer资源已释放")
                } catch (e: Exception) {
                    Log.e(TAG, "释放MediaPlayer时出错", e)
                } finally {
                    currentMediaPlayer = null
                }
            }
        }
    }

    /**
     * 安全停止播放（不释放资源）
     */
    fun stop() {
        synchronized(lock) {
            currentMediaPlayer?.let { player ->
                try {
                    player.stop()
                    Log.d(TAG, "播放已停止")
                } catch (e: Exception) {
                    Log.e(TAG, "停止播放时出错", e)
                }
            }
        }
    }

    /**
     * 安全暂停播放
     */
    fun pause() {
        synchronized(lock) {
            currentMediaPlayer?.let { player ->
                if (player.isPlaying) {
                    try {
                        player.pause()
                        Log.d(TAG, "播放已暂停")
                    } catch (e: Exception) {
                        Log.e(TAG, "暂停播放时出错", e)
                    }
                }
            }
        }
    }

    /**
     * 检查是否正在播放
     */
    fun isPlaying(): Boolean {
        return currentMediaPlayer?.isPlaying ?: false
    }

    fun registPlayerCallback(playerCallback: PlayerCallback) {
        this.playerCallbacks += playerCallback
    }

    /**
     * 移除指定的回调
     */
    fun unregistPlayerCallback(playerCallback: PlayerCallback) {
        this.playerCallbacks.remove(playerCallback)
    }
}