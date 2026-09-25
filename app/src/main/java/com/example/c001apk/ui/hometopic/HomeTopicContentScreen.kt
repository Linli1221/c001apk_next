package com.example.c001apk.ui.hometopic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.observeAsState
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.constant.Constants.LOADING_EMPTY
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 话题/节点内容流列表页（页面级 composable，内容区自含 Scaffold 之外，可单独使用或嵌入）。
 *
 * 对应老实现：[HomeTopicContentFragment] + [com.example.c001apk.ui.base.BaseAppFragment] +
 * [com.example.c001apk.ui.base.BaseViewFragment]（SwipeRefreshLayout + RecyclerView + Footer）。
 * 数据流完全复用 [HomeTopicContentViewModel]（LiveData 用 observeAsState 桥接）：
 *  - 初始/重试：置 `loadingState = Loading` → 本页副作用按老逻辑 [refreshContent]；
 *  - 下拉刷新：[PullToRefresh] `isRefreshing` 提升，`onRefresh` 同步置 true 并刷新；
 *  - 上拉加载：滚到 footer 且 `!isEnd && !isLoadMore && !isRefreshing` 时 loadMore；
 *  - footer：[FooterState] 驱动 加载中/没有更多了/加载失败重试。
 *
 * 老逻辑里 `viewModel.listSize` 由 Fragment 的 dataList observer 维护，本页同样在 dataList
 * 变化时回写，保证 [HomeTopicContentViewModel.fetchData] 的 listSize 分支语义与 View 时代一致。
 */
@Composable
fun HomeTopicContentScreen(
    viewModel: HomeTopicContentViewModel,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onTopicClick: (type: String?, title: String?, url: String?, id: String?) -> Unit = { _, _, _, _ -> },
    onListScrolled: (dy: Int) -> Unit = {},
) {
    val dataList by viewModel.dataList.observeAsState(emptyList())
    val loadingState by viewModel.loadingState.observeAsState()
    val footerState by viewModel.footerState.observeAsState()

    // 与 BaseAppFragment.initObserve 的 viewModel.listSize = it.size 对齐
    LaunchedEffect(dataList) {
        viewModel.listSize = dataList.size
    }

    // 首次组合：与 BaseViewFragment.onResume 的 initData() 对齐，置 Loading 触发首刷
    LaunchedEffect(Unit) {
        if (viewModel.isInit) {
            viewModel.isInit = false
            viewModel.loadingState.value = LoadingState.Loading
        }
    }

    // 与 BaseViewFragment.initObserve 对齐：Loading → refreshData()（loadMore 触发的 Loading 不重刷）
    LaunchedEffect(loadingState) {
        if (loadingState is LoadingState.Loading && !viewModel.isLoadMore) {
            refreshContent(viewModel)
        }
    }

    // 下拉刷新指示器：任意结果状态（footer/主状态变化）到达即结束
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(footerState, loadingState) {
        if (footerState !is FooterState.Loading) {
            isRefreshing = false
        }
    }

    // 上拉加载：滚到列表末尾（footer 可见）且无进行中的请求时触发，对齐 BaseViewFragment.initScroll
    LaunchedEffect(listState, footerState, loadingState, dataList) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 1
        }
            .distinctUntilChanged()
            .collect { atEnd ->
                val canLoadMore = atEnd &&
                    loadingState is LoadingState.LoadingDone &&
                    (footerState == null || footerState is FooterState.LoadingDone) &&
                    !viewModel.isEnd && !viewModel.isRefreshing && !viewModel.isLoadMore
                if (canLoadMore) {
                    viewModel.isLoadMore = true
                    viewModel.fetchData()
                }
            }
    }

    // 滚动方向回调（老实现里用于显示/隐藏底部导航栏）
    val currentOnListScrolled by rememberUpdatedState(onListScrolled)
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y != 0f) currentOnListScrolled(available.y.toInt())
                return Offset.Zero
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = loadingState) {
            null, is LoadingState.Loading -> HomeTopicLoadingView(
                modifier = Modifier.align(Alignment.Center)
            )

            is LoadingState.LoadingError -> HomeTopicErrorView(
                message = state.errMsg,
                retryText = "重试",
                onRetry = { viewModel.loadingState.value = LoadingState.Loading },
                modifier = Modifier.align(Alignment.Center)
            )

            is LoadingState.LoadingFailed -> HomeTopicErrorView(
                message = state.msg,
                // 空态用「刷新」，失败用「重试」，与老实现一致
                retryText = if (state.msg == LOADING_EMPTY) "刷新" else "重试",
                onRetry = { viewModel.loadingState.value = LoadingState.Loading },
                modifier = Modifier.align(Alignment.Center)
            )

            is LoadingState.LoadingDone -> {
                val pullToRefreshState = rememberPullToRefreshState()
                PullToRefresh(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        refreshContent(viewModel)
                    },
                    pullToRefreshState = pullToRefreshState,
                ) {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection),
                    ) {
                        items(dataList) { item ->
                            HomeTopicItemCard(
                                data = item,
                                onClick = {
                                    onTopicClick(
                                        item.entityType,
                                        item.title,
                                        item.url,
                                        item.id
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        item(key = "footer") {
                            ListFooter(
                                footerState = footerState,
                                onRetry = {
                                    // 与 BaseAppFragment.ReloadListener.onReLoad 对齐
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

// ---------------------------------------------------------------------------
// 状态视图（自含实现；ui/common 收敛后可替换为公共组件）
// ---------------------------------------------------------------------------

/** 加载中：对应老 `item_indicator`（不确定进度指示器）。 */
@Composable
internal fun HomeTopicLoadingView(modifier: Modifier = Modifier) {
    InfiniteProgressIndicator(modifier = modifier)
}

/** 错误/空态：对应老 `item_error_layout` / `item_error_message`（消息 + 重试按钮）。 */
@Composable
internal fun HomeTopicErrorView(
    message: String,
    retryText: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            text = retryText,
            onClick = onRetry,
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    }
}

/** 列表 footer：对应老 `FooterAdapter`（加载中 / 没有更多了 / 出错重试）。 */
@Composable
private fun ListFooter(
    footerState: FooterState?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (footerState) {
            FooterState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                InfiniteProgressIndicator(size = 16.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "加载中…",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }

            is FooterState.LoadingEnd -> Text(
                text = footerState.msg,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )

            is FooterState.LoadingError -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = footerState.errMsg,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(text = "重试", onClick = onRetry)
            }

            else -> {}
        }
    }
}

// ---------------------------------------------------------------------------
// 数据流辅助（只操作现有 ViewModel 的公开字段，不改写 ViewModel）
// ---------------------------------------------------------------------------

/** 对应 BaseViewFragment.refreshData()：重置分页游标后重新拉第一页。 */
private fun refreshContent(viewModel: HomeTopicContentViewModel) {
    viewModel.lastItem = null
    viewModel.page = 1
    viewModel.isEnd = false
    viewModel.isRefreshing = true
    viewModel.isLoadMore = false
    viewModel.fetchData()
}
