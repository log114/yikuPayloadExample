package com.example.yikupayloadexample;

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yiku.yikupayloadSDK.service.BaseMegaphoneService
import com.yiku.yikupayloadSDK.service.MegaphoneService
import android.text.InputType
import android.widget.Button
import com.example.yikupayloadexample.util.AppUpdateManager
import com.example.yikupayloadexample.util.VersionData
import com.yiku.yikupayloadSDK.service.AllInOneService
import com.yiku.yikupayloadSDK.service.FourInOne2Service
import java.io.File

var megaphoneService: BaseMegaphoneService? = null
var allInOneService: AllInOneService = AllInOneService()
var fourInOne2Service: FourInOne2Service = FourInOne2Service()
var preferences: SharedPreferences? = null


class MainActivity : AppCompatActivity() {
    private lateinit var appUpdateManager: AppUpdateManager
    private var mHandler: Handler? = null
    private val missingPermission: MutableList<String> = ArrayList()
    private lateinit var mStartConnectBtn: Button
    private var conn: ServiceConnection? = null
    private var intent: Intent? = null
    private var isOpenedPayloadWeight: Boolean = false

    override fun onStop() {
        super.onStop()
        Log.w(TAG, "main onStop....")
    }

    private fun goHome() {
//        Thread.sleep()
        val i = Intent(Intent.ACTION_MAIN)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        i.addCategory(Intent.CATEGORY_HOME)
        startActivity(i)

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "main onDestroy....")
        if (conn != null) {
            unbindService(conn!!)
            stopService(intent)
        }
        Process.killProcess(Process.myPid())
    }

    private fun initView() {
        mStartConnectBtn = findViewById(R.id.startConnectBtn)
        mStartConnectBtn.setOnClickListener {
            val powerManager = this.getSystemService(POWER_SERVICE) as PowerManager
            if(powerManager.isPowerSaveMode){
                showToast(R.string.turn_off_power_saving_mode)
                return@setOnClickListener;
            }
            if(isOpenedPayloadWeight) {
                goHome()
                return@setOnClickListener;
            }
            isOpenedPayloadWeight = true
            conn = object : ServiceConnection {
                override fun onServiceDisconnected(name: ComponentName) {}
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    if (name.className == PayloadWeight::class.java.name) {
                        val serviceBinder = binder as PayloadWeight.PayloadWeightBinder
                        serviceBinder.showWindow()
                        serviceBinder.openFloatingWindow()
                        megaphoneService = MegaphoneService()
                        val shoutHost = preferences?.getString("ShoutHost", "")
                        if(shoutHost != null && "" != shoutHost) {
                            megaphoneService?.setIp(shoutHost)
                        }
                        val allInOneHost = preferences?.getString("AllInOneHost", "")
                        if(allInOneHost != null && "" != allInOneHost) {
                            allInOneService.setIp(allInOneHost)
                        }
                        val fourInOne2Host = preferences?.getString("FourInOne2Host", "")
                        if(fourInOne2Host != null && "" != fourInOne2Host) {
                            fourInOne2Service.setIp(fourInOne2Host)
                        }
                    }
                }
            }

            intent = Intent(
                this,
                PayloadWeight::class.java
            )

            this.bindService(intent!!, conn!!, Context.BIND_AUTO_CREATE)
//            this.startActivity(intent)

            run {
                Thread.sleep(1000)
                goHome()
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = getSharedPreferences("myPreferences", MODE_PRIVATE);

        // When the compile and target version is higher than 22, please request the following permission at runtime to ensure the SDK works well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions()
        }
        setContentView(R.layout.main)
        // 显示版本信息
        val versionText = findViewById<TextView>(R.id.app_version)
        val manager: PackageManager = this.packageManager
        var name: String? = null
        try {
            val info: PackageInfo = manager.getPackageInfo(this.packageName, 0)
            name = info.versionName
            versionText.text = "V$name"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        initView()
        //跳转页面
        val setting_btn = findViewById<Button>(R.id.setting_btn)
        setting_btn.setOnClickListener {
            // 创建密码输入对话框
            val passwordDialog = AlertDialog.Builder(this).apply {
                setTitle(R.string.password_verification)
                setMessage(R.string.please_enter_the_access_password)

                // 创建密码输入框
                val passwordInput = EditText(this@MainActivity).apply {
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
                setView(passwordInput)

                // 确定按钮
                setPositiveButton(R.string.ok) { dialog, which ->
                    val inputPassword = passwordInput.text.toString()
                    val correctPassword = "8888"

                    if (inputPassword == correctPassword) {
                        // 密码正确，执行跳转
                        val intent = Intent(this@MainActivity, SettingActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    } else {
                        // 密码错误，提示并重新显示对话框
                        Toast.makeText(this@MainActivity, R.string.password_error, Toast.LENGTH_SHORT).show()
                        // 可以重新调用设置按钮的点击事件，或者直接再次显示对话框
                        setting_btn.performClick()
                    }
                }

                // 取消按钮
                setNegativeButton(R.string.cancel) { dialog, which ->
                    dialog.dismiss() // 关闭对话框，不执行任何操作
                }
            }.create()

            // 显示对话框
            passwordDialog.show()
        }

        // 初始化版本更新管理器
        appUpdateManager = AppUpdateManager.with(this)
            .setBaseUrl("https://downloads.zzykhk.com/")
            .setApiPath("payloadAppUpdate/api/version")
            .build()
            .setOnUpdateListener(object : AppUpdateManager.OnUpdateListener {
                override fun onUpdateAvailable(versionData: VersionData) {
                    // 可以在这里处理更新可用时的逻辑
                    Log.d("AppUpdate", "发现新版本: ${versionData.version}")
                }

                override fun onUpdateCheckFailed(error: String) {
                    Log.e("AppUpdate", "检查更新失败: $error")
                }

                override fun onDownloadStarted() {
                    // 显示下载进度条等
                    Toast.makeText(this@MainActivity, R.string.start_download, Toast.LENGTH_SHORT).show()
                }

                override fun onDownloadProgress(progress: Int) {
                    // 更新下载进度
                    Log.d("AppUpdate", "下载进度: $progress%")
                }

                override fun onDownloadCompleted(file: File) {
                    Log.d("AppUpdate", "下载完成: ${file.absolutePath}")
                }

                override fun onDownloadFailed(error: String) {
                    Log.e("AppUpdate", "下载失败: $error")
                }

                override fun onInstallStarted() {
                    Toast.makeText(this@MainActivity, R.string.start_installing, Toast.LENGTH_SHORT).show()
                }

                override fun onInstallFailed(error: String) {
                    Log.e("AppUpdate", "安装失败: $error")
                }
            })

        // 检查版本更新
        checkVersionUpdate()
    }
    private fun checkVersionUpdate() {
        appUpdateManager.checkVersionUpdate()
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun checkAndRequestPermissions() {
        // 添加版本特定的权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // Android 9+
            REQUIRED_PERMISSION_LIST.add(Manifest.permission.FOREGROUND_SERVICE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            REQUIRED_PERMISSION_LIST.add(Manifest.permission.POST_NOTIFICATIONS)

            // 添加媒体权限
            REQUIRED_PERMISSION_LIST.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            // Android 12 及以下使用旧存储权限
            REQUIRED_PERMISSION_LIST.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            REQUIRED_PERMISSION_LIST.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        // Check for permissions
        for (eachPermission in REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    eachPermission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missingPermission.add(eachPermission)
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
//            startSDKRegistration()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            showToast(R.string.permission_required)
            ActivityCompat.requestPermissions(
                this,
                missingPermission.toTypedArray(),
                REQUEST_PERMISSION_CODE
            )
        }
    }

    /**
     * Result of runtime permission request
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (i in grantResults.indices.reversed()) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i])
                }
            }
        }
        // If there is enough permission, we will start the registration
        Log.i("missingPermission:", missingPermission.toString())
        if (missingPermission.isEmpty()) {
//            startSDKRegistration()
        } else {
            showToast(R.string.lack_of_permissions)
        }
    }


    private fun notifyStatusChange() {
        mHandler!!.removeCallbacks(updateRunnable)
        mHandler!!.postDelayed(updateRunnable, 500)
    }

    private val updateRunnable = Runnable {
        val intent = Intent(FLAG_CONNECTION_CHANGE)
        sendBroadcast(intent)
    }

    private fun showToast(toastMsg: Int) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(
                applicationContext,
                toastMsg,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.name
        const val FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change"

        @RequiresApi(Build.VERSION_CODES.P)
        private var REQUIRED_PERMISSION_LIST = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.VIBRATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
//            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
        private const val REQUEST_PERMISSION_CODE = 12345
    }
}