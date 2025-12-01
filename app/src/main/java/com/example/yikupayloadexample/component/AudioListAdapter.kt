package com.example.yikupayloadexample.component

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.yikupayloadexample.R

class AudioListAdapter(
    private val context: Context,
    private val audioItems: MutableList<String>
) : BaseAdapter() {

    private var selectedPosition = -1 // 记录被选中的位置
    private var playingPosition = -1 // 记录正在播放的位置
    private var playingItem: String = ""

    override fun getCount(): Int = audioItems.size

    override fun getItem(position: Int): String = audioItems[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
            holder = ViewHolder().apply {
                textView = view.findViewById(R.id.item_text)
                radioButton = view.findViewById(R.id.radio_button)
            }
            view.tag = holder
        } else {
            view = convertView
            holder = convertView.tag as ViewHolder
        }

        // 设置文本（超出部分显示省略号）
        val audioName = audioItems[position]
        holder.textView.text = audioName

        // 设置单选按钮状态
        holder.radioButton.isChecked = position == selectedPosition

        // 设置文字颜色：播放中为蓝色，其他为白色
        if (position == playingPosition) {
            holder.textView.setTextColor(ContextCompat.getColor(context, R.color.light_blue_600))
        } else {
            holder.textView.setTextColor(ContextCompat.getColor(context, R.color.white))
        }
        return view
    }

    // 设置选中项
    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }

    // 设置播放状态
    fun setPlayingPosition(position: Int) {
        playingPosition = position
        playingItem = if (playingPosition in audioItems.indices) {
            audioItems[playingPosition]
        } else {
            ""
        }
        notifyDataSetChanged()
    }

    // 停止播放
    fun stopPlaying() {
        playingPosition = -1
        playingItem = ""
        notifyDataSetChanged()
    }

    // 获取选中项
    fun getSelectedPosition(): Int = selectedPosition

    // 获取选中项文本
    fun getSelectedItem(): String? {
        return if (selectedPosition in audioItems.indices) {
            audioItems[selectedPosition]
        } else {
            null
        }
    }

    fun getPlayingPosition(): Int = playingPosition

    // 在 AudioListAdapter 类中添加方法
    fun removeItem(position: Int) {
        // 1. 检查位置是否有效
        if (position in 0 until audioItems.size) {
            // 2. 从数据源中移除项
            audioItems.removeAt(position)
            // 3. 如果删除的项是当前选中的项，重置选中位置
            if (selectedPosition == position) {
                selectedPosition = -1
            } else if (selectedPosition > position) {
                // 如果选中项在删除项之后，其索引需要减1
                selectedPosition--
            }
            if (playingPosition == position) {
                playingPosition = -1
                playingItem = ""
            } else if (playingPosition > position) {
                // 如果选中项在删除项之后，其索引需要减1
                playingPosition--
            }
            // 4. 通知适配器数据已变更，ListView将自动刷新
            notifyDataSetChanged()
        }
    }

    fun clearAllItems() {
        audioItems.clear()
        selectedPosition = -1
        notifyDataSetChanged()
    }

    // 批量更新数据（替换整个列表）
    fun updateAllItems(newItems: MutableList<String>) {
        audioItems.clear()
        audioItems.addAll(newItems)
        Log.d("更新后", audioItems.toString())
        selectedPosition = -1
        playingPosition = if(playingItem != "") {
            audioItems.indexOf(playingItem)
        } else {
            -1
        }
        notifyDataSetChanged()
    }

    private class ViewHolder {
        lateinit var textView: TextView
        lateinit var radioButton: RadioButton
    }
}