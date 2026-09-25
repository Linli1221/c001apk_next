package com.example.c001apk.ui.others

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build.VERSION.SDK_INT
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.example.c001apk.util.ClipboardUtil
import com.example.c001apk.util.PrefManager
import com.example.c001apk.util.SslErrorPrompter
import com.example.c001apk.util.UpdateChecker
import com.example.c001apk.util.http2https
import com.example.c001apk.util.makeToast
import java.net.URISyntaxException
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 每个进程只允许设置一次 WebView 数据目录后缀（与老 [WebViewActivity] 的 companion 逻辑一致） */
private var webViewDataDirSuffixSet = false

/** WebView 里触发的下载请求描述；下载流程（确认弹窗 + DownloadManager）由调用方接线 */
data class WebViewDownloadRequest(
    val url: String,
    val userAgent: String,
    val contentDisposition: String?,
    val mimeType: String?,
)

/**
 * 内置浏览器页（Compose + Miuix 版），替代原 [WebViewActivity] + activity_web_view.xml。
 *
 * 容器：Miuix [Scaffold] + [SmallTopAppBar]（返回 / 编辑 / 刷新 / 更多），正文为
 * `AndroidView` 包住的原生 [WebView]，加载进度显示在顶栏 [SmallTopAppBar.bottomContent]。
 *
 * WebView 行为与老 [WebViewActivity] 对齐（逻辑照搬，不改动）：
 *  - WebSettings 全套配置（UA 走 [PrefManager.USER_AGENT]、暗色算法、禁止 fileAccess…）；
 *  - coolapk 子域登录态 cookie 注入（[applyCoolapkCookies]，每次 onPageStarted 重新注入）；
 *  - **SSL 调试放行逻辑原样保留**：`PrefManager.isSslDebug` 时直接 `handler?.proceed()`，
 *    否则走 [SslErrorPrompter.onSslFailure] + `handler?.cancel()`（只调用，不改动）；
 *  - intent:// 协议解析后 `startActivity`；非 http 自定义协议先弹「是否打开外部链接」确认；
 *  - 返回键优先 WebView 回退（[BackHandler]），页面内 window 关闭（onCloseWindow）走 [onPageClose]。
 *
 * 老代码里的下载确认弹窗 + DownloadManager 流程属于业务流程，提升为 [onDownloadRequest]
 * 回调（为 null 时降级为「外部浏览器打开该链接」，保证交互不断头）；
 * 更多菜单里的复制链接 / 浏览器打开 / 清除缓存均为单个 util 调用，直接在页内复用
 * （[ClipboardUtil] / [UpdateChecker.openExternal] / [makeToast] 见实现）。
 *
 * 注意：老 [WebViewActivity.onDestroy] 结尾有 `exitProcess(0)`（:webview 进程退出策略），
 * 属于 Activity/进程级行为，不在 Compose 页面内复制，接线时由 Activity 自行决定。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    editUrl: String? = null,
    onPageClose: () -> Unit = onBack,
    onDownloadRequest: ((WebViewDownloadRequest) -> Unit)? = null,
) {
    val context = LocalContext.current

    var pageTitle by remember { mutableStateOf(title) }
    var loadingProgress by remember { mutableIntStateOf(100) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var pendingExternalUrl by remember { mutableStateOf<String?>(null) }
    val currentOnDownloadRequest by rememberUpdatedState(onDownloadRequest)

    val webViewResult = remember(context) {
        runCatching {
            if (SDK_INT >= 28 && !webViewDataDirSuffixSet) {
                webViewDataDirSuffixSet = true
                runCatching { WebView.setDataDirectorySuffix("webview") }
            }
            WebView(context)
        }
    }
    val webView = webViewResult.getOrNull()

    if (webView == null) {
        // 错误态：WebView 初始化失败（老代码弹「Failed to init WebView」对话框）
        Scaffold(
            modifier = modifier,
            topBar = {
                SmallTopAppBar(
                    title = title,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = "返回",
                                tint = MiuixTheme.colorScheme.onBackground,
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(12.dp),
                insideMargin = PaddingValues(16.dp),
            ) {
                Text(
                    text = "Failed to init WebView",
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = webViewResult.exceptionOrNull()?.message ?: "unknown",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        return
    }

    // 返回键：优先 WebView 内回退，退无可退再退出页面（对应老 onKeyDown）
    BackHandler {
        if (webView.canGoBack()) webView.goBack() else onBack()
    }

    DisposableEffect(webView) {
        webView.settings.apply {
            javaScriptEnabled = true
            blockNetworkImage = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            defaultTextEncodingName = "UTF-8"
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            javaScriptCanOpenWindowsAutomatically = true
            loadsImagesAutomatically = true
            allowFileAccess = false
            userAgentString = PrefManager.USER_AGENT
            if (SDK_INT >= 32) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(this, true)
                }
            } else {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    val nightModeFlags =
                        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                        WebSettingsCompat.setForceDark(this, WebSettingsCompat.FORCE_DARK_ON)
                    }
                }
            }
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
            // 跨子域（m/www/api 等）注入登录态 cookie；具体逻辑见 applyCoolapkCookies
            applyCoolapkCookies(url)
        }
        webView.setDownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, _ ->
            val handler = currentOnDownloadRequest
            if (handler != null) {
                handler(WebViewDownloadRequest(downloadUrl, userAgent, contentDisposition, mimeType))
            } else {
                // 未接下载流程时的降级：外部浏览器打开，不静默丢弃
                UpdateChecker.openExternal(context, downloadUrl, "打开下载链接失败")
            }
        }
        webView.webViewClient = object : WebViewClient() {
            /**
             * WebView 内部每次开始加载新页面时触发。
             * 当目标 host 是 coolapk 任意子域时，重新注入登录态 cookie，
             * 覆盖从 m.coolapk.com 跳转到 www / api 等子域时丢失登录态的情况。
             */
            override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                super.onPageStarted(view, pageUrl, favicon)
                if (pageUrl != null) applyCoolapkCookies(pageUrl)
            }

            /** SSL 证书校验不过（如抓包/中间人）：阻止加载并弹风险警告 */
            override fun onReceivedSslError(
                view: WebView?, handler: SslErrorHandler?, error: SslError?
            ) {
                // 网络传输调试模式：直接放行（不校验证书链/域名），方便抓包调试
                if (PrefManager.isSslDebug) {
                    handler?.proceed()
                    return
                }
                SslErrorPrompter.onSslFailure(error?.let {
                    java.security.cert.CertificateException(it.toString())
                })
                handler?.cancel()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean {
                request?.let {
                    try {
                        // 处理 intent 协议
                        if (request.url.toString().startsWith("intent://")) {
                            try {
                                val intent = Intent.parseUri(
                                    request.url.toString(), Intent.URI_INTENT_SCHEME
                                )
                                intent.addCategory("android.intent.category.BROWSABLE")
                                intent.component = null
                                intent.selector = null
                                val resolves =
                                    context.packageManager.queryIntentActivities(intent, 0)
                                if (resolves.size > 0) {
                                    context.startActivity(intent)
                                }
                            } catch (e: URISyntaxException) {
                                e.printStackTrace()
                            }
                            return true
                        }
                        // 处理自定义 scheme 协议：先确认再打开外部应用
                        if (!request.url.toString().startsWith("http")) {
                            pendingExternalUrl = request.url.toString()
                            return true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onCloseWindow(window: WebView?) {
                super.onCloseWindow(window)
                onPageClose()
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                loadingProgress = newProgress
            }

            override fun onReceivedTitle(view: WebView, receivedTitle: String) {
                super.onReceivedTitle(view, receivedTitle)
                pageTitle = receivedTitle
            }
        }
        onDispose {
            try {
                webView.apply {
                    loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
                    loadUrl("about:blank")
                    (parent as? ViewGroup)?.removeView(this)
                    stopLoading()
                    settings.javaScriptEnabled = false
                    clearHistory()
                    clearCache(true)
                    removeAllViewsInLayout()
                    removeAllViews()
                    setOnTouchListener(null)
                    setOnKeyListener(null)
                    onFocusChangeListener = null
                    webChromeClient = null
                    onPause()
                    destroy()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    // 注意：这里 loadUrl **不能**带 X-Requested-With: com.coolapk.market。
    // 酷安 H5（m.coolapk.com）用它判定 ajax 请求，带上后整页会返回 JSON 片段
    // （实测 myDevice / editProductOwner / report 等页面都是如此），WebView 只能显示原始 JSON。
    LaunchedEffect(webView, url) {
        applyCoolapkCookies(url)
        webView.loadUrl(url)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            SmallTopAppBar(
                title = pageTitle,
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
                    // 只有显式传了 editUrl 的页面（如「我的装备」）才显示编辑按钮
                    if (editUrl != null) {
                        IconButton(onClick = {
                            applyCoolapkCookies(editUrl)
                            webView.loadUrl(editUrl)
                        }) {
                            Icon(
                                imageVector = MiuixIcons.Edit,
                                contentDescription = "编辑",
                                tint = MiuixTheme.colorScheme.onBackground,
                            )
                        }
                    }
                    IconButton(onClick = { webView.reload() }) {
                        Icon(
                            imageVector = MiuixIcons.Refresh,
                            contentDescription = "刷新",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(
                            imageVector = MiuixIcons.More,
                            contentDescription = "更多",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
                bottomContent = {
                    if (loadingProgress in 0..99) {
                        LinearProgressIndicator(
                            progress = loadingProgress / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        AndroidView(
            factory = { webView },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        )

        // 更多操作菜单（老 webview_menu 的复制链接 / 浏览器打开 / 清除缓存）
        OverlayDialog(
            title = "更多操作",
            show = showMoreMenu,
            onDismissRequest = { showMoreMenu = false },
        ) {
            Card {
                BasicComponent(
                    title = "复制链接",
                    onClick = {
                        showMoreMenu = false
                        ClipboardUtil.copyText(context, (webView.url ?: url).http2https)
                    },
                )
                HorizontalDivider()
                BasicComponent(
                    title = "在浏览器中打开",
                    onClick = {
                        showMoreMenu = false
                        UpdateChecker.openExternal(context, (webView.url ?: url).http2https, "打开失败")
                    },
                )
                HorizontalDivider()
                BasicComponent(
                    title = "清除缓存",
                    onClick = {
                        showMoreMenu = false
                        webView.apply {
                            clearHistory()
                            clearCache(true)
                            clearFormData()
                        }
                        context.makeToast("清除缓存成功")
                    },
                )
            }
        }

        // 自定义 scheme 外链确认（老代码用 Snackbar + 「打开」action，这里换成 Miuix 对话框）
        OverlayDialog(
            title = "当前网页将要打开外部链接，是否打开",
            show = pendingExternalUrl != null,
            onDismissRequest = { pendingExternalUrl = null },
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    text = "取消",
                    onClick = { pendingExternalUrl = null },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = "打开",
                    onClick = {
                        pendingExternalUrl?.let {
                            UpdateChecker.openExternal(context, it, "打开失败")
                        }
                        pendingExternalUrl = null
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

/**
 * 判断 url 是否命中 coolapk 主域（含所有子域）。
 * 用于决定是否需要注入登录态 cookie。（与老 [WebViewActivity] / [WebViewFragment] 逻辑一致）
 */
private fun isCoolapkHost(url: String): Boolean {
    val host = runCatching { android.net.Uri.parse(url).host?.lowercase() }.getOrNull() ?: return false
    return host == "coolapk.com" || host.endsWith(".coolapk.com")
}

/**
 * 把登录态 cookie 写入 WebView 的 CookieManager。
 * - 当目标 host 是 coolapk 子域时生效；
 * - 同时写到当前 host 与 .coolapk.com 域，使 cookie 在所有子域之间共享，
 *   解决从 m.coolapk.com 跳到 www / api 等子域后显示未登录的问题。
 */
private fun applyCoolapkCookies(url: String) {
    if (!isCoolapkHost(url)) return
    val cookieManager = CookieManager.getInstance()
    val host = android.net.Uri.parse(url).host?.lowercase() ?: return
    val cookies = listOf(
        "DID=${PrefManager.SZLMID}",
        "forward=https://www.coolapk.com",
        "displayVersion=v14",
        "uid=${PrefManager.uid}",
        "username=${PrefManager.username}",
        "token=${PrefManager.token}",
    )
    cookies.forEach { value ->
        // 写到当前 host，确保该子域请求能立刻带上 cookie
        cookieManager.setCookie(url, "$value; path=/")
        // 再以 .coolapk.com 域写一份，覆盖之后跳转到其它子域的场景
        if (host != "m.coolapk.com") {
            cookieManager.setCookie(
                "https://m.coolapk.com/",
                "$value; domain=.coolapk.com; path=/"
            )
        }
    }
}
