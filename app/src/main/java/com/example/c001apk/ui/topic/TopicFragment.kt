package com.example.c001apk.ui.topic

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.c001apk.R
import com.example.c001apk.ui.base.BasePagerFragment
import com.example.c001apk.ui.feed.reply.ReplyActivity
import com.example.c001apk.ui.home.IOnTabClickListener
import com.example.c001apk.ui.search.SearchActivity
import com.example.c001apk.util.IntentUtil
import com.example.c001apk.util.PrefManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.GRAVITY_CENTER
import com.google.android.material.tabs.TabLayout.MODE_SCROLLABLE
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TopicFragment : BasePagerFragment() {

    private val viewModel by viewModels<TopicViewModel>(ownerProducer = { requireActivity() })
    override var tabController: IOnTabClickListener? = null
    private lateinit var subscribe: MenuItem
    private var menuBlock: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSelected()
        if (PrefManager.isLogin)
            initFab()
        initObserve()
    }

    override fun initFab() {
        super.initFab()
        fab.setOnClickListener {
            // 机型页可以发动态，也可以发表点评（type=rating）
            if (viewModel.type == "product" && !viewModel.ratingItemInfo.isNullOrEmpty()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("发布")
                    .setItems(arrayOf("发布动态", "发表点评")) { _, which ->
                        startReply(if (which == 0) "createFeed" else "rating")
                    }
                    .show()
            } else
                startReply("createFeed")
        }
    }

    private fun startReply(type: String) {
        val intent = Intent(requireContext(), ReplyActivity::class.java)
        intent.putExtra("type", type)
        intent.putExtra(
            "targetType",
            if (viewModel.type == "topic") "tag" else "product_phone"
        )
        intent.putExtra("targetId", viewModel.id)
        if (viewModel.type == "topic")
            intent.putExtra("title", viewModel.title)
        if (type == "rating") {
            intent.putExtra("ratingTarget", viewModel.title)
            intent.putExtra("ratingItems", Gson().toJson(viewModel.ratingItemInfo))
        }
        val animationBundle = ActivityOptions.makeCustomAnimation(
            context,
            R.anim.anim_bottom_sheet_slide_up,
            R.anim.anim_bottom_sheet_slide_down
        ).toBundle()
        requireContext().startActivity(intent, animationBundle)
    }

    override fun onTabReselectedExtra() {
        if (fabBehavior.isScrolledDown)
            fabBehavior.slideUp(fab, true)
    }

    private fun initSelected() {
        viewModel.tabSelected?.let {
            binding.viewPager.setCurrentItem(it, false)
            viewModel.tabSelected = null
        }
    }

    private fun initObserve() {
        viewModel.blockState.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                menuBlock?.title = if (it) "移除黑名单"
                else "加入黑名单"
            }
        }

        viewModel.followState.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                subscribe.title = if (it) "取消关注"
                else "关注"
            }
        }

        viewModel.toastText.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getFragment(position: Int): Fragment {
        val bean = viewModel.topicList?.getOrNull(position)
        // 「参数」tab 走原生 dataList（productConfigList / listCard），
        // 版本配置行来自 product/detail 的 configRows，随 Fragment 传入
        val isParamsTab = bean?.pageName == "main" || bean?.title == "参数"
        return TopicContentFragment.newInstance(
            bean?.url.orEmpty(),
            bean?.title.orEmpty(),
            if (isParamsTab && viewModel.type == "product")
                ArrayList(viewModel.configRows.orEmpty())
            else null,
        )
    }

    override fun initTabList() {
        binding.tabLayout.apply {
            tabGravity = GRAVITY_CENTER
            tabMode = MODE_SCROLLABLE
        }
        tabList = viewModel.topicList?.map { it.title } ?: emptyList()
    }

    override fun onBackClick() {
        activity?.finish()
    }

    override fun initBar() {
        super.initBar()
        binding.collapsingToolbar.isTitleEnabled = false
        binding.toolBar.apply {
            title = if (viewModel.type == "topic") viewModel.url.replace("/t/", "")
            else viewModel.title
            viewModel.subtitle?.let { subtitle = it }

            inflateMenu(R.menu.topic_product_menu)

            menuBlock = menu.findItem(R.id.block)
            subscribe = menu.findItem(R.id.subscribe)
            subscribe.isVisible = PrefManager.isLogin

            viewModel.checkMenuState()

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.search -> {
                        if (viewModel.type == "topic") {
                            IntentUtil.startActivity<SearchActivity>(requireContext()) {
                                putExtra("type", "topic")
                                putExtra("pageType", "tag")
                                putExtra("pageParam", viewModel.url.replace("/t/", ""))
                                putExtra("title", viewModel.url.replace("/t/", ""))
                            }
                        } else {
                            IntentUtil.startActivity<SearchActivity>(requireContext()) {
                                putExtra("type", "topic")
                                putExtra("pageType", "product_phone")
                                putExtra("pageParam", viewModel.id)
                                putExtra("title", viewModel.title)
                            }
                        }
                    }

                    R.id.block -> {
                        val isBlocked = menuBlock?.title.toString() == "移除黑名单"
                        MaterialAlertDialogBuilder(requireContext()).apply {
                            val title =
                                if (viewModel.type == "topic") viewModel.url
                                    .replace("/t/", "")
                                else viewModel.title
                            setTitle("确定将 $title ${menuBlock?.title}？")
                            setNegativeButton(android.R.string.cancel, null)
                            setPositiveButton(android.R.string.ok) { _, _ ->
                                viewModel.title.let { title ->
                                    menuBlock?.title = if (isBlocked) {
                                        viewModel.deleteTopic(title)
                                        "加入黑名单"
                                    } else {
                                        viewModel.saveTopic(title)
                                        "移除黑名单"
                                    }
                                }
                            }
                            show()
                        }
                    }

                    R.id.subscribe -> {
                        when (viewModel.type) {
                            "topic" -> {
                                val followUrl =
                                    if (viewModel.isFollow) "/v6/feed/unFollowTag"
                                    else "/v6/feed/followTag"
                                val tag = viewModel.url.replace("/t/", "")
                                viewModel.onGetFollow(followUrl, tag, null)
                            }

                            "product" -> {
                                if (viewModel.postFollowData.isNullOrEmpty())
                                    viewModel.postFollowData = HashMap()
                                viewModel.postFollowData?.let { map ->
                                    map["id"] = viewModel.id
                                    map["status"] =
                                        if (viewModel.isFollow) "0"
                                        else "1"
                                }
                                viewModel.onPostFollow()
                            }

                            else -> Toast.makeText(
                                requireContext(),
                                "type error: ${viewModel.type}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                }
                return@setOnMenuItemClickListener true
            }
        }
    }
}
