package com.example.c001apk.ui.carousel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.constant.Constants.LOADING_EMPTY
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.ui.common.EmptyState
import com.example.c001apk.ui.common.ErrorState
import com.example.c001apk.ui.common.LoadingState as LoadingPlaceholder
import com.example.c001apk.ui.common.TopBarScaffold
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.TabRow

/**
 * 杞挱/娲诲姩椤碉紙CarouselActivity + CarouselPagerFragment + CarouselFragment 鐨?Compose 褰㈡€侊級銆? *
 * 涓庤€佷唬鐮佸悓鏋勶紝涓ょ骇缁撴瀯锛? * 1. 椤甸潰绾?[CarouselViewModel]锛堟椿鍔?ViewModel锛夊厛 `initCarouselList()`锛? *    - 鏈嶅姟绔笅鍙?`iconTabLinkGridCard` 鈫?`topicList`锛堝鏍囩锛屾瘡鏍囩涓€涓瓙椤碉級锛? *    - 鍚﹀垯鍗曢〉鍒楄〃锛堟暟鎹惤鍦ㄥ悓涓€涓?VM 鐨?`dataList`锛夈€? * 2. 澶氭爣绛撅細鏍囩琛岋紙TabRow锛? `HorizontalPager`锛堣€?ViewPager2 + FragmentStateAdapter 鐨勫搴旂墿锛夛紝
 *    姣忛〉涓€涓幇鏈?[CarouselViewModel] 瀹炰緥锛坅ssisted 宸ュ巶鎸?topic 鐨?url/title 鍒涘缓锛夛紱
 *    鍗曢〉锛氱洿鎺ョ敤椤甸潰绾?VM 娓叉煋鍒楄〃锛堣€?`isSingle = true` 琛屼负锛夈€? *
 * 鍒楄〃鍐呭锛堝惈鍥剧墖杞挱鍗＄殑 HorizontalPager 鐢诲粖锛夌粺涓€璧?[FeedListPage]銆? *
 * **鍙傛暟**
 * @param viewModel 椤甸潰绾?[CarouselViewModel]锛堣€?`CarouselActivity.viewModel`锛宎ctivity 浣滅敤鍩燂級銆? * @param tabViewModelFactory 澶氭爣绛炬椂姣忛〉 [CarouselViewModel] 鐨?assisted 宸ュ巶锛圚ilt 娉ㄥ叆鍚庝紶鍏ワ級銆? * @param modifier 搴旂敤浜庨〉闈㈢殑 [Modifier]銆? * @param onBack 杩斿洖鍥炶皟銆? * @param onOpenItem 鐐瑰嚮鏉＄洰锛堣烦鍔ㄦ€?璇濋/浜у搧璇︽儏锛岀敱瀹夸富鎺ョ嚎锛夈€? * @param onOpenImage 鐐瑰嚮澶у浘锛堥粯璁ゅ垪琛ㄥ唴宸茶蛋 Mojito 棰勮锛岄€氬父涓嶇敤浼狅級銆? * @param onOpenUser 鐐瑰嚮澶村儚/鏄电О锛堣烦鐢ㄦ埛涓婚〉锛夈€? * @param onLikeClick 鐐硅禐鍥炶皟锛堣蛋 VM `ItemListener.onLikeClick`锛岀敱瀹夸富鎺ョ嚎锛夈€? *
 * **绁栧厛瑕佹眰**锛氬繀椤讳綅浜庢牴涓婚 `MiuixAppTheme` 涔嬪唴锛涙湰缁勪欢鑷甫 TopBarScaffold锛圡iuix Scaffold锛夈€? */
@Composable
fun CarouselScreen(
    viewModel: CarouselViewModel,
    tabViewModelFactory: CarouselViewModel.Factory,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onOpenItem: (HomeFeedResponse.Data) -> Unit = {},
    onOpenImage: (urls: List<String>, index: Int) -> Unit = { _, _ -> },
    onOpenUser: (uid: String?, username: String?) -> Unit = { _, _ -> },
    onLikeClick: ((HomeFeedResponse.Data) -> Unit)? = null,
) {
    val activityState by viewModel.activityState.observeAsState()

    // 澶嶅埢 CarouselActivity.initData()锛氶娆¤繘鍏ヨЕ鍙戝姞杞斤紙Loading 鈫?initCarouselList锛?    LaunchedEffect(Unit) {
        if (viewModel.isAInit) {
            viewModel.isAInit = false
            if (viewModel.topicList == null && viewModel.dataList.value.isNullOrEmpty()) {
                viewModel.activityState.value = LoadingState.Loading
            } else if (viewModel.activityState.value == null) {
                viewModel.activityState.value = LoadingState.LoadingDone
            }
        }
    }

    // 澶嶅埢 BaseViewActivity 鐨?activityState observer锛歀oading 鈫?fetchData()锛堝嵆 initCarouselList锛?    LaunchedEffect(activityState) {
        if (activityState == LoadingState.Loading) {
            viewModel.initCarouselList()
        }
    }

    val title = viewModel.pageTitle ?: viewModel.title

    TopBarScaffold(
        title = title,
        onBack = onBack,
        modifier = modifier,
    ) { paddingValues ->
        when (val state = activityState) {
            null, LoadingState.Loading -> LoadingPlaceholder(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )

            is LoadingState.LoadingError -> ErrorState(
                onRetry = { viewModel.activityState.value = LoadingState.Loading },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                message = state.errMsg,
                retryText = "閲嶈瘯",
            )

            is LoadingState.LoadingFailed -> {
                if (state.msg == LOADING_EMPTY) {
                    EmptyState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        text = state.msg,
                        actionText = "鍒锋柊",
                        onAction = { viewModel.activityState.value = LoadingState.Loading },
                    )
                } else {
                    ErrorState(
                        onRetry = { viewModel.activityState.value = LoadingState.Loading },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        message = state.msg,
                        retryText = "閲嶈瘯",
                    )
                }
            }

            LoadingState.LoadingDone -> {
                val topics = viewModel.topicList.orEmpty()
                if (topics.isEmpty()) {
                    // 鍗曢〉锛氬鐢ㄩ〉闈㈢骇 VM锛堣€?isSingle = true锛?                    FeedListPage(
                        viewModel = viewModel,
                        contentPadding = PaddingValues(vertical = 4.dp),
                        onOpenItem = onOpenItem,
                        onOpenImage = onOpenImage,
                        onOpenUser = onOpenUser,
                        onLikeClick = onLikeClick,
                    )
                } else {
                    val pagerState = rememberPagerState { topics.size }
                    val scope = rememberCoroutineScope()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = paddingValues.calculateTopPadding()),
                    ) {
                        TabRow(
                            tabs = topics.map { it.title },
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
                            val topic = topics[page]
                            // 姣忎釜鏍囩涓€涓幇鏈?CarouselViewModel 瀹炰緥锛坘ey 闅旂锛岄殢瀹夸富 ViewModelStore 瀛樻椿锛?                            val tabViewModel: CarouselViewModel = viewModel(
                                key = "carousel-tab-${topic.url}",
                                factory = CarouselViewModel.provideFactory(
                                    tabViewModelFactory, topic.url, topic.title,
                                ),
                            )
                            FeedListPage(
                                viewModel = tabViewModel,
                                contentPadding = PaddingValues(vertical = 4.dp),
                                onOpenItem = onOpenItem,
                                onOpenImage = onOpenImage,
                                onOpenUser = onOpenUser,
                                onLikeClick = onLikeClick,
                            )
                        }
                    }
                }
            }
        }
    }
}
