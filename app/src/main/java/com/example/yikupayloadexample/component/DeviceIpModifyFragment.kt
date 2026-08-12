package com.example.yikupayloadexample.component

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.example.yikupayloadexample.MApplication
import com.example.yikupayloadexample.R
import com.yiku.yikupayloadSDK.protocol.SETIP_READ_IP
import com.yiku.yikupayloadSDK.service.SetDeviceIpService
import com.yiku.yikupayloadSDK.util.EmitterHost
import com.yiku.yikupayloadSDK.util.MsgCallback
import com.yiku.yikupayloadSDK.util.byteArrayToInt16LE
import com.yiku.yikupayloadSDK.util.byteArrayToIpString
import kotlin.concurrent.thread

class DeviceIpModifyFragment : Fragment() {
    private val TAG = "DeviceIpModifyFragment"
    private lateinit var deviceSpinner: Spinner
    private lateinit var stateText: TextView
    private lateinit var ipEditText: EditText
    private lateinit var gatewayEditText: EditText
    private lateinit var portEditText: EditText
    private lateinit var saveBtn: Button
    private lateinit var restartBtn: Button
    private lateinit var resetBtn: Button

    private val deviceIpService = SetDeviceIpService()
    private var selectedDeviceName = ""
    private var ip = ""
    private var port = 8519

    private lateinit var sharedPreferences: SharedPreferences
    private var queryTimeoutRunnable: Runnable? = null
    private var selectedDeviceInfo: DeviceInfo? = null

    @Volatile
    private var isDestroying = false
    @Volatile
    private var shouldRetryConnect = false

    // 设备信息数据类
    private data class DeviceInfo(
        val ipKey: String,       // 用于 SharedPreferences 的 key 后缀（如 "AllInOneHost"）
        val defaultIp: String,
        val portKey: String,
        val defaultPort: Int
    )

    // 设备列表：资源ID -> 设备信息
    private val deviceMap = linkedMapOf(
//        R.string.megaphone to DeviceInfo("ShoutHost", ShoutHost, 8519),
//        R.string.searchlight_300W to DeviceInfo("LightHost", LightHost, 8519),
//        R.string.capture_net to DeviceInfo("CacheNetHost", CacheNetHost, 8519),
        R.string.emitter_38mm to DeviceInfo("EmitterHost", EmitterHost, "EmitterPort", 8519),
//        R.string.four_in_one to DeviceInfo("YA3Host", YA3Host, 8519),
//        R.string.thrower to DeviceInfo("ThrowerHost", ThrowerHost, 8519),
//        R.string.slow_descent_device_50 to DeviceInfo("SlowDescentDeviceHost", SlowDescentDeviceHost, 8519),
//        R.string.glass_breaker to DeviceInfo("ResqmeHost", ResqmeHost, 8519),
//        R.string.extinguisher to DeviceInfo("ExtinguisherHost", ExtinguisherHost, 8519),
//        R.string.waterGun to DeviceInfo("WaterGunHost", WaterGunHost, 8519),
//        R.string.bucket to DeviceInfo("BucketHost", BucketHost, 8519),
//        R.string.waterBranch to DeviceInfo("WaterBranchHost", WaterBranchHost, 8519),
//        R.string.all_in_one to DeviceInfo("AllInOneHost", AllInOneHost, 8529),
//        R.string.four_in_one_2 to DeviceInfo("FourInOne2Host", FourInOne2Host, 8519),
//        R.string.slow_descent_device_200 to DeviceInfo("SlowDescentDevice200Host", SlowDescentDevice200Host, 8519),
//        R.string.water_gun_escape to DeviceInfo("WaterGunEscapeHost", WaterGunEscapeHost, 8519)
    )

    // 存储资源 ID 的顺序列表（用于 Spinner 位置映射）
    private val deviceResIds = deviceMap.keys.toList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.device_ip_modify, container, false)
        sharedPreferences = requireActivity().getSharedPreferences("myPreferences", Context.MODE_PRIVATE)

        initViews(rootView)
        setupSpinner()

        // 注册消息回调
        deviceIpService.registMsgCallback(object : MsgCallback {
            override fun getId(): String = "SetDeviceIpServiceCallback"
            override fun onMsg(msg: ByteArray) {
                if (msg[0] != 0x8d.toByte()) {
                    return
                }
                if (msg[2] == SETIP_READ_IP.toByte()) {
                    // ip设置信息
                    handleMessage(msg)
                }
            }
        })

        return rootView
    }

    private fun handleMessage(msg: ByteArray) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            if (!isAdded) return@post
            componentEnabled(true)
            stateText.setText(R.string.connected)
            ip = byteArrayToIpString(msg.slice(3 until 7).toByteArray())
            ipEditText.setText(ip)
            gatewayEditText.setText(byteArrayToIpString(msg.slice(7 until 11).toByteArray()))
            port = byteArrayToInt16LE(msg.slice(11 until 13).toByteArray())
            portEditText.setText(port.toString())
        }

    }

    fun ByteArray.toHex(): String =
        joinToString(separator = "") { eachByte -> "%02x ".format(eachByte) }

    private fun initViews(rootView: View) {
        deviceSpinner = rootView.findViewById(R.id.device_spinner)
        stateText = rootView.findViewById(R.id.stateText)
        ipEditText = rootView.findViewById(R.id.ipEditText)
        gatewayEditText = rootView.findViewById(R.id.gatewayEditText)
        portEditText = rootView.findViewById(R.id.portEditText)
        saveBtn = rootView.findViewById(R.id.saveBtn)
        restartBtn = rootView.findViewById(R.id.restartBtn)
        resetBtn = rootView.findViewById(R.id.resetBtn)

        componentEnabled(false)

        // 保存IP设置
        saveBtn.setOnClickListener {
            componentEnabled(false)
            if(!inputValidation()) {
                componentEnabled(true)
                return@setOnClickListener
            }
            val ipStr = ipEditText.text.toString()
            val gatewayStr = gatewayEditText.text.toString()
            val portStr = portEditText.text.toString()
            deviceIpService.setDeviceIpInfo(ipStr, gatewayStr, portStr.toInt())
            showToast(R.string.saving_configuration)
            stateText.setText(R.string.requery_configuration)
            thread {
                Thread.sleep(1000)
                deviceIpService.getDeviceIpInfo()
                val handler = Handler(Looper.getMainLooper())
                // 5秒查询超时
                queryTimeoutRunnable = Runnable {
                    if (stateText.text == getString(R.string.requery_configuration)) {
                        stateText.text = getString(R.string.query_ip_failed)
                    }
                }
                handler.postDelayed(queryTimeoutRunnable!!, 5000)
            }
        }

        // 重启设备
        restartBtn.setOnClickListener {
            AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
                .setTitle(R.string.reset_confirm)
                .setMessage(R.string.reset_confirm_text)
                .setPositiveButton(R.string.ok) { _, _ ->
                    componentEnabled(false)
                    deviceIpService.restartDevice()
                    showToast(R.string.reboot_device)
                    onDeviceRestart()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        // 恢复出厂设置
        resetBtn.setOnClickListener {
            AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
                .setTitle(R.string.reset_confirm)
                .setMessage(R.string.reset_confirm_text)
                .setPositiveButton(R.string.ok) { _, _ ->
                    componentEnabled(false)
                    deviceIpService.resetIpInfo()
                    onDeviceRestart()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    // 当设备重启时调用
    private fun onDeviceRestart() {
        thread {
            Thread.sleep(500)
            deviceIpService.disConnect()
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                if (!isAdded) return@post
                stateText.setText(R.string.disconnected)
                ipEditText.text.clear()
                gatewayEditText.text.clear()
                portEditText.text.clear()
                // 保存配置到缓存
                if(selectedDeviceInfo != null) {
                    sharedPreferences.edit {
                        putString(selectedDeviceInfo!!.ipKey, ip)
                        putString(selectedDeviceInfo!!.portKey, port.toString())
                    }
                }
            }
            Thread.sleep(3000)
            connectDevice()
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
        selectedDeviceInfo = deviceMap[resId] ?: return
        selectedDeviceName = getString(resId) // 用于显示
        if(selectedDeviceInfo != null) {
            ip = sharedPreferences.getString(selectedDeviceInfo!!.ipKey, selectedDeviceInfo!!.defaultIp) ?: selectedDeviceInfo!!.defaultIp
            port = if (selectedDeviceInfo!!.portKey.isEmpty()) {
                selectedDeviceInfo!!.defaultPort
            } else {
                sharedPreferences.getString(selectedDeviceInfo!!.portKey, "")?.toIntOrNull() ?: selectedDeviceInfo!!.defaultPort
            }
        }
    }

    private fun connectDevice() {
        if (ip.isEmpty()) return
        shouldRetryConnect = true
        thread {
            val handler = Handler(Looper.getMainLooper())

            while (shouldRetryConnect && !isDestroying) {
                // 显示“正在连接”
                handler.post {
                    if (!isAdded) return@post
                    stateText.text = getString(R.string.connecting)
                }

                // 发起一次连接
                deviceIpService.connect(ip, port)

                Thread.sleep(1000)

                if (deviceIpService.getIsConnected()) {
                    // 连接成功，开始查询设备信息
                    handler.post {
                        if (!isAdded) return@post
                        stateText.text = getString(R.string.querying)
                    }
                    deviceIpService.getDeviceIpInfo()

                    // 5秒查询超时
                    queryTimeoutRunnable = Runnable {
                        if (stateText.text == getString(R.string.querying)) {
                            stateText.text = getString(R.string.query_ip_failed)
                        }
                    }
                    handler.postDelayed(queryTimeoutRunnable!!, 5000)
                    break // 退出重试循环
                } else {
                    // 连接失败，显示“连接失败”，等待3秒后重试
                    handler.post {
                        if (!isAdded) return@post
                        stateText.text = getString(R.string.connect_failed)
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
        componentEnabled(false)
        thread {
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                if (!isAdded) return@post
                stateText.text = getString(R.string.not_connected)
            }
            deviceIpService.disConnect()
            Thread.sleep(500)
            connectDevice() // 重新开始连接（会设置 shouldRetryConnect = true）
        }
    }

    // 输入值校验
    private fun inputValidation(): Boolean {
        if(!isValidIPv4(ipEditText.text.toString())) {
            showToast(R.string.ip_input_invalid, Toast.LENGTH_LONG)
            return false
        }
        if(!isValidIPv4(gatewayEditText.text.toString())) {
            showToast(R.string.gateway_input_invalid, Toast.LENGTH_LONG)
            return false
        }
        if(!isValidPort(portEditText.text.toString())) {
            showToast(R.string.port_input_invalid, Toast.LENGTH_LONG)
            return false
        }
        return true
    }

    // IPv4校验
    fun isValidIPv4(ipStr: String): Boolean {
        val segments = ipStr.split(".")
        if (segments.size != 4) return false
        return segments.all { segment ->
            // 允许 "0"，但不允许空串、非数字、前导零（如 "01"）
            if (segment.isEmpty() || segment.length > 1 && segment.startsWith('0')) return@all false
            val num = segment.toIntOrNull() ?: return@all false
            num in 0..255
        }
    }
    // 端口校验
    fun isValidPort(portStr: String): Boolean {
        val number = portStr.toIntOrNull() ?: return false
        return number in 0..65535
    }

    fun componentEnabled(isEnabled: Boolean) {
        ipEditText.isEnabled = isEnabled
        gatewayEditText.isEnabled = isEnabled
        portEditText.isEnabled = isEnabled
        saveBtn.isEnabled = isEnabled
        restartBtn.isEnabled = isEnabled
        resetBtn.isEnabled = isEnabled
    }

    private fun showToast(toastMsg: Int, duration: Int = Toast.LENGTH_SHORT) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(MApplication.applicationContext, toastMsg, duration).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isDestroying = true
        deviceIpService.disConnect()
        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
    }
}