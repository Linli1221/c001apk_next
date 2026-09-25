# Miuix 迁移接线清单（主代理维护，跨轮累积）

> 页面 Screen 写好后，把它们接进 Activity/Fragment（setContent + MiuixAppTheme）的工作记录在这里。
> 各迁移子代理报告里「需要主代理处理」的事项也归档到此。

## 通用接线模式

```kotlin
// Activity 内（后续统一）
setContent {
    MiuixAppTheme(/* colorSchemeMode 由 PrefManager.darkTheme 映射 */) {
        XxxScreen(viewModel = vm, onBack = { finish() }, ...)
    }
}
```

- `MiuixAppTheme` 与 `PrefManager.darkTheme`（AppCompatDelegate.MODE_NIGHT_*）打通：跟随系统→System、浅色→Light、深色→Dark。
- ViewModel 创建沿用现有方式（Hilt assisted 的用 `viewModels(extrasProducer + withCreationCallback)`）。
- LiveData 桥接：`androidx.lifecycle.compose.observeAsState`（lifecycle-livedata-compose:2.11.0 已接入）。

## 逐页接线待办

### blacklist（已交付 BlackListScreen.kt）
- [ ] BlackListActivity → setContent { MiuixAppTheme { BlackListScreen(...) } }；VM 为 assisted factory（type="user"）
- [ ] onUserClick(uid) → UserActivity；onBack → finish；onToast → makeToast
- [ ] 未迁移功能：uid 添加输入框、备份/恢复（SAF）、清空全部（后续用 TopAppBar actions + OverlayDialog 补）

### login（已交付 LoginScreen.kt）
- [ ] **事实修正**：账号/密码/短信登录不存在（ApiService 注释：表单接口已随表单登录移除），唯一入口 WebLoginActivity（WebView 抓 cookie）；无 LoginViewModel/LoginUtil。Screen 形态 = 网页登录入口 + 手动 Token 表单 + 设备标识提示
- [ ] onSubmitTokenLogin 落地：参考 WebLoginActivity.saveLogin（PrefManager.isLogin/uid/token、username URL 编码一次规范化、ActivityCollector.recreateActivity(MainActivity)）；该逻辑目前是 WebLoginActivity 私有方法，接线时抽公共或复制并注明
- [ ] 用户协议/隐私政策无现成页面：onOpen* 用 WebViewActivity 打开酷安协议页或空实现
- [ ] 复核 token：`colorScheme.onBackgroundVariant` 是否真实存在于 Colors.kt（login 代理标注了「?」）

### 其余页面
（随子代理报告陆续追加）

## 已核验 API（抽查记录）

**批量 token 审计（全量 Screen 代码）**：colorScheme 用到 25 个 token（background/dividerLine/error/onBackground/onError/onPrimary/onSecondary/onSecondaryContainer/onSecondaryVariant/onSurface/onSurfaceSecondary/onSurfaceVariantActions/onSurfaceVariantSummary/onTertiaryContainer/outline/primary/secondary/secondaryContainer/secondaryVariant/surface/surfaceContainer/surfaceContainerHigh/surfaceVariant/tertiaryContainer/windowDimming）、textStyles 用到 11 个（body1/body2/button/footnote1/footnote2/main/paragraph/subtitle/title2/title3/title4），**逐一比对 Colors.kt/TextStyles.kt（39c40f99）全部真实，零臆造**。
- SmallTopAppBar/TopAppBar/Scaffold（含 floatingToolbar/popupHost/snackbarHost 槽位）— v0.9.4 源码逐签名核对 ✓
- MiuixScrollBehavior() 工厂 + ScrollBehavior.nestedScrollConnection — TopAppBar.kt L250/L422 ✓
- MiuixIcons.Back/Close — miuix-icons extended 包 ✓
- OverlayDialog(title, summary, show, onDismissRequest) — overlay/OverlayDialog.kt L47 ✓
- ThemeController/MiuixTheme/ColorSchemeMode — theme/ ✓

## 环境备忘
- GitHub Actions：Linli1221/c001apk_next 是 fork，Actions 需网页启用一次后 workflow 才会跑（ci.yml 已注册）
- 本地编译诊断工具链：C:\code\coolapk\.toolchain（JDK17 + Android SDK 37），`./gradlew :app:assembleDebug` 可本地查编译错；正式构建仍走 CI
