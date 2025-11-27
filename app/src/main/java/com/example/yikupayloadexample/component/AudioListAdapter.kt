package com.example.yikupayloadexample.component

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.RadioButton
import android.widget.TextView
import com.example.yikupayloadexample.R

class AudioListAdapter(
    private val context: Context,
    private val audioItems: List<String>
) : BaseAdapter() {

    private var selectedPosition = -1

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

        return view
    }

    // 设置选中项
    fun setSelectedPosition(position: Int) {
        selectedPosition = position
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

    private class ViewHolder {
        lateinit var textView: TextView
        lateinit var radioButton: RadioButton
    }
}