package com.example.c001apk.ui.follow

import android.app.Activity
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.c001apk.R
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.adapter.ItemListener
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.constant.Constants.LOADING_EMPTY
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.util.DateUtils
import com.example.c001apk.util.ImageUtil
import com.example.c001apk.util.IntentUtil
import com.example.c001apk.util.PrefManager
import com.example.c001apk.ui.user.UserActivity
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 关注 / 粉丝列表页（Miuix 版）。
 *
 * 对照老代码：
 * - [FollowPagerFragment]：标题（initBar）+ 分栏（initTabList / getFragment）
 * - [FollowFragment] + [FollowViewModel]：每个分栏一个 ViewModel，数据仍走原 LiveData
 * - `BaseViewFragment` 的刷新 / 加载更多 / 空态 / 错误态流程在 [FollowUserList] 内复刻
 * - `AppAdapter.UserViewHolder` + `item_search_user.xml` 的用户行在 [FollowUserRow] 内复刻
 *
 * 只做 UI 层迁移：ViewModel / Repository / 网络逻辑全部复用，不改动任何既有文件。
 */

/** 一个分栏（老 FollowPagerFragment.getFragment 的一个 tab）。 */
data class FollowTab(
    val label: String,
    val type: String,
    /** 覆盖页面 uid（默认沿用页面 uid；老代码 apk 等类型内部会把 uid 置空，行为不变）。 */
    val uid: String? = null,
)

/** 老 FollowPagerFragment.initBar() 的标题映射。 */
fun followTitleOf(type: String, isMe: Boolean): String = when (type) {
    "feed" -> "我的动态"
    "follow" -> if (isMe) "我的关注" else "TA关注的人"
    "fans" -> if (isMe) "关注我的人" else "TA的粉丝"
    "like" -> "我的赞"
    "reply" -> "我的回复"
    "recentHistory" -> "我的常去"
    else -> type
}

/** 老 FollowPagerFragment.initTabList() + getFragment() 的分栏映射。 */
fun followTabsOf(type: String, isMe: Boolean): List<FollowTab> = when (type) {
    "follow" ->
        if (isMe)
            listOf(
                FollowTab("用户", "follow"),
                FollowTab("话题", "topic"),
                FollowTab("数码", "product"),
                FollowTab("应用", "apk"),
            )
        else
            listOf(FollowTab("", "follow"))

    "reply" -> listOf(
        FollowTab("我的回复", "reply"),
        FollowTab("我收到的回复", "replyToMe"),
    )

    else -> listOf(FollowTab("", type))
}

/**
 * 关注 / 粉丝列表整页（顶栏 + 可选 TabRow 分栏 + 列表）。
 *
 * @param viewModelFactory 由调用方注入 FollowViewModel 的 AssistedFactory，
 * 例如 `{ uid, type -> FollowViewModel.provideFactory(assistedFactory, uid, type) }`。
 * @param onBackClick 返回按钮；null 时结束宿主 Activity（等价老 onBackClick）。
 * @param onUserClick 点击用户行；null 时跳转 UserActivity（等价老 ItemListener.onViewUser）。
 * @param onEntityClick 点击非用户行（话题/数码/应用等兜底行）；null 时不跳转。
 */
@Composable
fun FollowScreen(
    uid: String,
    type: String,
    viewModelFactory: (uid: String, type: String) -> ViewModelProvider.Factory,
    modifier: Modifier = Modifier,
    title: String = followTitleOf(type, uid == PrefManager.uid),
    tabs: List<FollowTab> = followTabsOf(type, uid == PrefManager.uid),
    onBackClick: (() -> Unit)? = null,
    onUserClick: ((String?) -> Unit)? = null,
    onEntityClick: ((HomeFeedResponse.Data) -> Unit)? = null,
    onToast: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    Scaffold(
        modifier = modifier,
        topBar = {
            SmallTopAppBar(
                title = title,
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (onBackClick != null) onBackClick()
                            else (context as? Activity)?.finish()
                        },
                    ) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },
            )
        },
    ) { paddingValues ->
        if (tabs.size <= 1) {
            // 单列表（TA关注的人 / 粉丝 / 动态 等）：老代码隐藏 tabLayout
            val tab = tabs.firstOrNull() ?: return@Scaffold
            val viewModel = rememberFollowViewModel(tab, uid, viewModelFactory)
            FollowUserList(
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onUserClick = onUserClick,
                onEntityClick = onEntityClick,
                onToast = onToast,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                val pagerState = rememberPagerState { tabs.size }
                val scope = rememberCoroutineScope()
                TabRow(
                    tabs = tabs.map { it.label },
                    selectedTabIndex = pagerState.currentPage,
                    onTabSelected = { index ->
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val tab = tabs[page]
                    val viewModel = rememberFollowViewModel(tab, uid, viewModelFactory)
                    FollowUserList(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                        onUserClick = onUserClick,
                        onEntityClick = onEntityClick,
                        onToast = onToast,
                    )
                }
            }
        }
    }
}

/**
 * 单个分栏的列表内容（复用现有 [FollowViewModel]，LiveData 用 observeAsState 桥接）。
 * 状态闭环：加载中 / 列表 / 空态 / 错误态 / 下拉刷新 / 触底加载 / 底部重试。
 */
@Composable
fun FollowUserList(
    viewModel: FollowViewModel,
    modifier: Modifier = Modifier,
    onUserClick: ((String?) -> Unit)? = null,
    onEntityClick: ((HomeFeedResponse.Data) -> Unit)? = null,
    onToast: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val dataList by viewModel.dataList.observeAsState(emptyList())
    val loadingState by viewModel.loadingState.observeAsState()
    val footerState by viewModel.footerState.observeAsState()
    val toastEvent by viewModel.toastText.observeAsState()

    var pullRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    // 复用 BaseAppViewModel.ItemClickListener：关注/取关走原 onPostFollowUnFollow 链路
    val itemListener = remember(viewModel) { viewModel.ItemClickListener() }

    // 老 BaseAppFragment 在 dataList 变化时同步 listSize（fetchFeedList 据此决定 loading/footer 分支）
    SideEffect { viewModel.listSize = dataList.size }

    // 首次进入：等价 BaseViewFragment.onResume 的 isInit 分支 + refreshData()
    LaunchedEffect(viewModel) {
        applyFollowTypeConfig(viewModel)
        if (viewModel.isInit) {
            viewModel.isInit = false
            refreshFollowList(viewModel, showLoadingIndicator = true)
        }
    }

    // 下拉刷新收尾：loadingState 任意变化、footerState 变为非 Loading 时关闭刷新指示器
    // （等价老 BaseViewFragment.initObserve / BaseAppFragment.initObserve 对 swipeRefresh 的处理）
    DisposableEffect(viewModel, lifecycleOwner) {
        val stopRefreshing = { pullRefreshing = false }
        val loadingObserver = Observer<LoadingState> { stopRefreshing() }
        val footerObserver = Observer<FooterState> {
            if (it !is FooterState.Loading) stopRefreshing()
        }
        viewModel.loadingState.observe(lifecycleOwner, loadingObserver)
        viewModel.footerState.observe(lifecycleOwner, footerObserver)
        onDispose {
            viewModel.loadingState.removeObserver(loadingObserver)
            viewModel.footerState.removeObserver(footerObserver)
        }
    }

    // 触底加载更多（等价老 RecyclerView.OnScrollListener 的 loadMore 触发）
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .let { flow ->
                var lastEmitted = Int.MIN_VALUE
                flow.collect { last ->
                    if (last == lastEmitted) return@collect
                    lastEmitted = last
                    val items = viewModel.dataList.value
                    if (!items.isNullOrEmpty() && last >= items.size - 1 &&
                        !viewModel.isEnd && !viewModel.isRefreshing && !viewModel.isLoadMore
                    ) {
                        viewModel.isLoadMore = true
                        viewModel.fetchData()
                    }
                }
            }
    }

    // toastText 事件消费（等价老 FollowFragment.initObserve 的 Toast）
    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let { message ->
            if (message != null) {
                if (onToast != null) onToast(message)
                else Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val errorMessage = loadingState as? LoadingState.LoadingError
    val failedMessage = loadingState as? LoadingState.LoadingFailed
    val isLoading = loadingState == null || loadingState is LoadingState.Loading

    Box(modifier = modifier) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    InfiniteProgressIndicator(color = MiuixTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "加载中…",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            errorMessage != null -> {
                StateCenter(
                    message = errorMessage.errMsg,
                    buttonText = stringResource(R.string.retry),
                    onClick = { refreshFollowList(viewModel, showLoadingIndicator = true) },
                )
            }

            failedMessage != null -> {
                StateCenter(
                    message = failedMessage.msg,
                    buttonText = if (failedMessage.msg == LOADING_EMPTY)
                        stringResource(R.string.refresh)
                    else
                        stringResource(R.string.retry),
                    onClick = { refreshFollowList(viewModel, showLoadingIndicator = true) },
                )
            }

            else -> {
                PullToRefresh(
                    isRefreshing = pullRefreshing,
                    onRefresh = {
                        pullRefreshing = true
                        refreshFollowList(viewModel, showLoadingIndicator = false)
                    },
                    pullToRefreshState = pullToRefreshState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(dataList) { data ->
                            FollowItemRow(
                                data = data,
                                itemListener = itemListener,
                                onUserClick = onUserClick,
                                onEntityClick = onEntityClick,
                            )
                            HorizontalDivider()
                        }
                        item(key = "follow_footer") {
                            FollowFooter(
                                footerState = footerState,
                                onRetry = {
                                    viewModel.isEnd = false
                                    viewModel.isLoadMore = true
                                    viewModel.fetchData()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 空态 / 错误态：提示文案 + 重试按钮。 */
@Composable
private fun StateCenter(
    message: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(Modifier.height(12.dp))
        TextButton(text = buttonText, onClick = onClick)
    }
}

/** 列表底部：加载中 / 没有更多了 / 加载失败重试（等价老 FooterAdapter）。 */
@Composable
private fun FollowFooter(
    footerState: FooterState?,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (footerState) {
            is FooterState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InfiniteProgressIndicator(
                        color = MiuixTheme.colorScheme.primary,
                        size = 16.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "加载中…",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            is FooterState.LoadingEnd -> Text(
                text = footerState.msg,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )

            is FooterState.LoadingError -> Text(
                text = "${footerState.errMsg}，点击重试",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.error,
                modifier = Modifier.clickable(onClick = onRetry),
            )

            else -> {}
        }
    }
}

/**
 * 列表条目：用户行（entityType contacts/user，复刻 AppAdapter.UserViewHolder），
 * 其余实体（话题/数码/应用/回复等）用基础行兜底，点击走 onEntityClick。
 */
@Composable
private fun FollowItemRow(
    data: HomeFeedResponse.Data,
    itemListener: ItemListener,
    onUserClick: ((String?) -> Unit)?,
    onEntityClick: ((HomeFeedResponse.Data) -> Unit)?,
) {
    val context = LocalContext.current
    when (data.entityType) {
        "contacts", "user" -> FollowUserRow(
            data = data,
            onUserClick = { uid ->
                if (onUserClick != null) onUserClick(uid)
                else IntentUtil.startActivity<UserActivity>(context) { putExtra("id", uid) }
            },
            onFollowClick = { uid, isFollow -> itemListener.onFollowUser(uid, isFollow) },
        )

        else -> {
            val title = data.title ?: data.username ?: data.id.orEmpty()
            val summary = data.subTitle ?: data.description ?: ""
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEntityClick?.invoke(data) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (summary.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = summary,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * 用户行（复刻 item_search_user.xml + AppAdapter.UserViewHolder.bind 的三分支）。
 * 关注按钮只在「data.userInfo != null && data.fUserInfo == null」且已登录时显示。
 */
@Composable
private fun FollowUserRow(
    data: HomeFeedResponse.Data,
    onUserClick: (String?) -> Unit,
    onFollowClick: (uid: String, isFollow: Int) -> Unit,
) {
    val userInfo = data.userInfo
    val fUserInfo = data.fUserInfo

    val uid: String?
    val uname: String?
    val follow: String?
    val fans: String?
    val logintime: Long?
    val avatar: String?
    val isFollow: Int
    val showFollowButton: Boolean

    if (userInfo != null && fUserInfo != null) {
        uid = userInfo.uid
        uname = userInfo.username
        follow = userInfo.follow
        fans = userInfo.fans
        logintime = userInfo.logintime
        avatar = userInfo.userAvatar
        isFollow = 0
        showFollowButton = false
    } else if (userInfo == null && fUserInfo != null) {
        uid = fUserInfo.uid
        uname = fUserInfo.username
        follow = fUserInfo.follow
        fans = fUserInfo.fans
        logintime = fUserInfo.logintime
        avatar = fUserInfo.userAvatar
        isFollow = 0
        showFollowButton = false
    } else {
        uid = data.uid
        uname = data.username
        follow = data.follow
        fans = data.fans
        logintime = data.logintime
        avatar = data.userAvatar
        isFollow = data.isFollow ?: 0
        showFollowButton = PrefManager.isLogin
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserClick(uid) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarImage(
            url = avatar,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = uname.orEmpty(),
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Row {
                Text(
                    text = "$follow关注",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "$fans粉丝",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = DateUtils.fromToday(logintime ?: 0L) + "活跃",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                )
            }
        }
        if (showFollowButton) {
            Spacer(Modifier.width(8.dp))
            TextButton(
                text = if (isFollow == 1) "已关注" else "关注",
                onClick = {
                    uid?.let { onFollowClick(it, isFollow) }
                },
                colors = if (isFollow == 1)
                    ButtonDefaults.textButtonColors()
                else
                    ButtonDefaults.textButtonColorsPrimary(),
                insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** 圆形头像：沿用 Glide 链路（ImageUtil.showIMG），AndroidView 包 ImageView。 */
@Composable
private fun AvatarImage(
    url: String?,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        modifier = modifier,
        update = { imageView -> ImageUtil.showIMG(imageView, url) },
    )
}

/**
 * 复刻 FollowFragment.onCreate 的 url/title 预置（topic/product/favorite），
 * 必须在第一次 fetchData 之前调用。
 */
private fun applyFollowTypeConfig(viewModel: FollowViewModel) {
    when (viewModel.type) {
        "topic" -> {
            viewModel.url = "#/topic/userFollowTagList"
            viewModel.title = "我关注的话题"
        }

        "product" -> {
            viewModel.url = "#/product/followProductList"
            viewModel.title = "我关注的数码吧"
        }

        "favorite" -> {
            viewModel.url = "#/collection/followList"
            viewModel.title = "我关注的收藏单"
        }
    }
}

/** 复刻 BaseViewFragment.refreshData()。 */
private fun refreshFollowList(viewModel: FollowViewModel, showLoadingIndicator: Boolean) {
    viewModel.lastItem = null
    viewModel.page = 1
    viewModel.isEnd = false
    viewModel.isRefreshing = true
    viewModel.isLoadMore = false
    if (showLoadingIndicator) {
        viewModel.loadingState.value = LoadingState.Loading
    }
    viewModel.fetchData()
}

/** 每个分栏一个 [FollowViewModel]，随 ViewModelStore 走配置变更存活。 */
@Composable
private fun rememberFollowViewModel(
    tab: FollowTab,
    fallbackUid: String,
    viewModelFactory: (uid: String, type: String) -> ViewModelProvider.Factory,
): FollowViewModel {
    val uid = tab.uid ?: fallbackUid
    return viewModel(
        key = "c001apk_follow_${uid}_${tab.type}",
        factory = viewModelFactory(uid, tab.type),
    )
}
