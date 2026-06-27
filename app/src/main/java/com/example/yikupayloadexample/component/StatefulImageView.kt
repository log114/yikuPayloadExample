package com.example.yikupayloadexample.component

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.example.yikupayloadexample.R

class StatefulImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val TAG = "StatefulImage"
    private var prefix: String? = null
    private var defaultResId: Int = 0
    private var selectedResId: Int = 0
    private var isPressedState = false
    private var defaultOpacity = 0.5f
    private var selectedOpacity = 0.8f

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.StatefulImageButton, 0, 0).apply {
            try {
                prefix = getString(R.styleable.StatefulImageButton_imagePrefix)
                if (!prefix.isNullOrEmpty()) {
                    defaultResId = getResId("${prefix}_unselected")
                    selectedResId = getResId("${prefix}_selected")

                    updateImage()

                    setOnTouchListener { _, event ->
                        Log.d(TAG, "event.action=${event.action}")
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                if (isEnabled && selectedResId != 0) {
                                    isPressedState = true
                                    updateImage()
                                }
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                isPressedState = false
                                updateImage()
                            }
                        }
                        false
                    }
                }
            } finally {
                recycle()
            }
        }
    }

    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        updateImage()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        updateImage()
    }

    private fun updateImage() {
        Log.d(TAG, "isPressedState=${isPressedState}")
        val resId = when {
            isPressedState && selectedResId != 0 -> selectedResId  // 按下状态优先
            isSelected && selectedResId != 0 -> selectedResId
            else -> defaultResId
        }

        if (resId != 0) {
            setImageResource(resId)
        }
        alpha = if(isPressedState || isSelected) {
            selectedOpacity
        } else {
            defaultOpacity
        }
    }

    private fun getResId(name: String): Int {
        return resources.getIdentifier(name, "drawable", context.packageName)
    }
}