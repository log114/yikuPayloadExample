package com.example.yikupayloadexample

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
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
    private var isSettingVolume = false; // 是否正在设置音量
    private var volumeReal = 0;
    private var volumeLimit = 100
    private var temperature = "0"

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        initView(context)
        setCallbacksTask()
    }

    fun setCallbacks() {
        megaphoneService!!.msgCallbacks += object : MsgCallback {
            override fun getId(): String {
                return "RecordShoutWeightCallback"
            }

            override fun onMsg(msg: ByteArray) {
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
                if (files == null) {
//                    showToast("获取文件列表失败!")
                    return@thread
                }
                val mainHandler = Handler(Looper.getMainLooper())
                Log.i(TAG, "files:${files}")
                datas.clear()
                var i = 0
                while (i < files.size) {
                    Log.i(TAG, "item:${files[i]}")
                    if ("" != files[i]) {
                        // 获取之前的playing状态和loop状态
                        val playing =
                            if (audioPlayingStatusMap[files[i]] == null) false else audioPlayingStatusMap[files[i]]
                        val loop =
                            if (audioLoopStatusMap[files[i]] == null) false else audioLoopStatusMap[files[i]]
                        datas.add(
                            RecordPo(
                                i,
                                files[i],
                                checked = false,
                                playing = playing == true,
                                loop = loop == true
                            )
                        )
                    }
                    i++

                }

                mainHandler.post {
                    recordAdapter?.notifyDataSetChanged()
                }
//                megaphoneService.getAudioList(object : GetAudioFilesCallback {
//                    override fun onResult(files: String) {
//
//                    }
//                })
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

        mAddRecordBtn.setOnClickListener {
            val intent = Intent(this.context, AddRecordActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK;
            startActivity(this.context, intent, null)
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

                if (delArr.size > 0) {
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