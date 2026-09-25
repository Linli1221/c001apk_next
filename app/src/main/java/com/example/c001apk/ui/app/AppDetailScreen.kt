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
import androidx.lifecycle.compose.observeAsState
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
 * 应用详情页（Compose 版），对应老代码：
 * - `AppActivity`（状态机：Loading / LoadingDone / LoadingError / LoadingFailed）
 * - `AppFragment` + `res/layout/base_view_app.xml`（头部：logo / 名称 / 版本 / 大小 / 更新时间 / 下载按钮）
 * - `AppFragment.initBar()` 菜单：搜索、关注/取消关注、加入/移除黑名单
 * - `AppFragment.initFab()`：登录后发表回复（type=createFeed, targetType=apk）
 *
 * 数据全部走现有 [AppViewModel]（LiveData 用 observeAsState 桥接，appData/tabList/errMsg
 * 为普通字段，在 activityState 变化触发的重组中读取），不在 Composable 里新建业务逻辑。
 * 图片继续走 Glide（AndroidView 包 ImageView），不引入新图片库。
 *
 * @param viewModel 复用现有 [AppViewModel]（由 AppActivity 的 Hilt Factory 创建）。
 * @param isLogin 是否登录，决定「关注」菜单与 FAB 是否显示（默认取 PrefManager.isLogin）。
 * @param onBack 返回（老代码 activity.finish()）。
 * @param onSearch 点击搜索菜单，跳转 SearchActivity(pageType=apk, pageParam=appId)。
 * @param onLogoClick 点击应用图标，大图预览（老代码 ImageUtil.startBigImgViewSimple / Mojito）。
 * @param onWriteReply 点击 FAB 发表回复，targetId = 1000000000 + appId。
 * @param onDownloadStart 下载实现回调；为 null 时内置老代码的 downloadApk → ACTION_VIEW → 复制链接兜底链。
 * @param onRetry 加载失败后的重试。
 * @param tabContent 评论 tab 内容槽位（老代码 AppContentFragment 三个 pager），
 *   默认占位，由后续接线传入 Compose 版评论列表。
 */
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

    // LiveData → Compose 状态桥接
    val activityState by viewModel.activityState.observeAsState()
    val followEvent by viewModel.followState.observeAsState()
    val blockEvent by viewModel.blockState.observeAsState()
    val downloadEvent by viewModel.download.observeAsState()
    val toastEvent by viewModel.toastText.observeAsState()

    // appData / tabList / errMsg 是普通字段，activityState 变化会触发重组并读到最新值
    val appData = viewModel.appData
    val tabList = viewModel.tabList
    val errMsg = viewModel.errMsg

    // 菜单 / 弹窗的界面状态（老代码由 followState / blockState 事件驱动）
    var follow by remember { mutableStateOf(appData?.userAction?.follow ?: 0) }
    var isBlocked by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // 老代码 AppFragment.onDownload()：downloadApk 失败 → ACTION_VIEW → 复制链接
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
                    Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show()
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

    val followLabel = if (follow == 1) "取消关注" else "关注"
    val blockLabel = if (isBlocked) "移除黑名单" else "加入黑名单"

    Scaffold(
        modifier = modifier,
        topBar = {
            SmallTopAppBar(
                title = appData?.title ?: "应用详情",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
                actions = {
                    if (tabList != null) {
                        IconButton(onClick = { onSearch(viewModel.appId, appData?.title) }) {
                            Icon(
                                imageVector = MiuixIcons.Search,
                                contentDescription = "搜索",
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
                        contentDescription = "发表回复",
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
                buttonText = "重试",
                onClick = onRetry,
                modifier = Modifier.padding(paddingValues),
            )
        } else if (state is LoadingState.LoadingFailed) {
            AppDetailMessage(
                msg = state.msg,
                buttonText = if (state.msg == LOADING_EMPTY) "刷新" else "重试",
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

    // 更新日志弹窗（老代码：点击版本号 → MaterialAlertDialogBuilder）
    val changelog = appData?.changelog
    if (changelog != null) {
        OverlayDialog(
            show = showChangelog,
            title = "更新日志",
            summary = changelog,
            onDismissRequest = { showChangelog = false },
        ) {
            TextButton(
                text = "确定",
                onClick = { showChangelog = false },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // 黑名单确认弹窗（老代码：MaterialAlertDialogBuilder）
    val title = appData?.title
    if (title != null) {
        OverlayDialog(
            show = showBlockDialog,
            title = "确定将 $title $blockLabel？",
            onDismissRequest = { showBlockDialog = false },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = "取消",
                    onClick = { showBlockDialog = false },
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    text = "确定",
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

/** 评论 tab 类型（老代码 AppFragment.typeList） */
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
                text = "应用介绍",
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
                text = "评分 $score 分" +
                    (appData.starTotalCount?.takeIf { it.isNotBlank() }?.let { "（$it 人评分）" } ?: ""),
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
            // 老代码：评论关闭时不显示 tab，只显示 commentStatusText
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

/** 头部：logo / 名称 / 版本 / 大小 / 更新时间 / 下载按钮（对应 base_view_app.xml） */
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
                text = "版本: ${appData.version.orEmpty()}(${appData.apkversioncode.orEmpty()})",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = versionModifier,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "大小: ${appData.apksize.orEmpty()}",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "更新时间: " + (appData.lastupdate?.let { DateUtils.fromToday(it) } ?: "null"),
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
                Text("下载")
            }
        }
    }
}

/** Glide 加载网络 logo（AndroidView 包 ImageView，见 CONVENTIONS.md 第 4 节） */
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

/** 评论 tab 内容默认占位（后续由接线方传入 AppContentFragment 的 Compose 版列表） */
@Composable
private fun AppDetailTabPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "评论列表（AppContentFragment 的 Compose 版）由后续接线提供",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
        )
    }
}
