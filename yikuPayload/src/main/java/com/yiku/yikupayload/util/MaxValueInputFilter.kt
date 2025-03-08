package com.yiku.yikupayload.util

import android.text.InputFilter
import android.text.Spanned


class MaxValueInputFilter(private val maxValue: Int) : InputFilter {
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): String? {
        try {
            // 将新输入的字符串转换为数字
            val input = (dest.toString() + source.toString()).toInt()
            // 检查是否超过最大值
            if (input > maxValue) {
                // 超过最大值，返回空字符串表示不接受输入
                return ""
            }
        } catch (e: NumberFormatException) {
            // 输入的内容无法转换为数字，返回空字符串表示不接受输入
            return ""
        }
        // 接受输入
        return null
    }
}