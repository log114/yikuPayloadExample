package com.example.yikupayloadexample;

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.yiku.yikupayloadSDK.service.BaseCacheNetService
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

class CacheNetWeight(
    context: Context,
    attr: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val isDevice2: Boolean = false
) : LinearLayout(context, attr, defStyleAttr) {
    private val TAG = "CacheNetWeight"
    private lateinit var mLightView: View
    lateinit var cacheNetService: BaseCacheNetService
    private lateinit var mSafetySwitchSwitch: Switch
    private var isConnecting: Boolean = false
    private var isFirstConnect: Boolean = true
    private var host: String? = ""

    constructor(context: Context) : this(context, null, 0)

    init {
        host = if(isDevice2) {
            preferences?.getString("CacheNetHost2", "")
        } else {
            preferences?.getString("CacheNetHost", "")
        }
        initView(context)
    }


    private fun initView(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.cache_net_weight, this, true)
        mLightView = findViewById(R.id.cache_net_view)
        mSafetySwitchSwitch = findViewById(R.id.safetySwitchSwitch)
        val mCacheNetLaunchBtn = findViewById<Button>(R.id.cacheNetLaunchBtn)
        cacheNetService = BaseCacheNetService()
        if(host != null && "" != host) {
            cacheNetService.setIp(host!!)
        }
        Log.d(TAG, "isDevice2=${isDevice2}, host=${host}")
        if((isDevice2 && host != "" && host != null) || !isDevice2) {
            setConnectState()
        }

        mSafetySwitchSwitch.setOnCheckedChangeListener { _, isChecked ->
            mCacheNetLaunchBtn.isEnabled = isChecked
        }

        mCacheNetLaunchBtn.setOnClickListener {
            Log.i(TAG, "mSafetySwitchSwitch.isChecked:${mSafetySwitchSwitch.isChecked}")
            try {
                if (!mSafetySwitchSwitch.isChecked) {
                    showToast(R.string.need_to_open_safety_switch)
                } else {
                    Log.i(TAG, "发射...")
                    cacheNetService.launch()
                    showToast(R.string.launch_command_executed)
                    mSafetySwitchSwitch.isChecked = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(R.string.launch_failed)
            }

        }
    }

    fun ByteArray.toHex(): String =
        joinToString(separator = "") { eachByte -> "%02x ".format(eachByte) }


    private fun showToast(msg: Int) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(
                MApplication.applicationContext, msg, Toast.LENGTH_LONG
            ).show()
        }
    }

    // 定时器，判断连接状态
    private fun setConnectState(){
        val timer = Timer();
        val statusDot = findViewById<View>(R.id.statusDot)
        val background = statusDot.background as GradientDrawable
        val connectText = findViewById<TextView>(R.id.cacheNetConnect)
        val handler = Handler(Looper.getMainLooper())
        val task = object : TimerTask(){
            override fun run() {
                if(cacheNetService.getIsConnected()){
                    handler.post {
                        connectText.setText(R.string.connection_status_connected)
                        background.setColor(ContextCompat.getColor(context, R.color.green))
                    }
                }
                else if(!isConnecting){
                    isConnecting = true
                    handler.post {
                        connectText.setText(R.string.connection_status_notconnected)
                        background.setColor(ContextCompat.getColor(context, R.color.red))
                    }
                    // 尝试重连
                    thread {
                        if(!isFirstConnect) {
                            Thread.sleep(5000)
                        }
                        isFirstConnect = false
                        while (!cacheNetService.connect()) {
                            Thread.sleep(1000)
                        }
                        isConnecting = false
                    }
                }
            }
        }
        // 定时器，100毫秒后开始执行，每1秒执行一次
        timer.schedule(task, 100, 1000);
    }
}