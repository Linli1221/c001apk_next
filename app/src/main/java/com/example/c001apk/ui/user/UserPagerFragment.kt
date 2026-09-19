package com.example.c001apk.ui.user

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.c001apk.R
import com.example.c001apk.databinding.BaseViewUserBinding
import com.example.c001apk.ui.base.BasePagerFragment
import com.example.c001apk.ui.others.WebViewActivity
import com.example.c001apk.ui.search.SearchActivity
import com.example.c001apk.util.DateUtils
import com.example.c001apk.util.IntentUtil
import com.example.c001apk.util.PrefManager
import com.example.c001apk.util.ReplaceViewHelper
import com.example.c001apk.view.AppBarLayoutStateChangeListener
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserPagerFragment : BasePagerFragment() {

    private val viewModel by viewModels<UserViewModel>(ownerProducer = { requireActivity() })
    private lateinit var userBinding: BaseViewUserBinding
    private var menuBlock: MenuItem? = null

    // 官方的「主页」tab 是 homeTabCardRows 卡片体系，这里先只做列表类的 tab
    private val tabType = listOf("feed", "rating", "article", "question", "coolpic")
    private val tabTitle = listOf("动态", "点评", "图文", "问答", "酷图")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUser()
        initObserve()
    }

    override fun initTabList() {
        tabList = tabTitle
    }

    override fun getFragment(position: Int): Fragment =
        UserTabFragment.newInstance(viewModel.uid, tabType[position])

    override fun onBackClick() {
        activity?.finish()
    }

    private fun initUser() {
        userBinding = BaseViewUserBinding.inflate(layoutInflater, null, false)
        ReplaceViewHelper(requireContext()).toReplaceView(binding.view, userBinding.root)
        (userBinding.root.layoutParams as? CollapsingToolbarLayout.LayoutParams)
            ?.collapseMode = CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_PARALLAX

        userBinding.userData = viewModel.userData
        userBinding.listener = viewModel.ItemClickListener()

        // 关注按钮：自己 / 未登录不显示
        userBinding.followBtn.isVisible =
            PrefManager.isLogin && viewModel.uid != PrefManager.uid
        userBinding.followBtn.setOnClickListener {
            viewModel.onPostFollowUnFollow(
                if (viewModel.userData?.isFollow == 1) "/v6/user/unfollow" else "/v6/user/follow"
            )
        }

        userBinding.equipLayout.setOnClickListener {
            IntentUtil.startActivity<WebViewActivity>(requireContext()) {
                putExtra("url", "https://m.coolapk.com/myDevice/${viewModel.uid}")
            }
        }
    }

    private fun initObserve() {
        viewModel.blockState.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                menuBlock?.title = getMenuTitle(
                    if (it) "移除黑名单"
                    else "加入黑名单"
                )
            }
        }

        viewModel.followState.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                userBinding.userData = viewModel.userData
                userBinding.executePendingBindings()
            }
        }

        viewModel.toastText.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 顶栏图标当前是否为白色（null = 还没刷过，保证首次一定应用） */
    private var barIconWhite: Boolean? = null

    /**
     * 顶栏三个图标（返回 / 搜索 / 更多）统一上色。
     * `ic_search`、`ic_more` 自带 `android:tint="?attr/colorControlNormal"`，
     * 压在封面上会明显比返回键浅，所以这里显式覆盖。
     */
    private fun applyBarIconTint(white: Boolean) {
        if (barIconWhite == white) return
        barIconWhite = white
        val color = if (white) {
            Color.WHITE
        } else {
            MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorOnSurface,
                0
            )
        }
        binding.toolBar.navigationIcon?.setTintList(ColorStateList.valueOf(color))
        binding.toolBar.menu?.let { menu ->
            for (i in 0 until menu.size()) {
                val item = menu.getItem(i)
                val icon = item.icon ?: continue
                icon.mutate().setTint(color)
                item.icon = icon
            }
        }
        binding.toolBar.overflowIcon?.mutate()?.setTint(color)
    }

    private fun getMenuTitle(title: CharSequence?): SpannableString {
        val text = title ?: return SpannableString("")
        return SpannableString(text).also {
            it.setSpan(
                ForegroundColorSpan(
                    MaterialColors.getColor(
                        requireContext(),
                        com.google.android.material.R.attr.colorControlNormal,
                        0
                    )
                ),
                0, text.length, 0
            )
        }
    }

    override fun initBar() {
        super.initBar()

        // 官方主页 tab 是左对齐、可横向滑动的
        binding.tabLayout.tabMode = TabLayout.MODE_SCROLLABLE

        // 收起后才显示昵称；展开时昵称在头部里
        binding.collapsingToolbar.title = viewModel.userData?.username
        binding.collapsingToolbar.setCollapsedTitleTextColor(
            MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorOnSurface,
                0
            )
        )

        // percent: 1 = 完全展开，0 = 完全收起
        // 展开时返回键 / 搜索 / 更多都压在封面图上，统一纯白；收起后一起换回主题色
        binding.appBar.addOnOffsetChangedListener(object : AppBarLayoutStateChangeListener() {
            override fun onScroll(percent: Float) {
                applyBarIconTint(percent > 0f)
            }
        })

        binding.toolBar.apply {
            inflateMenu(R.menu.user_menu)
            menuBlock = menu?.findItem(R.id.block)
            menuBlock?.title = getMenuTitle(menuBlock?.title)

            val menuShare = menu?.findItem(R.id.share)
            menuShare?.title = getMenuTitle(menuShare?.title)

            val menuReport = menu?.findItem(R.id.report)
            menuReport?.title = getMenuTitle(menuReport?.title)
            menuReport?.isVisible = PrefManager.isLogin

            menu?.findItem(R.id.check)?.title = getMenuTitle(menu?.findItem(R.id.check)?.title)

            // 顶栏右侧图标和左侧返回键同一色调（展开纯白 / 收起主题色）
            overflowIcon = ContextCompat.getDrawable(context, R.drawable.ic_more)

            viewModel.checkMenuState()

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.check -> {
                        val data = viewModel.userData
                        MaterialAlertDialogBuilder(requireContext()).apply {
                            setTitle(data?.username)
                            setMessage(
                                """
                                uid: ${data?.uid}
                                
                                等级: Lv.${data?.level}
                                
                                性别: ${if (data?.gender == 0) "女" else if (data?.gender == 1) "男" else "未知"}
                                
                                注册时长: ${((System.currentTimeMillis() / 1000 - (data?.regdate ?: 0)) / 24 / 3600)} 天
                                
                                注册时间: ${DateUtils.timeStamp2Date(data?.regdate ?: 0)}
                            """.trimIndent()
                            )
                            show()
                        }
                    }

                    R.id.search -> {
                        IntentUtil.startActivity<SearchActivity>(requireContext()) {
                            putExtra("pageType", "user")
                            putExtra("pageParam", viewModel.uid)
                            putExtra("title", viewModel.userData?.username)
                        }
                    }

                    R.id.block -> {
                        val isBlocked = menuBlock?.title.toString() == "移除黑名单"
                        MaterialAlertDialogBuilder(requireContext()).apply {
                            setTitle("确定将 ${viewModel.userData?.username} ${menuBlock?.title}？")
                            setNegativeButton(android.R.string.cancel, null)
                            setPositiveButton(android.R.string.ok) { _, _ ->
                                viewModel.uid.let { uid ->
                                    menuBlock?.title = if (isBlocked) {
                                        viewModel.deleteUid(uid)
                                        getMenuTitle("加入黑名单")
                                    } else {
                                        viewModel.saveUid(uid)
                                        getMenuTitle("移除黑名单")
                                    }
                                }
                            }
                            show()
                        }
                    }

                    R.id.share -> {
                        IntentUtil.shareText(
                            requireContext(),
                            "https://www.coolapk1s.com/u/${viewModel.uid}"
                        )
                    }

                    R.id.report -> {
                        IntentUtil.startActivity<WebViewActivity>(requireContext()) {
                            putExtra(
                                "url",
                                "https://m.coolapk.com/mp/do?c=user&m=report&id=${viewModel.uid}"
                            )
                        }
                    }
                }
                return@setOnMenuItemClickListener true
            }
        }

        // 初始为完全展开
        applyBarIconTint(true)
    }
}
