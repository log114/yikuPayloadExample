package com.example.yikupayloadexample

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import java.util.*


data class RecordPo(
    var id: Int,
    var recordName: String,
    var checked: Boolean,
    var playing: Boolean,
    var loop: Boolean
)

class RecordAdapter(val mData: ArrayList<RecordPo>, val mContext: Context?) : BaseAdapter() {
    lateinit var mPlayerAudio: ImageView
    lateinit var mLoopPlayerAudio: ImageView
    private val TAG = "RecordAdapter"
    fun getData(): ArrayList<RecordPo> {
        return mData
    }

    override fun getCount(): Int {
        return mData.size
    }

    override fun getItem(position: Int): Any {
        return mData[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun resetAllImageStatus() {
        for (data in mData) {
            data.playing = false
            data.loop = false
        }
//
        audioLoopStatusMap.clear()
        audioPlayingStatusMap.clear()
        val mainHandler = Handler(Looper.getMainLooper());
        mainHandler.post {
            notifyDataSetChanged()
        }
    }


    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val convertView = LayoutInflater.from(mContext).inflate(R.layout.item_list_record, null)

        try {
            val mRecordName: TextView = convertView.findViewById(R.id.record_name)
            val mRecordCheckbox: CheckBox = convertView.findViewById(R.id.record_checkbox)
            mPlayerAudio = convertView.findViewById(R.id.play_audio)
            mLoopPlayerAudio = convertView.findViewById(R.id.loop_play_audio)
            mRecordName.text = mData[position].recordName
            mRecordCheckbox.isChecked = mData[position].checked
            Log.i(TAG, "${mData[position].recordName}----${mData[position].playing}")
            if (mData[position].playing) {
                if (mData[position].loop) {
                    mLoopPlayerAudio.setImageResource(R.drawable.stop)
                } else {
                    mPlayerAudio.setImageResource(R.drawable.stop)
                }
            }
            mPlayerAudio.setOnClickListener {
                // 不用是否在播放状态，先清播放状态，在进行播放
                audioPlayingStatusMap[mData[position].recordName] = false
                audioLoopStatusMap[mData[position].recordName] = false
                resetAllImageStatus()
                if (mData[position].playing) {
                    megaphoneService?.stopPlayAudio()
                    notifyDataSetChanged()
                    return@setOnClickListener
                }
                audioPlayingStatusMap[mData[position].recordName] = true
                audioLoopStatusMap[mData[position].recordName] = false
                mData[position].loop = false
                mData[position].playing = true
                notifyDataSetChanged()
                megaphoneService?.playAudio(mData[position].recordName)
            }
            mRecordCheckbox.setOnClickListener {
                mData[position].checked = mRecordCheckbox.isChecked
                notifyDataSetChanged()
            }
            mLoopPlayerAudio.setOnClickListener {
                audioPlayingStatusMap[mData[position].recordName] = false
                audioLoopStatusMap[mData[position].recordName] = false
                resetAllImageStatus()
                if (mData[position].playing) {
                    megaphoneService?.stopPlayAudio()
                    mData[position].loop = false
                    mData[position].playing = false
                    notifyDataSetChanged()
                    return@setOnClickListener
                }
                mData[position].loop = true
                mData[position].playing = true
                audioPlayingStatusMap[mData[position].recordName] = true
                audioLoopStatusMap[mData[position].recordName] = true
                notifyDataSetChanged()
                megaphoneService?.startLoopPlayAudio(mData[position].recordName)

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return convertView
    }

}