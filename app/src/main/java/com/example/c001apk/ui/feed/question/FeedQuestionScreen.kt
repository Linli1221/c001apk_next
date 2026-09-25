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
import androidx.compose.runtime.livedata.observeAsState
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
 * 闂瓟鍔ㄦ€佽鎯呴〉 鈥斺€?瀵瑰簲鑰佺殑 [com.example.c001apk.ui.feed.question.FeedQuestionFragment]
 * 锛坒eedType == "question"锛孎eedViewModel 鏁版嵁 + 闂鍗＄墖 + 鍥炵瓟鍒楄〃锛夈€? *
 * 鏁版嵁娴佸畬鍏ㄥ鐢?[FeedViewModel]锛圠iveData 鐢?observeAsState 妗ユ帴锛夛細
 * - 闂涓讳綋 = viewModel.feedDataList[0]锛圓ctivity 渚?LoadingDone 鍚庢墠浼?compose 鏈〉闈紝鏁版嵁宸插氨缁級
 * - 鍥炵瓟鍒楄〃 = viewModel.feedReplyData锛?v6/question/answerList锛宖etchAnswerList 濉厖锛? * - footer 鐘舵€?= viewModel.footerState锛涗笅鎷夊埛鏂版部鐢ㄨ€佺殑 page/isEnd/isRefreshing/isLoadMore 鏍囧織浣? *
 * 甯冨眬涓庤€?Fragment 涓€鑷达細绔栧睆鍗曞垪銆佹í灞忎袱鍒楃€戝竷娴侊紙StaggeredGridCells.Fixed(1/2) 鍒囨崲锛夈€? * 娉ㄦ剰锛? * - 涓嶈嚜甯?MiuixAppTheme锛堟牴涓婚鐢辨帴绾挎柟濂楋級锛涢〉闈㈣嚜鍚竴涓?Scaffold銆? * - 澶村儚娌跨敤 Glide 閾捐矾锛圛mageUtil.showIMG + AndroidView 鍖?ImageView锛夛紝鏈紩鍏ユ柊鍥剧墖搴撱€? */
@Composable
fun FeedQuestionScreen(
    viewModel: FeedViewModel,
    onBack: () -> Unit,
    onOpenFeed: (String) -> Unit = {},
    onOpenUser: (String) -> Unit = {},
    onToast: (String) -> Unit = {},
) {
    // observeAsState 杩斿洖 State<T?>锛岀粺涓€鍦ㄨ繖閲屽綊涓€鍖?    val replies by viewModel.feedReplyData.observeAsState(emptyList())
    val footer by viewModel.footerState.observeAsState(FooterState.LoadingDone)
    val toastEvent by viewModel.toastText.observeAsState()
    val answerList = replies.orEmpty()
    val footerState = footer ?: FooterState.LoadingDone

    // toast 浜嬩欢妗ユ帴锛圗vent 璇箟锛氬彧浼氳娑堣垂涓€娆★級
    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let { onToast(it) }
    }

    // 杩涘叆椤甸潰棣栨鎷夊彇锛堜笌鑰?Fragment 鐨?initData 涓€鑷达級
    LaunchedEffect(Unit) {
        if (viewModel.isInit) {
            viewModel.isInit = false
            refreshAnswer(viewModel)
        }
    }

    // PullToRefresh 鐨?isRefreshing 鎻愬崌涓烘湰鍦扮姸鎬侊紱footer 绂诲紑 Loading 鍗宠涓哄埛鏂扮粨鏉?    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(footerState) {
        if (footerState !is FooterState.Loading) isRefreshing = false
    }

    // 婊戝埌搴曡嚜鍔ㄥ姞杞芥洿澶氾紙瀵瑰簲鑰?Fragment 鐨?OnScrollListener锛?    val listState = rememberLazyStaggeredGridState()
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

    // 绔栧睆鍗曞垪銆佹í灞忎袱鍒楋紙瀵瑰簲鑰?Fragment 鐨?LinearLayoutManager / StaggeredGridLayoutManager 鍒囨崲锛?    val isPortrait =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = viewModel.feedTypeName ?: "闂瓟",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            MiuixIcons.Back,
                            contentDescription = "杩斿洖",
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
                // 澶撮儴锛氶棶棰樺崱鐗?                item(span = StaggeredGridItemSpan.FullLine) {
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
                            text = if (footerState is FooterState.Loading) "" else "鏆傛棤鍥炵瓟",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        )
                    }
                }

                // footer锛氬姞杞戒腑 / 娌℃湁鏇村 / 鍑洪敊閲嶈瘯
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

/** 鍒锋柊锛氬浣嶈€?ViewModel 鐨勫垎椤垫爣蹇椾綅鍚庨噸鏂版媺鍙栵紙瀵瑰簲鑰?Fragment 鐨?refreshData锛?*/
private fun refreshAnswer(viewModel: FeedViewModel) {
    viewModel.firstItem = null
    viewModel.lastItem = null
    viewModel.page = 1
    viewModel.isEnd = false
    viewModel.isRefreshing = true
    viewModel.isLoadMore = false
    viewModel.fetchAnswerList()
}

/** 鍔犺浇鏇村锛堝搴旇€?Fragment 鐨?loadMore锛?*/
private fun loadMoreAnswer(viewModel: FeedViewModel) {
    viewModel.isLoadMore = true
    viewModel.fetchAnswerList()
}

/**
 * 闂瓟鍗＄墖锛堝潡绾х粍浠讹紝鍙崟鐙祵鍏ュ姩鎬佽鎯呴〉锛夛細
 * 鎻愰棶浜?+ 闂鏍囬锛坢essageTitle锛? 闂鎻忚堪锛坢essage锛? 鍥炵瓟/鐐硅禐鏁般€? */
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
        // messageTitle 鏄?val銆乵essage 鏄?var锛堜笉鍙?smart cast锛夛紝鍏堝彇鍒版湰鍦板彉閲?        val questionTitle = data.messageTitle
        val questionBody = data.message
        if (!questionTitle.isNullOrEmpty()) {
            Text(
                text = questionTitle,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            )
        }
        if (!questionBody.isNullOrEmpty()) {
            Text(
                text = questionBody,
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
                text = "${data.replynum ?: "0"} 涓洖绛?,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${data.likenum ?: "0"} 浜鸿禐鍚?,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/** 鍥炵瓟鍗＄墖锛坕tem_feed_content_reply_item 鐨?Compose 鐗堬級锛氬洖绛斾汉 + 姝ｆ枃 + 鏃堕棿/鐐硅禐/鍥炲鏁?*/
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
                            text = "鍥炲 ${answer.replynum}",
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

