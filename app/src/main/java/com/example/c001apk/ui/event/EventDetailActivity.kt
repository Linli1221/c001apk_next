package com.example.c001apk.ui.event

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.example.c001apk.databinding.ActivityEventDetailBinding
import com.example.c001apk.logic.model.EventDetailData
import com.example.c001apk.ui.base.BaseActivity
import com.example.c001apk.util.ImageUtil
import com.example.c001apk.util.makeToast
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 众测/活动详情（原生）。
 *
 * 2026-09-19：替代原来的 WebView 落地页（www.coolapk.com/event/<id> 只是下载引导页）。
 */
@AndroidEntryPoint
class EventDetailActivity : BaseActivity<ActivityEventDetailBinding>() {

    private val viewModel by viewModels<EventDetailViewModel>()

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.toolBar.title = "众测详情"
        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.loading.observe(this) { show ->
            binding.indicator.parent.isIndeterminate = show
            binding.indicator.parent.isVisible = show
        }
        viewModel.toastText.observe(this) { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let { makeToast(it) }
        }
        viewModel.detail.observe(this) { data -> bind(data) }

        val id = intent.getStringExtra("id").orEmpty()
        if (id.isNotEmpty()) {
            viewModel.load(id)
        } else {
            makeToast("缺少活动 id")
        }
    }

    private fun bind(data: EventDetailData) {
        binding.titleText.text = data.title

        binding.banner.isVisible = !data.logo.isNullOrEmpty()
        if (!data.logo.isNullOrEmpty()) {
            ImageUtil.showIMG(binding.banner, data.logo)
        }

        val sponsor = data.sponsorUser
            ?: data.sponsorUserList?.firstOrNull()?.username
        binding.sponsorText.isVisible = !sponsor.isNullOrEmpty()
        binding.sponsorText.text = sponsor?.let { "赞助方：$it" }

        val regNum = data.showRegNum ?: data.regNum
        binding.regNumText.isVisible = !regNum.isNullOrEmpty()
        binding.regNumText.text = regNum?.let { "报名人数：$it" }

        val start = data.timeRegStart
        val end = data.timeRegEnd
        binding.regTimeText.isVisible = start != null && end != null
        if (start != null && end != null) {
            binding.regTimeText.text = "报名时间：${fmt(start)} ~ ${fmt(end)}"
        }

        binding.ruleText.text = data.noticeRule

        val prizes = data.sponsorPrizeList.orEmpty()
            .mapNotNull { it.title }
            .joinToString("、")
        binding.prizeText.isVisible = prizes.isNotEmpty()
        binding.prizeText.text = "奖品：$prizes"

        val applyUrl = data.actionUrl
        binding.applyButton.isVisible = !applyUrl.isNullOrEmpty()
        binding.applyButton.setOnClickListener {
            if (!applyUrl.isNullOrEmpty()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(applyUrl)))
            }
        }
    }

    private fun fmt(ts: Long): String = dateFmt.format(Date(ts * 1000L))
}
