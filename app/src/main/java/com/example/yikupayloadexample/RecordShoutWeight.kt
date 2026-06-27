package com.example.yikupayloadexample

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.yiku.yikupayloadSDK.util.MsgCallback
import org.json.JSONException
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

val audioPlayingStatusMap = HashMap<String, Boolean>()
val audioLoopStatusMap = HashMap<String, Boolean>()


class RecordShoutWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "RecordShoutWeight"
    private val datas: ArrayList<RecordPo> = ArrayList()
    private lateinit var mRecordList: ListView
    var recordAdapter: RecordAdapter? = null
    private lateinit var mAddRecordBtn: Button
    private lateinit var mDelAudioBtn: Button
    private lateinit var mStopAudioBtn: Button
    private lateinit var mTemperature: TextView // 温度
    private lateinit var mStatus: TextView // 状态
    private lateinit var mVolumeSeekBar: SeekBar // 音量滑块
    private var thisPayloadWeight: PayloadWeight? = null
    private var isSettingVolume = false; // 是否正在设置音量
    private var volumeReal = 0;
    private var volumeLimit = 100
    private var temperature = "0"
    private var sharedPreferences: SharedPreferences? = null

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        initView(context)
        setCallbacksTask()
    }

    fun onShow() {
        if (recordAdapter == null) {
            recordAdapter = RecordAdapter(datas, context)
            megaphoneService?.registMsgCallback(object : MsgCallback {
                override fun getId(): String {
                    return "recordAdapter"
                }

                override fun onMsg(msg: ByteArray) {
                    if (msg.decodeToString() == "[39]") {
//                        showToast("音频播放完成.")
                        recordAdapter?.resetAllImageStatus()

                    }
                }
            })
            mRecordList.adapter = recordAdapter
        }

        thread {
            try {
                val files = megaphoneService?.fetchFiles()
//                val files = arrayOf("康姆-我在画中走.mp3", "林俊杰-江南.mp3", "王心凌-梦的光点.mp3", "王忻辰&苏星婕-清空.mp3", "林俊杰-江南.mp3", "王心凌-梦的光点.mp3", "王忻辰&苏星婕-清空.mp3", "林俊杰-江南.mp3", "王心凌-梦的光点.mp3", "王忻辰&苏星婕-清空.mp3", "林俊杰-江南.mp3", "王心凌-梦的光点.mp3", "王忻辰&苏星婕-清空.mp3")
                if (files == null) {
//                    showToast("获取文件列表失败!")
                    return@thread
                }
                Log.i(TAG, "files:${files}")
                // 先构建一个新列表，避免在后台线程操作 datas
                val newList = mutableListOf<RecordPo>()
                files.forEachIndexed { index, name ->
                    if (name.isNotEmpty()) {
                        val playing = audioPlayingStatusMap[name] ?: false
                        val loop = audioLoopStatusMap[name] ?: false
                        newList.add(RecordPo(index, name, checked = false, playing = playing, loop = loop))
                    }
                }

                // 回到 UI 线程一次性更新 datas 并通知适配器
                Handler(Looper.getMainLooper()).post {
                    datas.clear()
                    datas.addAll(newList)
                    recordAdapter?.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                e.message?.let { Log.e(TAG, it) }
            }
        }
    }

    private fun initView(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.record_shout_weight, this, true)
        mRecordList = findViewById(R.id.record_list)
        mAddRecordBtn = findViewById(R.id.addRecordBtn)
        mDelAudioBtn = findViewById(R.id.del_audio)
        mStopAudioBtn = findViewById(R.id.stop_audio)
        mVolumeSeekBar = findViewById(R.id.volume_seek_bar)
        mTemperature = findViewById(R.id.temperature)
        mStatus = findViewById(R.id.status)
        sharedPreferences = context.getSharedPreferences("Megaphone", MODE_PRIVATE)

        mAddRecordBtn.setOnClickListener {
            val intent = Intent(this.context, AddRecordActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK;
            startActivity(this.context, intent, null)
            this.thisPayloadWeight?.hideFloatWindow()
        }

        mDelAudioBtn.setOnClickListener {
            try {
                val delArr = ArrayList<RecordPo>()
                for (item in datas) {
                    Log.i(TAG, "checked:${item.checked}")
                    if (item.checked) {
                        delArr.add(item)
                        try {
                            // 删除同时停止播放
                            for (data in recordAdapter?.getData()!!) {
                                if (data.playing && data.recordName == item.recordName) {
                                    megaphoneService?.stopPlayAudio()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            showToast("停止删除播放音频失败，请手动停止.")
                        }
                        thread {
                            megaphoneService?.delFile(item.recordName)
//                        if () {
//                            showToast("删除成功!")
//                        } else {
//                            showToast("删除失败!")
//                        }
                        }
                    }
                }

                if (delArr.isNotEmpty()) {
                    for (item in delArr) {
                        datas.remove(item)
                    }
                    val mainHandler = Handler(Looper.getMainLooper())
                    mainHandler.post {
                        recordAdapter?.notifyDataSetChanged()
                    }

                } else {
                    showToast(R.string.select_file_to_be_delete)
                }
            } catch (e: Exception) {
                e.message?.let { it1 -> showToast("fail:${it1}") }
            }
        }
        mStopAudioBtn.setOnClickListener {
            megaphoneService?.stopLoopPlayAudio()
            recordAdapter?.resetAllImageStatus()

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
        mTemperature.text = "${context.resources.getString(R.string.temperature)} 0℃"
    }

    fun attachFloatingWindow(service: PayloadWeight) {
        this.thisPayloadWeight = service
    }

    // 定时器，同步更新温度音量等
    private fun setCallbacksTask() {
        val statusDot = findViewById<View>(R.id.statusDot)
        val background = statusDot.background as GradientDrawable
        val connectText = findViewById<TextView>(R.id.realTimeShoutConnect)
        val timer = Timer();
        val task = object : TimerTask() {
            override fun run() {
                // 每秒从内存里拿出状态消息更新
                if (sharedPreferences != null) {
                    volumeReal = sharedPreferences!!.getInt("volume_real", 0)
                    volumeLimit = sharedPreferences!!.getInt("volume_limit", 100)
                    temperature = sharedPreferences!!.getString("temperature", "0").toString()
                    val connectStatus = sharedPreferences!!.getBoolean("shoutConnectStatus", false)
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
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.schedule(task, 100, 1000);
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }

    private fun showToast(msg: Int) {
        Toast.makeText(
            context,
            msg,
            Toast.LENGTH_SHORT
        ).show()
    }
    private fun showToast(msg: String) {
        Toast.makeText(
            context,
            msg,
            Toast.LENGTH_SHORT
        ).show()
    }
}