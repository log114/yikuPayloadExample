package com.example.yikupayloadexample

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FirmwareUpdateActivity : AppCompatActivity() {
    private val TAG = "FirmwareUpdateActivity"
    private var deviceName = ""
    private lateinit var deviceNameText: TextView
    private lateinit var currentVersionText: TextView
    private lateinit var fileNameText: TextView
    private lateinit var newVersionText: TextView
    private lateinit var selectFileBtn: Button
    private lateinit var updateBtn: Button
    private lateinit var restartBtn: Button

    @SuppressLint("SourceLockedOrientationPortrait")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.firmware_update)

        deviceName = intent.getStringExtra("deviceName") ?: "" // 获取设备名称
        initViews()
    }

    private fun initViews() {
        deviceNameText = findViewById(R.id.device_name)
        currentVersionText = findViewById(R.id.currentVersion)
        fileNameText = findViewById(R.id.fileName)
        newVersionText = findViewById(R.id.newVersion)
        selectFileBtn = findViewById(R.id.selectFileBtn)
        updateBtn = findViewById(R.id.updateBtn)
        restartBtn = findViewById(R.id.restartBtn)

        deviceNameText.text = when(deviceName) {
            "allInOne" -> resources.getString(R.string.all_in_one)
            else -> ""
        }

        // 选择文件
        selectFileBtn.setOnClickListener {

        }
        // 开始升级
        updateBtn.setOnClickListener {

        }
        // 重启设备
        restartBtn.setOnClickListener {

        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun showToast(toastMsg: Int) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(
                MApplication.applicationContext,
                toastMsg,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showToast(msg: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(
                MApplication.applicationContext, msg, Toast.LENGTH_SHORT
            ).show()
        }
    }
}