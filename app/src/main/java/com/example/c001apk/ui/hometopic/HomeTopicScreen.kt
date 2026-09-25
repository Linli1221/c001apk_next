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
import androidx.lifecycle.compose.observeAsState
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
 * 话题 / 数码（节点）页面：左侧标签栏 + 右侧内容流。
 *
 * 对应老实现：[HomeTopicFragment]（fragment_home_topic.xml：左 BrandLabelAdapter 标签栏 +
 * 右 HomeTopicContentFragment 内容流）。数据流完全复用 [HomeTopicViewModel]（LiveData 用
 * observeAsState 桥接）：
 *  - 首次进入置 `loadingState = Loading`，Loading 副作用按老逻辑抓取标签表
 *   （topic → V11_VERTICAL_TOPIC，product → 产品表）；
 *  - LoadingDone 后渲染标签栏 + 当前标签的内容流；
 *  - LoadingError / LoadingFailed → 错误与空态 + 重试。
 *
 * 每个标签的内容流 ViewModel 通过 [contentViewModelFactory]（由接线层用 Hilt AssistedFactory
 * 提供）创建并按标签缓存，切换标签不丢数据；列表滚动位置用 [LazyListState] 缓存近似还原
 * 老实现「hide/show Fragment 保状态」的行为。
 */
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

    // 各标签的内容流 ViewModel / 列表状态缓存（对应老实现的 childFragmentManager 复用）
    val contentViewModels = remember { mutableMapOf<Int, HomeTopicContentViewModel>() }
    val contentListStates = remember { mutableMapOf<Int, LazyListState>() }
    var selectedIndex by remember { mutableIntStateOf(viewModel.position) }

    // 与 HomeTopicFragment.onResume 对齐：首次进入置 Loading 触发抓取
    LaunchedEffect(Unit) {
        if (viewModel.isInit) {
            viewModel.isInit = false
            viewModel.loadingState.value = LoadingState.Loading
        }
    }

    // 与 HomeTopicFragment.initObserve 的 Loading 分支对齐：按 type 抓取标签表
    LaunchedEffect(loadingState) {
        if (loadingState is LoadingState.Loading) {
            if (viewModel.type == "topic") {
                viewModel.url = "/page?url=V11_VERTICAL_TOPIC"
                viewModel.title = "话题"
                viewModel.fetchTopicList()
            } else {
                viewModel.fetchProductList()
            }
        }
    }

    val pageTitle = title ?: viewModel.title ?: if (viewModel.type == "topic") "话题" else "数码"

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
                                contentDescription = "返回",
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
                null, is LoadingState.Loading -> HomeTopicLoadingView(
                    modifier = Modifier.align(Alignment.Center)
                )

                is LoadingState.LoadingError -> HomeTopicErrorView(
                    message = state.errMsg,
                    retryText = "重试",
                    onRetry = { viewModel.loadingState.value = LoadingState.Loading },
                    modifier = Modifier.align(Alignment.Center)
                )

                is LoadingState.LoadingFailed -> HomeTopicErrorView(
                    message = state.msg,
                    // 空态用「刷新」，失败用「重试」，与老实现一致
                    retryText = if (state.msg == LOADING_EMPTY) "刷新" else "重试",
                    onRetry = { viewModel.loadingState.value = LoadingState.Loading },
                    modifier = Modifier.align(Alignment.Center)
                )

                is LoadingState.LoadingDone -> {
                    val labels = viewModel.tabList
                    val tabs = viewModel.topicList
                    if (labels.isEmpty() || tabs.isEmpty()) {
                        HomeTopicErrorView(
                            message = LOADING_EMPTY,
                            retryText = "刷新",
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
            }
        }
    }
}

/**
 * 左侧标签栏（对应 BrandLabelAdapter + item_brand_label.xml）：
 * 选中项主色文字 + 左侧 3dp 主色指示条 + 浅色底。
 */
@Composable
private fun HomeTopicLabelRail(
    labels: List<String>,
    selectedIndex: Int,
    onLabelClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // 对应老实现 scrollToCenter()：选中后把标签滚到可见位置
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
