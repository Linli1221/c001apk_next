package com.example.c001apk.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.c001apk.R
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 点赞 / 评论 / 转发计数条（对应旧布局 item_feed_content.xml 底部的 like/reply 动作条）。
 *
 * **用途**：动态、评论、消息等条目底部的三连计数。计数文案由调用方格式化后传入
 * （如 `UserCardUtils.countText(...)`），本组件不关心数值类型。
 *
 * **参数**
 * @param likeCount 点赞数文案（如「12」「1.2k」）。
 * @param replyCount 评论数文案。
 * @param forwardCount 转发数文案。
 * @param modifier 应用于整条的 [Modifier]。
 * @param isLiked 是否已点赞：true 时点赞图标与文字用主题色 [MiuixTheme.colorScheme.primary]，
 *   否则用 [MiuixTheme.colorScheme.onSurfaceVariantSummary]（对应旧 `setLike` 的着色逻辑）。
 * @param onLikeClick 点赞点击回调；null 时该项不可点击。
 * @param onReplyClick 评论点击回调；null 时该项不可点击。
 * @param onForwardClick 转发点击回调；null 时该项不可点击。
 *
 * **祖先要求**：必须位于根主题 `MiuixAppTheme`（即 [MiuixTheme]）之内；无其他前置要求。
 * 图标沿用现有 drawable（`ic_like` / `ic_message` / `ic_forward`），按主题色 tint，
 * 不引入 Miuix 图标，保持与旧版视觉一致。
 */
@Composable
fun StatBar(
    likeCount: String,
    replyCount: String,
    forwardCount: String,
    modifier: Modifier = Modifier,
    isLiked: Boolean = false,
    onLikeClick: (() -> Unit)? = null,
    onReplyClick: (() -> Unit)? = null,
    onForwardClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatBarItem(
            iconRes = R.drawable.ic_like,
            count = likeCount,
            tint = if (isLiked) {
                MiuixTheme.colorScheme.primary
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantSummary
            },
            onClick = onLikeClick,
        )
        StatBarItem(
            iconRes = R.drawable.ic_message,
            count = replyCount,
            onClick = onReplyClick,
        )
        StatBarItem(
            iconRes = R.drawable.ic_forward,
            count = forwardCount,
            onClick = onForwardClick,
        )
    }
}

/**
 * 单个计数项（图标 + 数字），[StatBar] 的组成单元，也可单独用于「收藏」「浏览」等扩展计数。
 *
 * **参数**
 * @param iconRes 图标 drawable 资源（现有矢量图标，如 `R.drawable.ic_star`）。
 * @param count 计数文案。
 * @param modifier 应用于单项的 [Modifier]。
 * @param tint 图标与文字颜色，默认 [MiuixTheme.colorScheme.onSurfaceVariantSummary]。
 * @param onClick 点击回调；null 时不可点击。
 */
@Composable
fun StatBarItem(
    @DrawableRes iconRes: Int,
    count: String,
    modifier: Modifier = Modifier,
    tint: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = count,
            style = MiuixTheme.textStyles.footnote1,
            color = tint,
        )
    }
}
