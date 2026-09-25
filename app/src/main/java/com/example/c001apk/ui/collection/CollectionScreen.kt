package com.example.c001apk.ui.collection

import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.observeAsState
import com.example.c001apk.R
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.constant.Constants.LOADING_EMPTY
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.ui.feed.CollectionPickViewModel
import com.example.c001apk.util.DateUtils
import com.example.c001apk.util.ImageUtil
import com.example.c001apk.util.Utils.richToString
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 收藏夹（多收藏夹）页面 —— 对应老的 [CollectionActivity] / [CollectionFragment] /
 * [CollectionContentFragment]（`/v6/collection/list` 收藏夹分组列表 + `/v6/collection/itemList`
 * 单个收藏夹内容两层）。
 *
 * 数据流完全复用 [CollectionContentViewModel]（LiveData 用 observeAsState 桥接），本文件只负责界面与交互：
 * - 第一层「我的收藏单」：收藏夹卡片列表（封面 / 标题 / 简介 / 公开私密 / 关注数 / 内容数），
 *   点击进入第二层；
 * - 第二层「具体收藏夹」：内容列表 + 右上角管理菜单（编辑收藏夹信息 / 清除无效内容 / 删除收藏夹，
 *   后两项用 Miuix [OverlayDialog] 二次确认，管理动作走 [CollectionPickViewModel]，与老
 *   [CollectionFragment] 的菜单一致）；
 * - 两层导航由 [CollectionScreen] 内部状态承载（返回键逐层退出），也可以单独使用
 *   [CollectionFolderListScreen] / [CollectionFolderContentScreen] 走外部导航回调。
 *
 * 注意：
 * - 不自带 MiuixAppTheme（根主题由 Activity 接线时套）；页面自含 Scaffold（OverlayDialog 需要 Scaffold 祖先）。
 * - [CollectionContentViewModel] 是 Hilt assisted ViewModel：
 *   [CollectionScreen.listViewModel] 需用 `url = "/v6/collection/list", id = null` 创建，
 *   [CollectionScreen.contentViewModelFactory] 传入 `{ id -> factory.create("/v6/collection/itemList", id) }`。
 * - 列表动态（entityType = "feed"）默认渲染 [DefaultFeedItem] 简化卡片占位，
 *   后续由 ui/feed 组通过 [feedItem] 插槽替换为完整动态卡片。
 * - 图片沿用 Glide 链路（ImageUtil.showIMG + AndroidView 包 ImageView），未引入新图片库。
 */
@Composable
fun CollectionScreen(
    listViewModel: CollectionContentViewModel,
    contentViewModelFactory: (folderId: String) -> CollectionContentViewModel,
    onBack: () -> Unit,
    manageViewModel: CollectionPickViewModel? = null,
    onOpenFeed: (HomeFeedResponse.Data) -> Unit = {},
    onEditCollectionInfo: (folderId: String, title: String) -> Unit = { _, _ -> },
    onToast: (String) -> Unit = {},
    feedItem: @Composable (HomeFeedResponse.Data) -> Unit = { DefaultFeedItem(it, onOpenFeed) },
) {
    // 当前打开的收藏夹（id → title）；null = 停留在第一层
    var openFolder by remember { mutableStateOf<Pair<String, String>?>(null) }

    BackHandler(enabled = openFolder != null) { openFolder = null }

    val folder = openFolder
    if (folder == null) {
        CollectionFolderListScreen(
            viewModel = listViewModel,
            onBack = onBack,
            onOpenFolder = { id, title -> openFolder = id to title },
            onOpenFeed = onOpenFeed,
            onToast = onToast,
            feedItem = feedItem,
        )
    } else {
        // 每个收藏夹一个 VM；离开第二层即丢弃，重新进入按 id 重建
        val contentViewModel = remember(folder.first) { contentViewModelFactory(folder.first) }
        CollectionFolderContentScreen(
            viewModel = contentViewModel,
            title = folder.second,
            onBack = { openFolder = null },
            manageViewModel = manageViewModel,
            onOpenFeed = onOpenFeed,
            onEditCollectionInfo = onEditCollectionInfo,
            onOpenCollection = { id, title -> openFolder = id to title },
            onToast = onToast,
            feedItem = feedItem,
        )
    }
}

/**
 * 第一层：我的收藏单（收藏夹分组列表）。
 * 对应老 [CollectionFragment]（id 为空的根页面，标题「我的收藏单」）。
 */
@Composable
fun CollectionFolderListScreen(
    viewModel: CollectionContentViewModel,
    onBack: () -> Unit,
    onOpenFolder: (id: String, title: String) -> Unit,
    onOpenFeed: (HomeFeedResponse.Data) -> Unit = {},
    onToast: (String) -> Unit = {},
    feedItem: @Composable (HomeFeedResponse.Data) -> Unit = { DefaultFeedItem(it, onOpenFeed) },
) {
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "我的收藏单",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            CollectionListBody(
                viewModel = viewModel,
                onToast = onToast,
                itemContent = { item ->
                    if (item.entityType == "collection") {
                        CollectionFolderCard(
                            item = item,
                            onClick = {
                                onOpenFolder(item.id.orEmpty(), item.title.orEmpty())
                            },
                        )
                    } else {
                        feedItem(item)
                    }
                },
            )
        }
    }
}

/**
 * 第二层：单个收藏夹内容列表 + 右上角管理菜单。
 * 对应老 [CollectionFragment]（id 非空）+ [CollectionContentFragment]（`/v6/collection/itemList`）。
 *
 * @param title 收藏夹标题（老代码通过 arguments 传入，VM 不保存）。
 * @param manageViewModel 管理动作（清除无效内容 / 删除收藏夹）的 VM；为 null 时隐藏这两项。
 * @param onEditCollectionInfo 「编辑收藏夹信息」回调（老代码是标题 / 简介 / 公开私密 / 封面表单对话框，
 *   含图片选择器，留给接线方或后续轮实现）。
 */
@Composable
fun CollectionFolderContentScreen(
    viewModel: CollectionContentViewModel,
    title: String,
    onBack: () -> Unit,
    manageViewModel: CollectionPickViewModel? = null,
    onOpenFeed: (HomeFeedResponse.Data) -> Unit = {},
    onEditCollectionInfo: (folderId: String, title: String) -> Unit = { _, _ -> },
    onOpenCollection: (id: String, title: String) -> Unit = { _, _ -> },
    onToast: (String) -> Unit = {},
    feedItem: @Composable (HomeFeedResponse.Data) -> Unit = { DefaultFeedItem(it, onOpenFeed) },
) {
    var showMenu by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 管理动作的 toast / 删除成功后退出（老 CollectionFragment 的 deleted → popBackStack）
    if (manageViewModel != null) {
        ManageEvents(
            viewModel = manageViewModel,
            onToast = onToast,
            onDeleted = onBack,
        )
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = title.ifEmpty { "我的收藏单" },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(MiuixIcons.More, contentDescription = "更多")
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            CollectionListBody(
                viewModel = viewModel,
                onToast = onToast,
                itemContent = { item ->
                    if (item.entityType == "collection") {
                        CollectionFolderCard(
                            item = item,
                            onClick = {
                                onOpenCollection(item.id.orEmpty(), item.title.orEmpty())
                            },
                        )
                    } else {
                        feedItem(item)
                    }
                },
            )

            val folderId = viewModel.id.orEmpty()

            // 管理菜单（老 collection_menu：编辑收藏夹信息 / 清除无效内容 / 删除收藏夹）
            OverlayDialog(
                title = title.ifEmpty { "编辑收藏夹" },
                show = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = "编辑收藏夹信息",
                        onClick = {
                            showMenu = false
                            onEditCollectionInfo(folderId, title)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (manageViewModel != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            text = "清除无效内容",
                            onClick = {
                                showMenu = false
                                showClearConfirm = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            text = "删除收藏夹",
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(
                                textColor = MiuixTheme.colorScheme.error,
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = { showMenu = false },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // 清除无效内容确认
            OverlayDialog(
                title = "清除无效内容",
                summary = "将清除该收藏夹内已失效的内容，约 5 分钟后生效，确定继续？",
                show = showClearConfirm,
                onDismissRequest = { showClearConfirm = false },
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = { showClearConfirm = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    TextButton(
                        text = stringResource(android.R.string.ok),
                        onClick = {
                            showClearConfirm = false
                            manageViewModel?.clearUnUse(folderId)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }

            // 删除收藏夹确认
            OverlayDialog(
                title = "删除收藏夹",
                summary = "删除后不可恢复，确定删除「${title}」？",
                show = showDeleteConfirm,
                onDismissRequest = { showDeleteConfirm = false },
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = { showDeleteConfirm = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    TextButton(
                        text = stringResource(android.R.string.ok),
                        onClick = {
                            showDeleteConfirm = false
                            manageViewModel?.delete(folderId)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            textColor = MiuixTheme.colorScheme.error,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * 列表主体（两层共用）：下拉刷新 + 加载更多 + 加载 / 空 / 错误态。
 * 状态机与老 [com.example.c001apk.ui.base.BaseAppFragment] 一致，只是把 View 状态换成了 Compose 状态。
 */
@Composable
private fun CollectionListBody(
    viewModel: CollectionContentViewModel,
    onToast: (String) -> Unit,
    itemContent: @Composable (HomeFeedResponse.Data) -> Unit,
) {
    val dataList by viewModel.dataList.observeAsState(emptyList())
    val loading by viewModel.loadingState.observeAsState()
    val footer by viewModel.footerState.observeAsState()
    val toastEvent by viewModel.toastText.observeAsState()
    val list = dataList.orEmpty()
    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    var refreshing by remember { mutableStateOf(false) }

    // 老 BaseAppFragment：dataList 变化时记录 listSize，供 VM 判断首屏 / 加载更多走哪个状态通道
    LaunchedEffect(list) {
        viewModel.listSize = list.size
    }

    // 首次进入拉取（老 initData → refreshData）；VM 已有数据或已发起过请求则不重复拉取
    LaunchedEffect(Unit) {
        if (viewModel.dataList.value.isNullOrEmpty() && viewModel.loadingState.value == null) {
            refreshCollectionList(viewModel)
        }
    }

    // 任一状态落地即收起下拉刷新指示（老 observer 里的 swipeRefresh.isRefreshing = false）
    LaunchedEffect(loading, footer) {
        refreshing = false
    }

    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let { onToast(it) }
    }

    // 滚到底自动加载更多（老 RecyclerView.OnScrollListener 的 SCROLL_STATE_IDLE 判断）
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to
                listState.layoutInfo.totalItemsCount
        }.collect { (last, total) ->
            if (last != null && total > 0 && last >= total - 1 &&
                !viewModel.isEnd && !viewModel.isRefreshing && !viewModel.isLoadMore
            ) {
                viewModel.isLoadMore = true
                viewModel.fetchData()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            loading is LoadingState.Loading && list.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            loading is LoadingState.LoadingError && list.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = (loading as LoadingState.LoadingError).errMsg,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        text = stringResource(R.string.retry),
                        onClick = { refreshCollectionList(viewModel) },
                    )
                }
            }

            loading is LoadingState.LoadingFailed && list.isEmpty() -> {
                val msg = (loading as LoadingState.LoadingFailed).msg
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = msg,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        text = stringResource(
                            if (msg == LOADING_EMPTY) R.string.refresh else R.string.retry
                        ),
                        onClick = { refreshCollectionList(viewModel) },
                    )
                }
            }

            else -> {
                PullToRefresh(
                    isRefreshing = refreshing,
                    onRefresh = {
                        refreshing = true
                        refreshCollectionList(viewModel)
                    },
                    pullToRefreshState = pullToRefreshState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                    ) {
                        items(list) { item ->
                            itemContent(item)
                        }
                        item {
                            CollectionFooter(
                                footer = footer,
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

/** 列表底部加载态（对应老 FooterAdapter） */
@Composable
private fun CollectionFooter(
    footer: FooterState?,
    onRetry: () -> Unit,
) {
    val state = footer
    when (state) {
        is FooterState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is FooterState.LoadingEnd -> {
            ListFooterText(state.msg)
        }

        is FooterState.LoadingError -> {
            Text(
                text = state.errMsg,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRetry)
                    .padding(16.dp),
            )
        }

        else -> {}
    }
}

/** 列表底部提示文字（如「没有更多了」） */
@Composable
private fun ListFooterText(text: String) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    )
}

/** 对应老 BaseViewFragment.refreshData()：重置分页并拉首页 */
private fun refreshCollectionList(viewModel: CollectionContentViewModel) {
    viewModel.lastItem = null
    viewModel.page = 1
    viewModel.isEnd = false
    viewModel.isRefreshing = true
    viewModel.isLoadMore = false
    viewModel.fetchData()
}

/** 管理动作事件桥接（toast + 删除成功退出），仅 manageViewModel 非空时组合 */
@Composable
private fun ManageEvents(
    viewModel: CollectionPickViewModel,
    onToast: (String) -> Unit,
    onDeleted: () -> Unit,
) {
    val toastEvent by viewModel.toastText.observeAsState()
    val deletedEvent by viewModel.deleted.observeAsState()

    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let { onToast(it) }
    }
    LaunchedEffect(deletedEvent) {
        deletedEvent?.getContentIfNotHandledOrReturnNull()?.let { onDeleted() }
    }
}

/**
 * 收藏夹卡片（对应 item_collection_list_item.xml）：
 * 封面（3:4）+ 标题 / 简介 / 公开私密 / 关注数 / 内容数。
 */
@Composable
private fun CollectionFolderCard(
    item: HomeFeedResponse.Data,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        insideMargin = PaddingValues(10.dp),
    ) {
        Row {
            GlideImage(
                url = item.coverPic,
                modifier = Modifier
                    .width(72.dp)
                    .height(96.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            ) {
                Text(
                    text = item.title.orEmpty(),
                    style = MiuixTheme.textStyles.body1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val description = item.description.orEmpty()
                if (description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = buildString {
                        append(if (item.isOpen == 0) "私密" else "公开")
                        append(" · ")
                        append(item.followNum ?: "0")
                        append("人关注")
                        append(" · ")
                        append(item.itemNum ?: "0")
                        append("个内容")
                    },
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * 收藏内容里的动态卡片占位（entityType = "feed"）。
 * 仅覆盖「头像 / 昵称 / 正文 / 时间 / 转评赞数 + 点击进详情」的最小闭环，
 * 完整动态卡片（图片九宫格 / 投票 / 转发链等）由 ui/feed 组通过 feedItem 插槽替换。
 */
@Composable
private fun DefaultFeedItem(
    item: HomeFeedResponse.Data,
    onClick: (HomeFeedResponse.Data) -> Unit,
) {
    Card(
        onClick = { onClick(item) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        insideMargin = PaddingValues(12.dp),
    ) {
        Row {
            GlideImage(
                url = item.userAvatar,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            ) {
                Text(
                    text = item.username.orEmpty(),
                    style = MiuixTheme.textStyles.body1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val message = item.message.orEmpty().richToString().trim()
                if (message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = message,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = buildString {
                        item.dateline?.let {
                            append(DateUtils.fromToday(it))
                            append(" · ")
                        }
                        append("赞 ${item.likenum ?: "0"}")
                        append(" · 评论 ${item.replynum ?: item.commentnum ?: "0"}")
                    },
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Glide 图片（ImageUtil.showIMG）+ AndroidView 包 ImageView。
 * 未引入新图片库（CONVENTIONS §4）；公共图片组件收敛后可替换。
 */
@Composable
private fun GlideImage(
    url: String?,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            // url 变化才重新走 Glide，避免每次重组都发起加载
            if (imageView.tag != url) {
                imageView.tag = url
                ImageUtil.showIMG(imageView, url)
            }
        },
        modifier = modifier,
    )
}
