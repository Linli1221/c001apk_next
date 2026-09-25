package com.example.c001apk.ui.homefeed

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.livedata.observeAsState
import com.example.c001apk.R
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.adapter.ItemListener
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.adapter.PopClickListener
import com.example.c001apk.constant.Constants.LOADING_EMPTY
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.util.PrefManager
import com.example.c001apk.util.showPublishStatusDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 棣栭〉淇℃伅娴侀〉闈紙杩佺Щ鑷?HomeFeedFragment + BaseRefreshRecyclerviewBinding + AppAdapter锛夈€? *
 * 鏁版嵁娴佸畬鍏ㄥ鐢ㄧ幇鏈?[HomeFeedViewModel]锛堢户鎵?BaseAppViewModel锛歞ataList / loadingState /
 * footerState / toastText / publishStatusEvent锛夛紝鏈枃浠跺彧鍋?LiveData 鈫?Compose 鐘舵€佹ˉ鎺ヤ笌
 * 銆屽埛鏂?/ 鍔犺浇鏇村 / 绌烘€?/ 閿欒鎬?/ 閲嶈瘯銆嶇姸鎬佹満锛堝搴?BaseViewFragment 鐨? * refreshData / loadMore / initObserve 閫昏緫锛夈€? *
 * @param viewModel 澶嶇敤鐜版湁 HomeFeedViewModel锛堢敱瀹夸富鐢?Factory 娉ㄥ叆 installTime 鍚庝紶鍏ワ級
 * @param type 椤甸潰绫诲瀷锛?follow" 鍏虫敞 / "feed" 澶存潯 / "rank" 鐑 / "coolPic" 閰峰浘
 * @param tabReselectedTick 棣栭〉 tab 鍐嶆鐐瑰嚮鐨勮鏁板櫒锛堟潵鑷?HomeScreen 鐨勫唴瀹规彃妲斤級锛? * 姣忔 +1 瑙﹀彂銆屽洖鍒伴《閮?鍒锋柊銆嶏紝鍏虫敞椤靛垯寮广€屽叧娉ㄥ垎缁勩€嶉€夋嫨妗? * @param onScrolled 鍒楄〃婊氬姩鍥炶皟锛堝師锛氫笂婊戦殣钘?/ 涓嬫粦鏄剧ず搴曢儴瀵艰埅锛? * @param onPublish 鍙戝竷鎸夐挳鍥炶皟锛氬叆鍙?"createFeed" 鍙戝姩鎬?/ "createArticle" 鍙戝浘鏂? * 锛堝師锛歴tartReply 璺?ReplyActivity / ArticlePublishActivity锛屾帴绾胯疆鐢卞涓诲疄鐜帮級
 */
@Composable
fun HomeFeedScreen(
    viewModel: HomeFeedViewModel,
    type: String,
    modifier: Modifier = Modifier,
    tabReselectedTick: Int = 0,
    onScrolled: (dy: Int) -> Unit = {},
    onPublish: (publishType: String) -> Unit = {},
) {
    val context = LocalContext.current
    val anchorView = LocalView.current
    val listener = remember(viewModel) { viewModel.ItemClickListener() }
    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    val dataList by viewModel.dataList.observeAsState()
    val loadingState by viewModel.loadingState.observeAsState()
    val footerState by viewModel.footerState.observeAsState()
    val toastEvent by viewModel.toastText.observeAsState()
    val publishStatusEvent by viewModel.publishStatusEvent.observeAsState()

    // 鈹€鈹€ type 鈫?鎺ュ彛鍦板潃鏄犲皠锛堝搴?HomeFeedFragment.onCreate锛夆攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
    LaunchedEffect(type) {
        if (viewModel.type == null) {
            viewModel.type = type
            when (type) {
                "rank" -> {
                    viewModel.dataListUrl = "/page?url=V9_HOME_TAB_RANKING"
                    viewModel.dataListTitle = "鐑"
                }

                "follow" -> {
                    if (viewModel.dataListUrl.isNullOrEmpty()) {
                        when (PrefManager.FOLLOWTYPE) {
                            "all" -> {
                                viewModel.dataListUrl = "/page?url=V9_HOME_TAB_FOLLOW"
                                viewModel.dataListTitle = "鍏ㄩ儴鍏虫敞"
                            }

                            "circle" -> {
                                viewModel.dataListUrl = "/page?url=V9_HOME_TAB_FOLLOW&type=circle"
                                viewModel.dataListTitle = "濂藉弸鍏虫敞"
                            }

                            "topic" -> {
                                viewModel.dataListUrl = "/page?url=V9_HOME_TAB_FOLLOW&type=topic"
                                viewModel.dataListTitle = "璇濋鍏虫敞"
                            }

                            else -> {
                                viewModel.dataListUrl = "/page?url=V9_HOME_TAB_FOLLOW&type=product"
                                viewModel.dataListTitle = "鏁扮爜鍏虫敞"
                            }
                        }
                    }
                }

                "coolPic" -> {
                    viewModel.dataListUrl = "/page?url=V11_FIND_COOLPIC"
                    viewModel.dataListTitle = "閰峰浘"
                }
            }
        }
    }

    // 鍒锋柊锛堝搴?BaseViewFragment.refreshData + HomeFeedFragment.fetchData锛?    val refreshData: () -> Unit = {
        viewModel.lastItem = null
        viewModel.page = 1
        viewModel.isEnd = false
        viewModel.isRefreshing = true
        viewModel.isLoadMore = false
        viewModel.fetchData()
        viewModel.changeFirstItem = true
    }

    // 鍔犺浇鏇村锛堝搴?BaseViewFragment.loadMore锛?    val loadMore: () -> Unit = {
        viewModel.isLoadMore = true
        viewModel.fetchData()
        viewModel.changeFirstItem = true
    }

    // 棣栨杩涘叆瑙﹀彂棣栧睆鍔犺浇锛堝搴旀棫 initData()锛歭oadingState = Loading锛?    LaunchedEffect(Unit) {
        if (viewModel.loadingState.value == null && viewModel.dataList.value == null) {
            viewModel.loadingState.value = LoadingState.Loading
        }
    }

    // loadingState 鐘舵€佹満锛堝搴?BaseViewFragment.initObserve锛? 鍒楄〃闀垮害闀滃儚
    LaunchedEffect(dataList) {
        viewModel.listSize = dataList?.size ?: -1
    }
    LaunchedEffect(loadingState) {
        when (loadingState) {
            LoadingState.Loading -> if (!viewModel.isLoadMore) refreshData()
            else -> {}
        }
        isRefreshing = false
    }
    LaunchedEffect(footerState) {
        if (footerState !is FooterState.Loading) isRefreshing = false
    }

    // toast / 淇敼鍙鎬у脊绐楋紙瀵瑰簲 HomeFeedFragment.initObserve + BaseAppFragment.initObserve锛?    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(publishStatusEvent) {
        publishStatusEvent?.getContentIfNotHandledOrReturnNull()?.let { id ->
            showPublishStatusDialog(context, viewModel.publishStatusOf(id)) { status ->
                viewModel.onPostPublishStatus(id, status)
            }
        }
    }

    // 瑙﹀簳鑷姩鍔犺浇鏇村锛堝搴?BaseViewFragment.initScroll锛?    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            info.totalItemsCount to (info.visibleItemsInfo.lastOrNull()?.index ?: -1)
        }
            .distinctUntilChanged()
            .collect { (total, lastVisible) ->
                if (total > 0 && lastVisible >= total - 2 &&
                    !viewModel.isEnd && !viewModel.isRefreshing && !viewModel.isLoadMore
                ) {
                    loadMore()
                }
            }
    }

    // 鍒楄〃婊氬姩鏂瑰悜鍥炶皟锛堝搴?HomeFeedFragment.onScrolled 鈫?鏄鹃殣搴曢儴瀵艰埅锛?    val currentOnScrolled by rememberUpdatedState(onScrolled)
    LaunchedEffect(listState) {
        var lastPosition = 0
        snapshotFlow { listState.firstVisibleItemIndex * 100000 + listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { position ->
                val dy = position - lastPosition
                lastPosition = position
                if (dy != 0) currentOnScrolled(dy)
            }
    }

    // 棣栭〉 tab 鍐嶆鐐瑰嚮锛堝搴?HomeFeedFragment.onReturnTop锛夛細
    // 鍏虫敞椤靛脊銆屽叧娉ㄥ垎缁勩€嶏紝鍏朵綑鍥為《 + 鍒锋柊
    LaunchedEffect(tabReselectedTick) {
        if (tabReselectedTick > 0) {
            if (type == "follow") {
                showFollowGroupDialog(context, viewModel) { refreshData() }
            } else {
                listState.scrollToItem(0)
                isRefreshing = true
                refreshData()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (type == "feed" && PrefManager.isLogin) {
                FloatingActionButton(
                    onClick = { showPublishDialog(context, onPublish) },
                ) {
                    Icon(
                        imageVector = MiuixIcons.Add,
                        contentDescription = stringResource(R.string.publishFeed),
                        tint = MiuixTheme.colorScheme.onPrimary,
                    )
                }
            }
        },
    ) { paddingValues ->
        PullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = {
                // 鏃т唬鐮佸彧鍦?LoadingDone 鏃跺厑璁镐笅鎷夊埛鏂帮紙swipeRefresh.isEnabled锛?                if (loadingState is LoadingState.LoadingDone && !viewModel.isLoadMore) {
                    isRefreshing = true
                    refreshData()
                }
            },
            pullToRefreshState = pullToRefreshState,
        ) {
            val list = dataList.orEmpty()
            when {
                list.isEmpty() && loadingState is LoadingState.Loading -> {
                    LoadingContent(modifier = Modifier.fillMaxSize())
                }

                list.isEmpty() && loadingState is LoadingState.LoadingError -> {
                    MessageContent(
                        message = (loadingState as LoadingState.LoadingError).errMsg,
                        isError = true,
                        buttonText = stringResource(R.string.retry),
                        onClick = { viewModel.loadingState.value = LoadingState.Loading },
                    )
                }

                list.isEmpty() && loadingState is LoadingState.LoadingFailed -> {
                    val msg = (loadingState as LoadingState.LoadingFailed).msg
                    MessageContent(
                        message = msg,
                        isError = false,
                        buttonText = stringResource(
                            if (msg == LOADING_EMPTY) R.string.refresh else R.string.retry
                        ),
                        onClick = { viewModel.loadingState.value = LoadingState.Loading },
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 10.dp,
                            bottom = paddingValues.calculateBottomPadding() + 10.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        itemsIndexed(list) { index, data ->
                            when {
                                data.entityType == "feed" -> FeedCard(
                                    data = data,
                                    onFeedClick = {
                                        listener.onViewFeed(
                                            anchorView, data.id, data.userInfo?.uid,
                                            data.userInfo?.username, data.userInfo?.userAvatar,
                                            data.deviceTitle, data.message,
                                            data.dateline?.toString(), null, null,
                                        )
                                    },
                                    onUserClick = {
                                        listener.onViewUser(anchorView, data.userInfo?.uid)
                                    },
                                    onLikeClick = {
                                        listener.onLikeClick(
                                            data.entityType, data.id.orEmpty(),
                                            data.userAction?.like ?: 0,
                                        )
                                    },
                                    onReplyClick = {
                                        listener.onViewFeed(
                                            anchorView, data.id, data.userInfo?.uid,
                                            data.userInfo?.username, data.userInfo?.userAvatar,
                                            data.deviceTitle, data.message,
                                            data.dateline?.toString(), null, true,
                                        )
                                    },
                                    onMoreClick = {
                                        showFeedMenu(context, anchorView, listener, data, index)
                                    },
                                    onLongClick = {
                                        listener.onCopyText(anchorView, data.message)
                                    },
                                    onForwardClick = {
                                        listener.onViewFeed(
                                            anchorView, data.id, data.userInfo?.uid,
                                            data.userInfo?.username, data.userInfo?.userAvatar,
                                            data.deviceTitle, data.message,
                                            data.dateline?.toString(), null, null,
                                        )
                                    },
                                    onHotReplyClick = {
                                        listener.onViewFeed(
                                            anchorView, data.id, data.userInfo?.uid,
                                            data.userInfo?.username, data.userInfo?.userAvatar,
                                            data.deviceTitle, data.message,
                                            data.dateline?.toString(), null, true,
                                        )
                                    },
                                    onForwardSourceClick = {
                                        listener.onViewFeed(
                                            anchorView, data.forwardSourceFeed?.id,
                                            null, null, null, null, null, null, null, null,
                                        )
                                    },
                                )

                                data.entityTemplate == "refreshCard" -> RefreshCard(data)

                                else -> UnsupportedCard(data)
                            }
                        }
                        item {
                            FeedFooter(
                                footerState = footerState,
                                onReload = {
                                    viewModel.isEnd = false
                                    loadMore()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 鍔犺浇涓紙瀵瑰簲鏃?indicator 鍏ㄥ睆鎬侊級 */
@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp))
    }
}

/** 閿欒 / 绌烘€侊紙瀵瑰簲鏃?errorMessage / errorLayout锛?*/
@Composable
private fun MessageContent(
    message: String,
    isError: Boolean,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MiuixTheme.textStyles.body2,
            color = if (isError) MiuixTheme.colorScheme.error
            else MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        TextButton(text = buttonText, onClick = onClick)
    }
}

/** 鍒楄〃搴曢儴鍔犺浇鎬侊紙瀵瑰簲鏃?FooterAdapter锛?*/
@Composable
private fun FeedFooter(
    footerState: FooterState?,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (footerState) {
            FooterState.Loading -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
            is FooterState.LoadingEnd -> Text(
                text = footerState.msg,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )

            is FooterState.LoadingError -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = footerState.errMsg,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.error,
                )
                TextButton(text = stringResource(R.string.retry), onClick = onReload)
            }

            else -> {}
        }
    }
}

/** 鍒锋柊鍗★紙瀵瑰簲 item_home_feed_refresh_card.xml锛?*/
@Composable
private fun RefreshCard(data: HomeFeedResponse.Data) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = data.title.orEmpty(),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

/** 鏈敮鎸佺殑鍗＄墖妯℃澘鍗犱綅锛堝搴旀棫 ItemHomeUnsupportedBinding锛?*/
@Composable
private fun UnsupportedCard(data: HomeFeedResponse.Data) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(
                R.string.unsupported_card,
                data.entityTemplate ?: data.entityType.orEmpty(),
            ),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

/**
 * 鍔ㄦ€佸崱鐗囥€屾洿澶氥€嶈彍鍗曪紙澶嶇敤鏃?PopupMenu + PopClickListener + R.menu.feed_reply_menu锛? * 鍒犻櫎 / 涓炬姤 / 淇敼鍙鎬?/ 缃《锛屽彲瑙佹€ц鍒欎笌 AppAdapter.FeedViewHolder.init 鐩稿悓锛? */
private fun showFeedMenu(
    context: Context,
    anchor: View,
    listener: ItemListener,
    data: HomeFeedResponse.Data,
    position: Int,
) {
    val uid = data.uid.orEmpty()
    PopupMenu(context, anchor).apply {
        menuInflater.inflate(R.menu.feed_reply_menu, menu).apply {
            menu.findItem(R.id.copy)?.isVisible = false
            menu.findItem(R.id.delete)?.isVisible = PrefManager.uid == uid
            menu.findItem(R.id.show)?.isVisible = false
            menu.findItem(R.id.report)?.isVisible = PrefManager.isLogin
            menu.findItem(R.id.publishStatus)?.isVisible =
                PrefManager.uid == uid && data.entityType == "feed"
            menu.findItem(R.id.stickTop)?.apply {
                isVisible = PrefManager.uid == uid && data.entityType == "feed"
                title = context.getString(
                    if (data.isStickTop == 1) R.string.unstick_top else R.string.stick_top
                )
            }
        }
        setOnMenuItemClickListener(
            PopClickListener(
                listener,
                context,
                data.entityType,
                data.id.orEmpty(),
                uid,
                position,
                data.isStickTop == 1,
            )
        )
        show()
    }
}

/** 鍙戝竷鎸夐挳浜岄€変竴寮圭獥锛堝搴旀棫 HomeFeedFragment.initPublish锛涜烦杞蛋 onPublish 鍥炶皟锛?*/
private fun showPublishDialog(context: Context, onPublish: (String) -> Unit) {
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.publish)
        .setItems(
            arrayOf(
                context.getString(R.string.publishFeed),
                context.getString(R.string.type_article),
            )
        ) { _, which ->
            onPublish(if (which == 0) "createFeed" else "createArticle")
        }
        .show()
}

/**
 * 銆屽叧娉ㄥ垎缁勩€嶅崟閫夊脊绐楋紙瀵瑰簲鏃?HomeFeedFragment.onReturnTop(false)锛? * 閫夋嫨鍚庡啓鍥?PrefManager.FOLLOWTYPE 骞舵竻绌哄垪琛ㄩ噸鏂板姞杞斤級
 */
private fun showFollowGroupDialog(
    context: Context,
    viewModel: HomeFeedViewModel,
    onReload: () -> Unit,
) {
    MaterialAlertDialogBuilder(context).apply {
        setTitle("鍏虫敞鍒嗙粍")
        val items = arrayOf("鍏ㄩ儴鍏虫敞", "濂藉弸鍏虫敞", "璇濋鍏虫敞", "鏁扮爜鍏虫敞", "搴旂敤鍏虫敞")
        viewModel.position = when (PrefManager.FOLLOWTYPE) {
            "all" -> 0
            "circle" -> 1
            "topic" -> 2
            "product" -> 3
            "apk" -> 4
            else -> 0
        }
        setSingleChoiceItems(items, viewModel.position ?: 0) { dialog, position ->
            when (position) {
                0 -> {
                    viewModel.dataListUrl = "/page?url=V9_HOME_TAB_FOLLOW"
                    viewModel.dataListTitle = "鍏ㄩ儴鍏虫敞"
                    PrefManager.FOLLOWTYPE = "all"
                }

                1 -> {
                    viewModel.dataListUrl = "/page?url=V9_HOME_TAB_FOLLOW&type=circle"
                    viewModel.dataListTitle = "濂藉弸鍏虫敞"
                    PrefManager.FOLLOWTYPE = "circle"
                }

                2 -> {
                    viewModel.dataListUrl = "/page?url=V9_HOME_TAB_FOLLOW&type=topic"
                    viewModel.dataListTitle = "璇濋鍏虫敞"
                    PrefManager.FOLLOWTYPE = "topic"
                }

                3 -> {
                    viewModel.dataListUrl = "/page?url=V9_HOME_TAB_FOLLOW&type=product"
                    viewModel.dataListTitle = "鏁扮爜鍏虫敞"
                    PrefManager.FOLLOWTYPE = "product"
                }

                4 -> {
                    viewModel.dataListUrl = "/page?url=V9_HOME_TAB_FOLLOW&type=apk"
                    viewModel.dataListTitle = "搴旂敤鍏虫敞"
                    PrefManager.FOLLOWTYPE = "apk"
                }
            }
            viewModel.dataList.value = emptyList()
            viewModel.footerState.value = FooterState.LoadingDone
            onReload()
            dialog.dismiss()
        }
        show()
    }
}
