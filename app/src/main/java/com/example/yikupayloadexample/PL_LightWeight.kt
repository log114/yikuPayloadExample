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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.example.yikupayloadexample.component.RoundMenuView
import com.yiku.yikupayloadSDK.externalService.PL_LightService
import com.yiku.yikupayloadSDK.util.MsgCallback
import java.util.Timer
import java.util.TimerTask

class PL_LightWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "PL_LightWeight"
    var plLightService: PL_LightService = PL_LightService()
    private lateinit var mLightSwitch: Switch
//    private lateinit var mTemperatureText: TextView
    private lateinit var mLuminanceSeekbar: SeekBar
    private var rotatingSpeed = 30
    private lateinit var mLuminanceText: TextView
    private lateinit var mFlashingFrequencySpinner: Spinner
    private lateinit var mFlashingSwitch: Switch
    private lateinit var mRotatingSpeedSeekbar: SeekBar
    private lateinit var mRotatingSpeedText: TextView
    private lateinit var mPtzControl: RoundMenuView
    private lateinit var mToCenterBtn: Button
    private lateinit var mDownwardBtn: Button
    private var isOpenLight: Boolean = false
    private var isFirstInit: Boolean = true

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        val host = preferences?.getString("PL_LightHost", "")
        if(host != null && "" != host) {
            plLightService.setIp(host)
        }
        initView(context)
        plLightService.registMsgCallback(LightMsgCallback())
    }

    inner class LightMsgCallback : MsgCallback {
        override fun getId(): String {
            return "PLLightMsgCallback"
        }

        override fun onMsg(msg: ByteArray) {
            Log.i(TAG, "recv: ${msg.asList()}")
//            if(msg.size > 3) {
//                if (msg[0] == 0x55.toByte() && msg[1] == 0xAA.toByte() && msg[2] == 0xDC.toByte()) {
//                    Log.i(TAG, "包头正确")
//                    val handler = Handler(Looper.getMainLooper())
//                    handler.post {
//                        // 灯定时上报状态
//                        if (msg[4] == 0x2D.toByte()) {
//                            if (isFirstInit) {
//                                isFirstInit = false
//                                if (msg[5] == 0x0F.toByte()) {
//                                    mLightSwitch.isChecked = true
//                                } else if (msg[5] == 0x1F.toByte()) {
//                                    mLightSwitch.isChecked = false
//                                }
//                                mLuminanceSeekbar.progress = msg[6].toInt()
//                            }
//                            mTemperatureText.text = "${msg[7].toInt()}°C"
//                            when (msg[8]) {
//                                0x02.toByte() -> mFlashingFrequencySpinner.setSelection(0)
//                                0x05.toByte() -> mFlashingFrequencySpinner.setSelection(1)
//                                0x0A.toByte() -> mFlashingFrequencySpinner.setSelection(2)
//                                0x0F.toByte() -> mFlashingFrequencySpinner.setSelection(3)
//                            }
//                        }
//                    }
//                }
//            }
        }

    }

    private fun initView(context: Context?){
        LayoutInflater.from(context).inflate(R.layout.light_weight_pl, this, true)
        mLightSwitch = findViewById<Switch>(R.id.lightSwitch)
//        mTemperatureText = findViewById<TextView>(R.id.temperatureText)
        mLuminanceSeekbar = findViewById<SeekBar>(R.id.luminanceSeekbar)
        mLuminanceText = findViewById<TextView>(R.id.luminanceText)
        mFlashingFrequencySpinner = findViewById<Spinner>(R.id.flashingFrequencySpinner)
        mFlashingSwitch = findViewById<Switch>(R.id.flashingSwitch)
        mRotatingSpeedSeekbar = findViewById<SeekBar>(R.id.rotatingSpeedSeekbar)
        mRotatingSpeedText = findViewById<TextView>(R.id.rotatingSpeedText)
        mPtzControl = findViewById<RoundMenuView>(R.id.ptzControl)
        mToCenterBtn = findViewById<Button>(R.id.toCenterBtn)
        mDownwardBtn = findViewById<Button>(R.id.downwardBtn)
        // 开灯关灯
        mLightSwitch.setOnClickListener {
            plLightService.openLight(mLightSwitch.isChecked)
            isOpenLight = mLightSwitch.isChecked
        }
        // 亮度调节
        mLuminanceSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mLuminanceText.text = "$progress%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                plLightService.luminanceChange(seekBar.progress)
            }
        })
        initFlashingFrequencySpinner() // 设置闪烁频率选项
        // 爆闪开关
        mFlashingSwitch.setOnClickListener {
            // 打开爆闪
            if(mFlashingSwitch.isChecked) {
                plLightService.startFlashing()
            }
            // 关闭爆闪（先关灯，再开灯，会关闭爆闪模式）
            else {
                plLightService.openLight(false)
                if(isOpenLight) {
                    plLightService.openLight(true)
                }
            }
        }
        // 设置云台转动速度
        mRotatingSpeedSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                rotatingSpeed = progress
                mRotatingSpeedText.text = "$progress°/s"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })
        initPtzControl() // 设置云台控制按键功能
        // 回中
        mToCenterBtn.setOnClickListener {
            plLightService.PTZToCenter()
        }
        // 向下
        mDownwardBtn.setOnClickListener {
            plLightService.PTZCtrlByAngle(0, -90, 0)
        }
        setConnectState()
    }

    private fun initFlashingFrequencySpinner() {
        val frequencyOptions = arrayOf("2Hz", "5Hz", "10Hz", "15Hz")

        // 使用最基础的构造函数
        val adapter = object : ArrayAdapter<String>(context, R.layout.spinner_item_custom) {
            override fun getCount(): Int = frequencyOptions.size
            override fun getItem(position: Int): String = frequencyOptions[position]
            override fun getItemId(position: Int): Long = position.toLong()
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mFlashingFrequencySpinner.adapter = adapter
        // 设置选项改变事件监听器
        mFlashingFrequencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // 根据选中的频率执行相应的操作
                when (position) {
                    0 -> {
                        // 2Hz 逻辑
                        plLightService.setFlashingFrequency(2)
                    }
                    1 -> {
                        // 5Hz 逻辑
                        plLightService.setFlashingFrequency(5)
                    }
                    2 -> {
                        // 10Hz 逻辑
                        plLightService.setFlashingFrequency(10)
                    }
                    3 -> {
                        // 15Hz 逻辑
                        plLightService.setFlashingFrequency(15)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 当没有选项被选中时调用（通常很少使用）
                Log.d("Spinner", "没有选择任何频率")
            }
        }
        // 最后设置默认选项（会触发一次onItemSelected事件）
        mFlashingFrequencySpinner.setSelection(2)
    }

    private fun initPtzControl() {
        val drawable = resources.getDrawable(R.drawable.right) // 替换成你的 Drawable 资源
        val bitmap = Bitmap.createBitmap(
            50, 50, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, 50, 50)
        drawable.draw(canvas)

        // 俯仰往下
        var roundMenu = RoundMenuView.RoundMenu()
        roundMenu.selectSolidColor = R.color.gray_9999
        roundMenu.strokeColor = R.color.gray_9999
        roundMenu.icon = bitmap
        roundMenu.onKeyDownListener = RoundMenuView.OnKeyDownListener {
            plLightService.PTZCtrlBySpeed(0, -rotatingSpeed, 0)
            Log.i(TAG, "下")
        }
        roundMenu.onKeyUpListener = RoundMenuView.OnKeyUpListener {
            plLightService.PTZCtrlBySpeed(0, 0, 0)
        }
        mPtzControl.addRoundMenu(roundMenu)

        // 偏航往左
        roundMenu = RoundMenuView.RoundMenu()
        roundMenu.selectSolidColor = R.color.gray_9999
        roundMenu.strokeColor = R.color.gray_9999
        roundMenu.icon = bitmap
        roundMenu.onKeyDownListener = RoundMenuView.OnKeyDownListener {
            plLightService.PTZCtrlBySpeed(-rotatingSpeed, 0, 0)
            Log.i(TAG, "左")
        }
        roundMenu.onKeyUpListener = RoundMenuView.OnKeyUpListener {
            plLightService.PTZCtrlBySpeed(0, 0, 0)
        }
        mPtzControl.addRoundMenu(roundMenu)

        // 俯仰往上
        roundMenu = RoundMenuView.RoundMenu()
        roundMenu.selectSolidColor = R.color.gray_9999
        roundMenu.strokeColor = R.color.gray_9999
        roundMenu.icon = bitmap
        roundMenu.onKeyDownListener = RoundMenuView.OnKeyDownListener {
            plLightService.PTZCtrlBySpeed(0, rotatingSpeed, 0)
            Log.i(TAG, "上")
        }
        roundMenu.onKeyUpListener = RoundMenuView.OnKeyUpListener {
            plLightService.PTZCtrlBySpeed(0, 0, 0)
        }
        mPtzControl.addRoundMenu(roundMenu)

        // 偏航往右
        roundMenu = RoundMenuView.RoundMenu()
        roundMenu.selectSolidColor = R.color.gray_9999
        roundMenu.strokeColor = R.color.gray_9999
        roundMenu.icon = bitmap
        roundMenu.onKeyDownListener = RoundMenuView.OnKeyDownListener {
            plLightService.PTZCtrlBySpeed(rotatingSpeed, 0, 0)
            Log.i(TAG, "右")
        }
        roundMenu.onKeyUpListener = RoundMenuView.OnKeyUpListener {
            plLightService.PTZCtrlBySpeed(0, 0, 0)
        }
        mPtzControl.addRoundMenu(roundMenu)
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
                if(plLightService.getIsConnected()){
                    handler.post {
                        connectText.setText(R.string.connection_status_connected)
                    }
                }
                else{
                    handler.post {
                        connectText.setText(R.string.connection_status_notconnected)
                    }
                    // 尝试重连
                    plLightService.connect()
                }
                Log.i(TAG, "灯光连接状态: ${plLightService.getIsConnected()}")
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.scheduleAtFixedRate(task, 100, 1000);
    }
}