package com.example.yikupayloadexample.component

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.yikupayloadexample.MApplication
import com.example.yikupayloadexample.R
import com.yiku.yikupayloadSDK.service.UpgradeService
import com.yiku.yikupayloadSDK.util.*
import java.io.File
import java.io.FileInputStream
import kotlin.concurrent.thread

class FirmwareUpdateFragment : Fragment() {

    private val TAG = "FirmwareUpdateFragment"
    private lateinit var deviceSpinner: Spinner
    private lateinit var currentVersionText: TextView
    private lateinit var fileNameText: TextView
    private lateinit var newVersionText: EditText
    private lateinit var selectFileBtn: Button
    private lateinit var updateBtn: Button
    private lateinit var restartBtn: Button
    private lateinit var progressBarUpdate: ProgressBar
    private lateinit var updateText: TextView

    private var firmwareFile: File? = null
    private val upgradeService = UpgradeService()
    private var selectedDeviceName = ""
    private var ip = ""
    private var port = 8519
    private var hadUpgrading = false
    private var result_resetUpgradeInfo = -1
    private var result_startUpgrade = -1
    private var result_transmission = -1
    private var totalPackages = 0
    private var uploadedPackages = 0
    private var result_verify = -1
    private var errorCount = 0
    private var maxPackageSize = 1024
    private var retryCount_jumpPacket = 0

    private lateinit var sharedPreferences: SharedPreferences
    private var queryTimeoutRunnable: Runnable? = null
    @Volatile
    private var isDestroying = false
    @Volatile
    private var shouldRetryConnect = false


    // 设备信息数据类
    private data class DeviceInfo(
        val identifier: String,       // 用于 SharedPreferences 的 key 后缀（如 "AllInOneHost"）
        val defaultIp: String,
        val port: Int
    )

    // 设备列表：资源ID -> 设备信息
    private val deviceMap = linkedMapOf(
//        R.string.megaphone to DeviceInfo("ShoutHost", ShoutHost, 8519),
//        R.string.searchlight_300W to DeviceInfo("LightHost", LightHost, 8519),
//        R.string.capture_net to DeviceInfo("CacheNetHost", CacheNetHost, 8519),
//        R.string.emitter_38mm to DeviceInfo("EmitterHost", EmitterHost, 8519),
//        R.string.four_in_one to DeviceInfo("YA3Host", YA3Host, 8519),
//        R.string.thrower to DeviceInfo("ThrowerHost", ThrowerHost, 8519),
//        R.string.slow_descent_device_50 to DeviceInfo("SlowDescentDeviceHost", SlowDescentDeviceHost, 8519),
//        R.string.glass_breaker to DeviceInfo("ResqmeHost", ResqmeHost, 8519),
//        R.string.extinguisher to DeviceInfo("ExtinguisherHost", ExtinguisherHost, 8519),
//        R.string.waterGun to DeviceInfo("WaterGunHost", WaterGunHost, 8519),
//        R.string.bucket to DeviceInfo("BucketHost", BucketHost, 8519),
//        R.string.waterBranch to DeviceInfo("WaterBranchHost", WaterBranchHost, 8519),
        R.string.all_in_one to DeviceInfo("AllInOneHost", AllInOneHost, 8529),
        R.string.four_in_one_2 to DeviceInfo("FourInOne2Host", FourInOne2Host, 8529),
//        R.string.slow_descent_device_200 to DeviceInfo("SlowDescentDevice200Host", SlowDescentDevice200Host, 8519),
//        R.string.water_gun_escape to DeviceInfo("WaterGunEscapeHost", WaterGunEscapeHost, 8519)
    )

    // 存储资源 ID 的顺序列表（用于 Spinner 位置映射）
    private val deviceResIds = deviceMap.keys.toList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.firmware_update, container, false)
        sharedPreferences = requireActivity().getSharedPreferences("myPreferences", Context.MODE_PRIVATE)

        initViews(rootView)
        setupSpinner()

        // 注册消息回调
        upgradeService.registMsgCallback(object : MsgCallback {
            override fun getId(): String = "UpgradeServiceCallback"
            override fun onMsg(msg: ByteArray) {
                if (msg[0] == 0xFF.toByte() && msg[1] == 0xAA.toByte() &&
                    msg[2] == 0x55.toByte() && msg[3] == 0xFF.toByte()) {
                    handleMessage(msg)
                }
            }
        })

        return rootView
    }

    private fun handleMessage(msg: ByteArray) {
        val handler = Handler(Looper.getMainLooper())
        when (msg[11].toInt()) {
            0x01 -> { // 设备信息
                Log.d(TAG, "设备信息")
                handler.post {
                    if (!isAdded) return@post
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
                result_transmission = msg[12].toInt()
                totalPackages = littleEndianToInt(msg, 13)
                uploadedPackages = littleEndianToInt(msg, 15)
            }
            0x05 -> { // 升级包校验结果
                Log.d(TAG, "升级包校验结果")
                result_verify = msg[12].toInt()
            }
            0x06 -> { // 设备重启
                Log.d(TAG, "设备重启")
                if (msg[12].toInt() == 0x00) {
                    showToast(R.string.device_will_restart)
                    thread {
                        Thread.sleep(5000)
                        upgradeService.disConnect()
                        Thread.sleep(2000)
                        connectDevice()
                    }
                } else {
                    showToast(R.string.please_restart_manually)
                }
                handler.post {
                    if (!isAdded) return@post
                    restartBtn.isEnabled = true
                }
            }
            else -> {
                Log.e(TAG, "反馈消息异常，msg= ${bytesToHex(msg)}")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initViews(rootView: View) {
        deviceSpinner = rootView.findViewById(R.id.device_spinner)
        currentVersionText = rootView.findViewById(R.id.currentVersion)
        fileNameText = rootView.findViewById(R.id.fileName)
        newVersionText = rootView.findViewById(R.id.newVersion)
        selectFileBtn = rootView.findViewById(R.id.selectFileBtn)
        updateBtn = rootView.findViewById(R.id.updateBtn)
        restartBtn = rootView.findViewById(R.id.restartBtn)
        progressBarUpdate = rootView.findViewById(R.id.progressBar_update)
        updateText = rootView.findViewById(R.id.updateText)

        currentVersionText.text = getString(R.string.not_connected)

        selectFileBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/octet-stream"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(intent, 100)
        }

        updateBtn.setOnClickListener { onUpdateClicked() }

        restartBtn.setOnClickListener {
            restartBtn.isEnabled = false
            upgradeService.restartDevice()
        }
    }

    private fun setupSpinner() {
        // 将资源 ID 转换为当前语言的字符串列表
        val deviceNames = deviceResIds.map { getString(it) }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, deviceNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deviceSpinner.adapter = adapter

        deviceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onDeviceSelected(deviceResIds[position])
                disconnectAndReconnect()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 默认选中第一个
        deviceSpinner.setSelection(0)
    }

    private fun onDeviceSelected(resId: Int) {
        val info = deviceMap[resId] ?: return
        selectedDeviceName = getString(resId) // 用于显示
        ip = sharedPreferences.getString(info.identifier, info.defaultIp) ?: info.defaultIp
        port = info.port
    }

    private fun connectDevice() {
        if (ip.isEmpty()) return
        shouldRetryConnect = true
        thread {
            val handler = Handler(Looper.getMainLooper())

            while (shouldRetryConnect && !isDestroying) {
                upgradeService.disConnect()   // 先断开任何现有连接
                Thread.sleep(200)
                // 显示“正在连接”
                handler.post {
                    if (!isAdded) return@post
                    currentVersionText.text = getString(R.string.connecting)
                }

                if(!shouldRetryConnect) break
                // 发起一次连接
                upgradeService.connect(ip, port)

                Thread.sleep(1000)
                if(!shouldRetryConnect) break

                if (upgradeService.getIsConnected()) {
                    // 连接成功，开始查询设备信息
                    handler.post {
                        if (!isAdded) return@post
                        currentVersionText.text = getString(R.string.querying)
                    }
                    upgradeService.getDeviceInfo()

                    // 5秒查询超时
                    queryTimeoutRunnable = Runnable {
                        if (currentVersionText.text == getString(R.string.querying)) {
                            currentVersionText.text = getString(R.string.query_failed)
                        }
                    }
                    handler.postDelayed(queryTimeoutRunnable!!, 5000)
                    break // 退出重试循环
                } else {
                    // 连接失败，显示“连接失败”，等待3秒后重试
                    handler.post {
                        if (!isAdded) return@post
                        currentVersionText.text = getString(R.string.connect_failed)
                    }
                    // 等待3秒，期间如果 shouldRetryConnect 变为 false 则退出
                    for (i in 0 until 3) {
                        if (!shouldRetryConnect) return@thread
                        Thread.sleep(1000)
                    }
                }
            }
        }
    }

    private fun disconnectAndReconnect() {
        shouldRetryConnect = false // 停止旧的重试循环
        thread {
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                if (!isAdded) return@post
                currentVersionText.text = getString(R.string.not_connected)
            }
            upgradeService.disConnect()
            Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
            Thread.sleep(2000) // 等待两秒再重连，等connectDevice里面的线程彻底结束
            connectDevice() // 重新开始连接（会设置 shouldRetryConnect = true）
        }
    }

    private fun onUpdateClicked() {
        if (!upgradeService.getIsConnected()) {
            showToast(R.string.not_connected)
            return
        }
        if (currentVersionText.text.isEmpty()) {
            showToast(R.string.unknown_current_version)
            return
        }
        if (firmwareFile == null || firmwareFile!!.length().toInt() == 0) {
            showToast(R.string.please_select_file)
            return
        }
        if (newVersionText.text.isEmpty() || newVersionText.text.split(".").size != 4) {
            showToast(R.string.version_error)
            return
        }
        updateBtn.setText(R.string.upgrading)
        componentEnabled(false)
        if (hadUpgrading) {
            result_resetUpgradeInfo = -1
            upgradeService.resetDeviceInfo()
            updateText.setText(R.string.resetting_upgrade_info)
            updateText.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        }
        thread {
            val handler = Handler(Looper.getMainLooper())
            if (hadUpgrading) {
                Thread.sleep(3000)
                errorCount = 0
                while (result_resetUpgradeInfo != 0 && errorCount < 3) {
                    upgradeService.resetDeviceInfo()
                    errorCount++
                    Thread.sleep(3000)
                }
                if (result_resetUpgradeInfo == -1) {
                    handler.post {
                        if (!isAdded) return@post
                        updateText.setText(R.string.device_not_responding)
                        updateText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                        updateBtn.setText(R.string.start_upgrade)
                        componentEnabled(true)
                    }
                    return@thread
                }
                if (result_resetUpgradeInfo > 0) {
                    handler.post {
                        if (!isAdded) return@post
                        updateText.setText(R.string.upgrade_info_reset_failed)
                        updateText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                        updateBtn.setText(R.string.start_upgrade)
                        componentEnabled(true)
                    }
                    return@thread
                }
            }
            handler.post {
                if (!isAdded) return@post
                updateText.setText(R.string.requesting_upgrade)
            }
            result_startUpgrade = -1
            upgradeService.startUpgrade(newVersionText.text.toString(), firmwareFile!!.length().toInt())
            errorCount = 0
            while (result_startUpgrade == -1) {
                errorCount++
                if (errorCount > 5) {
                    handler.post {
                        if (!isAdded) return@post
                        updateText.setText(R.string.device_not_responding)
                        updateText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                        updateBtn.setText(R.string.start_upgrade)
                        componentEnabled(true)
                    }
                    return@thread
                }
                Thread.sleep(1000)
            }
            if (result_startUpgrade > 0) {
                val errorMsg = when (result_startUpgrade) {
                    0x01 -> resources.getString(R.string.no_need_upgrade)
                    0x02 -> resources.getString(R.string.inconsistency_in_size)
                    0x03 -> resources.getString(R.string.inconsistent_firmware_versions)
                    else -> resources.getString(R.string.upgrade_request_failed)
                }
                handler.post {
                    if (!isAdded) return@post
                    updateText.text = errorMsg
                    updateText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                    updateBtn.setText(R.string.start_upgrade)
                    componentEnabled(true)
                }
                return@thread
            }
            retryCount_jumpPacket = 0
            if (!uploadFirmwareFile()) {
                return@thread
            }
            handler.post {
                if (!isAdded) return@post
                updateText.setText(R.string.verifying_firmware_file)
            }
            result_verify = -1
            upgradeService.packageVerification()
            errorCount = 0
            var retryCount = 0
            while (result_verify == -1) {
                errorCount++
                if (errorCount > 5) {
                    if (retryCount < 3) {
                        retryCount++
                        errorCount = 0
                        upgradeService.packageVerification()
                    } else {
                        handler.post {
                            if (!isAdded) return@post
                            updateText.setText(R.string.device_not_responding)
                            updateText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                            updateBtn.setText(R.string.start_upgrade)
                            componentEnabled(true)
                        }
                        return@thread
                    }
                }
                Thread.sleep(1000)
            }
            if (result_verify > 0) {
                val errorMsg = when (result_verify) {
                    0x01 -> resources.getString(R.string.firmware_is_incomplete)
                    0x02 -> resources.getString(R.string.firmware_verification_failed)
                    else -> resources.getString(R.string.firmware_verification_failed)
                }
                handler.post {
                    if (!isAdded) return@post
                    updateText.text = errorMsg
                    updateText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                    updateBtn.setText(R.string.start_upgrade)
                    componentEnabled(true)
                }
                return@thread
            }
            handler.post {
                if (!isAdded) return@post
                updateText.setText(R.string.upgrade_successful)
                updateBtn.setText(R.string.start_upgrade)
                componentEnabled(true)
            }
        }
    }

    fun componentEnabled(isEnabled: Boolean) {
        selectFileBtn.isEnabled = isEnabled
        newVersionText.isEnabled = isEnabled
        updateBtn.isEnabled = isEnabled
        restartBtn.isEnabled = isEnabled
        if (isEnabled) {
            progressBarUpdate.visibility = View.INVISIBLE
        }
    }

    fun uploadFirmwareFile(): Boolean {
        val handler = Handler(Looper.getMainLooper())
        val packagesNum = (firmwareFile!!.length() / maxPackageSize + 1).toInt()
        handler.post {
            if (!isAdded) return@post
            updateText.setText(R.string.transmitted_firmware)
            progressBarUpdate.max = packagesNum
            progressBarUpdate.progress = 0
            progressBarUpdate.visibility = View.VISIBLE
        }
        Log.d(TAG, "包总数：${packagesNum}, 总大小：${firmwareFile!!.length()}")
        val buffer = ByteArray(maxPackageSize)
        var packageIndex = 1
        FileInputStream(firmwareFile).use { inputStream ->
            if (uploadedPackages > 0) {
                val skipBytes = uploadedPackages.toLong() * maxPackageSize
                val actualSkipped = inputStream.skip(skipBytes)
                Log.d(TAG, "尝试跳过 $skipBytes 字节，实际跳过 $actualSkipped 字节")
                if (actualSkipped < skipBytes) {
                    handler.post {
                        if (!isAdded) return@post
                        updateText.setText(R.string.resume_failure)
                        updateText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                        updateBtn.setText(R.string.start_upgrade)
                        componentEnabled(true)
                    }
                    return false
                }
                packageIndex = uploadedPackages + 1
            }
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                val packageData = buffer.copyOf(bytesRead)
                result_transmission = -1
                upgradeService.transmissionPackage(packagesNum, packageIndex, packageData)
                errorCount = 0
                var retryCount = 0
                while (result_transmission == -1) {
                    Thread.sleep(200)
                    errorCount++
                    if (errorCount > 15) {
                        if (retryCount < 3) {
                            retryCount++
                            errorCount = 0
                            upgradeService.transmissionPackage(packagesNum, packageIndex, packageData)
                        } else {
                            handler.post {
                                if (!isAdded) return@post
                                updateText.setText(R.string.device_not_responding)
                                updateText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                                updateBtn.setText(R.string.start_upgrade)
                                componentEnabled(true)
                            }
                            return false
                        }
                    }
                }
                when (result_transmission) {
                    0, 2 -> {
                        handler.post {
                            if (!isAdded) return@post
                            progressBarUpdate.progress = packageIndex
                        }
                        packageIndex++
                        continue
                    }
                    1 -> {
                        handler.post {
                            if (!isAdded) return@post
                            updateText.setText(R.string.packages_number_error)
                            updateText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                            updateBtn.setText(R.string.start_upgrade)
                            componentEnabled(true)
                        }
                        return false
                    }
                    3 -> {
                        retryCount_jumpPacket++
                        if (retryCount_jumpPacket > 3) {
                            handler.post {
                                if (!isAdded) return@post
                                updateText.text = "${resources.getString(R.string.jumping_bag)}，（${retryCount_jumpPacket}/3）"
                                updateText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                                updateBtn.setText(R.string.start_upgrade)
                                componentEnabled(true)
                            }
                            return false
                        } else {
                            handler.post {
                                if (!isAdded) return@post
                                updateText.text = "${resources.getString(R.string.jumping_bag)}，（${retryCount_jumpPacket}/3）"
                            }
                            return uploadFirmwareFile()
                        }
                    }
                }
            }
        }
        return true
    }

    private fun showToast(toastMsg: Int) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(MApplication.applicationContext, toastMsg, Toast.LENGTH_SHORT).show()
        }
    }

    fun littleEndianToInt(byteArray: ByteArray, startIndex: Int = 0): Int {
        require(byteArray.size >= startIndex + 2) { "字节数组从索引${startIndex}开始长度不足2字节" }
        val lowByte = byteArray[startIndex].toInt() and 0xFF
        val highByte = byteArray[startIndex + 1].toInt() and 0xFF
        return (highByte shl 8) or lowByte
    }

    fun byteArrayToVersionString(byteArray: ByteArray): String {
        require(byteArray.size == 4) { "字节数组长度必须为4" }
        return byteArray.joinToString(".") { (it.toInt() and 0xFF).toString() }
    }

    fun versionStringToByteArray(version: String): ByteArray {
        val parts = version.split(".")
        require(parts.size == 4) { "版本号格式必须为 X.X.X.X" }
        return ByteArray(4) { index ->
            val part = parts.getOrNull(index)?.toIntOrNull()
                ?: throw IllegalArgumentException("版本号部分必须为数字: ${parts.getOrNull(index)}")
            require(part in 0..255) { "版本号各部分必须在 0-255 范围内: $part" }
            part.toByte()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i(TAG, "onActivityResult... requestCode:$requestCode, resultCode:$resultCode, data:$data")
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null || requestCode != 100) {
            Log.i(TAG, "data:${data}, req:${requestCode}")
            return
        }
        val path = GetFilePathFromUri.getFileAbsolutePath(requireContext(), data.data)
        Log.i(TAG, "name: ${path.split("/").last()}, path:$path")
        firmwareFile = File(path)
        if (!firmwareFile!!.exists()) {
            val altPath = path.replace("emulated/0", "external_sd")
            Log.i(TAG, "altPath:$altPath")
            firmwareFile = File(altPath)
        }
        if (!firmwareFile!!.exists()) {
            showToast(R.string.failed_to_obtain_file)
            return
        }
        Log.e(TAG, "Name: ${firmwareFile?.name}")
        val fileName = firmwareFile!!.name.split(":").last()
        fileNameText.text = fileName
        if (fileName.contains("v", ignoreCase = true)) {
            val firmwareVersion = extractVersionWithRegex(fileName)
            val parts = firmwareVersion?.split(".")
            if (parts?.size != 4) {
                Log.d(TAG, "文件名格式不正确，无法自动获取版本号")
            } else {
                newVersionText.setText(firmwareVersion)
            }
        }
    }

    fun extractVersionWithRegex(filename: String): String? {
        val pattern = """(?i)V(\d+(?:\.\d+)*)\.bin""".toRegex()
        val result = pattern.find(filename)
        return result?.groupValues?.get(1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isDestroying = true
        upgradeService.disConnect()
        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
    }
}