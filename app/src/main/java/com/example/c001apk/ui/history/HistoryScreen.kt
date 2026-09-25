package com.example.c001apk.ui.history

import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.observeAsState
import com.example.c001apk.R
import com.example.c001apk.logic.model.HitHistoryData
import com.example.c001apk.util.DateUtils
import com.example.c001apk.util.ImageUtil
import com.example.c001apk.util.Utils.richToString
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.HorizontalDivider
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
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 浏览历史页面（酷安云端）—— 对应老的 [HistoryActivity] + [HistoryAdapter]（item_history_cloud.xml）。
 *
 * 数据流完全复用 [HistoryViewModel]（LiveData 用 observeAsState 桥接），本文件只负责界面与交互：
 * 缩略图（logo）/ 标题 / 描述 / 类型·时间，下拉刷新、滚到底自动加载更多、进入页面自动拉取，
 * 右上角「清空」入口，清空前用 Miuix [OverlayDialog] 确认。
 *
 * 注意：
 * - 不自带 MiuixAppTheme（根主题由 Activity 接线时套）；页面自含 Scaffold（OverlayDialog 需要 Scaffold 祖先）。
 * - [HistoryViewModel] 是 `@HiltViewModel`，由 Activity 侧 `viewModels()` 创建后传入。
 * - [onClearHistory] 是提升出来的清空回调：现有 [HistoryViewModel] 只有 `GET /v6/user/hitHistoryList`
 *   读取接口（云端无删除 / 清空接口，老代码也没有清空入口），清空落地逻辑由接线方决定
 *   （可先清本地 Room 历史 `HistoryFavoriteRepo.deleteAllHistory()` + 本地隐藏，见报告）。
 * - 图片沿用 Glide 链路（ImageUtil.showIMG + AndroidView 包 ImageView），未引入新图片库。
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onOpenHistory: (HitHistoryData) -> Unit,
    onClearHistory: () -> Unit = {},
    onToast: (String) -> Unit = {},
) {
    val historyList by viewModel.historyList.observeAsState(emptyList())
    val initialLoading by viewModel.initialLoading.observeAsState(false)
    val toastEvent by viewModel.toastText.observeAsState()
    val list = historyList.orEmpty()

    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    var refreshing by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    // 进入页面拉取（老 HistoryActivity.onCreate 直接 viewModel.refresh()）
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    // 下拉刷新指示与 VM 的首屏加载指示保持同步
    LaunchedEffect(initialLoading) {
        refreshing = initialLoading == true
    }

    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let { onToast(it) }
    }

    // 滚到底自动加载更多（HistoryViewModel.loadMore 自带 isEnd / isLoading 守卫）
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to
                listState.layoutInfo.totalItemsCount
        }.collect { (last, total) ->
            if (last != null && total > 0 && last >= total - 1) {
                viewModel.loadMore()
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.history),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },
                actions = {
                    // 清空入口（老页面没有；新需求加的），确认后走 onClearHistory 回调
                    TextButton(
                        text = "清空",
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.textButtonColors(
                            textColor = MiuixTheme.colorScheme.error,
                        ),
                    )
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                refreshing && list.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                list.isEmpty() -> {
                    Text(
                        text = "暂无浏览历史",
                        modifier = Modifier.align(Alignment.Center),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }

                else -> {
                    PullToRefresh(
                        isRefreshing = refreshing,
                        onRefresh = {
                            refreshing = true
                            viewModel.refresh()
                        },
                        pullToRefreshState = pullToRefreshState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                        ) {
                            items(list) { item ->
                                HistoryRow(
                                    item = item,
                                    onClick = { onOpenHistory(item) },
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 74.dp),
                                )
                            }
                            if (viewModel.isEnd) {
                                item {
                                    Text(
                                        text = "没有更多了",
                                        style = MiuixTheme.textStyles.footnote2,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 清空确认对话框（OverlayDialog 需要 Scaffold 祖先，放在 Scaffold content 内）
            OverlayDialog(
                title = "清空浏览历史",
                summary = "确定清空全部浏览历史吗？",
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
                        text = "清空",
                        onClick = {
                            showClearConfirm = false
                            onClearHistory()
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
 * 一行浏览历史（对应 item_history_cloud.xml）：
 * logo（46dp，无 logo 时隐藏）+ 标题（1 行）+ 描述（2 行，空则隐藏）+ 类型 · 时间。
 * 服务端 title/description 带 HTML 标签，沿用 [richToString] 转纯文本。
 */
@Composable
private fun HistoryRow(
    item: HitHistoryData,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val logo = item.logo
        if (!logo.isNullOrEmpty()) {
            HistoryLogo(url = logo)
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title.orEmpty().richToString(),
                style = MiuixTheme.textStyles.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val descText = item.description.orEmpty().richToString().trim()
            if (descText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = descText,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = buildString {
                    if (!item.typeName.isNullOrEmpty()) append(item.typeName)
                    item.dateline?.let {
                        if (isNotEmpty()) append(" · ")
                        append(DateUtils.fromToday(it))
                    }
                },
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 历史条目缩略图：Glide（ImageUtil.showIMG）+ AndroidView 包 ImageView */
@Composable
private fun HistoryLogo(url: String) {
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
        modifier = Modifier.size(46.dp),
    )
}
