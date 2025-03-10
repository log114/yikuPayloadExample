package com.example.yikupayloadexample

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioTrack.MODE_STREAM
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import com.yiku.yikupayload_sdk.service.BaseMegaphoneService
import com.yiku.yikupayload_sdk.service.FourInOneService
import com.yiku.yikupayload_sdk.service.MegaphoneService
import com.yiku.yikupayload_sdk.util.MsgCallback
import com.yiku.yikupayload_sdk.util.OpusUtils
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

class RealTimeShoutWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "RealTimeShoutWeight"
    private lateinit var mTemperature: TextView // 温度
    private lateinit var mStatus: TextView // 状态
    private lateinit var mRealTimeSpeakBtn: Button // 开始喊话按钮
    private lateinit var mVolumeSeekBar: SeekBar // 音量滑块
    private lateinit var mPlayAlarm: Button // 播放警报按钮
    private var isStartSpeak = false
    private var isPlayAlarm = false;
    private lateinit var mServoControlSeekbar: SeekBar // 舵机控制

    private lateinit var sharedPreferences: SharedPreferences


    private lateinit var mRadioBtn: Button
    private lateinit var audioTrack: AudioTrack
    private lateinit var mRadioDisable: Switch
    private var isRadio = false;
    private val sampleRate = 48000
    private val channels = 1
    private val frameSize = 960
    private val channelsConfig =
        AudioFormat.CHANNEL_OUT_MONO  // CHANNEL_OUT_MONO 单声道 CHANNEL_OUT_STEREO双声道

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        initView(context)
        if (megaphoneService != null) {
            setCallbacks()
        }
    }

    fun setCallbacks() {
        megaphoneService!!.msgCallbacks += object : MsgCallback {
            override fun getId(): String {
                return "RealTimeShoutWeightCallback"
            }

            override fun onMsg(msg: ByteArray) {


                if (msg.isNotEmpty() && msg[0] != 0x8d.toByte()) {
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

    // 更新喊话器温度状态
    fun updateTemperatureStatus(msg: ByteArray) {
        Log.i(TAG, "喊话器温度msg:${msg.toHex()}")
        // 温度
        val temperature = (msg[0 + 3]).toUByte() - 50.toUByte();

        mTemperature.text = "${context.resources.getString(R.string.temperature)} ${temperature}℃"
        // 状态，0：正常，1：温度过高，喊话器不可用
        val status = msg[1 + 3]

        when (status) {
            0x00.toByte() -> {
                mStatus.text =
                    "${context.resources.getString(R.string.state)} ${context.resources.getString(R.string.status_normal)}"
                mStatus.setTextColor(Color.WHITE)
            }

            0x01.toByte() -> {
                mStatus.text =
                    "${context.resources.getString(R.string.state)} ${context.resources.getString(R.string.excessive_temperature)}"
                mStatus.setTextColor(Color.RED)
            }
        }
    }

    private fun initStatus() {
        if (sharedPreferences.getBoolean("alar_status", false)) {
            isPlayAlarm = true
            mPlayAlarm.setText(R.string.stop_playing)
        }


        if (sharedPreferences.getBoolean("record", false)) {
            isStartSpeak = true
            mRealTimeSpeakBtn.setText(R.string.stop_speak)
        }
    }

    fun initAudioTrack() {
//        bufferSizeInBytes = AudioTrack.getMinBufferSize(8000, 1, AudioFormat.ENCODING_PCM_16BIT);
        val mMinBufferSize = AudioTrack.getMinBufferSize(
            sampleRate, channelsConfig, AudioFormat.ENCODING_PCM_16BIT
        );//计算最小缓冲区
        Log.i(TAG, "mMinBufferSize:${mMinBufferSize}")

        val audioFormat = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate).setChannelMask(channelsConfig).build()

        audioTrack =
            AudioTrack.Builder().setAudioFormat(audioFormat).setBufferSizeInBytes(mMinBufferSize)
                .setTransferMode(MODE_STREAM).build()


    }

    private fun stopRadio() {
        isRadio = false
        megaphoneService?.unRegistMsgCallback("radioCallback")
        audioTrack.stop()
//        audioTrack.release()
        megaphoneService?.stopRadio()
        mRadioBtn.setText(R.string.start_listening)
    }

    private fun initView(context: Context?) {

        LayoutInflater.from(context).inflate(R.layout.real_time_shout_weight, this, true)
        mTemperature = findViewById(R.id.temperature)
        mStatus = findViewById(R.id.status)
        mRealTimeSpeakBtn = findViewById(R.id.real_time_speak_btn)
        mVolumeSeekBar = findViewById(R.id.volume_seek_bar)
        mPlayAlarm = findViewById(R.id.play_alarm)
        mRadioBtn = findViewById(R.id.radio_btn)
        mRadioDisable = findViewById(R.id.radio_disable)
        setConnectState()
        setDefaultVolume()

        mRadioDisable.setOnClickListener{
            if (mRadioDisable.isChecked) {
                megaphoneService?.disableRadio()
            } else {
                megaphoneService?.restartRadio()
            }
        }
        mRadioBtn.setOnClickListener {
            if (!isRadio) {
//                megaphoneService?.stopRealTimeShout()
//                if (isRadio) {
//                    megaphoneService?.stopRadio()
//                }
                isRadio = true
                mRadioBtn.setText(R.string.stop_listening)

                initAudioTrack()
                audioTrack.play()
                megaphoneService?.registMsgCallback(object : MsgCallback {
                    private val buffer = ByteArray(1024) // 创建一个缓冲区，大小根据实际情况调整
                    private var bufferIndex = 0
                    val opusUtils = OpusUtils.getInstant()
                    val createDecoder = opusUtils.createDecoder(sampleRate, channels)
                    override fun getId(): String {
                        return "radioCallback"
                    }

                    override fun onMsg(msg: ByteArray) {
                        //                        Log.i(TAG, "header:${String(msg.slice(0..3).toByteArray())}")
                        if (msg.size > 4 && String(msg.slice(0..3).toByteArray()) == "[40]") {
                            val data = ShortArray(frameSize)
                            val rc = opusUtils.decode(
                                createDecoder, msg.slice(4 until msg.size).toByteArray(), data
                            )
                            // 接收到的是PCM音频

                            // 持续写入到一个文件中

                            // 转存 当点击停止收音，对保存的PCM音频文件进行wav编码

                            // 转存结束后删除原始PCM音频

                            // 新增一个按钮，可以打开保存音频文件路径

                            //                            Log.i(TAG, "data: size:${data.size} data:${data.asList()}")
                            //                            data = butterworthBandpassFilter.applyFilter(data)
                            //                            Log.i(TAG, "decode: size:${data.size} data:${data.asList()}")
                            audioTrack.write(data, 0, rc)
                        }
                    }
                })
                megaphoneService?.startRadio()
            } else {
                stopRadio()
            }

        }
        sharedPreferences =
            context?.getSharedPreferences("RealTimeShoutWeight", Context.MODE_PRIVATE)!!


        mServoControlSeekbar = findViewById(R.id.servo_control_seekbar)
        mServoControlSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Log.i(TAG, "seekBar:${seekBar?.progress?.toUInt()}")
                if (seekBar != null) {
                    Log.i(TAG, "(seekBar != null)")
                    Thread {
                        megaphoneService?.servoControl(seekBar.progress.toUInt())
//                        val msg = Msg()
//                        msg.msgId = 0x09.toByte()
//                        msg.payload = ByteArray(1)
//                        msg.payload[0] = seekBar.progress.toUInt().toByte()
//                        megaphoneService?.sendData2Payload(msg.getMsg())
//                        Log.i(TAG, "Thread start，msg:"+ bytesToHex(msg.getMsg()) )
                    }.start()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        mPlayAlarm.setOnClickListener {

            val edit = sharedPreferences.edit()
            if (megaphoneService?.isPlayAlarm != true) {
                // 播放警报
                megaphoneService?.playAlarm()
                mPlayAlarm.setText(R.string.stop_playing)
                edit.putBoolean("alar_status", true);
            } else {
                // 停止警报
                megaphoneService?.stopPlayAlarm()
                mPlayAlarm.setText(R.string.play_alarm)
                edit.putBoolean("alar_status", false);
            }
            edit.apply()

        }

        mVolumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    megaphoneService?.setVolume(seekBar.progress)

                }
            }

        })

        // 开始喊话按钮点击事件
        mRealTimeSpeakBtn.setOnClickListener {
            val edit = sharedPreferences.edit()
            Log.i(TAG, "isStartSpeak:${isStartSpeak}")

            // 判断是否在收音中，如果正在收音，关闭收音
//            if (isRadio) {
//                stopRadio()
//            }

            if (megaphoneService?.isRecording == true) {
                mRealTimeSpeakBtn.setText(R.string.start_speak)
                Log.i(TAG, "stopRecord...")
                megaphoneService?.stopRealTimeShout()
                edit.putBoolean("record", false)
            } else {
                Log.i(TAG, "startRecord...")
                mRealTimeSpeakBtn.setText(R.string.staring_speak)
                mRealTimeSpeakBtn.isEnabled = false
                megaphoneService?.startRealTimeShout(mRadioDisable.isEnabled)
                mRealTimeSpeakBtn.setText(R.string.stop_speak)
                mRealTimeSpeakBtn.isEnabled = true
                edit.putBoolean("record", true)
            }
            edit.apply()
            isStartSpeak = !isStartSpeak
        }
        initStatus()
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
    private fun setConnectState() {
        val timer = Timer();
        val connectText = findViewById<TextView>(R.id.realTimeShoutConnect)
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask() {
            override fun run() {
                if (megaphoneService?.getIsConnected() == true || megaphoneService?.getIsConnectedYA3() == true) {
                    handler.post {
                        connectText.setText(R.string.connection_status_connected)
                    }
                } else {
                    handler.post {
                        connectText.setText(R.string.connection_status_notconnected)
                    }
                    // 尝试重连
                    val megaphoneService1: BaseMegaphoneService = MegaphoneService()// 喊话器
                    val host1 = preferences?.getString("ShoutHost", "")
                    if(host1 != null && "" != host1) {
                        megaphoneService1.setIp(host1)
                    }
                    val megaphoneService2: BaseMegaphoneService = FourInOneService()// 四合一
                    val host2 = preferences?.getString("YA3Host", "")
                    if(host2 != null && "" != host2) {
                        megaphoneService1.setIp(host2)
                    }
                    thread {
                        megaphoneService1.connect()
                        if (megaphoneService1.getIsConnected()) {
                            megaphoneService = megaphoneService1;
                            setCallbacks()
                        }
                    }
                    thread {
                        megaphoneService2.connect()
                        if (megaphoneService2.getIsConnectedYA3()) {
                            megaphoneService = megaphoneService2;
                            setCallbacks()
                        }
                    }
                }
//                Log.i(TAG, "喊话器连接状态: ${megaphoneService?.getIsConnected()}")
//                Log.i(TAG, "四合一连接状态: ${megaphoneService?.getIsConnectedYA3()}")
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.scheduleAtFixedRate(task, 100, 1000);
    }

    // 设置默认音量
    private fun setDefaultVolume() {
        val timer = Timer();
        val task = object : TimerTask() {
            override fun run() {
                if (megaphoneService != null && (megaphoneService?.getIsConnected() == true || megaphoneService?.getIsConnectedYA3() == true)) {
                    megaphoneService?.setVolume(mVolumeSeekBar.progress)// 设置默认音量
                    Log.i(TAG, "设置默认音量：${mVolumeSeekBar.progress}")
                    timer.cancel()// 关闭定时器
                }
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.scheduleAtFixedRate(task, 100, 1000);
    }
}