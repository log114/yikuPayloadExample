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

class AppUpdateManager private constructor(
    private val context: Context,
    private val baseUrl: String,
    private val apiPath: String
) {

    private lateinit var apiService: ApiService
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

    fun checkVersionUpdate() {
        initRetrofit()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.checkVersion()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val versionResponse = response.body()
                        if (versionResponse?.result == "success") {
                            versionResponse.data?.let { versionData ->
                                onUpdateListener?.onUpdateAvailable(versionData)
                                showUpdateDialog(versionData)
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

                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                    outputStream.write(buffer, 0, bytesRead)
                                    totalBytesRead += bytesRead

                                    // 更新下载进度
                                    if (contentLength > 0) {
                                        val progress = (totalBytesRead * 100 / contentLength).toInt()
                                        onUpdateListener?.onDownloadProgress(progress)
                                    }
                                }
                            }
                        }

                        withContext(Dispatchers.Main) {
                            onUpdateListener?.onDownloadCompleted(outputFile)
                            installApk(outputFile)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onUpdateListener?.onDownloadFailed("下载失败")
                        Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onUpdateListener?.onDownloadFailed("下载错误: ${e.message}")
                    Toast.makeText(context, "下载错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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
}