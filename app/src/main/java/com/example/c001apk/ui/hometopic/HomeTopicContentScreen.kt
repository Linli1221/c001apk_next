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
import androidx.compose.runtime.livedata.observeAsState
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.constant.Constants.LOADING_EMPTY
import com.example.c001apk.logic.model.HomeFeedResponse
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 璇濋/鑺傜偣鍐呭娴佸垪琛ㄩ〉锛堥〉闈㈢骇 composable锛屽唴瀹瑰尯鑷惈 Scaffold 涔嬪锛屽彲鍗曠嫭浣跨敤鎴栧祵鍏ワ級銆? *
 * 瀵瑰簲鑰佸疄鐜帮細[HomeTopicContentFragment] + [com.example.c001apk.ui.base.BaseAppFragment] +
 * [com.example.c001apk.ui.base.BaseViewFragment]锛圫wipeRefreshLayout + RecyclerView + Footer锛夈€? * 鏁版嵁娴佸畬鍏ㄥ鐢?[HomeTopicContentViewModel]锛圠iveData 鐢?observeAsState 妗ユ帴锛夛細
 *  - 鍒濆/閲嶈瘯锛氱疆 `loadingState = Loading` 鈫?鏈〉鍓綔鐢ㄦ寜鑰侀€昏緫 [refreshContent]锛? *  - 涓嬫媺鍒锋柊锛歔PullToRefresh] `isRefreshing` 鎻愬崌锛宍onRefresh` 鍚屾缃?true 骞跺埛鏂帮紱
 *  - 涓婃媺鍔犺浇锛氭粴鍒?footer 涓?`!isEnd && !isLoadMore && !isRefreshing` 鏃?loadMore锛? *  - footer锛歔FooterState] 椹卞姩 鍔犺浇涓?娌℃湁鏇村浜?鍔犺浇澶辫触閲嶈瘯銆? *
 * 鑰侀€昏緫閲?`viewModel.listSize` 鐢?Fragment 鐨?dataList observer 缁存姢锛屾湰椤靛悓鏍峰湪 dataList
 * 鍙樺寲鏃跺洖鍐欙紝淇濊瘉 [HomeTopicContentViewModel.fetchData] 鐨?listSize 鍒嗘敮璇箟涓?View 鏃朵唬涓€鑷淬€? */
@Composable
fun HomeTopicContentScreen(
    viewModel: HomeTopicContentViewModel,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onTopicClick: (type: String?, title: String?, url: String?, id: String?) -> Unit = { _, _, _, _ -> },
    onListScrolled: (dy: Int) -> Unit = {},
) {
    // observeAsState 鐨勮繑鍥炲彲绌烘€ч殢 lifecycle 鐗堟湰鐣ユ湁宸紓锛岃繖閲岀粺涓€鎸夊彲绌鸿鍙栧悗鍏滃簳
    val dataList: List<HomeFeedResponse.Data> =
        viewModel.dataList.observeAsState(emptyList()).value ?: emptyList()
    val loadingState by viewModel.loadingState.observeAsState()
    val footerState by viewModel.footerState.observeAsState()

    // 涓?BaseAppFragment.initObserve 鐨?viewModel.listSize = it.size 瀵归綈
    LaunchedEffect(dataList) {
        viewModel.listSize = dataList.size
    }

    // 棣栨缁勫悎锛氫笌 BaseViewFragment.onResume 鐨?initData() 瀵归綈锛岀疆 Loading 瑙﹀彂棣栧埛
    LaunchedEffect(Unit) {
        if (viewModel.isInit) {
            viewModel.isInit = false
            viewModel.loadingState.value = LoadingState.Loading
        }
    }

    // 涓?BaseViewFragment.initObserve 瀵归綈锛歀oading 鈫?refreshData()锛坙oadMore 瑙﹀彂鐨?Loading 涓嶉噸鍒凤級
    LaunchedEffect(loadingState) {
        if (loadingState is LoadingState.Loading && !viewModel.isLoadMore) {
            refreshContent(viewModel)
        }
    }

    // 涓嬫媺鍒锋柊鎸囩ず鍣細浠绘剰缁撴灉鐘舵€侊紙footer/涓荤姸鎬佸彉鍖栵級鍒拌揪鍗崇粨鏉?    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(footerState, loadingState) {
        if (footerState !is FooterState.Loading) {
            isRefreshing = false
        }
    }

    // 涓婃媺鍔犺浇锛氭粴鍒板垪琛ㄦ湯灏撅紙footer 鍙锛変笖鏃犺繘琛屼腑鐨勮姹傛椂瑙﹀彂锛屽榻?BaseViewFragment.initScroll
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

    // 婊氬姩鏂瑰悜鍥炶皟锛堣€佸疄鐜伴噷鐢ㄤ簬鏄剧ず/闅愯棌搴曢儴瀵艰埅鏍忥級
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
            is LoadingState.LoadingError -> HomeTopicErrorView(
                message = state.errMsg,
                retryText = "閲嶈瘯",
                onRetry = { viewModel.loadingState.value = LoadingState.Loading },
                modifier = Modifier.align(Alignment.Center)
            )

            is LoadingState.LoadingFailed -> HomeTopicErrorView(
                message = state.msg,
                // 绌烘€佺敤銆屽埛鏂般€嶏紝澶辫触鐢ㄣ€岄噸璇曘€嶏紝涓庤€佸疄鐜颁竴鑷?                retryText = if (state.msg == LOADING_EMPTY) "鍒锋柊" else "閲嶈瘯",
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
                                    // 涓?BaseAppFragment.ReloadListener.onReLoad 瀵归綈
                                    viewModel.isEnd = false
                                    viewModel.isLoadMore = true
                                    viewModel.fetchData()
                                },
                            )
                        }
                    }
                }
            }

            // null锛堢姸鎬佸皻鏈骇鐢燂級涓庢湭鐭ョ姸鎬佸厹搴曚负鍔犺浇涓?            else -> HomeTopicLoadingView(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 鐘舵€佽鍥撅紙鑷惈瀹炵幇锛泆i/common 鏀舵暃鍚庡彲鏇挎崲涓哄叕鍏辩粍浠讹級
// ---------------------------------------------------------------------------

/** 鍔犺浇涓細瀵瑰簲鑰?`item_indicator`锛堜笉纭畾杩涘害鎸囩ず鍣級銆?*/
@Composable
internal fun HomeTopicLoadingView(modifier: Modifier = Modifier) {
    InfiniteProgressIndicator(modifier = modifier)
}

/** 閿欒/绌烘€侊細瀵瑰簲鑰?`item_error_layout` / `item_error_message`锛堟秷鎭?+ 閲嶈瘯鎸夐挳锛夈€?*/
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

/** 鍒楄〃 footer锛氬搴旇€?`FooterAdapter`锛堝姞杞戒腑 / 娌℃湁鏇村浜?/ 鍑洪敊閲嶈瘯锛夈€?*/
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
                    text = "鍔犺浇涓€?,
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
                TextButton(text = "閲嶈瘯", onClick = onRetry)
            }

            else -> {}
        }
    }
}

// ---------------------------------------------------------------------------
// 鏁版嵁娴佽緟鍔╋紙鍙搷浣滅幇鏈?ViewModel 鐨勫叕寮€瀛楁锛屼笉鏀瑰啓 ViewModel锛?// ---------------------------------------------------------------------------

/** 瀵瑰簲 BaseViewFragment.refreshData()锛氶噸缃垎椤垫父鏍囧悗閲嶆柊鎷夌涓€椤点€?*/
private fun refreshContent(viewModel: HomeTopicContentViewModel) {
    viewModel.lastItem = null
    viewModel.page = 1
    viewModel.isEnd = false
    viewModel.isRefreshing = true
    viewModel.isLoadMore = false
    viewModel.fetchData()
}
