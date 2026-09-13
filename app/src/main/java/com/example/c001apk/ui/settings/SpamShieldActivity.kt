package com.example.c001apk.ui.settings

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.c001apk.R
import com.example.c001apk.databinding.ActivitySpamShieldBinding
import com.example.c001apk.ui.base.BaseActivity
import com.example.c001apk.util.Event
import com.example.c001apk.util.makeToast
import dagger.hilt.android.AndroidEntryPoint

/** 其他屏蔽项管理：关键字 / 用户 / 节点（服务端 spam_word_config） */
@AndroidEntryPoint
class SpamShieldActivity : BaseActivity<ActivitySpamShieldBinding>() {

    private val viewModel by viewModels<SpamShieldViewModel>()

    private lateinit var adapter: SpamShieldAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.init(intent.getStringExtra("type") ?: "word")
        initBar()
        initView()
        initObserve()
        viewModel.load()
    }

    private fun initBar() {
        binding.toolBar.apply {
            title = when (viewModel.type) {
                "user" -> getString(R.string.spam_shield_user)
                "node" -> getString(R.string.spam_shield_node)
                else -> getString(R.string.spam_shield_word)
            }
            setNavigationIcon(R.drawable.ic_back)
            setNavigationOnClickListener { finish() }
        }
    }

    private fun initView() {
        adapter = SpamShieldAdapter { viewModel.remove(it) }
        binding.recyclerView.apply {
            adapter = this@SpamShieldActivity.adapter
            layoutManager = LinearLayoutManager(this@SpamShieldActivity)
        }
        binding.indicator.isIndeterminate = true
        binding.indicator.isVisible = true

        binding.editText.hint = when (viewModel.type) {
            "user" -> getString(R.string.spam_shield_user_hint)
            "node" -> getString(R.string.spam_shield_node_hint)
            else -> getString(R.string.spam_shield_word_hint)
        }
        binding.editText2.isVisible = viewModel.type == "node"
        binding.editText2.hint = getString(R.string.spam_shield_node_name_hint)

        binding.add.setOnClickListener { add() }
    }

    private fun add() {
        val value = binding.editText.text?.toString()?.trim().orEmpty()
        if (value.isEmpty()) {
            makeToast("请输入内容")
            return
        }
        val name = binding.editText2.text?.toString()?.trim()
        if (viewModel.type == "node" && name.isNullOrEmpty()) {
            makeToast("请填写节点名称")
            return
        }
        viewModel.add(value, name)
        binding.editText.text = null
        binding.editText2.text = null
    }

    private fun initObserve() {
        viewModel.list.observe(this) {
            adapter.submitList(it)
        }
        viewModel.loading.observe(this) {
            binding.indicator.isIndeterminate = it
            binding.indicator.isVisible = it
        }
        viewModel.toastText.observe(this) { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                makeToast(it)
            }
        }
    }
}
