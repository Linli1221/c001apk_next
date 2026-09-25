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
import androidx.compose.runtime.livedata.observeAsState
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
 * 鏀惰棌澶癸紙澶氭敹钘忓す锛夐〉闈?鈥斺€?瀵瑰簲鑰佺殑 [CollectionActivity] / [CollectionFragment] /
 * [CollectionContentFragment]锛坄/v6/collection/list` 鏀惰棌澶瑰垎缁勫垪琛?+ `/v6/collection/itemList`
 * 鍗曚釜鏀惰棌澶瑰唴瀹逛袱灞傦級銆? *
 * 鏁版嵁娴佸畬鍏ㄥ鐢?[CollectionContentViewModel]锛圠iveData 鐢?observeAsState 妗ユ帴锛夛紝鏈枃浠跺彧璐熻矗鐣岄潰涓庝氦浜掞細
 * - 绗竴灞傘€屾垜鐨勬敹钘忓崟銆嶏細鏀惰棌澶瑰崱鐗囧垪琛紙灏侀潰 / 鏍囬 / 绠€浠?/ 鍏紑绉佸瘑 / 鍏虫敞鏁?/ 鍐呭鏁帮級锛? *   鐐瑰嚮杩涘叆绗簩灞傦紱
 * - 绗簩灞傘€屽叿浣撴敹钘忓す銆嶏細鍐呭鍒楄〃 + 鍙充笂瑙掔鐞嗚彍鍗曪紙缂栬緫鏀惰棌澶逛俊鎭?/ 娓呴櫎鏃犳晥鍐呭 / 鍒犻櫎鏀惰棌澶癸紝
 *   鍚庝袱椤圭敤 Miuix [OverlayDialog] 浜屾纭锛岀鐞嗗姩浣滆蛋 [CollectionPickViewModel]锛屼笌鑰? *   [CollectionFragment] 鐨勮彍鍗曚竴鑷达級锛? * - 涓ゅ眰瀵艰埅鐢?[CollectionScreen] 鍐呴儴鐘舵€佹壙杞斤紙杩斿洖閿€愬眰閫€鍑猴級锛屼篃鍙互鍗曠嫭浣跨敤
 *   [CollectionFolderListScreen] / [CollectionFolderContentScreen] 璧板閮ㄥ鑸洖璋冦€? *
 * 娉ㄦ剰锛? * - 涓嶈嚜甯?MiuixAppTheme锛堟牴涓婚鐢?Activity 鎺ョ嚎鏃跺锛夛紱椤甸潰鑷惈 Scaffold锛圤verlayDialog 闇€瑕?Scaffold 绁栧厛锛夈€? * - [CollectionContentViewModel] 鏄?Hilt assisted ViewModel锛? *   [CollectionScreen.listViewModel] 闇€鐢?`url = "/v6/collection/list", id = null` 鍒涘缓锛? *   [CollectionScreen.contentViewModelFactory] 浼犲叆 `{ id -> factory.create("/v6/collection/itemList", id) }`銆? * - 鍒楄〃鍔ㄦ€侊紙entityType = "feed"锛夐粯璁ゆ覆鏌?[DefaultFeedItem] 绠€鍖栧崱鐗囧崰浣嶏紝
 *   鍚庣画鐢?ui/feed 缁勯€氳繃 [feedItem] 鎻掓Ы鏇挎崲涓哄畬鏁村姩鎬佸崱鐗囥€? * - 鍥剧墖娌跨敤 Glide 閾捐矾锛圛mageUtil.showIMG + AndroidView 鍖?ImageView锛夛紝鏈紩鍏ユ柊鍥剧墖搴撱€? */
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
    // 褰撳墠鎵撳紑鐨勬敹钘忓す锛坕d 鈫?title锛夛紱null = 鍋滅暀鍦ㄧ涓€灞?    var openFolder by remember { mutableStateOf<Pair<String, String>?>(null) }

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
        // 姣忎釜鏀惰棌澶逛竴涓?VM锛涚寮€绗簩灞傚嵆涓㈠純锛岄噸鏂拌繘鍏ユ寜 id 閲嶅缓
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
 * 绗竴灞傦細鎴戠殑鏀惰棌鍗曪紙鏀惰棌澶瑰垎缁勫垪琛級銆? * 瀵瑰簲鑰?[CollectionFragment]锛坕d 涓虹┖鐨勬牴椤甸潰锛屾爣棰樸€屾垜鐨勬敹钘忓崟銆嶏級銆? */
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
                title = "鎴戠殑鏀惰棌鍗?,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "杩斿洖")
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
 * 绗簩灞傦細鍗曚釜鏀惰棌澶瑰唴瀹瑰垪琛?+ 鍙充笂瑙掔鐞嗚彍鍗曘€? * 瀵瑰簲鑰?[CollectionFragment]锛坕d 闈炵┖锛? [CollectionContentFragment]锛坄/v6/collection/itemList`锛夈€? *
 * @param title 鏀惰棌澶规爣棰橈紙鑰佷唬鐮侀€氳繃 arguments 浼犲叆锛孷M 涓嶄繚瀛橈級銆? * @param manageViewModel 绠＄悊鍔ㄤ綔锛堟竻闄ゆ棤鏁堝唴瀹?/ 鍒犻櫎鏀惰棌澶癸級鐨?VM锛涗负 null 鏃堕殣钘忚繖涓ら」銆? * @param onEditCollectionInfo 銆岀紪杈戞敹钘忓す淇℃伅銆嶅洖璋冿紙鑰佷唬鐮佹槸鏍囬 / 绠€浠?/ 鍏紑绉佸瘑 / 灏侀潰琛ㄥ崟瀵硅瘽妗嗭紝
 *   鍚浘鐗囬€夋嫨鍣紝鐣欑粰鎺ョ嚎鏂规垨鍚庣画杞疄鐜帮級銆? */
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

    // 绠＄悊鍔ㄤ綔鐨?toast / 鍒犻櫎鎴愬姛鍚庨€€鍑猴紙鑰?CollectionFragment 鐨?deleted 鈫?popBackStack锛?    if (manageViewModel != null) {
        ManageEvents(
            viewModel = manageViewModel,
            onToast = onToast,
            onDeleted = onBack,
        )
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = title.ifEmpty { "鎴戠殑鏀惰棌鍗? },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "杩斿洖")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(MiuixIcons.More, contentDescription = "鏇村")
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

            // 绠＄悊鑿滃崟锛堣€?collection_menu锛氱紪杈戞敹钘忓す淇℃伅 / 娓呴櫎鏃犳晥鍐呭 / 鍒犻櫎鏀惰棌澶癸級
            OverlayDialog(
                title = title.ifEmpty { "缂栬緫鏀惰棌澶? },
                show = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = "缂栬緫鏀惰棌澶逛俊鎭?,
                        onClick = {
                            showMenu = false
                            onEditCollectionInfo(folderId, title)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (manageViewModel != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            text = "娓呴櫎鏃犳晥鍐呭",
                            onClick = {
                                showMenu = false
                                showClearConfirm = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            text = "鍒犻櫎鏀惰棌澶?,
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

            // 娓呴櫎鏃犳晥鍐呭纭
            OverlayDialog(
                title = "娓呴櫎鏃犳晥鍐呭",
                summary = "灏嗘竻闄よ鏀惰棌澶瑰唴宸插け鏁堢殑鍐呭锛岀害 5 鍒嗛挓鍚庣敓鏁堬紝纭畾缁х画锛?,
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

            // 鍒犻櫎鏀惰棌澶圭‘璁?            OverlayDialog(
                title = "鍒犻櫎鏀惰棌澶?,
                summary = "鍒犻櫎鍚庝笉鍙仮澶嶏紝纭畾鍒犻櫎銆?{title}銆嶏紵",
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
 * 鍒楄〃涓讳綋锛堜袱灞傚叡鐢級锛氫笅鎷夊埛鏂?+ 鍔犺浇鏇村 + 鍔犺浇 / 绌?/ 閿欒鎬併€? * 鐘舵€佹満涓庤€?[com.example.c001apk.ui.base.BaseAppFragment] 涓€鑷达紝鍙槸鎶?View 鐘舵€佹崲鎴愪簡 Compose 鐘舵€併€? */
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

    // 鑰?BaseAppFragment锛歞ataList 鍙樺寲鏃惰褰?listSize锛屼緵 VM 鍒ゆ柇棣栧睆 / 鍔犺浇鏇村璧板摢涓姸鎬侀€氶亾
    LaunchedEffect(list) {
        viewModel.listSize = list.size
    }

    // 棣栨杩涘叆鎷夊彇锛堣€?initData 鈫?refreshData锛夛紱VM 宸叉湁鏁版嵁鎴栧凡鍙戣捣杩囪姹傚垯涓嶉噸澶嶆媺鍙?    LaunchedEffect(Unit) {
        if (viewModel.dataList.value.isNullOrEmpty() && viewModel.loadingState.value == null) {
            refreshCollectionList(viewModel)
        }
    }

    // 浠讳竴鐘舵€佽惤鍦板嵆鏀惰捣涓嬫媺鍒锋柊鎸囩ず锛堣€?observer 閲岀殑 swipeRefresh.isRefreshing = false锛?    LaunchedEffect(loading, footer) {
        refreshing = false
    }

    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let { onToast(it) }
    }

    // 婊氬埌搴曡嚜鍔ㄥ姞杞芥洿澶氾紙鑰?RecyclerView.OnScrollListener 鐨?SCROLL_STATE_IDLE 鍒ゆ柇锛?    LaunchedEffect(listState) {
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

/** 鍒楄〃搴曢儴鍔犺浇鎬侊紙瀵瑰簲鑰?FooterAdapter锛?*/
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

/** 鍒楄〃搴曢儴鎻愮ず鏂囧瓧锛堝銆屾病鏈夋洿澶氫簡銆嶏級 */
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

/** 瀵瑰簲鑰?BaseViewFragment.refreshData()锛氶噸缃垎椤靛苟鎷夐椤?*/
private fun refreshCollectionList(viewModel: CollectionContentViewModel) {
    viewModel.lastItem = null
    viewModel.page = 1
    viewModel.isEnd = false
    viewModel.isRefreshing = true
    viewModel.isLoadMore = false
    viewModel.fetchData()
}

/** 绠＄悊鍔ㄤ綔浜嬩欢妗ユ帴锛坱oast + 鍒犻櫎鎴愬姛閫€鍑猴級锛屼粎 manageViewModel 闈炵┖鏃剁粍鍚?*/
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
 * 鏀惰棌澶瑰崱鐗囷紙瀵瑰簲 item_collection_list_item.xml锛夛細
 * 灏侀潰锛?:4锛? 鏍囬 / 绠€浠?/ 鍏紑绉佸瘑 / 鍏虫敞鏁?/ 鍐呭鏁般€? */
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
                        append(if (item.isOpen == 0) "绉佸瘑" else "鍏紑")
                        append(" 路 ")
                        append(item.followNum ?: "0")
                        append("浜哄叧娉?)
                        append(" 路 ")
                        append(item.itemNum ?: "0")
                        append("涓唴瀹?)
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
 * 鏀惰棌鍐呭閲岀殑鍔ㄦ€佸崱鐗囧崰浣嶏紙entityType = "feed"锛夈€? * 浠呰鐩栥€屽ご鍍?/ 鏄电О / 姝ｆ枃 / 鏃堕棿 / 杞瘎璧炴暟 + 鐐瑰嚮杩涜鎯呫€嶇殑鏈€灏忛棴鐜紝
 * 瀹屾暣鍔ㄦ€佸崱鐗囷紙鍥剧墖涔濆鏍?/ 鎶曠エ / 杞彂閾剧瓑锛夌敱 ui/feed 缁勯€氳繃 feedItem 鎻掓Ы鏇挎崲銆? */
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
                            append(" 路 ")
                        }
                        append("璧?${item.likenum ?: "0"}")
                        append(" 路 璇勮 ${item.replynum ?: item.commentnum ?: "0"}")
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
 * Glide 鍥剧墖锛圛mageUtil.showIMG锛? AndroidView 鍖?ImageView銆? * 鏈紩鍏ユ柊鍥剧墖搴擄紙CONVENTIONS 搂4锛夛紱鍏叡鍥剧墖缁勪欢鏀舵暃鍚庡彲鏇挎崲銆? */
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
            // url 鍙樺寲鎵嶉噸鏂拌蛋 Glide锛岄伩鍏嶆瘡娆￠噸缁勯兘鍙戣捣鍔犺浇
            if (imageView.tag != url) {
                imageView.tag = url
                ImageUtil.showIMG(imageView, url)
            }
        },
        modifier = modifier,
    )
}
