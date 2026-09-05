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
import com.yiku.yikupayloadSDK.util.ProbeMixer16k
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@RequiresApi(Build.VERSION_CODES.S)
class RealTimeShoutWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
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
    private var isConnecting_1 = false; // 喊话器是否正在连接
    private var isConnecting_2 = false; // 四合一是否正在连接
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
    private var isAudioTrackReleased = AtomicBoolean(true)
    val probeMixer = ProbeMixer16k()
    // 创建一个专用于 AECM 处理的协程作用域
    private val aecmScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        initView(context)
    }

    fun setCallbacks() {
        megaphoneService!!.msgCallbacks += object : MsgCallback {
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
            if(megaphoneService?.isRecording == true){
                isStartSpeak = true
                mRealTimeSpeakBtn.setText(R.string.stop_speak)
            }
            else {
                megaphoneService?.stopRealTimeShout()
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
        isAudioTrackReleased.set(false)
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
        megaphoneService?.registMsgCallback(object : MsgCallback {
            val opusUtils = OpusUtils.getInstant()
            val createDecoder = opusUtils.createDecoder(radioRate, channels)// 新收音麦的数据opus编码使用的是16000采样率
            override fun getId(): String {
                return "radioCallback"
            }

            override fun onMsg(msg: ByteArray) {
                if (msg.size > 4 && String(msg.slice(0..3).toByteArray()) == "[40]") {
                    Log.i(TAG, "收音数据长度："+ msg.size)
                    if(!isRadio) {
                        Log.d(TAG, "收音已关闭")
                        return
                    }
                    try {
                        val data = ShortArray(frameSize)
                        val rc = opusUtils.decode(
                            createDecoder, msg.slice(4 until msg.size).toByteArray(), data
                        )
                        if (rc <= 0) return

                        // ★ 不在锁里做 PN 混合和参考帧入队
                        val pcmToPlay: ShortArray
                        if (isStartSpeak) {
                            pcmToPlay = probeMixer.mix(data)
                            // ★ 同步调用，不入协程（inputReferenceFrame 只是往队列放数据，很快）
                            megaphoneService?.inputReferenceFrame(pcmToPlay)
                        } else {
                            pcmToPlay = data
                        }

                        // ★ 只锁 write 操作
                        val written = synchronized(audioTrack) {
                            if (isAudioTrackReleased.get()) return
                            if (audioTrack.playState != AudioTrack.PLAYSTATE_PLAYING) {
                                audioTrack.play()
                            }
                            audioTrack.write(pcmToPlay, 0, rc)
                        }

                        if (written <= 0) {
                            Log.e(TAG, "AudioTrack写入失败: $written")
                            synchronized(audioTrack) {
                                if (!isAudioTrackReleased.get()) {
                                    audioTrack.stop()
                                    audioTrack.release()
                                    isAudioTrackReleased.set(true)
                                }
                            }
                            initAudioTrack()
                            audioTrack.play()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "音频处理异常", e)
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
                        synchronized(audioTrack) {
                            if (isAudioTrackReleased.get()) return

                            val written = if (isStartSpeak) {
                                val pcm16kWithPN = probeMixer.mix(data)
                                aecmScope.launch(Dispatchers.IO) {
                                    megaphoneService?.inputReferenceFrame(pcm16kWithPN)
                                }
                                audioTrack.write(pcm16kWithPN, 0, rc) //  用 rc 而不是 size
                            } else {
                                audioTrack.write(data, 0, rc)
                            }

                            if (written <= 0) {
                                Log.e(TAG, "AudioTrack写入失败: $written, 尝试重新初始化")
                                // 写入失败说明底层状态坏了，重新初始化
                                audioTrack.stop()
                                audioTrack.release()
                                isAudioTrackReleased.set(true)
                                initAudioTrack()
                                audioTrack.play()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "音频处理异常", e)
                    }
                }
            }
        })
        megaphoneService?.startRadio()
    }

    private fun stopRadio() {
        isRadio = false
        megaphoneService?.unRegistMsgCallback("radioCallback")
        isAudioTrackReleased.set(true)
        audioTrack.stop()
        audioTrack.release()
        megaphoneService?.stopRadio()
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
                megaphoneService?.disableRadio()
            } else {
                megaphoneService?.restartRadio()
            }
        }
        // 收音
        mRadioBtn.setOnClickListener {
            if (!isRadio) {
//                megaphoneService?.stopRealTimeShout()
//                if (isRadio) {
//                    megaphoneService?.stopRadio()
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
                isSettingVolume = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    megaphoneService?.setVolume(seekBar.progress)
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

            if (megaphoneService?.isRecording == true) {
                mRealTimeSpeakBtn.setText(R.string.start_speak)
                Log.i(TAG, "stopRecord...")
                stopForegroundService()
                megaphoneService?.stopRealTimeShout()
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
        megaphoneService?.onRecordingReady = {
            Handler(Looper.getMainLooper()).post {
                mRealTimeSpeakBtn.setText(R.string.stop_speak)
                mRealTimeSpeakBtn.isEnabled = true
            }
        }

        // 开始录音
        megaphoneService?.startRealTimeShout(isRadio)
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
                if (megaphoneService?.getIsConnected() == true || megaphoneService?.getIsConnectedYA3() == true) {
                    handler.post {
                        connectText.setText(R.string.connection_status_connected)
                        background.setColor(ContextCompat.getColor(context, R.color.green))
                    }
                    sharedPreferences.edit {
                        putBoolean("shoutConnectStatus", true);
                    }

                    if (!isInit && megaphoneService != null) {
                        isInit = true
                        setCallbacks()
                    }
                } else {
                    handler.post {
                        connectText.setText(R.string.connection_status_notconnected)
                        background.setColor(ContextCompat.getColor(context, R.color.red))
                    }
                    sharedPreferences.edit {
                        putBoolean("shoutConnectStatus", false);
                    }
                    if(!isConnecting_1) {
                        isConnecting_1 = true
                        // 尝试重连
                        val megaphoneService1: BaseMegaphoneService = MegaphoneService()// 喊话器
                        val host1 = preferences?.getString("ShoutHost", "")
                        if(host1 != null && "" != host1) {
                            megaphoneService1.setIp(host1)
                        }
                        thread {
                            Log.i(TAG, "喊话器连接："+ megaphoneService1.getIp())
                            megaphoneService1.connect()
                            if (megaphoneService1.getIsConnected()) {
                                megaphoneService = megaphoneService1;
                                setCallbacks()
                                megaphoneService?.setContext(context)
                            }
                            isConnecting_1 = false
                        }

                    }
                    if(!isConnecting_2) {
                        isConnecting_2 = true
                        val megaphoneService2: BaseMegaphoneService = FourInOneService()// 四合一
                        val host2 = preferences?.getString("YA3Host", "")
                        if (host2 != null && "" != host2) {
                            megaphoneService2.setIp(host2)
                        }
                        thread {
                            Log.i(TAG, "四合一连接：" + megaphoneService2.getIp())
                            megaphoneService2.connect()
                            if (megaphoneService2.getIsConnectedYA3()) {
                                megaphoneService = megaphoneService2;
                                setCallbacks()
                                megaphoneService?.setContext(context)
                            }
                            isConnecting_2 = false
                        }
                    }
                }
                if(megaphoneService?.getIsConnectedYA3() == true) {
                    handler.post {
                        mRadioBtn.visibility = VISIBLE;
                    }
                }
                else if(megaphoneService?.getIsConnected() == true) {
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
                if (megaphoneService != null && (megaphoneService?.getIsConnected() == true || megaphoneService?.getIsConnectedYA3() == true)) {
                    megaphoneService?.setVolume(mVolumeSeekBar.progress)// 设置默认音量
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
        megaphoneService?.stopRealTimeShout()
        // 其他清理工作
        stopRadio() // 确保停止收音
        megaphoneService?.releaseAudioResources() // 释放SDK音频资源
    }
}