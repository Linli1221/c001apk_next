package com.example.c001apk.ui.settings.params

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.c001apk.BuildConfig
import com.example.c001apk.R
import com.example.c001apk.constant.Constants
import com.example.c001apk.util.PrefManager
import com.example.c001apk.util.TokenDeviceUtils.applyDefaultFingerprint
import com.example.c001apk.util.TokenDeviceUtils.applyRealDeviceFingerprint
import com.example.c001apk.util.TokenDeviceUtils.defaultDeviceCode
import com.example.c001apk.util.TokenDeviceUtils.detectRealDevice
import com.example.c001apk.util.TokenDeviceUtils.getDeviceCode
import com.example.c001apk.util.TokenDeviceUtils.getLastingDeviceCode
import com.example.c001apk.util.TokenDeviceUtils.getTokenV3
import com.example.c001apk.util.TokenDeviceUtils.randHexString
import com.example.c001apk.util.Utils.randomAndroidVersionRelease
import com.example.c001apk.util.Utils.randomBrand
import com.example.c001apk.util.Utils.randomDeviceModel
import com.example.c001apk.util.Utils.randomManufacturer
import com.example.c001apk.util.Utils.randomSdkInt
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 机型参数页（Miuix/Compose 版）。
 *
 * 对应老的 [ParamsActivity] + [ParamsPreferenceFragment] + res/xml/params.xml，
 * 覆盖「数字联盟ID / 机型检测 / 设备指纹参数 / X-App-Device、X-App-Token / 图片画质」的查看与编辑。
 *
 * 所有读写直接落在 [PrefManager]（SharedPreferences 封装）；设备串重造走
 * `TokenDeviceUtils`，与老实现完全一致。页面内的提示用 Miuix [SnackbarHostState]。
 */
@Composable
fun SettingsParamsScreen(
    onBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 编辑落库后 +1，驱动各项 summary 重新从 PrefManager 读取
    var revision by remember { mutableStateOf(0) }
    val paramItems = remember { buildParamItems() }
    val values = remember(revision) { paramItems.map { it.current() } }
    val xAppToken = remember(revision) { PrefManager.xAppToken }
    val xAppDevice = remember(revision) { PrefManager.xAppDevice }

    var reportRealDevice by remember { mutableStateOf(PrefManager.reportRealDevice) }
    val realDeviceSummary = remember(revision, reportRealDevice) {
        detectRealDevice().let {
            "${it.brand} ${it.model}（Android ${it.androidVersion}）" +
                if (reportRealDevice) " · 上报中" else " · 未上报（用默认机型）"
        }
    }

    var editingIndex by remember { mutableStateOf(-1) }
    var editInput by remember { mutableStateOf("") }

    var szlmId by remember { mutableStateOf(PrefManager.SZLMID) }
    var showSzlmDialog by remember { mutableStateOf(false) }
    var szlmInput by remember { mutableStateOf("") }

    var showRealDeviceDialog by remember { mutableStateOf(false) }

    var showTokenDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }

    var showDeviceDialog by remember { mutableStateOf(false) }
    var deviceInput by remember { mutableStateOf("") }

    var imageQualityIndex by remember {
        mutableStateOf(IMAGE_QUALITY_VALUES.indexOf(PrefManager.imageQuality).coerceAtLeast(0))
    }

    fun toast(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    // 进页面按当前参数刷新一次 v3 token，方便抓包对照/排错（同老实现）
    LaunchedEffect(Unit) {
        PrefManager.xAppToken = getLastingDeviceCode().getTokenV3(PrefManager.VERSION_CODE)
        revision += 1
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.params),
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
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            // ---- 机型检测 ----
            item {
                SmallTitle(text = "机型检测")
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    SwitchPreference(
                        title = stringResource(R.string.report_real_device),
                        summary = "按本机 Build.* 重建设备串，帖子/回复下方显示本机机型",
                        checked = reportRealDevice,
                        onCheckedChange = { checked ->
                            reportRealDevice = checked
                            PrefManager.reportRealDevice = checked
                            // 立刻重建设备串；关掉则回落到官方认可的那一组
                            if (checked) applyRealDeviceFingerprint() else applyDefaultFingerprint()
                            revision += 1
                            toast(
                                if (checked) "已按本机机型上报（需酷安收录该机型，否则帖子下方不显示）"
                                else "已回落到默认机型"
                            )
                        },
                    )
                    ArrowPreference(
                        title = stringResource(R.string.real_device_info),
                        summary = realDeviceSummary,
                        holdDownState = showRealDeviceDialog,
                        onClick = { showRealDeviceDialog = true },
                    )
                }
            }
            // ---- 数字联盟 ID ----
            item {
                SmallTitle(text = "数字联盟")
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    ArrowPreference(
                        title = stringResource(R.string.szlmId),
                        summary = szlmId.ifEmpty { stringResource(R.string.szlmId_summary) },
                        holdDownState = showSzlmDialog,
                        onClick = {
                            szlmInput = szlmId
                            showSzlmDialog = true
                        },
                    )
                }
            }
            // ---- 设备指纹参数 ----
            item {
                SmallTitle(text = "设备指纹参数")
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    paramItems.forEachIndexed { index, item ->
                        ArrowPreference(
                            title = item.title,
                            summary = values[index],
                            holdDownState = editingIndex == index,
                            onClick = {
                                editInput = values[index]
                                editingIndex = index
                            },
                        )
                    }
                }
            }
            // ---- 设备串 ----
            item {
                SmallTitle(text = "设备串")
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    ArrowPreference(
                        title = stringResource(R.string.xAppToken),
                        summary = xAppToken,
                        holdDownState = showTokenDialog,
                        onClick = {
                            tokenInput = xAppToken
                            showTokenDialog = true
                        },
                    )
                    ArrowPreference(
                        title = stringResource(R.string.xAppDevice),
                        summary = xAppDevice,
                        holdDownState = showDeviceDialog,
                        onClick = {
                            deviceInput = xAppDevice
                            showDeviceDialog = true
                        },
                    )
                    ArrowPreference(
                        title = stringResource(R.string.regenerate),
                        summary = "随机生成机型（MAC/尾字段沿用骨架，szlmId 走数字联盟ID）",
                        onClick = {
                            PrefManager.xAppDevice = getDeviceCode(true)
                            // 随机生成 = 显式自定义：置位后不再被自动还原
                            PrefManager.customFingerprint = true
                            revision += 1
                            toast("已重新生成随机机型（酷安未收录的机型不会显示）")
                        },
                    )
                }
            }
            // ---- 图片画质 ----
            item {
                SmallTitle(text = "图片")
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
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
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    // ---- 设备指纹参数编辑对话框（老 item_x_app_token 对话框）----
    val editingItem = paramItems.getOrNull(editingIndex)
    if (editingItem != null) {
        val hasExtraButton = editingItem.systemValue != null || editingItem.randomValue != null
        OverlayDialog(
            title = editingItem.title,
            show = true,
            onDismissRequest = { editingIndex = -1 },
        ) {
            TextField(
                value = editInput,
                onValueChange = { editInput = it },
                label = editingItem.title,
                singleLine = editingItem.title != "USER_AGENT",
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 与老对话框一致：有「系统信息/随机值」的字段不再放「取消」（点外面/返回键取消）
                if (!hasExtraButton) {
                    TextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = { editingIndex = -1 },
                        modifier = Modifier.weight(1f),
                    )
                }
                editingItem.systemValue?.let { systemValue ->
                    TextButton(
                        text = stringResource(R.string.system_info),
                        onClick = {
                            editingItem.write(systemValue())
                            editingItem.onSaved()
                            revision += 1
                            editingIndex = -1
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                editingItem.randomValue?.let { randomValue ->
                    TextButton(
                        text = stringResource(R.string.random_value),
                        onClick = {
                            editingItem.write(randomValue())
                            editingItem.onSaved()
                            revision += 1
                            editingIndex = -1
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                TextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = {
                        editingItem.write(editInput.ifEmpty { editingItem.fallback() })
                        editingItem.onSaved()
                        revision += 1
                        editingIndex = -1
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }

    // ---- 数字联盟 ID 编辑（老 szlmId 对话框）----
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
                        revision += 1
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
                    // 填过（非空）才算「已配置」；清空则回到未配置状态
                    PrefManager.szlmIdConfigured = id.isNotEmpty()
                    // szlmId 是设备串首字段，改完必须重造设备串才能生效
                    applyDefaultFingerprint()
                    szlmId = PrefManager.SZLMID
                    revision += 1
                    showSzlmDialog = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }

    // ---- 本机机型检测 ----
    OverlayDialog(
        title = stringResource(R.string.real_device_info),
        summary = detectRealDevice().let {
            "厂商：${it.manufacturer}\n品牌：${it.brand}\n型号：${it.model}\n" +
                "版本：${it.buildNumber}\nAndroid：${it.androidVersion}（API ${it.sdkInt}）"
        },
        show = showRealDeviceDialog,
        onDismissRequest = { showRealDeviceDialog = false },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = stringResource(android.R.string.cancel),
                onClick = { showRealDeviceDialog = false },
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = stringResource(R.string.real_device_apply),
                onClick = {
                    applyRealDeviceFingerprint()
                    PrefManager.reportRealDevice = true
                    reportRealDevice = true
                    revision += 1
                    showRealDeviceDialog = false
                    toast("设备串已按本机机型重建")
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }

    // ---- X-App-Token 编辑 ----
    OverlayDialog(
        title = stringResource(R.string.xAppToken),
        show = showTokenDialog,
        onDismissRequest = { showTokenDialog = false },
    ) {
        TextField(
            value = tokenInput,
            onValueChange = { tokenInput = it },
            label = stringResource(R.string.xAppToken),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = stringResource(android.R.string.cancel),
                onClick = { showTokenDialog = false },
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = stringResource(android.R.string.ok),
                onClick = {
                    PrefManager.xAppToken = tokenInput
                    revision += 1
                    showTokenDialog = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }

    // ---- X-App-Device 编辑（手填整串有风控风险，保留「恢复默认」通道）----
    OverlayDialog(
        title = stringResource(R.string.xAppDevice),
        show = showDeviceDialog,
        onDismissRequest = { showDeviceDialog = false },
    ) {
        TextField(
            value = deviceInput,
            onValueChange = { deviceInput = it },
            label = stringResource(R.string.xAppDevice),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = stringResource(android.R.string.cancel),
                onClick = { showDeviceDialog = false },
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = "恢复默认",
                onClick = {
                    applyDefaultFingerprint()
                    revision += 1
                    showDeviceDialog = false
                    toast("已恢复默认设备串")
                },
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = stringResource(android.R.string.ok),
                onClick = {
                    PrefManager.xAppDevice = deviceInput.ifEmpty { defaultDeviceCode() }
                    // 与默认串一致就不算自定义，避免以后指纹升级时被这份旧值卡住
                    PrefManager.customFingerprint =
                        PrefManager.xAppDevice != defaultDeviceCode()
                    revision += 1
                    showDeviceDialog = false
                    if (PrefManager.customFingerprint) {
                        toast("自定义设备串可能被酷安风控要求验证码")
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 设备指纹参数条目模型
// ---------------------------------------------------------------------------

/**
 * 一个可编辑的机型参数条目：读写都走 [PrefManager]，
 * [fallback] 是「留空确定」时填回的默认值（同老实现：为空则随机/取常量）。
 */
private class ParamItem(
    val title: String,
    val current: () -> String,
    val write: (String) -> Unit,
    val fallback: () -> String,
    val onSaved: () -> Unit = {},
    val systemValue: (() -> String)? = null,
    val randomValue: (() -> String)? = null,
)

/** 与老 params.xml 顺序一致。 */
private fun buildParamItems(): List<ParamItem> = listOf(
    ParamItem(
        title = "VERSION_NAME",
        current = { PrefManager.VERSION_NAME },
        write = { PrefManager.VERSION_NAME = it },
        fallback = { Constants.VERSION_NAME },
        onSaved = { updateUserAgent() },
    ),
    ParamItem(
        title = "API_VERSION",
        current = { PrefManager.API_VERSION },
        write = { PrefManager.API_VERSION = it },
        fallback = { Constants.API_VERSION },
    ),
    ParamItem(
        title = "VERSION_CODE",
        current = { PrefManager.VERSION_CODE },
        write = { PrefManager.VERSION_CODE = it },
        fallback = { Constants.VERSION_CODE },
        onSaved = { updateUserAgent() },
    ),
    ParamItem(
        title = "MANUFACTURER",
        current = { PrefManager.MANUFACTURER },
        write = { PrefManager.MANUFACTURER = it },
        fallback = { randomManufacturer() },
        onSaved = { PrefManager.xAppDevice = getDeviceCode(false) },
        systemValue = { Build.MANUFACTURER },
        randomValue = { randomManufacturer() },
    ),
    ParamItem(
        title = "BRAND",
        current = { PrefManager.BRAND },
        write = { PrefManager.BRAND = it },
        fallback = { randomBrand() },
        onSaved = {
            PrefManager.xAppDevice = getDeviceCode(false)
            updateUserAgent()
        },
        systemValue = { Build.BRAND },
        randomValue = { randomBrand() },
    ),
    ParamItem(
        title = "MODEL",
        current = { PrefManager.MODEL },
        write = { PrefManager.MODEL = it },
        fallback = { randomDeviceModel() },
        onSaved = {
            PrefManager.xAppDevice = getDeviceCode(false)
            updateUserAgent()
        },
        systemValue = { Build.MODEL },
        randomValue = { randomDeviceModel() },
    ),
    ParamItem(
        title = "BUILDNUMBER",
        current = { PrefManager.BUILDNUMBER },
        write = { PrefManager.BUILDNUMBER = it },
        fallback = { randHexString(16) },
        onSaved = {
            PrefManager.xAppDevice = getDeviceCode(false)
            updateUserAgent()
        },
        systemValue = { Build.DISPLAY },
        randomValue = { randHexString(16) },
    ),
    ParamItem(
        title = "SDK_INT",
        current = { PrefManager.SDK_INT },
        write = { PrefManager.SDK_INT = it },
        fallback = { randomSdkInt() },
        systemValue = { Build.VERSION.SDK_INT.toString() },
        randomValue = { randomSdkInt() },
    ),
    ParamItem(
        title = "ANDROID_VERSION",
        current = { PrefManager.ANDROID_VERSION },
        write = { PrefManager.ANDROID_VERSION = it },
        fallback = { randomAndroidVersionRelease() },
        onSaved = { updateUserAgent() },
        systemValue = { Build.VERSION.RELEASE },
        randomValue = { randomAndroidVersionRelease() },
    ),
    ParamItem(
        title = "USER_AGENT",
        current = { PrefManager.USER_AGENT },
        write = { PrefManager.USER_AGENT = it },
        fallback = { Constants.USER_AGENT },
    ),
)

/** 由当前机型参数拼 User-Agent（同老 ParamsPreferenceFragment.updateUserAgent）。 */
private fun updateUserAgent() {
    PrefManager.USER_AGENT =
        "Dalvik/2.1.0 (Linux; U; Android ${PrefManager.ANDROID_VERSION}; ${PrefManager.MODEL} ${PrefManager.BUILDNUMBER}) (#Build; ${PrefManager.BRAND}; ${PrefManager.MODEL}; ${PrefManager.BUILDNUMBER}; ${PrefManager.ANDROID_VERSION}) +CoolMarket/${PrefManager.VERSION_NAME}-${PrefManager.VERSION_CODE}-${Constants.MODE}"
}

private val IMAGE_QUALITY_LABELS = listOf("网络自适应", "原图", "普清")

private val IMAGE_QUALITY_VALUES = listOf("auto", "origin", "thumbnail")
