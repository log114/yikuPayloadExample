package com.example.yikupayloadexample

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yiku.yikupayloadSDK.service.UpgradeService
import com.yiku.yikupayloadSDK.util.AllInOneHost
import com.yiku.yikupayloadSDK.util.GetFilePathFromUri
import com.yiku.yikupayloadSDK.util.MsgCallback
import com.yiku.yikupayloadSDK.util.bytesToHex
import java.io.File
import java.io.FileInputStream
import kotlin.concurrent.thread

class FirmwareUpdateActivity : AppCompatActivity() {
//    private val TAG = "FirmwareUpdateActivity"
//    private var deviceName = ""
//    private lateinit var deviceNameText: TextView
//    private lateinit var currentVersionText: TextView
//    private lateinit var fileNameText: TextView
//    private lateinit var newVersionText: TextView
//    private lateinit var selectFileBtn: Button
//    private lateinit var updateBtn: Button
//    private lateinit var restartBtn: Button
//    private lateinit var progressBarUpdate: ProgressBar
//    private lateinit var updateText: TextView
//    private lateinit var firmwareFile: File
//    private val upgradeService: UpgradeService = UpgradeService()
//    private var ip = ""
//    private var port = 8519
//    private var isDestroying = false
//    private var hadUpgrading = false // 是否有正在升级的内容
//    private var result_resetUpgradeInfo = -1
//    private var result_startUpgrade = -1
//    private var result_transmission = -1
//    private var totalPackages = 0 // 上传的总包数
//    private var uploadedPackages = 0 // 已上传的包数
//    private var result_verify = -1
//    private var errorCount = 0
//    private var maxPackageSize = 1024
//    private var retryCount_jumpPacket = 0; // 跳包重试次数
//
//    @SuppressLint("SourceLockedOrientationPortrait")
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.firmware_update)
//
//        deviceName = intent.getStringExtra("deviceName") ?: "" // 获取设备名称
//        initViews()
//        connectDevice()
//        upgradeService.registMsgCallback(object : MsgCallback {
//            override fun getId(): String {
//                return "UpgradeServiceCallback"
//            }
//
//            override fun onMsg(msg: ByteArray) {
//                Log.i(TAG, "收到消息，msg= ${bytesToHex(msg)}")
//                // 先确定是更新程序反馈的信息才处理
//                if(msg[0] == 0xFF.toByte() && msg[1] == 0xAA.toByte() && msg[2] == 0x55.toByte() && msg[3] == 0xFF.toByte()) {
//                    val handler = Handler(Looper.getMainLooper())
//                    val dataLength = littleEndianToInt(msg, 4) // 这个长度包括了4位地址位+1位命令类型（0xE0）+1位指令码+若干位具体的数据
//                    when(msg[11].toInt()) {
//                        0x01 -> { // 设备信息
//                            Log.d(TAG, "设备信息")
//                            handler.post {
//                                currentVersionText.text = byteArrayToVersionString(msg.copyOfRange(12, 16))
//                            }
//                            val upgradingVersion = byteArrayToVersionString(msg.copyOfRange(16, 20))
//                            hadUpgrading = upgradingVersion != "0.0.0.0"
//                        }
//                        0x02 -> { // 重置升级结果
//                            Log.d(TAG, "重置升级结果")
//                            result_resetUpgradeInfo = msg[12].toInt()
//                        }
//                        0x03 -> { // 下发升级请求结果
//                            Log.d(TAG, "下发升级请求结果")
//                            result_startUpgrade = msg[12].toInt()
//                        }
//                        0x04 -> { // 升级数据包发送结果
//                            Log.d(TAG, "升级数据包发送结果")
//                            result_transmission = msg[12].toInt()
//                            totalPackages = littleEndianToInt(msg, 13)
//                            uploadedPackages = littleEndianToInt(msg, 15)
//                        }
//                        0x05 -> { // 升级包校验结果
//                            Log.d(TAG, "升级包校验结果")
//                            result_verify = msg[12].toInt()
//                        }
//                        0x06 -> { // 设备重启
//                            Log.d(TAG, "设备重启")
//                            if (msg[12].toInt() == 0x00) {
//                                showToast(R.string.device_will_restart)
//                                thread {
//                                    Thread.sleep(5000)
//                                    upgradeService.disConnect()
//                                    Thread.sleep(2000)
//                                    connectDevice()
//                                }
//                            }
//                            else {
//                                showToast(R.string.please_restart_manually)
//                            }
//                            handler.post {
//                                restartBtn.isEnabled = true
//                            }
//                        }
//                        else -> {
//                            Log.e(TAG, "反馈消息异常，msg= ${bytesToHex(msg)}")
//                        }
//                    }
//                }
//
//            }
//
//        })
//    }
//
//    private fun initViews() {
//        deviceNameText = findViewById(R.id.device_name)
//        currentVersionText = findViewById(R.id.currentVersion)
//        fileNameText = findViewById(R.id.fileName)
//        newVersionText = findViewById(R.id.newVersion)
//        selectFileBtn = findViewById(R.id.selectFileBtn)
//        updateBtn = findViewById(R.id.updateBtn)
//        restartBtn = findViewById(R.id.restartBtn)
//        progressBarUpdate = findViewById(R.id.progressBar_update)
//        updateText = findViewById(R.id.updateText)
//
//        when(deviceName) {
//            "allInOne" -> {
//                deviceNameText.setText(R.string.all_in_one)
//                ip = preferences?.getString("AllInOneHost", null) ?: AllInOneHost
//                port = 8529
//            }
//            else -> {
//
//            }
//        }
//
//        // 选择文件
//        selectFileBtn.setOnClickListener {
//            val intent = Intent(Intent.ACTION_GET_CONTENT)
//            //任意类型文件
//            intent.type = "application/octet-stream"
//            intent.addCategory(Intent.CATEGORY_OPENABLE)
//            ActivityCompat.startActivityForResult(this, intent, 100, null)
//        }
//        // 开始升级
//        updateBtn.setOnClickListener {
//            if(!upgradeService.getIsConnected()) {
//                showToast(R.string.not_connected)
//                return@setOnClickListener
//            }
//            if(currentVersionText.text == "") {
//                showToast(R.string.unknown_current_version)
//                return@setOnClickListener
//            }
//            if(!::firmwareFile.isInitialized || firmwareFile.length().toInt() == 0) {
//                showToast(R.string.please_select_file)
//                return@setOnClickListener
//            }
//            if(newVersionText.text == "" || newVersionText.text.split(".").size != 4) {
//                showToast(R.string.version_error)
//                return@setOnClickListener
//            }
//            updateBtn.setText(R.string.upgrading)
//            componentEnabled(false)
//            // 如果有上次没升级完的内容，先重置
//            if(hadUpgrading) {
//                result_resetUpgradeInfo = -1
//                upgradeService.resetDeviceInfo()
//                updateText.setText(R.string.resetting_upgrade_info)
//                updateText.setTextColor(ContextCompat.getColor(this, R.color.white))
//            }
//            thread {
//                val handler = Handler(Looper.getMainLooper())
//                // 等待3秒清理上次升级的内容
//                if(hadUpgrading) {
//                    Thread.sleep(3000)
//                    errorCount = 0
//                    // 最多重试3次
//                    while(result_resetUpgradeInfo != 0 && errorCount < 3) {
//                        upgradeService.resetDeviceInfo()
//                        errorCount ++
//                        Thread.sleep(3000)
//                    }
//                    if(result_resetUpgradeInfo == -1) {
//                        handler.post {
//                            updateText.setText(R.string.device_not_responding)
//                            updateText.setTextColor(ContextCompat.getColor(this, R.color.red))
//                            updateBtn.setText(R.string.start_upgrade)
//                            componentEnabled(true)
//                        }
//                        return@thread
//                    }
//                    if(result_resetUpgradeInfo > 0) {
//                        handler.post {
//                            updateText.setText(R.string.upgrade_info_reset_failed)
//                            updateText.setTextColor(ContextCompat.getColor(this, R.color.red))
//                            updateBtn.setText(R.string.start_upgrade)
//                            componentEnabled(true)
//                        }
//                        return@thread
//                    }
//                }
//                // 发送升级请求
//                handler.post {
//                    updateText.setText(R.string.requesting_upgrade)
//                }
//                result_startUpgrade = -1
//                upgradeService.startUpgrade(newVersionText.text.toString(),
//                    firmwareFile.length().toInt()
//                )
//                // 等待处理升级请求结果
//                errorCount = 0
//                while(result_startUpgrade == -1) {
//                    errorCount ++
//                    // 超过5秒没有收到结果，提示设备无响应
//                    if(errorCount > 5) {
//                        handler.post {
//                            updateText.setText(R.string.device_not_responding)
//                            updateText.setTextColor(ContextCompat.getColor(this, R.color.red))
//                            updateBtn.setText(R.string.start_upgrade)
//                            componentEnabled(true)
//                        }
//                        return@thread
//                    }
//                    Thread.sleep(1000)
//                }
//                if(result_startUpgrade > 0) {
//                    val errorMsg =  when(result_startUpgrade) {
//                        0x01 -> {
//                            this.resources.getString(R.string.no_need_upgrade)
//                        }
//                        0x02 -> {
//                            this.resources.getString(R.string.inconsistency_in_size)
//                        }
//                        0x03 -> {
//                            this.resources.getString(R.string.inconsistent_firmware_versions)
//                        }
//                        else -> {
//                            this.resources.getString(R.string.upgrade_request_failed)
//                        }
//                    }
//                    handler.post {
//                        updateText.text = errorMsg
//                        updateText.setTextColor(ContextCompat.getColor(this, R.color.red))
//                        updateBtn.setText(R.string.start_upgrade)
//                        componentEnabled(true)
//                    }
//                    return@thread
//                }
//                // 发送升级数据包，支持断点续传
//                retryCount_jumpPacket = 0
//                if(!uploadFirmwareFile()){
//                    return@thread
//                }
//                // 固件包传输完成，开始校验
//                handler.post {
//                    updateText.setText(R.string.verifying_firmware_file)
//                }
//                result_verify = -1
//                upgradeService.packageVerification()
//                // 等待处理校验结果
//                errorCount = 0
//                var retryCount = 0
//                while(result_verify == -1) {
//                    errorCount ++
//                    // 超过5秒没有收到结果，提示设备无响应
//                    if(errorCount > 5) {// 最多重试3次
//                        if(retryCount < 3) {
//                            retryCount ++
//                            errorCount = 0
//                            upgradeService.packageVerification()
//                        }
//                        else {
//                            handler.post {
//                                updateText.setText(R.string.device_not_responding)
//                                updateText.setTextColor(ContextCompat.getColor(this, R.color.red))
//                                updateBtn.setText(R.string.start_upgrade)
//                                componentEnabled(true)
//                            }
//                            return@thread
//                        }
//                    }
//                    Thread.sleep(1000)
//                }
//                if(result_verify > 0) {
//                    val errorMsg =  when(result_verify) {
//                        0x01 -> {
//                            this.resources.getString(R.string.firmware_is_incomplete)
//                        }
//                        0x02 -> {
//                            this.resources.getString(R.string.firmware_verification_failed)
//                        }
//                        else -> {
//                            this.resources.getString(R.string.firmware_verification_failed)
//                        }
//                    }
//                    handler.post {
//                        updateText.text = errorMsg
//                        updateText.setTextColor(ContextCompat.getColor(this, R.color.red))
//                        updateBtn.setText(R.string.start_upgrade)
//                        componentEnabled(true)
//                    }
//                    return@thread
//                }
//                // 升级成功，重启后生效
//                handler.post {
//                    updateText.setText(R.string.upgrade_successful)
//                    updateBtn.setText(R.string.start_upgrade)
//                    componentEnabled(true)
//
//                }
//            }
//        }
//        // 重启设备
//        restartBtn.setOnClickListener {
//            restartBtn.isEnabled = false
//            upgradeService.restartDevice()
//        }
//    }
//
//    // 发送升级数据包，支持断点续传
//    fun uploadFirmwareFile(): Boolean {
//        val handler = Handler(Looper.getMainLooper())
//        // 发送升级数据包
//        val packagesNum = (firmwareFile.length()/maxPackageSize + 1).toInt()
//        handler.post {
//            updateText.setText(R.string.transmitted_firmware)
//            progressBarUpdate.max = packagesNum
//            progressBarUpdate.progress = 0
//            progressBarUpdate.visibility = View.VISIBLE
//        }
//        Log.d(TAG, "包总数：${packagesNum}, 总大小：${firmwareFile.length()}")
//        val buffer = ByteArray(maxPackageSize)
//        var packageIndex = 1
//        FileInputStream(firmwareFile).use { inputStream ->
//            // 跳过已传输的字节
//            if (uploadedPackages > 0) {
//                val skipBytes = uploadedPackages.toLong() * maxPackageSize
//                val actualSkipped = inputStream.skip(skipBytes)
//                Log.d(TAG, "尝试跳过 $skipBytes 字节，实际跳过 $actualSkipped 字节")
//
//                if (actualSkipped < skipBytes) {
//                    // 如果跳过的字节数比预期少，认为异常
//                    handler.post {
//                        updateText.setText(R.string.resume_failure)
//                        updateText.setTextColor(ContextCompat.getColor(this, R.color.red))
//                        updateBtn.setText(R.string.start_upgrade)
//                        componentEnabled(true)
//                    }
//                    return false
//                }
//                packageIndex = uploadedPackages + 1
//            }
//            var bytesRead: Int
//            // 循环读取文件，直到读完所有内容
//            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
//                // 精确复制实际读取的字节，避免末尾包包含旧数据
//                val packageData = buffer.copyOf(bytesRead)
//                result_transmission = -1 // 发送命令前，先重置状态
//                upgradeService.transmissionPackage(packagesNum, packageIndex, packageData)
//                errorCount = 0
//                var retryCount = 0
//                while (result_transmission == -1) {
//                    Thread.sleep(200)
//                    errorCount ++
//                    // 每次最多等待3秒
//                    if(errorCount > 15) {
//                        // 最多重试3次
//                        if(retryCount < 3) {
//                            retryCount ++
//                            errorCount = 0
//                            upgradeService.transmissionPackage(packagesNum, packageIndex, packageData)
//                        }
//                        else {
//                            handler.post {
//                                updateText.setText(R.string.device_not_responding)
//                                updateText.setTextColor(
//                                    ContextCompat.getColor(
//                                        this,
//                                        R.color.red
//                                    )
//                                )
//                                updateBtn.setText(R.string.start_upgrade)
//                                componentEnabled(true)
//                            }
//                            return false
//                        }
//                    }
//                }
//                when(result_transmission) {
//                    0,2 -> { // 成功或者重复，都认为这个包已经上传成功，继续下一包
//                        handler.post {
//                            progressBarUpdate.progress = packageIndex
//                        }
//                        packageIndex ++
//                        continue
//                    }
//                    1 -> { // 总包数错误，直接退出
//                        handler.post {
//                            updateText.setText(R.string.packages_number_error)
//                            updateText.setTextColor(
//                                ContextCompat.getColor(
//                                    this,
//                                    R.color.red
//                                )
//                            )
//                            updateBtn.setText(R.string.start_upgrade)
//                            componentEnabled(true)
//                        }
//                        return false
//                    }
//                    3 -> { // 跳包，尝试续传，最多3次
//                        retryCount_jumpPacket ++
//                        if(retryCount_jumpPacket > 3) {
//                            handler.post {
//                                updateText.text = "${resources.getString(R.string.jumping_bag)}，（${retryCount_jumpPacket}/3）"
//                                updateText.setTextColor(
//                                    ContextCompat.getColor(
//                                        this,
//                                        R.color.red
//                                    )
//                                )
//                                updateBtn.setText(R.string.start_upgrade)
//                                componentEnabled(true)
//                            }
//                            return false
//                        }
//                        else {
//                            handler.post {
//                                updateText.text = "${resources.getString(R.string.jumping_bag)}，（${retryCount_jumpPacket}/3）"
//                            }
//                            return uploadFirmwareFile()
//                        }
//                    }
//                }
//            }
//        }
//        return true
//    }
//
//    // 设置组件是否可用
//    fun componentEnabled(isEnabled: Boolean) {
//        selectFileBtn.isEnabled = isEnabled
//        newVersionText.isEnabled = isEnabled
//        updateBtn.isEnabled = isEnabled
//        restartBtn.isEnabled = isEnabled
//        if(isEnabled) {
//            progressBarUpdate.visibility = View.INVISIBLE
//        }
//    }
//
//    // 文件选择后的处理
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        Log.i(
//            TAG,
//            "onActivityResult... requestCode:${requestCode}, resultCode:${resultCode}, data:${data} "
//        )
//        super.onActivityResult(requestCode, resultCode, data)
//
////
//        if (data == null || requestCode != 100) {
//            // 用户未选择任何文件，直接返回
//            Log.i(TAG, "data:${data}, req:${requestCode}")
//            return
//        }
//        val path = GetFilePathFromUri.getFileAbsolutePath(this, data.data)
//        Log.i(TAG, "name: ${path.split("/").last()}")
//        Log.i(TAG, "path:$path")
//        firmwareFile = File(path)
//        if (!firmwareFile.exists()) {
//            val _path = path.replace("emulated/0", "external_sd")
//            Log.i(TAG, "path:${_path}")
//            firmwareFile = File(_path)
//        }
//        if (!firmwareFile.exists()) {
//            showToast(R.string.failed_to_obtain_file)
//            return
//        }
//        Log.e(TAG, "Name: ${firmwareFile.name}")
//        val fileName = firmwareFile.name.split(":").last()
//        fileNameText.text = fileName
//        if(fileName.contains("v", ignoreCase = true)) {
//            val firmwareVersion = extractVersionWithRegex(fileName)
//            val parts = firmwareVersion?.split(".")
//            if(parts?.size != 4) {
//                Log.d(TAG, "文件名格式不正确，无法自动获取版本号")
//            }
//            else {
//                newVersionText.text = firmwareVersion
//            }
//        }
//    }
//
//    // 从文件名中提取版本号
//    fun extractVersionWithRegex(filename: String): String? {
//        val pattern = """(?i)V(\d+(?:\.\d+)*)\.bin""".toRegex()
//        val result = pattern.find(filename)
//        return result?.groupValues?.get(1)
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        isDestroying = true
//    }
//
//    private fun showToast(toastMsg: Int) {
//        val handler = Handler(Looper.getMainLooper())
//        handler.post {
//            Toast.makeText(
//                MApplication.applicationContext,
//                toastMsg,
//                Toast.LENGTH_SHORT
//            ).show()
//        }
//    }
//
//    private fun connectDevice() {
//        if(ip == "") {
//            return;
//        }
//        thread {
//            while (!upgradeService.getIsConnected()) {
//                if(isDestroying) {
//                    return@thread;
//                }
//                upgradeService.connect(ip, port)
//                Thread.sleep(1000)
//            }
//            // 连接成功，获取设备信息
//            upgradeService.getDeviceInfo()
//        }
//    }
//
//    /**
//     * 小端序转换：低位在前 → Int
//     */
//    fun littleEndianToInt(byteArray: ByteArray, startIndex: Int = 0): Int {
//        require(byteArray.size >= startIndex + 2) {
//            "字节数组从索引${startIndex}开始长度不足2字节"
//        }
//
//        val lowByte = byteArray[startIndex].toInt() and 0xFF
//        val highByte = byteArray[startIndex + 1].toInt() and 0xFF
//
//        return (highByte shl 8) or lowByte
//    }
//
//    /**
//     * 将ByteArray(4)转换为版本号字符串
//     * 例如：01 00 00 00 → "1.0.0.0"
//     * @param byteArray 4字节的字节数组
//     * @return 版本号字符串
//     */
//    fun byteArrayToVersionString(byteArray: ByteArray): String {
//        require(byteArray.size == 4) { "字节数组长度必须为4" }
//
//        return byteArray.joinToString(".") { byte ->
//            // 将字节转换为无符号整数 (0-255)
//            (byte.toInt() and 0xFF).toString()
//        }
//    }
//
//    /**
//     * 将版本号字符串转换为ByteArray(4)
//     * 例如："1.0.0.0" → 01 00 00 00
//     * @param version 版本号字符串，格式为 X.X.X.X
//     * @return 4字节的字节数组
//     */
//    fun versionStringToByteArray(version: String): ByteArray {
//        val parts = version.split(".")
//        require(parts.size == 4) { "版本号格式必须为 X.X.X.X" }
//
//        return ByteArray(4) { index ->
//            val part = parts.getOrNull(index)?.toIntOrNull()
//                ?: throw IllegalArgumentException("版本号部分必须为数字: ${parts.getOrNull(index)}")
//
//            if (part < 0 || part > 255) {
//                throw IllegalArgumentException("版本号各部分必须在 0-255 范围内: $part")
//            }
//
//            part.toByte()
//        }
//    }
}