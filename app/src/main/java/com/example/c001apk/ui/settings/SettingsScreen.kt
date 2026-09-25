package com.example.c001apk.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.c001apk.BuildConfig
import com.example.c001apk.R
import com.example.c001apk.util.CacheDataManager
import com.example.c001apk.util.PrefManager
import com.example.c001apk.util.TokenDeviceUtils.applyDefaultFingerprint
import com.example.c001apk.util.TokenDeviceUtils.randHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 设置页面（Miuix/Compose 版）。
 *
 * 对应老的 [SettingsActivity] + [SettingsPreferenceFragment] + res/xml/settings.xml，
 * 按 TODOS.md「C. 设置 → 一级菜单分组」组织成 5 个一级分组：
 * 外观 / 推荐流相关 / 隐私 / 高级 / 其他。
 *
 * - 一级页 [SettingsScreen] 只放分组入口，点进哪个分组由 [SettingsScreen.onOpenGroup] 回调决定
 *   （子页面就是本文件里的 [SettingsAppearanceScreen] / [SettingsRecommendationScreen] /
 *   [SettingsPrivacyScreen] / [SettingsAdvancedScreen] / [SettingsOtherScreen]，
 *   页面之间的压栈/转场交给宿主 Activity）。
 * - 设置项的读写直接落在 [PrefManager]（SharedPreferences 封装），开关值就地维护成 Compose 状态。
 * - 跳出本目录的页面（黑名单/屏蔽页/关于页/机型参数页…）一律用回调参数表达，不直接 startActivity。
 */
enum class SettingsGroup {
    /** 外观：主题颜色、深色主题、纯黑主题、字体调节、显示表情 */
    Appearance,

    /** 推荐流相关：黑名单、关键字屏蔽、外链、头条、历史、更新检查 */
    Recommendation,

    /** 隐私：用户黑名单 */
    Privacy,

    /** 高级：数字联盟ID、机型参数、图片画质、SSL */
    Advanced,

    /** 其他：关于、清理缓存 */
    Other,
}

/** 一级设置页：只列 5 个分组入口。 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenGroup: (SettingsGroup) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.tab_setting),
                subtitle = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                navigationIcon = { SettingsBackIcon(onBack = onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding,
        ) {
            item {
                Card(modifier = Modifier.padding(12.dp)) {
                    ArrowPreference(
                        title = stringResource(R.string.settings_appearance),
                        summary = "主题颜色 · 深色主题 · 字体调节 · 显示表情",
                        onClick = { onOpenGroup(SettingsGroup.Appearance) },
                    )
                    ArrowPreference(
                        title = stringResource(R.string.settings_recommendation),
                        summary = "黑名单 · 关键字屏蔽 · 外链 · 浏览历史 · 更新检查",
                        onClick = { onOpenGroup(SettingsGroup.Recommendation) },
                    )
                    ArrowPreference(
                        title = stringResource(R.string.settings_privacy),
                        summary = "用户黑名单",
                        onClick = { onOpenGroup(SettingsGroup.Privacy) },
                    )
                    ArrowPreference(
                        title = stringResource(R.string.settings_advanced),
                        summary = "数字联盟ID · 机型参数 · 图片画质 · SSL",
                        onClick = { onOpenGroup(SettingsGroup.Advanced) },
                    )
                    ArrowPreference(
                        title = stringResource(R.string.settings_other),
                        summary = "关于 · 清理缓存",
                        onClick = { onOpenGroup(SettingsGroup.Other) },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

/** 外观设置页（老 appearance PreferenceScreen）。 */
@Composable
fun SettingsAppearanceScreen(
    onBack: () -> Unit,
    onRequestRecreate: () -> Unit = {},
) {
    val context = LocalContext.current
    val colorEntries = remember {
        context.resources.getStringArray(R.array.color_texts).toList()
    }
    val colorValues = remember {
        context.resources.getStringArray(R.array.color_values).toList()
    }
    val themeEntries = remember {
        context.resources.getStringArray(R.array.theme_texts).toList()
    }
    val themeValues = remember {
        context.resources.getStringArray(R.array.theme_values).toList()
    }

    var followSystemAccent by remember { mutableStateOf(PrefManager.followSystemAccent) }
    var themeColorIndex by remember {
        mutableStateOf(colorValues.indexOf(PrefManager.themeColor).coerceAtLeast(0))
    }
    var darkThemeIndex by remember {
        mutableStateOf(themeValues.indexOf(PrefManager.darkTheme.toString()).coerceAtLeast(0))
    }
    var blackDarkTheme by remember { mutableStateOf(PrefManager.blackDarkTheme) }
    var fontScale by remember {
        mutableStateOf(PrefManager.FONTSCALE.toFloatOrNull() ?: 1.00f)
    }
    var showEmoji by remember { mutableStateOf(PrefManager.showEmoji) }

    SettingsSubPage(
        title = stringResource(R.string.settings_appearance),
        onBack = onBack,
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            item {
                Card(modifier = Modifier.padding(12.dp)) {
                    // 老逻辑：followSystemAccent 开启时不使用自定义主题色（disableDependentsState）
                    SwitchPreference(
                        title = stringResource(R.string.settings_system_theme_color),
                        checked = followSystemAccent,
                        onCheckedChange = { checked ->
                            followSystemAccent = checked
                            PrefManager.followSystemAccent = checked
                            onRequestRecreate()
                        },
                    )
                    OverlayDropdownPreference(
                        title = stringResource(R.string.settings_theme_color),
                        items = colorEntries,
                        selectedIndex = themeColorIndex,
                        enabled = !followSystemAccent,
                        onSelectedIndexChange = { index ->
                            themeColorIndex = index
                            PrefManager.themeColor = colorValues[index]
                            onRequestRecreate()
                        },
                    )
                    OverlayDropdownPreference(
                        title = stringResource(R.string.dark_theme),
                        items = themeEntries,
                        selectedIndex = darkThemeIndex,
                        onSelectedIndexChange = { index ->
                            darkThemeIndex = index
                            val mode = themeValues[index].toIntOrNull()
                                ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            if (PrefManager.darkTheme != mode) {
                                PrefManager.darkTheme = mode
                                AppCompatDelegate.setDefaultNightMode(mode)
                            }
                        },
                    )
                    SwitchPreference(
                        title = stringResource(R.string.settings_pure_black_dark_theme),
                        summary = stringResource(R.string.settings_pure_black_dark_theme_summary),
                        checked = blackDarkTheme,
                        onCheckedChange = { checked ->
                            blackDarkTheme = checked
                            PrefManager.blackDarkTheme = checked
                            onRequestRecreate()
                        },
                    )
                    // 字体调节：0.80 ~ 1.30，松手后落库并重建界面（老对话框的「确定」）
                    SliderPreference(
                        value = fontScale,
                        onValueChange = { fontScale = it },
                        onValueChangeFinished = {
                            PrefManager.FONTSCALE = String.format("%.2f", fontScale)
                            onRequestRecreate()
                        },
                        title = stringResource(R.string.font_scale),
                        valueText = String.format("%.2f", fontScale),
                        valueRange = 0.80f..1.30f,
                        steps = 9,
                        showKeyPoints = true,
                        keyPoints = listOf(0.80f, 1.00f, 1.30f),
                    )
                    SwitchPreference(
                        title = stringResource(R.string.show_emoji),
                        checked = showEmoji,
                        onCheckedChange = { checked ->
                            showEmoji = checked
                            PrefManager.showEmoji = checked
                            onRequestRecreate()
                        },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

/** 推荐流相关设置页（老 recommendation PreferenceScreen）。 */
@Composable
fun SettingsRecommendationScreen(
    onBack: () -> Unit,
    onOpenTopicBlackList: () -> Unit,
    onOpenSpamWord: () -> Unit,
    onOpenSpamUser: () -> Unit,
    onOpenSpamNode: () -> Unit,
    onCheckUpdate: () -> Unit = {},
) {
    var isColorFilter by remember { mutableStateOf(PrefManager.isColorFilter) }
    var isOpenLinkOutside by remember { mutableStateOf(PrefManager.isOpenLinkOutside) }
    var isIconMiniCard by remember { mutableStateOf(PrefManager.isIconMiniCard) }
    var isRecordHistory by remember { mutableStateOf(PrefManager.isRecordHistory) }
    var isCheckUpdateStable by remember { mutableStateOf(PrefManager.isCheckUpdateStable) }
    var isCheckUpdateBeta by remember { mutableStateOf(PrefManager.isCheckUpdateBeta) }

    SettingsSubPage(
        title = stringResource(R.string.settings_recommendation),
        onBack = onBack,
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            item {
                SmallTitle(text = "内容屏蔽")
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    ArrowPreference(
                        title = stringResource(R.string.topic_black_list),
                        onClick = onOpenTopicBlackList,
                    )
                    ArrowPreference(
                        title = stringResource(R.string.spam_shield_word),
                        onClick = onOpenSpamWord,
                    )
                    ArrowPreference(
                        title = stringResource(R.string.spam_shield_user),
                        onClick = onOpenSpamUser,
                    )
                    ArrowPreference(
                        title = stringResource(R.string.spam_shield_node),
                        onClick = onOpenSpamNode,
                    )
                }
            }
            item {
                SmallTitle(text = "信息流")
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    SwitchPreference(
                        title = stringResource(R.string.color_filter_in_dark_mode),
                        checked = isColorFilter,
                        onCheckedChange = { checked ->
                            isColorFilter = checked
                            PrefManager.isColorFilter = checked
                        },
                    )
                    SwitchPreference(
                        title = stringResource(R.string.open_link_outside),
                        checked = isOpenLinkOutside,
                        onCheckedChange = { checked ->
                            isOpenLinkOutside = checked
                            PrefManager.isOpenLinkOutside = checked
                        },
                    )
                    SwitchPreference(
                        title = stringResource(R.string.show_icon_mini_card),
                        checked = isIconMiniCard,
                        onCheckedChange = { checked ->
                            isIconMiniCard = checked
                            PrefManager.isIconMiniCard = checked
                        },
                    )
                    SwitchPreference(
                        title = stringResource(R.string.record_history),
                        checked = isRecordHistory,
                        onCheckedChange = { checked ->
                            isRecordHistory = checked
                            PrefManager.isRecordHistory = checked
                        },
                    )
                }
            }
            item {
                SmallTitle(text = "版本更新")
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    SwitchPreference(
                        title = stringResource(R.string.check_stable_update),
                        checked = isCheckUpdateStable,
                        onCheckedChange = { checked ->
                            isCheckUpdateStable = checked
                            PrefManager.isCheckUpdateStable = checked
                        },
                    )
                    SwitchPreference(
                        title = stringResource(R.string.check_beta_update),
                        checked = isCheckUpdateBeta,
                        onCheckedChange = { checked ->
                            isCheckUpdateBeta = checked
                            PrefManager.isCheckUpdateBeta = checked
                        },
                    )
                    ArrowPreference(
                        title = stringResource(R.string.check_update_now_stable),
                        onClick = onCheckUpdate,
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

/** 隐私设置页（老 privacy PreferenceScreen）。 */
@Composable
fun SettingsPrivacyScreen(
    onBack: () -> Unit,
    onOpenUserBlackList: () -> Unit,
) {
    SettingsSubPage(
        title = stringResource(R.string.settings_privacy),
        onBack = onBack,
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            item {
                Card(modifier = Modifier.padding(12.dp)) {
                    ArrowPreference(
                        title = stringResource(R.string.user_black_list),
                        onClick = onOpenUserBlackList,
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

/**
 * 高级设置页（老 advanced PreferenceScreen）：数字联盟ID、机型参数、图片画质、SSL 相关。
 *
 * @param onOpenParams 跳「机型参数」页（[com.example.c001apk.ui.settings.params.SettingsParamsScreen]）。
 * @param onRestartApp 网络栈改动后「立即重启」：由宿主执行（老 SettingsPreferenceFragment.restartApp）。
 */
@Composable
fun SettingsAdvancedScreen(
    onBack: () -> Unit,
    onOpenParams: () -> Unit,
    onRestartApp: () -> Unit = {},
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var szlmId by remember { mutableStateOf(PrefManager.SZLMID) }
    var showSzlmDialog by remember { mutableStateOf(false) }
    var szlmInput by remember { mutableStateOf("") }

    var imageQualityIndex by remember {
        mutableStateOf(IMAGE_QUALITY_VALUES.indexOf(PrefManager.imageQuality).coerceAtLeast(0))
    }

    var verifySsl by remember { mutableStateOf(PrefManager.isVerifySsl) }
    var sslDebug by remember { mutableStateOf(PrefManager.isSslDebug) }
    var showSslConfirm by remember { mutableStateOf(false) }
    var sslCountdown by remember { mutableStateOf(SSL_CONFIRM_SECONDS) }
    var showRestartDialog by remember { mutableStateOf(false) }

    // 开启调试模式前的二次确认：正向按钮先禁用并倒计时，防止误触一键放开全部 SSL 校验
    LaunchedEffect(showSslConfirm) {
        if (showSslConfirm) {
            sslCountdown = SSL_CONFIRM_SECONDS
            while (sslCountdown > 0) {
                delay(1000L)
                sslCountdown -= 1
            }
        }
    }

    SettingsSubPage(
        title = stringResource(R.string.settings_advanced),
        onBack = onBack,
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            item {
                Card(modifier = Modifier.padding(12.dp)) {
                    ArrowPreference(
                        title = stringResource(R.string.szlmId),
                        summary = szlmId.ifEmpty { stringResource(R.string.szlmId_summary) },
                        holdDownState = showSzlmDialog,
                        onClick = {
                            szlmInput = szlmId
                            showSzlmDialog = true
                        },
                    )
                    ArrowPreference(
                        title = stringResource(R.string.params),
                        summary = "X-App-Device、User-Agent 等设备指纹参数",
                        onClick = onOpenParams,
                    )
                    OverlayDropdownPreference(
                        title = stringResource(R.string.image_quality),
                        items = IMAGE_QUALITY_LABELS,
                        selectedIndex = imageQualityIndex,
                        onSelectedIndexChange = { index ->
                            imageQualityIndex = index
                            PrefManager.imageQuality = IMAGE_QUALITY_VALUES[index]
                        },
                    )
                }
            }
            item {
                SmallTitle(text = "网络安全")
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    // 默认开：不信任系统 CA，只用证书链固定公钥校验；开启时强制关掉调试模式
                    SwitchPreference(
                        title = stringResource(R.string.settings_verify_ssl),
                        summary = stringResource(R.string.settings_verify_ssl_summary),
                        checked = verifySsl,
                        onCheckedChange = { checked ->
                            verifySsl = checked
                            PrefManager.isVerifySsl = checked
                            if (checked && PrefManager.isSslDebug) {
                                PrefManager.isSslDebug = false
                                sslDebug = false
                            }
                        },
                    )
                    // 与「校验 SSL 证书」互斥：严格校验开着时禁用；开启需二次确认 + 5 秒等待
                    SwitchPreference(
                        title = stringResource(R.string.settings_ssl_debug),
                        summary = if (verifySsl) {
                            stringResource(R.string.settings_ssl_debug_locked_summary)
                        } else {
                            stringResource(R.string.settings_ssl_debug_summary)
                        },
                        checked = sslDebug,
                        enabled = !verifySsl,
                        onCheckedChange = { checked ->
                            when {
                                // 严格校验开着时不可开启（开关本应是禁用态，这里兜底拦截）
                                checked && verifySsl -> Unit

                                // 开启：先弹二次确认，确认后再落库
                                checked -> showSslConfirm = true

                                // 关闭：直接落库 + 提示（需重启生效）
                                else -> {
                                    sslDebug = false
                                    PrefManager.isSslDebug = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.ssl_debug_disabled_toast)
                                        )
                                    }
                                }
                            }
                        },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    // 数字联盟 ID 编辑（老 szlmId 对话框）：填过（非空）才算「已配置」，改完必须重造设备串
    OverlayDialog(
        title = stringResource(R.string.szlmId),
        summary = stringResource(R.string.szlmId_summary),
        show = showSzlmDialog,
        onDismissRequest = { showSzlmDialog = false },
    ) {
        TextField(
            value = szlmInput,
            onValueChange = { szlmInput = it },
            label = stringResource(R.string.szlmId),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = stringResource(android.R.string.cancel),
                onClick = { showSzlmDialog = false },
                modifier = Modifier.weight(1f),
            )
            if (BuildConfig.DEBUG) {
                TextButton(
                    text = stringResource(R.string.random_value),
                    onClick = {
                        // 调试用：换一份随机 szlmId（不置 szlmIdConfigured，仍算未配置）
                        PrefManager.SZLMID = randHexString(16)
                        applyDefaultFingerprint()
                        szlmId = PrefManager.SZLMID
                        showSzlmDialog = false
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            TextButton(
                text = stringResource(android.R.string.ok),
                onClick = {
                    val id = szlmInput.trim()
                    PrefManager.SZLMID = id
                    PrefManager.szlmIdConfigured = id.isNotEmpty()
                    // szlmId 是设备串首字段，改完必须重造设备串才能生效
                    applyDefaultFingerprint()
                    szlmId = PrefManager.SZLMID
                    showSzlmDialog = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }

    // 开启网络传输调试模式前的二次确认
    OverlayDialog(
        title = stringResource(R.string.ssl_debug_confirm_title),
        summary = stringResource(R.string.ssl_debug_confirm_message),
        show = showSslConfirm,
        onDismissRequest = { showSslConfirm = false },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = stringResource(android.R.string.cancel),
                onClick = { showSslConfirm = false },
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = if (sslCountdown > 0) {
                    stringResource(R.string.ssl_debug_confirm_ok_wait, sslCountdown)
                } else {
                    stringResource(R.string.ssl_debug_confirm_ok)
                },
                enabled = sslCountdown <= 0,
                onClick = {
                    PrefManager.isSslDebug = true
                    sslDebug = true
                    showSslConfirm = false
                    showRestartDialog = true
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }

    // 开启后提示需要重启（网络栈在 Application 启动时构建，只有整个进程重启才生效）
    OverlayDialog(
        title = stringResource(R.string.ssl_debug_restart_title),
        summary = stringResource(R.string.ssl_debug_restart_message),
        show = showRestartDialog,
        onDismissRequest = { showRestartDialog = false },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = stringResource(R.string.ssl_debug_restart_later),
                onClick = { showRestartDialog = false },
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = stringResource(R.string.ssl_debug_restart_now),
                onClick = {
                    showRestartDialog = false
                    onRestartApp()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

/** 其他（老 other PreferenceScreen）：关于、清理缓存。 */
@Composable
fun SettingsOtherScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val context = LocalContext.current
    var cacheText by remember { mutableStateOf<String?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 缓存大小需要遍历目录，丢到 IO 线程算
    LaunchedEffect(Unit) {
        cacheText = withContext(Dispatchers.IO) {
            CacheDataManager.getTotalCacheSize(context.applicationContext)
        }
    }

    SettingsSubPage(
        title = stringResource(R.string.settings_other),
        onBack = onBack,
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            item {
                Card(modifier = Modifier.padding(12.dp)) {
                    ArrowPreference(
                        title = stringResource(R.string.about),
                        summary = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})",
                        onClick = onOpenAbout,
                    )
                    ArrowPreference(
                        title = stringResource(R.string.clear_cache),
                        summary = cacheText ?: "计算中…",
                        holdDownState = showClearDialog,
                        onClick = { showClearDialog = true },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (cacheText != null) {
        OverlayDialog(
            title = "确定清除缓存吗？",
            summary = "当前缓存：$cacheText",
            show = showClearDialog,
            onDismissRequest = { showClearDialog = false },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = { showClearDialog = false },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = {
                        showClearDialog = false
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                CacheDataManager.clearAllCache(context.applicationContext)
                            }
                            cacheText = "刚刚清理"
                            snackbarHostState.showSnackbar("缓存已清理")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 页面骨架 / 公共小组件（本轮允许各页自带顶栏，公共收敛留到后续轮）
// ---------------------------------------------------------------------------

/** 二级设置页骨架：[SmallTopAppBar] + 返回键，Overlay 弹窗依赖这里的 [Scaffold] 祖先。 */
@Composable
private fun SettingsSubPage(
    title: String,
    onBack: () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = title,
                navigationIcon = { SettingsBackIcon(onBack = onBack) },
            )
        },
        snackbarHost = snackbarHost,
        content = content,
    )
}

/** 返回按钮（MiuixIcons.Back，RTL 自动镜像留给宿主语义，这里按 LTR 直出）。 */
@Composable
private fun SettingsBackIcon(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(
            imageVector = MiuixIcons.Back,
            contentDescription = "返回",
            tint = MiuixTheme.colorScheme.onBackground,
        )
    }
}

private val IMAGE_QUALITY_LABELS = listOf("网络自适应", "原图", "普清")

private val IMAGE_QUALITY_VALUES = listOf("auto", "origin", "thumbnail")

/** 开启「网络传输调试模式」二次确认的倒计时秒数。 */
private const val SSL_CONFIRM_SECONDS = 5
