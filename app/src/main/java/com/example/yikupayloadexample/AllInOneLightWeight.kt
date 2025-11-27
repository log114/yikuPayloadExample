package com.example.yikupayloadexample

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.example.yikupayloadexample.component.ModeItem
import com.example.yikupayloadexample.component.ModeSelectionDialog

class AllInOneLightWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    private lateinit var connectState: TextView
    private lateinit var lightOpenBtn: Button
    private lateinit var lightCloseBtn: Button
    private lateinit var flashOpenBtn: Button
    private lateinit var flashCloseBtn: Button
    private lateinit var luminanceSeekBar: SeekBar
    private lateinit var luminanceText: TextView
    private lateinit var redAndBlueOpenBtn: Button
    private lateinit var redAndBlueCloseBtn: Button
    private lateinit var modeSelectorLayout: LinearLayout
    private lateinit var currentModeText: TextView
    private lateinit var pitchSeekBar: SeekBar
    private lateinit var pitchText: TextView
    private val modeItems = listOf(
        ModeItem("1", "${context.resources.getString(R.string.mode)}1"),
        ModeItem("2", "${context.resources.getString(R.string.mode)}2"),
        ModeItem("3", "${context.resources.getString(R.string.mode)}3"),
        ModeItem("4", "${context.resources.getString(R.string.mode)}4"),
        ModeItem("5", "${context.resources.getString(R.string.mode)}5"),
        ModeItem("6", "${context.resources.getString(R.string.mode)}6"),
        ModeItem("7", "${context.resources.getString(R.string.mode)}7"),
        ModeItem("8", "${context.resources.getString(R.string.mode)}8"),
        ModeItem("9", "${context.resources.getString(R.string.mode)}9"),
        ModeItem("10", "${context.resources.getString(R.string.mode)}10"),
        ModeItem("11", "${context.resources.getString(R.string.mode)}11"),
        ModeItem("12", "${context.resources.getString(R.string.mode)}12"),
        ModeItem("13", "${context.resources.getString(R.string.mode)}13"),
        ModeItem("14", "${context.resources.getString(R.string.mode)}14"),
        ModeItem("15", "${context.resources.getString(R.string.mode)}15"),
        ModeItem("16", "${context.resources.getString(R.string.mode)}16")
    )
    private var currentMode = modeItems[0]

    init {
        initView(context)
        currentModeText.text = currentMode.name
        modeSelectorLayout.setOnClickListener {
            showModeSelectionDialog()
        }
    }
    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.all_in_one_light_weight, this, true)
        connectState = findViewById(R.id.connectState)
        lightOpenBtn = findViewById(R.id.light_open_btn)
        lightCloseBtn = findViewById(R.id.light_close_btn)
        flashOpenBtn = findViewById(R.id.flash_open_btn)
        flashCloseBtn = findViewById(R.id.flash_close_btn)
        luminanceSeekBar = findViewById(R.id.luminance_seek_bar)
        luminanceText = findViewById(R.id.luminance_text)
        redAndBlueOpenBtn = findViewById(R.id.redAndBlue_open_btn)
        redAndBlueCloseBtn = findViewById(R.id.redAndBlue_close_btn)
        modeSelectorLayout = findViewById(R.id.mode_selector_layout)
        currentModeText = findViewById(R.id.current_mode_text)
        pitchSeekBar = findViewById(R.id.pitch_seek_bar)
        pitchText = findViewById(R.id.pitch_text)
    }
    private fun showModeSelectionDialog() {
        val dialog = ModeSelectionDialog(
            context = context,
            modes = modeItems,
            currentMode = currentMode
        ) {
            selectedMode ->
                currentMode = selectedMode
                currentModeText.text = currentMode.name
        }
        dialog.show()
    }
}