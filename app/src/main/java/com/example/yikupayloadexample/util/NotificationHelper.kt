package com.example.yikupayloadexample.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.yikupayloadexample.R
import java.io.File

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "app_update_channel"
        const val NOTIFICATION_ID = 1001
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.resources.getString(R.string.app_update),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.resources.getString(R.string.app_update_notification)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    // 添加这个方法
    fun showInitialNotification(builder: NotificationCompat.Builder) {
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun createDownloadNotification(): NotificationCompat.Builder {
        val intent = Intent(context, context.javaClass) // 使用当前Activity
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.resources.getString(R.string.app_update))
            .setContentText(context.resources.getString(R.string.downloading_new_version))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, 0, false)
    }

    fun updateDownloadProgress(progress: Int, builder: NotificationCompat.Builder) {
        builder.setProgress(100, progress, false)
            .setContentText("${context.resources.getString(R.string.download_progress)}: $progress%")

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun showDownloadCompleteNotification(file: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                android.net.Uri.fromFile(file)
            }
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.resources.getString(R.string.download_completed))
            .setContentText(context.resources.getString(R.string.click_to_install))
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun showDownloadFailedNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.resources.getString(R.string.download_failed))
            .setContentText(context.resources.getString(R.string.app_update_fail))
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}