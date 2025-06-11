package com.example.yikupayloadexample

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.marginBottom
import com.example.yikupayloadexample.service.GasMonitoringService
import com.lzf.easyfloat.utils.InputMethodUtils
import com.yiku.yikupayloadSDK.util.MsgCallback
import org.json.JSONObject
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

class GasMonitoringWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr)  {
    private val TAG = "GasMonitoringWeight"
    private lateinit var gasMonitoringView: LinearLayout
    private lateinit var settingBtn: Button
    private lateinit var alarmSwitch: Switch
    private lateinit var gasDatasView: LinearLayout
    private lateinit var settingView: LinearLayout
    private lateinit var gasMonitoringSettingsView: LinearLayout
    private lateinit var saveBtn: Button
    private lateinit var cancelBtn: Button

    var gasMonitoringService: GasMonitoringService = GasMonitoringService()
    private var isConnecting: Boolean = false
    private var isFirstConnect: Boolean = true
    private var updateTime = Date().time
    private var gasValueMap = HashMap<String, TextView>()
    private var gasStatusMap = HashMap<String, TextView>()
    private var gasMonitoringSettingMap = HashMap<String, String>()
    private var standardTypeSelectorMap = HashMap<String, Spinner>()
    private var standardValueEditTextMap = HashMap<String, EditText>()
    private val options = listOf(context.resources.getString(R.string.lower_than), context.resources.getString(R.string.higher_than)) // 0：低于，1：高于
    private var isPlayingAlarm: Boolean = false

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        val host = preferences?.getString("GasMonitoringHost", "192.168.144.211")
        if(host != null && "" != host) {
            gasMonitoringService.setIp(host)
        }
        initView(context)
        getGasMonitoringSettings()
        gasMonitoringService.registMsgCallback(object : MsgCallback {
            override fun getId(): String {
                return "GasMonitoringServiceCallback"
            }

            override fun onMsg(msg: ByteArray) {
                Log.i(TAG, "msg:${String(msg)}")
                updateState(msg)
            }

        })
    }

    // 更新气体监测数据
    private fun updateState(msg: ByteArray) {
        updateTime = Date().time
        val statusStr = String(msg)
        val statusObject = JSONObject(statusStr)
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            var isNeedAlarm = false // 是否需要响警报
            // 遍历所有键值对
            statusObject.keys().forEach { key ->
                if(key == "Time") {
                    return@forEach
                }
                val value = statusObject.getString(key)
                // 如果已经添加，就只更改值
                if(gasValueMap.containsKey(key)) {
                    gasValueMap[key]?.text = value
                    if(updateStatusText(key, value)) {
                        isNeedAlarm = true
                    }
                }
                else {
                    val linearLayout = LinearLayout(context).apply {
                        orientation = HORIZONTAL // 横向排列
                        layoutParams = LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, // 宽度
                            ViewGroup.LayoutParams.WRAP_CONTENT   // 高度
                        ).apply {
                            bottomMargin = 8
                        }
                    }
                    // 气体名称
                    val gasNameText = TextView(context).apply {
                        this.text = "$key: "
                        setTextColor(Color.WHITE)
                        layoutParams = LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, 0)
                        }
                    }
                    linearLayout.addView(gasNameText)

                    // 气体浓度
                    val gasValueText = TextView(context).apply {
                        this.text = value
                        setTextColor(Color.WHITE)
                        layoutParams = LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(10, 0, 0, 0)
                        }
                    }
                    linearLayout.addView(gasValueText)
                    gasValueMap[key] = gasValueText

                    // 气体状态
                    val gasStatusText = TextView(context).apply {
                        layoutParams = LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(10, 0, 0, 0)
                        }
                    }
                    linearLayout.addView(gasStatusText)
                    gasStatusMap[key] = gasStatusText
                    if(updateStatusText(key, value)) {
                        isNeedAlarm = true
                    }
                    gasDatasView.addView(linearLayout)
                }
            }
            val edit = preferences?.edit()
            val alarStatus = preferences?.getBoolean("isPlayingAlarm", false)
            // 如果警报开关没打开，说明强制关闭报警
            if(!alarmSwitch.isChecked) {
                if(isPlayingAlarm) {
                    isPlayingAlarm = false
                    megaphoneService?.stopLoopTts()
                    megaphoneService?.redBlueLedControl(0) // 关闭红蓝
                    edit?.putBoolean("isPlayingAlarm", false)
                    edit?.apply()
                }
                return@post
            }
            // 未连上喊话器或四合一，不作处理
            if(megaphoneService?.getIsConnected() == false && megaphoneService?.getIsConnectedYA3() == false){
                return@post
            }
            // 如果需要响警报
            if(isNeedAlarm) {
                // 如果未响警报，则播放警报
                if(!isPlayingAlarm) {
                    isPlayingAlarm = true
                    megaphoneService?.startLoopTtsV2(context.resources.getString(R.string.gas_concentration_is_abnormal), 1)
                    edit?.putBoolean("isPlayingAlarm", true)
                    megaphoneService?.redBlueLedControl(1) // 打开红蓝
                }
            }
            else {
                // 如果在不需要播放的时候，正在播放警报，停止
                isPlayingAlarm = false
                megaphoneService?.stopLoopTts()
                edit?.putBoolean("isPlayingAlarm", false)
                megaphoneService?.redBlueLedControl(0) // 关闭红蓝
            }
            edit?.apply()
        }
    }

    // 更新状态文字显示
    private fun updateStatusText(key: String, value: String): Boolean {
        var isNeedAlarm = false // 是否需要响警报
        if(!gasMonitoringSettingMap.containsKey(key)) {
            gasStatusMap[key]?.setText(R.string.standard_not_set)
            gasStatusMap[key]?.setTextColor(Color.YELLOW)
            return false
        }

        val standardStr = gasMonitoringSettingMap.getValue(key)
        try {
            if(standardStr == "") {
                gasStatusMap[key]?.setText(R.string.standard_not_set)
                gasStatusMap[key]?.setTextColor(Color.YELLOW)
            }

            val standardList = standardStr.split(",")
            val standardType = standardList[0].toInt()
            val standardValue = standardList[1].toDouble()
            when (standardType) {
                0 -> { // 低于
                    if (value.toDouble() < standardValue) {
                        gasStatusMap[key]?.setText(R.string.too_low)
                        gasStatusMap[key]?.setTextColor(Color.RED)
                        isNeedAlarm = true
                    } else {
                        gasStatusMap[key]?.setText(R.string.normal)
                        gasStatusMap[key]?.setTextColor(Color.GREEN)
                    }
                }

                1 -> { // 高于
                    if (value.toDouble() > standardValue) {
                        gasStatusMap[key]?.setText(R.string.too_high)
                        gasStatusMap[key]?.setTextColor(Color.RED)
                        isNeedAlarm = true
                    } else {
                        gasStatusMap[key]?.setText(R.string.normal)
                        gasStatusMap[key]?.setTextColor(Color.GREEN)
                    }
                }
            }
        }
        catch (e: Exception) {
            gasStatusMap[key]?.setText(R.string.setting_exceptions)
            gasStatusMap[key]?.setTextColor(Color.YELLOW)
            Log.e(TAG, "气体监控设置异常，设置内容：${standardStr}")
        }
        Log.i(TAG, "气体：$key，是否需要警报：$isNeedAlarm")
        return isNeedAlarm
    }

    private fun initView(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.gas_monitoring_weight, this, true)
        gasMonitoringView = findViewById(R.id.GasMonitoring_view)
        settingBtn = findViewById(R.id.settingBtn)
        alarmSwitch = findViewById(R.id.alarmSwitch)
        gasDatasView = findViewById(R.id.gasDatas)
        settingView = findViewById(R.id.GasMonitoringSetting_view)
        gasMonitoringSettingsView = findViewById(R.id.gasMonitoringSettings)
        saveBtn = findViewById(R.id.saveBtn)
        cancelBtn = findViewById(R.id.cancelBtn)

        // 警报开关
        alarmSwitch.isChecked = preferences?.getBoolean("alarmSwitch", true) == true
        alarmSwitch.setOnClickListener {
            val edit = preferences?.edit()
            edit?.putBoolean("alarmSwitch", alarmSwitch.isChecked)
            edit?.apply()
        }

        // 打开设置页面
        settingBtn.setOnClickListener {
            gasMonitoringView.visibility = GONE
            settingView.visibility = VISIBLE
            // 先清空设置页面内容
            gasMonitoringSettingsView.removeAllViews()
            standardTypeSelectorMap.clear()
            standardValueEditTextMap.clear()

            Log.i(TAG, "gasValueMap_O2= " + gasValueMap["O2"])
            // 根据当前监测的气体来设置内容
            for((key, gasValueTextView) in gasValueMap) {
                Log.i(TAG, "key=$key")
                val linearLayout = LinearLayout(context).apply {
                    orientation = HORIZONTAL // 横向排列
                    layoutParams = LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, // 宽度
                        ViewGroup.LayoutParams.WRAP_CONTENT   // 高度
                    ).apply {
                        bottomMargin = 8
                    }
                }
                var standardType = 1
                var standardValue = ""
                if(gasMonitoringSettingMap.containsKey(key) && gasMonitoringSettingMap[key] != "") {
                    try{
                        val standardList = gasMonitoringSettingMap[key]!!.split(",")
                        standardType = standardList[0].toInt()
                        standardValue = standardList[1]
                    }catch (e: Exception) {
                        Log.e(TAG, "气体监测标准设置异常，设置内容：${gasMonitoringSettingMap[key]}")
                    }
                }

                // 气体名称
                val gasNameText = TextView(context).apply {
                    setTextColor(Color.WHITE)
                    val textStr = "$key: "
                    this.text = textStr
                }
                linearLayout.addView(gasNameText)

                // 标准类型：低于、高于
                val standardTypeSelector = Spinner(context).apply {
                    setBackgroundResource(R.drawable.boder)
                    layoutParams = LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        74
                    ).apply {
                        setMargins(10, 0, 10, 0)
                    }
                }
                val adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, options) {
                    // 设置显示项文字样式
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getView(position, convertView, parent) as TextView
                        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f) // 文字大小
                        view.setTextColor(Color.WHITE) // 文字颜色
                        return view
                    }

                    // 设置下拉项文字样式
                    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getDropDownView(position, convertView, parent) as TextView
                        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                        view.setTextColor(Color.DKGRAY)
                        return view
                    }
                }

                standardTypeSelector.adapter = adapter // 绑定适配器
                standardTypeSelector.setSelection(standardType)
                linearLayout.addView(standardTypeSelector)
                standardTypeSelectorMap[key] = standardTypeSelector

                // 标准值
                val standardValueEditText = EditText(context).apply {
                    setTextColor(Color.WHITE)
                    setBackgroundResource(R.drawable.boder)
                    layoutParams = LayoutParams(
                        140, // 宽度
                        ViewGroup.LayoutParams.MATCH_PARENT   // 高度
                    )
                    setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) InputMethodUtils.openInputMethod(
                            this,
                            "yk_payload_weight_op"
                        )
                        false
                    }
                }
                if(standardValue != "") {
                    standardValueEditText.setText(standardValue)
                }
                linearLayout.addView(standardValueEditText)
                standardValueEditTextMap[key.toString()] = standardValueEditText

                gasMonitoringSettingsView.addView(linearLayout)
            }
        }
        // 保存
        saveBtn.setOnClickListener {
            saveBtn.isEnabled = false
            saveGasMonitoringSettings()
            saveBtn.isEnabled = true
            settingView.visibility = GONE
            gasMonitoringView.visibility = VISIBLE
        }
        // 取消
        cancelBtn.setOnClickListener {
            settingView.visibility = GONE
            gasMonitoringView.visibility = VISIBLE
        }
        setConnectState()
    }

    // 读取气体监管设置
    private fun getGasMonitoringSettings() {
        val gasMonitoringSettings = preferences?.getString("GasMonitoringSettings", "")
        if(gasMonitoringSettings != null && gasMonitoringSettings != "") {
            val jsonObject = JSONObject(gasMonitoringSettings)
            jsonObject.keys().forEach { key ->
                gasMonitoringSettingMap[key] = jsonObject.getString(key)
            }
        }
    }

    // 保存气体监管设置
    private fun saveGasMonitoringSettings() {
        // 先将设置的内容添加到gasMonitoringSettingMap里面
        for ((key, standardTypeSelector) in standardTypeSelectorMap) {
            val standardType = standardTypeSelectorMap[key]?.selectedItemId
            val standardValue = standardValueEditTextMap[key]?.text
            if(standardValue != null && standardValue.trim().toString() != "") {
                Log.i(TAG, standardValue.trim().toString())
                gasMonitoringSettingMap[key] = "${standardType},${standardValue}"
            }
            // 如果值为空字符串，移除这项设置
            if(standardValue != null && standardValue.trim().toString() == "" && gasMonitoringSettingMap.containsKey(key)) {
                gasMonitoringSettingMap.remove(key)
            }
        }

        // 将gasMonitoringSettingMap内容转成json字符串，存进缓存里
        val jsonObject = JSONObject()
        for((key, gasMonitoringSetting) in gasMonitoringSettingMap) {
            jsonObject.put(key, gasMonitoringSettingMap.getValue(key))
        }
        Log.i(TAG, "气体监测设置1：${gasMonitoringSettingMap}")
        Log.i(TAG, "气体监测设置2：${jsonObject}")
        val edit = preferences!!.edit()
        edit.putString("GasMonitoringSettings", jsonObject.toString())
        edit.apply()
    }

    private fun showToast(msg: Int) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(
                MApplication.applicationContext, msg, Toast.LENGTH_LONG
            ).show()
        }
    }

    // 定时器，判断连接状态
    private fun setConnectState(){
        val timer = Timer();
        val connectText = findViewById<TextView>(R.id.GasMonitoringConnect)
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask(){
            override fun run() {
                if(gasMonitoringService.getIsConnected()){
                    if (Date().time - updateTime < 3000) {
                        handler.post {
                            connectText.setText(R.string.connection_status_connected)
                        }
                    }
                    // 3秒没收到信息，显示未连接
                    if (Date().time - updateTime > 3000) {
                        handler.post {
                            connectText.setText(R.string.connection_status_notconnected)
                        }
                    }
                    // 如果超过10s没收到消息，主动断开连接，等待重连
                    if (Date().time - updateTime > 10000) {
                        // 断连
                        gasMonitoringService.disConnect()
                    }
                }
                else if(!isConnecting){
                    isConnecting = true
                    handler.post {
                        connectText.setText(R.string.connection_status_notconnected)
                    }
                    // 尝试重连
                    thread {
                        if(!isFirstConnect) {
                            Thread.sleep(10000)
                        }
                        isFirstConnect = false
                        while (!gasMonitoringService.connect()) {
                            Thread.sleep(1000)
                        }
                        isConnecting = false
                    }
                }
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.scheduleAtFixedRate(task, 100, 1000);
    }
}