package com.example.yikupayloadexample.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.yikupayloadexample.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        // 1. 保存日志到文件
        saveCrashLog(ex)
        // 2. 可选：重启应用或退出
        defaultHandler?.uncaughtException(thread, ex) // 调用默认处理（退出）
        // 或重启： restartApp(context)
    }

    private fun saveCrashLog(ex: Throwable) {
        val log = buildLogContent(ex)
        val fileName = "log_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.txt"
        val logDir = context.getExternalFilesDir("crash_logs") ?: context.filesDir.resolve("crash_logs")
        logDir.mkdirs()
        Log.d("CrashHandler", "崩溃日志保存目录: ${logDir.absolutePath}")
        File(logDir, fileName).writeText(log)

        // 定期清理旧日志（例如保留最近7天）
        val maxAgeMs = 7 * 24 * 60 * 60 * 1000L
        logDir.listFiles()?.forEach { file ->
            if (file.lastModified() < Date().time - maxAgeMs) {
                file.delete()
            }
        }
    }

    private fun buildLogContent(ex: Throwable): String {
        val deviceInfo = """
            |App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
            |Android API: ${Build.VERSION.SDK_INT}
            |Device: ${Build.MANUFACTURER} ${Build.MODEL}
            |Fingerprint: ${Build.FINGERPRINT}
        """.trimMargin()

        val stackTrace = StringWriter().apply {
            ex.printStackTrace(PrintWriter(this))
        }.toString()

        return "$deviceInfo\n\n$stackTrace"
    }
}