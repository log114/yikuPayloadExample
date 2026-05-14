package com.example.yikupayloadexample.component

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageButton
import com.example.yikupayloadexample.R

class StatefulImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageButton(context, attrs, defStyleAttr) {

    private val TAG = "StatefulImageButton"
    private var prefix: String? = null
    private var closedResId: Int = 0
    private var disabledResId: Int = 0
    private var openedResId: Int = 0
    private var isPressedState = false

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.StatefulImageButton, 0, 0).apply {
            try {
                prefix = getString(R.styleable.StatefulImageButton_imagePrefix)
                if (!prefix.isNullOrEmpty()) {
                    closedResId = getResId("${prefix}_closed")
                    disabledResId = getResId("${prefix}_disabled")
                    openedResId = getResId("${prefix}_opened")

                    updateImage()

                    setOnTouchListener { _, event ->
                        Log.d(TAG, "event.action=${event.action}")
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                if (isEnabled && openedResId != 0) {
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
            !isEnabled && disabledResId != 0 -> disabledResId
            isPressedState && openedResId != 0 -> openedResId  // 按下状态优先
            isSelected && openedResId != 0 -> openedResId
            else -> closedResId
        }

        if (resId != 0) {
            setImageResource(resId)
        }
    }

    private fun getResId(name: String): Int {
        return resources.getIdentifier(name, "drawable", context.packageName)
    }
}