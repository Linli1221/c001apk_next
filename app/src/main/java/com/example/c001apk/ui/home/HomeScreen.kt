package com.example.c001apk.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.observeAsState
import com.example.c001apk.R
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Search

/**
 * 首页骨架（迁移自 HomeFragment + fragment_home.xml）：
 * 顶部 TabRow（tab 列表来自 HomeViewModel.tabListLiveData，数据库为空时回落默认 tab）
 * + 搜索按钮（SearchActivity）+ 编辑 Tab 按钮（CopyActivity("homeMenu")），
 * 内容区是插槽，由宿主按 tab 标题决定放哪个页面（关注/头条/热榜/酷图 → HomeFeedScreen，
 * 应用 → AppList、话题/数码 → HomeTopic，均为其它迁移组的产出）。
 *
 * 状态复用现有 [HomeViewModel]（LiveData 用 observeAsState 桥接），不在本文件写业务逻辑。
 *
 * @param viewModel 复用现有 HomeViewModel（tab 数据源、position）
 * @param onSearchClick 搜索按钮点击（原：跳 SearchActivity，接线轮由宿主实现）
 * @param onEditTabsClick 编辑 Tab 按钮点击（原：跳 CopyActivity("homeMenu")）
 * @param onTabReselected tab 再次点击（原「关注」弹分组选择、其余回顶+刷新并显示底部导航）
 * @param tabContent 内容区插槽：(当前 tab 标题, tab 再选计数器)；计数器每次 tab 重选 +1，
 * 便于子页面（如 HomeFeedScreen）响应「回到顶部/弹关注分组」
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSearchClick: () -> Unit,
    onEditTabsClick: () -> Unit,
    onTabReselected: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    tabContent: @Composable (tabTitle: String, tabReselectedTick: Int) -> Unit,
) {
    val tabList by viewModel.tabListLiveData.observeAsState()
    var tabReselectedTick by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // 与旧 HomeFragment 观察者一致：数据库空 → 写默认 tab；全部被禁用 → 恢复默认
    LaunchedEffect(tabList) {
        val list = tabList.orEmpty()
        when {
            list.isEmpty() -> viewModel.initTab()
            list.none { it.isEnable } -> viewModel.updateTab(viewModel.defaultList)
        }
    }

    val enableList = remember(tabList) {
        val titles = tabList?.filter { it.isEnable }?.map { it.title }.orEmpty()
        if (titles.isEmpty()) viewModel.defaultList.filter { it.isEnable }.map { it.title }
        else titles
    }

    // 首次进入默认落在「头条」（对应旧 viewModel.isInit 分支）
    val currentIndex: Int = when {
        selectedIndex in enableList.indices -> selectedIndex
        enableList.isEmpty() -> 0
        else -> enableList.indexOfFirst { it == "头条" }.coerceAtLeast(0)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (enableList.isNotEmpty()) {
                        TabRow(
                            tabs = enableList,
                            selectedTabIndex = currentIndex,
                            onTabSelected = { index ->
                                if (index == currentIndex) {
                                    tabReselectedTick++
                                    enableList.getOrNull(index)?.let { onTabReselected(it) }
                                } else {
                                    selectedIndex = index
                                    viewModel.position = index
                                }
                            },
                            modifier = Modifier.weight(1f),
                            listState = rememberLazyListState(),
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = MiuixIcons.Search,
                            contentDescription = stringResource(R.string.search),
                        )
                    }
                    IconButton(onClick = onEditTabsClick) {
                        Icon(
                            imageVector = MiuixIcons.Edit,
                            contentDescription = stringResource(R.string.edit_tab),
                        )
                    }
                }
                HorizontalDivider()
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (enableList.isNotEmpty()) {
                tabContent(enableList[currentIndex], tabReselectedTick)
            }
        }
    }
}
