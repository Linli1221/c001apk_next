package com.example.c001apk.ui.dyh

import android.widget.ImageView
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.livedata.observeAsState
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.constant.Constants
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.util.ImageUtil
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * 鐪嬬湅鍙峰崟涓?tab 鐨勫姩鎬佸垪琛紙DyhDetailFragment 鐨?Compose 鐗堬級銆? *
 * 鏁版嵁娴佸叏閮ㄥ鐢ㄧ幇鏈?[DyhViewModel]锛圔aseAppViewModel 鐨?dataList / loadingState /
 * footerState / toastText锛孡iveData 鐢?observeAsState 妗ユ帴锛夛細
 * - 棣栨鍔犺浇 / 閲嶈瘯锛氱疆 loadingState = Loading 瑙﹀彂鍒锋柊锛堝榻?BaseViewFragment.initObserve锛? * - 涓嬫媺鍒锋柊锛歳efreshData() 璇箟锛坙astItem/page/isEnd/isRefreshing/isLoadMore 褰掍綅鍚?fetchData锛? * - 涓婃媺鍔犺浇锛歠ooter 杩涘叆鍙鍖轰笖 !isEnd && !isRefreshing && !isLoadMore 鏃?loadMore
 * - 绌烘€?/ 鍔犺浇澶辫触 / 閿欒娑堟伅涓?footer锛堝姞杞戒腑 / 鍑洪敊閲嶈瘯 / 娌℃湁鏇村浜嗭級閮芥寜鍘熺姸鎬佹覆鏌? *
 * 鍒楄〃椤归粯璁ゆ槸绠€鍖栫殑鍔ㄦ€佸崱鐗囷紙澶村儚 + 鐢ㄦ埛鍚?+ 姝ｆ枃锛夛紝瀹屾暣涔濆鏍?/ 瑙嗛 / 鎶曠エ绛? * 鍔ㄦ€佸崱鐗囩敱鍏叡 feed 鍗＄墖缁勪欢閫氳繃 [itemContent] 鎻掓Ы鏇挎崲锛涚偣鍑诲崰浣嶈蛋 [onItemClick]銆? */
@Composable
fun DyhDetailScreen(
    viewModel: DyhViewModel,
    modifier: Modifier = Modifier,
    onItemClick: (HomeFeedResponse.Data) -> Unit = {},
    itemContent: (@Composable (HomeFeedResponse.Data) -> Unit)? = null,
) {
    val context = LocalContext.current
    val dataList by viewModel.dataList.observeAsState()
    val loadingState by viewModel.loadingState.observeAsState()
    val footerState by viewModel.footerState.observeAsState()
    val toastText by viewModel.toastText.observeAsState()
    val list = dataList.orEmpty()

    // 涓嬫媺鍒锋柊鎸囩ず鍣紙isRefreshing 鎻愬崌鍒版湰鍦帮紝浠讳竴鍔犺浇鎬佺粨鏉熷悗鏀惰捣锛?    var isRefreshing by remember { mutableStateOf(false) }

    fun refresh() {
        viewModel.lastItem = null
        viewModel.page = 1
        viewModel.isEnd = false
        viewModel.isRefreshing = true
        viewModel.isLoadMore = false
        viewModel.fetchData()
    }

    fun loadMore() {
        if (!viewModel.isEnd && !viewModel.isRefreshing && !viewModel.isLoadMore
            && viewModel.listSize > 0
        ) {
            viewModel.isLoadMore = true
            viewModel.fetchData()
        }
    }

    // 涓?BaseAppFragment 涓€鑷达細listSize 鍙備笌 fetchData 閲?loadingState / footerState 鐨勫垎鏀?    LaunchedEffect(list.size) { viewModel.listSize = list.size }

    // 瀵归綈 BaseViewFragment锛歭oadingState = Loading 鏄€屽埛鏂般€嶇殑瑙﹀彂鍣紙棣栨鍔犺浇 / 鍑洪敊閲嶈瘯锛?    LaunchedEffect(loadingState) {
        if (loadingState is LoadingState.Loading && !viewModel.isLoadMore) {
            refresh()
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.dataList.value == null && viewModel.loadingState.value == null) {
            viewModel.loadingState.value = LoadingState.Loading
        }
    }

    LaunchedEffect(loadingState, footerState) {
        if (loadingState !is LoadingState.Loading && footerState !is FooterState.Loading) {
            isRefreshing = false
        }
    }

    LaunchedEffect(toastText) {
        toastText?.getContentIfNotHandledOrReturnNull()?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    PullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (!viewModel.isLoadMore) {
                isRefreshing = true
                refresh()
            }
        },
        modifier = modifier.fillMaxSize(),
        refreshTexts = listOf("涓嬫媺鍒锋柊", "閲婃斁鍒锋柊", "姝ｅ湪鍒锋柊鈥?, "鍒锋柊鎴愬姛"),
    ) {
        val state = loadingState
        when {
            // 棣栧睆鍔犺浇
            state is LoadingState.Loading && list.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            // 绌烘€?/ 鍔犺浇澶辫触锛堝垪琛ㄤ负绌烘椂鍏ㄥ睆灞曠ず锛?            (state is LoadingState.LoadingFailed || state is LoadingState.LoadingError)
                    && list.isEmpty() -> {
                val msg = when (state) {
                    is LoadingState.LoadingFailed -> state.msg
                    is LoadingState.LoadingError -> state.errMsg
                    else -> ""
                }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = msg,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                    )
                    if (state is LoadingState.LoadingFailed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            text = if (msg == Constants.LOADING_EMPTY) "鍒锋柊" else "閲嶈瘯",
                            onClick = { viewModel.loadingState.value = LoadingState.Loading },
                        )
                    }
                }
            }

            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(list) { _, item ->
                        if (itemContent != null) {
                            itemContent(item)
                        } else {
                            DefaultFeedItem(item = item, onItemClick = onItemClick)
                        }
                    }
                    item {
                        LoadMoreFooter(
                            footerState = footerState,
                            onRetry = {
                                viewModel.isEnd = false
                                loadMore()
                            },
                            onVisible = { loadMore() },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 鍒楄〃灏撅紙FooterAdapter 鐨?Compose 鐗堬級锛氬姞杞戒腑 / 鍑洪敊閲嶈瘯 / 娌℃湁鏇村浜嗐€? * 杩涘叆鍙鍖烘椂灏濊瘯鍔犺浇涓嬩竴椤碉紙瀵归綈鍘?RecyclerView 婊氬姩鐩戝惉鐨勮Е搴曞姞杞斤級銆? */
@Composable
private fun LoadMoreFooter(
    footerState: FooterState?,
    onRetry: () -> Unit,
    onVisible: () -> Unit,
) {
    LaunchedEffect(Unit) { onVisible() }

    when (footerState) {
        is FooterState.Loading -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "鍔犺浇涓€?,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        is FooterState.LoadingError -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "鍔犺浇澶辫触锛?{footerState.errMsg}",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                TextButton(text = "閲嶈瘯", onClick = onRetry)
            }
        }

        is FooterState.LoadingEnd -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = footerState.msg,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        else -> Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 榛樿鍔ㄦ€佸崱鐗囷紙鍗犱綅绠€鍖栫増锛夛細澶村儚 + 鐢ㄦ埛鍚?+ 姝ｆ枃銆? * 瀹屾暣鍔ㄦ€佸崱鐗囷紙鍥剧墖涔濆鏍?/ 瑙嗛 / 杞彂 / 鐐硅禐绛夛級寰呭叕鍏?feed 鍗＄墖缁勪欢灏辩华鍚? * 閫氳繃 [DyhDetailScreen] 鐨?itemContent 鎻掓Ы鏇挎崲銆? */
@Composable
private fun DefaultFeedItem(
    item: HomeFeedResponse.Data,
    onItemClick: (HomeFeedResponse.Data) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        insideMargin = PaddingValues(12.dp),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = { onItemClick(item) },
    ) {
        Row {
            GlideImage(
                url = item.userAvatar,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Text(
                    text = item.username.orEmpty(),
                    style = MiuixTheme.textStyles.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val message = item.message ?: item.title.orEmpty()
                if (message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
