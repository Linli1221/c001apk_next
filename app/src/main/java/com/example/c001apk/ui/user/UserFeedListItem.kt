package com.example.c001apk.ui.user

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.util.DateUtils
import com.example.c001apk.util.UserCardUtils
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 用户主页 tab 列表里的动态条目（自含、可独立复用）：
 * 头像 + 昵称 + 时间 + 正文 + 首图 + 赞/评论数。
 *
 * 这是 tab 内容插槽（[UserProfileScreen] 的 `tabContent`）里可用的最简列表项，
 * 完整的 AppAdapter 富卡片体系不在本轮范围内。
 */
@Composable
fun UserFeedListItem(
    data: HomeFeedResponse.Data,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onUserClick: () -> Unit = {},
) {
    Card(
        onClick = onClick,
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row {
            UserGlideImage(
                url = data.userAvatar,
                modifier = Modifier.size(40.dp),
                circleCrop = true,
                onClick = onUserClick,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = data.username ?: data.displayUsername.orEmpty(),
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val dateline = data.dateline
                    if (dateline != null && dateline > 0) {
                        Text(
                            text = DateUtils.fromToday(dateline),
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                        )
                    }
                }

                val content = data.message ?: data.messageTitle ?: data.title
                if (!content.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = content,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val firstPic = data.picArr?.firstOrNull() ?: data.pic
                if (!firstPic.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    UserGlideImage(
                        url = firstPic,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text(
                        text = "赞 " + UserCardUtils.countText(data.likenum),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "评论 " + UserCardUtils.countText(data.commentnum),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}
