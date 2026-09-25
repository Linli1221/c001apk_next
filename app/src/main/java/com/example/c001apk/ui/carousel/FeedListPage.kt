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
import androidx.compose.runtime.livedata.observeAsState
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
 * 閫氱敤銆宖eed 鍒楄〃椤点€嶏細SwipeRefreshLayout + RecyclerView锛圔aseAppFragment锛夌殑 Compose 瀵瑰簲鐗┿€? *
 * 鍐峰浘锛圕oolPic锛夈€佽疆鎾?娲诲姩锛圕arousel锛変袱缁勯〉闈㈢殑鍒楄〃鍐呭閮界敱 [BaseAppViewModel] 绯? * 锛圼com.example.c001apk.ui.collection.CollectionContentViewModel] / [CarouselViewModel]锛夐┍鍔紝
 * 鍥犳杩欓噷缁熶竴妗ユ帴锛歚dataList` / `loadingState` / `footerState` / `toastText` 鐢?observeAsState
 * 鍙栫姸鎬侊紝鍒锋柊/缈婚〉鐩存帴璋冪敤 ViewModel 鐜版湁鏂规硶锛堝鍒?BaseViewFragment 鐨?refreshData/loadMore 璇箟锛夈€? *
 * **鏁版嵁娴侊紙涓庤€佷唬鐮佸榻愶級**
 * - 棣栨缁勫悎锛歚loadingState = Loading` 鍚?`refreshFromStart()`锛? 鏃?`loadingState=Loading 鈫?refreshData()`锛夈€? * - 涓嬫媺鍒锋柊锛歔PullToRefresh] 鍖?LazyColumn锛宍onRefresh` 璧?`refreshFromStart()`銆? * - 瑙﹀簳缈婚〉锛氭粴鍔ㄥ仠姝紙绛変环鏃?SCROLL_STATE_IDLE锛変笖鏈€鍚庝竴椤瑰彲瑙佹椂 `loadMore()`锛? *   搴曢儴 footer 鍑洪敊鏃朵篃鍙偣鍑婚噸璇曘€? * - `listSize` 鍦?dataList 鍙樺寲鏃跺洖鍐?ViewModel锛堟棫 BaseAppFragment 鍚屾锛孷M 鍒嗛〉鍒ゆ柇渚濊禆瀹冿級銆? *
 * **鍥剧墖**锛氭部鐢?ui/common 鐨?[SimpleImage] / [NineGrid]锛圓ndroidView + 鐜版湁 Glide 閾捐矾锛? * 涔濆鏍艰嚜甯?Mojito 澶у浘棰勮锛夛紝涓嶅紩鍏ユ柊鍥剧墖搴撱€? *
 * **鍙傛暟**
 * @param viewModel 鍒楄〃 ViewModel锛圔aseAppViewModel 瀛愮被锛夛紝鐘舵€佸叏閮ㄦ潵鑷畠锛屼笉鍦ㄦ湰缁勪欢鍐呭垱寤轰笟鍔￠€昏緫銆? * @param modifier 搴旂敤浜庢渶澶栧眰瀹瑰櫒鐨?[Modifier]銆? * @param contentPadding LazyColumn 鐨勫唴瀹瑰唴杈硅窛锛堥《鏍?鏍囩鏍忓崰浣嶇敱璋冪敤鏂规姌绠楋級銆? * @param onOpenItem 鐐瑰嚮鏉＄洰鍥炶皟锛堣烦鍔ㄦ€佽鎯呯瓑锛岀敱瀹夸富鎺ョ嚎锛夈€? * @param onOpenImage 鐐瑰嚮澶у浘鍥炶皟锛堥粯璁よ蛋 NineGrid 鍐呴儴 Mojito 棰勮锛岄€氬父涓嶇敤浼狅級銆? * @param onOpenUser 鐐瑰嚮澶村儚/鏄电О鍥炶皟锛堣烦鐢ㄦ埛涓婚〉锛夈€? * @param onShowCollection 鐐瑰嚮銆屽浘闆?鍚堥泦銆嶆潯鐩洖璋冿紙鏃?`showCollection` 浜嬩欢锛夈€? * @param onLikeClick 鐐硅禐鍥炶皟锛沶ull 鏃剁偣璧為」涓嶅彲鐐瑰嚮锛堢偣璧炶蛋 VM `ItemListener.onLikeClick`锛岀敱瀹夸富鎺ョ嚎锛夈€? *
 * **绁栧厛瑕佹眰**锛氬繀椤讳綅浜庢牴涓婚 `MiuixAppTheme` 涔嬪唴銆傛湰缁勪欢涓嶅惈 Scaffold/TopAppBar锛岀敱椤甸潰 Screen 鎻愪緵銆? */
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

    // 涓庢棫 BaseAppFragment 涓€鑷达細dataList 鍙樺寲鏃跺洖鍐?listSize锛圴M 鐨勫垎椤?绌哄垪琛ㄥ垽鏂緷璧栧畠锛?    LaunchedEffect(dataList) { viewModel.listSize = dataList.size }

    // toastText 涓€娆℃€т簨浠?鈫?Toast
    LaunchedEffect(toast) {
        toast?.getContentIfNotHandledOrReturnNull()?.let { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 澶嶅埢 BaseViewFragment.refreshData()锛氫粠澶村姞杞?    fun refreshFromStart() {
        viewModel.loadingState.value = LoadingState.Loading
        viewModel.lastItem = null
        viewModel.page = 1
        viewModel.isEnd = false
        viewModel.isRefreshing = true
        viewModel.isLoadMore = false
        viewModel.fetchData()
    }

    // 澶嶅埢 BaseViewFragment.loadMore()
    fun loadMore() {
        if (viewModel.isEnd || viewModel.isRefreshing || viewModel.isLoadMore) return
        viewModel.isLoadMore = true
        viewModel.fetchData()
    }

    // 棣栨缁勫悎鑷姩鍔犺浇锛堢瓑浠锋棫銆宭oadingState=Loading 鈫?refreshData()銆嶉摼璺級锛?    // 椤甸潰鍦?pager 閲岃鍥炴敹閲嶅缓鏃跺凡鏈夋暟鎹?姝ｅ湪鍔犺浇鍒欎笉閲嶅鎷?    LaunchedEffect(Unit) {
        if (viewModel.dataList.value.isNullOrEmpty() &&
            viewModel.loadingState.value !is LoadingState.Loading &&
            !viewModel.isRefreshing && !viewModel.isLoadMore
        ) {
            refreshFromStart()
        }
    }

    // 涓嬫媺鍒锋柊鎸囩ず鍣?    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) return@LaunchedEffect
        // 鑰?VM 鍦?fetch 鏀舵暃鏃舵妸 isRefreshing/isLoadMore 缃洖 false锛涗釜鍒棭閫€鍒嗘敮涓嶅浣嶏紝
        // 鎵€浠ュ啀鍏滀竴涓秴鏃讹紝閬垮厤鎸囩ず鍣ㄥ崱浣?        val start = System.currentTimeMillis()
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

    // 瑙﹀簳缈婚〉锛氱瓑浠锋棫 RecyclerView.SCROLL_STATE_IDLE + 鏈€鍚庝竴椤瑰彲瑙?    val listState = rememberLazyListState()
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
                    retryText = "閲嶈瘯",
                )

                is LoadingState.LoadingFailed -> {
                    if (state.msg == LOADING_EMPTY) {
                        EmptyState(
                            modifier = Modifier.fillMaxSize(),
                            text = state.msg,
                            actionText = "鍒锋柊",
                            onAction = { refreshFromStart() },
                        )
                    } else {
                        ErrorState(
                            onRetry = { refreshFromStart() },
                            modifier = Modifier.fillMaxSize(),
                            message = state.msg,
                            retryText = "閲嶈瘯",
                        )
                    }
                }

                LoadingState.LoadingDone -> EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    text = LOADING_EMPTY,
                    actionText = "鍒锋柊",
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

/** 鍒楄〃搴曢儴鍔犺浇鏉★細鍔犺浇涓?/ 娌℃湁鏇村浜?/ 鍑洪敊鐐瑰嚮閲嶈瘯锛堝搴旀棫 FooterAdapter锛夈€?*/
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
                text = "${footerState.errMsg}锛岀偣鍑婚噸璇?,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        else -> Spacer(modifier = Modifier.height(4.dp))
    }
}

/** 鎸夊疄浣撶被鍨嬪垎鍙戞潯鐩覆鏌擄紙鏃?AppAdapter 鐨?getItemViewType 瀵瑰簲锛岀畝鍖栦负涓夌被鍗＄墖锛夈€?*/
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

/** 鍔ㄦ€?璇濋/浜у搧/鐢ㄦ埛/鍥炬枃鍗＄墖锛坕tem_home_feed.xml 鐨勭畝鍖?Compose 褰㈡€侊級銆?*/
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
                    contentDescription = "澶村儚",
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
                    ).joinToString(" 路 ")
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
                text = title.orEmpty(),
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        val body = data.message ?: data.description ?: data.goodsPromoTitle
        if (!body.isNullOrBlank()) {
            Text(
                text = body.orEmpty(),
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
            onLikeClick = onLikeClick?.let { handler -> { handler(data) } },
            onReplyClick = { onOpenItem(data) },
            onForwardClick = { onOpenItem(data) },
        )
    }
}

/** 鍥鹃泦/鍚堥泦鏉＄洰锛坕tem_collection_list_item.xml 鐨勭畝鍖?Compose 褰㈡€侊級銆?*/
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
                contentDescription = "鍥鹃泦灏侀潰",
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
                        text = description.orEmpty(),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOfNotNull(
                        if (data.isOpen == 0) "绉佸瘑" else "鍏紑",
                        data.followNum?.takeIf { it.isNotEmpty() }?.let { "$it 浜哄叧娉? },
                        data.itemNum?.takeIf { it.isNotEmpty() }?.let { "$it 涓唴瀹? },
                    ).joinToString(" 路 "),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

/** 鍥剧墖杞挱鍗★紙imageCarouselCard 绛夊甫 entities 鐨勫崱鐗囷級锛欻orizontalPager 鐢诲粖 + 椤电爜 + 鏍囬銆?*/
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

/** 鏉＄洰鍥剧墖鍦板潃闆嗗悎锛歱icArr 浼樺厛锛屽叾娆″崟鍥?灏侀潰/鍟嗗搧鍥撅紙瑕嗙洊 feed/topic/product/card/pear_goods锛夈€?*/
private fun resolveImages(data: HomeFeedResponse.Data): List<String> {
    if (!data.picArr.isNullOrEmpty()) return data.picArr!!
    return listOfNotNull(
        data.pic?.takeIf { it.isNotEmpty() },
        data.coverPic?.takeIf { it.isNotEmpty() },
        data.goodsPic?.takeIf { it.isNotEmpty() },
        data.extraPic?.takeIf { it.isNotEmpty() },
    ).distinct()
}
