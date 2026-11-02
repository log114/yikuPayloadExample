package com.example.yikupayloadexample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.yikupayloadexample.component.RoundMenuView
import com.yiku.yikupayloadSDK.service.LightService
import com.yiku.yikupayloadSDK.util.MsgCallback
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread


class LightWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "LightWeight"
    private lateinit var mLightBtn: ImageView
    var lightService: LightService = LightService()
    private var openLightStatus = 0
    private var sharpFlashStatus = 0
    private val timer: Timer = Timer()
    private lateinit var timerTask: TimerTask
    private lateinit var mDriveTemperatureText: TextView
    private lateinit var mLampHeadTemperatureText: TextView
    private var lastRecvTime: Long = 0L
    private var isFirstConnect = true
    private var isConnecting = false


    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        val host = preferences?.getString("LightHost", "")
        if(host != null && "" != host) {
            lightService.setIp(host)
        }
        initView(context)
    }

    private fun resetShoutBtnsBackground() {
        mLightBtn.setBackgroundResource(R.drawable.yk_shout_btn)
    }


    inner class LightMsgCallback : MsgCallback {
        private val TAG = "LightMsgCallback"
        override fun getId(): String {
            return "LightMsgCallback1129"
        }

        override fun onMsg(msg: ByteArray) {
            if (msg[0] != 0x8d.toByte()) {
                return
            }
            Log.i(TAG, "recv: ${msg.asList()}")
            if (msg[2] == 0x04.toByte()) {
                lastRecvTime = Date().time

                Handler(Looper.getMainLooper()).post(Runnable {
                    changeLuminance(
                        (msg[4].toInt() and 0xFF) - 50, (msg[3].toInt() and 0xff) - 50
                    )
                })
            }
        }

    }

    fun changeLuminance(driverLuminance: Int, ledLuminance: Int) {
        mDriveTemperatureText.text = "${driverLuminance}℃"
        mLampHeadTemperatureText.text = "${ledLuminance}℃"
    }

    private fun initView(context: Context){
        LayoutInflater.from(context).inflate(R.layout.light_weight, this, true)

        val drawable = resources.getDrawable(R.drawable.right) // 替换成你的 Drawable 资源
        val bitmap = Bitmap.createBitmap(
            50, 50, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, 50, 50)
        drawable.draw(canvas)

        val mLightLumText = findViewById<TextView>(R.id.light_lum_text)
        val mOpenLightBtn = findViewById<Button>(R.id.open_light_btn)
        val mSharpFlashBtn = findViewById<Button>(R.id.sharp_flash_btn)
        val mLuminanceChangeSeekbar =
            findViewById<SeekBar>(R.id.luminance_change_seekbar)

        mDriveTemperatureText = findViewById(R.id.drive_temperature)
        mLampHeadTemperatureText = findViewById(R.id.lamp_head_temperature)

        mDriveTemperatureText.text = "0℃"
        mLampHeadTemperatureText.text = "0℃"
        // 初始化通讯回调函数
        lightService.registMsgCallback(LightMsgCallback())

        // 查询初始化状态
        lightService.openLight(0, true) // 开灯状态
//        lightService.sharpFlash(0, true) // 爆闪状态（如果当前是开灯状态，会导致灯爆闪）
        lightService.fetchTemperature() // 温度

        mOpenLightBtn.setOnClickListener {
            lightService.openLight(if (openLightStatus == 0) 1 else 0, false)
            if (openLightStatus == 0) {
                openLightStatus = 1
                mOpenLightBtn.setText(R.string.turn_off_the_light)
            } else {
                openLightStatus = 0
                mOpenLightBtn.setText(R.string.turn_on_the_light)
                // 关灯时自动关爆闪
                lightService.sharpFlash(0, false)
                sharpFlashStatus = 0
                mSharpFlashBtn.setText(R.string.explosive_flashing)
            }
        }
        mSharpFlashBtn.setOnClickListener {
            lightService.sharpFlash(if (sharpFlashStatus == 0) 1 else 0, false)
            // 如果是打开爆闪
            if (sharpFlashStatus == 0) {
                sharpFlashStatus = 1
                mSharpFlashBtn.setText(R.string.turn_off_explosion_flash)
            } else {// 如果是关闭爆闪
                sharpFlashStatus = 0
                mSharpFlashBtn.setText(R.string.explosive_flashing)
                // 关爆闪时自动关灯
                lightService.openLight(0, false)
                openLightStatus = 0
                mOpenLightBtn.setText(R.string.turn_on_the_light)
            }
        }
        mLuminanceChangeSeekbar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?, progress: Int, fromUser: Boolean
            ) {
                if (seekBar != null) {
                    lightService.luminanceChange(seekBar.progress, false)
                    mLightLumText.post {
                        mLightLumText.text = "${context!!.resources.getString(R.string.brightness_adjustment)}  ${seekBar.progress}"
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })
        // 定时获取温度
        timerTask = object : TimerTask() {
            override fun run() {
//                Log.i(
//                    TAG,
//                    "lightService.getIsConnected() = ${lightService.getIsConnected()}"
//                )
//                Log.i(TAG, "Date().time - lastRecvTime = ${Date().time - lastRecvTime}")
                if (lightService.getIsConnected() && Date().time - lastRecvTime > 6000) {
                    // 断连，重新连接
                    lightService.disConnect()
                }
                if (lightService.getIsConnected()) {
                    lightService.fetchTemperature()
                }
            }

        };
        timer.schedule(timerTask, 0, 2000)
        isFocusable = true
        setConnectState()
    }

    private fun showToast(msg: Int) {
        val handler = Handler(Looper.getMainLooper())

        handler.post {
            Toast.makeText(
                MApplication.applicationContext, msg, Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 定时器，判断连接状态
    private fun setConnectState(){
        val timer = Timer();
        val statusDot = findViewById<View>(R.id.statusDot)
        val background = statusDot.background as GradientDrawable
        val connectText = findViewById<TextView>(R.id.lightConnect)
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask(){
            override fun run() {
                if(lightService.getIsConnected()){
                    handler.post {
                        connectText.setText(R.string.connection_status_connected)
                        background.setColor(ContextCompat.getColor(context, R.color.green))
                    }
                }
                else{
                    handler.post {
                        connectText.setText(R.string.connection_status_notconnected)
                        background.setColor(ContextCompat.getColor(context, R.color.red))
                    }
                    // 尝试重连
                    if(!isConnecting) {
                        isConnecting = true
                        thread {
                            // 如果不是第一次连接，先休眠3秒，避免上一次连接内容还没清空
                            if(!isFirstConnect) {
                                Thread.sleep(3000)
                            }
                            isFirstConnect = false
                            while (!lightService.getIsConnected()) {
                                lightService.connect()
                                Thread.sleep(1000)
                            }
                            isConnecting = false
                            lastRecvTime = Date().time
                        }
                    }
                }
                Log.i(TAG, "灯光连接状态: ${lightService.getIsConnected()}")
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.schedule(task, 100, 1000);
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