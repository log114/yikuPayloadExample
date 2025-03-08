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
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yiku.yikupayload.service.BaseMegaphoneService
import com.yiku.yikupayload.service.MegaphoneService

var megaphoneService: BaseMegaphoneService? = null
var preferences: SharedPreferences? = null


class MainActivity : AppCompatActivity() {
    private var mHandler: Handler? = null


    private val missingPermission: MutableList<String> = ArrayList()

    private lateinit var mOpenH16View: ImageView

    private var conn: ServiceConnection? = null
    private var intent: Intent? = null
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
        mOpenH16View = findViewById(R.id.open_h16_view)
        mOpenH16View.setOnClickListener {
            val powerManager = this.getSystemService(POWER_SERVICE) as PowerManager
//            Log.i(TAG, "省电模式：${powerManager.isPowerSaveMode}")
//            Log.i(TAG, "省电模式，机型：${Build.MANUFACTURER}")
            if(powerManager.isPowerSaveMode){
                showToast(R.string.turn_off_power_saving_mode)
                return@setOnClickListener;
            }
            conn = object : ServiceConnection {
                override fun onServiceDisconnected(name: ComponentName) {}
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    if (name.className == PayloadWeight::class.java.name) {
                        val serviceBinder = binder as PayloadWeight.PayloadWeightBinder
                        serviceBinder.showWindow()
                        serviceBinder.openFloatingWindow()
                        megaphoneService = MegaphoneService()
                        val host = preferences?.getString("ShoutHost", "")
                        if(host != null && "" != host) {
                            megaphoneService?.setIp(host)
                        }
                    }
                }
            }

            intent = Intent(
                this,
                PayloadWeight::class.java
            )

            this.bindService(intent, conn!!, Context.BIND_AUTO_CREATE)
//            this.startActivity(intent)

            run {
                Thread.sleep(1000)
                goHome()
            }

        }
    }

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
        val setting_btn = findViewById<ImageView>(R.id.setting_btn)
        setting_btn.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private fun checkAndRequestPermissions() {
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

        private val REQUIRED_PERMISSION_LIST = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
//            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
        )
        private const val REQUEST_PERMISSION_CODE = 12345
    }
}