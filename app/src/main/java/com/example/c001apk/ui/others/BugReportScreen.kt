package com.example.c001apk.ui.others

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.c001apk.util.ClipboardUtil
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 崩溃日志页（Compose + Miuix 版），替代原 [BugHandlerActivity] + activity_bug_handler.xml。
 *
 * 与老版行为对齐：
 *  - 展示 `intent.getStringExtra("exception_message")` 传入的崩溃文本（等宽字体、可选中复制）；
 *  - 进入页面时自动把日志复制到剪贴板（老代码同样静默复制，方便用户直接去反馈）。
 *
 * 新增交互（老版只有展示 + 静默复制）：
 *  - 「复制日志」按钮：走 [ClipboardUtil.copyText]（带「已复制」Toast）；
 *  - 「重启应用」按钮：由调用方通过 [onRestartApp] 接线
 *    （老 [CopyActivity] 里的重启写法：`packageManager.getLaunchIntentForPackage(packageName)` + FLAG_ACTIVITY_CLEAR_TOP）。
 *
 * 状态/业务全部提升为参数：[crashLog] 为崩溃文本，[onRestartApp] / [onBack] 为回调占位。
 */
@Composable
fun BugReportScreen(
    crashLog: String,
    onRestartApp: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current

    // 老行为：进入页面即静默复制一份日志，方便用户直接粘贴去反馈
    LaunchedEffect(crashLog) {
        if (crashLog.isNotBlank()) {
            ClipboardUtil.copyText(context, crashLog, showToast = false)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            SmallTopAppBar(
                title = "Crash Log",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .verticalScroll(rememberScrollState()),
        ) {
            if (crashLog.isBlank()) {
                // 空态
                Text(
                    text = "没有可显示的崩溃日志",
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                Text(
                    text = "应用发生崩溃，以下是崩溃日志",
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(12.dp),
                ) {
                    SelectionContainer {
                        Text(
                            text = crashLog,
                            style = MiuixTheme.textStyles.footnote1,
                            fontFamily = FontFamily.Monospace,
                            color = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 操作区：复制日志 / 重启应用
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { ClipboardUtil.copyText(context, crashLog) },
                    enabled = crashLog.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("复制日志")
                }
                Button(
                    onClick = onRestartApp,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("重启应用")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
