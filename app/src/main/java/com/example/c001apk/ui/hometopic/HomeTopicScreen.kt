package com.example.c001apk.ui.hometopic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.livedata.observeAsState
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.constant.Constants.LOADING_EMPTY
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 璇濋 / 鏁扮爜锛堣妭鐐癸級椤甸潰锛氬乏渚ф爣绛炬爮 + 鍙充晶鍐呭娴併€? *
 * 瀵瑰簲鑰佸疄鐜帮細[HomeTopicFragment]锛坒ragment_home_topic.xml锛氬乏 BrandLabelAdapter 鏍囩鏍?+
 * 鍙?HomeTopicContentFragment 鍐呭娴侊級銆傛暟鎹祦瀹屽叏澶嶇敤 [HomeTopicViewModel]锛圠iveData 鐢? * observeAsState 妗ユ帴锛夛細
 *  - 棣栨杩涘叆缃?`loadingState = Loading`锛孡oading 鍓綔鐢ㄦ寜鑰侀€昏緫鎶撳彇鏍囩琛? *   锛坱opic 鈫?V11_VERTICAL_TOPIC锛宲roduct 鈫?浜у搧琛級锛? *  - LoadingDone 鍚庢覆鏌撴爣绛炬爮 + 褰撳墠鏍囩鐨勫唴瀹规祦锛? *  - LoadingError / LoadingFailed 鈫?閿欒涓庣┖鎬?+ 閲嶈瘯銆? *
 * 姣忎釜鏍囩鐨勫唴瀹规祦 ViewModel 閫氳繃 [contentViewModelFactory]锛堢敱鎺ョ嚎灞傜敤 Hilt AssistedFactory
 * 鎻愪緵锛夊垱寤哄苟鎸夋爣绛剧紦瀛橈紝鍒囨崲鏍囩涓嶄涪鏁版嵁锛涘垪琛ㄦ粴鍔ㄤ綅缃敤 [LazyListState] 缂撳瓨杩戜技杩樺師
 * 鑰佸疄鐜般€宧ide/show Fragment 淇濈姸鎬併€嶇殑琛屼负銆? */
@Composable
fun HomeTopicScreen(
    viewModel: HomeTopicViewModel,
    contentViewModelFactory: (url: String, title: String) -> HomeTopicContentViewModel,
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    onTopicClick: (type: String?, title: String?, url: String?, id: String?) -> Unit = { _, _, _, _ -> },
    onListScrolled: (dy: Int) -> Unit = {},
) {
    val loadingState by viewModel.loadingState.observeAsState()

    // 鍚勬爣绛剧殑鍐呭娴?ViewModel / 鍒楄〃鐘舵€佺紦瀛橈紙瀵瑰簲鑰佸疄鐜扮殑 childFragmentManager 澶嶇敤锛?    val contentViewModels = remember { mutableMapOf<Int, HomeTopicContentViewModel>() }
    val contentListStates = remember { mutableMapOf<Int, LazyListState>() }
    var selectedIndex by remember { mutableIntStateOf(viewModel.position) }

    // 涓?HomeTopicFragment.onResume 瀵归綈锛氶娆¤繘鍏ョ疆 Loading 瑙﹀彂鎶撳彇
    LaunchedEffect(Unit) {
        if (viewModel.isInit) {
            viewModel.isInit = false
            viewModel.loadingState.value = LoadingState.Loading
        }
    }

    // 涓?HomeTopicFragment.initObserve 鐨?Loading 鍒嗘敮瀵归綈锛氭寜 type 鎶撳彇鏍囩琛?    LaunchedEffect(loadingState) {
        if (loadingState is LoadingState.Loading) {
            if (viewModel.type == "topic") {
                viewModel.url = "/page?url=V11_VERTICAL_TOPIC"
                viewModel.title = "璇濋"
                viewModel.fetchTopicList()
            } else {
                viewModel.fetchProductList()
            }
        }
    }

    val pageTitle = title ?: viewModel.title ?: if (viewModel.type == "topic") "璇濋" else "鏁扮爜"

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = pageTitle,
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = "杩斿洖",
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = loadingState) {
                is LoadingState.LoadingError -> HomeTopicErrorView(
                    message = state.errMsg,
                    retryText = "閲嶈瘯",
                    onRetry = { viewModel.loadingState.value = LoadingState.Loading },
                    modifier = Modifier.align(Alignment.Center)
                )

                is LoadingState.LoadingFailed -> HomeTopicErrorView(
                    message = state.msg,
                    // 绌烘€佺敤銆屽埛鏂般€嶏紝澶辫触鐢ㄣ€岄噸璇曘€嶏紝涓庤€佸疄鐜颁竴鑷?                    retryText = if (state.msg == LOADING_EMPTY) "鍒锋柊" else "閲嶈瘯",
                    onRetry = { viewModel.loadingState.value = LoadingState.Loading },
                    modifier = Modifier.align(Alignment.Center)
                )

                is LoadingState.LoadingDone -> {
                    val labels = viewModel.tabList
                    val tabs = viewModel.topicList
                    if (labels.isEmpty() || tabs.isEmpty()) {
                        HomeTopicErrorView(
                            message = LOADING_EMPTY,
                            retryText = "鍒锋柊",
                            onRetry = { viewModel.loadingState.value = LoadingState.Loading },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        val safeIndex = selectedIndex.coerceIn(0, tabs.lastIndex)
                        Row(modifier = Modifier.fillMaxSize()) {
                            HomeTopicLabelRail(
                                labels = labels,
                                selectedIndex = safeIndex,
                                onLabelClick = { position ->
                                    selectedIndex = position
                                    viewModel.position = position
                                },
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(0.22f)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(0.78f)
                            ) {
                                val tab = tabs[safeIndex]
                                val contentViewModel = contentViewModels.getOrPut(safeIndex) {
                                    contentViewModelFactory(tab.url, tab.title)
                                }
                                val contentListState = contentListStates.getOrPut(safeIndex) {
                                    LazyListState()
                                }
                                HomeTopicContentScreen(
                                    viewModel = contentViewModel,
                                    listState = contentListState,
                                    onTopicClick = onTopicClick,
                                    onListScrolled = onListScrolled,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Loading 涓?null锛堢姸鎬佸皻鏈骇鐢燂級鍏滃簳涓哄姞杞戒腑
                else -> HomeTopicLoadingView(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

/**
 * 宸︿晶鏍囩鏍忥紙瀵瑰簲 BrandLabelAdapter + item_brand_label.xml锛夛細
 * 閫変腑椤逛富鑹叉枃瀛?+ 宸︿晶 3dp 涓昏壊鎸囩ず鏉?+ 娴呰壊搴曘€? */
@Composable
private fun HomeTopicLabelRail(
    labels: List<String>,
    selectedIndex: Int,
    onLabelClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // 瀵瑰簲鑰佸疄鐜?scrollToCenter()锛氶€変腑鍚庢妸鏍囩婊氬埌鍙浣嶇疆
    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(selectedIndex)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.background(MiuixTheme.colorScheme.background),
    ) {
        itemsIndexed(labels) { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        if (selected) MiuixTheme.colorScheme.surfaceContainer
                        else MiuixTheme.colorScheme.background
                    )
                    .clickable { onLabelClick(index) }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(if (selected) MiuixTheme.colorScheme.primary else Color.Transparent)
                )
                Text(
                    text = label,
                    modifier = Modifier.align(Alignment.Center),
                    style = MiuixTheme.textStyles.body2,
                    color = if (selected) MiuixTheme.colorScheme.primary
                    else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                )
            }
        }
    }
}
