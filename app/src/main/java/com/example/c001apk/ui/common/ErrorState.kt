package com.example.c001apk.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 错误态占位（错误文案 + 重试按钮），对应旧布局 `item_error_layout.xml`。
 *
 * **用途**：网络/加载失败时的居中提示与重试入口。
 * 典型用法：`ErrorState(modifier = Modifier.fillMaxSize(), onRetry = { viewModel.refresh() })`。
 *
 * **参数**
 * @param onRetry 重试回调；非 null 才显示重试按钮（Miuix [Button]）。
 * @param modifier 应用于外层容器的 [Modifier]；传 `Modifier.fillMaxSize()` 时内容在容器内居中。
 * @param message 错误提示文案，默认「加载失败」（与 `strings.xml` 中 `loading_failed` 一致）。
 * @param retryText 重试按钮文案，默认「重试」（与 `strings.xml` 中 `retry` 一致）。
 * @param icon 可选图标插槽，默认不显示。
 *
 * **祖先要求**：必须位于根主题 `MiuixAppTheme`（即 [MiuixTheme]）之内；无其他前置要求。
 */
@Composable
fun ErrorState(
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    message: String = "加载失败",
    retryText: String = "重试",
    icon: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp),
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = message,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRetry) {
                    Text(retryText)
                }
            }
        }
    }
}
