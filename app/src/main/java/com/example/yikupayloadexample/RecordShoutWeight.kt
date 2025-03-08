package com.example.yikupayloadexample

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.core.content.ContextCompat.startActivity
import com.yiku.yikupayload.util.MsgCallback
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


    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        initView(context)

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

    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.record_shout_weight, this, true)
        mRecordList = findViewById(R.id.record_list)
        mAddRecordBtn = findViewById(R.id.addRecordBtn)
        mDelAudioBtn = findViewById(R.id.del_audio)
        mStopAudioBtn = findViewById(R.id.stop_audio)

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