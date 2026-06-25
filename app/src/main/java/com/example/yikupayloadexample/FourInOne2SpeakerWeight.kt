package com.example.yikupayloadexample

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
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
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.edit
import com.example.yikupayloadexample.MApplication.applicationContext
import com.example.yikupayloadexample.component.AudioListAdapter
import com.yiku.yikupayloadSDK.util.MsgCallback
import com.yiku.yikupayloadSDK.util.OpusUtils
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

@RequiresApi(Build.VERSION_CODES.S)
class FourInOne2SpeakerWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)
    private val TAG = "FourInOne2SpeakerWeight"
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var speakerMainPage: LinearLayout
    private lateinit var speakerSecondPage: LinearLayout
    private lateinit var ttsSettingPage: LinearLayout
    private lateinit var audioFileListPage: LinearLayout
    private lateinit var realTimeSpeakBtn: Button
    private lateinit var playAlarmBtn: Button
    private lateinit var mConnectState: TextView
    private lateinit var statusDot: View
    private lateinit var background: GradientDrawable
    private lateinit var ttsText: EditText
    private lateinit var ttsPlayBtn: Button
    private lateinit var ttsRadioGroup: RadioGroup
    private lateinit var ttsLoopPlaybackCheckbox: CheckBox
    private lateinit var ttsSettingBtn: Button
    private lateinit var openFileListBtn: Button
    private lateinit var addRecordBtn: Button
    private lateinit var delAudioBtn: Button
    private lateinit var audioFileNameText: TextView
    private lateinit var audioLoopPlaybackCheckbox: CheckBox
    private lateinit var audioPlayBtn: Button
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var volumeText: TextView
    private lateinit var ttsBackBtn: Button
    private lateinit var audioBackBtn: Button
    private lateinit var mRadioBtn: Button
    private var isConnecting: Boolean = false
    private var isForegroundServiceRunning = false
    private var isStartSpeak = false
    private var isPlayAlarm = false;
    private var isLoopTTSPlaying = false
    private var isSettingPitch: Boolean = false
    private var isInit: Boolean = false
    private var isFirstConneted: Boolean = true
    private var settingTimer: Timer? = null

    private var isPlayingAudio: Boolean = false
    private lateinit var audioListView: ListView
    private var adapter: AudioListAdapter? = null
    private var lastSelectedPosition = -1
    private var lastSelectedFileName = ""
    // 初始化数据
    private var audioItems: MutableList<String> = mutableListOf()
    private lateinit var audioTrack: AudioTrack
    private var isRadio = false;
    private val radioRate = 16000 // 新版收音麦opus编码采样率是16000
    private val channels = 1
    private val frameSize = 320
    private val channelsConfig = AudioFormat.CHANNEL_OUT_MONO  // CHANNEL_OUT_MONO 单声道 CHANNEL_OUT_STEREO双声道

    init {
        initView(context)

        // 播放警报
        playAlarmBtn.setOnClickListener {
            sharedPreferences.edit {
                if (!fourInOne2Service.isPlayAlarm) {
                    // 播放警报
                    fourInOne2Service.playAlarm()
                    playAlarmBtn.setText(R.string.stop_playing)
                    putBoolean("alar_status", true);
                } else {
                    // 停止警报
                    fourInOne2Service.stopPlayAlarm()
                    playAlarmBtn.setText(R.string.play_alarm)
                    putBoolean("alar_status", false);
                }
            }
        }
        // 文字转语音
        ttsPlayBtn.setOnClickListener {
            if(isLoopTTSPlaying) {
                fourInOne2Service.stopLoopTts()
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
                    fourInOne2Service.startLoopTtsV2(translateText, voice)
                }
                else {
                    fourInOne2Service.ttsV2(translateText, voice)
                }
                ttsPlayBtn.setText(R.string.stop_playing)
                isLoopTTSPlaying = true
            }
        }
        // 设置文字转语音
        ttsSettingBtn.setOnClickListener {
            speakerMainPage.visibility = GONE
            speakerSecondPage.visibility = VISIBLE
            ttsSettingPage.visibility = VISIBLE
            audioFileListPage.visibility = GONE
        }

        // 音量控制
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                volumeText.text = "${seekBar.progress}%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                fourInOne2Service.setVolume(seekBar.progress)
            }
        })

        // 播放、停止播放音频文件
        audioPlayBtn.setOnClickListener {
            if(isPlayingAudio) {
                fourInOne2Service.stopPlayAudio()
                adapter?.stopPlaying()
                audioPlayBtn.setText(R.string.play)
            }
            else {
                val selectedAudio = adapter?.getSelectedItem()
                val selectPositon = adapter?.getSelectedPosition()
                if (selectedAudio != null) {
                    adapter?.setPlayingPosition(selectPositon!!)
                    if(audioLoopPlaybackCheckbox.isChecked) {
                        fourInOne2Service.startLoopPlayAudio(selectedAudio)
                    }
                    else {
                        fourInOne2Service.playAudio(selectedAudio)
                    }
                    audioPlayBtn.setText(R.string.stop_playing)
                }
            }
            isPlayingAudio = !isPlayingAudio
        }

        // 打开音频文件选择列表
        openFileListBtn.setOnClickListener {
            speakerMainPage.visibility = GONE
            speakerSecondPage.visibility = VISIBLE
            ttsSettingPage.visibility = GONE
            audioFileListPage.visibility = VISIBLE
        }

        // 上传音频文件
        addRecordBtn.setOnClickListener {
            val intent = Intent(this.context, FourInOne2AddRecordActivity::class.java)
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
                    fourInOne2Service.stopPlayAudio()
                    adapter!!.stopPlaying()
                }
                thread {
                    if(fourInOne2Service.delFile(selectedAudio)) {
                        val handler = Handler(Looper.getMainLooper())
                        handler.post {
                            adapter!!.removeItem(selectedPosition!!)
                        }
                    }
                }
            }
        }

        // 返回主界面
        ttsBackBtn.setOnClickListener {
            speakerMainPage.visibility = VISIBLE
            speakerSecondPage.visibility = GONE
        }
        audioBackBtn.setOnClickListener {
            speakerMainPage.visibility = VISIBLE
            speakerSecondPage.visibility = GONE
        }

        audioFileNameText.isSelected = true
        initStatus()
        setConnectState()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun initView(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.four_in_one_2_speaker_weight, this, true)
        speakerMainPage = findViewById(R.id.speakerMainPage)
        speakerSecondPage = findViewById(R.id.speakerSecondPage)
        ttsSettingPage = findViewById(R.id.tts_setting_page)
        audioFileListPage = findViewById(R.id.audio_file_list_page)
        realTimeSpeakBtn = findViewById(R.id.real_time_speak_btn)
        mRadioBtn = findViewById(R.id.radio_btn)
        playAlarmBtn = findViewById(R.id.play_alarm)
        mConnectState = findViewById(R.id.connectState)
        statusDot = findViewById(R.id.statusDot)
        background = statusDot.background as GradientDrawable
        ttsSettingBtn = findViewById(R.id.tts_setting_btn)
        ttsText = findViewById(R.id.tts_text)
        ttsPlayBtn = findViewById(R.id.tts_play)
        ttsRadioGroup = findViewById(R.id.radioGroup)
        ttsLoopPlaybackCheckbox = findViewById(R.id.tts_loop_playback_checkbox)
        openFileListBtn = findViewById(R.id.open_fileList_btn)
        addRecordBtn = findViewById(R.id.addRecordBtn)
        delAudioBtn = findViewById(R.id.del_audio)
        audioFileNameText = findViewById(R.id.audio_file_name_text)
        audioLoopPlaybackCheckbox = findViewById(R.id.audio_loop_playback_checkbox)
        audioPlayBtn = findViewById(R.id.audio_play_btn)
        audioListView = findViewById(R.id.record_list)
        volumeSeekBar = findViewById(R.id.volume_seek_bar)
        volumeText = findViewById(R.id.volumeText)
        ttsBackBtn = findViewById(R.id.tts_back_btn)
        audioBackBtn = findViewById(R.id.audio_back_btn)

        sharedPreferences = context.getSharedPreferences("RealTimeShoutWeight", Context.MODE_PRIVATE)
        // 实时喊话
        realTimeSpeakBtn.setOnClickListener {
            val edit = sharedPreferences.edit()
            Log.i(TAG, "isStartSpeak:${isStartSpeak}")
            if (fourInOne2Service.isRecording) {
                realTimeSpeakBtn.setText(R.string.start_speak)
                Log.i(TAG, "stopRecord...")
                stopForegroundService()
                fourInOne2Service.stopRealTimeShout()
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
        // 收音
        mRadioBtn.setOnClickListener {
            if (!isRadio) {
                startRadio()
            } else {
                stopRadio()
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
        fourInOne2Service.registMsgCallback(object : MsgCallback {
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
        fourInOne2Service.startRadio()
    }

    private fun stopRadio() {
        isRadio = false
        fourInOne2Service.unRegistMsgCallback("radioCallback")
        audioTrack.stop()
        audioTrack.release()
        fourInOne2Service.stopRadio()
        mRadioBtn.setText(R.string.start_listening)
    }

    private fun getFileList() {
        thread {
            try {
                val files = fourInOne2Service.fetchFiles()
//                val files = arrayOf("康姆-我在画中走.mp3", "林俊杰-江南.mp3", "王心凌-梦的光点.mp3", "王忻辰&苏星婕-清空.mp3")
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
                    // 文件更新后恢复选中状态
                    restoreSelectionAfterUpdate(tempList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                e.message?.let { Log.e(TAG, it) }
            }
        }
    }

    private fun restoreSelectionAfterUpdate(fileList: List<String>) {
        if (lastSelectedFileName.isNotEmpty() && fileList.contains(lastSelectedFileName)) {
            val position = fileList.indexOf(lastSelectedFileName)
            if (position >= 0) {
                adapter?.setSelectedPosition(position)
                audioListView.setSelection(position)
                audioFileNameText.text = lastSelectedFileName
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
                audioFileNameText.text = adapter?.getSelectedItem()
                // 保存选中状态
                lastSelectedPosition = position
                lastSelectedFileName = adapter?.getSelectedItem() ?: ""
            }
        }
        getFileList()
        // 文件加载完成后恢复选中状态
        restoreSelection()
    }

    private fun restoreSelection() {
        // 方式1：使用内存中的状态恢复
        if (lastSelectedPosition >= 0 && lastSelectedFileName.isNotEmpty()) {
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                adapter?.setSelectedPosition(lastSelectedPosition)
                audioListView.setSelection(lastSelectedPosition)
            }, 300) // 延迟确保列表已加载完成
        }
    }

    // 恢复俯仰状态读取
    private fun resumeMonitor() {
        settingTimer = Timer();
        val task = object : TimerTask() {
            override fun run() {
                isSettingPitch = false
            }
        }
        settingTimer?.schedule(task, 4000);
    }
    // 取消恢复
    private fun cancelResume() {
        settingTimer?.cancel()
        settingTimer?.purge()
        settingTimer = null
    }

    // 定时器，判断连接状态
    private fun setConnectState() {
        isConnecting = true
        thread {
            val fourInOne2Host = preferences?.getString("FourInOne2Host", "")
            if(fourInOne2Host != null && "" != fourInOne2Host) {
                fourInOne2Service.setIp(fourInOne2Host)
            }
            while (!fourInOne2Service.connect()) {
                Thread.sleep(1000)
            }
            isConnecting = false
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
                if (fourInOne2Service.getIsConnected()) {
                    if(isFirstConneted) {
                        isFirstConneted = false
                        fourInOne2Service.setContext(context)
                        fourInOne2Service.setVolume(volumeSeekBar.progress)
                    }
                }
                else{// 未连接
                    if(!isConnecting){
                        isConnecting = true
                        thread {
                            if(isInit) {
                                Thread.sleep(5000)// 先等待5s，防止刚断连就重连，报错
                            }
                            isInit = true
                            while (!fourInOne2Service.connect()) {
                                Thread.sleep(1000)
                            }
                            isConnecting = false
                            // 默认音量与滑条值同步
                            fourInOne2Service.setVolume(volumeSeekBar.progress)
                        }
                    }
                }

                // 喊话器界面连接状态，显示成灯的连接状态（主要为了保持显示内容一致）
                if(fourInOne2Service.getMainIsConnected()) {
                    handler.post {
                        mConnectState.setText(R.string.connection_status_connected)
                        background.setColor(ContextCompat.getColor(context, R.color.green))
                    }
                }
                else {
                    handler.post {
                        mConnectState.setText(R.string.connection_status_notconnected)
                        background.setColor(ContextCompat.getColor(context, R.color.red))
                    }
                }
            }
        }
        // 定时器，100毫秒后开始执行，每2秒执行一次
        timer.schedule(task, 100, 2000);
    }

    private fun initStatus() {
        if (sharedPreferences.getBoolean("alar_status", false)) {
            isPlayAlarm = true
            playAlarmBtn.setText(R.string.stop_playing)
        }

        // 如果缓存里的状态是正在喊话
        if (sharedPreferences.getBoolean("record", false)) {
            // 如果megaphoneService里的状态是未在喊话，可能是在喊话未关闭的情况下关闭了APP，停止喊话同步状态
            if(fourInOne2Service.isRecording){
                isStartSpeak = true
                realTimeSpeakBtn.setText(R.string.stop_speak)
            }
            else {
                fourInOne2Service.stopRealTimeShout()
                sharedPreferences.edit {
                    putBoolean("record", false)
                }
            }
        }

        val ttstext: String = sharedPreferences.getString("ttstext", "")!!
        ttsText.setText(ttstext)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecordingProcess() {
        realTimeSpeakBtn.setText(R.string.staring_speak)
        realTimeSpeakBtn.isEnabled = false

        // 设置录音准备回调
        fourInOne2Service.onRecordingReady = {
            Handler(Looper.getMainLooper()).post {
                realTimeSpeakBtn.setText(R.string.stop_speak)
                realTimeSpeakBtn.isEnabled = true
            }
        }

        // 开始录音
        fourInOne2Service.startRealTimeShout(true)
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

    // 释放资源
    fun releaseResources() {
        stopForegroundService()
        fourInOne2Service.stopRealTimeShout()
        stopRadio() // 确保停止收音
        fourInOne2Service.releaseAudioResources() // 释放SDK音频资源
    }
}