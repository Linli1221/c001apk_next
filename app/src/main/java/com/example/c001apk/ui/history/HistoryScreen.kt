package com.example.c001apk.ui.history

import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.livedata.observeAsState
import com.example.c001apk.R
import com.example.c001apk.logic.model.HitHistoryData
import com.example.c001apk.util.DateUtils
import com.example.c001apk.util.ImageUtil
import com.example.c001apk.util.Utils.richToString
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.HorizontalDivider
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
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 娴忚鍘嗗彶椤甸潰锛堥叿瀹変簯绔級鈥斺€?瀵瑰簲鑰佺殑 [HistoryActivity] + [HistoryAdapter]锛坕tem_history_cloud.xml锛夈€? *
 * 鏁版嵁娴佸畬鍏ㄥ鐢?[HistoryViewModel]锛圠iveData 鐢?observeAsState 妗ユ帴锛夛紝鏈枃浠跺彧璐熻矗鐣岄潰涓庝氦浜掞細
 * 缂╃暐鍥撅紙logo锛? 鏍囬 / 鎻忚堪 / 绫诲瀷路鏃堕棿锛屼笅鎷夊埛鏂般€佹粴鍒板簳鑷姩鍔犺浇鏇村銆佽繘鍏ラ〉闈㈣嚜鍔ㄦ媺鍙栵紝
 * 鍙充笂瑙掋€屾竻绌恒€嶅叆鍙ｏ紝娓呯┖鍓嶇敤 Miuix [OverlayDialog] 纭銆? *
 * 娉ㄦ剰锛? * - 涓嶈嚜甯?MiuixAppTheme锛堟牴涓婚鐢?Activity 鎺ョ嚎鏃跺锛夛紱椤甸潰鑷惈 Scaffold锛圤verlayDialog 闇€瑕?Scaffold 绁栧厛锛夈€? * - [HistoryViewModel] 鏄?`@HiltViewModel`锛岀敱 Activity 渚?`viewModels()` 鍒涘缓鍚庝紶鍏ャ€? * - [onClearHistory] 鏄彁鍗囧嚭鏉ョ殑娓呯┖鍥炶皟锛氱幇鏈?[HistoryViewModel] 鍙湁 `GET /v6/user/hitHistoryList`
 *   璇诲彇鎺ュ彛锛堜簯绔棤鍒犻櫎 / 娓呯┖鎺ュ彛锛岃€佷唬鐮佷篃娌℃湁娓呯┖鍏ュ彛锛夛紝娓呯┖钀藉湴閫昏緫鐢辨帴绾挎柟鍐冲畾
 *   锛堝彲鍏堟竻鏈湴 Room 鍘嗗彶 `HistoryFavoriteRepo.deleteAllHistory()` + 鏈湴闅愯棌锛岃鎶ュ憡锛夈€? * - 鍥剧墖娌跨敤 Glide 閾捐矾锛圛mageUtil.showIMG + AndroidView 鍖?ImageView锛夛紝鏈紩鍏ユ柊鍥剧墖搴撱€? */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onOpenHistory: (HitHistoryData) -> Unit,
    onClearHistory: () -> Unit = {},
    onToast: (String) -> Unit = {},
) {
    val historyList by viewModel.historyList.observeAsState(emptyList())
    val initialLoading by viewModel.initialLoading.observeAsState(false)
    val toastEvent by viewModel.toastText.observeAsState()
    val list = historyList.orEmpty()

    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    var refreshing by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    // 杩涘叆椤甸潰鎷夊彇锛堣€?HistoryActivity.onCreate 鐩存帴 viewModel.refresh()锛?    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    // 涓嬫媺鍒锋柊鎸囩ず涓?VM 鐨勯灞忓姞杞芥寚绀轰繚鎸佸悓姝?    LaunchedEffect(initialLoading) {
        refreshing = initialLoading == true
    }

    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let { onToast(it) }
    }

    // 婊氬埌搴曡嚜鍔ㄥ姞杞芥洿澶氾紙HistoryViewModel.loadMore 鑷甫 isEnd / isLoading 瀹堝崼锛?    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to
                listState.layoutInfo.totalItemsCount
        }.collect { (last, total) ->
            if (last != null && total > 0 && last >= total - 1) {
                viewModel.loadMore()
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.history),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "杩斿洖")
                    }
                },
                actions = {
                    // 娓呯┖鍏ュ彛锛堣€侀〉闈㈡病鏈夛紱鏂伴渶姹傚姞鐨勶級锛岀‘璁ゅ悗璧?onClearHistory 鍥炶皟
                    TextButton(
                        text = "娓呯┖",
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.textButtonColors(
                            textColor = MiuixTheme.colorScheme.error,
                        ),
                    )
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                refreshing && list.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                list.isEmpty() -> {
                    Text(
                        text = "鏆傛棤娴忚鍘嗗彶",
                        modifier = Modifier.align(Alignment.Center),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }

                else -> {
                    PullToRefresh(
                        isRefreshing = refreshing,
                        onRefresh = {
                            refreshing = true
                            viewModel.refresh()
                        },
                        pullToRefreshState = pullToRefreshState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                        ) {
                            items(list) { item ->
                                HistoryRow(
                                    item = item,
                                    onClick = { onOpenHistory(item) },
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 74.dp),
                                )
                            }
                            if (viewModel.isEnd) {
                                item {
                                    Text(
                                        text = "娌℃湁鏇村浜?,
                                        style = MiuixTheme.textStyles.footnote2,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 娓呯┖纭瀵硅瘽妗嗭紙OverlayDialog 闇€瑕?Scaffold 绁栧厛锛屾斁鍦?Scaffold content 鍐咃級
            OverlayDialog(
                title = "娓呯┖娴忚鍘嗗彶",
                summary = "纭畾娓呯┖鍏ㄩ儴娴忚鍘嗗彶鍚楋紵",
                show = showClearConfirm,
                onDismissRequest = { showClearConfirm = false },
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = { showClearConfirm = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    TextButton(
                        text = "娓呯┖",
                        onClick = {
                            showClearConfirm = false
                            onClearHistory()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            textColor = MiuixTheme.colorScheme.error,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * 涓€琛屾祻瑙堝巻鍙诧紙瀵瑰簲 item_history_cloud.xml锛夛細
 * logo锛?6dp锛屾棤 logo 鏃堕殣钘忥級+ 鏍囬锛? 琛岋級+ 鎻忚堪锛? 琛岋紝绌哄垯闅愯棌锛? 绫诲瀷 路 鏃堕棿銆? * 鏈嶅姟绔?title/description 甯?HTML 鏍囩锛屾部鐢?[richToString] 杞函鏂囨湰銆? */
@Composable
private fun HistoryRow(
    item: HitHistoryData,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val logo = item.logo
        if (!logo.isNullOrEmpty()) {
            HistoryLogo(url = logo)
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title.orEmpty().richToString(),
                style = MiuixTheme.textStyles.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val descText = item.description.orEmpty().richToString().trim()
            if (descText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = descText,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = buildString {
                    if (!item.typeName.isNullOrEmpty()) append(item.typeName)
                    item.dateline?.let {
                        if (isNotEmpty()) append(" 路 ")
                        append(DateUtils.fromToday(it))
                    }
                },
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 鍘嗗彶鏉＄洰缂╃暐鍥撅細Glide锛圛mageUtil.showIMG锛? AndroidView 鍖?ImageView */
@Composable
private fun HistoryLogo(url: String) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            // url 鍙樺寲鎵嶉噸鏂拌蛋 Glide锛岄伩鍏嶆瘡娆￠噸缁勯兘鍙戣捣鍔犺浇
            if (imageView.tag != url) {
                imageView.tag = url
                ImageUtil.showIMG(imageView, url)
            }
        },
        modifier = Modifier.size(46.dp),
    )
}
