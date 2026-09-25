package com.example.c001apk.ui.dyh

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.c001apk.logic.model.HomeFeedResponse
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/**
 * 看看号页（DyhActivity + DyhFragment 的 Compose 版）：
 * 顶栏标题 + 「精选 / 广场」两个 tab，每个 tab 是一份动态列表（[DyhDetailScreen]）。
 *
 * 两个 tab 各有独立的 [DyhViewModel]（id 相同、type 分别为 all / square，与
 * DyhFragment.typeList 一致）。ViewModel 通过 [viewModelFactory] 由接线层创建
 * （Hilt assisted factory：factory.create(id, type)），本文件只做持有与传递。
 */
@Composable
fun DyhScreen(
    id: String,
    title: String,
    viewModelFactory: (id: String, type: String) -> DyhViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onItemClick: (HomeFeedResponse.Data) -> Unit = {},
) {
    val tabs = remember { listOf("精选" to "all", "广场" to "square") }
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            SmallTopAppBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            TabRow(
                tabs = tabs.map { it.first },
                selectedTabIndex = pagerState.currentPage,
                onTabSelected = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                val type = tabs[page].second
                // 每个 tab 一个独立 ViewModel；remember 保证翻页/重组不重建
                val viewModel = remember(type) { viewModelFactory(id, type) }
                DyhDetailScreen(
                    viewModel = viewModel,
                    onItemClick = onItemClick,
                )
            }
        }
    }
}
