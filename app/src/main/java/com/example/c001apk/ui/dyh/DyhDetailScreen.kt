package com.example.c001apk.ui.dyh

import android.widget.ImageView
import android.widget.Toast
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.observeAsState
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.constant.Constants
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.util.ImageUtil
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * 看看号单个 tab 的动态列表（DyhDetailFragment 的 Compose 版）。
 *
 * 数据流全部复用现有 [DyhViewModel]（BaseAppViewModel 的 dataList / loadingState /
 * footerState / toastText，LiveData 用 observeAsState 桥接）：
 * - 首次加载 / 重试：置 loadingState = Loading 触发刷新（对齐 BaseViewFragment.initObserve）
 * - 下拉刷新：refreshData() 语义（lastItem/page/isEnd/isRefreshing/isLoadMore 归位后 fetchData）
 * - 上拉加载：footer 进入可视区且 !isEnd && !isRefreshing && !isLoadMore 时 loadMore
 * - 空态 / 加载失败 / 错误消息与 footer（加载中 / 出错重试 / 没有更多了）都按原状态渲染
 *
 * 列表项默认是简化的动态卡片（头像 + 用户名 + 正文），完整九宫格 / 视频 / 投票等
 * 动态卡片由公共 feed 卡片组件通过 [itemContent] 插槽替换；点击占位走 [onItemClick]。
 */
@Composable
fun DyhDetailScreen(
    viewModel: DyhViewModel,
    modifier: Modifier = Modifier,
    onItemClick: (HomeFeedResponse.Data) -> Unit = {},
    itemContent: (@Composable (HomeFeedResponse.Data) -> Unit)? = null,
) {
    val context = LocalContext.current
    val dataList by viewModel.dataList.observeAsState()
    val loadingState by viewModel.loadingState.observeAsState()
    val footerState by viewModel.footerState.observeAsState()
    val toastText by viewModel.toastText.observeAsState()
    val list = dataList.orEmpty()

    // 下拉刷新指示器（isRefreshing 提升到本地，任一加载态结束后收起）
    var isRefreshing by remember { mutableStateOf(false) }

    fun refresh() {
        viewModel.lastItem = null
        viewModel.page = 1
        viewModel.isEnd = false
        viewModel.isRefreshing = true
        viewModel.isLoadMore = false
        viewModel.fetchData()
    }

    fun loadMore() {
        if (!viewModel.isEnd && !viewModel.isRefreshing && !viewModel.isLoadMore
            && viewModel.listSize > 0
        ) {
            viewModel.isLoadMore = true
            viewModel.fetchData()
        }
    }

    // 与 BaseAppFragment 一致：listSize 参与 fetchData 里 loadingState / footerState 的分支
    LaunchedEffect(list.size) { viewModel.listSize = list.size }

    // 对齐 BaseViewFragment：loadingState = Loading 是「刷新」的触发器（首次加载 / 出错重试）
    LaunchedEffect(loadingState) {
        if (loadingState is LoadingState.Loading && !viewModel.isLoadMore) {
            refresh()
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.dataList.value == null && viewModel.loadingState.value == null) {
            viewModel.loadingState.value = LoadingState.Loading
        }
    }

    LaunchedEffect(loadingState, footerState) {
        if (loadingState !is LoadingState.Loading && footerState !is FooterState.Loading) {
            isRefreshing = false
        }
    }

    LaunchedEffect(toastText) {
        toastText?.getContentIfNotHandledOrReturnNull()?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    PullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (!viewModel.isLoadMore) {
                isRefreshing = true
                refresh()
            }
        },
        modifier = modifier.fillMaxSize(),
        refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新…", "刷新成功"),
    ) {
        val state = loadingState
        when {
            // 首屏加载
            state is LoadingState.Loading && list.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            // 空态 / 加载失败（列表为空时全屏展示）
            (state is LoadingState.LoadingFailed || state is LoadingState.LoadingError)
                    && list.isEmpty() -> {
                val msg = when (state) {
                    is LoadingState.LoadingFailed -> state.msg
                    is LoadingState.LoadingError -> state.errMsg
                    else -> ""
                }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = msg,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                    )
                    if (state is LoadingState.LoadingFailed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            text = if (msg == Constants.LOADING_EMPTY) "刷新" else "重试",
                            onClick = { viewModel.loadingState.value = LoadingState.Loading },
                        )
                    }
                }
            }

            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(list) { _, item ->
                        if (itemContent != null) {
                            itemContent(item)
                        } else {
                            DefaultFeedItem(item = item, onItemClick = onItemClick)
                        }
                    }
                    item {
                        LoadMoreFooter(
                            footerState = footerState,
                            onRetry = {
                                viewModel.isEnd = false
                                loadMore()
                            },
                            onVisible = { loadMore() },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 列表尾（FooterAdapter 的 Compose 版）：加载中 / 出错重试 / 没有更多了。
 * 进入可视区时尝试加载下一页（对齐原 RecyclerView 滚动监听的触底加载）。
 */
@Composable
private fun LoadMoreFooter(
    footerState: FooterState?,
    onRetry: () -> Unit,
    onVisible: () -> Unit,
) {
    LaunchedEffect(Unit) { onVisible() }

    when (footerState) {
        is FooterState.Loading -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "加载中…",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        is FooterState.LoadingError -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "加载失败：${footerState.errMsg}",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                TextButton(text = "重试", onClick = onRetry)
            }
        }

        is FooterState.LoadingEnd -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = footerState.msg,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        else -> Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 默认动态卡片（占位简化版）：头像 + 用户名 + 正文。
 * 完整动态卡片（图片九宫格 / 视频 / 转发 / 点赞等）待公共 feed 卡片组件就绪后
 * 通过 [DyhDetailScreen] 的 itemContent 插槽替换。
 */
@Composable
private fun DefaultFeedItem(
    item: HomeFeedResponse.Data,
    onItemClick: (HomeFeedResponse.Data) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        insideMargin = PaddingValues(12.dp),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = { onItemClick(item) },
    ) {
        Row {
            GlideImage(
                url = item.userAvatar,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            )
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    text = item.username.orEmpty(),
                    style = MiuixTheme.textStyles.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val message = item.message ?: item.title.orEmpty()
                if (message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
