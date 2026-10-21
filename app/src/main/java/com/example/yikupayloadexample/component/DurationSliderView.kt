package com.example.yikupayloadexample.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import com.example.yikupayloadexample.R
import kotlin.math.max
import kotlin.math.min

/**
 * 时长滑动条模块
 * 内部逻辑值: 0-99
 * UI展示值: 0.0s - 9.9s (value / 10)
 */
class DurationSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val seekBar: SeekBar
    private val tvValue: TextView

    /** 当前内部真实值 (0-99) */
    private var currentValue: Int = 0

    /** 对外回调：仅在滑动结束后通知 */
    var onValueConfirmed: ((Int) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_duration_slider, this, true)
        seekBar = findViewById(R.id.seek_bar)
        tvValue = findViewById(R.id.tv_duration_value)

        seekBar.max = 99

        // 初始UI
        updateDisplay(currentValue)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            // 拖动过程中只更新UI，不回调外部
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateDisplay(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 不需要处理
            }

            // ✅ 滑动结束才落值 + 通知外部
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar ?: return
                currentValue = seekBar.progress
                onValueConfirmed?.invoke(currentValue)
            }
        })
    }

    // ---------------- 对外 API ----------------

    /**
     * 外部设置滑条当前值 (0-99)
     * 不会触发 onValueConfirmed 回调（避免循环）
     */
    fun setSliderValue(value: Int) {
        val safeValue = min(99, max(0, value))
        currentValue = safeValue
        // 非用户操作，同步进度 & 显示
        seekBar.progress = safeValue
        updateDisplay(safeValue)
    }

    /**
     * 获取当前模块值（0-99）
     */
    fun getSliderValue(): Int = currentValue

    // ---------------- 内部方法 ----------------

    private fun updateDisplay(internalValue: Int) {
        // 0-99 -> 0.0s ~ 9.9s
        val seconds = internalValue / 10f
        tvValue.text = context.getString(R.string.second, seconds)
    }
}