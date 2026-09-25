package com.example.c001apk.ui.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.observeAsState
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.util.PrefManager
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 个人主页（UserActivity / UserPagerFragment 的 Compose 版）：
 * 顶栏（返回 / 昵称 / 搜索 / 更多）+ [UserProfileHeader] 资料头 + 内容 tab 区插槽。
 *
 * - 复用现有 [UserViewModel]（LiveData 用 observeAsState 桥接，不重写 ViewModel）。
 * - tab 内容由 [tabContent] 插槽提供（通常是 [UserTabViewModel] 驱动的列表），
 *   本文件不关心列表内部怎么渲染。
 * - 加载 / 失败 / 空态由 activityState 驱动；点击跳转一律走回调参数，
 *   由接线方（Activity/Fragment）负责真正的 Intent。
 *
 * 简化说明（相对老的 CollapsingToolbarLayout）：头部固定不随列表折叠，
 * 折叠行为留给后续轮统一处理。
 *
 * @param viewModel 复用的用户资料 ViewModel
 * @param tabTitles tab 文案，默认 动态/点评/图文/问答/酷图
 * @param onBack 返回
 * @param onSearch 跳用户内搜索（老 UI：SearchActivity pageType=user）
 * @param onMore 更多菜单（查看资料/拉黑/分享/举报等，由接线方弹菜单）
 * @param onEditProfile 编辑资料（自己的主页）
 * @param onMessageClick 私信（别人的主页）
 * @param onShowFollowList 关注/粉丝列表，type 为 "follow" / "fans"
 * @param onOpenImage 头像/封面大图预览
 * @param onOpenEquip 装备页（老 UI：m.coolapk.com/myDevice/{uid}）
 * @param onToast ViewModel 的 toast 文案出口
 * @param tabContent 内容 tab 区插槽，参数为当前选中 tab 下标
 */
@Composable
fun UserProfileScreen(
    viewModel: UserViewModel,
    tabTitles: List<String> = UserProfileScreenDefaults.TAB_TITLES,
    onBack: () -> Unit,
    onSearch: () -> Unit = {},
    onMore: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onMessageClick: () -> Unit = {},
    onShowFollowList: (uid: String, type: String) -> Unit = { _, _ -> },
    onOpenImage: (url: String) -> Unit = {},
    onOpenEquip: (uid: String) -> Unit = {},
    onToast: (String) -> Unit = {},
    onRetry: () -> Unit = { viewModel.fetchUser() },
    tabContent: @Composable (selectedTabIndex: Int) -> Unit,
) {
    // LiveData 桥接：profileState / followState 每次 post 都是新 Event 实例，
    // 当作「userData 已更新」的刷新信号，userData 本身还是存在 ViewModel 里。
    val activityState by viewModel.activityState.observeAsState()
    val profileEvent by viewModel.profileState.observeAsState()
    val followEvent by viewModel.followState.observeAsState()
    val toastEvent by viewModel.toastText.observeAsState()

    val user = remember(profileEvent, followEvent) { viewModel.userData }

    val currentUid = user?.uid ?: viewModel.uid
    val isSelf = PrefManager.isLogin && PrefManager.uid.isNotEmpty() &&
            currentUid == PrefManager.uid

    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let { message ->
            message?.let(onToast)
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = user?.username.orEmpty(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(
                            imageVector = MiuixIcons.Search,
                            contentDescription = "搜索",
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = onMore) {
                        Icon(
                            imageVector = MiuixIcons.More,
                            contentDescription = "更多",
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        when (val state = activityState) {
            null, LoadingState.Loading -> {
                // 资料加载中
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is LoadingState.LoadingError -> UserProfileErrorState(
                message = state.errMsg,
                paddingValues = paddingValues,
                onRetry = onRetry,
            )

            is LoadingState.LoadingFailed -> UserProfileErrorState(
                message = state.msg,
                paddingValues = paddingValues,
                onRetry = onRetry,
            )

            else -> {
                // LoadingDone：头部 + tab + 内容插槽
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    UserProfileHeader(
                        userData = user,
                        isSelf = isSelf,
                        showActionButtons = isSelf || PrefManager.isLogin,
                        onFollowClick = {
                            viewModel.onPostFollowUnFollow(
                                if (user?.isFollow == 1) "/v6/user/unfollow"
                                else "/v6/user/follow"
                            )
                        },
                        onMessageClick = onMessageClick,
                        onEditProfile = onEditProfile,
                        onAvatarClick = { user?.userAvatar?.let(onOpenImage) },
                        onCoverClick = { user?.cover?.let(onOpenImage) },
                        onShowFollowList = { type -> onShowFollowList(viewModel.uid, type) },
                        onEquipClick = { onOpenEquip(viewModel.uid) },
                    )
                    if (tabTitles.isNotEmpty()) {
                        var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
                        if (selectedTabIndex > tabTitles.lastIndex) selectedTabIndex = 0
                        TabRow(
                            tabs = tabTitles,
                            selectedTabIndex = selectedTabIndex,
                            onTabSelected = { selectedTabIndex = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        ) {
                            tabContent(selectedTabIndex)
                        }
                    }
                }
            }
        }
    }
}

/** 资料加载失败 / 空态：文案 + 重试。 */
@Composable
private fun UserProfileErrorState(
    message: String,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(text = "重试", style = MiuixTheme.textStyles.button)
        }
    }
}

/** tab 文案 / 类型常量（与老 UserPagerFragment 的 tabType / tabTitle 对齐）。 */
object UserProfileScreenDefaults {
    val TAB_TITLES = listOf("动态", "点评", "图文", "问答", "酷图")
    val TAB_TYPES = listOf("feed", "rating", "article", "question", "coolpic")
}
