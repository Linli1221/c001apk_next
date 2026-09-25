package com.example.c001apk.ui.hometopic

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.util.ImageUtil
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * 话题 / 产品条目卡片（自含实现）。
 *
 * 对应老实现：`item_search_topic.xml` + [com.example.c001apk.adapter.AppAdapter.TopicProductViewHolder]。
 * 交互等价：整卡点击 → `onClick`（老实现是 `ItemListener.onViewTopic(view, entityType, title, url, id)`）。
 *
 * 注意：本卡片是 hometopic 目录自含实现，与 ui/common 后续收敛的公共卡片/图片组件存在少量重复
 * （本轮 ui/common 未必就绪，见迁移报告）。图片继续走 Glide 链路（[ImageUtil.showIMG]），
 * 通过 [AndroidView] 包 [ImageView]，不引入 coil 等新图片库。
 */
@Composable
fun HomeTopicItemCard(
    data: HomeFeedResponse.Data,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        pressFeedbackType = PressFeedbackType.Sink,
        insideMargin = PaddingValues(10.dp),
    ) {
        Row {
            HomeTopicLogo(
                url = data.logo,
                modifier = Modifier.size(64.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = data.title.orEmpty(),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(5.dp))
                Row {
                    Text(
                        text = "${data.hotNumTxt.orEmpty()}热度",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        // 与 TopicProductViewHolder.bind 一致：topic 用 commentnumTxt，其余用 feedCommentNumTxt
                        text = if (data.entityType == "topic")
                            "${data.commentnumTxt.orEmpty()}讨论"
                        else
                            "${data.feedCommentNumTxt.orEmpty()}讨论",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}

/**
 * 方形圆角 logo（对应 `searchTopicRadius` = 12dp 圆角 + `app:setImage="@{data.logo}"`）。
 * Glide 无官方 Compose 集成，按迁移契约用 [AndroidView] 包 [ImageView]。
 */
@Composable
private fun HomeTopicLogo(
    url: String?,
    modifier: Modifier = Modifier,
) {
    val shape = remember { RoundedCornerShape(12.dp) }
    Box(
        modifier = modifier
            .clip(shape)
            .background(MiuixTheme.colorScheme.surfaceVariant)
    ) {
        AndroidView(
            factory = { context ->
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
            },
            update = { imageView -> ImageUtil.showIMG(imageView, url) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
