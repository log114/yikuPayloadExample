package com.example.yikupayloadexample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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
import com.example.yikupayloadexample.component.RoundMenuView
import com.yiku.yikupayload_sdk.service.LightService
import com.yiku.yikupayload_sdk.util.MsgCallback
import java.util.Date
import java.util.Timer
import java.util.TimerTask


class LightWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "LightWeight"
    private lateinit var mLightBtn: ImageView
    var lightService: LightService = LightService()
    private var openLightStatus = 0
    private var sharpFlashStatus = 0
    private val timer: Timer = Timer()
    private lateinit var timerTask: TimerTask
    private lateinit var openInvertedModeCheckBox: CheckBox
    private lateinit var mLuminanceText: TextView
    private var pitchVal = 0
    private var yawVal = 0
    private val pitchMedianVal = 3000
    private val yawMedianVal = 17000
    private val pitchMax = 12000
    private val yawMax = 34000
    private lateinit var mPitchValueText: TextView
    private lateinit var mYawValueText: TextView
    private var lastRecvTime: Long = 0L


    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        val host = preferences?.getString("LightHost", "")
        if(host != null && "" != host) {
            lightService.setIp(host)
        }
        initView(context)
    }

    private fun yawReset() {
        yawVal = yawMedianVal
        sendTripodHead()
    }

    private fun pitchDown() {
        pitchVal = 0
        sendTripodHead()
    }

    private fun gimbalCentering() {
        yawVal = yawMedianVal
        pitchVal = 9000
        sendTripodHead()
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
//                Log.i(TAG, "查询温度: ${msg.asList()} ")
//                val ledLuminance = msg[3].toInt() - 50
//                val driverLuminance = msg[4].toInt() - 50
                if (mLuminanceText != null) {
//                    Log.i(TAG, "修改温度...")
                    mLuminanceText.post(Runnable {
                        changeLuminance(
                            (msg[4].toInt() and 0xFF) - 50, (msg[3].toInt() and 0xff) - 50
                        )
                    })
                }
            }
        }

    }

    fun changeLuminance(driverLuminance: Int, ledLuminance: Int) {
        mLuminanceText.text = "${context.resources.getString(R.string.drive_temperature)}: ${driverLuminance}℃ \n" + "${context.resources.getString(R.string.lamp_head_temperature)}: ${ledLuminance}℃"
    }

    fun sendTripodHead() {
        Log.i(
            TAG, "Picth:${pitchVal.toShort()}, Yaw:${yawVal.toShort()}"
        )
        val pv = pitchVal / 100 - 90
        val yv = yawVal / 100 - 170
        mPitchValueText.post {
            mPitchValueText.text = "${context.resources.getString(R.string.pitch)}:  ${pv}°"
            mYawValueText.text = "${context.resources.getString(R.string.course)}:  ${yv}°"
        }

        lightService.gimbalControl(
            pitchVal.toShort(), 9000, yawVal.toShort()
        )


//        lightService.tripodHead(
//            mSeekPicth.progress.toShort(),
//            mSeekRoll.progress.toShort(),
//            mSeekYaw.progress.toShort()
//        )
    }

    private fun initView(context: Context?){
        LayoutInflater.from(context).inflate(R.layout.light_weight, this, true)

        val drawable = resources.getDrawable(R.drawable.right) // 替换成你的 Drawable 资源
        val bitmap = Bitmap.createBitmap(
            50, 50, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, 50, 50)
        drawable.draw(canvas)

        val mLightLumText = findViewById<TextView>(R.id.light_lum_text)
        val mGlobalRoundMenu = findViewById<RoundMenuView>(R.id.global_round)
        val mOpenLightBtn = findViewById<Button>(R.id.open_light_btn)
        val mSharpFlashBtn = findViewById<Button>(R.id.sharp_flash_btn)
        val mLuminanceChangeSeekbar =
            findViewById<SeekBar>(R.id.luminance_change_seekbar)


        val mYawResetBtn = findViewById<ImageView>(R.id.yaw_reset_btn)
        val mPitchDownBtn = findViewById<ImageView>(R.id.pitch_down_btn)
        val mGimbalCenteringBtn = findViewById<ImageView>(R.id.gimbal_centering_btn)

        mYawResetBtn.setOnClickListener { yawReset() }
        mPitchDownBtn.setOnClickListener { pitchDown() }
        mGimbalCenteringBtn.setOnClickListener { gimbalCentering() }

        mPitchValueText = findViewById(R.id.pitch_value_text)
        mYawValueText = findViewById(R.id.yaw_value_text)
        openInvertedModeCheckBox = findViewById(R.id.open_inverted_mode)
        mLuminanceText = findViewById(R.id.luminance_text)
        // 初始化通讯回调函数
        lightService.registMsgCallback(LightMsgCallback())

        var roundMenu = RoundMenuView.RoundMenu()
        roundMenu.selectSolidColor = R.color.gray_9999
        roundMenu.strokeColor = R.color.gray_9999

        roundMenu.icon = bitmap
        roundMenu.onClickListener = View.OnClickListener {
            Log.i(TAG, "click 1")
            pitchVal -= 100
            var minPitchVal = 0
            // 倒装模式
            if(openInvertedModeCheckBox.isChecked) {
                minPitchVal = 6000
            }
            if (pitchVal < minPitchVal) {
                showToast(R.string.max_pitch)
                pitchVal = minPitchVal
                return@OnClickListener
            }
            sendTripodHead()
        }

        mGlobalRoundMenu.addRoundMenu(roundMenu)

        roundMenu = RoundMenuView.RoundMenu()
        roundMenu.selectSolidColor = R.color.gray_9999
        roundMenu.strokeColor = R.color.gray_9999
        roundMenu.icon = bitmap
        roundMenu.onClickListener = View.OnClickListener {
            Log.i(TAG, "click 2")

            yawVal -= 100
            if (yawVal < 0) {
                showToast(R.string.max_course)
                yawVal = 0
                return@OnClickListener

            }
            sendTripodHead()
        }
        mGlobalRoundMenu.addRoundMenu(roundMenu)

        roundMenu = RoundMenuView.RoundMenu()
        roundMenu.selectSolidColor = R.color.gray_9999
        roundMenu.strokeColor = R.color.gray_9999
        roundMenu.icon = bitmap
        roundMenu.onClickListener = View.OnClickListener {
            Log.i(TAG, "click 3")
            pitchVal += 100
            var maxPitchVal = pitchMax
            // 倒装模式
            if(openInvertedModeCheckBox.isChecked) {
                maxPitchVal = 18000
            }
            if (pitchVal > maxPitchVal) {
                showToast(R.string.max_pitch)
                pitchVal = maxPitchVal
                return@OnClickListener
            }
            sendTripodHead()
        }
        mGlobalRoundMenu.addRoundMenu(roundMenu)

        roundMenu = RoundMenuView.RoundMenu()
        roundMenu.selectSolidColor = R.color.gray_9999
        roundMenu.strokeColor = R.color.gray_9999
        roundMenu.icon = bitmap
        roundMenu.onClickListener = View.OnClickListener {
            Log.i(TAG, "click 4")
            yawVal += 100
            if (yawVal > yawMax) {
                showToast(R.string.max_course)
                yawVal = yawMax
                return@OnClickListener
            }
            sendTripodHead()
        }
        mGlobalRoundMenu.addRoundMenu(roundMenu)
        gimbalCentering()

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
                if (lightService.getIsConnected() && lastRecvTime != 0L && Date().time - lastRecvTime > 4000) {
                    // 断连，重新连接
                    lightService.reConnect()
                }
                if (lightService != null && lightService.getIsConnected()) {
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
        val connectText = findViewById<TextView>(R.id.lightConnect)
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask(){
            override fun run() {
                if(lightService.getIsConnected()){
                    handler.post {
                        connectText.setText(R.string.connection_status_connected)
                    }
                }
                else{
                    handler.post {
                        connectText.setText(R.string.connection_status_notconnected)
                    }
                    // 尝试重连
                    lightService.connect()
                }
                Log.i(TAG, "灯光连接状态: ${lightService.getIsConnected()}")
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.scheduleAtFixedRate(task, 100, 1000);
    }
}