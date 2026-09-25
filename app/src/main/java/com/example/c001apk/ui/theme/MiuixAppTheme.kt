package com.example.c001apk.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * Miuix 迁移后的应用根主题，全 App 唯一的主题封装。
 *
 * 所有 Compose 页面/组件都必须挂在 [MiuixAppTheme] 下面，页面内不要再自行创建 MiuixTheme；
 * 颜色与字体统一从 MiuixTheme.colorScheme / MiuixTheme.textStyles 读取语义 token。
 *
 * [colorSchemeMode] 后续会与 PrefManager 的深色主题设置（PrefManager.darkTheme）打通，
 * 映射为跟随系统 / 浅色 / 深色；预览或单页调试时也可以直接传入固定模式
 * （如 [ColorSchemeMode.Light] / [ColorSchemeMode.Dark]）以获得确定性的渲染结果。
 *
 * @param colorSchemeMode 配色模式，默认 [ColorSchemeMode.System] 跟随系统深浅色。
 * @param content 主题作用域内的页面内容。
 */
@Composable
fun MiuixAppTheme(
    colorSchemeMode: ColorSchemeMode = ColorSchemeMode.System,
    content: @Composable () -> Unit,
) {
    // 以 colorSchemeMode 为 key 记忆 ThemeController：
    // 模式不变时保持同一个 controller 实例，模式切换（后续接 PrefManager 设置）时才重建
    val controller = remember(colorSchemeMode) { ThemeController(colorSchemeMode = colorSchemeMode) }
    MiuixTheme(controller = controller) { content() }
}
