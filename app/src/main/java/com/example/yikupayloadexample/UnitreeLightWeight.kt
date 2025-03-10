package com.example.yikupayloadexample;

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.yiku.yikupayload_sdk.util.MsgCallback

import java.util.Timer
import java.util.TimerTask

class UnitreeLightWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "UnitreeLightWeight"
    private lateinit var mLightView: View
    private var openLightStatus = 0
    private var sharpFlashStatus = 0
    private val timer: Timer = Timer()
    private lateinit var timerTask: TimerTask;
    private lateinit var mLuminanceText: TextView
    private lateinit var mLuminanceChangePitch: SeekBar // 舵机控制
    private var rbModel = 0

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        initView(context)
    }

    fun changeLuminance(driverLuminance: Int, ledLuminance: Int) {
        mLuminanceText.text = "驱动温度: ${driverLuminance}℃ \n" + "灯头温度: ${ledLuminance}℃"
    }

    inner

    class LightMsgCallback : MsgCallback {
        private val TAG = "LightMsgCallback"
        override fun getId(): String {
            return "LightMsgCallback"
        }

        override fun onMsg(msg: ByteArray) {
            if (msg[0] != 0x8d.toByte()) {
                return
            }
            Log.i(TAG, "recv: ${msg.asList()}")
            if (msg[2] == 0x04.toByte()) {
//                Log.i(TAG, "查询温度: ${msg.asList()} ")
//                val ledLuminance = msg[3].toInt() - 50
//                val driverLuminance = msg[4].toInt() - 50
                if (mLuminanceText != null) {
//                    Log.i(TAG, "修改温度...")
                    mLightView.post(Runnable {
                        changeLuminance(msg[4].toInt() - 50, msg[3].toInt() - 50)
                    })
                }
            }
        }

    }

    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.light_weight_ya3, this, true)
        mLightView = findViewById(R.id.ya3_light_view)
        val mOpenLightBtn = findViewById<Button>(R.id.open_light_btn)
        val mSharpFlashBtn = findViewById<Button>(R.id.sharp_flash_btn)
        val mLuminanceChangeSeekbar = findViewById<SeekBar>(R.id.luminance_change_seekbar)

        val mCloseRbBtn: Button = findViewById(R.id.close_rb_btn)
        val mOpenRbBtn: Button = findViewById(R.id.open_rb_btn)
        val mRbModelText: TextView = findViewById(R.id.rb_model_text)

        mLuminanceChangePitch = findViewById(R.id.luminance_change_pitch)
        mLuminanceChangePitch.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (seekBar != null) {
                    megaphoneService?.controlServo(seekBar.progress + 100)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
//                if (seekBar != null) {
//                    megaphoneService.setVolume(seekBar.progress)
//                }
            }

        })
        // 初始化通讯回调函数
        megaphoneService?.registMsgCallback(LightMsgCallback())


        // 查询初始化状态
        megaphoneService?.openLight(0, true) // 开灯状态
        megaphoneService?.sharpFlash(0, true) // 爆闪状态
        megaphoneService?.fetchTemperature() // 温度
        setConnectState()
        fun close_light() {
            megaphoneService?.openLight(0, false)
            megaphoneService?.sharpFlash(0, false)
            openLightStatus = 0
            mOpenLightBtn.setText(R.string.turn_on_the_light)
            sharpFlashStatus = 0
            mSharpFlashBtn.setText(R.string.explosive_flashing)
        }
        mOpenLightBtn.setOnClickListener {
//            close_light()
            megaphoneService?.openLight(if (openLightStatus == 0) 1 else 0, false)
            if (openLightStatus == 0) {
                openLightStatus = 1
                mOpenLightBtn.setText(R.string.turn_off_the_light)
            } else {
                openLightStatus = 0
                mOpenLightBtn.setText(R.string.turn_on_the_light)
                // 关灯时自动关爆闪
                megaphoneService?.sharpFlash(0, false)
                sharpFlashStatus = 0
                mSharpFlashBtn.setText(R.string.explosive_flashing)
            }
        }
        mSharpFlashBtn.setOnClickListener {
            megaphoneService?.sharpFlash(if (sharpFlashStatus == 0) 1 else 0, false)
            // 如果是打开爆闪
            if (sharpFlashStatus == 0) {
                sharpFlashStatus = 1
                mSharpFlashBtn.setText(R.string.turn_off_explosion_flash)
            } else {// 如果是关闭爆闪
                sharpFlashStatus = 0
                mSharpFlashBtn.setText(R.string.explosive_flashing)
                // 关爆闪时，自动关灯
                megaphoneService?.openLight(0, false)
                openLightStatus = 0
                mOpenLightBtn.setText(R.string.turn_on_the_light)
            }
        }
        mLuminanceChangeSeekbar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?, progress: Int, fromUser: Boolean
            ) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    megaphoneService?.luminanceChange(seekBar.progress, false)

                }
            }

        })
        @SuppressLint("SetTextI18n")
        fun setRbModel(vrbModel: Int) {
//                        rbModel = 0
            if (vrbModel == 0) {
                megaphoneService?.redBlueLedControl(0x00)
                mRbModelText.setText(R.string.red_and_blue_not_turned_on)
            }
            if (vrbModel == 1) {
                megaphoneService?.redBlueLedControl(0x01)
            }
            if (vrbModel == 2) {
                megaphoneService?.redBlueLedControl(0x02)
            }
            if (vrbModel == 3) {
                megaphoneService?.redBlueLedControl(0x03)
            }
            if (vrbModel == 4) {
                megaphoneService?.redBlueLedControl(0x04)
            }
            if (vrbModel == 5) {
                megaphoneService?.redBlueLedControl(0x05)
            }
            if (vrbModel == 6) {
                megaphoneService?.redBlueLedControl(0x06)
            }
            if (vrbModel == 7) {
                megaphoneService?.redBlueLedControl(0x07)
            }
            if (vrbModel == 8) {
                megaphoneService?.redBlueLedControl(0x08)
            }
            if (vrbModel == 9) {
                megaphoneService?.redBlueLedControl(0x09)
            }
            if (vrbModel == 10) {
                megaphoneService?.redBlueLedControl(0x0a)
            }
            if (vrbModel == 11) {
                megaphoneService?.redBlueLedControl(0x0b)
            }
            if (vrbModel == 12) {
                megaphoneService?.redBlueLedControl(0x0c)
            }
            if (vrbModel == 13) {
                megaphoneService?.redBlueLedControl(0x0d)
            }
            if (vrbModel == 14) {
                megaphoneService?.redBlueLedControl(0x0e)
            }
            if (vrbModel == 15) {
                megaphoneService?.redBlueLedControl(0x0f)
            }
            if (vrbModel == 16) {
                megaphoneService?.redBlueLedControl(0x10)
            }
            rbModel = vrbModel
            if (rbModel > 0) {
                mRbModelText.setText(R.string.mode)
                mRbModelText.text = "${mRbModelText.text}${rbModel}"
            }
        }
        mCloseRbBtn.setOnClickListener {
            setRbModel(0)

        }
        mOpenRbBtn.setOnClickListener {
            if (rbModel + 1 > 16) {
                rbModel = 0
            }
            setRbModel(rbModel + 1)
        }


        // 定时获取温度
        timerTask = object : TimerTask() {
            override fun run() {
                if (megaphoneService != null && megaphoneService?.getIsConnectedYA3() == true) {
                    megaphoneService?.fetchTemperature()
                }
            }
        }
        timer.schedule(timerTask, 0, 2000)
        isFocusable = true
    }

    fun ByteArray.toHex(): String =
        joinToString(separator = "") { eachByte -> "%02x ".format(eachByte) }


    private fun showToast(msg: String) {
        (context as Activity).runOnUiThread {
            Toast.makeText(
                context, msg, Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 定时器，判断连接状态
    private fun setConnectState(){
        val timer = Timer();
        val connectText = findViewById<TextView>(R.id.lightConnectYA3)
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask(){
            override fun run() {
                if(megaphoneService?.getIsConnectedYA3() == true){
                    handler.post {
                        connectText.setText(R.string.connection_status_connected)
                    }
                }
                else{
                    handler.post {
                        connectText.setText(R.string.connection_status_notconnected)
                    }
                    // 此处不重连，因为是四合一，重连部分在RealTimeShoutWeight
                }
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.scheduleAtFixedRate(task, 100, 1000);
    }
}