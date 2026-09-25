package com.example.c001apk.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.c001apk.logic.model.TotalReplyResponse
import com.example.c001apk.util.DateUtils
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 评论条目（含子回复预览），对应 item_feed_content_reply_item.xml + item_feed_content_reply_to_reply_item.xml。
 *
 * - 点条目本体 = 回复该评论；点右上角展开箭头 = 操作菜单（复制/举报/删除/查看对话）
 * - 子回复卡：每条可点（回复该子回复）/ 长按（操作菜单），底部「查看更多回复」进对话弹层
 */
@Composable
fun FeedReplyItem(
    data: TotalReplyResponse.Data,
    position: Int,
    modifier: Modifier = Modifier,
    onOpenUser: (uid: String?) -> Unit = {},
    onReply: (id: String, cuid: String, uid: String, username: String?, position: Int, rPosition: Int?) -> Unit = { _, _, _, _, _, _ -> },
    onExpand: (id: String, uid: String, message: String?, position: Int, rPosition: Int?) -> Unit = { _, _, _, _, _ -> },
    onLikeReply: (id: String, isLike: Int) -> Unit = { _, _ -> },
    onShowTotalReply: (id: String, uid: String, position: Int, rPosition: Int?, intercept: Boolean) -> Unit = { _, _, _, _, _ -> },
    onPreviewImages: (urls: List<String>, index: Int) -> Unit = { _, _ -> },
    onLongCopyText: (text: String?) -> Unit = {},
) {
    val isLike = data.userAction?.like ?: 0
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onReply(data.id, data.uid, data.uid, data.username, position, null)
            }
            .padding(horizontal = 15.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FeedAvatar(
                url = data.userAvatar,
                size = 30,
                onClick = { onOpenUser(data.uid) },
            )
            Text(
                text = data.username,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 10.dp),
            )
            IconButton(
                onClick = {
                    onExpand(data.id, data.uid, data.message, position, null)
                },
                minWidth = 32.dp,
                minHeight = 32.dp,
            ) {
                Icon(
                    MiuixIcons.ExpandMore,
                    contentDescription = "更多操作",
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                )
            }
        }

        // 评论正文（富文本：@用户 链接 / 图片链接 / 表情）
        if (data.message.isNotEmpty()) {
            FeedHtmlText(
                text = data.message,
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp, start = 40.dp),
                onLongClick = { onLongCopyText(data.message) },
            )
        }

        // 评论配图
        FeedImageGrid(
            picArr = data.picArr,
            pic = data.pic,
            modifier = Modifier.padding(top = 10.dp, start = 40.dp),
            onPreview = onPreviewImages,
        )

        // 时间 / 评论数 / 点赞数
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = DateUtils.fromToday(data.dateline),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.weight(1f),
            )
            if (data.replynum.isNotEmpty() && data.replynum != "0") {
                Text(
                    text = data.replynum,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(end = 10.dp),
                )
            }
            Text(
                text = data.likenum,
                style = MiuixTheme.textStyles.footnote2,
                color = if (isLike == 1) MiuixTheme.colorScheme.primary
                else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.clickable {
                    onLikeReply(data.id, isLike)
                },
            )
        }

        // 子回复预览
        val replyRows = data.replyRows
        val showReplyCard = !replyRows.isNullOrEmpty() || data.replyRowsMore != 0
        if (showReplyCard) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 40.dp),
                cornerRadius = 8.dp,
                colors = top.yukonga.miuix.kmp.basic.CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.surfaceVariant,
                ),
            ) {
                replyRows?.forEachIndexed { index, replyData ->
                    val rMessage = replyData.message
                    FeedHtmlText(
                        text = rMessage,
                        style = MiuixTheme.textStyles.footnote1,
                        imgList = replyData.picArr,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onReply(
                                    replyData.id, data.uid, replyData.uid, replyData.username,
                                    position, index,
                                )
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        onLongClick = {
                            onExpand(
                                replyData.id, replyData.uid, rMessage,
                                position, index,
                            )
                        },
                        onShowMoreReply = {
                            onShowTotalReply(data.id, data.uid, position, null, true)
                        },
                    )
                }
                if (data.replyRowsMore != 0) {
                    Text(
                        text = "查看更多回复(${data.replynum})",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onShowTotalReply(data.id, data.uid, position, null, false)
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
