package com.example.c001apk.ui.others

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.c001apk.BuildConfig
import com.example.c001apk.R
import com.example.c001apk.util.UpdateChecker
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 「关于本应用」页面（Compose + Miuix 版），替代原 drakeet about-page 观感
 * （原实现：[AboutActivity] + [AboutUpdateItems]，本文件不改动老代码）。
 *
 * 页面结构：
 *  1. 头部：logo / 应用名 / 版本号（`VERSION_NAME(VERSION_CODE)`，与老版一致）；
 *  2. 关于卡片（fake coolapk）；
 *  3. 开发者（三位贡献者，点击跳 GitHub）；
 *  4. 更新：正式版 / Beta 两个开关 + 两个「立即检查」入口；
 *  5. 反馈：云端下发的群组链接（[UpdateChecker.fetchOrgLinks]，拉不到就不显示）+ GitHub 卡片；
 *  6. 开放源代码许可证列表。
 *
 * 状态与业务全部提升为参数，页面内不 new 业务逻辑：
 *  - 更新开关的两个状态由调用方持有（老代码写在 [com.example.c001apk.util.PrefManager]，
 *    接线时用 `remember { mutableStateOf(PrefManager.isCheckUpdateStable) }` 持有并写回即可；
 *    注意 UpdateChecker.showUpdateDialog 的「不再提示」也会改写 PrefManager，
 *    老代码靠 adapter.notifyDataSetChanged 刷新，接线方在弹窗回调里刷新这两个状态即可）；
 *  - [onCheckUpdateNow] 对应老 `AboutActivity.checkUpdateNow(channel)` 的完整流程
 *    （fetchUpdate + Toast/更新弹窗），请由调用方接 [UpdateChecker]；
 *  - [onOpenUrl] 统一外链出口（建议接 [com.example.c001apk.util.NetWorkUtil.openLink]，
 *    与全站外链策略保持一致）；
 *  - [orgLinks] 由调用方用 [UpdateChecker.fetchOrgLinks] 拉取后传入，空列表 = 不显示该组。
 */
@Composable
fun AboutScreen(
    checkUpdateStable: Boolean,
    onCheckUpdateStableChange: (Boolean) -> Unit,
    checkUpdateBeta: Boolean,
    onCheckUpdateBetaChange: (Boolean) -> Unit,
    onCheckUpdateNow: (channel: String) -> Unit,
    onOpenUrl: (url: String) -> Unit,
    modifier: Modifier = Modifier,
    orgLinks: List<UpdateChecker.OrgLink> = emptyList(),
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val appName = remember(context) {
        context.applicationInfo.loadLabel(context.packageManager).toString()
    }
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.about),
                largeTitle = stringResource(R.string.about),
                scrollBehavior = scrollBehavior,
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 24.dp,
            ),
        ) {
            // ---------- 头部：logo / 应用名 / 版本 ----------
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher),
                        contentDescription = appName,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(18.dp)),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = appName,
                        style = MiuixTheme.textStyles.title3,
                        color = MiuixTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            // ---------- 关于 ----------
            item { SmallTitle(text = stringResource(R.string.about)) }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(16.dp),
                ) {
                    Text(
                        text = "fake coolapk",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onBackground,
                    )
                }
            }

            // ---------- 开发者 ----------
            item { SmallTitle(text = stringResource(R.string.about_developer)) }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                ) {
                    aboutContributors.forEachIndexed { index, contributor ->
                        if (index > 0) HorizontalDivider()
                        BasicComponent(
                            title = contributor.name,
                            summary = contributor.role,
                            startAction = {
                                Image(
                                    painter = painterResource(contributor.avatar),
                                    contentDescription = contributor.name,
                                    modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                )
                            },
                            onClick = { onOpenUrl(contributor.url) },
                        )
                    }
                }
            }

            // ---------- 更新（开关 + 立即检查入口，位置与老版一致：开发者之后、反馈之前） ----------
            item { SmallTitle(text = "更新") }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                ) {
                    SwitchPreference(
                        title = stringResource(R.string.check_stable_update),
                        checked = checkUpdateStable,
                        onCheckedChange = onCheckUpdateStableChange,
                    )
                    HorizontalDivider()
                    SwitchPreference(
                        title = stringResource(R.string.check_beta_update),
                        checked = checkUpdateBeta,
                        onCheckedChange = onCheckUpdateBetaChange,
                    )
                    HorizontalDivider()
                    ArrowPreference(
                        title = stringResource(R.string.check_update_now_stable),
                        onClick = { onCheckUpdateNow(UpdateChecker.CHANNEL_STABLE) },
                    )
                    HorizontalDivider()
                    ArrowPreference(
                        title = stringResource(R.string.check_update_now_beta),
                        onClick = { onCheckUpdateNow(UpdateChecker.CHANNEL_BETA) },
                    )
                }
            }

            // ---------- 反馈 ----------
            item { SmallTitle(text = stringResource(R.string.feedback)) }
            if (orgLinks.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    ) {
                        orgLinks.forEachIndexed { index, link ->
                            if (index > 0) HorizontalDivider()
                            ArrowPreference(
                                title = link.name,
                                onClick = { onOpenUrl(link.url) },
                            )
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    pressFeedbackType = top.yukonga.miuix.kmp.utils.PressFeedbackType.Sink,
                    showIndication = true,
                    onClick = { onOpenUrl("https://github.com/kongwufang/c001apk_next") },
                ) {
                    Text(
                        text = "GitHub\nhttps://github.com/kongwufang/c001apk_next",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onBackground,
                    )
                }
            }

            // ---------- 开放源代码许可证 ----------
            item { SmallTitle(text = stringResource(R.string.about_open_source)) }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                ) {
                    aboutLicenses.forEachIndexed { index, license ->
                        if (index > 0) HorizontalDivider()
                        BasicComponent(
                            title = license.name,
                            summary = license.author,
                            endActions = {
                                Text(
                                    text = license.licenseLabel,
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            },
                            onClick = { onOpenUrl(license.url) },
                        )
                    }
                }
            }
        }
    }
}

/** 贡献者条目（对应老 [AboutActivity] 里的 drakeet Contributor） */
private data class AboutContributor(
    val avatar: Int,
    val name: String,
    val role: String,
    val url: String,
)

/** 开源库条目（对应老 [AboutActivity] 里的 drakeet License） */
private data class AboutLicense(
    val name: String,
    val author: String,
    val licenseLabel: String,
    val url: String,
)

private val aboutContributors = listOf(
    AboutContributor(
        R.drawable.cont_author,
        "bggRGjQaUbCoE",
        "Developer & Designer",
        "https://github.com/bggRGjQaUbCoE",
    ),
    AboutContributor(
        R.drawable.cont_klxiaoniu,
        "klxiaoniu",
        "Developer & Collaborator",
        "https://github.com/klxiaoniu",
    ),
    AboutContributor(
        R.drawable.cont_kongwufang,
        "kongwufang",
        "Developer & Maintainer",
        "https://github.com/kongwufang",
    ),
)

private const val LICENSE_APACHE_2 = "Apache License 2.0"
private const val LICENSE_MIT = "MIT"
private const val LICENSE_GPL_V3 = "GPL v3"

private val aboutLicenses = listOf(
    AboutLicense("kotlin", "JetBrains", LICENSE_APACHE_2, "https://github.com/JetBrains/kotlin"),
    AboutLicense("AndroidX", "Google", LICENSE_APACHE_2, "https://source.google.com"),
    AboutLicense(
        "material-components-android",
        "Google",
        LICENSE_APACHE_2,
        "https://github.com/material-components/material-components-android",
    ),
    AboutLicense("RikkaX", "RikkaApps", LICENSE_MIT, "https://github.com/RikkaApps/RikkaX"),
    AboutLicense("about-page", "drakeet", LICENSE_APACHE_2, "https://github.com/drakeet/about-page"),
    AboutLicense("LSPosed", "LSPosed", LICENSE_GPL_V3, "https://github.com/LSPosed/LSPosed"),
    AboutLicense("LibChecker", "LibChecker", LICENSE_APACHE_2, "https://github.com/LibChecker/LibChecker"),
    AboutLicense("Hide-My-Applist", "Dr-TSNG", LICENSE_GPL_V3, "https://github.com/Dr-TSNG/Hide-My-Applist"),
    AboutLicense("okhttp", "square", LICENSE_APACHE_2, "https://github.com/square/okhttp"),
    AboutLicense("retrofit", "square", LICENSE_APACHE_2, "https://github.com/square/retrofit"),
    AboutLicense("glide", "bumptech", LICENSE_APACHE_2, "https://github.com/bumptech/glide"),
    AboutLicense("jBCrypt", "jeremyh", LICENSE_APACHE_2, "https://github.com/jeremyh/jBCrypt"),
    AboutLicense("flexbox-layout", "google", LICENSE_APACHE_2, "https://github.com/google/flexbox-layout"),
    AboutLicense("glide-transformations", "wasabeef", LICENSE_APACHE_2, "https://github.com/wasabeef/glide-transformations"),
    AboutLicense("jsoup", "jhy", LICENSE_MIT, "https://github.com/jhy/jsoup"),
    AboutLicense("NineGridImageView", "plain-dev", LICENSE_MIT, "https://github.com/plain-dev/NineGridImageView"),
    AboutLicense("mojito", "mikaelzero", LICENSE_APACHE_2, "https://github.com/mikaelzero/mojito"),
    AboutLicense("CircleIndicator", "ongakuer", LICENSE_APACHE_2, "https://github.com/ongakuer/CircleIndicator"),
    AboutLicense("libraries", "zhaobozhen", LICENSE_MIT, "https://github.com/zhaobozhen/libraries"),
    AboutLicense("dagger", "google", LICENSE_APACHE_2, "https://github.com/google/dagger"),
    AboutLicense("SmoothInputLayout", "AlexMofer", LICENSE_APACHE_2, "https://github.com/AlexMofer/SmoothInputLayout"),
)
