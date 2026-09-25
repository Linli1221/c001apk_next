package com.example.c001apk.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.FabPosition
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.ToolbarPosition
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 统一页面骨架：Miuix [Scaffold] + [SmallTopAppBar]/[TopAppBar]（返回 / 标题 / 右侧动作插槽）。
 *
 * 所有迁移后的页面级 Screen 建议直接用它组织页面结构，保证顶栏形态、返回按钮、
 * 滚动折叠行为一致；页面自己不要再套第二个 Scaffold。
 *
 * **用途**
 * - 小标题页（默认）：`TopBarScaffold(title = "标题", onBack = { ... }) { padding -> ... }`
 * - 大标题折叠页：传 [largeTitle]，组件内部创建 [MiuixScrollBehavior] 并把
 *   `nestedScrollConnection` 包在 content 外层，内容里的 LazyColumn/滚动容器滚动时顶栏自动折叠。
 * - 右侧动作：[actions] 插槽（`RowScope`），放 [IconButton] 等。
 *
 * **参数**
 * @param title 顶栏标题（折叠态小标题）。
 * @param modifier 应用于骨架的 [Modifier]。
 * @param largeTitle 大标题文本；`null`（默认）使用 [SmallTopAppBar]，非 null 使用可折叠 [TopAppBar]。
 * @param subtitle 副标题文本，显示在标题栏下方，默认空串不显示。
 * @param onBack 返回回调；非 null 时自动显示返回箭头（`MiuixIcons.Back`）。根页面传 null 即无返回键。
 * @param navigationIcon 自定义导航图标插槽；非 null 时覆盖默认返回键（[onBack] 被忽略）。
 * @param actions 顶栏右侧动作区（`RowScope`）。
 * @param bottomBar 底部栏插槽（如 NavigationBar）。
 * @param floatingActionButton 悬浮按钮插槽。
 * @param floatingActionButtonPosition 悬浮按钮位置，默认 [FabPosition.End]。
 * @param floatingToolbar 悬浮工具栏插槽。
 * @param floatingToolbarPosition 悬浮工具栏位置，默认 [ToolbarPosition.BottomCenter]。
 * @param snackbarHost Snackbar 容器（Miuix 不提供 Snackbar 组件，需要时由页面自己放）。
 * @param containerColor 骨架背景色，默认 [MiuixTheme.colorScheme.surface]。
 * @param content 页面内容，接收 Scaffold 的 [PaddingValues]（页面自己负责 `padding(paddingValues)`）。
 *
 * **祖先要求**：必须位于根主题 `com.example.c001apk.ui.theme.MiuixAppTheme`（即 [MiuixTheme]）之内；
 * 本组件自带一个 Miuix [Scaffold]，因此其 content 内可以直接使用 Overlay* 系列弹窗组件。
 * 有大标题折叠时，内容里的滚动容器无需额外处理，嵌套滚动已由本组件接线。
 */
@Composable
fun TopBarScaffold(
    title: String,
    modifier: Modifier = Modifier,
    largeTitle: String? = null,
    subtitle: String = "",
    onBack: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    floatingToolbar: @Composable () -> Unit = {},
    floatingToolbarPosition: ToolbarPosition = ToolbarPosition.BottomCenter,
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = MiuixTheme.colorScheme.surface,
    content: @Composable (PaddingValues) -> Unit,
) {
    // 大标题模式才需要滚动折叠行为
    val scrollBehavior = if (largeTitle != null) MiuixScrollBehavior() else null

    Scaffold(
        modifier = modifier,
        topBar = {
            if (largeTitle != null) {
                TopAppBar(
                    title = title,
                    largeTitle = largeTitle,
                    subtitle = subtitle,
                    navigationIcon = navigationIcon ?: {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = MiuixIcons.Back,
                                    contentDescription = "返回",
                                )
                            }
                        }
                    },
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                )
            } else {
                SmallTopAppBar(
                    title = title,
                    subtitle = subtitle,
                    navigationIcon = navigationIcon ?: {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = MiuixIcons.Back,
                                    contentDescription = "返回",
                                )
                            }
                        }
                    },
                    actions = actions,
                )
            }
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        floatingToolbar = floatingToolbar,
        floatingToolbarPosition = floatingToolbarPosition,
        snackbarHost = snackbarHost,
        containerColor = containerColor,
    ) { paddingValues ->
        if (scrollBehavior != null) {
            // 大标题模式：把折叠行为接线到内容滚动（内容内 LazyColumn 等滚动会传导到这里）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
            ) {
                content(paddingValues)
            }
        } else {
            content(paddingValues)
        }
    }
}
