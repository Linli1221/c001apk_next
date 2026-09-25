package com.example.c001apk.ui.feed.vote

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.observeAsState
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.logic.model.TotalReplyResponse
import com.example.c001apk.ui.feed.FeedViewModel
import com.example.c001apk.util.DateUtils
import com.example.c001apk.util.ImageUtil
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * 投票动态详情页 —— 对应老的 [com.example.c001apk.ui.feed.vote.FeedVoteFragment]
 * （feedType == "vote"，FeedViewModel 数据 + 投票面板 + 投票评论瀑布流）。
 *
 * 数据流完全复用 [FeedViewModel]（LiveData 用 observeAsState 桥接）：
 * - 头部主体 = viewModel.feedDataList[0]（Activity 侧 LoadingDone 后才会 compose 本页面，数据已就绪）
 * - 评论列表 = viewModel.feedReplyData；加载更多 = viewModel.preFetchVoteComment()（与老 Fragment 一致）
 * - footer 状态 = viewModel.footerState；下拉刷新沿用老的 page/isEnd/isRefreshing/isLoadMore 标志位
 *
 * 注意：
 * - 不自带 MiuixAppTheme（根主题由接线方套）；页面自含一个 Scaffold。
 * - 头像沿用 Glide 链路（ImageUtil.showIMG + AndroidView 包 ImageView），未引入新图片库。
 * - 老代码没有投票面板 UI（只有列表），[VotePanel] 是按 HomeFeedResponse.Vote 模型新做的块级组件，
 *   可单独嵌入动态详情页；投票提交接口老代码没有实现，故只暴露 onOptionClick/onSubmit 回调。
 */
@Composable
fun FeedVoteScreen(
    viewModel: FeedViewModel,
    onBack: () -> Unit,
    onOpenFeed: (String) -> Unit = {},
    onOpenUser: (String) -> Unit = {},
    onToast: (String) -> Unit = {},
) {
    // observeAsState 返回 State<T?>，统一在这里归一化
    val replies by viewModel.feedReplyData.observeAsState(emptyList())
    val footer by viewModel.footerState.observeAsState(FooterState.LoadingDone)
    val toastEvent by viewModel.toastText.observeAsState()
    val replyList = replies.orEmpty()
    val footerState = footer ?: FooterState.LoadingDone

    // toast 事件桥接（Event 语义：只会被消费一次）
    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let { onToast(it) }
    }

    // 进入页面首次拉取（与老 Fragment 的 initData 一致）
    LaunchedEffect(Unit) {
        if (viewModel.isInit) {
            viewModel.isInit = false
            refreshVote(viewModel)
        }
    }

    // PullToRefresh 的 isRefreshing 提升为本地状态；footer 离开 Loading 即视为刷新结束
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(footerState) {
        if (footerState !is FooterState.Loading) isRefreshing = false
    }

    // 滑到底自动加载更多（对应老 Fragment 的 OnScrollListener）
    val gridState = rememberLazyStaggeredGridState()
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo }
            .collect { info ->
                val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@collect
                if (info.totalItemsCount > 0 &&
                    lastVisible >= info.totalItemsCount - 1 &&
                    !viewModel.isEnd && !viewModel.isRefreshing && !viewModel.isLoadMore &&
                    footerState !is FooterState.Loading && footerState !is FooterState.LoadingError
                ) {
                    loadMoreVote(viewModel)
                }
            }
    }

    // 投票面板的选中态：纯界面态（老代码没有投票提交接口）
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    val vote = viewModel.feedDataList?.getOrNull(0)?.vote

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = viewModel.feedTypeName ?: "投票",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            MiuixIcons.Back,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        PullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (!viewModel.isLoadMore) {
                    isRefreshing = true
                    refreshVote(viewModel)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            pullToRefreshState = rememberPullToRefreshState(),
        ) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalItemSpacing = 10.dp,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 头部：动态主体 + 投票面板
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        FeedAuthorRow(
                            avatar = viewModel.feedDataList?.getOrNull(0)?.userAvatar,
                            username = viewModel.feedDataList?.getOrNull(0)?.userInfo?.username,
                            dateText = viewModel.feedDataList?.getOrNull(0)?.dateline
                                ?.let { DateUtils.fromToday(it) }.orEmpty(),
                            device = viewModel.feedDataList?.getOrNull(0)?.deviceTitle,
                            onOpenUser = {
                                viewModel.feedDataList?.getOrNull(0)?.uid?.let(onOpenUser)
                            },
                        )
                        val message = viewModel.feedDataList?.getOrNull(0)?.message
                        if (!message.isNullOrEmpty()) {
                            Text(
                                text = message,
                                style = MiuixTheme.textStyles.body1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                            )
                        }
                        if (vote != null) {
                            VotePanel(
                                vote = vote,
                                selectedOptionIds = selectedIds,
                                onOptionClick = { option ->
                                    val id = option.id ?: return@VotePanel
                                    val maxSelect = vote.maxSelectNum ?: 1
                                    selectedIds = when {
                                        id in selectedIds -> selectedIds - id
                                        maxSelect <= 1 -> setOf(id)
                                        selectedIds.size < maxSelect -> selectedIds + id
                                        else -> selectedIds
                                    }
                                },
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                }

                items(replyList, key = { it.id }) { reply ->
                    VoteCommentCard(
                        reply = reply,
                        onClick = { onOpenFeed(reply.id) },
                        onUserClick = { onOpenUser(reply.uid) },
                        onLikeClick = {
                            viewModel.onLikeFeed(reply.id, reply.userAction?.like ?: 0)
                        },
                    )
                }

                if (replyList.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Text(
                            text = if (footerState is FooterState.Loading) "" else "暂无评论",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        )
                    }
                }

                // footer：加载中 / 没有更多 / 出错重试
                item(span = StaggeredGridItemSpan.FullLine) {
                    ListFooter(
                        state = footerState,
                        onRetry = {
                            viewModel.isEnd = false
                            loadMoreVote(viewModel)
                        },
                    )
                }
            }
        }
    }
}

/** 刷新：复位老 ViewModel 的分页标志位后重新拉取（对应老 Fragment 的 refreshData） */
private fun refreshVote(viewModel: FeedViewModel) {
    viewModel.firstItem = null
    viewModel.lastItem = null
    viewModel.page = 1
    viewModel.isEnd = false
    viewModel.isRefreshing = true
    viewModel.isLoadMore = false
    viewModel.preFetchVoteComment()
}

/** 加载更多（对应老 Fragment 的 loadMore） */
private fun loadMoreVote(viewModel: FeedViewModel) {
    viewModel.isLoadMore = true
    viewModel.preFetchVoteComment()
}

/**
 * 投票面板（块级组件，可单独嵌入动态详情页）：
 * 题目 + 每个选项的进度条/百分比/票数 + 参与人数与截止时间。
 *
 * 进度用 Miuix [LinearProgressIndicator]；颜色一律走 MiuixTheme.colorScheme。
 * 选项的 `color` 字段是接口下发的十六进制色值，按契约不硬编码/不解析色值，统一用主题 primary。
 *
 * @param vote 投票数据（HomeFeedResponse.Vote）
 * @param selectedOptionIds 当前选中的选项 id（界面态由调用方持有）
 * @param onOptionClick 点选项回调（投票提交接口老代码未实现，由调用方决定行为）
 * @param onSubmit 非空时展示「投票」按钮；为 null 时只展示结果
 */
@Composable
fun VotePanel(
    vote: HomeFeedResponse.Vote,
    modifier: Modifier = Modifier,
    selectedOptionIds: Set<String> = emptySet(),
    onOptionClick: (HomeFeedResponse.Option) -> Unit = {},
    onSubmit: (() -> Unit)? = null,
) {
    val options = vote.options.orEmpty()
    val totalVotes = vote.totalVoteNum ?: 0
    val fallbackTotal = options.sumOf { it.totalSelectNum ?: 0L }
    val denominator = when {
        totalVotes > 0 -> totalVotes.toLong()
        fallbackTotal > 0L -> fallbackTotal
        else -> 1L
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = vote.messageTitle?.takeIf { it.isNotEmpty() } ?: "投票",
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
            )

            val modeText = if ((vote.maxSelectNum ?: 1) > 1) {
                "多选（最多 ${vote.maxSelectNum} 项）"
            } else {
                "单选"
            }
            Text(
                text = modeText,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 4.dp),
            )

            options.forEach { option ->
                val selected = option.id != null && option.id in selectedOptionIds
                VoteOptionRow(
                    option = option,
                    fraction = (option.totalSelectNum ?: 0L).toFloat() / denominator,
                    selected = selected,
                    onClick = { onOptionClick(option) },
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            val nowSeconds = System.currentTimeMillis() / 1000
            val endSeconds = vote.endTime
            val metaText = buildString {
                append("${vote.totalVoteNum ?: 0} 人参与")
                vote.totalOptionNum?.let { append(" · $it 个选项") }
                if (endSeconds != null && endSeconds > 0) {
                    if (endSeconds < nowSeconds) {
                        append(" · 已截止")
                    } else {
                        append(" · 截止 ${DateUtils.timeStamp2Date(endSeconds)}")
                    }
                }
            }
            Text(
                text = metaText,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 10.dp),
            )

            if (onSubmit != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        text = "投票",
                        onClick = onSubmit,
                        enabled = selectedOptionIds.isNotEmpty(),
                    )
                }
            }
        }
    }
}

/** 单个投票项：选中圆点 + 选项标题 + 进度条 + 百分比/票数 */
@Composable
private fun VoteOptionRow(
    option: HomeFeedResponse.Option,
    fraction: Float,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(12.dp),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VoteSelectDot(selected = selected)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.title.orEmpty(),
                    style = MiuixTheme.textStyles.body1,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = fraction.coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${(fraction * 100).toInt()}%",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${option.totalSelectNum ?: 0L}票",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

/** 选中态圆点（自绘，选中填 primary，未选中描边 outline） */
@Composable
private fun VoteSelectDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .border(
                width = 1.5.dp,
                color = if (selected) MiuixTheme.colorScheme.primary
                else MiuixTheme.colorScheme.outline,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.primary),
            )
        }
    }
}

/** 投票参与者评论卡片（item_feed_content_reply_item 的 Compose 版，块级可复用） */
@Composable
fun VoteCommentCard(
    reply: TotalReplyResponse.Data,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onUserClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(12.dp),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            FeedAvatar(url = reply.userAvatar, size = 30.dp, onClick = onUserClick)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reply.userInfo.username,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (reply.message.isNotEmpty()) {
                    Text(
                        text = reply.message,
                        style = MiuixTheme.textStyles.body1,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = DateUtils.fromToday(reply.dateline),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.weight(1f),
                    )
                    if (reply.replynum.isNotEmpty()) {
                        Text(
                            text = "回复 ${reply.replynum}",
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(end = 10.dp),
                        )
                    }
                    Text(
                        text = reply.likenum,
                        style = MiuixTheme.textStyles.footnote1,
                        color = if (reply.userAction?.like == 1) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.clickable(onClick = onLikeClick),
                    )
                }
            }
        }
    }
}

/** 列表 footer：加载中 / 没有更多 / 出错重试（对应老 FooterAdapter） */
@Composable
internal fun ListFooter(
    state: FooterState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is FooterState.Loading -> {
                CircularProgressIndicator()
            }

            is FooterState.LoadingEnd -> {
                Text(
                    text = state.msg,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }

            is FooterState.LoadingError -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.errMsg,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    TextButton(text = "重试", onClick = onRetry)
                }
            }

            else -> {
                // LoadingDone / LoadingReply：不留痕迹
            }
        }
    }
}

/** 动态作者行：头像 + 昵称 + 发布时间 · 设备 */
@Composable
internal fun FeedAuthorRow(
    avatar: String?,
    username: String?,
    dateText: String,
    device: String?,
    onOpenUser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FeedAvatar(url = avatar, size = 34.dp, onClick = onOpenUser)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = username.orEmpty(),
                style = MiuixTheme.textStyles.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    dateText.takeIf { it.isNotEmpty() },
                    device?.takeIf { it.isNotEmpty() },
                ).joinToString(" · "),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 圆形头像：Glide（ImageUtil.showIMG）+ AndroidView 包 ImageView，沿用老图片链路 */
@Composable
internal fun FeedAvatar(
    url: String?,
    size: Dp,
    onClick: () -> Unit,
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
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    )
}
