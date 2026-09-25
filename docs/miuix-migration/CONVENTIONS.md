# c001apk_next → Miuix 迁移共享契约（所有迁移子代理必读）

> 本文件是并行迁移的唯一对齐依据。任何与本文件冲突的即兴发挥都是错误。
> 主代理负责：构建链、依赖、提交推送、GitHub Actions 编译验证、后续接线。

## 0. 你在做什么

c001apk_next 是一个 View 体系（XML + ViewBinding/DataBinding + Material）的酷安第三方客户端。
现在要把 **UI 层** 全量重写为 **Jetpack Compose + Miuix**（HyperOS 设计语言组件库），
**逻辑层（`logic/` 下 network/repository/database/model、`util/`）一律复用，不要动**。

- Miuix 坐标（纯 Android 制品，Maven Central，**0.9.4**）：
  - `top.yukonga.miuix.kmp:miuix-ui-android:0.9.4`
  - `top.yukonga.miuix.kmp:miuix-preference-android:0.9.4`
  - `top.yukonga.miuix.kmp:miuix-icons-android:0.9.4`
- Compose BOM `androidx.compose:compose-bom:2026.09.00`，`androidx.activity:activity-compose:1.13.0`，
  `androidx.lifecycle:lifecycle-runtime-compose / lifecycle-viewmodel-compose / lifecycle-livedata-compose:2.11.0`。
- 依赖由主代理统一接入；**你不要改任何 gradle 文件**。

## 1. 硬性纪律（违反即返工）

1. **禁止臆造 Miuix API**。用任何组件前必须核对真实签名，核对方式（本地就有 Miuix v0.9.4 源码，commit `39c40f99`）：
   - 文档/演示：`C:\code\coolapk\miuix\docs\components\*.md`、`C:\code\coolapk\miuix\docs\demo\*.kt`
   - 源码：`git -C C:\code\coolapk\miuix show 39c40f99:miuix-ui/src/commonMain/kotlin/top/yukonga/miuix/kmp/<path>`
   - 封面组件（preference/menu/popup）在 `miuix-preference/src/commonMain/...`，图标在 `miuix-core`/`miuix-icons`。
   - 不确定参数顺序/名字时，宁可少用花活、用基础组件拼，也不要编。
2. **可以先调用 `skill` 工具加载 `miuix` 技能**（强烈建议），按其 Guardrails 执行。
3. **只写自己负责目录内的文件**。不许改：`AndroidManifest.xml`、任何 `*.gradle.kts`、`gradle/**`、
   `settings.gradle.kts`、别人的 `ui/xxx` 目录、`logic/**`、`util/**`、`res/**`（除非自己目录配套必需且已在报告中声明）。
4. **不要跑任何 git 命令**（提交/推送由主代理统一做）。不要跑 gradle（本机没有 JDK/Android SDK）。
5. 新代码必须能与现有 View 代码**共存编译**：只新增文件（或改自己目录内既有文件），老 View 代码本轮不删。
6. 本轮允许少量重复（例如各页自己的顶栏/加载态）；公共组件收敛是后续轮的事。

## 2. 产出形态（每个页面组）

- 一个页面级 `@Composable`（可拆多个文件），放自己的目录，命名 `<名字>Screen.kt`。
- 函数参数：把业务状态/回调**提升**为参数（或复用现有 ViewModel），不要在 Composable 里 new 业务逻辑。
- 复用现有 ViewModel（`androidx.lifecycle.ViewModel` + `LiveData`）：
  - 在 `Screen` 里用 `viewModel.xxx.observeAsState()`（`androidx.lifecycle:lifecycle-livedata-compose`）或
    `by viewModel.xxx`（`lifecycle-viewmodel-compose`）取状态；不要重写 ViewModel/Repository。
- **不要自己套 `MiuixTheme`**：根主题是 `com.example.c001apk.ui.theme.MiuixAppTheme`（另一代理在写，签名如下，
  可以假定存在）：
  ```kotlin
  @Composable
  fun MiuixAppTheme(
      colorSchemeMode: ColorSchemeMode = ColorSchemeMode.System, // top.yukonga.miuix.kmp.theme
      content: @Composable () -> Unit,
  )
  ```
  页面 `Screen` 直接写内容即可；Activity/Fragment 的 `setContent { MiuixAppTheme { XxxScreen(...) } }` 接线是后续轮的事。
- **不要自己套第二个 `Scaffold` 当根**：页面用 Miuix 的 `Scaffold`（topBar/bottomBar/floatingActionButton 槽位）组织页面级结构即可；
  Overlay 弹窗组件（OverlayDialog/OverlayBottomSheet 等）需要 `Scaffold` 祖先，页面内自含一个 `Scaffold` 是允许的。

## 3. 组件选型速查（Miuix，不许混 Material3）

| 场景 | 用什么 |
|---|---|
| 页面骨架/内边距 | `Scaffold`（topBar/bottomBar/floatingActionButton/floatingToolbar 槽位） |
| 顶栏 | `TopAppBar`（返回、标题、更多） |
| 底部导航 | `NavigationBar` / `NavigationBarItem`（或 `FloatingNavigationBar`） |
| 标签页 | `TabRow` |
| 按钮/开关/复选/单选 | `Button`/`TextButton`/`IconButton`、`Switch`、`Checkbox`、`RadioButton` |
| 列表项/设置项 | `BasicComponent`；设置页用 `miuix-preference`：`SwitchPreference`、`CheckboxPreference`、`SliderPreference`、`ArrowPreference`、`OverlayDropdownPreference` 等 |
| 卡片 | `Card` |
| 输入 | `TextField` / `InputField`；搜索用 `SearchBar`（注意：颜色参数在 `InputField.color`） |
| 下拉刷新 | `PullToRefresh`（`isRefreshing` 提升；`rememberPullToRefreshState`） |
| 对话框/底部弹层 | 页面内用 `Overlay*` 系列（需 `Scaffold` 祖先）；全局用 `Window*` 系列 |
| 加载/进度 | `ProgressIndicator`（不确定 API 就先用基础组件） |
| 分割线/文字 | `Divider`、`Text` + `MiuixTheme.textStyles.*` |
| 颜色 | 一律 `MiuixTheme.colorScheme.*`；不许硬编码色值；红=error；没有 warning/alert/success 语义色 |

主题 API（已核对 v0.9.4 源码，可直接用）：
```kotlin
package top.yukonga.miuix.kmp.theme
class ThemeController(
    colorSchemeMode: ColorSchemeMode = ColorSchemeMode.System,
    lightColors: Colors = lightColorScheme(),
    darkColors: Colors = darkColorScheme(),
    keyColor: Color? = null,
    colorSpec: ThemeColorSpec = ThemeColorSpec.Spec2021,
    paletteStyle: ThemePaletteStyle = ThemePaletteStyle.TonalSpot,
    isDark: Boolean? = null,
)
@Composable fun MiuixTheme(controller: ThemeController, textStyles: TextStyles = MiuixTheme.textStyles, content: @Composable () -> Unit)
enum class ColorSchemeMode { System, Light, Dark, MonetSystem, MonetLight, MonetDark }
object MiuixTheme { val colorScheme: Colors; val textStyles: TextStyles; ... }
```

## 4. 图片加载

沿用现有 Glide 链路：`com.bumptech.glide.Glide.with(...)`（可传 View 或 Context）。
Mojito 大图预览（`net.mikaelzero.mojito.Mojito`）继续用，不要引入 coil。
九宫格图、圆形头像等视觉效果在 Compose 里用 `Modifier.clip(shape)` + `AsyncImage` 不可用——
Glide 没有官方 Compose 集成，用 `androidx.compose.ui.viewinterop.AndroidView` 包 `ImageView` 或
在 Report 里标注「需要公共图片组件」，由 ui/common 代理统一提供。**不许引入新图片库。**

## 5. 业务对照

老代码在 `app/src/main/java/com/example/c001apk/ui/<组>/`，布局在 `app/src/main/res/layout/`。
迁移 = 读懂该组的 Fragment/Activity/Adapter/ViewModel + 对应 XML，把**界面与交互**用 Compose 表达，
数据流走原 ViewModel。文案、字符串先沿用硬编码中文/现有 string，不要求国际化。

## 6. 报告格式（每个子代理最后必须输出）

```
【产出】文件路径列表
【使用的 Miuix 组件】列表（注明哪些签名是从源码核对的）
【复用的 ViewModel/数据接口】
【未解决问题 / 需要主代理处理】（如：需要某依赖、需要接线点、API 疑问）
```

## 7. 本轮成功标准

- 新增的 Compose 代码在**下一次 CI 编译**中尽量少报错（报错由主代理统一修）；
- 页面交互闭环完整（空态/加载/错误态/点击跳转占位用回调参数表达）；
- 没有改到别人的文件、没有臆造 API。
