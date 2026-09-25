package com.example.c001apk.ui.coolpic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.ui.collection.CollectionContentViewModel
import com.example.c001apk.ui.common.TopBarScaffold
import com.example.c001apk.ui.carousel.FeedListPage
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.TabRow

/**
 * 冷图页（CoolPicActivity + CoolPicFragment + BasePagerFragment 的 Compose 形态）。
 *
 * 结构与老代码一致：顶栏（标题 = 图集 tag）+ 标签「精选 / 热门 / 最新」+ 每个标签一页图片列表。
 * 每页复用现有 [CollectionContentViewModel]（assisted 工厂注入 url = `/v6/picture/list?tag=<title>&type=<type>`），
 * 列表渲染/刷新/翻页统一走 [FeedListPage]。标签分页用 androidx `HorizontalPager`
 * （老 ViewPager2 + FragmentStateAdapter 的对应物）。
 *
 * **参数**
 * @param title 图集 tag（旧 Intent extra `"title"`，URLDecoder 解码后的值）。
 * @param viewModelFactory [CollectionContentViewModel] 的 assisted 工厂（Hilt 注入后传入；
 *   本 Screen 只按 tab 创建现有 VM 实例，不写业务逻辑）。
 * @param modifier 应用于页面的 [Modifier]。
 * @param onBack 返回回调。
 * @param onOpenItem 点击条目（跳动态详情，由宿主接线）。
 * @param onOpenImage 点击大图（默认列表内已走 Mojito 预览，通常不用传）。
 * @param onOpenUser 点击头像/昵称（跳用户主页）。
 * @param onShowCollection 点击图集条目（跳合集页，对应旧 `showCollection` 事件）。
 * @param onLikeClick 点赞回调（走 VM `ItemListener.onLikeClick`，由宿主接线）。
 *
 * **祖先要求**：必须位于根主题 `MiuixAppTheme` 之内；本组件自带 TopBarScaffold（Miuix Scaffold）。
 */
@Composable
fun CoolPicScreen(
    title: String,
    viewModelFactory: CollectionContentViewModel.Factory,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onOpenItem: (HomeFeedResponse.Data) -> Unit = {},
    onOpenImage: (urls: List<String>, index: Int) -> Unit = { _, _ -> },
    onOpenUser: (uid: String?, username: String?) -> Unit = { _, _ -> },
    onShowCollection: (id: String, title: String) -> Unit = { _, _ -> },
    onLikeClick: ((HomeFeedResponse.Data) -> Unit)? = null,
) {
    // 旧 CoolPicFragment：typeList = recommend / hot / newest，tabList = 精选 / 热门 / 最新
    val tabs = listOf("精选", "热门", "最新")
    val types = listOf("recommend", "hot", "newest")

    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    TopBarScaffold(
        title = title,
        onBack = onBack,
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
        ) {
            TabRow(
                tabs = tabs,
                selectedTabIndex = pagerState.currentPage,
                onTabSelected = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val url = "/v6/picture/list?tag=$title&type=${types[page]}"
                // 每个 tab 一个现有 CollectionContentViewModel 实例（key 隔离，随宿主 ViewModelStore 存活）
                val tabViewModel: CollectionContentViewModel = viewModel(
                    key = "coolpic-$url",
                    factory = CollectionContentViewModel.provideFactory(
                        viewModelFactory, url, null,
                    ),
                )
                FeedListPage(
                    viewModel = tabViewModel,
                    contentPadding = PaddingValues(vertical = 4.dp),
                    onOpenItem = onOpenItem,
                    onOpenImage = onOpenImage,
                    onOpenUser = onOpenUser,
                    onShowCollection = onShowCollection,
                    onLikeClick = onLikeClick,
                )
            }
        }
    }
}
