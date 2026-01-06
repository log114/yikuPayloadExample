package com.example.yikupayloadexample

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yiku.yikupayloadSDK.protocol.WATERGUN_STATE_RECEIVE
import com.yiku.yikupayloadSDK.service.UpgradeService
import com.yiku.yikupayloadSDK.util.AllInOneHost
import com.yiku.yikupayloadSDK.util.GetFilePathFromUri
import com.yiku.yikupayloadSDK.util.MsgCallback
import com.yiku.yikupayloadSDK.util.bytesToHex
import java.io.File
import java.util.Date
import kotlin.concurrent.thread

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
    private lateinit var progressBarUpdate: ProgressBar
    private lateinit var updateText: TextView
    private lateinit var firmwareFile: File
    private val upgradeService: UpgradeService = UpgradeService()
    private var ip = ""
    private var port = 8519
    private var isDestroying = false
    private var hadUpgrading = false // 是否有正在升级的内容
    private var result_resetUpgradeInfo = -1
    private var result_startUpgrade = -1
    private var errorCount = 0

    @SuppressLint("SourceLockedOrientationPortrait")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.firmware_update)

        deviceName = intent.getStringExtra("deviceName") ?: "" // 获取设备名称
        initViews()
        connectDevice()
        upgradeService.registMsgCallback(object : MsgCallback {
            override fun getId(): String {
                return "UpgradeServiceCallback"
            }

            override fun onMsg(msg: ByteArray) {
                Log.i(TAG, "收到消息，msg= ${bytesToHex(msg)}")
                // 先确定是更新程序反馈的信息才处理
                if(msg[0] == 0xFF.toByte() && msg[1] == 0xAA.toByte() && msg[2] == 0x55.toByte() && msg[3] == 0xFF.toByte()) {
                    val handler = Handler(Looper.getMainLooper())
                    val dataLength = littleEndianToInt(msg, 4) // 这个长度包括了4位地址位+1位命令类型（0xE0）+1位指令码+若干位具体的数据
                    when(msg[11].toInt()) {
                        0x01 -> { // 设备信息
                            Log.d(TAG, "设备信息")
                            handler.post {
                                currentVersionText.text = byteArrayToVersionString(msg.copyOfRange(12, 16))
                            }
                            val upgradingVersion = byteArrayToVersionString(msg.copyOfRange(16, 20))
                            hadUpgrading = upgradingVersion != "0.0.0.0"
                        }
                        0x02 -> { // 重置升级结果
                            Log.d(TAG, "重置升级结果")
                            result_resetUpgradeInfo = msg[12].toInt()
                        }
                        0x03 -> { // 下发升级请求结果
                            Log.d(TAG, "下发升级请求结果")
                            result_startUpgrade = msg[12].toInt()
                        }
                        0x04 -> { // 升级数据包发送结果
                            Log.d(TAG, "升级数据包发送结果")

                        }
                        0x05 -> { // 升级包校验结果
                            Log.d(TAG, "升级包校验结果")

                        }
                        0x06 -> { // 设备重启
                            Log.d(TAG, "设备重启")

                        }
                        else -> {
                            Log.e(TAG, "反馈消息异常，msg= ${bytesToHex(msg)}")
                        }
                    }
                }

            }

        })
    }

    private fun initViews() {
        deviceNameText = findViewById(R.id.device_name)
        currentVersionText = findViewById(R.id.currentVersion)
        fileNameText = findViewById(R.id.fileName)
        newVersionText = findViewById(R.id.newVersion)
        selectFileBtn = findViewById(R.id.selectFileBtn)
        updateBtn = findViewById(R.id.updateBtn)
        restartBtn = findViewById(R.id.restartBtn)
        progressBarUpdate = findViewById(R.id.progressBar_update)
        updateText = findViewById(R.id.updateText)

        when(deviceName) {
            "allInOne" -> {
                deviceNameText.setText(R.string.all_in_one)
                ip = preferences?.getString("AllInOneHost", null) ?: AllInOneHost
                port = 8529
            }
            else -> {

            }
        }

        // 选择文件
        selectFileBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            //任意类型文件
            intent.type = "application/octet-stream"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            ActivityCompat.startActivityForResult(this, intent, 100, null)
        }
        // 开始升级
        updateBtn.setOnClickListener {
            if(!upgradeService.getIsConnected()) {
                showToast(R.string.not_connected)
                return@setOnClickListener
            }
            if(!firmwareFile.isFile || firmwareFile.length().toInt() == 0) {
                showToast(R.string.please_select_file)
                return@setOnClickListener
            }
            if(newVersionText.text == "" || newVersionText.text.split(".").size != 4) {
                showToast(R.string.version_error)
                return@setOnClickListener
            }
            updateBtn.setText(R.string.upgrading)
            componentEnabled(false)
            // 如果有上次没升级完的内容，先重置
            if(hadUpgrading) {
                result_resetUpgradeInfo = -1
                upgradeService.resetDeviceInfo()
                updateText.setText(R.string.resetting_upgrade_info)
                updateText.setTextColor(ContextCompat.getColor(this, R.color.white))
            }
            thread {
                val handler = Handler(Looper.getMainLooper())
                // 等待3秒清理上次升级的内容
                if(hadUpgrading) {
                    Thread.sleep(3000)
                    errorCount = 0
                    // 最多重试3次
                    while(result_resetUpgradeInfo != 0 && errorCount < 3) {
                        upgradeService.resetDeviceInfo()
                        errorCount ++
                        Thread.sleep(3000)
                    }
                    if(result_resetUpgradeInfo == -1) {
                        handler.post {
                            updateText.setText(R.string.device_not_responding)
                            updateText.setTextColor(ContextCompat.getColor(this, R.color.red))
                            updateBtn.setText(R.string.start_upgrade)
                            componentEnabled(true)
                        }
                        return@thread
                    }
                    if(result_resetUpgradeInfo > 0) {
                        handler.post {
                            updateText.setText(R.string.upgrade_info_reset_failed)
                            updateText.setTextColor(ContextCompat.getColor(this, R.color.red))
                            updateBtn.setText(R.string.start_upgrade)
                            componentEnabled(true)
                        }
                        return@thread
                    }
                }
                // 发送升级请求
                handler.post {
                    updateText.setText(R.string.requesting_upgrade)
                }
                result_startUpgrade = -1
                upgradeService.startUpgrade(newVersionText.text.toString(),
                    firmwareFile.length().toInt()
                )
                // 等待处理升级请求结果
                errorCount = 0
                while(result_startUpgrade == -1) {
                    errorCount ++
                    // 超过5秒没有收到结果，提示设备无响应
                    if(errorCount > 5) {
                        handler.post {
                            updateText.setText(R.string.device_not_responding)
                            updateText.setTextColor(ContextCompat.getColor(this, R.color.red))
                            updateBtn.setText(R.string.start_upgrade)
                            componentEnabled(true)
                        }
                        return@thread
                    }
                    Thread.sleep(1000)
                }
                if(result_startUpgrade > 0) {
                    val errorMsg =  when(result_startUpgrade) {
                        0x01 -> {
                            this.resources.getString(R.string.no_need_upgrade)
                        }
                        0x02 -> {
                            this.resources.getString(R.string.inconsistency_in_size)
                        }
                        0x03 -> {
                            this.resources.getString(R.string.inconsistent_firmware_versions)
                        }
                        else -> {
                            this.resources.getString(R.string.upgrade_request_failed)
                        }
                    }
                    handler.post {
                        updateText.text = errorMsg
                        updateText.setTextColor(ContextCompat.getColor(this, R.color.red))
                        updateBtn.setText(R.string.start_upgrade)
                        componentEnabled(true)
                    }
                    return@thread
                }
                // 发送升级数据包

            }
        }
        // 重启设备
        restartBtn.setOnClickListener {

        }
    }

    // 组件isEn
    fun componentEnabled(isEnabled: Boolean) {
        selectFileBtn.isEnabled = isEnabled
        newVersionText.isEnabled = isEnabled
        updateBtn.isEnabled = isEnabled
    }

    // 文件选择后的处理
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i(
            TAG,
            "onActivityResult... requestCode:${requestCode}, resultCode:${resultCode}, data:${data} "
        )
        super.onActivityResult(requestCode, resultCode, data)

//
        if (data == null || requestCode != 100) {
            // 用户未选择任何文件，直接返回
            Log.i(TAG, "data:${data}, req:${requestCode}")
            return
        }
        val path = GetFilePathFromUri.getFileAbsolutePath(this, data.data)
        Log.i(TAG, "name: ${path.split("/").last()}")
        Log.i(TAG, "path:$path")
        firmwareFile = File(path)
        if (!firmwareFile.exists()) {
            val _path = path.replace("emulated/0", "external_sd")
            Log.i(TAG, "path:${_path}")
            firmwareFile = File(_path)
        }
        if (!firmwareFile.exists()) {
            showToast(R.string.failed_to_obtain_file)
            return
        }
        Log.e(TAG, "Name: ${firmwareFile.name}")
        val fileName = firmwareFile.name.split(":").last()
        fileNameText.text = fileName
        if(fileName.contains("v", ignoreCase = true)) {
            val firmwareVersion = extractVersionWithRegex(fileName)
            val parts = firmwareVersion?.split(".")
            if(parts?.size != 4) {
                Log.d(TAG, "文件名格式不正确，无法自动获取版本号")
            }
            else {
                newVersionText.text = firmwareVersion
            }
        }
    }

    // 从文件名中提取版本号
    fun extractVersionWithRegex(filename: String): String? {
        val pattern = """(?i)V(\d+(?:\.\d+)*)\.bin""".toRegex()
        val result = pattern.find(filename)
        return result?.groupValues?.get(1)
    }

    override fun onDestroy() {
        super.onDestroy()
        isDestroying = true
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

    private fun connectDevice() {
        if(ip == "") {
            return;
        }
        thread {
            while (!upgradeService.getIsConnected()) {
                if(isDestroying) {
                    return@thread;
                }
                upgradeService.connect(ip, port)
                Thread.sleep(1000)
            }
            // 连接成功，获取设备信息
            upgradeService.getDeviceInfo()
        }
    }

    /**
     * 小端序转换：低位在前 → Int
     */
    fun littleEndianToInt(byteArray: ByteArray, startIndex: Int = 0): Int {
        require(byteArray.size >= startIndex + 2) {
            "字节数组从索引${startIndex}开始长度不足2字节"
        }

        val lowByte = byteArray[startIndex].toInt() and 0xFF
        val highByte = byteArray[startIndex + 1].toInt() and 0xFF

        return (highByte shl 8) or lowByte
    }
    /**
     * Int → 小端序ByteArray(2)
     */
    fun intToLittleEndianByteArray(value: Int): ByteArray {
        require(value >= -32768 && value <= 65535) {
            "数值超出2字节表示范围: $value"
        }

        return byteArrayOf(
            (value and 0xFF).toByte(),        // 低字节
            ((value ushr 8) and 0xFF).toByte()  // 高字节
        )
    }

    /**
     * 将ByteArray(4)转换为版本号字符串
     * 例如：01 00 00 00 → "1.0.0.0"
     * @param byteArray 4字节的字节数组
     * @return 版本号字符串
     */
    fun byteArrayToVersionString(byteArray: ByteArray): String {
        require(byteArray.size == 4) { "字节数组长度必须为4" }

        return byteArray.joinToString(".") { byte ->
            // 将字节转换为无符号整数 (0-255)
            (byte.toInt() and 0xFF).toString()
        }
    }

    /**
     * 将版本号字符串转换为ByteArray(4)
     * 例如："1.0.0.0" → 01 00 00 00
     * @param version 版本号字符串，格式为 X.X.X.X
     * @return 4字节的字节数组
     */
    fun versionStringToByteArray(version: String): ByteArray {
        val parts = version.split(".")
        require(parts.size == 4) { "版本号格式必须为 X.X.X.X" }

        return ByteArray(4) { index ->
            val part = parts.getOrNull(index)?.toIntOrNull()
                ?: throw IllegalArgumentException("版本号部分必须为数字: ${parts.getOrNull(index)}")

            if (part < 0 || part > 255) {
                throw IllegalArgumentException("版本号各部分必须在 0-255 范围内: $part")
            }

            part.toByte()
        }
    }
}