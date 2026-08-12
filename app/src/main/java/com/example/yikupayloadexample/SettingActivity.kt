package com.example.yikupayloadexample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.yikupayloadexample.component.ConnectionSettingsFragment
import com.example.yikupayloadexample.component.DeviceIpModifyFragment
import com.example.yikupayloadexample.component.FirmwareUpdateFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class SettingActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.setting)  // 注意这里是 R.layout.setting，对应您提供的 setting.xml

        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)

        // 设置 ViewPager2 适配器
        val pagerAdapter = SettingsPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        // 关联 TabLayout 与 ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "连接设置"
                1 -> tab.text = "设备IP修改"
                2 -> tab.text = "固件升级"
            }
        }.attach()
    }

    /**
     * ViewPager2 适配器，负责提供三个 Fragment
     */
    inner class SettingsPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ConnectionSettingsFragment()
                1 -> DeviceIpModifyFragment()
                2 -> FirmwareUpdateFragment()
                else -> throw IllegalArgumentException("Invalid position $position")
            }
        }
    }
}