package com.example.c001apk.ui.messagedetail

import android.widget.ImageView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.c001apk.R
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.constant.Constants.LOADING_EMPTY
import com.example.c001apk.logic.model.MessageResponse
import com.example.c001apk.ui.message.MessageFooterRow
import com.example.c001apk.ui.message.MessageNav
import com.example.c001apk.ui.message.htmlToPlainText
import com.example.c001apk.ui.others.CopyActivity
import com.example.c001apk.util.DateUtils
import com.example.c001apk.util.ImageUtil
import com.example.c001apk.util.IntentUtil
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 娑堟伅璇︽儏椤碉紙杩佺Щ ui/messagedetail/ 涓?MessageActivity + MessageFragment +
 * MessageContentFragment + MessageContentAdapter 鐨勭晫闈笌浜や簰锛夈€? *
 * 涓夌鏉＄洰褰㈡€侊紙MessageContentAdapter.getItemViewType锛夛細
 * - atMe / atCommentMe / feedLike -> [MessageContentItem]锛坕tem_message_content.xml锛? * - contactsFollow / list锛堢淇★級 -> [MessageUserItem]锛坕tem_message_user.xml锛? *
 * 鏁版嵁娴佸鐢?[MessageViewModel]锛圓ssistedInject锛宼ype 鍐冲畾鎺ュ彛 url锛夈€? * 鏍囬鏄犲皠娌跨敤 MessageFragment.initBar銆? */
@Composable
fun MessageDetailScreen(
    type: String,
    viewModelFactory: MessageViewModel.Factory,
    onBack: (() -> Unit)? = null,
    onOpenUser: ((uid: String?) -> Unit)? = null,
    onOpenFeed: ((id: String?, rid: String?, viewReply: Boolean?) -> Unit)? = null,
    onCopyText: ((text: String?) -> Unit)? = null,
) {
    val context = LocalContext.current

    val openUser: (String?) -> Unit = onOpenUser ?: { uid -> MessageNav.viewUser(context, uid) }
    val openFeed: (String?, String?, Boolean?) -> Unit =
        onOpenFeed ?: { id, rid, viewReply -> MessageNav.viewFeed(context, id, rid, viewReply) }
    val copyText: (String?) -> Unit = onCopyText ?: { text ->
        IntentUtil.startActivity<CopyActivity>(context) {
            putExtra("text", text)
        }
    }

    val factory = remember(type) { MessageViewModel.provideFactory(viewModelFactory, type) }
    val viewModel: MessageViewModel = viewModel(factory = factory)

    // ---------------- 鐘舵€佹ˉ鎺ワ紙LiveData -> Compose锛?----------------
    val messageList by viewModel.messageListData.observeAsState(emptyList())
    val footerState by viewModel.footerState.observeAsState()
    val loadingState by viewModel.loadingState.observeAsState()

    var refreshing by remember { mutableStateOf(false) }

    val title = remember(type) {
        when (type) {
            "atMe" -> "@鎴戠殑鍔ㄦ€?
            "atCommentMe" -> "@鎴戠殑璇勮"
            "feedLike" -> "鎴戞敹鍒扮殑璧?
            "contactsFollow" -> "濂藉弸鍏虫敞"
            "list" -> "绉佷俊"
            else -> ""
        }
    }

    // 棣栨杩涘叆锛氱瓑浠蜂簬 BaseViewFragment.initData + refreshData
    LaunchedEffect(Unit) {
        if (viewModel.messageListData.value.isNullOrEmpty()) {
            viewModel.lastItem = null
            viewModel.page = 1
            viewModel.isEnd = false
            viewModel.isRefreshing = true
            viewModel.isLoadMore = false
            viewModel.fetchData()
        }
    }

    LaunchedEffect(footerState) {
        if (footerState != null && footerState !is FooterState.Loading) refreshing = false
    }
    LaunchedEffect(loadingState) {
        if (loadingState !is LoadingState.Loading) refreshing = false
    }

    // 鍔犺浇鏇村锛氭粴鍒版渶鍚庝竴鏉℃椂瑙﹀彂锛圔aseViewFragment.initScroll 鐨?loadMore 鍒ゆ柇锛?    val listState = rememberLazyListState()
    val atListEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 1
        }
    }
    LaunchedEffect(atListEnd) {
        if (atListEnd && !viewModel.isEnd && !viewModel.isRefreshing && !viewModel.isLoadMore
            && !refreshing
        ) {
            viewModel.isLoadMore = true
            viewModel.fetchData()
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = { onBack?.invoke() }) {
                        Icon(MiuixIcons.Back, contentDescription = "杩斿洖")
                    }
                }
            )
        }
    ) { paddingValues ->
        val isLoading = loadingState is LoadingState.Loading || loadingState == null
        val failed = loadingState as? LoadingState.LoadingFailed
        val error = loadingState as? LoadingState.LoadingError

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            when {
                // 绌烘€?/ 澶辫触鎬侊紙BaseViewFragment 鐨?errorLayout锛?                failed != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = failed.msg,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            text = if (failed.msg == LOADING_EMPTY) "鍒锋柊" else "閲嶈瘯",
                            onClick = {
                                viewModel.lastItem = null
                                viewModel.page = 1
                                viewModel.isEnd = false
                                viewModel.isRefreshing = true
                                viewModel.isLoadMore = false
                                viewModel.fetchData()
                            }
                        )
                    }
                }

                else -> {
                    PullToRefresh(
                        isRefreshing = refreshing,
                        onRefresh = {
                            if (!viewModel.isLoadMore) {
                                refreshing = true
                                viewModel.lastItem = null
                                viewModel.page = 1
                                viewModel.isEnd = false
                                viewModel.isRefreshing = true
                                viewModel.isLoadMore = false
                                viewModel.fetchData()
                            } else {
                                refreshing = false
                            }
                        },
                        pullToRefreshState = rememberPullToRefreshState(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 10.dp,
                                end = 10.dp,
                                top = 10.dp,
                                bottom = paddingValues.calculateBottomPadding() + 10.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 椤堕儴閿欒妯箙锛圔aseViewFragment 鐨?errorMessage锛?                            if (error != null) {
                                item(key = "error") {
                                    Text(
                                        text = error.errMsg,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        style = MiuixTheme.textStyles.footnote1,
                                        color = MiuixTheme.colorScheme.error
                                    )
                                }
                            }

                            // 棣栧睆鍔犺浇涓?                            if (isLoading && messageList.isEmpty()) {
                                item(key = "loading") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 60.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                    }
                                }
                            }

                            if (messageList.isEmpty() && !isLoading && error == null) {
                                item(key = "empty") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 30.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = LOADING_EMPTY,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                        )
                                    }
                                }
                            }

                            itemsIndexed(
                                items = messageList,
                                key = { _, item -> item.id }
                            ) { _, data ->
                                when (type) {
                                    "contactsFollow", "list" -> MessageUserItem(
                                        data = data,
                                        type = type,
                                        onClick = { openUser(data.uid) }
                                    )

                                    "feedLike" -> MessageContentItem(
                                        data = data,
                                        type = type,
                                        onOpenUser = openUser,
                                        onOpenFeed = openFeed,
                                        onCopyText = copyText
                                    )

                                    else -> MessageContentItem(
                                        data = data,
                                        type = type,
                                        onOpenUser = openUser,
                                        onOpenFeed = openFeed,
                                        onCopyText = copyText,
                                        onItemClick = {
                                            MessageNav.viewFeed(context, data.id, null, null)
                                        }
                                    )
                                }
                            }

                            item(key = "footer") {
                                MessageFooterRow(
                                    footerState = footerState,
                                    onRetry = {
                                        viewModel.isEnd = false
                                        viewModel.isLoadMore = true
                                        viewModel.fetchData()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 鏉＄洰

/**
 * 鍔ㄦ€佺被娑堟伅鏉＄洰锛坕tem_message_content.xml + MessageContentAdapter.MessageViewHolder锛夛細
 * 澶村儚 / 鐢ㄦ埛鍚?/ 鏈哄瀷 / 姝ｆ枃 / 杞彂婧愬崱鐗?/ 琚瘎鍔ㄦ€佸崱鐗?/ 鏃堕棿 + 鍥炲鏁般€? */
@Composable
private fun MessageContentItem(
    data: MessageResponse.Data,
    type: String,
    onOpenUser: (String?) -> Unit,
    onOpenFeed: (String?, String?, Boolean?) -> Unit,
    onCopyText: (String?) -> Unit,
    onItemClick: (() -> Unit)? = null
) {
    val isFeedLike = type == "feedLike"
    val uid = if (isFeedLike) data.likeUid else data.uid
    val uname = if (isFeedLike) data.likeUsername else data.username
    val avatar = if (isFeedLike) data.likeAvatar else data.userAvatar
    val time = if (isFeedLike) data.likeTime else data.dateline
    val messageText = remember(data, type) {
        if (isFeedLike) "璧炰簡浣犵殑" + htmlToPlainText(data.infoHtml)
        else htmlToPlainText(data.message)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onItemClick != null) Modifier.clickable(onClick = onItemClick) else Modifier)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlideThumb(
                    url = avatar,
                    size = 40.dp,
                    shape = CircleShape,
                    onClick = { onOpenUser(uid) }
                )
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(
                        text = uname,
                        style = MiuixTheme.textStyles.main,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!data.deviceTitle.isNullOrEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_device),
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = data.deviceTitle,
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (messageText.isNotEmpty()) {
                Text(
                    text = messageText,
                    modifier = Modifier.padding(top = 10.dp),
                    style = MiuixTheme.textStyles.body1
                )
            }

            // 杞彂婧愬姩鎬佸崱鐗囷紙forwardSourceFeed锛?            data.forwardSourceFeed?.let { forward ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clickable { onOpenFeed(forward.id, null, null) },
                    cornerRadius = 12.dp,
                    insideMargin = PaddingValues(10.dp)
                ) {
                    val forwardTitle = remember(forward) { htmlToPlainText(forward.messageTitle) }
                    if (forwardTitle.isNotEmpty()) {
                        Text(text = forwardTitle, style = MiuixTheme.textStyles.body1)
                    }
                    val forwardMessage = remember(forward) { htmlToPlainText(forward.message) }
                    if (forwardMessage.isNotEmpty()) {
                        Text(
                            text = forwardMessage,
                            modifier = Modifier.padding(top = 10.dp),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                    val pics = forward.picArr.orEmpty().filter { it.isNotEmpty() }
                    if (pics.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            pics.take(3).forEach { pic ->
                                GlideThumb(
                                    url = pic,
                                    size = 60.dp,
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = { onCopyText(forward.message) }
                                )
                            }
                        }
                    }
                }
            }

            // 琚瘎 / 琚禐鐨勫姩鎬佸崱鐗囷紙feed锛?            data.feed?.let { feed ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clickable {
                            onOpenFeed(
                                if (isFeedLike) data.fid else feed.id,
                                data.id,
                                true
                            )
                        },
                    cornerRadius = 12.dp,
                    insideMargin = PaddingValues(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlideThumb(
                            url = feed.pic,
                            size = 60.dp,
                            shape = RoundedCornerShape(8.dp)
                        )
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(
                                text = if (isFeedLike) data.username else feed.username,
                                style = MiuixTheme.textStyles.footnote1,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val feedMessage = remember(feed, type) {
                                if (isFeedLike) htmlToPlainText(data.message)
                                else htmlToPlainText(feed.message)
                            }
                            if (feedMessage.isNotEmpty()) {
                                Text(
                                    text = feedMessage,
                                    modifier = Modifier.padding(top = 5.dp),
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 鏃堕棿 + 鍥炲鏁帮紙likeData 鍦ㄨ閫傞厤鍣ㄩ噷鎭掍负 null锛屾晠涓嶆樉绀虹偣璧炴暟锛?            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_date),
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = DateUtils.fromToday(time),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Spacer(modifier = Modifier.weight(1f))
                if (data.replynum.isNotEmpty()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_message),
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = data.replynum,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }
        }
    }
}

/**
 * 鐢ㄦ埛绫绘秷鎭潯鐩紙item_message_user.xml + MessageContentAdapter.UserViewHolder锛夛細
 * 濂藉弸鍏虫敞銆屽叧娉ㄤ簡浣犮€? 绉佷俊鍒楄〃锛岀偣鍑昏繘瀵规柟涓婚〉銆? */
@Composable
private fun MessageUserItem(
    data: MessageResponse.Data,
    type: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(10.dp)) {
            GlideThumb(
                url = data.fromUserAvatar,
                size = 40.dp,
                shape = CircleShape
            )
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    text = data.fromusername,
                    style = MiuixTheme.textStyles.main,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateUtils.fromToday(data.dateline),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    if (type == "contactsFollow") {
                        Text(
                            text = "鍏虫敞浜嗕綘",
                            modifier = Modifier.padding(start = 5.dp),
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
            }
        }
    }
}

/** Glide 閾捐矾缂╃暐鍥?/ 澶村儚锛堜笉寮曞叆鏂板浘鐗囧簱锛岃 CONVENTIONS 搂4锛?*/
@Composable
private fun GlideThumb(
    url: String?,
    size: Dp,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: (() -> Unit)? = null
) {
    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView -> ImageUtil.showIMG(imageView, url) },
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    )
}
