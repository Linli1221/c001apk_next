package com.example.c001apk.ui.base

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/**
 * Compose 侧的轻量页面基座（对应老 BaseActivity/BaseFragment + 各页 XML 顶栏的公共部分）：
 * 一个 Miuix [Scaffold] + [SmallTopAppBar]（标题、可选返回键、右侧 actions）。
 *
 * 约定（与 CONVENTIONS.md 对齐）：
 * - 不套 MiuixTheme（根主题是 ui/theme 的 MiuixAppTheme），不引 ViewModel，只做壳；
 * - 页面内自含 [Scaffold] 是刻意的：Overlay 系列弹窗（OverlayDialog/OverlayBottomSheet 等）
 *   需要 Scaffold 祖先，套上本基座的页面天然获得弹窗宿主；
 * - 业务内容通过 [content] 传入，拿到 Scaffold 的 [PaddingValues] 自行应用
 *   （一般 `Modifier.padding(paddingValues)` 在滚动容器的外层或 contentPadding 上消化）；
 * - 顶栏只做「返回 + 标题 + actions」这种标准页；需要大标题折叠、自定义头图的页面
 *   （首页/我的/用户主页等）自己写 TopAppBar + Scaffold，不必硬套本基座。
 *
 * 示例：
 * ```
 * BasePageScaffold(
 *     title = "设置",
 *     onNavigateBack = { onBack() },
 * ) { paddingValues ->
 *     LazyColumn(contentPadding = paddingValues) { ... }
 * }
 * ```
 *
 * @param title 顶栏标题。
 * @param onNavigateBack 返回键回调；null = 不显示返回键（顶层页面）。
 * @param modifier 应用到 [Scaffold] 的 [Modifier]。
 * @param actions 顶栏右侧动作区（RowScope，可放多个 IconButton）。
 * @param content 页面内容，参数是 Scaffold 的 [PaddingValues]。
 */
@Composable
fun BasePageScaffold(
    title: String,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            SmallTopAppBar(
                title = title,
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = "返回",
                            )
                        }
                    }
                },
                actions = actions,
            )
        },
        content = content,
    )
}
