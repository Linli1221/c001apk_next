package com.example.c001apk.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Home

/**
 * 主壳底部导航的两个 tab，对应老 res/menu/nav_menu.xml 的
 * navigation_home / navigation_mine。
 */
enum class MainTab { Home, Mine }

/**
 * Miuix 风格主壳（对应老 MainActivity + activity_main.xml）：
 * 底部 [NavigationBar]（首页 / 我的）+ 页面容器插槽。
 *
 * 只负责壳层结构与 tab 切换，不承载业务：
 * - tab 状态由调用方提升（[selectedTab] / [onTabSelected]），方便接 ViewModel 或 ViewPager 遗留逻辑；
 * - 每个页面以 composable lambda 形式传入（[homePage] / [minePage]），页面自己的顶栏、
 *   加载态、空态由各自页面负责（老 HomeFragment / MineFragment 结构不变）；
 * - 「设置」入口回调 [onSettingsClick] 转发给「我的」页插槽：老代码的设置图标在「我的」tab
 *   的工具栏内（MineFragment → message_menu），入口 UI 由「我的」页渲染，跳转目标（SettingsActivity）
 *   在接线时由壳层统一提供；
 * - tab 再次点击走 [onTabReselected]（老交互：首页再点回顶、我的再点清小红点）；
 * - 停留在「我的」时按系统返回先回「首页」（老 onBackPressedCallback）。
 *
 * 页面切换用 [rememberSaveableStateHolder] 保留 rememberSaveable 状态（列表滚动位置等），
 * 对应老 ViewPager2（offscreenPageLimit = 1）的 tab 保活体验。
 *
 * 接线示例（后续轮，Activity 的 setContent 里，外层已由 MiuixAppTheme 包裹）：
 * ```
 * MainScreen(
 *     selectedTab = tab,
 *     onTabSelected = { tab = it },
 *     onSettingsClick = { /* 打开 SettingsActivity */ },
 *     homePage = { HomeScreen(...) },
 *     minePage = { onSettingsClick -> MineScreen(onSettingsClick = onSettingsClick, ...) },
 * )
 * ```
 *
 * @param selectedTab 当前选中的 tab（提升的状态）。
 * @param onTabSelected 切换到别的 tab 的回调。
 * @param onSettingsClick 「设置」入口回调，转发给「我的」页插槽，由该页的设置图标触发。
 * @param homePage 「首页」页内容（不含壳层，页面自带顶栏/加载态）。
 * @param minePage 「我的」页内容；参数是壳层提供的「设置」入口回调，页面内的设置图标调用它。
 * @param modifier 应用到壳层 [Scaffold] 的 [Modifier]。
 * @param onTabReselected 再次点击当前 tab 的回调（默认不做事）。
 * @param homeBadge 「首页」tab 图标上的角标（如消息数小红点），null = 不显示；
 *   由调用方用 Miuix 的 Badge 等组件拼出后传入。
 * @param mineBadge 「我的」tab 图标上的角标（老 CookieUtil.badge），null = 不显示。
 * @param mode 底部导航的图标/文字显示模式；
 *   默认 [NavigationBarDisplayMode.IconWithSelectedLabel] 对应老 XML 的 labelVisibilityMode="selected"，
 *   想要 Miuix 默认的「图标+文字常显」改成 [NavigationBarDisplayMode.IconAndText] 即可。
 */
@Composable
fun MainScreen(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onSettingsClick: () -> Unit,
    homePage: @Composable () -> Unit,
    minePage: @Composable (onSettingsClick: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    onTabReselected: (MainTab) -> Unit = {},
    homeBadge: (@Composable () -> Unit)? = null,
    mineBadge: (@Composable () -> Unit)? = null,
    mode: NavigationBarDisplayMode = NavigationBarDisplayMode.IconWithSelectedLabel,
) {
    val stateHolder = rememberSaveableStateHolder()

    // 老 onBackPressedCallback：停留在「我的」时返回先回「首页」
    BackHandler(enabled = selectedTab == MainTab.Mine) {
        onTabSelected(MainTab.Home)
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(mode = mode) {
                NavigationBarItem(
                    selected = selectedTab == MainTab.Home,
                    onClick = {
                        if (selectedTab == MainTab.Home) onTabReselected(MainTab.Home)
                        else onTabSelected(MainTab.Home)
                    },
                    icon = MiuixIcons.Home,
                    label = "首页",
                    badge = homeBadge,
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Mine,
                    onClick = {
                        if (selectedTab == MainTab.Mine) onTabReselected(MainTab.Mine)
                        else onTabSelected(MainTab.Mine)
                    },
                    icon = MiuixIcons.Contacts,
                    label = "我的",
                    badge = mineBadge,
                )
            }
        },
    ) { paddingValues ->
        // 按 Scaffold 约定消费 contentPadding，避免页面内层再叠加一遍 insets/导航栏高度
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            stateHolder.SaveableStateProvider(selectedTab.name) {
                when (selectedTab) {
                    MainTab.Home -> homePage()
                    MainTab.Mine -> minePage(onSettingsClick)
                }
            }
        }
    }
}
