package com.example.yikupayloadexample

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.yiku.yikupayloadSDK.util.MsgCallback
import org.json.JSONException
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread


class TtsShoutWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr)  {
    private val TAG = "TtsShoutWeight"
    private lateinit var mTemperature: TextView // 温度
    private lateinit var mStatus: TextView // 状态
    private lateinit var mTtsPlayBtn: Button;
    private lateinit var mTtsStopBtn: Button
    private lateinit var mTextView: EditText;
    private lateinit var mTtsLoopPlaybackSwitch: Switch
    private lateinit var mBtnManVoice: RadioButton
    private lateinit var mBtnWomanVoice: RadioButton
    private var sharedPreferences: SharedPreferences? = null
    private var isPlaying = false
    private var voice: Int = 0
    private lateinit var mVolumeSeekBar: SeekBar // 音量滑块
    private lateinit var mSpeechRateLine: LinearLayout
    private lateinit var mSpeechRateSeekBar: SeekBar // 语速滑块
    private var isSettingVolume = false; // 是否正在设置音量
    private var hasNewVolumeSetting = false
    private var volumeReal = 0;
    private var volumeLimit = 100
    private var temperature = "0"

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        initView(context)
        setCallbacksTask()
    }

    // 定时器，同步更新音量、温度等
    private fun setCallbacksTask() {
        val statusDot = findViewById<View>(R.id.statusDot)
        val background = statusDot.background as GradientDrawable
        val connectText = findViewById<TextView>(R.id.realTimeShoutConnect)
        val timer = Timer();
        val task = object : TimerTask() {
            override fun run() {
                val handler = Handler(Looper.getMainLooper())
                // 每秒从内存里拿出状态消息更新
                if (sharedPreferences != null) {
                    volumeReal = sharedPreferences!!.getInt("volume_real", 0)
                    volumeLimit = sharedPreferences!!.getInt("volume_limit", 100)
                    temperature = sharedPreferences!!.getString("temperature", "0").toString()
                    val connectStatus = sharedPreferences!!.getBoolean("shoutConnectStatus", false)
                    // 更新到主线程
                    handler.post {
                        // 更新音量进度条
                        if (!isSettingVolume) {
                            mVolumeSeekBar.progress = volumeReal
                        }
                        mTemperature.text = "${context.resources.getString(R.string.temperature)} ${temperature}℃"
                        if(volumeLimit < 100) {
                            mStatus.setText(R.string.excessive_temperature)
                            mStatus.setTextColor(Color.RED)
                        }
                        else {
                            mStatus.setText(R.string.normal_temperature)
                            mStatus.setTextColor(Color.WHITE)
                        }

                        if(connectStatus) {
                            connectText.setText(R.string.connection_status_connected)
                            background.setColor(ContextCompat.getColor(context, R.color.green))
                        }
                        else {
                            connectText.setText(R.string.connection_status_notconnected)
                            background.setColor(ContextCompat.getColor(context, R.color.red))
                        }
                    }
                }
                // 根据连接状态，判断在连接的是四合一的时候，显示语速调整组件
                if(megaphoneService?.getIsConnectedYA3() == true) {
                    handler.post {
                        mSpeechRateLine.visibility = VISIBLE;
                    }
                }
                else if(megaphoneService?.getIsConnected() == true) {
                    handler.post {
                        mSpeechRateLine.visibility = GONE;
                    }
                }
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.schedule(task, 100, 500);
    }

    private fun sendText2Vehicle(
        loopPlayback: Boolean // 循环播放
    ) {
        // 获取文字
        val text = mTextView.text.toString()
        val translateText = text.replace(Regex("\\d")){
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
            isPlaying = false
            return
        }
        /*
        循环播放
         */
        Log.i(TAG, "voice:${voice}")
        megaphoneService?.startLoopTtsV2(translateText, voice)
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

    private fun initView(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.tts_shout_weight, this, true)
        mTemperature = findViewById(R.id.temperature)
        mStatus = findViewById(R.id.status)
        mTtsPlayBtn = this.findViewById(R.id.tts_play)
        mTtsStopBtn = this.findViewById(R.id.tts_stop)
        mTextView = this.findViewById(R.id.tts_text)
        mBtnManVoice = this.findViewById(R.id.btn_man_voice)
        mBtnWomanVoice = this.findViewById(R.id.btn_woman_voice)
        mVolumeSeekBar = findViewById(R.id.volume_seek_bar)
        mSpeechRateSeekBar = findViewById(R.id.speech_rate_seek_bar)
        mSpeechRateLine = findViewById(R.id.speechRateLine)
        val hintText = context.resources.getString(R.string.place_input_text)
        val spannable = SpannableString(hintText)
        spannable.setSpan(
            AbsoluteSizeSpan(12, true), // 第二个参数 true 表示单位是 sp
            0,
            hintText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        mTextView.hint = spannable

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
        mTtsLoopPlaybackSwitch = this.findViewById(R.id.tts_loop_playback)
        sharedPreferences = context.getSharedPreferences("Megaphone", MODE_PRIVATE)
        val ttstext: String = sharedPreferences!!.getString("ttstext", "")!!
        Log.i(TAG, "ttstext:$ttstext")
        mTextView.setText(ttstext)

        mTtsPlayBtn.setOnClickListener {
            if (isPlaying) {
                stopTTSPlay()
                Thread.sleep(100)
            }
            sendText2Vehicle(mTtsLoopPlaybackSwitch.isChecked)
        }
        mTtsStopBtn.setOnClickListener {
            stopTTSPlay()
        }
        mVolumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSettingVolume = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    hasNewVolumeSetting = true
                    megaphoneService?.setVolume(seekBar.progress)
                    Log.i(TAG, "音量设置，当前音量：${seekBar.progress}")
                    thread {
                        hasNewVolumeSetting = false
                        Thread.sleep(1000)
                        if(hasNewVolumeSetting) { // 如果这1秒内，又做了更改，退出本线程，留后面的线程处理
                            return@thread
                        }
                        mVolumeSeekBar.post {
                            if (seekBar.progress > volumeLimit) {
                                seekBar.progress = volumeReal
                                showToast(context.resources.getString(R.string.high_temperature_protection) + volumeLimit + "%")
                            }
                            isSettingVolume = false
                        }
                    }
                }
            }

        })
        mSpeechRateSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    megaphoneService?.setTtsSpeechRate(seekBar.progress)
                    Log.i(TAG, "语速设置，当前语速：${seekBar.progress}")
                }
            }

        })
        mTemperature.text = "${context.resources.getString(R.string.temperature)} 0℃"
    }

    fun ByteArray.toHex(): String =
        joinToString(separator = "") { eachByte -> "%02x ".format(eachByte) }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }

    private fun showToast(msg: String) {
        Toast.makeText(
            context,
            msg,
            Toast.LENGTH_SHORT
        ).show()
    }

}