package com.example.yikupayloadexample.component

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.RadioButton
import android.widget.TextView
import com.example.yikupayloadexample.R

class ModeListAdapter(
    private val modes: List<ModeItem>,
    private var selectedPosition: Int
) : BaseAdapter() {

    override fun getCount(): Int = modes.size

    override fun getItem(position: Int): ModeItem = modes[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(parent.context).inflate(R.layout.dialog_list_item, parent, false)
            holder = ViewHolder().apply {
                modeName = view.findViewById(R.id.mode_name)
                radioButton = view.findViewById(R.id.mode_radio)
            }
            view.tag = holder
        } else {
            view = convertView
            holder = convertView.tag as ViewHolder
        }

        val modeItem = modes[position]
        holder.modeName.text = modeItem.name
        holder.radioButton.isChecked = position == selectedPosition

        return view
    }

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }

    fun getSelectedPosition(): Int = selectedPosition

    private class ViewHolder {
        lateinit var modeName: TextView
        lateinit var radioButton: RadioButton
    }
}

data class ModeItem(
    val id: String,
    val name: String
)