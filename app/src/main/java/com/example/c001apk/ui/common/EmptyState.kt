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
 * 空态占位（可选图标 + 文案 + 可选动作按钮）。
 *
 * **用途**：列表/页面数据为空（如搜索无结果、收藏为空）时的居中提示。
 * 典型用法：`EmptyState(modifier = Modifier.fillMaxSize(), text = "暂无收藏")`。
 *
 * **参数**
 * @param modifier 应用于外层容器的 [Modifier]；传 `Modifier.fillMaxSize()` 时内容在容器内居中。
 * @param text 主提示文案，默认「暂无内容」。
 * @param icon 可选图标插槽（如 `Icon(MiuixIcons.Info, ...)`），默认不显示。
 * @param actionText 可选动作按钮文案（如「去逛逛」）；与 [onAction] 同时非 null 才显示按钮。
 * @param onAction 动作按钮点击回调。
 *
 * **祖先要求**：必须位于根主题 `MiuixAppTheme`（即 [MiuixTheme]）之内；无其他前置要求。
 */
@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    text: String = "暂无内容",
    icon: (@Composable () -> Unit)? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
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
                text = text,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
            )
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onAction) {
                    Text(actionText)
                }
            }
        }
    }
}
