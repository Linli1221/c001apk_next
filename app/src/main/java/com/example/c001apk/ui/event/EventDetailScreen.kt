package com.example.c001apk.ui.event

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.observeAsState
import com.example.c001apk.logic.model.EventDetailData
import com.example.c001apk.ui.common.ErrorState
import com.example.c001apk.ui.common.LoadingState as LoadingPlaceholder
import com.example.c001apk.ui.common.SimpleImage
import com.example.c001apk.ui.common.TopBarScaffold
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 众测/活动详情页（EventDetailActivity + activity_event_detail.xml 的 Compose 形态）。
 *
 * 数据走现有 [EventDetailViewModel]（LiveData 用 observeAsState 桥接，不重写 VM）：
 * `detail` / `loading` / `toastText`。进入页面自动 `viewModel.load(id)`（对应旧 onCreate 里的加载），
 * 失败时展示错误态并可重试；「立即报名」打开 `actionUrl`（跳转方式由宿主 [onOpenUrl] 决定）。
 *
 * **参数**
 * @param id 活动 id（旧 Intent extra `"id"`），为空时提示「缺少活动 id」。
 * @param viewModel 复用的 [EventDetailViewModel]（Hilt 注入后传入）。
 * @param modifier 应用于页面的 [Modifier]。
 * @param onBack 返回回调（顶栏返回箭头）。
 * @param onOpenUrl 点击「立即报名」的落地 url（如 startActivity ACTION_VIEW），不传则页面内直接 Toast 占位。
 *
 * **祖先要求**：必须位于根主题 `MiuixAppTheme` 之内；本组件自带 TopBarScaffold（Miuix Scaffold），
 * 其 content 内可直接使用 Overlay* 弹窗。
 */
@Composable
fun EventDetailScreen(
    id: String,
    viewModel: EventDetailViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
) {
    val detail by viewModel.detail.observeAsState()
    val loading = viewModel.loading.observeAsState().value
    val toast = viewModel.toastText.observeAsState().value
    val context = LocalContext.current

    // 对应旧 onCreate：有 id 就加载，没有就提示
    LaunchedEffect(id) {
        if (id.isNotEmpty()) {
            viewModel.load(id)
        } else {
            Toast.makeText(context, "缺少活动 id", Toast.LENGTH_SHORT).show()
        }
    }

    // toastText 一次性事件 → Toast
    LaunchedEffect(toast) {
        toast?.getContentIfNotHandledOrReturnNull()?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    TopBarScaffold(
        title = "众测详情",
        onBack = onBack,
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            val data = detail
            when {
                data != null -> EventDetailContent(
                    data = data,
                    onOpenUrl = { url ->
                        if (url.isNotEmpty()) onOpenUrl(url)
                        else Toast.makeText(context, "暂无报名入口", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // 加载失败（loading 已收敛为 false 且没有数据）→ 错误态 + 重试
                loading == false -> ErrorState(
                    onRetry = { if (id.isNotEmpty()) viewModel.load(id) },
                    modifier = Modifier.fillMaxSize(),
                    message = "加载失败，请重试",
                    retryText = "重试",
                )

                // loading 为 null（尚未开始）/ true：加载中
                else -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/** 详情正文：banner + 标题 + 赞助方/报名人数/报名时间 + 规则 + 奖品 + 报名按钮（对应旧 XML 的 LinearLayout）。 */
@Composable
private fun EventDetailContent(
    data: EventDetailData,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        val logo = data.logo
        if (!logo.isNullOrEmpty()) {
            SimpleImage(
                url = logo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentDescription = "活动 banner",
            )
        }

        val title = data.title
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            )
        }

        val sponsor = data.sponsorUser ?: data.sponsorUserList?.firstOrNull()?.username
        if (!sponsor.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "赞助方：$sponsor",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        val regNum = data.showRegNum ?: data.regNum
        if (!regNum.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "报名人数：$regNum",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        val start = data.timeRegStart
        val end = data.timeRegEnd
        if (start != null && end != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "报名时间：${fmtDate(start)} ~ ${fmtDate(end)}",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        val rule = data.noticeRule
        if (!rule.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = rule,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        val prizes = data.sponsorPrizeList.orEmpty()
            .mapNotNull { it.title }
            .joinToString("、")
        if (prizes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "奖品：$prizes",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        val applyUrl = data.actionUrl
        if (!applyUrl.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onOpenUrl(applyUrl) },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text("立即报名")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** 旧 EventDetailActivity.fmt()：秒级时间戳 → yyyy-MM-dd（本地时区）。 */
private fun fmtDate(ts: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(ts * 1000L))
