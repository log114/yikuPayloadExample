package com.example.yikupayloadexample

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.*
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.anim.DefaultAnimator
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import com.lzf.easyfloat.utils.DisplayUtils
import com.lzf.easyfloat.utils.InputMethodUtils
import java.util.Timer
import java.util.TimerTask


class PayloadWeight : Service() {
    private val TAG = "ShoutWeight"
    private lateinit var mShoutView: View
    private val binder = PayloadWeightBinder()
    private lateinit var mShoutViewContent: LinearLayout
    private var opened: Int = 0

    private lateinit var mVideoWindowView: View

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
    private lateinit var lockBtn: ImageView
    private var isLockWindow = false
    private var isVideoWindowInit = false

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
    fun setSVVisibility(type: Int, view: View): Boolean {
        Log.i(TAG, "mShoutView. setSVVisibility:${mShoutView.visibility}, opened:${opened}")
        if (mShoutView.visibility == INVISIBLE || opened != type) {
            mShoutView.visibility = VISIBLE
        } else {
            mShoutView.visibility = INVISIBLE
            EasyFloat.hide("yk_payload_weight_op")
            EasyFloat.hide("video_window")
//            popupWindow.dismiss()
            opened = 0
        }
        try {
            if (mShoutView.visibility == VISIBLE) {
                if (type == 12) {
                    opened = 12
                    mShoutViewContent.removeAllViews()
                    mShoutViewContent.addView(extinguisherWeight)
                    EasyFloat.show("yk_payload_weight_op")
                }
                if (type == 11) {
                    opened = 11
                    mShoutViewContent.removeAllViews()
                    mShoutViewContent.addView(resqmeWeight)
                    EasyFloat.show("yk_payload_weight_op")
                    // 视频窗口
                    if(!isVideoWindowInit) {
                        EasyFloat.with(applicationContext)
                            // 设置浮窗xml布局文件/自定义View，并可设置详细信息
                            .setLayout(R.layout.video_window){
//                                mVideoWindowView = it.findViewById(R.id.videoWindowView)
                                it.isFocusable = true;
                            }
                            // 设置浮窗显示类型，默认只`在当前Activity显示，可选一直显示、仅前台显示
                            .setShowPattern(ShowPattern.ALL_TIME)
                            // 设置吸附方式，共15种模式，详情参考SidePattern
                            .setSidePattern(SidePattern.DEFAULT)
                            // 设置浮窗的标签，用于区分多个浮窗
                            .setTag("video_window")
                            // 设置浮窗是否可拖拽
                            .setDragEnable(true)
                            // 浮窗是否包含EditText，默认不包含
                            .hasEditText(true)
                            // 设置浮窗的对齐方式和坐标偏移量
                            .setGravity(Gravity.END or Gravity.BOTTOM, 0, 0)
                            // 设置当布局大小变化后，整体view的位置对齐方式
                            .setLayoutChangedGravity(Gravity.BOTTOM or Gravity.END)
                            // 设置宽高是否充满父布局，直接在xml设置match_parent属性无效
                            .setMatchParent(widthMatch = false, heightMatch = false)
                            // 设置浮窗的出入动画，可自定义，实现相应接口即可（策略模式），无需动画直接设置为null
                            .setAnimator(DefaultAnimator())
                            // 设置系统浮窗的不需要显示的页面
                            .setFilter(AddRecordActivity::class.java)
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
                                    Log.w(TAG, "video_window float dismiss")
                                    resetShoutBtnsBackground()
                                }
                                touchEvent { view, motionEvent ->

                                }
                                drag { view, motionEvent -> }
                                dragEnd { }

                            }
//                            .show()
                        isVideoWindowInit = true
                    }
                    EasyFloat.show("video_window")
                }
                else {
                    EasyFloat.hide("video_window")
                }
                if (type == 10) {
                    opened = 10
                    mShoutViewContent.removeAllViews()
                    mShoutViewContent.addView(gripperWeight)
                    EasyFloat.show("yk_payload_weight_op")
                }
                if (type == 9) {
                    opened = 9
                    mShoutViewContent.removeAllViews()
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
                    EasyFloat.show("yk_payload_weight_op")
                }
                if (type == 8) {
                    opened = 8
                    mShoutViewContent.removeAllViews()
                    mShoutViewContent.addView(throwerweight)
                    val detonateHeightEditText = mShoutViewContent.findViewById<EditText>(R.id.detonateHeight)
                    detonateHeightEditText.setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) InputMethodUtils.openInputMethod(
                            detonateHeightEditText,
                            "yk_payload_weight_op"
                        )
                        false
                    }
                    EasyFloat.show("yk_payload_weight_op")
                }
                if (type == 7) {
                    opened = 7
                    mShoutViewContent.removeAllViews()
                    mShoutViewContent.addView(lightYl300Weight)
                    EasyFloat.show("yk_payload_weight_op")
                }
                if (type == 6) {
                    opened = 6
                    mShoutViewContent.removeAllViews()
                    mShoutViewContent.addView(emitterWeight)
                    EasyFloat.show("yk_payload_weight_op")
                }
                if (type == 5) {
                    opened = 5
                    mShoutViewContent.removeAllViews()
                    mShoutViewContent.addView(cacheNetWeight)
                    EasyFloat.show("yk_payload_weight_op")
                }
                if (type == 4) {
                    opened = 4
                    mShoutViewContent.removeAllViews()
                    mShoutViewContent.addView(unitreeLightWeight)
                    EasyFloat.show("yk_payload_weight_op")
//                popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
//                Log.i(TAG, "width:${popupWindow.width}，height:${popupWindow.height}")
                }
                if (type == 3) {
                    opened = 3
                    mShoutViewContent.removeAllViews()
                    mShoutViewContent.addView(realTimeShoutWeight)
                    EasyFloat.show("yk_payload_weight_op")
                }
                if (type == 1) {
                    opened = 1
                    mShoutViewContent.removeAllViews()
                    mShoutViewContent.addView(ttsShoutWeight)
                    val editText = mShoutViewContent.findViewById<EditText>(R.id.tts_text)
                    editText.setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) InputMethodUtils.openInputMethod(
                            editText,
                            "yk_payload_weight_op"
                        )
                        false
                    }
                    EasyFloat.show("yk_payload_weight_op")
                }
                if (type == 2) {
                    opened = 2
                    mShoutViewContent.removeAllViews()
                    mShoutViewContent.addView(recordShoutWeight)
                    recordShoutWeight.onShow()
//                popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
                    EasyFloat.show("yk_payload_weight_op")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mShoutView.visibility == VISIBLE
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

        if (!isInit) {
            EasyFloat.with(applicationContext)
                // 设置浮窗xml布局文件/自定义View，并可设置详细信息
                .setLayout(R.layout.payload_weight) {
                    mShoutView = it.findViewById(R.id.shoutView)
                    lockBtn = it.findViewById(R.id.lockBtn)
                    it.isFocusable = true;
//                    // 初始化关闭喊话界面
                    mShoutView.visibility = View.GONE
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

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "onDestroy........")

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
                    // 喊话器
                if(isConnectedMegaphone){
                    // 已连接，显示
                    handler.post {
                        mShoutBtn.visibility = VISIBLE;
                        mTTSBtn.visibility = VISIBLE;
                        mRecordBtn.visibility = VISIBLE;
                        emptyText.visibility = View.GONE;
                    }
                }
                // 捕捉网
                if(isConnectedCacheNet){
                    handler.post {
                        mCacheNetBtn.visibility = VISIBLE;
                        emptyText.visibility = View.GONE;
                    }
                }
                // 38mm发射器
                if(isConnectedEmitter){
                    handler.post {
                        mEmitterBtn.visibility = VISIBLE;
                        emptyText.visibility = View.GONE;
                    }
                }
                // 探照灯
                if(isConnectedLightYl300){
                    handler.post {
                        mLightYl300Btn.visibility = VISIBLE;
                        emptyText.visibility = View.GONE;
                    }
                }
                // 抛投
                if(isConnectedThrower){
                    handler.post {
                        throwerBtn.visibility = VISIBLE;
                        emptyText.visibility = View.GONE;
                    }
                }
                // 缓降器
                if(isConnectedSlowDescentDevice){
                    handler.post {
                        slowDescentDeviceBtn.visibility = VISIBLE;
                        emptyText.visibility = View.GONE;
                    }
                }
                // 机械爪
                if(isConnectedGripper){
                    handler.post {
                        gripperBtn.visibility = VISIBLE;
                        emptyText.visibility = View.GONE;
                    }
                }
                // 破窗器
                if(isConnectedResqme){
                    handler.post {
                        resqmeBtn.visibility = VISIBLE;
                        emptyText.visibility = View.GONE;
                    }
                }
                // 灭火罐
                if(isConnectedExtinguisher){
                    handler.post {
                        extinguisherBtn.visibility = VISIBLE;
                        emptyText.visibility = View.GONE;
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
                        emptyText.visibility = View.GONE;
                    }
                }
            }
        }
        // 定时器，1秒后开始执行，每1秒执行一次
        timer.scheduleAtFixedRate(task, 100, 1000);
    }
}