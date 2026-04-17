package com.example.yikupayloadexample

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.databinding.tool.ext.T
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import com.example.yikupayloadexample.AllInOneSpeakerWeight
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.anim.DefaultAnimator
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import com.lzf.easyfloat.utils.DisplayUtils
import com.lzf.easyfloat.utils.InputMethodUtils
import java.util.Timer
import java.util.TimerTask
import androidx.core.view.isVisible
import androidx.core.view.isGone
import kotlin.concurrent.thread


class PayloadWeight : Service() {
    private val TAG = "PayloadWeight"
    private lateinit var mShoutComp: View
    private lateinit var mShoutView: LinearLayout
    private val binder = PayloadWeightBinder()
    private lateinit var mWindowTitle: TextView
    private lateinit var mShoutViewContent: LinearLayout
    private var opened: Int = 0

    // 组件
    private lateinit var realTimeShoutWeight: RealTimeShoutWeight
    private lateinit var unitreeLightWeight: UnitreeLightWeight
    private lateinit var ttsShoutWeight: TtsShoutWeight
    private lateinit var recordShoutWeight: RecordShoutWeight
    private lateinit var cacheNetWeight: CacheNetWeight
    private lateinit var emitterWeight: EmitterWeight
    private lateinit var lightYl300Weight: LightWeight
    private lateinit var throwerweight: ThrowerWeight
    private lateinit var slowDescentDeviceWeight: SlowDescentDeviceWeight
    private lateinit var gripperWeight: GripperWeight
    private lateinit var resqmeWeight: ResqmeWeight
    private lateinit var extinguisherWeight: ExtinguisherWeight
    private lateinit var waterGunWeight: WaterGunWeight
    private lateinit var bucketWeight: BucketWeight
    private lateinit var waterBranchWeight: WaterBranchWeight
    private lateinit var plLightweight: PL_LightWeight
    private lateinit var allInOneSpeakerWeight: AllInOneSpeakerWeight
    private lateinit var allInOneLightWeight: AllInOneLightWeight
    private lateinit var allInOneThrowerWeight: AllInOneThrowerWeight
    private lateinit var allInOneFpvWeight: AllInOneFpvWeight
    private lateinit var fourInOne2SpeakerWeight: FourInOne2SpeakerWeight
    private lateinit var fourInOne2LightWeight: FourInOne2LightWeight

    private var isInit = false
    private var floatingWindowStatus = false

    // 按钮
    private lateinit var mShoutBtn: ImageView
    private lateinit var mTTSBtn: ImageView
    private lateinit var mRecordBtn: ImageView
    private lateinit var mLightBtn: ImageView
    private lateinit var mCacheNetBtn: ImageView
    private lateinit var mEmitterBtn: ImageView
    private lateinit var mLightYl300Btn: ImageView
    private lateinit var throwerBtn: ImageView
    private lateinit var slowDescentDeviceBtn: ImageView
    private lateinit var gripperBtn: ImageView
    private lateinit var resqmeBtn: ImageView
    private lateinit var extinguisherBtn: ImageView
    private lateinit var waterGunBtn: ImageView
    private lateinit var bucketBtn: ImageView
    private lateinit var waterBranchBtn: ImageView
    private lateinit var plLightBtn: ImageView
    private lateinit var allInOneSpeakerBtn: ImageView
    private lateinit var allInOneLightBtn: ImageView
    lateinit var allInOneThrowerBtn: ImageView
    lateinit var allInOneFpvBtn: ImageView
    private lateinit var fourInOne2SpeakerBtn: ImageView
    private lateinit var fourInOne2LightBtn: ImageView
    private lateinit var lockBtn: ImageView
    private var isLockWindow = false
    var isRotated = false
    private var originalContentWidth = 0
    private var originalContentHeight = 0

    // 空状态
    private lateinit var emptyText: View

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showWindow()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun resetShoutBtnsBackground() {
        mShoutBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        mTTSBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        mRecordBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        mLightBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        mCacheNetBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        mEmitterBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        mLightYl300Btn.setBackgroundResource(R.drawable.yk_shout_btn)
        throwerBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        slowDescentDeviceBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        gripperBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        resqmeBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        extinguisherBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        waterGunBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        bucketBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        waterBranchBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        plLightBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        allInOneSpeakerBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        allInOneLightBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        allInOneThrowerBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        allInOneFpvBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        fourInOne2SpeakerBtn.setBackgroundResource(R.drawable.yk_shout_btn)
        fourInOne2LightBtn.setBackgroundResource(R.drawable.yk_shout_btn)
    }

    private fun openFloatingWindow() {
        if (!floatingWindowStatus) {
            EasyFloat.with(applicationContext)
                // 设置浮窗xml布局文件/自定义View，并可设置详细信息
                .setLayout(R.layout.payload_btn_group_weight) {
                    val view = it
//                    val _mShoutWeight = view
                    mShoutBtn = it.findViewById(R.id.startShoutBtn)

                    mTTSBtn = it.findViewById(R.id.ttsBtn)
                    mRecordBtn = it.findViewById(R.id.recordBtn)
                    mLightBtn = it.findViewById(R.id.lightBtn)
                    mCacheNetBtn = it.findViewById(R.id.catchNetBtn)
//                    it.findViewById<ImageView>(R.id.openFloatingWindowBtn).visibility = INVISIBLE
                    mEmitterBtn = it.findViewById(R.id.emitterBtn)
                    mLightYl300Btn = it.findViewById(R.id.light_yl300_Btn)
                    throwerBtn = it.findViewById(R.id.throwerBtn)
                    slowDescentDeviceBtn = it.findViewById(R.id.slowDescentDeviceBtn)
                    gripperBtn = it.findViewById(R.id.gripperBtn)
                    resqmeBtn = it.findViewById(R.id.resqmeBtn)
                    extinguisherBtn = it.findViewById(R.id.extinguisherBtn)
                    waterGunBtn = it.findViewById(R.id.waterGunBtn)
                    bucketBtn = it.findViewById(R.id.bucketBtn)
                    waterBranchBtn = it.findViewById(R.id.waterBranchBtn)
                    plLightBtn = it.findViewById(R.id.light_pl_Btn)
                    allInOneSpeakerBtn = it.findViewById(R.id.all_in_on_speaker_Btn)
                    allInOneLightBtn = it.findViewById(R.id.all_in_on_light_Btn)
                    allInOneThrowerBtn = it.findViewById(R.id.all_in_on_thrower_Btn)
                    allInOneFpvBtn = it.findViewById(R.id.all_in_on_fpv_Btn)
                    fourInOne2SpeakerBtn = it.findViewById(R.id.four_in_one_2_speaker_Btn)
                    fourInOne2LightBtn = it.findViewById(R.id.four_in_one_2_light_Btn)

                    emptyText = it.findViewById(R.id.emptyText)

                    mShoutBtn.setOnClickListener {
                        Log.i(TAG, "mShoutBtn clicked!");
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(3, mShoutBtn)) {
                            mShoutBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                    mTTSBtn.setOnClickListener {
                        Log.i(TAG, "mTTSBtn clicked!");
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(1, mTTSBtn)) {
                            mTTSBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                    mRecordBtn.setOnClickListener {
                        Log.i(TAG, "mRecordBtn clicked!");
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(2, mRecordBtn)) {
                            mRecordBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }

                    mLightBtn.setOnClickListener {
//                        Log.i(TAG, "mShoutBtn clicked!");
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(4, mLightBtn)) {
                            mLightBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }

                    mCacheNetBtn.setOnClickListener {
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(5, mCacheNetBtn)) {
                            mCacheNetBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                    mEmitterBtn.setOnClickListener {
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(6, mEmitterBtn)) {
                            mEmitterBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                    mLightYl300Btn.setOnClickListener {
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(7, mLightYl300Btn)) {
                            mLightYl300Btn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                    throwerBtn.setOnClickListener {
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(8, throwerBtn)) {
                            throwerBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                    slowDescentDeviceBtn.setOnClickListener {
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(9, slowDescentDeviceBtn)) {
                            slowDescentDeviceBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                    gripperBtn.setOnClickListener {
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(10, gripperBtn)) {
                            gripperBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                    resqmeBtn.setOnClickListener {
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(11, resqmeBtn)) {
                            resqmeBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                    extinguisherBtn.setOnClickListener {
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(12, extinguisherBtn)) {
                            extinguisherBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                    waterGunBtn.setOnClickListener {
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(13, waterGunBtn)) {
                            waterGunBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                    bucketBtn.setOnClickListener {
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(14, bucketBtn)) {
                            bucketBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                    waterBranchBtn.setOnClickListener {
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(15, waterBranchBtn)) {
                            waterBranchBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                    plLightBtn.setOnClickListener {
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(16, plLightBtn)) {
                            plLightBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                    allInOneSpeakerBtn.setOnClickListener {
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(17, allInOneSpeakerBtn)) {
                            allInOneSpeakerBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                    allInOneLightBtn.setOnClickListener {
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(18, allInOneLightBtn)) {
                            allInOneLightBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                    allInOneThrowerBtn.setOnClickListener {
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(19, allInOneThrowerBtn)) {
                            allInOneThrowerBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                    allInOneFpvBtn.setOnClickListener {
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(20, allInOneFpvBtn)) {
                            allInOneFpvBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                    fourInOne2SpeakerBtn.setOnClickListener {
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(21, fourInOne2SpeakerBtn)) {
                            fourInOne2SpeakerBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                    fourInOne2LightBtn.setOnClickListener {
                        resetShoutBtnsBackground()
                        if (this.setSVVisibility(22, fourInOne2LightBtn)) {
                            fourInOne2LightBtn.setBackgroundResource(R.drawable.yk_shout_clicked_btn)
                        }
                    }
                }
                // 设置浮窗显示类型，默认只在当前Activity显示，可选一直显示、仅前台显示
                .setShowPattern(ShowPattern.ALL_TIME)
                // 设置吸附方式，共15种模式，详情参考SidePattern
                .setSidePattern(SidePattern.RESULT_HORIZONTAL)
                // 设置浮窗的标签，用于区分多个浮窗
                .setTag("yk_payload_weight")
                // 设置浮窗是否可拖拽
                .setDragEnable(true)
                // 浮窗是否包含EditText，默认不包含
//                .hasEditText(false)
                // 设置浮窗固定坐标，ps：设置固定坐标，Gravity属性和offset属性将无效
//            .setLocation(100, 200)
                // 设置浮窗的对齐方式和坐标偏移量
                .setGravity(Gravity.END or Gravity.CENTER_VERTICAL, 0, 200)
                // 设置当布局大小变化后，整体view的位置对齐方式
                .setLayoutChangedGravity(Gravity.END)
                // 设置拖拽边界值
//                .setBorder(100, 100, 800, 800)
                // 设置宽高是否充满父布局，直接在xml设置match_parent属性无效
                .setMatchParent(widthMatch = false, heightMatch = false)
                // 设置浮窗的出入动画，可自定义，实现相应接口即可（策略模式），无需动画直接设置为null
                .setAnimator(DefaultAnimator())
                // 设置系统浮窗的不需要显示的页面
//                .setFilter(MainActivity::class.java, CompleteWidgetActivity::class.java)
                // 设置系统浮窗的有效显示高度（不包含虚拟导航栏的高度），基本用不到，除非有虚拟导航栏适配问题
                .setDisplayHeight { context -> DisplayUtils.rejectedNavHeight(context) }
                // 浮窗的一些状态回调，如：创建结果、显示、隐藏、销毁、touchEvent、拖拽过程、拖拽结束。
                // ps：通过Kotlin DSL实现的回调，可以按需复写方法，用到哪个写哪个
                .registerCallback {
                    createResult { isCreated, msg, view ->
                    }
                    show {
                        floatingWindowStatus = true
                    }
                    hide {
                    }
                    dismiss {
                        Log.w(TAG, "yk_payload_weight float dismiss")
                        floatingWindowStatus = false
                    }
                    touchEvent { view, motionEvent ->

                    }
                    drag { view, motionEvent -> }
                    dragEnd { }

                }
                .show()
            showToast(R.string.floating_window_opened);
            setBtnShow()
        } else {
//            EasyFloat.dismiss("yk_payload_weight")
        }

    }

    /**
     * 设置显示隐藏，返回true表示本次调用使组件显示，否则表示使隐藏
     */
    @SuppressLint("ClickableViewAccessibility")
    fun setSVVisibility(type: Int, view: View): Boolean {
        Log.i(TAG, "mShoutComp. setSVVisibility:${mShoutComp.visibility}, opened:${opened}")
        if (mShoutComp.isGone || opened != type) {
            mShoutComp.visibility = VISIBLE
        } else {
            mShoutComp.visibility = GONE
            EasyFloat.hide("yk_payload_weight_op")
            EasyFloat.hide("video_window")
//            popupWindow.dismiss()
            opened = 0
        }
        try {
            // 重置旋转状态
            resetRotation()
            if (mShoutComp.isVisible) {
                mShoutViewContent.removeAllViews()
                when(type) {
                    22 -> { // 机器狗多合一二代，灯光
                        opened = 22
                        mWindowTitle.setText(R.string.lamplight)
                        mShoutViewContent.addView(fourInOne2LightWeight)
                        // 测量内容并决定是否旋转
                        measureAndAdjustLayout()
                    }
                    21 -> { // 机器狗多合一二代，声音
                        opened = 21
                        mWindowTitle.setText(R.string.sound)
                        mShoutViewContent.addView(fourInOne2SpeakerWeight)
                        val speedEditText = mShoutViewContent.findViewById<EditText>(R.id.tts_text)
                        speedEditText.setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) InputMethodUtils.openInputMethod(
                                speedEditText,
                                "yk_payload_weight_op"
                            )
                            false
                        }
                        // 测量内容并决定是否旋转
                        measureAndAdjustLayout()
                        fourInOne2SpeakerWeight.onShow()
                    }
                    20 -> { // 五技吊舱，FPV
                        opened = 20
                        mWindowTitle.text = "FPV"
                        mShoutViewContent.addView(allInOneFpvWeight)
                        // 测量内容并决定是否旋转
                        measureAndAdjustLayout()
                    }
                    19 -> { // 五技吊舱，抛投器
                        opened = 19
                        mWindowTitle.setText(R.string.thrower)
                        mShoutViewContent.addView(allInOneThrowerWeight)
                        val editText = mShoutViewContent.findViewById<EditText>(R.id.detonateHeightEditText)
                        editText.setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) InputMethodUtils.openInputMethod(
                                editText,
                                "yk_payload_weight_op"
                            )
                            false
                        }
                        // 测量内容并决定是否旋转
                        measureAndAdjustLayout()
                    }
                    18 -> { // 五技吊舱，灯光
                        opened = 18
                        mWindowTitle.setText(R.string.lamplight)
                        mShoutViewContent.addView(allInOneLightWeight)
                        // 测量内容并决定是否旋转
                        measureAndAdjustLayout()
                    }
                    17 -> { // 五技吊舱，声音
                        opened = 17
                        mWindowTitle.setText(R.string.sound)
                        mShoutViewContent.addView(allInOneSpeakerWeight)
                        val speedEditText = mShoutViewContent.findViewById<EditText>(R.id.tts_text)
                        speedEditText.setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) InputMethodUtils.openInputMethod(
                                speedEditText,
                                "yk_payload_weight_op"
                            )
                            false
                        }
                        // 测量内容并决定是否旋转
                        measureAndAdjustLayout()
                        allInOneSpeakerWeight.onShow()
                    }
                    16 -> {
                        opened = 16
                        mShoutViewContent.addView(plLightweight)
                    }
                    15 -> {
                        opened = 15
                        mShoutViewContent.addView(waterBranchWeight)
                    }
                    14 -> {
                        opened = 14
                        mShoutViewContent.addView(bucketWeight)
                    }
                    13 -> {
                        opened = 13
                        mShoutViewContent.addView(waterGunWeight)
                    }
                    12 -> {
                        opened = 12
                        mShoutViewContent.addView(extinguisherWeight)
                    }
                    11 -> {
                        opened = 11
                        mShoutViewContent.addView(resqmeWeight)
                    }
                    10 -> {
                        opened = 10
                        mShoutViewContent.addView(gripperWeight)
                    }
                    9 -> {
                        opened = 9
                        mShoutViewContent.addView(slowDescentDeviceWeight)
                        val speedEditText = mShoutViewContent.findViewById<EditText>(R.id.speed)
                        speedEditText.setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) InputMethodUtils.openInputMethod(
                                speedEditText,
                                "yk_payload_weight_op"
                            )
                            false
                        }
                        val lengthEditText = mShoutViewContent.findViewById<EditText>(R.id.length)
                        lengthEditText.setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) InputMethodUtils.openInputMethod(
                                lengthEditText,
                                "yk_payload_weight_op"
                            )
                            false
                        }
                    }
                    8 -> { // 抛投器
                        opened = 8
                        mShoutViewContent.addView(throwerweight)
                        val detonateHeightEditText = mShoutViewContent.findViewById<EditText>(R.id.detonateHeight)
                        detonateHeightEditText.setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) InputMethodUtils.openInputMethod(
                                detonateHeightEditText,
                                "yk_payload_weight_op"
                            )
                            false
                        }
                        // 测量内容并决定是否旋转
                        measureAndAdjustLayout()
                    }
                    7 -> {
                        opened = 7
                        mShoutViewContent.addView(lightYl300Weight)
                    }
                    6 -> {
                        opened = 6
                        mShoutViewContent.addView(emitterWeight)
                    }
                    5 -> {
                        opened = 5
                        mShoutViewContent.addView(cacheNetWeight)
                    }
                    4 -> {
                        opened = 4
                        mShoutViewContent.addView(unitreeLightWeight)
                    }
                    3 -> {
                        opened = 3
                        mShoutViewContent.addView(realTimeShoutWeight)
                    }
                    2 -> {
                        opened = 2
                        mShoutViewContent.addView(recordShoutWeight)
                        recordShoutWeight.onShow()
                    }
                    1 -> {
                        opened = 1
                        mShoutViewContent.addView(ttsShoutWeight)
                        val editText = mShoutViewContent.findViewById<EditText>(R.id.tts_text)
                        editText.setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) InputMethodUtils.openInputMethod(
                                editText,
                                "yk_payload_weight_op"
                            )
                            false
                        }
                    }
                }
                EasyFloat.show("yk_payload_weight_op")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mShoutComp.isVisible
    }

    fun getIsRotated(): Boolean {
        return this.isRotated
    }

    // 测量内容并调整布局
    fun measureAndAdjustLayout() {
        // 等待布局完成
        mShoutComp.post {
            // 测量内容实际尺寸
            mShoutViewContent.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )

            val contentWidth = mShoutViewContent.measuredWidth
            val contentHeight = mShoutViewContent.measuredHeight

            Log.d(TAG, "Measured content: ${contentWidth}px x ${contentHeight}px")

            // 获取屏幕宽度
            val screenWidth = getScreenSize().first
            Log.d(TAG, "Screen width: ${screenWidth}px")

            // 将dp转换为px
            val minWidthPx = dpToPx(300f)

            if (contentWidth > minWidthPx) {
                // 内容宽度超过300dp，需要处理
                if (contentWidth <= screenWidth) {
                    // 1. 宽度不超过屏幕：直接扩展悬浮窗
                    expandFloatWindow(contentWidth, contentHeight)
                } else {
                    // 2. 宽度超过屏幕：旋转90度显示
                    rotateContent90Degrees(contentWidth, contentHeight)
                }
            } else {
                // 3. 宽度不超过300dp：保持原样
                keepOriginalLayout()
            }
        }
    }

    // 扩展悬浮窗宽度
    private fun expandFloatWindow(contentWidth: Int, contentHeight: Int) {
        Log.d(TAG, "Expanding float window to fit content: ${contentWidth}px")

        // 保存原始尺寸
        originalContentWidth = contentWidth
        originalContentHeight = contentHeight

        // 设置悬浮窗的宽度为内容宽度（加上一些内边距）
        val padding = dpToPx(20f)
        val newWidth = contentWidth + padding

        val params = mShoutComp.layoutParams
        params.width = newWidth
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        mShoutComp.layoutParams = params

        // 重新测量和布局
        mShoutComp.requestLayout()

        Log.d(TAG, "Float window expanded to: ${newWidth}px")
    }

    // 旋转内容90度
    @SuppressLint("ClickableViewAccessibility")
    private fun rotateContent90Degrees(contentWidth: Int, contentHeight: Int) {
        Log.d(TAG, "Rotating content 90 degrees. Original: ${contentWidth}px x ${contentHeight}px")

        // 保存原始尺寸
        originalContentWidth = contentWidth
        originalContentHeight = contentHeight

        // 标记为已旋转
        isRotated = true

        // 1. 先设置新的尺寸，再旋转
        val rotatedWidth = contentHeight + dpToPx(24f + 10f)
        val rotatedHeight = contentWidth + dpToPx(10f)

        // 2. 先设置新尺寸
        val params = mShoutComp.layoutParams
        params.width = rotatedWidth
        params.height = rotatedHeight
        mShoutComp.layoutParams = params

        // 3. 等待布局更新后再旋转
        mShoutComp.post {
            // 设置旋转中心为视图中心
            mShoutView.pivotX = mShoutView.width / 2f
            mShoutView.pivotY = mShoutView.height / 2f

            // 执行旋转动画
            mShoutView.animate()
                .rotation(90f)
                .setDuration(300)
                .withStartAction {
                    // 旋转前确保视图可见
                    mShoutView.visibility = VISIBLE
                    mShoutView.alpha = 0f
                }
                .withEndAction {
                    mShoutView.alpha = 1f
                }
                .start()

            val paramsShoutView = mShoutView.layoutParams
            paramsShoutView.width = rotatedHeight
            paramsShoutView.height = rotatedWidth
            mShoutView.layoutParams = paramsShoutView
        }
    }

    // 重置旋转状态
    private fun resetRotation() {
        if (isRotated) {
            mShoutView.rotation = 0f
            mShoutView.pivotX = mShoutView.width / 2f
            mShoutView.pivotY = mShoutView.height / 2f

            keepOriginalLayout()

            isRotated = false
            Log.d(TAG, "Rotation reset")
        }
    }

    // 保持原始布局
    private fun keepOriginalLayout() {
        val params = mShoutComp.layoutParams
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        mShoutComp.layoutParams = params
        mShoutComp.requestLayout()

        val paramsShoutView = mShoutView.layoutParams
        paramsShoutView.width = ViewGroup.LayoutParams.WRAP_CONTENT
        paramsShoutView.height = ViewGroup.LayoutParams.WRAP_CONTENT
        mShoutView.layoutParams = paramsShoutView
        mShoutView.requestLayout()

        val paramsShoutContent = mShoutViewContent.layoutParams
        paramsShoutContent.width = ViewGroup.LayoutParams.WRAP_CONTENT
        paramsShoutContent.height = ViewGroup.LayoutParams.WRAP_CONTENT
        mShoutViewContent.layoutParams = paramsShoutContent
        mShoutViewContent.requestLayout()
    }

    private fun showWindow() {
        realTimeShoutWeight = RealTimeShoutWeight(this)
        ttsShoutWeight = TtsShoutWeight(this)
        recordShoutWeight = RecordShoutWeight(this)
        unitreeLightWeight = UnitreeLightWeight(this)
        cacheNetWeight = CacheNetWeight(this)
        emitterWeight = EmitterWeight(this)
        lightYl300Weight = LightWeight(this)
        throwerweight = ThrowerWeight(this)
        slowDescentDeviceWeight = SlowDescentDeviceWeight(this)
        gripperWeight = GripperWeight(this)
        resqmeWeight = ResqmeWeight(this)
        extinguisherWeight = ExtinguisherWeight(this)
        waterGunWeight = WaterGunWeight(this)
        bucketWeight = BucketWeight(this)
        waterBranchWeight = WaterBranchWeight(this)
        plLightweight = PL_LightWeight(this)
        allInOneSpeakerWeight = AllInOneSpeakerWeight(this)
        allInOneLightWeight = AllInOneLightWeight(this)
        allInOneThrowerWeight = AllInOneThrowerWeight(this)
        allInOneThrowerWeight.attachFloatingWindow(this)
        allInOneFpvWeight = AllInOneFpvWeight(this)
        allInOneFpvWeight.attachFloatingWindow(this)
        fourInOne2SpeakerWeight = FourInOne2SpeakerWeight(this)
        fourInOne2LightWeight = FourInOne2LightWeight(this)

        if (!isInit) {
            EasyFloat.with(applicationContext)
                // 设置浮窗xml布局文件/自定义View，并可设置详细信息
                .setLayout(R.layout.payload_weight) {
                    mShoutComp = it.findViewById(R.id.shoutComp)
                    mShoutView = it.findViewById(R.id.shoutView)
                    lockBtn = it.findViewById(R.id.lockBtn)
                    it.isFocusable = true;
                    // 初始化关闭喊话界面
                    mShoutComp.visibility = View.GONE
                    mWindowTitle = it.findViewById(R.id.windowTitle)
                    mShoutViewContent = it.findViewById(R.id.shout_view_content)
                    mShoutViewContent.setOnClickListener {
                        Log.i(TAG, "mShoutViewContent clicked....")
                    }
                    // 弹窗页面是否可拖拽控制
                    lockBtn.setOnClickListener {
                        isLockWindow = !isLockWindow
                        // 锁住，不可拖拽
                        if(isLockWindow) {
                            lockBtn.setBackgroundResource(R.drawable.lock_up)
                            EasyFloat.dragEnable(false, "yk_payload_weight_op")
                            showToast(R.string.window_locked)
                        }
                        // 解锁，可拖拽
                        else {
                            lockBtn.setBackgroundResource(R.drawable.unlock)
                            EasyFloat.dragEnable(true, "yk_payload_weight_op")
                            showToast(R.string.window_unlocked)
                        }
                    }
                }
                // 设置浮窗显示类型，默认只`在当前Activity显示，可选一直显示、仅前台显示
                .setShowPattern(ShowPattern.ALL_TIME)
                // 设置吸附方式，共15种模式，详情参考SidePattern
                .setSidePattern(SidePattern.DEFAULT)
                // 设置浮窗的标签，用于区分多个浮窗
                .setTag("yk_payload_weight_op")
                // 设置浮窗是否可拖拽
                .setDragEnable(true)
                // 浮窗是否包含EditText，默认不包含
                .hasEditText(true)
                // 设置浮窗固定坐标，ps：设置固定坐标，Gravity属性和offset属性将无效
//            .setLocation(100, 200)
                // 设置浮窗的对齐方式和坐标偏移量
                .setGravity(Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL, 0, 0)
                // 设置当布局大小变化后，整体view的位置对齐方式
                .setLayoutChangedGravity(Gravity.CENTER)
                // 设置拖拽边界值
//                .setBorder(100, 100, 800, 800)
                // 设置宽高是否充满父布局，直接在xml设置match_parent属性无效
                .setMatchParent(widthMatch = false, heightMatch = false)
                // 设置浮窗的出入动画，可自定义，实现相应接口即可（策略模式），无需动画直接设置为null
                .setAnimator(DefaultAnimator())
                // 设置系统浮窗的不需要显示的页面
                .setFilter(AddRecordActivity::class.java)
//                .setFilter(MainActivity::class.java, CompleteWidgetActivity::class.java)
                // 设置系统浮窗的有效显示高度（不包含虚拟导航栏的高度），基本用不到，除非有虚拟导航栏适配问题
                .setDisplayHeight { context -> DisplayUtils.rejectedNavHeight(context) }
                // 浮窗的一些状态回调，如：创建结果、显示、隐藏、销毁、touchEvent、拖拽过程、拖拽结束。
                // ps：通过Kotlin DSL实现的回调，可以按需复写方法，用到哪个写哪个
                .registerCallback {
                    createResult { isCreated, msg, view ->

                    }
                    show {
                    }
                    hide {
                    }
                    dismiss {
                        Log.w(TAG, "yk_payload_weight_op float dismiss")
                        resetShoutBtnsBackground()
                    }
                    touchEvent { view, motionEvent ->

                    }
                    drag { view, motionEvent -> }
                    dragEnd { }

                }
                .show()

            isInit = true
        }
    }

    fun getScreenSize(includeSystemBars: Boolean = true): Pair<Int, Int> {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 获取当前窗口的metrics
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            val windowInsets = windowMetrics.windowInsets

            if (!includeSystemBars) {
                // 获取系统栏的insets
                val systemBarInsets = windowInsets.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                )

                // 计算可用区域
                val availableWidth = bounds.width() - systemBarInsets.left - systemBarInsets.right
                val availableHeight = bounds.height() - systemBarInsets.top - systemBarInsets.bottom

                Log.d(TAG, "计算可用区域: 窗口${bounds.width()}x${bounds.height()}, " +
                        "系统栏Insets[L=${systemBarInsets.left}, T=${systemBarInsets.top}, " +
                        "R=${systemBarInsets.right}, B=${systemBarInsets.bottom}], " +
                        "可用${availableWidth}x${availableHeight}")

                return Pair(availableWidth, availableHeight)
            }
            return Pair(bounds.width(), bounds.height())

        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)

            if (!includeSystemBars) {
                val resources = resources

                // 获取状态栏高度
                val statusBarId = resources.getIdentifier("status_bar_height", "dimen", "android")
                var statusBarHeight = 0
                if (statusBarId > 0) {
                    statusBarHeight = resources.getDimensionPixelSize(statusBarId)
                }

                // 获取导航栏尺寸
                val navigationBarId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
                var navigationBarHeight = 0
                if (navigationBarId > 0) {
                    navigationBarHeight = resources.getDimensionPixelSize(navigationBarId)
                }

                // 获取导航栏宽度（用于横向导航栏）
                val navigationBarWidthId = resources.getIdentifier("navigation_bar_width", "dimen", "android")
                var navigationBarWidth = 0
                if (navigationBarWidthId > 0) {
                    navigationBarWidth = resources.getDimensionPixelSize(navigationBarWidthId)
                }

                // 判断设备方向
                val display = windowManager.defaultDisplay
                val rotation = display.rotation

                // 判断导航栏位置
                val isNavigationBarAtBottom = when (rotation) {
                    Surface.ROTATION_0, Surface.ROTATION_180 -> {
                        // 竖屏时，导航栏通常在底部
                        true
                    }
                    Surface.ROTATION_90, Surface.ROTATION_270 -> {
                        // 横屏时，导航栏可能在底部或右侧
                        // 可以通过配置判断
                        val config = resources.configuration
                        val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE

                        // 有些设备在横屏时导航栏在右侧
                        if (isLandscape) {
                            // 检查是否有导航栏高度（底部）和宽度（右侧）
                            val hasBottomNav = navigationBarHeight > 0
                            val hasSideNav = navigationBarWidth > 0

                            // 通常如果导航栏宽度 > 0 且高度较小，可能在侧面
                            hasSideNav && navigationBarWidth > navigationBarHeight
                        } else {
                            true
                        }
                    }
                    else -> true
                }

                val availableWidth: Int
                val availableHeight: Int

                if (isNavigationBarAtBottom) {
                    // 导航栏在底部
                    availableWidth = displayMetrics.widthPixels
                    availableHeight = displayMetrics.heightPixels - statusBarHeight - navigationBarHeight
                    Log.d(TAG, "导航栏在底部: 状态栏${statusBarHeight}px, 导航栏${navigationBarHeight}px")
                } else {
                    // 导航栏在侧面（右侧或左侧）
                    availableWidth = displayMetrics.widthPixels - navigationBarWidth
                    availableHeight = displayMetrics.heightPixels - statusBarHeight
                    Log.d(TAG, "导航栏在侧面: 状态栏${statusBarHeight}px, 导航栏宽度${navigationBarWidth}px")
                }

                Log.d(TAG, "计算可用区域: 屏幕${displayMetrics.widthPixels}x${displayMetrics.heightPixels}, " +
                        "可用${availableWidth}x${availableHeight}")

                return Pair(availableWidth, availableHeight)
            }
            return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
        }
    }

    // 悬浮窗全屏 (使用可用区域)
    fun fullScreen() {
        val (screenWidth, screenHeight) = getScreenSize(includeSystemBars = false)

        // 设置 shoutComp 的宽高（保留边距）
        val params = mShoutComp.layoutParams
        params.width = screenWidth
        params.height = screenHeight
        mShoutComp.layoutParams = params

        val floatWindowView = EasyFloat.getFloatView("yk_payload_weight_op")
        floatWindowView?.let { view ->
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val layoutParams = view.layoutParams as WindowManager.LayoutParams

            // 关键修改：移除 FLAG_LAYOUT_NO_LIMITS，避免坐标混乱
            layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

            // 移除 FLAG_LAYOUT_NO_LIMITS 避免边界问题
            layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS.inv()

            try {
                windowManager.updateViewLayout(view, layoutParams)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "更新悬浮窗布局参数失败: ${e.message}")
            }
        }

        val decorView = mShoutComp.rootView
        val uiOptions = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        decorView.systemUiVisibility = uiOptions
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    // 恢复原有尺寸
    fun recoverSize() {
        keepOriginalLayout()
        // 如果已经旋转
        if(isRotated) {
            measureAndAdjustLayout()
            return
        }

        val floatWindowView = EasyFloat.getFloatView("yk_payload_weight_op")
        floatWindowView?.let { view ->
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val layoutParams = view.layoutParams as WindowManager.LayoutParams

            // 清除全屏和布局相关的标志位
            layoutParams.flags = layoutParams.flags and
                    (WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN.inv() and
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS.inv())

            try {
                windowManager.updateViewLayout(view, layoutParams)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "恢复悬浮窗布局参数失败: ${e.message}")
            }
        }

        val decorView = mShoutComp.rootView
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    fun setTitleText(textId: Int) {
        mWindowTitle.setText(textId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "onDestroy........")
        realTimeShoutWeight.releaseResources()
        allInOneSpeakerWeight.releaseResources()
        fourInOne2SpeakerWeight.releaseResources()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        // 停止程序时关闭所有悬浮窗防止无法点击
//        Log.w(TAG, "onUnbind........")

        EasyFloat.dismiss("yk_payload_weight")
        EasyFloat.dismiss("yk_payload_weight_op")
        return super.onUnbind(intent)

    }

    private fun showToast(msg: Int) {
        run {
            Toast.makeText(
                this,
                msg,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    inner class PayloadWeightBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): PayloadWeight = this@PayloadWeight
        fun setSVVisibility(v: Int, view: View): Boolean {
            return this@PayloadWeight.setSVVisibility(v, view)
        }

        fun showWindow() {
            return this@PayloadWeight.showWindow()
        }

        fun openFloatingWindow() {
            return this@PayloadWeight.openFloatingWindow()
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    // 判断是否连接，未连接则不显示
    private fun setBtnShow(){
        val timer = Timer();
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask(){
            override fun run() {
                val isConnectedMegaphone = (megaphoneService?.getIsConnected() == true);// 喊话器
                val isConnectedYA3 = (megaphoneService?.getIsConnectedYA3() == true); // 四合一
                val isConnectedCacheNet = cacheNetWeight.cacheNetService.getIsConnected(); // 网枪
                val isConnectedEmitter = emitterWeight.emitterService.getIsConnected(); // 38mm发射器
                val isConnectedLightYl300 = lightYl300Weight.lightService.getIsConnected(); // 探照灯
                val isConnectedThrower = throwerweight.throwerService.getIsConnected(); // 抛投器
                val isConnectedSlowDescentDevice = slowDescentDeviceWeight.slowDescentDeviceService.getIsConnected(); // 缓降器
                val isConnectedGripper = gripperWeight.gripperService.getIsConnected(); // 机械爪
                val isConnectedResqme = resqmeWeight.resqmeService.getIsConnected(); // 破窗器
                val isConnectedExtinguisher = extinguisherWeight.extinguisherService.getIsConnected(); // 灭火罐
                val isConnectedWaterGun = waterGunWeight.waterGunService.getIsConnected(); // 清洗水枪
                val isConnectedBucket = bucketWeight.bucketService.getIsConnected(); // 吊桶
                val isConnectedWaterBranch = waterBranchWeight.waterBranchService.getIsConnected(); // 消防水枪
                val isConnectedPLLight = plLightweight.plLightService.getIsConnected(); // 品灵探照灯
                val isConnectedAllInOne = allInOneService.getIsConnected(); // 多合一
                val isConnectedFourInOne2 = fourInOne2Service.getIsConnected(); // 四合一二代

//                val isConnectedMegaphone = true;// 喊话器
//                val isConnectedYA3 = true; // 四合一
//                val isConnectedCacheNet = true; // 网枪
//                val isConnectedEmitter = true; // 38mm发射器
//                val isConnectedLightYl300 = true; // 探照灯
//                val isConnectedThrower = true; // 抛投器
//                val isConnectedSlowDescentDevice = true; // 缓降器
//                val isConnectedGripper = true; // 机械爪
//                val isConnectedResqme = true; // 破窗器
//                val isConnectedExtinguisher = true; // 灭火罐
//                val isConnectedWaterGun = true; // 清洗水枪
//                val isConnectedBucket = true; // 吊桶
//                val isConnectedWaterBranch = true; // 消防水枪
//                val isConnectedPLLight = true; // 品灵探照灯
//                val isConnectedAllInOne = true; // 多合一
//                val isConnectedFourInOne2 = true; // 四合一二代
                    // 喊话器
                if(isConnectedMegaphone){
                    // 已连接，显示
                    handler.post {
                        mShoutBtn.visibility = VISIBLE;
                        mTTSBtn.visibility = VISIBLE;
                        mRecordBtn.visibility = VISIBLE;
                        emptyText.visibility = GONE;
                    }
                }
                // 捕捉网
                if(isConnectedCacheNet){
                    handler.post {
                        mCacheNetBtn.visibility = VISIBLE;
                        emptyText.visibility = GONE;
                    }
                }
                // 38mm发射器
                if(isConnectedEmitter){
                    handler.post {
                        mEmitterBtn.visibility = VISIBLE;
                        emptyText.visibility = GONE;
                    }
                }
                // 探照灯
                if(isConnectedLightYl300){
                    handler.post {
                        mLightYl300Btn.visibility = VISIBLE;
                        emptyText.visibility = GONE;
                    }
                }
                // 抛投
                if(isConnectedThrower){
                    handler.post {
                        throwerBtn.visibility = VISIBLE;
                        emptyText.visibility = GONE;
                    }
                }
                // 缓降器
                if(isConnectedSlowDescentDevice){
                    handler.post {
                        slowDescentDeviceBtn.visibility = VISIBLE;
                        emptyText.visibility = GONE;
                    }
                }
                // 机械爪
                if(isConnectedGripper){
                    handler.post {
                        gripperBtn.visibility = VISIBLE;
                        emptyText.visibility = GONE;
                    }
                }
                // 破窗器
                if(isConnectedResqme){
                    handler.post {
                        resqmeBtn.visibility = VISIBLE;
                        emptyText.visibility = GONE;
                    }
                }
                // 灭火罐
                if(isConnectedExtinguisher){
                    handler.post {
                        extinguisherBtn.visibility = VISIBLE;
                        emptyText.visibility = GONE;
                    }
                }
                // 水枪
                if(isConnectedWaterGun){
                    handler.post {
                        waterGunBtn.visibility = VISIBLE;
                        emptyText.visibility = GONE;
                    }
                }
                // 吊桶
                if(isConnectedBucket){
                    handler.post {
                        bucketBtn.visibility = VISIBLE;
                        emptyText.visibility = GONE;
                    }
                }
                // 消防水枪
                if(isConnectedWaterBranch){
                    handler.post {
                        waterBranchBtn.visibility = VISIBLE;
                        emptyText.visibility = GONE;
                    }
                }

                // 四合一（喊话器、灯光、红蓝、收音），放到最后判断，此时该隐藏的已经都隐藏了，只需要判断是否显示
                if(isConnectedYA3){
                    handler.post {
                        // 喊话器、收音
                        mShoutBtn.visibility = VISIBLE;
                        mTTSBtn.visibility = VISIBLE;
                        mRecordBtn.visibility = VISIBLE;
                        // 灯光、红蓝
                        mLightBtn.visibility = VISIBLE;
                        emptyText.visibility = GONE;
                    }
                }
                // 品灵探照灯
                if(isConnectedPLLight){
                    handler.post {
                        plLightBtn.visibility = VISIBLE;
                        emptyText.visibility = GONE;
                    }
                }
                // 多合一
                if(isConnectedAllInOne){
                    handler.post {
                        allInOneSpeakerBtn.visibility = VISIBLE;
                        allInOneLightBtn.visibility = VISIBLE
                        allInOneThrowerBtn.visibility = VISIBLE
                        allInOneFpvBtn.visibility = VISIBLE
                        emptyText.visibility = GONE;
                    }
                }
                // 四合一二代
                if(isConnectedFourInOne2){
                    handler.post {
                        fourInOne2SpeakerBtn.visibility = VISIBLE;
                        fourInOne2LightBtn.visibility = VISIBLE
                        emptyText.visibility = GONE;
                    }
                }
            }
        }
        // 定时器，1秒后开始执行，每1秒执行一次
        timer.schedule(task, 100, 1000);
    }
}