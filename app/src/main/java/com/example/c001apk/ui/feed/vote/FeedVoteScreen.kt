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
import androidx.compose.runtime.livedata.observeAsState
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
 * 鎶曠エ鍔ㄦ€佽鎯呴〉 鈥斺€?瀵瑰簲鑰佺殑 [com.example.c001apk.ui.feed.vote.FeedVoteFragment]
 * 锛坒eedType == "vote"锛孎eedViewModel 鏁版嵁 + 鎶曠エ闈㈡澘 + 鎶曠エ璇勮鐎戝竷娴侊級銆? *
 * 鏁版嵁娴佸畬鍏ㄥ鐢?[FeedViewModel]锛圠iveData 鐢?observeAsState 妗ユ帴锛夛細
 * - 澶撮儴涓讳綋 = viewModel.feedDataList[0]锛圓ctivity 渚?LoadingDone 鍚庢墠浼?compose 鏈〉闈紝鏁版嵁宸插氨缁級
 * - 璇勮鍒楄〃 = viewModel.feedReplyData锛涘姞杞芥洿澶?= viewModel.preFetchVoteComment()锛堜笌鑰?Fragment 涓€鑷达級
 * - footer 鐘舵€?= viewModel.footerState锛涗笅鎷夊埛鏂版部鐢ㄨ€佺殑 page/isEnd/isRefreshing/isLoadMore 鏍囧織浣? *
 * 娉ㄦ剰锛? * - 涓嶈嚜甯?MiuixAppTheme锛堟牴涓婚鐢辨帴绾挎柟濂楋級锛涢〉闈㈣嚜鍚竴涓?Scaffold銆? * - 澶村儚娌跨敤 Glide 閾捐矾锛圛mageUtil.showIMG + AndroidView 鍖?ImageView锛夛紝鏈紩鍏ユ柊鍥剧墖搴撱€? * - 鑰佷唬鐮佹病鏈夋姇绁ㄩ潰鏉?UI锛堝彧鏈夊垪琛級锛孾VotePanel] 鏄寜 HomeFeedResponse.Vote 妯″瀷鏂板仛鐨勫潡绾х粍浠讹紝
 *   鍙崟鐙祵鍏ュ姩鎬佽鎯呴〉锛涙姇绁ㄦ彁浜ゆ帴鍙ｈ€佷唬鐮佹病鏈夊疄鐜帮紝鏁呭彧鏆撮湶 onOptionClick/onSubmit 鍥炶皟銆? */
@Composable
fun FeedVoteScreen(
    viewModel: FeedViewModel,
    onBack: () -> Unit,
    onOpenFeed: (String) -> Unit = {},
    onOpenUser: (String) -> Unit = {},
    onToast: (String) -> Unit = {},
) {
    // observeAsState 杩斿洖 State<T?>锛岀粺涓€鍦ㄨ繖閲屽綊涓€鍖?    val replies by viewModel.feedReplyData.observeAsState(emptyList())
    val footer by viewModel.footerState.observeAsState(FooterState.LoadingDone)
    val toastEvent by viewModel.toastText.observeAsState()
    val replyList = replies.orEmpty()
    val footerState = footer ?: FooterState.LoadingDone

    // toast 浜嬩欢妗ユ帴锛圗vent 璇箟锛氬彧浼氳娑堣垂涓€娆★級
    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let { onToast(it) }
    }

    // 杩涘叆椤甸潰棣栨鎷夊彇锛堜笌鑰?Fragment 鐨?initData 涓€鑷达級
    LaunchedEffect(Unit) {
        if (viewModel.isInit) {
            viewModel.isInit = false
            refreshVote(viewModel)
        }
    }

    // PullToRefresh 鐨?isRefreshing 鎻愬崌涓烘湰鍦扮姸鎬侊紱footer 绂诲紑 Loading 鍗宠涓哄埛鏂扮粨鏉?    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(footerState) {
        if (footerState !is FooterState.Loading) isRefreshing = false
    }

    // 婊戝埌搴曡嚜鍔ㄥ姞杞芥洿澶氾紙瀵瑰簲鑰?Fragment 鐨?OnScrollListener锛?    val gridState = rememberLazyStaggeredGridState()
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

    // 鎶曠エ闈㈡澘鐨勯€変腑鎬侊細绾晫闈㈡€侊紙鑰佷唬鐮佹病鏈夋姇绁ㄦ彁浜ゆ帴鍙ｏ級
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    val vote = viewModel.feedDataList?.getOrNull(0)?.vote

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = viewModel.feedTypeName ?: "鎶曠エ",
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
                // 澶撮儴锛氬姩鎬佷富浣?+ 鎶曠エ闈㈡澘
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
                            text = if (footerState is FooterState.Loading) "" else "鏆傛棤璇勮",
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
                            loadMoreVote(viewModel)
                        },
                    )
                }
            }
        }
    }
}

/** 鍒锋柊锛氬浣嶈€?ViewModel 鐨勫垎椤垫爣蹇椾綅鍚庨噸鏂版媺鍙栵紙瀵瑰簲鑰?Fragment 鐨?refreshData锛?*/
private fun refreshVote(viewModel: FeedViewModel) {
    viewModel.firstItem = null
    viewModel.lastItem = null
    viewModel.page = 1
    viewModel.isEnd = false
    viewModel.isRefreshing = true
    viewModel.isLoadMore = false
    viewModel.preFetchVoteComment()
}

/** 鍔犺浇鏇村锛堝搴旇€?Fragment 鐨?loadMore锛?*/
private fun loadMoreVote(viewModel: FeedViewModel) {
    viewModel.isLoadMore = true
    viewModel.preFetchVoteComment()
}

/**
 * 鎶曠エ闈㈡澘锛堝潡绾х粍浠讹紝鍙崟鐙祵鍏ュ姩鎬佽鎯呴〉锛夛細
 * 棰樼洰 + 姣忎釜閫夐」鐨勮繘搴︽潯/鐧惧垎姣?绁ㄦ暟 + 鍙備笌浜烘暟涓庢埅姝㈡椂闂淬€? *
 * 杩涘害鐢?Miuix [LinearProgressIndicator]锛涢鑹蹭竴寰嬭蛋 MiuixTheme.colorScheme銆? * 閫夐」鐨?`color` 瀛楁鏄帴鍙ｄ笅鍙戠殑鍗佸叚杩涘埗鑹插€硷紝鎸夊绾︿笉纭紪鐮?涓嶈В鏋愯壊鍊硷紝缁熶竴鐢ㄤ富棰?primary銆? *
 * @param vote 鎶曠エ鏁版嵁锛圚omeFeedResponse.Vote锛? * @param selectedOptionIds 褰撳墠閫変腑鐨勯€夐」 id锛堢晫闈㈡€佺敱璋冪敤鏂规寔鏈夛級
 * @param onOptionClick 鐐归€夐」鍥炶皟锛堟姇绁ㄦ彁浜ゆ帴鍙ｈ€佷唬鐮佹湭瀹炵幇锛岀敱璋冪敤鏂瑰喅瀹氳涓猴級
 * @param onSubmit 闈炵┖鏃跺睍绀恒€屾姇绁ㄣ€嶆寜閽紱涓?null 鏃跺彧灞曠ず缁撴灉
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
                text = vote.messageTitle?.takeIf { it.isNotEmpty() } ?: "鎶曠エ",
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
            )

            val modeText = if ((vote.maxSelectNum ?: 1) > 1) {
                "澶氶€夛紙鏈€澶?${vote.maxSelectNum} 椤癸級"
            } else {
                "鍗曢€?
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
                append("${vote.totalVoteNum ?: 0} 浜哄弬涓?)
                vote.totalOptionNum?.let { append(" 路 $it 涓€夐」") }
                if (endSeconds != null && endSeconds > 0) {
                    if (endSeconds < nowSeconds) {
                        append(" 路 宸叉埅姝?)
                    } else {
                        append(" 路 鎴 ${DateUtils.timeStamp2Date(endSeconds)}")
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
                        text = "鎶曠エ",
                        onClick = onSubmit,
                        enabled = selectedOptionIds.isNotEmpty(),
                    )
                }
            }
        }
    }
}

/** 鍗曚釜鎶曠エ椤癸細閫変腑鍦嗙偣 + 閫夐」鏍囬 + 杩涘害鏉?+ 鐧惧垎姣?绁ㄦ暟 */
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
                    text = "${option.totalSelectNum ?: 0L}绁?,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

/** 閫変腑鎬佸渾鐐癸紙鑷粯锛岄€変腑濉?primary锛屾湭閫変腑鎻忚竟 outline锛?*/
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

/** 鎶曠エ鍙備笌鑰呰瘎璁哄崱鐗囷紙item_feed_content_reply_item 鐨?Compose 鐗堬紝鍧楃骇鍙鐢級 */
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
                            text = "鍥炲 ${reply.replynum}",
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

/** 鍒楄〃 footer锛氬姞杞戒腑 / 娌℃湁鏇村 / 鍑洪敊閲嶈瘯锛堝搴旇€?FooterAdapter锛?*/
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
                    TextButton(text = "閲嶈瘯", onClick = onRetry)
                }
            }

            else -> {
                // LoadingDone / LoadingReply锛氫笉鐣欑棔杩?            }
        }
    }
}

/** 鍔ㄦ€佷綔鑰呰锛氬ご鍍?+ 鏄电О + 鍙戝竷鏃堕棿 路 璁惧 */
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
                ).joinToString(" 路 "),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 鍦嗗舰澶村儚锛欸lide锛圛mageUtil.showIMG锛? AndroidView 鍖?ImageView锛屾部鐢ㄨ€佸浘鐗囬摼璺?*/
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
            // url 鍙樺寲鎵嶉噸鏂拌蛋 Glide锛岄伩鍏嶆瘡娆￠噸缁勯兘鍙戣捣鍔犺浇
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
