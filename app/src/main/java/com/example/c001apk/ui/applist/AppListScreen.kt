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
import androidx.compose.runtime.livedata.observeAsState
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
 * 搴旂敤鍒楄〃椤碉紙Compose 鐗堬級锛屽搴旇€佷唬鐮侊細
 * - `AppListFragment`锛圔aseRefreshRecyclerviewBinding锛歋wipeRefreshLayout + RecyclerView锛? * - `AppListAdapter` + `res/layout/item_app.xml`锛堝浘鏍囪锛欸lide 鍔犺浇 LocalAppIcon / 鍚嶇О / 鍖呭悕 / 鐗堟湰锛? *
 * 鏁版嵁璧扮幇鏈?[AppListViewModel]锛坕tems LiveData 鐢?observeAsState 妗ユ帴锛沢etItems(context)
 * 鎵弿鏈満宸插畨瑁呭簲鐢ㄧ殑閫昏緫淇濇寔涓嶅彉锛夛紝鍒楄〃椤瑰浘鏍囩户缁敤 Glide锛圓ndroidView 鍖?ImageView锛夈€? * 鑰佷唬鐮佹病鏈夌瓫閫?UI锛屾湰杞寜闇€姹傛柊澧炵函鐣岄潰灞傜殑鍏抽敭瀛楄繃婊や笌鎺掑簭锛堜笉鏀?ViewModel锛夈€? *
 * @param viewModel 澶嶇敤鐜版湁 [AppListViewModel]銆? * @param title 椤舵爮鏍囬锛堣€佷唬鐮佹爣棰樻潵鑷椤?tab锛岀嫭绔嬮〉闈㈡椂榛樿銆屽簲鐢ㄣ€嶏級銆? * @param onBack 杩斿洖锛涗负 null 鏃朵笉鏄剧ず杩斿洖鎸夐挳锛堜綔涓洪椤?tab 宓屽叆鏃剁殑褰㈡€侊級銆? * @param onAppClick 鐐瑰嚮搴旂敤琛岋紙鑰佷唬鐮佽烦 AppActivity(id=鍖呭悕)锛岀敱鎺ョ嚎鏂瑰疄鐜帮級銆? * @param onScrollDown 鍒楄〃鍚戜笅婊氬姩锛堣€佷唬鐮侀殣钘忓簳閮ㄥ鑸級銆? * @param onScrollUp 鍒楄〃鍚戜笂婊氬姩锛堣€佷唬鐮佹樉绀哄簳閮ㄥ鑸級銆? */
@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    modifier: Modifier = Modifier,
    title: String = "搴旂敤",
    onBack: (() -> Unit)? = null,
    onAppClick: (AppItem) -> Unit = {},
    onScrollDown: () -> Unit = {},
    onScrollUp: () -> Unit = {},
) {
    val context = LocalContext.current

    // LiveData 鈫?Compose 鐘舵€佹ˉ鎺?    val items by viewModel.items.observeAsState()
    val loadingState by viewModel.loadingState.observeAsState()

    // 鐣岄潰灞傜瓫閫?鎺掑簭鐘舵€侊紙绾睍绀猴紝涓嶈繘 ViewModel锛?    var keyword by rememberSaveable { mutableStateOf("") }
    var sortByName by rememberSaveable { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // 棣栨杩涘叆鑷姩鍔犺浇锛堣€佷唬鐮佺敱瀹夸富 fetchData() 瑙﹀彂锛涘瀹夸富宸茶Е鍙戣繃鍒?guard 浼氳烦杩囷級
    LaunchedEffect(Unit) {
        if (viewModel.items.value == null && viewModel.loadingState.value == null) {
            viewModel.getItems(context)
        }
    }

    // 鍒楄〃鍒锋柊瀹屾垚鍚庢敹璧蜂笅鎷夋寚绀哄櫒锛堣€佷唬鐮佸湪 items observer 閲?isRefreshing = false锛?    LaunchedEffect(items, loadingState) {
        if (items != null || loadingState is LoadingState.LoadingFailed || loadingState is LoadingState.LoadingError) {
            isRefreshing = false
        }
    }

    val refresh: () -> Unit = {
        isRefreshing = true
        viewModel.getItems(context)
    }

    // 娲剧敓鏁版嵁锛氳ˉ鍏ㄥ簲鐢ㄥ悕 鈫?鍏抽敭瀛楄繃婊?鈫?鍙€夋寜鍚嶇О鎺掑簭
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

    // 婊氬姩鏂瑰悜鍥炶皟锛堣€佷唬鐮?onScrolled(dy) 椹卞姩搴曢儴瀵艰埅鏄鹃殣锛?    val listState = rememberLazyListState()
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
                                contentDescription = "杩斿洖",
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
                    buttonText = if (failed.msg == LOADING_EMPTY) "鍒锋柊" else "閲嶈瘯",
                    onClick = refresh,
                    modifier = Modifier.fillMaxSize(),
                )

                items == null && error != null -> AppListMessage(
                    msg = error.errMsg,
                    buttonText = "閲嶈瘯",
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
                    msg = if (keyword.isBlank()) LOADING_EMPTY else "娌℃湁鍖归厤鐨勫簲鐢?,
                    buttonText = if (keyword.isBlank()) "鍒锋柊" else null,
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
                                label = "鎼滅储搴旂敤鍚?/ 鍖呭悕",
                                useLabelAsPlaceholder = true,
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                text = if (sortByName) "鎸夊悕绉? else "鎸夋渶杩戞洿鏂?,
                                onClick = { sortByName = !sortByName },
                            )
                        }
                    }
                    item(key = "count") {
                        Text(
                            text = "鍏?${displayItems.size} 涓簲鐢?,
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

/** 搴旂敤琛岋細鍥炬爣 / 鍚嶇О / 鍖呭悕 / 鐗堟湰锛堝搴?item_app.xml锛屽浘鏍囪蛋 Glide + LocalAppIcon锛?*/
@Composable
private fun AppListRow(
    app: AppItem,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    // 鑰?Adapter.onBindViewHolder 鐨勫簲鐢ㄥ悕鍏滃簳閫昏緫
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

/** 鏈満搴旂敤鍥炬爣锛圙lide + LocalAppIcon锛孉ndroidView 鍖?ImageView锛?*/
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
