package com.example.yikupayloadexample.util

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.yikupayloadexample.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import kotlinx.coroutines.delay

class AppUpdateManager private constructor(
    private val context: Context,
    private val baseUrl: String,
    private val apiPath: String
) {

    private lateinit var apiService: ApiService
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var isDownloading = false
    private var downloadUrl: String = ""
    private var onUpdateListener: OnUpdateListener? = null

    companion object {
        fun with(context: Context): Builder {
            return Builder(context)
        }
    }

    class Builder(private val context: Context) {
        private var baseUrl: String = ""
        private var apiPath: String = ""

        fun setBaseUrl(baseUrl: String): Builder {
            this.baseUrl = baseUrl
            return this
        }

        fun setApiPath(apiPath: String): Builder {
            this.apiPath = apiPath
            return this
        }

        fun build(): AppUpdateManager {
            return AppUpdateManager(context, baseUrl, apiPath)
        }
    }

    interface OnUpdateListener {
        fun onUpdateAvailable(versionData: VersionData)
        fun onUpdateCheckFailed(error: String)
        fun onDownloadStarted()
        fun onDownloadProgress(progress: Int)
        fun onDownloadCompleted(file: File)
        fun onDownloadFailed(error: String)
        fun onInstallStarted()
        fun onInstallFailed(error: String)
    }

    fun setOnUpdateListener(listener: OnUpdateListener): AppUpdateManager {
        this.onUpdateListener = listener
        return this
    }

    private fun initComponents() {
        initRetrofit()
        notificationHelper = NotificationHelper(context)
        notificationBuilder = notificationHelper.createDownloadNotification()
    }

    fun checkVersionUpdate() {
        initComponents()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.checkVersion()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val versionResponse = response.body()
                        if (versionResponse?.result == "success") {
                            versionResponse.data?.let { versionData ->
                                val currentVersion = getCurrentVersionName()

                                if (isNewVersionAvailable(versionData.version, currentVersion)) {
                                    onUpdateListener?.onUpdateAvailable(versionData)
                                    showUpdateDialog(versionData)
                                } else {
                                    onUpdateListener?.onUpdateCheckFailed("当前已是最新版本")
                                    Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            val errorMessage = versionResponse?.message ?: "检查更新失败"
                            onUpdateListener?.onUpdateCheckFailed(errorMessage)
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorMessage = "网络请求失败: ${response.code()}"
                        onUpdateListener?.onUpdateCheckFailed(errorMessage)
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "网络错误: ${e.message}"
                    onUpdateListener?.onUpdateCheckFailed(errorMessage)
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initRetrofit() {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    /**
     * 获取当前应用的版本名称
     */
    private fun getCurrentVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            e.printStackTrace()
            "1.0.0"
        }
    }

    /**
     * 比较版本号，判断新版本是否比当前版本新
     */
    private fun isNewVersionAvailable(newVersion: String, currentVersion: String): Boolean {
        return try {
            val newParts = newVersion.split(".").map { it.toInt() }
            val currentParts = currentVersion.split(".").map { it.toInt() }

            for (i in 0 until maxOf(newParts.size, currentParts.size)) {
                val newPart = newParts.getOrElse(i) { 0 }
                val currentPart = currentParts.getOrElse(i) { 0 }

                when {
                    newPart > currentPart -> return true
                    newPart < currentPart -> return false
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun showUpdateDialog(versionData: VersionData) {
        val activity = context as? Activity ?: return

        activity.runOnUiThread {
            val dialog = Dialog(activity)
            dialog.setContentView(R.layout.dialog_update)
            dialog.setCancelable(false)

            val tvVersion = dialog.findViewById<TextView>(R.id.tvVersion)
            val tvReleaseDate = dialog.findViewById<TextView>(R.id.tvReleaseDate)
            val tvDescription = dialog.findViewById<TextView>(R.id.tvDescription)
            val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
            val btnUpdate = dialog.findViewById<Button>(R.id.btnUpdate)

            tvVersion.text = "版本号：${versionData.version}"
            tvReleaseDate.text = "发布日期：${versionData.releaseDate}"
            tvDescription.text = versionData.description.replace("\\r\\n", "\n")

            downloadUrl = versionData.downloadUrl

            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            btnUpdate.setOnClickListener {
                onUpdateListener?.onDownloadStarted()
                downloadAndInstallApk()
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    private fun downloadAndInstallApk() {
        isDownloading = true

        // 显示初始下载通知 - 修正这里，使用notificationHelper
        notificationHelper.showInitialNotification(notificationBuilder)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val downloadDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "AppUpdates"
                )
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }

                val fileName = "智慧负载v${System.currentTimeMillis()}.apk"
                val outputFile = File(downloadDir, fileName)

                val client = OkHttpClient()
                val request = okhttp3.Request.Builder().url(downloadUrl).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    response.body?.let { body ->
                        val contentLength = body.contentLength()
                        var totalBytesRead = 0L

                        body.byteStream().use { inputStream ->
                            outputFile.outputStream().use { outputStream ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int

                                while (inputStream.read(buffer).also { bytesRead = it } != -1 && isDownloading) {
                                    outputStream.write(buffer, 0, bytesRead)
                                    totalBytesRead += bytesRead

                                    if (contentLength > 0) {
                                        val progress = (totalBytesRead * 100 / contentLength).toInt()
                                        // 确保进度不超过 100%
                                        val safeProgress = progress.coerceAtMost(99)
                                        // 更新通知栏进度
                                        withContext(Dispatchers.Main) {
                                            notificationHelper.updateDownloadProgress(safeProgress, notificationBuilder)
                                        }

                                        // 回调进度
                                        onUpdateListener?.onDownloadProgress(safeProgress)
                                    }
                                }
                            }
                        }

                        if (isDownloading) {
                            withContext(Dispatchers.Main) {
                                // 先更新到 100%
                                notificationHelper.updateDownloadProgress(100, notificationBuilder)
                                onUpdateListener?.onDownloadProgress(100)

                                delay(300) // 延迟 300ms 让用户看到 100%

                                notificationHelper.showDownloadCompleteNotification(outputFile)
                                onUpdateListener?.onDownloadCompleted(outputFile)
                                installApk(outputFile)
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        notificationHelper.showDownloadFailedNotification()
                        onUpdateListener?.onDownloadFailed("下载失败")
                        Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isDownloading) {
                        notificationHelper.showDownloadFailedNotification()
                        onUpdateListener?.onDownloadFailed("下载错误: ${e.message}")
                        Toast.makeText(context, "下载错误: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                isDownloading = false
            }
        }
    }

    private fun installApk(apkFile: File) {
        try {
            onUpdateListener?.onInstallStarted()

            val intent = Intent(Intent.ACTION_VIEW)
            val apkUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }

            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        } catch (e: Exception) {
            onUpdateListener?.onInstallFailed("安装失败: ${e.message}")
            Toast.makeText(context, "安装失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun cancelDownload() {
        isDownloading = false
        notificationHelper.cancelNotification()
    }
}