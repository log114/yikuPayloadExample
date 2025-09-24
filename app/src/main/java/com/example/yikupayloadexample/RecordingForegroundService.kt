package com.example.yikupayloadexample

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class RecordingForegroundService : Service() {
    private val TAG = "RecordingForegroundService"

    companion object {
        const val ACTION_STOP = "com.example.yikupayloadexample.action.STOP"
        const val ACTION_STARTED = "com.example.yikupayloadexample.action.STARTED"
        var onServiceStarted: (() -> Unit)? = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Log.d(TAG, "收到停止服务请求")
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1, notification)
        }

        Log.d(TAG, "前台服务已启动，通知ID: 1")

        // 通知服务已启动
        onServiceStarted?.invoke()
        onServiceStarted = null

        // 发送广播通知服务已启动
        sendBroadcast(Intent(ACTION_STARTED))

        // 返回 START_STICKY 确保服务持续运行
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "recording_channel"
            val channelName = "Recording Service"
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            // 检查渠道是否已存在
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(channelId, channelName, importance).apply {
                    description = "Channel for recording foreground service"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC // 锁屏可见
                    setShowBadge(true) // 显示角标
                    enableVibration(false) // 无振动
                    enableLights(false) // 无灯光
                }
                manager.createNotificationChannel(channel)
                Log.d(TAG, "创建通知渠道")
            } else {
                Log.d(TAG, "通知渠道已存在")
            }
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "recording_channel")
            .setContentTitle("实时喊话")
            .setContentText("正在使用麦克风进行实时喊话")
            .setSmallIcon(R.drawable.ic_microphone) // 确保有这个图标资源
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // 提高优先级
            .setOngoing(true) // 设置为持续通知
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 锁屏可见
            .setCategory(Notification.CATEGORY_SERVICE) // 设置为服务类别
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}