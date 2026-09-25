package com.example.c001apk.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 底部操作条（对应老页面的回复 FAB + 正文卡右下角的评论/点赞聚合）：
 * 「写评论…」输入条占位（点击去回复页）+ 评论数 + 点赞 + 收藏 + 分享。
 */
@Composable
fun FeedBottomBar(
    likeNum: String,
    isLike: Int,
    replyNum: String,
    favNum: String,
    showReplyInput: Boolean,
    modifier: Modifier = Modifier,
    onReplyFeed: () -> Unit = {},
    onLikeFeed: () -> Unit = {},
    onOpenCollection: () -> Unit = {},
    onShare: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showReplyInput) {
                // 「写评论…」占位输入条，点击进入回复页（老版为悬浮回复按钮）
                Text(
                    text = "写评论…",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .background(
                            MiuixTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(18.dp),
                        )
                        .clickable(onClick = onReplyFeed)
                        .padding(horizontal = 14.dp),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            BarAction(
                label = replyNum,
                icon = { tint ->
                    Icon(
                        MiuixIcons.Messages,
                        contentDescription = "评论",
                        modifier = Modifier.size(18.dp),
                        tint = tint,
                    )
                },
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                onClick = onReplyFeed,
            )
            BarAction(
                label = likeNum,
                icon = null,
                tint = if (isLike == 1) MiuixTheme.colorScheme.primary
                else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                onClick = onLikeFeed,
            )
            BarAction(
                label = favNum,
                icon = { tint ->
                    Icon(
                        MiuixIcons.Favorites,
                        contentDescription = "收藏",
                        modifier = Modifier.size(18.dp),
                        tint = tint,
                    )
                },
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                onClick = onOpenCollection,
            )
            BarAction(
                label = "",
                icon = { tint ->
                    Icon(
                        MiuixIcons.Share,
                        contentDescription = "分享",
                        modifier = Modifier.size(18.dp),
                        tint = tint,
                    )
                },
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                onClick = onShare,
            )
        }
    }
}

@Composable
private fun BarAction(
    label: String,
    icon: (@Composable (androidx.compose.ui.graphics.Color) -> Unit)?,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
    ) {
        icon?.invoke(tint)
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MiuixTheme.textStyles.footnote1,
                color = tint,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
