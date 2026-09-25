package com.example.c001apk.ui.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.livedata.observeAsState
import com.bumptech.glide.Glide
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.constant.Constants.LOADING_EMPTY
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.util.ClipboardUtil
import com.example.c001apk.util.DateUtils
import com.example.c001apk.util.PrefManager
import com.example.c001apk.util.Utils.downloadApk
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 搴旂敤璇︽儏椤碉紙Compose 鐗堬級锛屽搴旇€佷唬鐮侊細
 * - `AppActivity`锛堢姸鎬佹満锛歀oading / LoadingDone / LoadingError / LoadingFailed锛? * - `AppFragment` + `res/layout/base_view_app.xml`锛堝ご閮細logo / 鍚嶇О / 鐗堟湰 / 澶у皬 / 鏇存柊鏃堕棿 / 涓嬭浇鎸夐挳锛? * - `AppFragment.initBar()` 鑿滃崟锛氭悳绱€佸叧娉?鍙栨秷鍏虫敞銆佸姞鍏?绉婚櫎榛戝悕鍗? * - `AppFragment.initFab()`锛氱櫥褰曞悗鍙戣〃鍥炲锛坱ype=createFeed, targetType=apk锛? *
 * 鏁版嵁鍏ㄩ儴璧扮幇鏈?[AppViewModel]锛圠iveData 鐢?observeAsState 妗ユ帴锛宎ppData/tabList/errMsg
 * 涓烘櫘閫氬瓧娈碉紝鍦?activityState 鍙樺寲瑙﹀彂鐨勯噸缁勪腑璇诲彇锛夛紝涓嶅湪 Composable 閲屾柊寤轰笟鍔￠€昏緫銆? * 鍥剧墖缁х画璧?Glide锛圓ndroidView 鍖?ImageView锛夛紝涓嶅紩鍏ユ柊鍥剧墖搴撱€? *
 * @param viewModel 澶嶇敤鐜版湁 [AppViewModel]锛堢敱 AppActivity 鐨?Hilt Factory 鍒涘缓锛夈€? * @param isLogin 鏄惁鐧诲綍锛屽喅瀹氥€屽叧娉ㄣ€嶈彍鍗曚笌 FAB 鏄惁鏄剧ず锛堥粯璁ゅ彇 PrefManager.isLogin锛夈€? * @param onBack 杩斿洖锛堣€佷唬鐮?activity.finish()锛夈€? * @param onSearch 鐐瑰嚮鎼滅储鑿滃崟锛岃烦杞?SearchActivity(pageType=apk, pageParam=appId)銆? * @param onLogoClick 鐐瑰嚮搴旂敤鍥炬爣锛屽ぇ鍥鹃瑙堬紙鑰佷唬鐮?ImageUtil.startBigImgViewSimple / Mojito锛夈€? * @param onWriteReply 鐐瑰嚮 FAB 鍙戣〃鍥炲锛宼argetId = 1000000000 + appId銆? * @param onDownloadStart 涓嬭浇瀹炵幇鍥炶皟锛涗负 null 鏃跺唴缃€佷唬鐮佺殑 downloadApk 鈫?ACTION_VIEW 鈫?澶嶅埗閾炬帴鍏滃簳閾俱€? * @param onRetry 鍔犺浇澶辫触鍚庣殑閲嶈瘯銆? * @param tabContent 璇勮 tab 鍐呭妲戒綅锛堣€佷唬鐮?AppContentFragment 涓変釜 pager锛夛紝
 *   榛樿鍗犱綅锛岀敱鍚庣画鎺ョ嚎浼犲叆 Compose 鐗堣瘎璁哄垪琛ㄣ€? */
@Composable
fun AppDetailScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    isLogin: Boolean = PrefManager.isLogin,
    onBack: () -> Unit = {},
    onSearch: (appId: String?, title: String?) -> Unit = { _, _ -> },
    onLogoClick: (logoUrl: String) -> Unit = {},
    onWriteReply: (targetId: String) -> Unit = {},
    onDownloadStart: ((url: String, fileName: String) -> Unit)? = null,
    onRetry: () -> Unit = {
        viewModel.activityState.value = LoadingState.Loading
        viewModel.fetchAppInfo()
    },
    tabContent: @Composable (index: Int, type: String) -> Unit = { _, _ -> AppDetailTabPlaceholder() },
) {
    val context = LocalContext.current

    // LiveData 鈫?Compose 鐘舵€佹ˉ鎺?    val activityState by viewModel.activityState.observeAsState()
    val followEvent by viewModel.followState.observeAsState()
    val blockEvent by viewModel.blockState.observeAsState()
    val downloadEvent by viewModel.download.observeAsState()
    val toastEvent by viewModel.toastText.observeAsState()

    // appData / tabList / errMsg 鏄櫘閫氬瓧娈碉紝activityState 鍙樺寲浼氳Е鍙戦噸缁勫苟璇诲埌鏈€鏂板€?    val appData = viewModel.appData
    val tabList = viewModel.tabList
    val errMsg = viewModel.errMsg

    // 鑿滃崟 / 寮圭獥鐨勭晫闈㈢姸鎬侊紙鑰佷唬鐮佺敱 followState / blockState 浜嬩欢椹卞姩锛?    var follow by remember { mutableStateOf(appData?.userAction?.follow ?: 0) }
    var isBlocked by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // 鑰佷唬鐮?AppFragment.onDownload()锛歞ownloadApk 澶辫触 鈫?ACTION_VIEW 鈫?澶嶅埗閾炬帴
    fun startDownload() {
        val url = viewModel.downloadUrl ?: return
        val data = viewModel.appData
        val fileName = "${data?.title}-${data?.apkversionname}-${data?.apkversioncode}.apk"
        val handler = onDownloadStart
        if (handler != null) {
            handler(url, fileName)
        } else {
            try {
                downloadApk(context, url, fileName)
            } catch (e: Exception) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e2: ActivityNotFoundException) {
                    Toast.makeText(context, "涓嬭浇澶辫触", Toast.LENGTH_SHORT).show()
                    ClipboardUtil.copyText(context, url)
                    e2.printStackTrace()
                }
            }
        }
    }

    LaunchedEffect(followEvent) {
        followEvent?.getContentIfNotHandledOrReturnNull()?.let { follow = it }
    }
    LaunchedEffect(blockEvent) {
        blockEvent?.getContentIfNotHandledOrReturnNull()?.let { isBlocked = it }
    }
    LaunchedEffect(downloadEvent) {
        downloadEvent?.getContentIfNotHandledOrReturnNull()?.let { handled ->
            if (handled) startDownload()
        }
    }
    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let { text ->
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    val followLabel = if (follow == 1) "鍙栨秷鍏虫敞" else "鍏虫敞"
    val blockLabel = if (isBlocked) "绉婚櫎榛戝悕鍗? else "鍔犲叆榛戝悕鍗?

    Scaffold(
        modifier = modifier,
        topBar = {
            SmallTopAppBar(
                title = appData?.title ?: "搴旂敤璇︽儏",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "杩斿洖",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
                actions = {
                    if (tabList != null) {
                        IconButton(onClick = { onSearch(viewModel.appId, appData?.title) }) {
                            Icon(
                                imageVector = MiuixIcons.Search,
                                contentDescription = "鎼滅储",
                                tint = MiuixTheme.colorScheme.onBackground,
                            )
                        }
                    }
                    if (appData != null && isLogin && appData.entityType == "apk") {
                        TextButton(
                            text = followLabel,
                            onClick = {
                                viewModel.onGetFollowApk(
                                    if (follow == 1) "/v6/apk/unFollow" else "/v6/apk/follow",
                                    null,
                                    viewModel.appId,
                                )
                            },
                        )
                    }
                    if (appData != null) {
                        TextButton(
                            text = blockLabel,
                            onClick = { showBlockDialog = true },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (isLogin && tabList != null) {
                FloatingActionButton(
                    onClick = {
                        onWriteReply("${1000000000 + (viewModel.appId?.toIntOrNull() ?: 4599)}")
                    },
                ) {
                    Icon(
                        imageVector = MiuixIcons.Add,
                        contentDescription = "鍙戣〃鍥炲",
                        tint = MiuixTheme.colorScheme.onPrimary,
                    )
                }
            }
        },
    ) { paddingValues ->
        val state = activityState
        val data = appData
        if (state is LoadingState.LoadingError) {
            AppDetailMessage(
                msg = state.errMsg,
                buttonText = "閲嶈瘯",
                onClick = onRetry,
                modifier = Modifier.padding(paddingValues),
            )
        } else if (state is LoadingState.LoadingFailed) {
            AppDetailMessage(
                msg = state.msg,
                buttonText = if (state.msg == LOADING_EMPTY) "鍒锋柊" else "閲嶈瘯",
                onClick = onRetry,
                modifier = Modifier.padding(paddingValues),
            )
        } else if (state == LoadingState.Loading || data == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            AppDetailContent(
                appData = data,
                tabList = tabList,
                errMsg = errMsg,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onLogoClick = onLogoClick,
                onVersionClick = { showChangelog = true },
                onDownloadClick = {
                    if (viewModel.downloadUrl.isNullOrEmpty())
                        viewModel.onGetDownloadLink()
                    else
                        startDownload()
                },
                tabContent = tabContent,
                paddingValues = paddingValues,
            )
        }
    }

    // 鏇存柊鏃ュ織寮圭獥锛堣€佷唬鐮侊細鐐瑰嚮鐗堟湰鍙?鈫?MaterialAlertDialogBuilder锛?    val changelog = appData?.changelog
    if (changelog != null) {
        OverlayDialog(
            show = showChangelog,
            title = "鏇存柊鏃ュ織",
            summary = changelog,
            onDismissRequest = { showChangelog = false },
        ) {
            TextButton(
                text = "纭畾",
                onClick = { showChangelog = false },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // 榛戝悕鍗曠‘璁ゅ脊绐楋紙鑰佷唬鐮侊細MaterialAlertDialogBuilder锛?    val title = appData?.title
    if (title != null) {
        OverlayDialog(
            show = showBlockDialog,
            title = "纭畾灏?$title $blockLabel锛?,
            onDismissRequest = { showBlockDialog = false },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = "鍙栨秷",
                    onClick = { showBlockDialog = false },
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    text = "纭畾",
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        showBlockDialog = false
                        if (isBlocked) {
                            viewModel.deleteTopic(title)
                            isBlocked = false
                        } else {
                            viewModel.saveTopic(title)
                            isBlocked = true
                        }
                    },
                )
            }
        }
    }
}

/** 璇勮 tab 绫诲瀷锛堣€佷唬鐮?AppFragment.typeList锛?*/
private val APP_COMMENT_TYPES = listOf("reply", "pub", "hot")

@Composable
private fun AppDetailContent(
    appData: HomeFeedResponse.Data,
    tabList: List<String>?,
    errMsg: String?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onLogoClick: (String) -> Unit,
    onVersionClick: () -> Unit,
    onDownloadClick: () -> Unit,
    tabContent: @Composable (index: Int, type: String) -> Unit,
    paddingValues: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState()),
    ) {
        AppDetailHeader(
            appData = appData,
            onLogoClick = onLogoClick,
            onVersionClick = onVersionClick,
            onDownloadClick = onDownloadClick,
        )

        val intro = appData.description?.takeIf { it.isNotBlank() }
            ?: appData.intro?.takeIf { it.isNotBlank() }
        if (intro != null) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            Text(
                text = "搴旂敤浠嬬粛",
                style = MiuixTheme.textStyles.subtitle,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = intro,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        val score = appData.starAverageScore?.takeIf { it.isNotBlank() }
        if (score != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "璇勫垎 $score 鍒? +
                    (appData.starTotalCount?.takeIf { it.isNotBlank() }?.let { "锛?it 浜鸿瘎鍒嗭級" } ?: ""),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!tabList.isNullOrEmpty()) {
            TabRow(
                tabs = tabList,
                selectedTabIndex = selectedTab.coerceIn(0, tabList.lastIndex),
                onTabSelected = onTabSelected,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            tabContent(selectedTab, APP_COMMENT_TYPES.getOrElse(selectedTab) { "" })
        } else if (!errMsg.isNullOrEmpty()) {
            // 鑰佷唬鐮侊細璇勮鍏抽棴鏃朵笉鏄剧ず tab锛屽彧鏄剧ず commentStatusText
            Text(
                text = errMsg,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            )
        }
    }
}

/** 澶撮儴锛歭ogo / 鍚嶇О / 鐗堟湰 / 澶у皬 / 鏇存柊鏃堕棿 / 涓嬭浇鎸夐挳锛堝搴?base_view_app.xml锛?*/
@Composable
private fun AppDetailHeader(
    appData: HomeFeedResponse.Data,
    onLogoClick: (String) -> Unit,
    onVersionClick: () -> Unit,
    onDownloadClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val logo = appData.logo
        AppLogoImage(
            url = logo,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = logo != null) { logo?.let(onLogoClick) },
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appData.title.orEmpty(),
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(5.dp))
            val versionModifier = if (appData.changelog != null)
                Modifier.clickable(onClick = onVersionClick)
            else Modifier
            Text(
                text = "鐗堟湰: ${appData.version.orEmpty()}(${appData.apkversioncode.orEmpty()})",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = versionModifier,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "澶у皬: ${appData.apksize.orEmpty()}",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "鏇存柊鏃堕棿: " + (appData.lastupdate?.let { DateUtils.fromToday(it) } ?: "null"),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        if (appData.entityType == "apk") {
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = onDownloadClick,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text("涓嬭浇")
            }
        }
    }
}

/** Glide 鍔犺浇缃戠粶 logo锛圓ndroidView 鍖?ImageView锛岃 CONVENTIONS.md 绗?4 鑺傦級 */
@Composable
private fun AppLogoImage(
    url: String?,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
        },
        update = { imageView ->
            Glide.with(imageView).load(url).into(imageView)
        },
    )
}

@Composable
private fun AppDetailMessage(
    msg: String,
    buttonText: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = msg,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
        )
        if (buttonText != null && onClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onClick) {
                Text(buttonText)
            }
        }
    }
}

/** 璇勮 tab 鍐呭榛樿鍗犱綅锛堝悗缁敱鎺ョ嚎鏂逛紶鍏?AppContentFragment 鐨?Compose 鐗堝垪琛級 */
@Composable
private fun AppDetailTabPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "璇勮鍒楄〃锛圓ppContentFragment 鐨?Compose 鐗堬級鐢卞悗缁帴绾挎彁渚?,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
        )
    }
}
