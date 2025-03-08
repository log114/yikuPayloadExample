package com.example.yikupayloadexample

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.yiku.yikupayload.service.GripperService
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

class GripperWeight(context: Context, attr: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "CacheNetWeight"
    private lateinit var mGripperView: View
    lateinit var gripperService: GripperService
    private var isConnecting: Boolean = false
    private var isFirstConnect: Boolean = true

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        initView(context)
    }

    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.gripper_weight, this, true)
        mGripperView = findViewById(R.id.gripper_view)
        val mGripperRiseBtn = findViewById<Button>(R.id.gripperRiseBtn)
        val mGripperDeclineBtn = findViewById<Button>(R.id.gripperDeclineBtn)
        val mGripperStopBtn = findViewById<Button>(R.id.gripperStop)
        val mGripperGrabBtn = findViewById<Button>(R.id.gripperGrabBtn)
        val mGripperReleaseBtn = findViewById<Button>(R.id.gripperReleaseBtn)
        gripperService = GripperService()
        val host = preferences?.getString("GripperHost", "")
        if(host != null && "" != host) {
            gripperService.setIp(host)
        }
        setConnectState()

        // 上升
        mGripperRiseBtn.setOnClickListener {
            gripperService.gripperRise()
        }
        // 下降
        mGripperDeclineBtn.setOnClickListener {
            gripperService.gripperDecline()
        }
        // 紧急制动
        mGripperStopBtn.setOnClickListener {
            gripperService.gripperStop()
        }
        // 抓取
        mGripperGrabBtn.setOnClickListener {
            gripperService.gripperGrab()
        }
        // 释放
        mGripperReleaseBtn.setOnClickListener {
            // 先解锁
            gripperService.gripperStop()
            Thread.sleep(100)
            // 再释放
            gripperService.gripperRelease()
        }
    }

    // 定时器，判断连接状态
    private fun setConnectState(){
        val timer = Timer();
        val connectText = findViewById<TextView>(R.id.gripperConnect)
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask(){
            override fun run() {
                if(gripperService.getIsConnected()){
                    handler.post {
                        connectText.setText(R.string.connection_status_connected)
                    }
                }
                else if(!isConnecting){
                    isConnecting = true
                    handler.post {
                        connectText.setText(R.string.connection_status_notconnected)
                    }
                    // 尝试重连
                    thread {
                        if(!isFirstConnect) {
                            Thread.sleep(10000)
                        }
                        isFirstConnect = false
                        while (!gripperService.connect()) {
                            Thread.sleep(1000)
                        }
                        isConnecting = false
                    }
                }
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.scheduleAtFixedRate(task, 100, 1000);
    }
}