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
import com.yiku.yikupayloadSDK.util.MsgCallback
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread


class TtsShoutWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr)  {
    private val TAG = "TtsShoutWeight"
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
    private lateinit var mVolumeSeekBar: SeekBar // 音量滑块
    private var isSettingVolume = false; // 是否正在设置音量
    private var isGetCurrentVolume = false; // 是否返回了当前实际音量
    private var currentVolume = 0; // 当前实际音量

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        initView(context)
        setCallbacksTask()
    }

    private fun setCallbacks() {
        megaphoneService!!.msgCallbacks += object : MsgCallback {
            override fun getId(): String {
                return "TtsShoutWeightCallback"
            }

            override fun onMsg(msg: ByteArray) {
                Log.i(TAG, "msg:${msg.toHex()}")
                if (msg.isNotEmpty() && msg[0] == 0x8d.toByte()) {
                    if (msg[2] == 0x18.toByte()) {
                        Log.i(TAG, "recv 0x18!")
                        val handler = Handler(Looper.getMainLooper())
                        // 喊话器温度状态
                        handler.post {
                            updateTemperatureStatus(msg)
                        }
                    }
                    return
                }
                if (msg.size > 6 && String(msg.slice(0..3).toByteArray()) == "[14]") {
                    // 假设 msg 是一个 ByteArray
                    val dataLength = msg.size - 2 - 4
                    // 使用 Kotlin 的 sliceArray 方法提取子数组，更简洁
                    val valueBytes = msg.sliceArray(5 until 5 + dataLength)

                    // 将字节数组（ASCII字符）转换为字符串
                    val hexString = valueBytes.toString(Charsets.US_ASCII)
                    try {
                        // 关键：使用字符串的 toInt(16) 方法进行十六进制解析
                        val result = hexString.toInt(16).toByte()
                        Log.i(TAG, "提取到的数值为: 0x${result.toString(16).padStart(2, '0')} (十进制${result.toUByte().toInt()})")
                        isGetCurrentVolume = true
                        currentVolume = result.toUByte().toInt()
                        // 如果不是正在设置音量的时候，收单音量生效数据
                        if(!isSettingVolume) {
                            mVolumeSeekBar.post {
                                mVolumeSeekBar.progress = currentVolume
                                isGetCurrentVolume = false
                            }
                        }
                    } catch (e: NumberFormatException) {
                        Log.e(TAG, "十六进制数据格式错误！")
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "数值超出字节范围(0-255)！")
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
                if(megaphoneService?.getIsConnected() == true || megaphoneService?.getIsConnectedYA3() == true) {
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

    private fun initView(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.tts_shout_weight, this, true)
        mTemperature = findViewById(R.id.temperature)
        mStatus = findViewById(R.id.status)
        mTtsPlayBtn = this.findViewById(R.id.tts_play)
        mTextView = this.findViewById(R.id.tts_text)
        mBtnManVoice = this.findViewById(R.id.btn_man_voice)
        mBtnWomanVoice = this.findViewById(R.id.btn_woman_voice)
        mVolumeSeekBar = findViewById(R.id.volume_seek_bar)
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
        mVolumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSettingVolume = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    megaphoneService?.setVolume(seekBar.progress)
                    Log.i(TAG, "音量设置，当前音量：${seekBar.progress}")
                    thread {
                        Thread.sleep(500)
                        mVolumeSeekBar.post {
                            if (isGetCurrentVolume && currentVolume < seekBar.progress) {
                                seekBar.progress = currentVolume
                                isGetCurrentVolume = false
                                showToast(context.resources.getString(R.string.high_temperature_protection) + currentVolume + "%")
                            }
                            isSettingVolume = false
                        }
                    }
                }
            }

        })
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