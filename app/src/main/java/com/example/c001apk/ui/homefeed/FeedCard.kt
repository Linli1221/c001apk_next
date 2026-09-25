package com.example.c001apk.ui.homefeed

import android.text.Html
import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.c001apk.adapter.setGridView
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.util.DateUtils
import com.example.c001apk.util.ImageUtil
import com.example.c001apk.view.ninegridimageview.NineGridImageView
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 信息流动态卡片（迁移自 item_home_feed.xml + AppAdapter.FeedViewHolder）：
 * 头像 / 用户名 / 时间 / 正文 / 图片九宫格 / 点赞·评论·转发条。
 *
 * 图片沿用 Glide 链路：九宫格复用现有 [NineGridImageView]（含 Mojito 大图预览、GIF/长图角标），
 * 圆形头像用 AndroidView 包 ImageView + ImageUtil.showIMG + CircleShape 裁剪。
 * 正文/热评/转发源的 HTML 用 Html.fromHtml 去标签展示（原 LinkTextView 的链接点击
 * 待公共图片/富文本组件轮统一处理，见迁移报告）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedCard(
    data: HomeFeedResponse.Data,
    onFeedClick: () -> Unit,
    onUserClick: () -> Unit,
    onLikeClick: () -> Unit,
    onReplyClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    onForwardClick: () -> Unit = {},
    onHotReplyClick: () -> Unit = {},
    onForwardSourceClick: () -> Unit = {},
) {
    Card(
        onClick = onFeedClick,
        onLongPress = onLongClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {

            // ── 头部：头像 / 用户名 / 来源·设备·标记 / 更多 ──────────────────
            Row(verticalAlignment = Alignment.Top) {
                FeedAvatar(
                    url = data.userInfo?.userAvatar,
                    onClick = onUserClick,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.userInfo?.username ?: data.username.orEmpty(),
                        style = MiuixTheme.textStyles.subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val from = fromPlainText(data.infoHtml)
                        if (from.isNotEmpty()) {
                            Text(
                                text = from,
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (!data.deviceTitle.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = data.deviceTitle,
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (data.isStickTop == 1) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "置顶",
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        if (data.publishStatus == 1) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "仅自己可见",
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = MiuixIcons.ExpandMore,
                        contentDescription = "更多",
                    )
                }
            }

            // ── 标题 / 正文 ────────────────────────────────────────────────
            if (!data.messageTitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = data.messageTitle,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 21.sp,
                )
            }
            if (!data.message.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = fromPlainText(data.message),
                    style = MiuixTheme.textStyles.body1,
                    lineHeight = 21.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // ── 图片九宫格 ────────────────────────────────────────────────
            if (!data.picArr.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FeedImageGrid(
                    pic = data.pic,
                    picArr = data.picArr,
                    feedType = data.feedType,
                )
            }

            // ── 热评 ──────────────────────────────────────────────────────
            val hotReply = data.replyRows?.firstOrNull()
            if (hotReply != null && !hotReply.message.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    cornerRadius = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = onHotReplyClick,
                            onLongClick = onLongClick,
                        ),
                ) {
                    Text(
                        text = fromPlainText(hotReply.message),
                        style = MiuixTheme.textStyles.body2,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            // ── 转发源 ────────────────────────────────────────────────────
            val forward = data.forwardSourceFeed
            if (forward != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    cornerRadius = 12.dp,
                    onClick = onForwardSourceClick,
                    onLongPress = onLongClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        val forwardText = buildString {
                            append("@")
                            append(forward.username.orEmpty())
                            append(": ")
                            append(fromPlainText(forward.message))
                        }
                        if (forward.message.isNotEmpty()) {
                            Text(
                                text = forwardText,
                                style = MiuixTheme.textStyles.body2,
                                lineHeight = 18.sp,
                            )
                        }
                        if (!forward.picArr.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            FeedImageGrid(
                                pic = forward.pic,
                                picArr = forward.picArr,
                                feedType = forward.feedType,
                            )
                        }
                    }
                }
            }

            // ── 底部条：时间 / 评论 / 转发 / 点赞 ───────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = DateUtils.fromToday(data.dateline ?: 0L),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(modifier = Modifier.weight(1f))
                FeedActionText(
                    text = "评论 " + (data.replynum ?: "0"),
                    onClick = onReplyClick,
                )
                FeedActionText(
                    text = "转发 " + (data.forwardnum ?: "0"),
                    onClick = onForwardClick,
                )
                FeedActionText(
                    text = "赞 " + (data.likenum ?: "0"),
                    onClick = onLikeClick,
                    isHighlighted = data.userAction?.like == 1,
                )
            }
        }
    }
}

/** 点赞/评论/转发条的小文字按钮（Miuix 无点赞语义图标，避免臆造，统一文字表达） */
@Composable
private fun FeedActionText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote1,
        color = if (isHighlighted) MiuixTheme.colorScheme.primary
        else MiuixTheme.colorScheme.onSurfaceVariantActions,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** 圆形头像：AndroidView 包 ImageView，沿用 Glide（ImageUtil.showIMG）+ CircleShape 裁剪 */
@Composable
private fun FeedAvatar(
    url: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context -> ImageView(context) },
        update = { view ->
            if (url.isNullOrEmpty()) view.setImageDrawable(null)
            else ImageUtil.showIMG(view, url)
        },
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    )
}

/**
 * 图片九宫格：复用现有 NineGridImageView + 数据绑定同款 setGridView 逻辑
 * （含 .s.jpg 缩略图、GIF/长图角标、点击进 Mojito 大图预览）
 */
@Composable
private fun FeedImageGrid(
    pic: String?,
    picArr: List<String>?,
    feedType: String?,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context -> NineGridImageView(context) },
        update = { view -> setGridView(view, pic, picArr, feedType) },
        modifier = modifier.fillMaxWidth(),
    )
}

/** HTML 片段转纯文本（正文/热评/来源串里的 <a> 标签只留文字） */
private fun fromPlainText(html: String?): String {
    if (html.isNullOrEmpty()) return ""
    return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
}
