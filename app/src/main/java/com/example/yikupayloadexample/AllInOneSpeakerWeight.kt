package com.example.yikupayloadexample

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import com.example.yikupayloadexample.component.AudioListAdapter

class AllInOneSpeakerWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)
    private lateinit var realTimeSpeakBtn: Button
    private lateinit var playAlarmBtn: Button
    private lateinit var ttsText: EditText
    private lateinit var ttsPlayMaleBtn: Button
    private lateinit var ttsPlayFemaleBtn: Button
    private lateinit var ttsLoopPlaybackCheckbox: CheckBox
    private lateinit var audioPlayBtn: Button
    private lateinit var audioStopPlayBtn: Button
    private lateinit var audioLoopPlayBtn: Button
    private lateinit var addRecordBtn: Button
    private lateinit var delAudioBtn: Button

    private lateinit var audioListView: ListView
    private var adapter: AudioListAdapter
    // 初始化数据（示例数据）
    private var audioItems: List<String> = listOf(
        "音频文件1.mp3",
        "这是一个很长的音频文件名可能会超出显示区域这是一个很长的音频文件名可能会超出显示区域.mp3",
        "音频文件3.wav",
        "第四个音频文件.aac",
        "第五个音频文件.mp3"
    )

    init {
        initView(context)
        // 设置适配器
        adapter = AudioListAdapter(context, audioItems)
        audioListView.adapter = adapter

        // 设置列表项点击事件
        audioListView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            // 更新选中状态
            adapter.setSelectedPosition(position)
        }

        audioPlayBtn.setOnClickListener {
            val selectedAudio = adapter.getSelectedItem()
            if (selectedAudio != null) {
                // 执行选中音频的相关操作
                Toast.makeText(context, "选中了: $selectedAudio", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.all_in_one_speaker_weight, this, true)
        realTimeSpeakBtn = findViewById(R.id.real_time_speak_btn)
        playAlarmBtn = findViewById(R.id.play_alarm)
        ttsText = findViewById(R.id.tts_text)
        ttsPlayMaleBtn = findViewById(R.id.tts_play_male)
        ttsPlayFemaleBtn = findViewById(R.id.tts_play_female)
        ttsLoopPlaybackCheckbox = findViewById(R.id.tts_loop_playback_checkbox)
        audioPlayBtn = findViewById(R.id.audio_play)
        audioStopPlayBtn = findViewById(R.id.audio_stopPlay)
        audioLoopPlayBtn = findViewById(R.id.audio_loopPlay)
        addRecordBtn = findViewById(R.id.addRecordBtn)
        delAudioBtn = findViewById(R.id.del_audio)
        audioListView = findViewById(R.id.record_list)
    }
}