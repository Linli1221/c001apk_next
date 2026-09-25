package com.example.c001apk.ui.feed.reply

import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.logic.model.TotalReplyResponse
import com.example.c001apk.util.DateUtils
import com.example.c001apk.util.ImageUtil
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/*
 * 回复体系共用小组件（Compose）：
 * - [GlideImage] / [GlideUriImage]：沿用现有 Glide 链路（ImageUtil.showIMG / Glide.with），
 *   用 AndroidView 包 ImageView，不引入新图片库。
 * - [ReplyListItem]：回复条目（item_reply_to_reply_item.xml / item_feed_content_reply_item.xml 的 Compose 版），
 *   ReplyScreen 回复列表与 Reply2ReplySheet 共用。
 * - [ReplyListFooter]：列表底部加载态（FooterState 桥接）。
 */

/** 网络图片（圆形头像等）：Glide + AndroidView 包 ImageView，裁剪交给外层 Modifier.clip */
@Composable
fun GlideImage(
    url: String?,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            // url 变化才重新走 Glide，避免每次重组都发起加载
            if (imageView.tag != url) {
                imageView.tag = url
                ImageUtil.showIMG(imageView, url)
            }
        },
        modifier = modifier,
    )
}

/** 本地图片附件缩略图（选图后展示）：Glide + AndroidView */
@Composable
fun GlideUriImage(
    uri: Uri,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            if (imageView.tag != uri.toString()) {
                imageView.tag = uri.toString()
                Glide.with(imageView).load(uri).into(imageView)
            }
        },
        modifier = modifier,
    )
}

/**
 * 去掉 generateName() 生成的 <a> 标签（Reply2ReplyBottomSheetViewModel 会把用户名拼成 HTML），
 * 保留标签内文字（含「[楼主]」「[层主]」「回复」等信息）。富文本内联渲染等公共组件收敛后再做。
 */
fun stripReplyHtml(html: String): String = html.replace(Regex("<[^>]*>"), "")

/**
 * 回复条目行：头像 + 用户名（含 楼主/层主/回复 目标）+ 正文 + 时间/点赞/更多。
 * 对应 item_reply_to_reply_item.xml 的纵向列表形态。
 */
@Composable
fun ReplyListItem(
    reply: TotalReplyResponse.Data,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
) {
    val isLiked = (reply.userAction?.like ?: 0) == 1
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        GlideImage(
            url = reply.userAvatar,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        ) {
            val name = stripReplyHtml(reply.username).trim()
                .ifBlank { reply.userInfo.username }
            Text(
                text = name,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = reply.message,
                style = MiuixTheme.textStyles.main,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = DateUtils.fromToday(reply.dateline),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    text = if (isLiked) "已赞 ${reply.likenum}" else "赞 ${reply.likenum}",
                    onClick = onLikeClick,
                    colors = if (isLiked) ButtonDefaults.textButtonColors(
                        textColor = MiuixTheme.colorScheme.primary,
                    ) else ButtonDefaults.textButtonColors(),
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    text = "更多",
                    onClick = onMoreClick,
                    colors = ButtonDefaults.textButtonColors(
                        textColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    ),
                )
            }
        }
    }
}

/** 列表底部加载态（FooterState → Compose）：加载中 / 加载完 / 没有更多 / 出错重试 */
@Composable
fun ReplyListFooter(
    footerState: FooterState?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (footerState) {
        is FooterState.Loading -> Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        is FooterState.LoadingEnd -> Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = footerState.msg,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        is FooterState.LoadingError -> Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = footerState.errMsg,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(text = "重试", onClick = onRetry)
        }

        FooterState.LoadingReply -> {}
        FooterState.LoadingDone, null -> {}
    }
}
