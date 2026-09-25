package com.example.c001apk.ui.topic

import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.observeAsState
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.constant.Constants
import com.example.c001apk.logic.model.TopicBean
import com.example.c001apk.util.ImageUtil
import com.example.c001apk.util.PrefManager
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 话题 / 产品页（TopicActivity + TopicFragment + TopicHeader 的 Compose 版）。
 *
 * 状态全部走现有 [TopicViewModel]（LiveData 用 observeAsState 桥接）：
 * - [TopicViewModel.activityState]：页面级 加载中 / 错误消息 / 失败重试（对齐 BaseViewActivity 状态机）
 * - [TopicViewModel.headerState]：头部话题卡（logo / 标题 / 热度 / 关注者头像）
 * - [TopicViewModel.followState]：关注按钮两态（Event 一次性消费）
 * - [TopicViewModel.toastText]：toast（Event 一次性消费）
 *
 * 关注按钮点击逻辑与 TopicFragment.onSubscribeClick 一致（话题 tag / 机型 product 两种协议），
 * 直接调用现有 ViewModel 方法，不新增业务逻辑。
 *
 * 各 tab 的内容（TopicContentFragment 的列表 / WebViewFragment 的 H5）通过 [tabContent]
 * 插槽由接线层提供，本文件不关心其内部实现。
 *
 * 悬浮发布按钮：产品页且有评分子项时先弹「发布动态 / 发表点评」二选一（OverlayDialog），
 * 否则直接回调 onPublishClick("createFeed")；具体跳转 ReplyActivity 由接线层处理。
 */
@Composable
fun TopicScreen(
    viewModel: TopicViewModel,
    tabContent: @Composable (index: Int, tab: TopicBean) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onPublishClick: (type: String) -> Unit = {},
    onFollowersClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val activityState by viewModel.activityState.observeAsState()
    val headerState by viewModel.headerState.observeAsState()
    val followState by viewModel.followState.observeAsState()
    val toastText by viewModel.toastText.observeAsState()

    // 关注按钮状态：初值取 ViewModel，后续由 followState 事件刷新
    var followed by remember { mutableStateOf(viewModel.isFollow) }
    var showPublishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(followState) {
        followState?.getContentIfNotHandledOrReturnNull()?.let { followed = it }
    }

    LaunchedEffect(toastText) {
        toastText?.getContentIfNotHandledOrReturnNull()?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    // 页面级状态机（对齐 BaseViewActivity）：Loading → 拉取布局；重试按钮 = 重新置 Loading
    LaunchedEffect(activityState) {
        if (activityState is LoadingState.Loading) {
            when (viewModel.type) {
                "topic" -> {
                    viewModel.url = viewModel.url.replace("/t/", "")
                    viewModel.fetchTopicLayout()
                }

                "product" -> viewModel.fetchProductLayout()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.isAInit) {
            viewModel.isAInit = false
            if (viewModel.topicList == null) {
                viewModel.activityState.value = LoadingState.Loading
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            SmallTopAppBar(
                title = if (viewModel.type == "topic") viewModel.url.replace("/t/", "")
                else viewModel.title,
                subtitle = viewModel.subtitle.orEmpty(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(MiuixIcons.Basic.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = onMoreClick) {
                        Icon(MiuixIcons.More, contentDescription = "更多")
                    }
                },
            )
        },
        floatingActionButton = {
            if (PrefManager.isLogin && !viewModel.topicList.isNullOrEmpty()) {
                FloatingActionButton(
                    onClick = {
                        // 机型页可以发动态，也可以发表点评（type=rating）
                        if (viewModel.type == "product" && !viewModel.ratingItemInfo.isNullOrEmpty())
                            showPublishDialog = true
                        else
                            onPublishClick("createFeed")
                    }
                ) {
                    Icon(MiuixIcons.Add, contentDescription = "发布")
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when (val state = activityState) {
                is LoadingState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is LoadingState.LoadingError -> {
                    Text(
                        text = state.errMsg,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                    )
                }

                is LoadingState.LoadingFailed -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = state.msg,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.body2,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            text = if (state.msg == Constants.LOADING_EMPTY) "刷新" else "重试",
                            onClick = { viewModel.activityState.value = LoadingState.Loading },
                        )
                    }
                }

                else -> {
                    val tabs = viewModel.topicList.orEmpty()
                    if (tabs.isEmpty()) {
                        Text(
                            text = Constants.LOADING_EMPTY,
                            modifier = Modifier.align(Alignment.Center),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.body2,
                        )
                    } else {
                        // 服务端指定的默认 tab，用完即清（对齐 TopicFragment.initSelected）
                        val initialPage = remember {
                            val page = (viewModel.tabSelected ?: 0).coerceIn(0, tabs.lastIndex)
                            viewModel.tabSelected = null
                            page
                        }
                        val pagerState = rememberPagerState(initialPage = initialPage) { tabs.size }
                        val scope = rememberCoroutineScope()

                        Column(modifier = Modifier.fillMaxSize()) {
                            headerState?.let { header ->
                                TopicHeaderCard(
                                    header = header,
                                    isFollow = followed,
                                    showFollowBtn = PrefManager.isLogin,
                                    onFollowClick = { onSubscribeClick(viewModel) },
                                    onFollowersClick = onFollowersClick,
                                )
                            }
                            TabRow(
                                tabs = tabs.map { it.title },
                                selectedTabIndex = pagerState.currentPage,
                                onTabSelected = { index ->
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            ) { page ->
                                tabContent(page, tabs[page])
                            }
                        }
                    }
                }
            }

            // 产品页「发布动态 / 发表点评」二选一（对齐 TopicFragment.initFab 的弹窗）
            OverlayDialog(
                show = showPublishDialog,
                title = "发布",
                onDismissRequest = { showPublishDialog = false },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = "发布动态",
                        onClick = {
                            showPublishDialog = false
                            onPublishClick("createFeed")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        text = "发表点评",
                        onClick = {
                            showPublishDialog = false
                            onPublishClick("rating")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * 关注 / 取消关注（与 TopicFragment.onSubscribeClick 一致）：
 * 话题（topic）走 getFollow 协议，机型（product）走 postFollow 协议。
 */
private fun onSubscribeClick(viewModel: TopicViewModel) {
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
                map["status"] = if (viewModel.isFollow) "0" else "1"
            }
            viewModel.onPostFollow()
        }
    }
}

/**
 * 话题页头部卡片（item_topic_header.xml 的 Compose 版）：
 * logo + 标题 + 热度/讨论数 + 最近关注者头像行 + 关注按钮。
 */
@Composable
private fun TopicHeaderCard(
    header: TopicHeader,
    isFollow: Boolean,
    showFollowBtn: Boolean,
    onFollowClick: () -> Unit,
    onFollowersClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        GlideImage(
            url = header.logo,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp)),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = header.title.orEmpty(),
                style = MiuixTheme.textStyles.title2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // 服务端只下发数字（如「1.2万」），单位在本地点上
            val stats = listOfNotNull(
                header.hotNum?.takeIf { it.isNotEmpty() },
                header.commentNum?.takeIf { it.isNotEmpty() }?.let { "${it}讨论" },
            ).joinToString(" · ")
            if (stats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stats,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val avatars = header.avatars.take(3)
            val followText =
                header.followNum?.takeIf { it.isNotEmpty() }?.let { "${it}人关注" }.orEmpty()
            if (avatars.isNotEmpty() || followText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.clickable(onClick = onFollowersClick),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    avatars.forEachIndexed { index, url ->
                        GlideImage(
                            url = url,
                            modifier = Modifier
                                .size(28.dp)
                                .offset(x = if (index == 0) 0.dp else (-8).dp)
                                .clip(CircleShape),
                        )
                    }
                    if (followText.isNotEmpty()) {
                        Text(
                            text = followText,
                            modifier = Modifier.padding(start = 6.dp),
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "›",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }

        // 关注 / 已关注：未关注 = 主题色填充，已关注 = 弱化底色（对齐 bindFollowBtn 两态配色）
        if (showFollowBtn) {
            Button(
                onClick = onFollowClick,
                colors = if (isFollow) ButtonDefaults.buttonColors()
                else ButtonDefaults.buttonColorsPrimary(),
                cornerRadius = 16.dp,
                minWidth = 76.dp,
                minHeight = 32.dp,
                insideMargin = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            ) {
                Text(
                    text = if (isFollow) "已关注" else "关注",
                    style = MiuixTheme.textStyles.button,
                )
            }
        }
    }
}

/**
 * 图片走现有 Glide 链路（Glide 没有官方 Compose 集成，用 AndroidView 包 ImageView）。
 * TODO: ui/common 提供公共图片组件后替换为统一实现。
 */
@Composable
private fun GlideImage(
    url: String?,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { view -> ImageUtil.showIMG(view, url) },
    )
}
