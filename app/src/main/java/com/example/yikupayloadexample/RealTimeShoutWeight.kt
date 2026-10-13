package com.example.yikupayloadexample

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioTrack.MODE_STREAM
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.example.yikupayloadexample.MApplication.applicationContext
import com.yiku.yikupayloadSDK.service.BaseMegaphoneService
import com.yiku.yikupayloadSDK.service.FourInOneService
import com.yiku.yikupayloadSDK.service.MegaphoneService
import com.yiku.yikupayloadSDK.util.MsgCallback
import com.yiku.yikupayloadSDK.util.OpusUtils
import org.json.JSONException
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread
import androidx.core.content.edit

@RequiresApi(Build.VERSION_CODES.S)
class RealTimeShoutWeight(
    context: Context,
    attr: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val deviceIndex: Int = 0) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "RealTimeShoutWeight"
    private var isInit = false
    private lateinit var mTemperature: TextView // 温度
    private lateinit var mStatus: TextView // 状态
    private lateinit var mRealTimeSpeakBtn: Button // 开始喊话按钮
    private lateinit var mVolumeSeekBar: SeekBar // 音量滑块
    private lateinit var mPlayAlarm: Button // 播放警报按钮
    private var isStartSpeak = false
    private var isPlayAlarm = false;
    private lateinit var mServoControlSeekbar: SeekBar // 舵机控制
    private var isConnecting = false; // 是否正在连接
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var mRadioBtn: Button
    private lateinit var audioTrack: AudioTrack
    private lateinit var mRadioDisable: Switch
    private var isRadio = false;
    private val radioRate = 16000 // 新版收音麦opus编码采样率是16000
    private val channels = 1
    private val frameSize = 320
    private val channelsConfig =
        AudioFormat.CHANNEL_OUT_MONO  // CHANNEL_OUT_MONO 单声道 CHANNEL_OUT_STEREO双声道
    private var isForegroundServiceRunning = false
    private var isSettingVolume = false; // 是否正在设置音量
    private var volumeReal = 0;
    private var volumeLimit = 100
    private var temperature = "0"
    private var service: BaseMegaphoneService? = null

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        service = when(deviceIndex) {
            1 -> fourInOneService
            2 -> fourInOneService2
            else -> megaphoneService
        }
        initView(context)
    }

    fun setCallbacks() {
        service!!.msgCallbacks += object : MsgCallback {
            override fun getId(): String {
                return "RealTimeShoutWeightCallback"
            }

            override fun onMsg(msg: ByteArray) {
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
                // 温度、音量返回
                if (msg.size > 6 && String(msg.slice(0..3).toByteArray()) == "[99]") {
                    // 假设 msg 是一个 ByteArray
                    val dataLength = msg.size - 2 - 4
                    // 使用 Kotlin 的 sliceArray 方法提取子数组，更简洁
                    val valueBytes = msg.sliceArray(5 until 5 + dataLength)
                    // 将字节数组（ASCII字符）转换为字符串
                    val jsonString = valueBytes.toString(Charsets.US_ASCII)
                    try {
                        // 使用 Kotlin 标准库的 JSONObject 进行解析
                        val jsonObject = JSONObject(jsonString)
                        // 从JSON对象中提取数据
                        volumeReal = jsonObject.getInt("volume_real")
                        volumeLimit = jsonObject.getInt("volume_limit")
                        temperature = jsonObject.getString("temperature")
                        // 记录日志以便调试
                        Log.i(TAG, "解析结果 - 实际音量: $volumeReal, 音量上限: $volumeLimit, 温度: $temperature")
                        // 更新到主线程
                        val handler = Handler(Looper.getMainLooper())
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
                        }
                        // 将内容更新到内存里
                        sharedPreferences.edit {
                            putInt("volume_real", volumeReal);
                            putInt("volume_limit", volumeLimit);
                            putString("temperature", temperature);
                        }
                    } catch (e: JSONException) {
                        // 处理JSON解析错误（如键不存在、类型不匹配、格式错误等）
                        Log.e(TAG, "JSON解析失败: ${e.message}")
                    } catch (e: Exception) {
                        // 处理其他潜在异常
                        Log.e(TAG, "处理消息时发生未知错误: ${e.message}")
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

        // 如果缓存里的状态是正在喊话
        if (sharedPreferences.getBoolean("record", false)) {
            // 如果megaphoneService里的状态是未在喊话，可能是在喊话未关闭的情况下关闭了APP，停止喊话同步状态
            if(service?.isRecording == true){
                isStartSpeak = true
                mRealTimeSpeakBtn.setText(R.string.stop_speak)
            }
            else {
                service?.stopRealTimeShout()
                val edit = sharedPreferences.edit()
                edit.putBoolean("record", false)
                edit.apply()
            }
        }

        // 初始化的时候更新一次，避免和其他界面不一致
        volumeReal = sharedPreferences.getInt("volume_real", 0)
        volumeLimit = sharedPreferences.getInt("volume_limit", 100)
        temperature = sharedPreferences.getString("temperature", "0").toString()
        // 更新到主线程
        val handler = Handler(Looper.getMainLooper())
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
        }
    }

    private fun initAudioTrack() {
        val mMinBufferSize = AudioTrack.getMinBufferSize(
            radioRate, channelsConfig, AudioFormat.ENCODING_PCM_16BIT
        );//计算最小缓冲区
        Log.i(TAG, "mMinBufferSize:${mMinBufferSize}")

        val audioFormat = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(radioRate).setChannelMask(channelsConfig).build()

        audioTrack =
            AudioTrack.Builder().setAudioFormat(audioFormat).setBufferSizeInBytes(mMinBufferSize)
                .setTransferMode(MODE_STREAM).build()


    }

    private fun startRadio() {
        isRadio = true
        mRadioBtn.setText(R.string.stop_listening)

        initAudioTrack()
        audioTrack.play()
        service?.registMsgCallback(object : MsgCallback {
            val opusUtils = OpusUtils.getInstant()
            val createDecoder = opusUtils.createDecoder(radioRate, channels)// 新收音麦的数据opus编码使用的是16000采样率
            override fun getId(): String {
                return "radioCallback"
            }

            override fun onMsg(msg: ByteArray) {
                Log.i(TAG, "收音数据长度："+ msg.size)
                if (msg.size > 4 && String(msg.slice(0..3).toByteArray()) == "[40]") {
                    if(!isRadio) {
                        Log.d(TAG, "收音已关闭")
                        return
                    }
                    try {
                        val data = ShortArray(frameSize)
                        val rc = opusUtils.decode(
                            createDecoder, msg.slice(4 until msg.size).toByteArray(), data
                        )
                        // 检查AudioTrack状态
                        if (audioTrack.playState != AudioTrack.PLAYSTATE_PLAYING) {
                            Log.w(TAG, "AudioTrack未播放，尝试恢复")
                            audioTrack.play()
                        }

                        val written = audioTrack.write(data, 0, rc)
                        if (written <= 0) {
                            Log.e(TAG, "AudioTrack写入失败，错误码：$written")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "音频处理异常", e)
                    }
                }
            }
        })
        service?.startRadio()
    }

    private fun stopRadio() {
        isRadio = false
        service?.unRegistMsgCallback("radioCallback")
        audioTrack.stop()
        audioTrack.release()
        service?.stopRadio()
        mRadioBtn.setText(R.string.start_listening)
    }

    @RequiresApi(Build.VERSION_CODES.S)
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
                service?.disableRadio()
            } else {
                service?.restartRadio()
            }
        }
        // 收音
        mRadioBtn.setOnClickListener {
            if (!isRadio) {
//                service?.stopRealTimeShout()
//                if (isRadio) {
//                    service?.stopRadio()
//                }
                startRadio()
            } else {
                stopRadio()
            }

        }
        sharedPreferences = context?.getSharedPreferences("Megaphone", Context.MODE_PRIVATE)!!

        mServoControlSeekbar = findViewById(R.id.servo_control_seekbar)
        mServoControlSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Log.i(TAG, "seekBar:${seekBar?.progress?.toUInt()}")
                if (seekBar != null) {
                    Log.i(TAG, "(seekBar != null)")
                    Thread {
                        service?.servoControl(seekBar.progress.toUInt())
//                        val msg = Msg()
//                        msg.msgId = 0x09.toByte()
//                        msg.payload = ByteArray(1)
//                        msg.payload[0] = seekBar.progress.toUInt().toByte()
//                        service?.sendData2Payload(msg.getMsg())
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
            if (service?.isPlayAlarm != true) {
                // 播放警报
                service?.playAlarm()
                mPlayAlarm.setText(R.string.stop_playing)
                edit.putBoolean("alar_status", true);
            } else {
                // 停止警报
                service?.stopPlayAlarm()
                mPlayAlarm.setText(R.string.play_alarm)
                edit.putBoolean("alar_status", false);
            }
            edit.apply()

        }

        mVolumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSettingVolume = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    service?.setVolume(seekBar.progress)
                    Log.i(TAG, "音量设置，当前音量：${seekBar.progress}")
                    thread {
                        Thread.sleep(500)
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

        // 开始喊话按钮点击事件
        mRealTimeSpeakBtn.setOnClickListener {
            val edit = sharedPreferences.edit()
            Log.i(TAG, "isStartSpeak:${isStartSpeak}")

            // 判断是否在收音中，如果正在收音，关闭收音
//            if (isRadio) {
//                stopRadio()
//            }

            if (service?.isRecording == true) {
                mRealTimeSpeakBtn.setText(R.string.start_speak)
                Log.i(TAG, "stopRecord...")
                stopForegroundService()
                service?.stopRealTimeShout()
                edit.putBoolean("record", false)
            } else {
                // 麦克风权限检查
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                    showToast(R.string.lack_of_permissions)
                    return@setOnClickListener
                }

                // 启动前台服务
                startForegroundService()

                RecordingForegroundService.onServiceStarted = {
                    Handler(Looper.getMainLooper()).post {
                        // 服务已启动，开始录音
                        startRecordingProcess()
                    }
                }

                edit.putBoolean("record", true)
            }
            edit.apply()
            isStartSpeak = !isStartSpeak
        }
        mTemperature.text = "${context.resources.getString(R.string.temperature)} 0℃"
        initStatus()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecordingProcess() {
        mRealTimeSpeakBtn.setText(R.string.staring_speak)
        mRealTimeSpeakBtn.isEnabled = false

        // 设置录音准备回调
        service?.onRecordingReady = {
            Handler(Looper.getMainLooper()).post {
                mRealTimeSpeakBtn.setText(R.string.stop_speak)
                mRealTimeSpeakBtn.isEnabled = true
            }
        }

        // 开始录音
        service?.startRealTimeShout(mRadioDisable.isEnabled)
    }

    private fun startForegroundService() {
        if (isForegroundServiceRunning) return

        // 检查通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                showToast(R.string.need_notification_permission)
                // 引导用户到设置开启权限
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                context.startActivity(intent)
                return
            }
        }

        val serviceIntent = Intent(context, RecordingForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        isForegroundServiceRunning = true
    }

    private fun stopForegroundService() {
        if (!isForegroundServiceRunning) return

        // 使用明确的停止动作
        val serviceIntent = Intent(context, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_STOP
        }
        context.startService(serviceIntent) // 或者使用 stopService
        isForegroundServiceRunning = false
    }

    fun ByteArray.toHex(): String =
        joinToString(separator = "") { eachByte -> "%02x ".format(eachByte) }


    private fun showToast(toastMsg: Int) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(
                applicationContext,
                toastMsg,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showToast(msg: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(
                context, msg, Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 定时器，判断连接状态
    private fun setConnectState() {
        val timer = Timer();
        val statusDot = findViewById<View>(R.id.statusDot)
        val background = statusDot.background as GradientDrawable
        val connectText = findViewById<TextView>(R.id.realTimeShoutConnect)
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask() {
            override fun run() {
                if(service == null) {
                    service = when(deviceIndex) {
                        1 -> fourInOneService
                        2 -> fourInOneService2
                        else -> megaphoneService
                    }
                    Log.d(TAG, "service为空")
                    return
                }
                Log.d(TAG, "index=${deviceIndex}， ip=${service!!.getIp()}")
                if (service?.getIsConnected() == true || service?.getIsConnectedYA3() == true) {
                    handler.post {
                        connectText.setText(R.string.connection_status_connected)
                        background.setColor(ContextCompat.getColor(context, R.color.green))
                    }
                    sharedPreferences.edit {
                        putBoolean("shoutConnectStatus", true);
                    }

                    if (!isInit && service != null) {
                        isInit = true
                        setCallbacks()
                    }
                } else {
                    handler.post {
                        connectText.setText(R.string.connection_status_notconnected)
                        background.setColor(ContextCompat.getColor(context, R.color.red))
                    }

                    if(!isConnecting) {
                        isConnecting = true
                        thread {
                            Log.i(TAG, "喊话器、四合一连接：" + service!!.getIp())
                            service!!.connect()
                            if(deviceIndex == 0){
                                if (service!!.getIsConnected()) {
                                    setCallbacks()
                                    service?.setContext(context)
                                }
                            }
                            else {
                                if(deviceIndex == 2) {
                                    val YA3Host2 = preferences?.getString("YA3Host2", "")
                                    Log.i(TAG, "四合一2：" + YA3Host2)
                                    if(YA3Host2 != null && "" != YA3Host2) {
                                        service!!.setIp(YA3Host2)
                                    }
                                    else {
                                        isConnecting = false
                                        return@thread
                                    }
                                    Log.i(TAG, "四合一2：" + service!!.getIp())
                                }
                                if (service!!.getIsConnectedYA3()) {
                                    setCallbacks()
                                    service?.setContext(context)
                                }
                            }
                            isConnecting = false
                        }
                    }
                }
                if(service?.getIsConnectedYA3() == true) {
                    handler.post {
                        mRadioBtn.visibility = VISIBLE;
                    }
                }
                else if(service?.getIsConnected() == true) {
                    handler.post {
                        mRadioBtn.visibility = GONE;
                    }
                }
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.schedule(task, 100, 2000);
    }

    // 设置默认音量
    private fun setDefaultVolume() {
        val timer = Timer();
        val task = object : TimerTask() {
            override fun run() {
                if (service != null && (service?.getIsConnected() == true || service?.getIsConnectedYA3() == true)) {
                    service?.setVolume(mVolumeSeekBar.progress)// 设置默认音量
                    Log.i(TAG, "设置默认音量：${mVolumeSeekBar.progress}")
                    timer.cancel()// 关闭定时器
                }
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.schedule(task, 100, 1000);
    }

    // 释放资源
    fun releaseResources() {
        stopForegroundService()
        service?.stopRealTimeShout()
        // 其他清理工作
        stopRadio() // 确保停止收音
        service?.releaseAudioResources() // 释放SDK音频资源
    }
}