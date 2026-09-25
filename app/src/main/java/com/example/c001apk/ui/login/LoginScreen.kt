package com.example.c001apk.ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.c001apk.R
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 酷安登录页面（Miuix 版）—— 对应老的登录流程（[WebLoginActivity] + 登录态三 cookie）。
 *
 * 现状（与老代码核对过，别按「账号/密码/短信表单」想象）：
 * - 表单登录（账号/密码/短信）**已随表单登录一起移除**（见 ApiService.kt 的注释）：
 *   短信验证码、图形验证码、二次验证、第三方登录全部由酷安官方网页负责，入口是
 *   [WebLoginActivity]（WebView 抓 cookie）。
 * - 登录态 = PrefManager.uid / username / token 三个 cookie（见 AddCookiesInterceptor），
 *   由 MainViewModel.checkLoginInfo() 校验/刷新。
 * - 设备标识（X-App-Device / X-App-Token / 数字联盟 ID）由 util/TokenDeviceUtils 生成，
 *   与登录风控（-415 / 验证码）相关，属「设置 → 高级」页的职责，本页只做提示，不重复实现。
 *
 * 因此本文件只负责界面与交互（业务提交一律走回调，不在 Composable 里新建业务逻辑）：
 * - [LoginScreen]：页面骨架（Scaffold + TopAppBar + Snackbar 错误提示 + 分步流程插槽）。
 * - [WebLoginStep]：网页登录入口（回调 onStartWebLogin → 接线层启动 [WebLoginActivity]）。
 * - [TokenLoginForm]：手动 Token 登录表单（uid / username / token + 协议勾选 + 覆盖确认弹窗），
 *   提交回调 onSubmitTokenLogin(LoginCredentials) 由接线层写 PrefManager / 走 MainViewModel 校验。
 *
 * 注意：
 * - 不自带 MiuixAppTheme（根主题由 Activity 接线时套）；页面自含一个 Scaffold
 *   （OverlayDialog 需要 Scaffold 祖先，Snackbar 挂在 Scaffold 的 snackbarHost 槽）。
 * - 表单状态提升进 [LoginUiState]，父级可持有（放进 ViewModel 以便进程重建恢复）。
 */
@Composable
fun LoginScreen(
    onStartWebLogin: () -> Unit,
    onSubmitTokenLogin: (LoginCredentials) -> Unit,
    modifier: Modifier = Modifier,
    state: LoginUiState = rememberLoginUiState(),
    steps: List<LoginStep> = listOf(LoginStep.WebLogin, LoginStep.TokenLogin),
    loggedIn: Boolean = false,
    title: String = stringResource(R.string.login),
    onBack: (() -> Unit)? = null,
    onOpenUserAgreement: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    /** 分步流程插槽：非空时完全接管每一步的渲染（传进来的 lambda 跑在 Scaffold 内容里） */
    stepContent: (@Composable ColumnScope.(LoginStep) -> Unit)? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // 提交失败等业务错误由父级写进 state.errorMessage，这里统一用 Miuix Snackbar 提示并消费
    val errorMessage = state.errorMessage
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            state.errorMessage = null
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
                                MiuixIcons.Back,
                                contentDescription = "返回",
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(state = snackbarHostState)
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            if (stepContent != null) {
                stepContent(state.step)
            } else {
                when (state.step) {
                    LoginStep.WebLogin -> WebLoginStep(
                        onStartWebLogin = onStartWebLogin,
                        onUseTokenLogin = {
                            if (LoginStep.TokenLogin in steps) state.step = LoginStep.TokenLogin
                        },
                        showTokenLoginEntry = LoginStep.TokenLogin in steps,
                    )

                    LoginStep.TokenLogin -> TokenLoginForm(
                        state = state,
                        onSubmit = onSubmitTokenLogin,
                        loggedIn = loggedIn,
                        onUseWebLogin = {
                            if (LoginStep.WebLogin in steps) state.step = LoginStep.WebLogin
                        },
                        showWebLoginEntry = LoginStep.WebLogin in steps,
                        onOpenUserAgreement = onOpenUserAgreement,
                        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                    )
                }
            }
        }
    }
}

/** 登录分步流程的一步；[LoginScreen] 的 stepContent 插槽可替换任意一步的渲染。 */
enum class LoginStep {
    /** 网页登录入口（酷安官方登录页，短信/图形验证码都在那边完成） */
    WebLogin,

    /** 手动 Token 登录（uid / username / token 三个 cookie） */
    TokenLogin,
}

/** Token 登录提交载荷；username 允许为空（cookie 里非必填）。 */
@Immutable
data class LoginCredentials(
    val uid: String,
    val username: String,
    val token: String,
)

/**
 * 登录表单/流程的展示状态（状态提升进 holder，业务动作仍走 [LoginScreen] 的回调）。
 * 父级可自行持有本对象（例如塞进 ViewModel），默认用 [rememberLoginUiState]。
 */
@Stable
class LoginUiState(
    initialStep: LoginStep = LoginStep.WebLogin,
) {
    var step by mutableStateOf(initialStep)
    var uid by mutableStateOf("")
    var username by mutableStateOf("")
    var token by mutableStateOf("")
    var agreed by mutableStateOf(false)

    /** 提交中（由父级在提交回调里置位/复位，按钮转圈并禁用） */
    var submitting by mutableStateOf(false)

    /** 业务错误（提交失败等）；非空时被 Snackbar 消费一次后清空 */
    var errorMessage by mutableStateOf<String?>(null)

    /** 提交载荷（自动 trim） */
    val credentials: LoginCredentials
        get() = LoginCredentials(
            uid = uid.trim(),
            username = username.trim(),
            token = token.trim(),
        )
}

@Composable
fun rememberLoginUiState(initialStep: LoginStep = LoginStep.WebLogin): LoginUiState =
    remember { LoginUiState(initialStep) }

/**
 * 网页登录入口步骤：说明文案 + 「网页登录」主按钮 + 「Token 登录」入口 + 设备标识提示。
 * 点击「网页登录」回调 onStartWebLogin，由接线层启动 [WebLoginActivity]（WebView 流程）。
 */
@Composable
fun WebLoginStep(
    onStartWebLogin: () -> Unit,
    modifier: Modifier = Modifier,
    onUseTokenLogin: () -> Unit = {},
    showTokenLoginEntry: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "登录后可评论、收藏、关注，并同步云端黑名单与消息提醒。",
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onStartWebLogin,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            Text(stringResource(R.string.loginWeb))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "网页登录打开酷安官方登录页，短信验证码、图形验证码与二次验证都在官方页面完成；" +
                "登录成功后自动写回本地登录态。",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        SmallTitle(text = "设备标识")
        Text(
            text = "酷安按设备做风控限流。设备串（X-App-Device / X-App-Token / 数字联盟 ID）在「设置 → 高级」" +
                "里查看与调整，由 TokenDeviceUtils 生成；换设备或手改设备串可能触发人机验证。",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        if (showTokenLoginEntry) {
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                text = "使用 Token 登录（高级）",
                onClick = onUseTokenLogin,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Token 登录表单：uid / username / token 三个输入框 + 协议勾选 + 提交按钮。
 * 表单值在 [state]（已提升），提交回调 onSubmit 收到 trim 后的 [LoginCredentials]；
 * 本已登录（loggedIn = true）时先用 [OverlayDialog] 确认覆盖再提交。
 */
@Composable
fun TokenLoginForm(
    state: LoginUiState,
    onSubmit: (LoginCredentials) -> Unit,
    modifier: Modifier = Modifier,
    loggedIn: Boolean = false,
    showWebLoginEntry: Boolean = true,
    onUseWebLogin: () -> Unit = {},
    onOpenUserAgreement: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
) {
    // 校验错误是纯展示态，用本地 remember；业务错误走 state.errorMessage（Snackbar）
    var validationError by remember { mutableStateOf<String?>(null) }
    var showOverwriteConfirm by remember { mutableStateOf(false) }

    fun trySubmit() {
        val error = validateTokenForm(state)
        if (error != null) {
            validationError = error
            return
        }
        validationError = null
        if (loggedIn) {
            showOverwriteConfirm = true
        } else {
            onSubmit(state.credentials)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SmallTitle(text = "Token 登录")
        TextField(
            value = state.uid,
            onValueChange = { state.uid = it },
            label = "UID",
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextField(
            value = state.username,
            onValueChange = { state.username = it },
            label = "用户名（可选）",
            useLabelAsPlaceholder = true,
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextField(
            value = state.token,
            onValueChange = { state.token = it },
            label = "Token",
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(16.dp))
        AgreementCheckbox(
            checked = state.agreed,
            onCheckedChange = { state.agreed = it },
            onOpenUserAgreement = onOpenUserAgreement,
            onOpenPrivacyPolicy = onOpenPrivacyPolicy,
        )
        validationError?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.error,
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { trySubmit() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.submitting,
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            if (state.submitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 3.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("登录")
        }
        if (showWebLoginEntry) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                text = "改用网页登录",
                onClick = onUseWebLogin,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "UID/Token 来自抓包或网页登录后的 cookie（uid / username / token）。" +
                "登录态会在下次启动时由 checkLoginInfo 校验，失效会自动退出登录。",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }

    if (showOverwriteConfirm) {
        OverlayDialog(
            title = "覆盖当前登录态？",
            summary = "检测到本机已有登录账号。继续将用新填的 UID/Token 覆盖本地登录态。",
            show = true,
            onDismissRequest = { showOverwriteConfirm = false },
        ) {
            Column {
                TextButton(
                    text = "覆盖并登录",
                    onClick = {
                        showOverwriteConfirm = false
                        onSubmit(state.credentials)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    text = "取消",
                    onClick = { showOverwriteConfirm = false },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** 协议勾选行：Checkbox + 可点击的《用户协议》《隐私政策》。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgreementCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpenUserAgreement: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            state = ToggleableState(checked),
            onClick = { onCheckedChange(!checked) },
        )
        Spacer(modifier = Modifier.width(4.dp))
        FlowRow {
            Text(
                text = "已阅读并同意",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                text = "《用户协议》",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onOpenUserAgreement),
            )
            Text(
                text = "与",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                text = "《隐私政策》",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onOpenPrivacyPolicy),
            )
        }
    }
}

/** Token 表单校验；返回 null 表示通过，否则返回中文错误提示。 */
private fun validateTokenForm(state: LoginUiState): String? = when {
    state.uid.isBlank() -> "请填写 UID"
    state.uid.trim().any { !it.isDigit() } -> "UID 应为数字"
    state.token.isBlank() -> "请填写 Token"
    !state.agreed -> "请先阅读并同意用户协议与隐私政策"
    else -> null
}
