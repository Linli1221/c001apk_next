package com.example.c001apk.ui.carousel

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.observeAsState
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.constant.Constants.LOADING_EMPTY
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.ui.base.BaseAppViewModel
import com.example.c001apk.ui.common.EmptyState
import com.example.c001apk.ui.common.ErrorState
import com.example.c001apk.ui.common.LoadingState as LoadingPlaceholder
import com.example.c001apk.ui.common.NineGrid
import com.example.c001apk.ui.common.SimpleImage
import com.example.c001apk.ui.common.StatBar
import com.example.c001apk.util.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * 通用「feed 列表页」：SwipeRefreshLayout + RecyclerView（BaseAppFragment）的 Compose 对应物。
 *
 * 冷图（CoolPic）、轮播/活动（Carousel）两组页面的列表内容都由 [BaseAppViewModel] 系
 * （[com.example.c001apk.ui.collection.CollectionContentViewModel] / [CarouselViewModel]）驱动，
 * 因此这里统一桥接：`dataList` / `loadingState` / `footerState` / `toastText` 用 observeAsState
 * 取状态，刷新/翻页直接调用 ViewModel 现有方法（复刻 BaseViewFragment 的 refreshData/loadMore 语义）。
 *
 * **数据流（与老代码对齐）**
 * - 首次组合：`loadingState = Loading` 后 `refreshFromStart()`（= 旧 `loadingState=Loading → refreshData()`）。
 * - 下拉刷新：[PullToRefresh] 包 LazyColumn，`onRefresh` 走 `refreshFromStart()`。
 * - 触底翻页：滚动停止（等价旧 SCROLL_STATE_IDLE）且最后一项可见时 `loadMore()`；
 *   底部 footer 出错时也可点击重试。
 * - `listSize` 在 dataList 变化时回写 ViewModel（旧 BaseAppFragment 同款，VM 分页判断依赖它）。
 *
 * **图片**：沿用 ui/common 的 [SimpleImage] / [NineGrid]（AndroidView + 现有 Glide 链路，
 * 九宫格自带 Mojito 大图预览），不引入新图片库。
 *
 * **参数**
 * @param viewModel 列表 ViewModel（BaseAppViewModel 子类），状态全部来自它，不在本组件内创建业务逻辑。
 * @param modifier 应用于最外层容器的 [Modifier]。
 * @param contentPadding LazyColumn 的内容内边距（顶栏/标签栏占位由调用方折算）。
 * @param onOpenItem 点击条目回调（跳动态详情等，由宿主接线）。
 * @param onOpenImage 点击大图回调（默认走 NineGrid 内部 Mojito 预览，通常不用传）。
 * @param onOpenUser 点击头像/昵称回调（跳用户主页）。
 * @param onShowCollection 点击「图集/合集」条目回调（旧 `showCollection` 事件）。
 * @param onLikeClick 点赞回调；null 时点赞项不可点击（点赞走 VM `ItemListener.onLikeClick`，由宿主接线）。
 *
 * **祖先要求**：必须位于根主题 `MiuixAppTheme` 之内。本组件不含 Scaffold/TopAppBar，由页面 Screen 提供。
 */
@Composable
fun FeedListPage(
    viewModel: BaseAppViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp),
    onOpenItem: (HomeFeedResponse.Data) -> Unit = {},
    onOpenImage: (urls: List<String>, index: Int) -> Unit = { _, _ -> },
    onOpenUser: (uid: String?, username: String?) -> Unit = { _, _ -> },
    onShowCollection: (id: String, title: String) -> Unit = { _, _ -> },
    onLikeClick: ((HomeFeedResponse.Data) -> Unit)? = null,
) {
    val dataList = viewModel.dataList.observeAsState().value ?: emptyList()
    val loadState = viewModel.loadingState.observeAsState().value
    val footerState = viewModel.footerState.observeAsState().value
    val toast = viewModel.toastText.observeAsState().value
    val context = LocalContext.current

    // 与旧 BaseAppFragment 一致：dataList 变化时回写 listSize（VM 的分页/空列表判断依赖它）
    LaunchedEffect(dataList) { viewModel.listSize = dataList.size }

    // toastText 一次性事件 → Toast
    LaunchedEffect(toast) {
        toast?.getContentIfNotHandledOrReturnNull()?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    // 复刻 BaseViewFragment.refreshData()：从头加载
    fun refreshFromStart() {
        viewModel.loadingState.value = LoadingState.Loading
        viewModel.lastItem = null
        viewModel.page = 1
        viewModel.isEnd = false
        viewModel.isRefreshing = true
        viewModel.isLoadMore = false
        viewModel.fetchData()
    }

    // 复刻 BaseViewFragment.loadMore()
    fun loadMore() {
        if (viewModel.isEnd || viewModel.isRefreshing || viewModel.isLoadMore) return
        viewModel.isLoadMore = true
        viewModel.fetchData()
    }

    // 首次组合自动加载（等价旧「loadingState=Loading → refreshData()」链路）；
    // 页面在 pager 里被回收重建时已有数据/正在加载则不重复拉
    LaunchedEffect(Unit) {
        if (viewModel.dataList.value.isNullOrEmpty() &&
            viewModel.loadingState.value !is LoadingState.Loading &&
            !viewModel.isRefreshing && !viewModel.isLoadMore
        ) {
            refreshFromStart()
        }
    }

    // 下拉刷新指示器
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) return@LaunchedEffect
        // 老 VM 在 fetch 收敛时把 isRefreshing/isLoadMore 置回 false；个别早退分支不复位，
        // 所以再兜一个超时，避免指示器卡住
        val start = System.currentTimeMillis()
        while (viewModel.isRefreshing || viewModel.isLoadMore) {
            if (System.currentTimeMillis() - start > 30_000) break
            delay(80)
        }
        isRefreshing = false
    }
    LaunchedEffect(loadState) {
        if (loadState is LoadingState.LoadingError || loadState is LoadingState.LoadingFailed) {
            isRefreshing = false
        }
    }

    // 触底翻页：等价旧 RecyclerView.SCROLL_STATE_IDLE + 最后一项可见
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        var wasScrolling = false
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (wasScrolling && !scrolling) {
                    val info = listState.layoutInfo
                    val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                    if (info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 1) {
                        loadMore()
                    }
                }
                wasScrolling = scrolling
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (dataList.isEmpty()) {
            when (val state = loadState) {
                null, LoadingState.Loading -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())

                is LoadingState.LoadingError -> ErrorState(
                    onRetry = { refreshFromStart() },
                    modifier = Modifier.fillMaxSize(),
                    message = state.errMsg,
                    retryText = "重试",
                )

                is LoadingState.LoadingFailed -> {
                    if (state.msg == LOADING_EMPTY) {
                        EmptyState(
                            modifier = Modifier.fillMaxSize(),
                            text = state.msg,
                            actionText = "刷新",
                            onAction = { refreshFromStart() },
                        )
                    } else {
                        ErrorState(
                            onRetry = { refreshFromStart() },
                            modifier = Modifier.fillMaxSize(),
                            message = state.msg,
                            retryText = "重试",
                        )
                    }
                }

                LoadingState.LoadingDone -> EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    text = LOADING_EMPTY,
                    actionText = "刷新",
                    onAction = { refreshFromStart() },
                )
            }
        } else {
            PullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = {
                    if (!viewModel.isLoadMore) {
                        isRefreshing = true
                        refreshFromStart()
                    }
                },
                pullToRefreshState = rememberPullToRefreshState(),
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(dataList) { _, item ->
                        FeedItem(
                            data = item,
                            onOpenItem = onOpenItem,
                            onOpenImage = onOpenImage,
                            onOpenUser = onOpenUser,
                            onShowCollection = onShowCollection,
                            onLikeClick = onLikeClick,
                        )
                    }
                    item(key = "footer") {
                        ListFooter(
                            footerState = footerState,
                            onRetry = {
                                viewModel.isEnd = false
                                loadMore()
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 列表底部加载条：加载中 / 没有更多了 / 出错点击重试（对应旧 FooterAdapter）。 */
@Composable
private fun ListFooter(
    footerState: FooterState?,
    onRetry: () -> Unit,
) {
    when (footerState) {
        FooterState.Loading -> LoadingPlaceholder(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            text = null,
        )

        is FooterState.LoadingEnd -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = footerState.msg,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        is FooterState.LoadingError -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onRetry)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${footerState.errMsg}，点击重试",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        else -> Spacer(modifier = Modifier.height(4.dp))
    }
}

/** 按实体类型分发条目渲染（旧 AppAdapter 的 getItemViewType 对应，简化为三类卡片）。 */
@Composable
private fun FeedItem(
    data: HomeFeedResponse.Data,
    onOpenItem: (HomeFeedResponse.Data) -> Unit,
    onOpenImage: (urls: List<String>, index: Int) -> Unit,
    onOpenUser: (uid: String?, username: String?) -> Unit,
    onShowCollection: (id: String, title: String) -> Unit,
    onLikeClick: ((HomeFeedResponse.Data) -> Unit)?,
) {
    when {
        data.entityType == "collection" -> CollectionCard(
            data = data,
            onShowCollection = onShowCollection,
        )

        !data.entities.isNullOrEmpty() -> ImageCarouselCard(
            data = data,
            onOpenItem = onOpenItem,
            onOpenImage = onOpenImage,
        )

        else -> FeedCard(
            data = data,
            onOpenItem = onOpenItem,
            onOpenImage = onOpenImage,
            onOpenUser = onOpenUser,
            onLikeClick = onLikeClick,
        )
    }
}

/** 动态/话题/产品/用户/图文卡片（item_home_feed.xml 的简化 Compose 形态）。 */
@Composable
private fun FeedCard(
    data: HomeFeedResponse.Data,
    onOpenItem: (HomeFeedResponse.Data) -> Unit,
    onOpenImage: (urls: List<String>, index: Int) -> Unit,
    onOpenUser: (uid: String?, username: String?) -> Unit,
    onLikeClick: ((HomeFeedResponse.Data) -> Unit)?,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        insideMargin = PaddingValues(12.dp),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = { onOpenItem(data) },
    ) {
        val avatar = data.userInfo?.userAvatar ?: data.userAvatar
        val username = data.userInfo?.username ?: data.username
        if (!avatar.isNullOrEmpty() || !username.isNullOrEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SimpleImage(
                    url = avatar,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable {
                            onOpenUser(
                                data.userInfo?.uid ?: data.uid,
                                data.userInfo?.username ?: data.username,
                            )
                        },
                    contentDescription = "头像",
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = username.orEmpty(),
                        style = MiuixTheme.textStyles.subtitle,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val meta = listOfNotNull(
                        data.deviceTitle?.takeIf { it.isNotEmpty() },
                        data.dateline?.let { DateUtils.fromToday(it) },
                    ).joinToString(" · ")
                    if (meta.isNotEmpty()) {
                        Text(
                            text = meta,
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        val title = data.title ?: data.messageTitle ?: data.goodsTitle
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        val body = data.message ?: data.description ?: data.goodsPromoTitle
        if (!body.isNullOrBlank()) {
            Text(
                text = body,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }

        val images = resolveImages(data)
        if (images.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            NineGrid(
                picArr = images,
                pic = data.pic ?: images.first(),
                feedType = data.feedType,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        StatBar(
            likeCount = data.likenum ?: "0",
            replyCount = data.replynum ?: data.commentnum ?: "0",
            forwardCount = data.forwardnum ?: "0",
            isLiked = data.userAction?.like == 1,
            onLikeClick = onLikeClick?.let { { it(data) } },
            onReplyClick = { onOpenItem(data) },
            onForwardClick = { onOpenItem(data) },
        )
    }
}

/** 图集/合集条目（item_collection_list_item.xml 的简化 Compose 形态）。 */
@Composable
private fun CollectionCard(
    data: HomeFeedResponse.Data,
    onShowCollection: (id: String, title: String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        insideMargin = PaddingValues(12.dp),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = {
            val id = data.id
            if (!id.isNullOrEmpty()) onShowCollection(id, data.title.orEmpty())
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SimpleImage(
                url = data.coverPic ?: data.pic,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(84.dp),
                contentDescription = "图集封面",
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.title.orEmpty(),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val description = data.description
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOfNotNull(
                        if (data.isOpen == 0) "私密" else "公开",
                        data.followNum?.takeIf { it.isNotEmpty() }?.let { "$it 人关注" },
                        data.itemNum?.takeIf { it.isNotEmpty() }?.let { "$it 个内容" },
                    ).joinToString(" · "),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

/** 图片轮播卡（imageCarouselCard 等带 entities 的卡片）：HorizontalPager 画廊 + 页码 + 标题。 */
@Composable
private fun ImageCarouselCard(
    data: HomeFeedResponse.Data,
    onOpenItem: (HomeFeedResponse.Data) -> Unit,
    onOpenImage: (urls: List<String>, index: Int) -> Unit,
) {
    val entities = data.entities.orEmpty().filter { it.pic.isNotEmpty() }
    if (entities.isEmpty()) return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        insideMargin = PaddingValues(12.dp),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = { onOpenItem(data) },
    ) {
        val pagerState = rememberPagerState { entities.size }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) { page ->
            SimpleImage(
                url = entities[page].pic,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onOpenImage(entities.map { it.pic }, page) },
                contentDescription = entities[page].title,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entities[pagerState.currentPage].title,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${pagerState.currentPage + 1}/${entities.size}",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/** 条目图片地址集合：picArr 优先，其次单图/封面/商品图（覆盖 feed/topic/product/card/pear_goods）。 */
private fun resolveImages(data: HomeFeedResponse.Data): List<String> {
    if (!data.picArr.isNullOrEmpty()) return data.picArr!!
    return listOfNotNull(
        data.pic?.takeIf { it.isNotEmpty() },
        data.coverPic?.takeIf { it.isNotEmpty() },
        data.goodsPic?.takeIf { it.isNotEmpty() },
        data.extraPic?.takeIf { it.isNotEmpty() },
    ).distinct()
}
