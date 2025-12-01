package com.example.yikupayloadexample

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.example.yikupayloadexample.MApplication.applicationContext
import com.example.yikupayloadexample.component.AudioListAdapter
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread
import androidx.core.content.edit
import com.yiku.yikupayloadSDK.protocol.ALLINONE_PITCH_STATE
import com.yiku.yikupayloadSDK.protocol.ALLINONE_STATE
import com.yiku.yikupayloadSDK.util.MsgCallback

class AllInOneSpeakerWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)
    private val TAG = "AllInOneSpeakerWeight"
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var realTimeSpeakBtn: Button
    private lateinit var playAlarmBtn: Button
    private lateinit var mConnectState: TextView
    private lateinit var ttsText: EditText
    private lateinit var ttsPlayBtn: Button
    private lateinit var ttsRadioGroup: RadioGroup
    private lateinit var ttsLoopPlaybackCheckbox: CheckBox
    private lateinit var audioPlayBtn: Button
    private lateinit var audioStopPlayBtn: Button
    private lateinit var audioLoopPlayBtn: Button
    private lateinit var addRecordBtn: Button
    private lateinit var delAudioBtn: Button
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var volumeText: TextView
    private lateinit var pitchSeekBar: SeekBar
    private lateinit var pitchText: TextView
    private var isConnecting: Boolean = false
    private var updateTime = Date().time
    private var isForegroundServiceRunning = false
    private var isStartSpeak = false
    private var isPlayAlarm = false;
    private var isLoopTTSPlaying = false
    private var isConnectingPtz: Boolean = false
    private var isSettingPitch: Boolean = false

    private lateinit var audioListView: ListView
    private var adapter: AudioListAdapter? = null
    // 初始化数据
    private var audioItems: MutableList<String> = mutableListOf()

    init {
        initView(context)
        // 消息订阅
        allInOneService.registMsgCallback(object : MsgCallback {
            override fun getId(): String {
                return "AllInOneSpeakerWeightCallback"
            }
            override fun onMsg(msg: ByteArray) {
                if (msg[0] != 0x8d.toByte()) {
                    return
                }
                if (msg[2] == ALLINONE_STATE.toByte()) {
                    // 更新状态
                    updateTime = Date().time
                }
            }
        })
        // 云台消息订阅
        allInOneService.registPtzMsgCallback(object : MsgCallback {
            override fun getId(): String {
                return "AllInOneSpeakerWeightPTZCallback"
            }
            override fun onMsg(msg: ByteArray) {
                if (msg[0] != 0x8d.toByte()) {
                    return
                }
                if (msg[2] == ALLINONE_PITCH_STATE.toByte()) {
                    if(isSettingPitch) {
                        return
                    }
                    // 俯仰值，0-900
                    val pitchValue = ((msg[3].toInt()  and 0xFF) shl 8) or (msg[4].toInt()  and 0xFF)
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        pitchSeekBar.progress = pitchValue
                        pitchText.text = "${pitchValue/10}°"
                    }
                }
            }
        })

        sharedPreferences = context.getSharedPreferences("RealTimeShoutWeight", Context.MODE_PRIVATE)

        // 实时喊话
        realTimeSpeakBtn.setOnClickListener {
            val edit = sharedPreferences.edit()
            Log.i(TAG, "isStartSpeak:${isStartSpeak}")
            if (allInOneService.isRecording) {
                realTimeSpeakBtn.setText(R.string.start_speak)
                Log.i(TAG, "stopRecord...")
                stopForegroundService()
                allInOneService.stopRealTimeShout()
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
        // 播放警报
        playAlarmBtn.setOnClickListener {
            val edit = sharedPreferences.edit()
            if (!allInOneService.isPlayAlarm) {
                // 播放警报
                allInOneService.playAlarm()
                playAlarmBtn.setText(R.string.stop_playing)
                edit.putBoolean("alar_status", true);
            } else {
                // 停止警报
                allInOneService.stopPlayAlarm()
                playAlarmBtn.setText(R.string.play_alarm)
                edit.putBoolean("alar_status", false);
            }
            edit.apply()

        }
        // 文字转语音
        ttsPlayBtn.setOnClickListener {
            if(isLoopTTSPlaying) {
                allInOneService.stopLoopTts()
                ttsPlayBtn.setText(R.string.play)
                isLoopTTSPlaying = false
            }
            else {
                // 获取文字
                var text = ttsText.text.toString()
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
                sharedPreferences.edit {
                    putString("ttstext", text)
                }
                val voice = when(ttsRadioGroup.checkedRadioButtonId) {
                    R.id.btn_man_voice -> 0
                    R.id.btn_woman_voice -> 1
                    else -> 0
                }
                if(ttsLoopPlaybackCheckbox.isChecked) {
                    isLoopTTSPlaying = true
                    allInOneService.startLoopTtsV2(translateText, voice)
                    ttsPlayBtn.setText(R.string.stop_playing)
                }
                else {
                    allInOneService.ttsV2(translateText, voice)
                }
            }
        }

        // 音量控制
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                volumeText.text = "${seekBar.progress}%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                allInOneService.setVolume(seekBar.progress)
            }
        })

        // 俯仰控制
        pitchSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                pitchText.text = "${seekBar.progress/10}°"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isSettingPitch = true
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                allInOneService.pitchControl(seekBar.progress)
                Log.i(TAG, "音量设置，当前音量：${seekBar.progress}")
                // 延迟一下，避免设置还未生效，导致滑条往回跳
                thread {
                    Thread.sleep(1000)
                    isSettingPitch = false
                }
            }
        })

        // 播放音频文件
        audioPlayBtn.setOnClickListener {
            val selectedAudio = adapter?.getSelectedItem()
            val selectPositon = adapter?.getSelectedPosition()
            if (selectedAudio != null) {
                // 先停止
                allInOneService.stopPlayAudio()
                adapter?.stopPlaying()
                // 再播放
                allInOneService.playAudio(selectedAudio)
                adapter?.setPlayingPosition(selectPositon!!)
            }
        }
        // 停止播放音频文件
        audioStopPlayBtn.setOnClickListener {
            allInOneService.stopPlayAudio()
            adapter?.stopPlaying()
        }

        // 循环播放音频文件
        audioLoopPlayBtn.setOnClickListener {
            val selectedAudio = adapter?.getSelectedItem()
            val selectPositon = adapter?.getSelectedPosition()
            if (selectedAudio != null) {
                // 先停止
                allInOneService.stopPlayAudio()
                adapter!!.stopPlaying()
                // 再播放
                allInOneService.startLoopPlayAudio(selectedAudio)
                adapter!!.setPlayingPosition(selectPositon!!)
            }
        }

        // 上传音频文件
        addRecordBtn.setOnClickListener {
            val intent = Intent(this.context, AllInOneAddRecordActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK;
            startActivity(this.context, intent, null)
        }
        // 删除音频文件
        delAudioBtn.setOnClickListener {
            val selectedAudio = adapter?.getSelectedItem()
            val selectedPosition = adapter?.getSelectedPosition()
            val playingPositon = adapter?.getPlayingPosition()
            if (selectedAudio != null) {
                // 如果要删除的是正在播放的音频，先停止
                if(selectedPosition == playingPositon) {
                    allInOneService.stopPlayAudio()
                    adapter!!.stopPlaying()
                }
                thread {
                    if(allInOneService.delFile(selectedAudio)) {
                        val handler = Handler(Looper.getMainLooper())
                        handler.post {
                            adapter!!.removeItem(selectedPosition!!)
                        }
                    }
                }
            }
        }
        initStatus()
        setConnectState()
    }
    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.all_in_one_speaker_weight, this, true)
        realTimeSpeakBtn = findViewById(R.id.real_time_speak_btn)
        playAlarmBtn = findViewById(R.id.play_alarm)
        mConnectState = findViewById(R.id.connectState)
        ttsText = findViewById(R.id.tts_text)
        ttsPlayBtn = findViewById(R.id.tts_play)
        ttsRadioGroup = findViewById(R.id.radioGroup)
        ttsLoopPlaybackCheckbox = findViewById(R.id.tts_loop_playback_checkbox)
        audioPlayBtn = findViewById(R.id.audio_play)
        audioStopPlayBtn = findViewById(R.id.audio_stopPlay)
        audioLoopPlayBtn = findViewById(R.id.audio_loopPlay)
        addRecordBtn = findViewById(R.id.addRecordBtn)
        delAudioBtn = findViewById(R.id.del_audio)
        audioListView = findViewById(R.id.record_list)
        volumeSeekBar = findViewById(R.id.volume_seek_bar)
        volumeText = findViewById(R.id.volumeText)
        pitchSeekBar = findViewById(R.id.pitch_seek_bar)
        pitchText = findViewById(R.id.pitchText)
    }

    private fun getFileList() {
        thread {
            try {
                val files = allInOneService.fetchFiles()
                if (files == null) {
//                    showToast("获取文件列表失败!")
                    return@thread
                }
                val mainHandler = Handler(Looper.getMainLooper())
                Log.i(TAG, "files:${files}")
                val tempList = mutableListOf<String>()
                var i = 0
                while (i < files.size) {
                    Log.i(TAG, "item:${files[i]}")
                    if ("" != files[i]) {
                        Log.i(TAG, "添加item:${files[i]}")
                        tempList.add(files[i])
                    }
                    i++

                }

                mainHandler.post {
                    adapter?.updateAllItems(tempList)
                    Log.d(TAG, "更新：${tempList}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                e.message?.let { Log.e(TAG, it) }
            }
        }
    }

    fun onShow() {
        if(adapter == null) {
            // 设置适配器
            adapter = AudioListAdapter(context, audioItems)
            audioListView.adapter = adapter

            // 设置列表项点击事件
            audioListView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                // 更新选中状态
                adapter?.setSelectedPosition(position)
            }
        }
        getFileList()
    }

    // 定时器，判断连接状态
    private fun setConnectState() {
        isConnecting = true
        thread {
            while (!allInOneService.connect()) {
                Thread.sleep(1000)
            }
            isConnecting = false
            updateTime = Date().time
            getMessageTime()
        }
    }

    // 定时器，判断消息接收情况
    private fun getMessageTime() {
        val timer = Timer();
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask() {
            override fun run() {
                // 已连接
                if (allInOneService.getIsConnected()) {
                    // 3秒没收到信息，显示未连接
//                    if (Date().time - updateTime > 3000) {
//                        handler.post {
//                            mConnectState.setText(R.string.connection_status_notconnected)
//                        }
//                    }
//                    else {
                        handler.post {
                            mConnectState.setText(R.string.connection_status_connected)
                        }
//                    }
//                    // 如果超过10s没收到消息，主动断开连接，等待重连
//                    if (Date().time - updateTime > 10000) {
//                        // 断连
//                        allInOneService.disConnect()
//                    }
                    // 如果没连接云台，尝试连接
                    if(!allInOneService.getIsPtzConnected() && !isConnectingPtz) {
                        isConnectingPtz = true
                        thread {
                            while (!allInOneService.ptzConnect()) {
                                Thread.sleep(1000)
                            }
                            isConnectingPtz = false
                        }
                    }
                }
                else{// 未连接
                    if(!isConnecting){
                        isConnecting = true
                        thread {
                            Thread.sleep(5000)// 先等待5s，防止刚断连就重连，报错
                            while (!allInOneService.connect()) {
                                Thread.sleep(1000)
                            }
                            isConnecting = false
                            updateTime = Date().time
                            handler.post {
                                mConnectState.setText(R.string.connection_status_connected)
                            }
                            // 默认音量与滑条值同步
                            allInOneService.setVolume(volumeSeekBar.progress)
                        }
                    }
                }
            }
        }
        // 定时器，100毫秒后开始执行，每2秒执行一次
        timer.scheduleAtFixedRate(task, 100, 2000);
    }

    private fun initStatus() {
        if (sharedPreferences.getBoolean("alar_status", false)) {
            isPlayAlarm = true
            playAlarmBtn.setText(R.string.stop_playing)
        }

        // 如果缓存里的状态是正在喊话
        if (sharedPreferences.getBoolean("record", false)) {
            // 如果megaphoneService里的状态是未在喊话，可能是在喊话未关闭的情况下关闭了APP，停止喊话同步状态
            if(allInOneService.isRecording){
                isStartSpeak = true
                realTimeSpeakBtn.setText(R.string.stop_speak)
            }
            else {
                allInOneService.stopRealTimeShout()
                sharedPreferences.edit {
                    putBoolean("record", false)
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecordingProcess() {
        realTimeSpeakBtn.setText(R.string.staring_speak)
        realTimeSpeakBtn.isEnabled = false

        // 设置录音准备回调
        allInOneService.onRecordingReady = {
            Handler(Looper.getMainLooper()).post {
                realTimeSpeakBtn.setText(R.string.stop_speak)
                realTimeSpeakBtn.isEnabled = true
            }
        }

        // 开始录音
        allInOneService.startRealTimeShout(true)
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
}