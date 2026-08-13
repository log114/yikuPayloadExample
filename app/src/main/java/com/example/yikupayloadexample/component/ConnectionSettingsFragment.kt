package com.example.yikupayloadexample.component

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.example.yikupayloadexample.MApplication
import com.example.yikupayloadexample.R
import com.example.yikupayloadexample.megaphoneService
import com.example.yikupayloadexample.preferences
import com.yiku.yikupayloadSDK.service.MegaphoneService
import com.yiku.yikupayloadSDK.util.*

class ConnectionSettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.connection_settings, container, false)

        // 绑定所有 EditText 和 Button
        val shoutEdit = rootView.findViewById<EditText>(R.id.ShoutHostIP)
        val lightEdit = rootView.findViewById<EditText>(R.id.LightHostIP)
        val cacheNetEdit = rootView.findViewById<EditText>(R.id.CacheNetHostIP)
        val emitterEdit = rootView.findViewById<EditText>(R.id.EmitterHostIP)
        val emitterPortEdit = rootView.findViewById<EditText>(R.id.EmitterPort)
        val ya3Edit = rootView.findViewById<EditText>(R.id.YA3HostIP)
        val throwerEdit = rootView.findViewById<EditText>(R.id.ThrowerHostIP)
        val slowDescentDeviceEdit = rootView.findViewById<EditText>(R.id.SlowDescentDeviceHostIP)
        val slowDescentDevicePortEdit = rootView.findViewById<EditText>(R.id.SlowDescentDevicePort)
        val gripperEdit = rootView.findViewById<EditText>(R.id.GripperHostIP)
        val resqmeEdit = rootView.findViewById<EditText>(R.id.ResqmeHostIP)
        val extinguisherEdit = rootView.findViewById<EditText>(R.id.ExtinguisherHostIP)
        val waterGunEdit = rootView.findViewById<EditText>(R.id.WaterGunHostIP)
        val bucketEdit = rootView.findViewById<EditText>(R.id.BucketHostIP)
        val waterBranchEdit = rootView.findViewById<EditText>(R.id.WaterBranchHostIP)
        val plLightEdit = rootView.findViewById<EditText>(R.id.PL_LightHostIP)
        val allInOneEdit = rootView.findViewById<EditText>(R.id.AllInOneHostIP)
        val fourInOne2Edit = rootView.findViewById<EditText>(R.id.FourInOne2HostIP)
        val slowDescentDevice200Edit = rootView.findViewById<EditText>(R.id.SlowDescentDevice200HostIP)
        val waterGunEscapeEdit = rootView.findViewById<EditText>(R.id.WaterGunEscapeHostIP)
        val cargoBoxEdit = rootView.findViewById<EditText>(R.id.CargoBoxHostIP)

        val rebootBtn = rootView.findViewById<Button>(R.id.reboot_ym_btn)
        val saveBtn = rootView.findViewById<Button>(R.id.save)

        // ---------- 加载已保存的数据（若无则显示默认值） ----------
        shoutEdit.setText(preferences?.getString("ShoutHost", ShoutHost))
        lightEdit.setText(preferences?.getString("LightHost", LightHost))
        cacheNetEdit.setText(preferences?.getString("CacheNetHost", CacheNetHost))
        emitterEdit.setText(preferences?.getString("EmitterHost", EmitterHost))
        emitterPortEdit.setText(preferences?.getString("EmitterPort", "8519"))
        ya3Edit.setText(preferences?.getString("YA3Host", YA3Host))
        throwerEdit.setText(preferences?.getString("ThrowerHost", ThrowerHost))
        slowDescentDeviceEdit.setText(preferences?.getString("SlowDescentDeviceHost", SlowDescentDeviceHost))
        slowDescentDevicePortEdit.setText(preferences?.getString("SlowDescentDevicePort", "8519"))
        gripperEdit.setText(preferences?.getString("GripperHost", GripperHost))
        resqmeEdit.setText(preferences?.getString("ResqmeHost", ResqmeHost))
        extinguisherEdit.setText(preferences?.getString("ExtinguisherHost", ExtinguisherHost))
        waterGunEdit.setText(preferences?.getString("WaterGunHost", WaterGunHost))
        bucketEdit.setText(preferences?.getString("BucketHost", BucketHost))
        waterBranchEdit.setText(preferences?.getString("WaterBranchHost", WaterBranchHost))
        plLightEdit.setText(preferences?.getString("PL_LightHost", PLLightHost))
        allInOneEdit.setText(preferences?.getString("AllInOneHost", AllInOneHost))
        fourInOne2Edit.setText(preferences?.getString("FourInOne2Host", FourInOne2Host))
        slowDescentDevice200Edit.setText(preferences?.getString("SlowDescentDevice200Host", SlowDescentDevice200Host))
        waterGunEscapeEdit.setText(preferences?.getString("WaterGunEscapeHost", WaterGunEscapeHost))
        cargoBoxEdit.setText(preferences?.getString("CargoBoxHost", CargoBoxHost))

        // ---------- 保存按钮 ----------
        saveBtn.setOnClickListener {
            preferences?.edit {
                putString("ShoutHost", shoutEdit.text.toString())
                putString("LightHost", lightEdit.text.toString())
                putString("CacheNetHost", cacheNetEdit.text.toString())
                putString("EmitterHost", emitterEdit.text.toString())
                putString("EmitterPort", emitterPortEdit.text.toString())
                putString("YA3Host", ya3Edit.text.toString())
                putString("ThrowerHost", throwerEdit.text.toString())
                putString("SlowDescentDeviceHost", slowDescentDeviceEdit.text.toString())
                putString("SlowDescentDevicePort", slowDescentDevicePortEdit.text.toString())
                putString("GripperHost", gripperEdit.text.toString())
                putString("ResqmeHost", resqmeEdit.text.toString())
                putString("ExtinguisherHost", extinguisherEdit.text.toString())
                putString("WaterGunHost", waterGunEdit.text.toString())
                putString("BucketHost", bucketEdit.text.toString())
                putString("WaterBranchHost", waterBranchEdit.text.toString())
                putString("PL_LightHost", plLightEdit.text.toString())
                putString("AllInOneHost", allInOneEdit.text.toString())
                putString("FourInOne2Host", fourInOne2Edit.text.toString())
                putString("SlowDescentDevice200Host", slowDescentDevice200Edit.text.toString())
                putString("WaterGunEscapeHost", waterGunEscapeEdit.text.toString())
                putString("CargoBoxHost", cargoBoxEdit.text.toString())
            }
            showToast(R.string.config_saved)
        }

        // ---------- 重启喊话器按钮 ----------
        rebootBtn.setOnClickListener {
            try {
                if (megaphoneService == null) {
                    megaphoneService = MegaphoneService()
                    val host = preferences?.getString("ShoutHost", ShoutHost)
                    if (!host.isNullOrEmpty()) {
                        megaphoneService?.setIp(host)
                    }
                }
                if (megaphoneService?.getIsConnected() == false) {
                    megaphoneService?.connect()
                }
                megaphoneService?.reboot()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return rootView
    }

    private fun showToast(msg: Int) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(
                MApplication.applicationContext, msg, Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}