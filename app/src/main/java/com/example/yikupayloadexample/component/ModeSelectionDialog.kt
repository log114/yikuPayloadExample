package com.example.yikupayloadexample.component

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ListView
import android.widget.Button
import com.example.yikupayloadexample.R

class ModeSelectionDialog(
    context: Context,
    private val modes: List<ModeItem>,
    private val currentMode: ModeItem,
    private val onModeSelected: (ModeItem) -> Unit
) : Dialog(context, R.style.CustomDialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_mode_selection)

        setupDialogWindow() // 设置窗口属性为居中
        setupListView()
        setupButtons()
    }

    private fun setupDialogWindow() {
        window?.apply {
            // 设置为居中显示
            setGravity(Gravity.CENTER)

            // 设置窗口类型（悬浮窗必需）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            } else {
                setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
            }

            // 设置背景
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        setCancelable(true)
        setCanceledOnTouchOutside(true)
    }

    private fun setupListView() {
        val listView = findViewById<ListView>(R.id.mode_list_view)
        val currentPosition = modes.indexOfFirst { it.id == currentMode.id }

        val adapter = ModeListAdapter(modes, currentPosition)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            adapter.setSelectedPosition(position)
        }

        // 滚动到当前选中项
        if (currentPosition >= 0) {
            listView.post { listView.setSelection(currentPosition) }
        }
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.confirm_btn).setOnClickListener {
            val selectedPosition = (findViewById<ListView>(R.id.mode_list_view).adapter as? ModeListAdapter)?.getSelectedPosition() ?: -1
            if (selectedPosition in modes.indices) {
                onModeSelected(modes[selectedPosition])
            }
            dismiss()
        }

        findViewById<Button>(R.id.cancel_btn).setOnClickListener {
            dismiss()
        }
    }
}