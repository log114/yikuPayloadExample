package com.example.yikupayloadexample

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.yiku.yikupayload_sdk.service.MegaphoneService
import com.yiku.yikupayload_sdk.util.CacheNetHost
import com.yiku.yikupayload_sdk.util.EmitterHost
import com.yiku.yikupayload_sdk.util.ExtinguisherHost
import com.yiku.yikupayload_sdk.util.GripperHost
import com.yiku.yikupayload_sdk.util.LightHost
import com.yiku.yikupayload_sdk.util.ResqmeHost
import com.yiku.yikupayload_sdk.util.ShoutHost
import com.yiku.yikupayload_sdk.util.SlowDescentDeviceHost
import com.yiku.yikupayload_sdk.util.ThrowerHost
import com.yiku.yikupayload_sdk.util.YA3Host

class SettingActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.setting)


        val save = findViewById<Button>(R.id.save)
        var shout  = findViewById<EditText>(R.id.ShoutHostIP)
        val light = findViewById<EditText>(R.id.LightHostIP)
        val cacheNet = findViewById<EditText>(R.id.CacheNetHostIP)
        val emitter = findViewById<EditText>(R.id.EmitterHostIP)
        val YA3 = findViewById<EditText>(R.id.YA3HostIP)
        val thrower = findViewById<EditText>(R.id.ThrowerHostIP)
        val mRebootYmBtn = findViewById<Button>(R.id.reboot_ym_btn)
        val slowDescentDevice = findViewById<EditText>(R.id.SlowDescentDeviceHostIP)
        val gripper = findViewById<EditText>(R.id.GripperHostIP)
        val resqme = findViewById<EditText>(R.id.ResqmeHostIP)
        val extinguisher = findViewById<EditText>(R.id.ExtinguisherHostIP)

        //        获取了 SharedPreferences 对象
//        preferences = getSharedPreferences("myPreferences", MODE_PRIVATE)
        // 从 SharedPreferences 中读取上次存储的值，并显示在 Shout 喊话器 中
        val valueShoutHost = preferences?.getString("ShoutHost", "")  //喊话器的
        val valueLightHost = preferences?.getString("LightHost", "") //灯的
        val valueCacheNetHost = preferences?.getString("CacheNetHost", "") //捕捉网的
        val valueEmitterHost = preferences?.getString("EmitterHost", "")//38mm发射器
        val valueYA3Host = preferences?.getString("YA3Host", "")//四合一
        val valueThrowerHost = preferences?.getString("ThrowerHost", "")//抛投器
        val valueSlowDescentDeviceHost = preferences?.getString("SlowDescentDeviceHost", "")//缓降器
        val valueGripperHost = preferences?.getString("GripperHost", "")//机械爪
        val valueResqmeHost = preferences?.getString("ResqmeHost", "")//破窗器
        val valueExtinguisherHost = preferences?.getString("ExtinguisherHost", "")//灭火罐
        // 当未设置过ip时，ip显示为Host.kt里面的值，否则显示设置后的值
        if (valueShoutHost == "") {
            shout.setText(ShoutHost)
        } else {
            shout.setText(valueShoutHost)
        }

        if (valueLightHost == "") {
            light.setText(LightHost)
        } else {
            light.setText(valueLightHost)
        }

        if (valueCacheNetHost == "") {
            cacheNet.setText(CacheNetHost)
        } else {
            cacheNet.setText(valueCacheNetHost)
        }

        if (valueEmitterHost == "") {

            emitter.setText(EmitterHost)
        } else {
            emitter.setText(valueEmitterHost)
        }

        if (valueYA3Host == "") {
            YA3.setText(YA3Host)
        } else {
            YA3.setText(valueYA3Host)
        }

        if (valueThrowerHost == "") {
            thrower.setText(ThrowerHost)
        } else {
            thrower.setText(valueThrowerHost)
        }

        if (valueSlowDescentDeviceHost == "") {
            slowDescentDevice.setText(SlowDescentDeviceHost)
        } else {
            slowDescentDevice.setText(valueSlowDescentDeviceHost)
        }

        if (valueGripperHost == "") {
            gripper.setText(GripperHost)
        } else {
            gripper.setText(valueGripperHost)
        }

        if (valueResqmeHost == "") {
            resqme.setText(ResqmeHost)
        } else {
            resqme.setText(valueResqmeHost)
        }

        if (valueExtinguisherHost == "") {
            extinguisher.setText(ExtinguisherHost)
        } else {
            extinguisher.setText(valueExtinguisherHost)
        }

        save.setOnClickListener {
            val textShoutHost = shout.text.toString()
            val textLightHost = light.text.toString()
            val textCacheNetHost = cacheNet.text.toString()
            val textEmitterHost = emitter.text.toString()
            val textYA3Host = YA3.text.toString()
            val textThrowerHost = thrower.text.toString()
            val textSlowDescentDeviceHost = slowDescentDevice.text.toString()
            val textGripperHost = gripper.text.toString()
            val textResqmeHost = resqme.text.toString()
            val textExtinguisherHost = extinguisher.text.toString()
            // 将修改后的值存储到 SharedPreferences 中
            val editer = preferences!!.edit()
            editer.putString("ShoutHost", textShoutHost)
            editer.putString("LightHost", textLightHost)
            editer.putString("CacheNetHost", textCacheNetHost)
            editer.putString("EmitterHost", textEmitterHost)
            editer.putString("YA3Host", textYA3Host)
            editer.putString("ThrowerHost", textThrowerHost)
            editer.putString("SlowDescentDeviceHost", textSlowDescentDeviceHost)
            editer.putString("GripperHost", textGripperHost)
            editer.putString("ResqmeHost", textResqmeHost)
            editer.putString("ExtinguisherHost", textExtinguisherHost)
            editer.apply()
            finish();  //直接关闭当前页面
        }
        mRebootYmBtn.setOnClickListener{
            try {
                if (megaphoneService == null){
                    megaphoneService = MegaphoneService();
                    val host = preferences?.getString("ShoutHost", "")
                    if(host != null && "" != host) {
                        megaphoneService?.setIp(host)
                    }
                }
                if (megaphoneService?.getIsConnected() == false){
                    megaphoneService?.connect()
//                    Thread.sleep(1000)
                }
                megaphoneService?.reboot()
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }
}










