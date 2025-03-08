package com.yiku.yikupayload.util

import android.text.InputFilter
import android.text.Spanned
import android.util.Log

class MaxFValueInputFilter(private val maxValue: Int) : InputFilter {
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
            val input = (dest.toString() + source.toString()).toFloat()
            Log.i("com.yiku.yikupayload", "长度：input:${input}")
            Log.i("com.yiku.yikupayload", "长度source：${source},dest.length：${dest.length},dest：${dest}")
            // 检查是否超过最大值
            if (input > maxValue) {
                // 超过最大值，返回空字符串表示不接受输入
                return ""
            }
            // 小数点位数判断，小数点后只保留1位小数
            val inputStr = input.toString()
            val temp = inputStr.split(".")
            if(temp.size > 1){
                if(temp[1].length > 1){
                    return ""
                }
            }
        } catch (e: NumberFormatException) {
            // 输入的内容无法转换为数字，返回空字符串表示不接受输入
            return ""
        }
        // 接受输入
        return null
    }
}