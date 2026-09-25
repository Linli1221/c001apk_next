package com.example.c001apk.ui.applist

import android.widget.ImageView
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.observeAsState
import com.bumptech.glide.Glide
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.constant.Constants.LOADING_EMPTY
import com.example.c001apk.logic.model.AppItem
import com.example.c001apk.util.AppUtils
import com.example.c001apk.util.LocalAppIcon
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 应用列表页（Compose 版），对应老代码：
 * - `AppListFragment`（BaseRefreshRecyclerviewBinding：SwipeRefreshLayout + RecyclerView）
 * - `AppListAdapter` + `res/layout/item_app.xml`（图标行：Glide 加载 LocalAppIcon / 名称 / 包名 / 版本）
 *
 * 数据走现有 [AppListViewModel]（items LiveData 用 observeAsState 桥接；getItems(context)
 * 扫描本机已安装应用的逻辑保持不变），列表项图标继续用 Glide（AndroidView 包 ImageView）。
 * 老代码没有筛选 UI，本轮按需求新增纯界面层的关键字过滤与排序（不改 ViewModel）。
 *
 * @param viewModel 复用现有 [AppListViewModel]。
 * @param title 顶栏标题（老代码标题来自首页 tab，独立页面时默认「应用」）。
 * @param onBack 返回；为 null 时不显示返回按钮（作为首页 tab 嵌入时的形态）。
 * @param onAppClick 点击应用行（老代码跳 AppActivity(id=包名)，由接线方实现）。
 * @param onScrollDown 列表向下滚动（老代码隐藏底部导航）。
 * @param onScrollUp 列表向上滚动（老代码显示底部导航）。
 */
@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    modifier: Modifier = Modifier,
    title: String = "应用",
    onBack: (() -> Unit)? = null,
    onAppClick: (AppItem) -> Unit = {},
    onScrollDown: () -> Unit = {},
    onScrollUp: () -> Unit = {},
) {
    val context = LocalContext.current

    // LiveData → Compose 状态桥接
    val items by viewModel.items.observeAsState()
    val loadingState by viewModel.loadingState.observeAsState()

    // 界面层筛选/排序状态（纯展示，不进 ViewModel）
    var keyword by rememberSaveable { mutableStateOf("") }
    var sortByName by rememberSaveable { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // 首次进入自动加载（老代码由宿主 fetchData() 触发；如宿主已触发过则 guard 会跳过）
    LaunchedEffect(Unit) {
        if (viewModel.items.value == null && viewModel.loadingState.value == null) {
            viewModel.getItems(context)
        }
    }

    // 列表刷新完成后收起下拉指示器（老代码在 items observer 里 isRefreshing = false）
    LaunchedEffect(items, loadingState) {
        if (items != null || loadingState is LoadingState.LoadingFailed || loadingState is LoadingState.LoadingError) {
            isRefreshing = false
        }
    }

    val refresh: () -> Unit = {
        isRefreshing = true
        viewModel.getItems(context)
    }

    // 派生数据：补全应用名 → 关键字过滤 → 可选按名称排序
    val displayItems = remember(items, keyword, sortByName) {
        items.orEmpty()
            .onEach { app ->
                if (app.appName.isEmpty())
                    app.appName = AppUtils.getAppName(context, app.packageName)
            }
            .filter { app ->
                keyword.isBlank() ||
                    app.appName.contains(keyword, ignoreCase = true) ||
                    app.packageName.contains(keyword, ignoreCase = true)
            }
            .let { list -> if (sortByName) list.sortedBy { it.appName.lowercase() } else list }
    }

    // 滚动方向回调（老代码 onScrolled(dy) 驱动底部导航显隐）
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        var lastTotal = 0
        var lastDirection = 0
        snapshotFlow {
            listState.firstVisibleItemIndex * 100_000 + listState.firstVisibleItemScrollOffset
        }.collect { total ->
            val direction = when {
                total > lastTotal -> 1
                total < lastTotal -> -1
                else -> 0
            }
            if (direction != 0 && direction != lastDirection) {
                if (direction > 0) onScrollDown() else onScrollUp()
            }
            lastDirection = direction
            lastTotal = total
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            SmallTopAppBar(
                title = title,
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = "返回",
                                tint = MiuixTheme.colorScheme.onBackground,
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        val failed = loadingState as? LoadingState.LoadingFailed
        val error = loadingState as? LoadingState.LoadingError
        PullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                items == null && failed != null -> AppListMessage(
                    msg = failed.msg,
                    buttonText = if (failed.msg == LOADING_EMPTY) "刷新" else "重试",
                    onClick = refresh,
                    modifier = Modifier.fillMaxSize(),
                )

                items == null && error != null -> AppListMessage(
                    msg = error.errMsg,
                    buttonText = "重试",
                    onClick = refresh,
                    modifier = Modifier.fillMaxSize(),
                )

                items == null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                displayItems.isEmpty() -> AppListMessage(
                    msg = if (keyword.isBlank()) LOADING_EMPTY else "没有匹配的应用",
                    buttonText = if (keyword.isBlank()) "刷新" else null,
                    onClick = if (keyword.isBlank()) refresh else null,
                    modifier = Modifier.fillMaxSize(),
                )

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                ) {
                    item(key = "filter") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextField(
                                value = keyword,
                                onValueChange = { keyword = it },
                                label = "搜索应用名 / 包名",
                                useLabelAsPlaceholder = true,
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                text = if (sortByName) "按名称" else "按最近更新",
                                onClick = { sortByName = !sortByName },
                            )
                        }
                    }
                    item(key = "count") {
                        Text(
                            text = "共 ${displayItems.size} 个应用",
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    items(displayItems, key = { it.packageName }) { app ->
                        AppListRow(
                            app = app,
                            onClick = { onAppClick(app) },
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

/** 应用行：图标 / 名称 / 包名 / 版本（对应 item_app.xml，图标走 Glide + LocalAppIcon） */
@Composable
private fun AppListRow(
    app: AppItem,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    // 老 Adapter.onBindViewHolder 的应用名兜底逻辑
    val name = remember(app.packageName, app.appName) {
        if (app.appName.isNotEmpty()) app.appName
        else AppUtils.getAppName(context, app.packageName).also { app.appName = it }
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIconImage(
                packageName = app.packageName,
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MiuixTheme.textStyles.body1,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.packageName,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.versionName,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

/** 本机应用图标（Glide + LocalAppIcon，AndroidView 包 ImageView） */
@Composable
private fun AppIconImage(
    packageName: String,
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
            Glide.with(imageView).load(LocalAppIcon(packageName)).into(imageView)
        },
    )
}

@Composable
private fun AppListMessage(
    msg: String,
    buttonText: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
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
