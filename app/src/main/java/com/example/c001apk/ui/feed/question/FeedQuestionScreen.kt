package com.example.c001apk.ui.feed.question

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.observeAsState
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.logic.model.TotalReplyResponse
import com.example.c001apk.ui.feed.FeedViewModel
import com.example.c001apk.ui.feed.vote.FeedAvatar
import com.example.c001apk.ui.feed.vote.FeedAuthorRow
import com.example.c001apk.ui.feed.vote.ListFooter
import com.example.c001apk.util.DateUtils
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * 问答动态详情页 —— 对应老的 [com.example.c001apk.ui.feed.question.FeedQuestionFragment]
 * （feedType == "question"，FeedViewModel 数据 + 问题卡片 + 回答列表）。
 *
 * 数据流完全复用 [FeedViewModel]（LiveData 用 observeAsState 桥接）：
 * - 问题主体 = viewModel.feedDataList[0]（Activity 侧 LoadingDone 后才会 compose 本页面，数据已就绪）
 * - 回答列表 = viewModel.feedReplyData（/v6/question/answerList，fetchAnswerList 填充）
 * - footer 状态 = viewModel.footerState；下拉刷新沿用老的 page/isEnd/isRefreshing/isLoadMore 标志位
 *
 * 布局与老 Fragment 一致：竖屏单列、横屏两列瀑布流（StaggeredGridCells.Fixed(1/2) 切换）。
 * 注意：
 * - 不自带 MiuixAppTheme（根主题由接线方套）；页面自含一个 Scaffold。
 * - 头像沿用 Glide 链路（ImageUtil.showIMG + AndroidView 包 ImageView），未引入新图片库。
 */
@Composable
fun FeedQuestionScreen(
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
    val answerList = replies.orEmpty()
    val footerState = footer ?: FooterState.LoadingDone

    // toast 事件桥接（Event 语义：只会被消费一次）
    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let { onToast(it) }
    }

    // 进入页面首次拉取（与老 Fragment 的 initData 一致）
    LaunchedEffect(Unit) {
        if (viewModel.isInit) {
            viewModel.isInit = false
            refreshAnswer(viewModel)
        }
    }

    // PullToRefresh 的 isRefreshing 提升为本地状态；footer 离开 Loading 即视为刷新结束
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(footerState) {
        if (footerState !is FooterState.Loading) isRefreshing = false
    }

    // 滑到底自动加载更多（对应老 Fragment 的 OnScrollListener）
    val listState = rememberLazyStaggeredGridState()
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { info ->
                val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@collect
                if (info.totalItemsCount > 0 &&
                    lastVisible >= info.totalItemsCount - 1 &&
                    !viewModel.isEnd && !viewModel.isRefreshing && !viewModel.isLoadMore &&
                    footerState !is FooterState.Loading && footerState !is FooterState.LoadingError
                ) {
                    loadMoreAnswer(viewModel)
                }
            }
    }

    // 竖屏单列、横屏两列（对应老 Fragment 的 LinearLayoutManager / StaggeredGridLayoutManager 切换）
    val isPortrait =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = viewModel.feedTypeName ?: "问答",
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
                    refreshAnswer(viewModel)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            pullToRefreshState = rememberPullToRefreshState(),
        ) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(if (isPortrait) 1 else 2),
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalItemSpacing = 10.dp,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 头部：问题卡片
                item(span = StaggeredGridItemSpan.FullLine) {
                    viewModel.feedDataList?.getOrNull(0)?.let { data ->
                        QuestionCard(
                            data = data,
                            onOpenUser = { data.uid?.let(onOpenUser) },
                        )
                    }
                }

                items(answerList, key = { it.id }) { answer ->
                    AnswerCard(
                        answer = answer,
                        onClick = { onOpenFeed(answer.id) },
                        onUserClick = { onOpenUser(answer.uid) },
                        onLikeClick = {
                            if (answer.entityType == "feed") {
                                viewModel.onLikeFeed(answer.id, answer.userAction?.like ?: 0)
                            } else {
                                viewModel.onLikeReply(answer.id, answer.userAction?.like ?: 0)
                            }
                        },
                    )
                }

                if (answerList.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Text(
                            text = if (footerState is FooterState.Loading) "" else "暂无回答",
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
                            loadMoreAnswer(viewModel)
                        },
                    )
                }
            }
        }
    }
}

/** 刷新：复位老 ViewModel 的分页标志位后重新拉取（对应老 Fragment 的 refreshData） */
private fun refreshAnswer(viewModel: FeedViewModel) {
    viewModel.firstItem = null
    viewModel.lastItem = null
    viewModel.page = 1
    viewModel.isEnd = false
    viewModel.isRefreshing = true
    viewModel.isLoadMore = false
    viewModel.fetchAnswerList()
}

/** 加载更多（对应老 Fragment 的 loadMore） */
private fun loadMoreAnswer(viewModel: FeedViewModel) {
    viewModel.isLoadMore = true
    viewModel.fetchAnswerList()
}

/**
 * 问答卡片（块级组件，可单独嵌入动态详情页）：
 * 提问人 + 问题标题（messageTitle）+ 问题描述（message）+ 回答/点赞数。
 */
@Composable
fun QuestionCard(
    data: HomeFeedResponse.Data,
    modifier: Modifier = Modifier,
    onOpenUser: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FeedAuthorRow(
            avatar = data.userAvatar,
            username = data.userInfo?.username,
            dateText = data.dateline?.let { DateUtils.fromToday(it) }.orEmpty(),
            device = data.deviceTitle,
            onOpenUser = onOpenUser,
        )
        if (!data.messageTitle.isNullOrEmpty()) {
            Text(
                text = data.messageTitle,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            )
        }
        if (!data.message.isNullOrEmpty()) {
            Text(
                text = data.message,
                style = MiuixTheme.textStyles.body1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        ) {
            Text(
                text = "${data.replynum ?: "0"} 个回答",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${data.likenum ?: "0"} 人赞同",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/** 回答卡片（item_feed_content_reply_item 的 Compose 版）：回答人 + 正文 + 时间/点赞/回复数 */
@Composable
fun AnswerCard(
    answer: TotalReplyResponse.Data,
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
            FeedAvatar(url = answer.userAvatar, size = 30.dp, onClick = onUserClick)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = answer.userInfo.username,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (answer.message.isNotEmpty()) {
                    Text(
                        text = answer.message,
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
                        text = DateUtils.fromToday(answer.dateline),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.weight(1f),
                    )
                    if (answer.replynum.isNotEmpty()) {
                        Text(
                            text = "回复 ${answer.replynum}",
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(end = 10.dp),
                        )
                    }
                    Text(
                        text = answer.likenum,
                        style = MiuixTheme.textStyles.footnote1,
                        color = if (answer.userAction?.like == 1) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.clickable(onClick = onLikeClick),
                    )
                }
            }
        }
    }
}

