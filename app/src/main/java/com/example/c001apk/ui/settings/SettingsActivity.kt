package com.example.c001apk.ui.settings

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.example.c001apk.databinding.ActivitySettingsBinding
import com.example.c001apk.ui.base.BaseActivity

/**
 * 设置页面。从 MineFragment 右上角的「设置」图标进入。
 *
 * 内部只承载 [SettingsPreferenceFragment]（不再保留外层 SettingsFragment 包装）。
 * 顶部 MaterialToolbar 由本 Activity 自带，提供返回与标题。
 */
class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!navUp()) finish()
            }
        })
    }

    private fun navUp(): Boolean {
        val nav = supportFragmentManager
        if (nav.backStackEntryCount > 0) {
            nav.popBackStack()
            return true
        }
        return false
    }

    override fun onSupportNavigateUp(): Boolean {
        return navUp() || super.onSupportNavigateUp()
    }
}