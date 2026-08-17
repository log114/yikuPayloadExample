package com.example.yikupayloadexample

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.yiku.yikupayloadSDK.protocol.CARGOBOX_STATE
import com.yiku.yikupayloadSDK.service.CargoBoxService
import com.yiku.yikupayloadSDK.util.MsgCallback
import com.yiku.yikupayloadSDK.util.byteArrayToInt16
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

class CargoBoxWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "CargoBoxWeight"
    private lateinit var mContentView: View
    var cargoBoxService: CargoBoxService = CargoBoxService()
    private lateinit var mSafetySwitch: Switch
    private lateinit var mCompressorSwitch: Switch
    private lateinit var mCompressorSpeed: TextView
    private lateinit var mTemperature: TextView
    private lateinit var mVoltage: TextView
    private lateinit var mCurrent: TextView
    private lateinit var mIncRPMBtn: Button
    private lateinit var mDecRPMBtn: Button
    private var isConnecting: Boolean = false
    private var isFirstConnect: Boolean = true
    private var updateTime = Date().time

    @Volatile
    private var isSettingCompressorSwitch: Boolean = false

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        val host = preferences?.getString("CargoBoxHost", "")
        if(host != null && "" != host) {
            cargoBoxService.setIp(host)
        }
        initView(context)
        cargoBoxService.registMsgCallback(object : MsgCallback {
            override fun getId(): String {
                return "CargoBoxServiceCallback"
            }

            override fun onMsg(msg: ByteArray) {
                Log.i(TAG, "msg:${msg.toHex()}")
                if (msg[0] != 0x8d.toByte()) {
                    return
                }
                if (msg[2] == CARGOBOX_STATE.toByte()) {
                    updateTime = Date().time
                    updateState(msg)
                }
            }

        })
    }

    // 状态更新
    private fun updateState(msg: ByteArray) {
        val stateCode = msg[3].toInt()
        val rotationSpeed = byteArrayToInt16(msg.slice(4 until 6).toByteArray())
        val temperature = byteArrayToInt16(msg.slice(6 until 8).toByteArray())
        val volatile = byteArrayToInt16(msg.slice(8 until 10).toByteArray())
        val current = byteArrayToInt16(msg.slice(10 until 12).toByteArray())
        val errorCode = byteArrayToInt16(msg.slice(12 until 14).toByteArray())
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            mCompressorSpeed.text = context.getString(R.string.rotation_speed_text, rotationSpeed)
            mTemperature.text = context.getString(R.string.temperature_text, temperature/10f)
            mVoltage.text = context.getString(R.string.voltage_text, volatile/1000f)
            mCurrent.text = context.getString(R.string.electric_current_text, current/1000f)
            if(!isSettingCompressorSwitch) {
                mCompressorSwitch.isEnabled = true
                mCompressorSwitch.isChecked = stateCode == 5
                if(stateCode == 6) {
                    showToast(context.getString(R.string.compressor_error, errorCode), Toast.LENGTH_LONG)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.cargo_box_weight, this, true)
        mContentView = findViewById(R.id.cargoBox_view)
        mSafetySwitch = findViewById(R.id.safetySwitch)
        mCompressorSwitch = findViewById(R.id.compressorSwitch)
        mCompressorSpeed = findViewById(R.id.compressorSpeed)
        mTemperature = findViewById(R.id.temperature)
        mVoltage = findViewById(R.id.voltage)
        mCurrent = findViewById(R.id.current)
        mIncRPMBtn = findViewById(R.id.incRPMBtn)
        mDecRPMBtn = findViewById(R.id.decRPMBtn)
        setConnectState()

        // 安全开关
        mSafetySwitch.setOnClickListener {
            mCompressorSwitch.isEnabled = mSafetySwitch.isChecked
            mIncRPMBtn.isEnabled = mSafetySwitch.isChecked
            mDecRPMBtn.isEnabled = mSafetySwitch.isChecked
        }
        
        // 压缩机使能，压缩机开关
        mCompressorSwitch.setOnClickListener {
            mCompressorSwitch.isEnabled = false
            isSettingCompressorSwitch = true
            cargoBoxService.setEnable(mCompressorSwitch.isChecked)
            thread {
                Thread.sleep(2000)
                isSettingCompressorSwitch = false
            }
        }

        // 提高转速
        mIncRPMBtn.setOnClickListener {
            cargoBoxService.increasePower()
        }
        // 降低转速
        mDecRPMBtn.setOnClickListener {
            cargoBoxService.reducePower()
        }
    }

    fun ByteArray.toHex(): String =
        joinToString(separator = "") { eachByte -> "%02x ".format(eachByte) }

    private fun showToast(msg: Int, duration: Int = Toast.LENGTH_SHORT) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(
                MApplication.applicationContext, msg, duration
            ).show()
        }
    }

    private fun showToast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(
                MApplication.applicationContext, msg, duration
            ).show()
        }
    }

    // 定时器，判断连接状态
    private fun setConnectState(){
        val timer = Timer();
        val statusDot = findViewById<View>(R.id.statusDot)
        val background = statusDot.background as GradientDrawable
        val connectText = findViewById<TextView>(R.id.cargoBoxConnect)
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask(){
            override fun run() {
                if(cargoBoxService.getIsConnected()){
                    if (Date().time - updateTime < 3000) {
                        handler.post {
                            connectText.setText(R.string.connection_status_connected)
                            background.setColor(ContextCompat.getColor(context, R.color.green))
                        }
                    }
                    // 3秒没收到信息，显示未连接
                    if (Date().time - updateTime > 3000) {
                        handler.post {
                            connectText.setText(R.string.connection_status_notconnected)
                            background.setColor(ContextCompat.getColor(context, R.color.red))
                        }
                    }
                    // 如果超过10s没收到消息，主动断开连接，等待重连
                    if (Date().time - updateTime > 10000) {
                        // 断连
                        cargoBoxService.disConnect()
                    }
                }
                else if(!isConnecting){
                    isConnecting = true
                    handler.post {
                        connectText.setText(R.string.connection_status_notconnected)
                        background.setColor(ContextCompat.getColor(context, R.color.red))
                    }
                    // 尝试重连
                    thread {
                        if(!isFirstConnect) {
                            Thread.sleep(5000)
                        }
                        isFirstConnect = false
                        while (!cargoBoxService.connect()) {
                            Thread.sleep(1000)
                        }
                        isConnecting = false
                        updateTime = Date().time
                        handler.post {
                            connectText.setText(R.string.connection_status_connected)
                            background.setColor(ContextCompat.getColor(context, R.color.green))
                        }
                    }
                }
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.schedule(task, 100, 1000);
    }
}