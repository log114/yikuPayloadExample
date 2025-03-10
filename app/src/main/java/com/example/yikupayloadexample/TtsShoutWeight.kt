package com.example.yikupayloadexample

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import com.yiku.yikupayload_sdk.util.MsgCallback
import java.util.Timer
import java.util.TimerTask


class TtsShoutWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr)  {
    private val TAG = "RealTimeShoutWeight"
    private lateinit var mTemperature: TextView // 温度
    private lateinit var mStatus: TextView // 状态
    private lateinit var mTtsPlayBtn: Button;
    private lateinit var mTextView: EditText;
    private lateinit var mTtsLoopPlaybackCheckbox: CheckBox
    private lateinit var mBtnManVoice: RadioButton
    private lateinit var mBtnWomanVoice: RadioButton
    private var sharedPreferences: SharedPreferences? = null
    private var isPlaying = false
    private var voice: Int = 0
    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        initView(context)
        if(megaphoneService != null) {
            setCallbacks()
        }
        else {
            setCallbacksTask()
        }
    }

    private fun setCallbacks() {
        megaphoneService!!.msgCallbacks += object : MsgCallback {
            override fun getId(): String {
                return "RealTimeShoutWeightCallback"
            }

            override fun onMsg(msg: ByteArray) {
                Log.i(TAG, "msg:${msg.toHex()}")
                if (msg[0] != 0x8d.toByte()) {
                    return
                }
                if (msg[2] == 0x18.toByte()) {
                    Log.i(TAG, "recv 0x18!")
                    val handler = Handler(Looper.getMainLooper())
                    // 喊话器温度状态
                    handler.post {
                        updateTemperatureStatus(msg)
                    }
                }
            }
        }
    }

    // 定时器，判断 megaphoneService不为null时，调用setCallbaks
    private fun setCallbacksTask() {
        val timer = Timer();
        val task = object : TimerTask() {
            override fun run() {
                if(megaphoneService != null) {
                    setCallbacks()
                    timer.cancel()
                }
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.scheduleAtFixedRate(task, 100, 1000);
    }

    // 更新喊话器温度状态
    fun updateTemperatureStatus(msg: ByteArray) {
        Log.i(TAG, "喊话器温度msg:${msg.toHex()}")
        // 温度
        val temperature = (msg[0 + 3]).toUByte() - (50).toUByte()
        mTemperature.text = "${context.resources.getString(R.string.temperature)} ${temperature}℃"
        // 状态，0：正常，1：温度过高，喊话器不可用
        val status = msg[1 + 3]

        when (status) {
            0x00.toByte() -> {
                mStatus.text = "${context.resources.getString(R.string.state)} ${context.resources.getString(R.string.status_normal)}"
                mStatus.setTextColor(Color.WHITE)
            }

            0x01.toByte() -> {
                mStatus.text = "${context.resources.getString(R.string.state)} ${context.resources.getString(R.string.excessive_temperature)}"
                mStatus.setTextColor(Color.RED)
            }
        }
    }
    private fun sendText2Vehicle(
        loopPlayback: Boolean // 循环播放
    ) {
        // 获取文字
        var text = mTextView.text.toString()
        var translateText = text.replace(Regex("\\d")){
            when(it.value) {
                "0" -> context.resources.getString(R.string.zero)
                "1" -> context.resources.getString(R.string.one)
                "2" -> context.resources.getString(R.string.two)
                "3" -> context.resources.getString(R.string.three)
                "4" -> context.resources.getString(R.string.four)
                "5" -> context.resources.getString(R.string.five)
                "6" -> context.resources.getString(R.string.six)
                "7" -> context.resources.getString(R.string.seven)
                "8" -> context.resources.getString(R.string.eight)
                "9" -> context.resources.getString(R.string.nine)
                else -> it.value
            }
        }
        // 保存文字
        if (sharedPreferences != null) {
            val se = sharedPreferences!!.edit()
            se.putString("ttstext", text)
            se.apply()
        }
        if (!loopPlayback) {
            Log.i(TAG, "voice:${voice}")
            megaphoneService?.ttsV2(translateText, voice)
            return
        }

        if (isPlaying) {
            megaphoneService?.stopLoopTts()
            mTtsPlayBtn.setText(R.string.play)
            isPlaying = false
            return
        }
        /*
        循环播放
         */
        Log.i(TAG, "voice:${voice}")
        megaphoneService?.startLoopTtsV2(translateText, voice)
        mTtsPlayBtn.setText(R.string.stop_playing)
        isPlaying = true

    }

    private fun stopTTSPlay() {
        megaphoneService?.stopLoopTts()
        mTtsPlayBtn.setText(R.string.play)
        isPlaying = false
    }

    public fun onShowWeight() {
        if (sharedPreferences != null) {
            val ttstext: String = sharedPreferences!!.getString("ttstext", "")!!
            Log.i(TAG, "ttstext:$ttstext")
            mTextView.setText(ttstext)
        }
    }

    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.tts_shout_weight, this, true)
        mTemperature = findViewById(R.id.temperature)
        mStatus = findViewById(R.id.status)
        mTtsPlayBtn = this.findViewById(R.id.tts_play)
        mTextView = this.findViewById(R.id.tts_text)
        mBtnManVoice = this.findViewById(R.id.btn_man_voice)
        mBtnWomanVoice = this.findViewById(R.id.btn_woman_voice)
        mBtnManVoice.setOnCheckedChangeListener{_, checked ->
            run {
                if(checked){
                    Log.i(TAG, "set man voice....")
                    voice = 0
                }

            }
        }
        mBtnWomanVoice.setOnCheckedChangeListener{_, checked ->   run {
            if(checked){
                Log.i(TAG, "set woman voice....")
                voice = 1
            }
        }}
        mTtsLoopPlaybackCheckbox = this.findViewById(R.id.tts_loop_playback_checkbox)
        if (context != null) {
            sharedPreferences = context.getSharedPreferences("TtsShoutWeight", MODE_PRIVATE)
            val ttstext: String = sharedPreferences!!.getString("ttstext", "")!!
            Log.i(TAG, "ttstext:$ttstext")
            mTextView.setText(ttstext)
        }

        mTtsPlayBtn.setOnClickListener {
            if (isPlaying) {
                stopTTSPlay()
            } else {
                sendText2Vehicle(mTtsLoopPlaybackCheckbox.isChecked)
            }
        }

    }

    fun ByteArray.toHex(): String =
        joinToString(separator = "") { eachByte -> "%02x ".format(eachByte) }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }


}