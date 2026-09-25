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
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 加载中状态占位（Miuix [CircularProgressIndicator] + 可选文案）。
 *
 * **用途**：页面/列表首次加载、局部刷新时的居中转圈占位。
 * 典型用法：`LoadingState(modifier = Modifier.fillMaxSize())`，
 * 或放在列表 footer：`LoadingState(modifier = Modifier.fillMaxWidth().padding(16.dp))`。
 *
 * **参数**
 * @param modifier 应用于外层容器的 [Modifier]；传 `Modifier.fillMaxSize()` 时内容在容器内居中。
 * @param text 转圈下方的提示文案（默认「加载中…」）；传 `null` 只显示转圈。
 *
 * **祖先要求**：必须位于根主题 `MiuixAppTheme`（即 [MiuixTheme]）之内；无其他前置要求。
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    text: String? = "加载中…",
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp),
        ) {
            CircularProgressIndicator()
            if (text != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = text,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
