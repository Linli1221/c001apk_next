package com.example.c001apk.ui.topic

import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.livedata.observeAsState
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.constant.Constants
import com.example.c001apk.logic.model.TopicBean
import com.example.c001apk.util.ImageUtil
import com.example.c001apk.util.PrefManager
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 璇濋 / 浜у搧椤碉紙TopicActivity + TopicFragment + TopicHeader 鐨?Compose 鐗堬級銆? *
 * 鐘舵€佸叏閮ㄨ蛋鐜版湁 [TopicViewModel]锛圠iveData 鐢?observeAsState 妗ユ帴锛夛細
 * - [TopicViewModel.activityState]锛氶〉闈㈢骇 鍔犺浇涓?/ 閿欒娑堟伅 / 澶辫触閲嶈瘯锛堝榻?BaseViewActivity 鐘舵€佹満锛? * - [TopicViewModel.headerState]锛氬ご閮ㄨ瘽棰樺崱锛坙ogo / 鏍囬 / 鐑害 / 鍏虫敞鑰呭ご鍍忥級
 * - [TopicViewModel.followState]锛氬叧娉ㄦ寜閽袱鎬侊紙Event 涓€娆℃€ф秷璐癸級
 * - [TopicViewModel.toastText]锛歵oast锛圗vent 涓€娆℃€ф秷璐癸級
 *
 * 鍏虫敞鎸夐挳鐐瑰嚮閫昏緫涓?TopicFragment.onSubscribeClick 涓€鑷达紙璇濋 tag / 鏈哄瀷 product 涓ょ鍗忚锛夛紝
 * 鐩存帴璋冪敤鐜版湁 ViewModel 鏂规硶锛屼笉鏂板涓氬姟閫昏緫銆? *
 * 鍚?tab 鐨勫唴瀹癸紙TopicContentFragment 鐨勫垪琛?/ WebViewFragment 鐨?H5锛夐€氳繃 [tabContent]
 * 鎻掓Ы鐢辨帴绾垮眰鎻愪緵锛屾湰鏂囦欢涓嶅叧蹇冨叾鍐呴儴瀹炵幇銆? *
 * 鎮诞鍙戝竷鎸夐挳锛氫骇鍝侀〉涓旀湁璇勫垎瀛愰」鏃跺厛寮广€屽彂甯冨姩鎬?/ 鍙戣〃鐐硅瘎銆嶄簩閫変竴锛圤verlayDialog锛夛紝
 * 鍚﹀垯鐩存帴鍥炶皟 onPublishClick("createFeed")锛涘叿浣撹烦杞?ReplyActivity 鐢辨帴绾垮眰澶勭悊銆? */
@Composable
fun TopicScreen(
    viewModel: TopicViewModel,
    tabContent: @Composable (index: Int, tab: TopicBean) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onPublishClick: (type: String) -> Unit = {},
    onFollowersClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val activityState by viewModel.activityState.observeAsState()
    val headerState by viewModel.headerState.observeAsState()
    val followState by viewModel.followState.observeAsState()
    val toastText by viewModel.toastText.observeAsState()

    // 鍏虫敞鎸夐挳鐘舵€侊細鍒濆€煎彇 ViewModel锛屽悗缁敱 followState 浜嬩欢鍒锋柊
    var followed by remember { mutableStateOf(viewModel.isFollow) }
    var showPublishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(followState) {
        followState?.getContentIfNotHandledOrReturnNull()?.let { followed = it }
    }

    LaunchedEffect(toastText) {
        toastText?.getContentIfNotHandledOrReturnNull()?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    // 椤甸潰绾х姸鎬佹満锛堝榻?BaseViewActivity锛夛細Loading 鈫?鎷夊彇甯冨眬锛涢噸璇曟寜閽?= 閲嶆柊缃?Loading
    LaunchedEffect(activityState) {
        if (activityState is LoadingState.Loading) {
            when (viewModel.type) {
                "topic" -> {
                    viewModel.url = viewModel.url.replace("/t/", "")
                    viewModel.fetchTopicLayout()
                }

                "product" -> viewModel.fetchProductLayout()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.isAInit) {
            viewModel.isAInit = false
            if (viewModel.topicList == null) {
                viewModel.activityState.value = LoadingState.Loading
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            SmallTopAppBar(
                title = if (viewModel.type == "topic") viewModel.url.replace("/t/", "")
                else viewModel.title,
                subtitle = viewModel.subtitle.orEmpty(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "杩斿洖")
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(MiuixIcons.Basic.Search, contentDescription = "鎼滅储")
                    }
                    IconButton(onClick = onMoreClick) {
                        Icon(MiuixIcons.More, contentDescription = "鏇村")
                    }
                },
            )
        },
        floatingActionButton = {
            if (PrefManager.isLogin && !viewModel.topicList.isNullOrEmpty()) {
                FloatingActionButton(
                    onClick = {
                        // 鏈哄瀷椤靛彲浠ュ彂鍔ㄦ€侊紝涔熷彲浠ュ彂琛ㄧ偣璇勶紙type=rating锛?                        if (viewModel.type == "product" && !viewModel.ratingItemInfo.isNullOrEmpty())
                            showPublishDialog = true
                        else
                            onPublishClick("createFeed")
                    }
                ) {
                    Icon(MiuixIcons.Add, contentDescription = "鍙戝竷")
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when (val state = activityState) {
                is LoadingState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is LoadingState.LoadingError -> {
                    Text(
                        text = state.errMsg,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                    )
                }

                is LoadingState.LoadingFailed -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = state.msg,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.body2,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            text = if (state.msg == Constants.LOADING_EMPTY) "鍒锋柊" else "閲嶈瘯",
                            onClick = { viewModel.activityState.value = LoadingState.Loading },
                        )
                    }
                }

                else -> {
                    val tabs = viewModel.topicList.orEmpty()
                    if (tabs.isEmpty()) {
                        Text(
                            text = Constants.LOADING_EMPTY,
                            modifier = Modifier.align(Alignment.Center),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.body2,
                        )
                    } else {
                        // 鏈嶅姟绔寚瀹氱殑榛樿 tab锛岀敤瀹屽嵆娓咃紙瀵归綈 TopicFragment.initSelected锛?                        val initialPage = remember {
                            val page = (viewModel.tabSelected ?: 0).coerceIn(0, tabs.lastIndex)
                            viewModel.tabSelected = null
                            page
                        }
                        val pagerState = rememberPagerState(initialPage = initialPage) { tabs.size }
                        val scope = rememberCoroutineScope()

                        Column(modifier = Modifier.fillMaxSize()) {
                            headerState?.let { header ->
                                TopicHeaderCard(
                                    header = header,
                                    isFollow = followed,
                                    showFollowBtn = PrefManager.isLogin,
                                    onFollowClick = { onSubscribeClick(viewModel) },
                                    onFollowersClick = onFollowersClick,
                                )
                            }
                            TabRow(
                                tabs = tabs.map { it.title },
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
                                tabContent(page, tabs[page])
                            }
                        }
                    }
                }
            }

            // 浜у搧椤点€屽彂甯冨姩鎬?/ 鍙戣〃鐐硅瘎銆嶄簩閫変竴锛堝榻?TopicFragment.initFab 鐨勫脊绐楋級
            OverlayDialog(
                show = showPublishDialog,
                title = "鍙戝竷",
                onDismissRequest = { showPublishDialog = false },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = "鍙戝竷鍔ㄦ€?,
                        onClick = {
                            showPublishDialog = false
                            onPublishClick("createFeed")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        text = "鍙戣〃鐐硅瘎",
                        onClick = {
                            showPublishDialog = false
                            onPublishClick("rating")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * 鍏虫敞 / 鍙栨秷鍏虫敞锛堜笌 TopicFragment.onSubscribeClick 涓€鑷达級锛? * 璇濋锛坱opic锛夎蛋 getFollow 鍗忚锛屾満鍨嬶紙product锛夎蛋 postFollow 鍗忚銆? */
private fun onSubscribeClick(viewModel: TopicViewModel) {
    when (viewModel.type) {
        "topic" -> {
            val followUrl =
                if (viewModel.isFollow) "/v6/feed/unFollowTag"
                else "/v6/feed/followTag"
            val tag = viewModel.url.replace("/t/", "")
            viewModel.onGetFollow(followUrl, tag, null)
        }

        "product" -> {
            if (viewModel.postFollowData.isNullOrEmpty())
                viewModel.postFollowData = HashMap()
            viewModel.postFollowData?.let { map ->
                map["id"] = viewModel.id
                map["status"] = if (viewModel.isFollow) "0" else "1"
            }
            viewModel.onPostFollow()
        }
    }
}

/**
 * 璇濋椤靛ご閮ㄥ崱鐗囷紙item_topic_header.xml 鐨?Compose 鐗堬級锛? * logo + 鏍囬 + 鐑害/璁ㄨ鏁?+ 鏈€杩戝叧娉ㄨ€呭ご鍍忚 + 鍏虫敞鎸夐挳銆? */
@Composable
private fun TopicHeaderCard(
    header: TopicHeader,
    isFollow: Boolean,
    showFollowBtn: Boolean,
    onFollowClick: () -> Unit,
    onFollowersClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        GlideImage(
            url = header.logo,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp)),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = header.title.orEmpty(),
                style = MiuixTheme.textStyles.title2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // 鏈嶅姟绔彧涓嬪彂鏁板瓧锛堝銆?.2涓囥€嶏級锛屽崟浣嶅湪鏈湴鐐逛笂
            val stats = listOfNotNull(
                header.hotNum?.takeIf { it.isNotEmpty() },
                header.commentNum?.takeIf { it.isNotEmpty() }?.let { "${it}璁ㄨ" },
            ).joinToString(" 路 ")
            if (stats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stats,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val avatars = header.avatars.take(3)
            val followText =
                header.followNum?.takeIf { it.isNotEmpty() }?.let { "${it}浜哄叧娉? }.orEmpty()
            if (avatars.isNotEmpty() || followText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.clickable(onClick = onFollowersClick),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    avatars.forEachIndexed { index, url ->
                        GlideImage(
                            url = url,
                            modifier = Modifier
                                .size(28.dp)
                                .offset(x = if (index == 0) 0.dp else (-8).dp)
                                .clip(CircleShape),
                        )
                    }
                    if (followText.isNotEmpty()) {
                        Text(
                            text = followText,
                            modifier = Modifier.padding(start = 6.dp),
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "鈥?,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }

        // 鍏虫敞 / 宸插叧娉細鏈叧娉?= 涓婚鑹插～鍏咃紝宸插叧娉?= 寮卞寲搴曡壊锛堝榻?bindFollowBtn 涓ゆ€侀厤鑹诧級
        if (showFollowBtn) {
            Button(
                onClick = onFollowClick,
                colors = if (isFollow) ButtonDefaults.buttonColors()
                else ButtonDefaults.buttonColorsPrimary(),
                cornerRadius = 16.dp,
                minWidth = 76.dp,
                minHeight = 32.dp,
                insideMargin = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            ) {
                Text(
                    text = if (isFollow) "宸插叧娉? else "鍏虫敞",
                    style = MiuixTheme.textStyles.button,
                )
            }
        }
    }
}

/**
 * 鍥剧墖璧扮幇鏈?Glide 閾捐矾锛圙lide 娌℃湁瀹樻柟 Compose 闆嗘垚锛岀敤 AndroidView 鍖?ImageView锛夈€? * TODO: ui/common 鎻愪緵鍏叡鍥剧墖缁勪欢鍚庢浛鎹负缁熶竴瀹炵幇銆? */
@Composable
private fun GlideImage(
    url: String?,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { view -> ImageUtil.showIMG(view, url) },
    )
}
