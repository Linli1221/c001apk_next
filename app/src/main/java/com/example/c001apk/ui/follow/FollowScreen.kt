package com.example.c001apk.ui.follow

import android.app.Activity
import android.widget.ImageView
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.c001apk.R
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.adapter.ItemListener
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.constant.Constants.LOADING_EMPTY
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.util.DateUtils
import com.example.c001apk.util.ImageUtil
import com.example.c001apk.util.IntentUtil
import com.example.c001apk.util.PrefManager
import com.example.c001apk.ui.user.UserActivity
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 鍏虫敞 / 绮変笣鍒楄〃椤碉紙Miuix 鐗堬級銆? *
 * 瀵圭収鑰佷唬鐮侊細
 * - [FollowPagerFragment]锛氭爣棰橈紙initBar锛? 鍒嗘爮锛坕nitTabList / getFragment锛? * - [FollowFragment] + [FollowViewModel]锛氭瘡涓垎鏍忎竴涓?ViewModel锛屾暟鎹粛璧板師 LiveData
 * - `BaseViewFragment` 鐨勫埛鏂?/ 鍔犺浇鏇村 / 绌烘€?/ 閿欒鎬佹祦绋嬪湪 [FollowUserList] 鍐呭鍒? * - `AppAdapter.UserViewHolder` + `item_search_user.xml` 鐨勭敤鎴疯鍦?[FollowUserRow] 鍐呭鍒? *
 * 鍙仛 UI 灞傝縼绉伙細ViewModel / Repository / 缃戠粶閫昏緫鍏ㄩ儴澶嶇敤锛屼笉鏀瑰姩浠讳綍鏃㈡湁鏂囦欢銆? */

/** 涓€涓垎鏍忥紙鑰?FollowPagerFragment.getFragment 鐨勪竴涓?tab锛夈€?*/
data class FollowTab(
    val label: String,
    val type: String,
    /** 瑕嗙洊椤甸潰 uid锛堥粯璁ゆ部鐢ㄩ〉闈?uid锛涜€佷唬鐮?apk 绛夌被鍨嬪唴閮ㄤ細鎶?uid 缃┖锛岃涓轰笉鍙橈級銆?*/
    val uid: String? = null,
)

/** 鑰?FollowPagerFragment.initBar() 鐨勬爣棰樻槧灏勩€?*/
fun followTitleOf(type: String, isMe: Boolean): String = when (type) {
    "feed" -> "鎴戠殑鍔ㄦ€?
    "follow" -> if (isMe) "鎴戠殑鍏虫敞" else "TA鍏虫敞鐨勪汉"
    "fans" -> if (isMe) "鍏虫敞鎴戠殑浜? else "TA鐨勭矇涓?
    "like" -> "鎴戠殑璧?
    "reply" -> "鎴戠殑鍥炲"
    "recentHistory" -> "鎴戠殑甯稿幓"
    else -> type
}

/** 鑰?FollowPagerFragment.initTabList() + getFragment() 鐨勫垎鏍忔槧灏勩€?*/
fun followTabsOf(type: String, isMe: Boolean): List<FollowTab> = when (type) {
    "follow" ->
        if (isMe)
            listOf(
                FollowTab("鐢ㄦ埛", "follow"),
                FollowTab("璇濋", "topic"),
                FollowTab("鏁扮爜", "product"),
                FollowTab("搴旂敤", "apk"),
            )
        else
            listOf(FollowTab("", "follow"))

    "reply" -> listOf(
        FollowTab("鎴戠殑鍥炲", "reply"),
        FollowTab("鎴戞敹鍒扮殑鍥炲", "replyToMe"),
    )

    else -> listOf(FollowTab("", type))
}

/**
 * 鍏虫敞 / 绮変笣鍒楄〃鏁撮〉锛堥《鏍?+ 鍙€?TabRow 鍒嗘爮 + 鍒楄〃锛夈€? *
 * @param viewModelFactory 鐢辫皟鐢ㄦ柟娉ㄥ叆 FollowViewModel 鐨?AssistedFactory锛? * 渚嬪 `{ uid, type -> FollowViewModel.provideFactory(assistedFactory, uid, type) }`銆? * @param onBackClick 杩斿洖鎸夐挳锛沶ull 鏃剁粨鏉熷涓?Activity锛堢瓑浠疯€?onBackClick锛夈€? * @param onUserClick 鐐瑰嚮鐢ㄦ埛琛岋紱null 鏃惰烦杞?UserActivity锛堢瓑浠疯€?ItemListener.onViewUser锛夈€? * @param onEntityClick 鐐瑰嚮闈炵敤鎴疯锛堣瘽棰?鏁扮爜/搴旂敤绛夊厹搴曡锛夛紱null 鏃朵笉璺宠浆銆? */
@Composable
fun FollowScreen(
    uid: String,
    type: String,
    viewModelFactory: (uid: String, type: String) -> ViewModelProvider.Factory,
    modifier: Modifier = Modifier,
    title: String = followTitleOf(type, uid == PrefManager.uid),
    tabs: List<FollowTab> = followTabsOf(type, uid == PrefManager.uid),
    onBackClick: (() -> Unit)? = null,
    onUserClick: ((String?) -> Unit)? = null,
    onEntityClick: ((HomeFeedResponse.Data) -> Unit)? = null,
    onToast: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    Scaffold(
        modifier = modifier,
        topBar = {
            SmallTopAppBar(
                title = title,
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (onBackClick != null) onBackClick()
                            else (context as? Activity)?.finish()
                        },
                    ) {
                        Icon(MiuixIcons.Back, contentDescription = "杩斿洖")
                    }
                },
            )
        },
    ) { paddingValues ->
        if (tabs.size <= 1) {
            // 鍗曞垪琛紙TA鍏虫敞鐨勪汉 / 绮変笣 / 鍔ㄦ€?绛夛級锛氳€佷唬鐮侀殣钘?tabLayout
            val tab = tabs.firstOrNull() ?: return@Scaffold
            val viewModel = rememberFollowViewModel(tab, uid, viewModelFactory)
            FollowUserList(
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onUserClick = onUserClick,
                onEntityClick = onEntityClick,
                onToast = onToast,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                val pagerState = rememberPagerState { tabs.size }
                val scope = rememberCoroutineScope()
                TabRow(
                    tabs = tabs.map { it.label },
                    selectedTabIndex = pagerState.currentPage,
                    onTabSelected = { index ->
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val tab = tabs[page]
                    val viewModel = rememberFollowViewModel(tab, uid, viewModelFactory)
                    FollowUserList(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                        onUserClick = onUserClick,
                        onEntityClick = onEntityClick,
                        onToast = onToast,
                    )
                }
            }
        }
    }
}

/**
 * 鍗曚釜鍒嗘爮鐨勫垪琛ㄥ唴瀹癸紙澶嶇敤鐜版湁 [FollowViewModel]锛孡iveData 鐢?observeAsState 妗ユ帴锛夈€? * 鐘舵€侀棴鐜細鍔犺浇涓?/ 鍒楄〃 / 绌烘€?/ 閿欒鎬?/ 涓嬫媺鍒锋柊 / 瑙﹀簳鍔犺浇 / 搴曢儴閲嶈瘯銆? */
@Composable
fun FollowUserList(
    viewModel: FollowViewModel,
    modifier: Modifier = Modifier,
    onUserClick: ((String?) -> Unit)? = null,
    onEntityClick: ((HomeFeedResponse.Data) -> Unit)? = null,
    onToast: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val dataList by viewModel.dataList.observeAsState(emptyList())
    val loadingState by viewModel.loadingState.observeAsState()
    val footerState by viewModel.footerState.observeAsState()
    val toastEvent by viewModel.toastText.observeAsState()

    var pullRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    // 澶嶇敤 BaseAppViewModel.ItemClickListener锛氬叧娉?鍙栧叧璧板師 onPostFollowUnFollow 閾捐矾
    val itemListener = remember(viewModel) { viewModel.ItemClickListener() }

    // 鑰?BaseAppFragment 鍦?dataList 鍙樺寲鏃跺悓姝?listSize锛坒etchFeedList 鎹鍐冲畾 loading/footer 鍒嗘敮锛?    SideEffect { viewModel.listSize = dataList.size }

    // 棣栨杩涘叆锛氱瓑浠?BaseViewFragment.onResume 鐨?isInit 鍒嗘敮 + refreshData()
    LaunchedEffect(viewModel) {
        applyFollowTypeConfig(viewModel)
        if (viewModel.isInit) {
            viewModel.isInit = false
            refreshFollowList(viewModel, showLoadingIndicator = true)
        }
    }

    // 涓嬫媺鍒锋柊鏀跺熬锛歭oadingState 浠绘剰鍙樺寲銆乫ooterState 鍙樹负闈?Loading 鏃跺叧闂埛鏂版寚绀哄櫒
    // 锛堢瓑浠疯€?BaseViewFragment.initObserve / BaseAppFragment.initObserve 瀵?swipeRefresh 鐨勫鐞嗭級
    DisposableEffect(viewModel, lifecycleOwner) {
        val stopRefreshing = { pullRefreshing = false }
        val loadingObserver = Observer<LoadingState> { stopRefreshing() }
        val footerObserver = Observer<FooterState> {
            if (it !is FooterState.Loading) stopRefreshing()
        }
        viewModel.loadingState.observe(lifecycleOwner, loadingObserver)
        viewModel.footerState.observe(lifecycleOwner, footerObserver)
        onDispose {
            viewModel.loadingState.removeObserver(loadingObserver)
            viewModel.footerState.removeObserver(footerObserver)
        }
    }

    // 瑙﹀簳鍔犺浇鏇村锛堢瓑浠疯€?RecyclerView.OnScrollListener 鐨?loadMore 瑙﹀彂锛?    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .let { flow ->
                var lastEmitted = Int.MIN_VALUE
                flow.collect { last ->
                    if (last == lastEmitted) return@collect
                    lastEmitted = last
                    val items = viewModel.dataList.value
                    if (!items.isNullOrEmpty() && last >= items.size - 1 &&
                        !viewModel.isEnd && !viewModel.isRefreshing && !viewModel.isLoadMore
                    ) {
                        viewModel.isLoadMore = true
                        viewModel.fetchData()
                    }
                }
            }
    }

    // toastText 浜嬩欢娑堣垂锛堢瓑浠疯€?FollowFragment.initObserve 鐨?Toast锛?    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let { message ->
            if (message != null) {
                if (onToast != null) onToast(message)
                else Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val errorMessage = loadingState as? LoadingState.LoadingError
    val failedMessage = loadingState as? LoadingState.LoadingFailed
    val isLoading = loadingState == null || loadingState is LoadingState.Loading

    Box(modifier = modifier) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    InfiniteProgressIndicator(color = MiuixTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "鍔犺浇涓€?,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            errorMessage != null -> {
                StateCenter(
                    message = errorMessage.errMsg,
                    buttonText = stringResource(R.string.retry),
                    onClick = { refreshFollowList(viewModel, showLoadingIndicator = true) },
                )
            }

            failedMessage != null -> {
                StateCenter(
                    message = failedMessage.msg,
                    buttonText = if (failedMessage.msg == LOADING_EMPTY)
                        stringResource(R.string.refresh)
                    else
                        stringResource(R.string.retry),
                    onClick = { refreshFollowList(viewModel, showLoadingIndicator = true) },
                )
            }

            else -> {
                PullToRefresh(
                    isRefreshing = pullRefreshing,
                    onRefresh = {
                        pullRefreshing = true
                        refreshFollowList(viewModel, showLoadingIndicator = false)
                    },
                    pullToRefreshState = pullToRefreshState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(dataList) { data ->
                            FollowItemRow(
                                data = data,
                                itemListener = itemListener,
                                onUserClick = onUserClick,
                                onEntityClick = onEntityClick,
                            )
                            HorizontalDivider()
                        }
                        item(key = "follow_footer") {
                            FollowFooter(
                                footerState = footerState,
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

/** 绌烘€?/ 閿欒鎬侊細鎻愮ず鏂囨 + 閲嶈瘯鎸夐挳銆?*/
@Composable
private fun StateCenter(
    message: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(Modifier.height(12.dp))
        TextButton(text = buttonText, onClick = onClick)
    }
}

/** 鍒楄〃搴曢儴锛氬姞杞戒腑 / 娌℃湁鏇村浜?/ 鍔犺浇澶辫触閲嶈瘯锛堢瓑浠疯€?FooterAdapter锛夈€?*/
@Composable
private fun FollowFooter(
    footerState: FooterState?,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (footerState) {
            is FooterState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InfiniteProgressIndicator(
                        color = MiuixTheme.colorScheme.primary,
                        size = 16.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "鍔犺浇涓€?,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            is FooterState.LoadingEnd -> Text(
                text = footerState.msg,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )

            is FooterState.LoadingError -> Text(
                text = "${footerState.errMsg}锛岀偣鍑婚噸璇?,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.error,
                modifier = Modifier.clickable(onClick = onRetry),
            )

            else -> {}
        }
    }
}

/**
 * 鍒楄〃鏉＄洰锛氱敤鎴疯锛坋ntityType contacts/user锛屽鍒?AppAdapter.UserViewHolder锛夛紝
 * 鍏朵綑瀹炰綋锛堣瘽棰?鏁扮爜/搴旂敤/鍥炲绛夛級鐢ㄥ熀纭€琛屽厹搴曪紝鐐瑰嚮璧?onEntityClick銆? */
@Composable
private fun FollowItemRow(
    data: HomeFeedResponse.Data,
    itemListener: ItemListener,
    onUserClick: ((String?) -> Unit)?,
    onEntityClick: ((HomeFeedResponse.Data) -> Unit)?,
) {
    val context = LocalContext.current
    when (data.entityType) {
        "contacts", "user" -> FollowUserRow(
            data = data,
            onUserClick = { uid ->
                if (onUserClick != null) onUserClick(uid)
                else IntentUtil.startActivity<UserActivity>(context) { putExtra("id", uid) }
            },
            onFollowClick = { uid, isFollow -> itemListener.onFollowUser(uid, isFollow) },
        )

        else -> {
            val title = data.title ?: data.username ?: data.id.orEmpty()
            val summary = data.subTitle ?: data.description ?: ""
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEntityClick?.invoke(data) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (summary.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = summary,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * 鐢ㄦ埛琛岋紙澶嶅埢 item_search_user.xml + AppAdapter.UserViewHolder.bind 鐨勪笁鍒嗘敮锛夈€? * 鍏虫敞鎸夐挳鍙湪銆宒ata.userInfo != null && data.fUserInfo == null銆嶄笖宸茬櫥褰曟椂鏄剧ず銆? */
@Composable
private fun FollowUserRow(
    data: HomeFeedResponse.Data,
    onUserClick: (String?) -> Unit,
    onFollowClick: (uid: String, isFollow: Int) -> Unit,
) {
    val userInfo = data.userInfo
    val fUserInfo = data.fUserInfo

    val uid: String?
    val uname: String?
    val follow: String?
    val fans: String?
    val logintime: Long?
    val avatar: String?
    val isFollow: Int
    val showFollowButton: Boolean

    if (userInfo != null && fUserInfo != null) {
        uid = userInfo.uid
        uname = userInfo.username
        follow = userInfo.follow
        fans = userInfo.fans
        logintime = userInfo.logintime
        avatar = userInfo.userAvatar
        isFollow = 0
        showFollowButton = false
    } else if (userInfo == null && fUserInfo != null) {
        uid = fUserInfo.uid
        uname = fUserInfo.username
        follow = fUserInfo.follow
        fans = fUserInfo.fans
        logintime = fUserInfo.logintime
        avatar = fUserInfo.userAvatar
        isFollow = 0
        showFollowButton = false
    } else {
        uid = data.uid
        uname = data.username
        follow = data.follow
        fans = data.fans
        logintime = data.logintime
        avatar = data.userAvatar
        isFollow = data.isFollow ?: 0
        showFollowButton = PrefManager.isLogin
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserClick(uid) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarImage(
            url = avatar,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = uname.orEmpty(),
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Row {
                Text(
                    text = "$follow鍏虫敞",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "$fans绮変笣",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = DateUtils.fromToday(logintime ?: 0L) + "娲昏穬",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                )
            }
        }
        if (showFollowButton) {
            Spacer(Modifier.width(8.dp))
            TextButton(
                text = if (isFollow == 1) "宸插叧娉? else "鍏虫敞",
                onClick = {
                    uid?.let { onFollowClick(it, isFollow) }
                },
                colors = if (isFollow == 1)
                    ButtonDefaults.textButtonColors()
                else
                    ButtonDefaults.textButtonColorsPrimary(),
                insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** 鍦嗗舰澶村儚锛氭部鐢?Glide 閾捐矾锛圛mageUtil.showIMG锛夛紝AndroidView 鍖?ImageView銆?*/
@Composable
private fun AvatarImage(
    url: String?,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        modifier = modifier,
        update = { imageView -> ImageUtil.showIMG(imageView, url) },
    )
}

/**
 * 澶嶅埢 FollowFragment.onCreate 鐨?url/title 棰勭疆锛坱opic/product/favorite锛夛紝
 * 蹇呴』鍦ㄧ涓€娆?fetchData 涔嬪墠璋冪敤銆? */
private fun applyFollowTypeConfig(viewModel: FollowViewModel) {
    when (viewModel.type) {
        "topic" -> {
            viewModel.url = "#/topic/userFollowTagList"
            viewModel.title = "鎴戝叧娉ㄧ殑璇濋"
        }

        "product" -> {
            viewModel.url = "#/product/followProductList"
            viewModel.title = "鎴戝叧娉ㄧ殑鏁扮爜鍚?
        }

        "favorite" -> {
            viewModel.url = "#/collection/followList"
            viewModel.title = "鎴戝叧娉ㄧ殑鏀惰棌鍗?
        }
    }
}

/** 澶嶅埢 BaseViewFragment.refreshData()銆?*/
private fun refreshFollowList(viewModel: FollowViewModel, showLoadingIndicator: Boolean) {
    viewModel.lastItem = null
    viewModel.page = 1
    viewModel.isEnd = false
    viewModel.isRefreshing = true
    viewModel.isLoadMore = false
    if (showLoadingIndicator) {
        viewModel.loadingState.value = LoadingState.Loading
    }
    viewModel.fetchData()
}

/** 姣忎釜鍒嗘爮涓€涓?[FollowViewModel]锛岄殢 ViewModelStore 璧伴厤缃彉鏇村瓨娲汇€?*/
@Composable
private fun rememberFollowViewModel(
    tab: FollowTab,
    fallbackUid: String,
    viewModelFactory: (uid: String, type: String) -> ViewModelProvider.Factory,
): FollowViewModel {
    val uid = tab.uid ?: fallbackUid
    return viewModel(
        key = "c001apk_follow_${uid}_${tab.type}",
        factory = viewModelFactory(uid, tab.type),
    )
}
