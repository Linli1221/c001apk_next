package com.example.c001apk.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.c001apk.R
import com.example.c001apk.logic.model.FeedArticleContentBean
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.util.DateUtils
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 动态详情「正文块」：对应 item_feed_content.xml。
 * 头像 / 昵称 / 关注 / 时间 / 设备 / 私密角标 / 标题 / 正文（富文本）/ 图片 / 外链卡片 /
 * 转发卡片 / IP 属地 / 评分 / 评论数 / 点赞数。
 */
@Composable
fun FeedContentCard(
    data: HomeFeedResponse.Data,
    likeNum: String,
    isLike: Int,
    followAuthor: Int,
    modifier: Modifier = Modifier,
    showFollow: Boolean = true,
    onOpenUser: (uid: String?) -> Unit = {},
    onFollow: (uid: String, followAuthor: Int) -> Unit = { _, _ -> },
    onLikeFeed: (id: String, isLike: Int) -> Unit = { _, _ -> },
    onPreviewImages: (urls: List<String>, index: Int) -> Unit = { _, _ -> },
    onOpenLink: (url: String?, title: String?) -> Unit = { _, _ -> },
    onViewForwardFeed: (id: String?) -> Unit = {},
    onLongCopyText: (text: String?) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { onLongCopyText(data.message) },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // 头像 + 昵称 + 关注 + 时间 / 设备 / 私密角标
        Row(verticalAlignment = Alignment.Top) {
            FeedAvatar(
                url = data.userInfo?.userAvatar ?: data.userAvatar,
                size = 36,
                onClick = { onOpenUser(data.uid) },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = data.userInfo?.username.orEmpty(),
                        style = MiuixTheme.textStyles.body1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (showFollow && followAuthor in 0..1) {
                        TextButton(
                            text = if (followAuthor == 1) "取消关注" else "关注",
                            onClick = {
                                data.uid?.let { onFollow(it, followAuthor) }
                            },
                            minHeight = 28.dp,
                            insideMargin = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 8.dp,
                                vertical = 0.dp,
                            ),
                            colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColors(
                                textColor = if (followAuthor == 1)
                                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                                else
                                    MiuixTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = data.dateline?.let { DateUtils.fromToday(it) }.orEmpty(),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                    )
                    val device = data.deviceTitle
                    if (!device.isNullOrEmpty()) {
                        Text(
                            text = device,
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(start = 5.dp)
                                .weight(1f, fill = false),
                        )
                    }
                    if (data.publishStatus == 1) {
                        Text(
                            text = stringResource(R.string.publish_status_private),
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier
                                .padding(start = 5.dp)
                                .background(
                                    MiuixTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }

        // 标题（部分动态带）
        val messageTitle = data.messageTitle
        if (!messageTitle.isNullOrEmpty()) {
            Text(
                text = messageTitle,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        // 正文（富文本，长按复制）
        val message = data.message
        if (!message.isNullOrEmpty()) {
            FeedHtmlText(
                text = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                onLongClick = { onLongCopyText(message) },
            )
        }

        // 图片九宫格
        FeedImageGrid(
            picArr = data.picArr,
            pic = data.pic,
            modifier = Modifier.padding(top = 10.dp),
            onPreview = onPreviewImages,
        )

        // 外链卡片
        FeedExtraLinkCard(data = data, onClick = onOpenLink)

        // 转发内容卡片
        FeedForwardedCard(
            data = data,
            onPreviewImages = onPreviewImages,
            onViewFeed = onViewForwardFeed,
            onLongCopyText = onLongCopyText,
        )

        // 评分（点评类动态）
        FeedRatingBlock(data = data)

        // IP 属地
        val ip = data.ipLocation
        if (!ip.isNullOrEmpty()) {
            Text(
                text = "发布于 $ip",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        // 评论数 / 点赞数（右下角）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val replynum = data.replynum
            if (!replynum.isNullOrEmpty()) {
                Text(
                    text = replynum,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(end = 10.dp),
                )
            }
            Text(
                text = likeNum,
                style = MiuixTheme.textStyles.footnote2,
                color = if (isLike == 1) MiuixTheme.colorScheme.primary
                else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.combinedClickable(
                    onClick = {
                        data.id?.let { onLikeFeed(it, isLike) }
                    },
                ),
            )
        }
    }
}

/** 外链卡片（extraUrl / extraTitle / extraPic） */
@Composable
private fun FeedExtraLinkCard(
    data: HomeFeedResponse.Data,
    onClick: (url: String?, title: String?) -> Unit,
) {
    val extraUrl = data.extraUrl
    if (extraUrl.isNullOrEmpty() || extraUrl.startsWith("/goods/")) return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        onClick = { onClick(extraUrl, data.extraTitle) },
        cornerRadius = 12.dp,
        insideMargin = androidx.compose.foundation.layout.PaddingValues(10.dp),
        colors = top.yukonga.miuix.kmp.basic.CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlideImage(
                url = data.extraPic,
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp),
                cornerRadius = 8,
            )
            Column(modifier = Modifier.padding(start = 10.dp)) {
                val extraTitle = data.extraTitle
                if (!extraTitle.isNullOrEmpty()) {
                    Text(
                        text = extraTitle,
                        style = MiuixTheme.textStyles.body2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = extraUrl,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 转发内容卡片（forwardSourceFeed） */
@Composable
private fun FeedForwardedCard(
    data: HomeFeedResponse.Data,
    onPreviewImages: (urls: List<String>, index: Int) -> Unit,
    onViewFeed: (id: String?) -> Unit,
    onLongCopyText: (text: String?) -> Unit,
) {
    val forward = data.forwardSourceFeed ?: return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        onClick = { onViewFeed(forward.id) },
        onLongPress = { onLongCopyText(forward.message) },
        cornerRadius = 12.dp,
        insideMargin = androidx.compose.foundation.layout.PaddingValues(10.dp),
        colors = top.yukonga.miuix.kmp.basic.CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceVariant,
        ),
    ) {
        val forwardMessage = forward.message
        if (!forwardMessage.isNullOrEmpty()) {
            FeedHtmlText(
                text = """<a class="feed-link-uname" href="/u/${forward.uid}">@${forward.username.orEmpty()}</a>: $forwardMessage""",
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.fillMaxWidth(),
                onLongClick = { onLongCopyText(forwardMessage) },
            )
        }
        FeedImageGrid(
            picArr = forward.picArr,
            pic = data.pic,
            modifier = Modifier.padding(top = 10.dp),
            onPreview = onPreviewImages,
        )
    }
}

/**
 * 评分块（点评类动态：star / ratingItemInfo / commentGood·General·Bad / 设备信息）。
 * 老 View 版本里评分在 nodeRating 实体渲染，详情页这里是纯展示。
 */
@Composable
private fun FeedRatingBlock(data: HomeFeedResponse.Data) {
    val star = data.star
    val ratingItems = data.ratingItemInfo
    val hasRating = star != null || !ratingItems.isNullOrEmpty() ||
        !data.commentGood.isNullOrEmpty() || !data.commentGeneral.isNullOrEmpty() ||
        !data.commentBad.isNullOrEmpty()
    if (!hasRating) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        cornerRadius = 12.dp,
        insideMargin = androidx.compose.foundation.layout.PaddingValues(10.dp),
        colors = top.yukonga.miuix.kmp.basic.CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceVariant,
        ),
    ) {
        if (star != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "我的评分",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "★".repeat(star.coerceIn(0, 5)) + "☆".repeat((5 - star).coerceIn(0, 5)),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$star/5",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary,
                )
            }
        }
        ratingItems?.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text(
                    text = item.name.orEmpty(),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = item.averageScore.orEmpty(),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary,
                )
            }
        }
        RatingCommentLine(label = "优点", text = data.commentGood)
        RatingCommentLine(label = "一般", text = data.commentGeneral)
        RatingCommentLine(label = "缺点", text = data.commentBad)
    }
}

@Composable
private fun RatingCommentLine(label: String, text: String?) {
    if (text.isNullOrEmpty()) return
    Text(
        text = "$label：$text",
        style = MiuixTheme.textStyles.body2,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    )
}

/** 长文（feedArticle / trade）文本块，对应 item_feed_article_text.xml */
@Composable
fun FeedArticleTextBlock(
    data: FeedArticleContentBean.Data,
    bold: Boolean,
    onLongCopyText: (text: String?) -> Unit = {},
) {
    val message = data.message.orEmpty()
    FeedHtmlText(
        text = message,
        style = if (bold) MiuixTheme.textStyles.title3 else MiuixTheme.textStyles.main,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        onLongClick = { onLongCopyText(message) },
    )
}

/** 长文图片块，对应 item_feed_article_image.xml */
@Composable
fun FeedArticleImageBlock(
    data: FeedArticleContentBean.Data,
    onPreviewImages: (urls: List<String>, index: Int) -> Unit = { _, _ -> },
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)) {
        GlideImage(
            url = data.url,
            modifier = Modifier.fillMaxWidth(),
            onClick = { data.url?.let { onPreviewImages(listOf(it), 0) } },
        )
        val description = data.description
        if (!description.isNullOrEmpty()) {
            Text(
                text = description,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

/** 长文外链块，对应 item_feed_article_share_url.xml */
@Composable
fun FeedArticleShareUrlBlock(
    data: FeedArticleContentBean.Data,
    onOpenLink: (url: String?, title: String?) -> Unit = { _, _ -> },
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        onClick = { onOpenLink(data.url, data.title) },
        cornerRadius = 12.dp,
        insideMargin = androidx.compose.foundation.layout.PaddingValues(10.dp),
        colors = top.yukonga.miuix.kmp.basic.CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Text(
            text = data.title.orEmpty(),
            style = MiuixTheme.textStyles.body2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
