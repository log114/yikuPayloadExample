package com.example.yikupayloadexample

import android.Manifest
import android.annotation.SuppressLint
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
import android.widget.ImageView
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
import com.example.yikupayloadexample.MApplication.applicationContext
import com.example.yikupayloadexample.component.AudioListAdapter
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread
import androidx.core.content.edit
import com.yiku.yikupayloadSDK.protocol.ALLINONE_PITCH_STATE
import com.yiku.yikupayloadSDK.util.MsgCallback

@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("ClickableViewAccessibility")
class AllInOneSpeakerWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)
    private val TAG = "AllInOneSpeakerWeight"
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var speakerMainPage: LinearLayout
    private lateinit var speakerSecondPage: LinearLayout
    private lateinit var ttsSettingPage: LinearLayout
    private lateinit var audioFileListPage: LinearLayout
    private lateinit var realTimeSpeakBtn: Button
    private lateinit var playAlarmBtn: Button
    private lateinit var mConnectState: TextView
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
    private lateinit var pitchSeekBar: SeekBar
    private lateinit var pitchText: TextView
    private lateinit var backBtn: ImageView
    private var isConnecting: Boolean = false
    private var isForegroundServiceRunning = false
    private var isStartSpeak = false
    private var isPlayAlarm = false;
    private var isLoopTTSPlaying = false
    private var isConnectingPtz: Boolean = false
    private var isSettingPitch: Boolean = false
    private var isInit: Boolean = false
    private var isFirstConneted: Boolean = true
    private var updateTime = Date().time
    private val interval = 100 // 限制两次俯仰控制间隔时间不得小于50ms
    private var lastTime = Date().time // 上一次控制俯仰的时间
    private var settingTimer: Timer? = null

    private var isPlayingAudio: Boolean = false
    private lateinit var audioListView: ListView
    private var adapter: AudioListAdapter? = null
    private var lastSelectedPosition = -1
    private var lastSelectedFileName = ""
    // 初始化数据
    private var audioItems: MutableList<String> = mutableListOf()

    init {
        initView(context)
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
                    updateTime = Date().time
                    if(isSettingPitch) {
                        return
                    }
                    // 俯仰值，0-900
                    var pitchValue = ((msg[3].toInt()  and 0xFF) shl 8) or (msg[4].toInt()  and 0xFF)
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        pitchSeekBar.progress = pitchValue
                        // 俯仰小于3°的时候，显示为0°
                        if(pitchValue < 3) {
                            pitchValue = 0
                        }
                        pitchText.text = "${pitchValue/10}°"
                    }
                }
            }
        })

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
                    allInOneService.startLoopTtsV2(translateText, voice)
                }
                else {
                    allInOneService.ttsV2(translateText, voice)
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
                allInOneService.setVolume(seekBar.progress)
            }
        })

        // 俯仰控制
        pitchSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 如果不是手动拖动导致的更改，不重新发命令
                if(!isSettingPitch) {
                    return
                }
                pitchText.text = "${seekBar.progress/10}°"
                if(Date().time - lastTime > interval) {
                    lastTime = Date().time
                    allInOneService.pitchControl(seekBar.progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isSettingPitch = true
                cancelResume()
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                Log.i(TAG, "音量设置，当前音量：${seekBar.progress}")
                thread {
                    Thread.sleep(100)
                    allInOneService.pitchControl(seekBar.progress) // 结束的时候也发一下，避免不统一
                }
                resumeMonitor()
            }
        })

        // 播放、停止播放音频文件
        audioPlayBtn.setOnClickListener {
            if(isPlayingAudio) {
                allInOneService.stopPlayAudio()
                adapter?.stopPlaying()
                audioPlayBtn.setText(R.string.play)
            }
            else {
                val selectedAudio = adapter?.getSelectedItem()
                val selectPositon = adapter?.getSelectedPosition()
                if (selectedAudio != null) {
                    adapter?.setPlayingPosition(selectPositon!!)
                    if(audioLoopPlaybackCheckbox.isChecked) {
                        allInOneService.startLoopPlayAudio(selectedAudio)
                    }
                    else {
                        allInOneService.playAudio(selectedAudio)
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

        // 返回主界面
        backBtn.setOnClickListener {
            speakerMainPage.visibility = VISIBLE
            speakerSecondPage.visibility = GONE
        }

        audioFileNameText.isSelected = true
        initStatus()
        setConnectState()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun initView(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.all_in_one_speaker_weight, this, true)
        speakerMainPage = findViewById(R.id.speakerMainPage)
        speakerSecondPage = findViewById(R.id.speakerSecondPage)
        ttsSettingPage = findViewById(R.id.tts_setting_page)
        audioFileListPage = findViewById(R.id.audio_file_list_page)
        realTimeSpeakBtn = findViewById(R.id.real_time_speak_btn)
        playAlarmBtn = findViewById(R.id.play_alarm)
        mConnectState = findViewById(R.id.connectState)
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
        pitchSeekBar = findViewById(R.id.pitch_seek_bar)
        pitchText = findViewById(R.id.pitchText)
        backBtn = findViewById(R.id.back_btn)

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
            while (!allInOneService.connect()) {
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
                if (allInOneService.getIsConnected()) {
                    if(isFirstConneted) {
                        isFirstConneted = false
                        allInOneService.setContext(context)
                        allInOneService.setVolume(volumeSeekBar.progress)
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
                            while (!allInOneService.connect()) {
                                Thread.sleep(1000)
                            }
                            isConnecting = false
                            // 默认音量与滑条值同步
                            allInOneService.setVolume(volumeSeekBar.progress)
                        }
                    }
                }

                // 如果云台没连接，尝试连接
                if(!allInOneService.getIsPtzConnected() && !isConnectingPtz) {
                    isConnectingPtz = true
                    thread {
                        Thread.sleep(5000)
                        while (!allInOneService.ptzConnect()) {
                            Thread.sleep(1000)
                        }
                        isConnectingPtz = false
                        updateTime = Date().time
                    }
                }
                // 已连接
                if (allInOneService.getIsPtzConnected()) {
                    // 如果超过10s没收到消息，主动断开连接，等待重连
                    if (Date().time - updateTime > 10000 && !isConnectingPtz) {
                        Log.d(TAG, "${Date().time}, ${updateTime}, ${Date().time - updateTime}")
                        // 断连
                        allInOneService.disConnectPtz()
                    }
                }
                // 喊话器界面连接状态，显示成灯的连接状态（主要为了保持显示内容一致）
                if(allInOneService.getMainIsConnected()) {
                    handler.post {
                        mConnectState.setText(R.string.connection_status_connected)
                    }
                }
                else {
                    handler.post {
                        mConnectState.setText(R.string.connection_status_notconnected)
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

        val ttstext: String = sharedPreferences.getString("ttstext", "")!!
        ttsText.setText(ttstext)
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

    // 释放资源
    fun releaseResources() {
        stopForegroundService()
        allInOneService.stopRealTimeShout()
    }
}