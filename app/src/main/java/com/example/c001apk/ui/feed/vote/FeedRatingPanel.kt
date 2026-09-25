package com.example.c001apk.ui.feed.vote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.c001apk.logic.model.HomeFeedResponse
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 发表点评（type=rating）的评分面板 —— 对应老的 [com.example.c001apk.ui.feed.reply.ReplyActivity]
 * 里 `ratingLayout` 那一块（总体评分 + 各子项打分 + 优点/不足 + 已购机）。
 *
 * 视觉要点（对应上游「回复页评分面板补底色」的改动）：
 * 回复页是半透明主题（AppThemeTranslucent），评分面板必须自己铺底色，否则下层页面会透上来。
 * 这里用 [MiuixTheme.colorScheme] 的语义背景色铺底（可通过 backgroundColor 覆盖，不硬编码色值），
 * 由宿主页面决定形状（底部弹层宿主可再套顶部圆角裁剪）。
 *
 * 状态全部提升为参数（打分/文案/已购机都是业务状态，不在这里自持）：
 * - 总体评分老代码是 10 星制（rating_score_1，0~10）
 * - 各子项（续航/影像/性能/屏幕/外观质感/性价比）是 5 星制（v4_score_item_1..n，0~5），
 *   星档文案取 RatingItem.starDesc
 *
 * 打分控件为自绘星标（Miuix 无星级组件，按契约用基础组件拼；颜色走主题 token）。
 */
@Composable
fun FeedRatingPanel(
    targetTitle: String,
    items: List<HomeFeedResponse.RatingItem>,
    overallRating: Int,
    onOverallRatingChange: (Int) -> Unit,
    subRatings: List<Int>,
    onSubRatingChange: (index: Int, rating: Int) -> Unit,
    goodText: String,
    onGoodTextChange: (String) -> Unit,
    badText: String,
    onBadTextChange: (String) -> Unit,
    buyStatus: Boolean,
    onBuyStatusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MiuixTheme.colorScheme.background,
    overallMaxStars: Int = 10,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = backgroundColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = targetTitle,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = "总体评分",
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )
        StarRatingRow(
            rating = overallRating,
            maxStars = overallMaxStars,
            onRatingChange = onOverallRatingChange,
            modifier = Modifier.padding(top = 6.dp),
        )

        items.forEachIndexed { index, item ->
            val rating = subRatings.getOrElse(index) { 0 }
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = item.name.orEmpty(),
                    style = MiuixTheme.textStyles.body2,
                )
                StarRatingRow(
                    rating = rating,
                    maxStars = 5,
                    onRatingChange = { onSubRatingChange(index, it) },
                    modifier = Modifier.padding(top = 4.dp),
                )
                val desc = if (rating > 0) {
                    item.starDesc?.getOrNull(rating - 1)?.let { "${item.name}：$it" }
                } else {
                    null
                }
                Text(
                    text = desc.orEmpty(),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        TextField(
            value = goodText,
            onValueChange = onGoodTextChange,
            label = "优点（选填）",
            minLines = 1,
            maxLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
        TextField(
            value = badText,
            onValueChange = onBadTextChange,
            label = "不足（选填）",
            minLines = 1,
            maxLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clickable { onBuyStatusChange(!buyStatus) },
        ) {
            Checkbox(
                state = if (buyStatus) ToggleableState.On else ToggleableState.Off,
                onClick = { onBuyStatusChange(!buyStatus) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "已购买此机型",
                style = MiuixTheme.textStyles.body1,
            )
        }
    }
}

/** 自绘星级打分行（Miuix 无星级组件）：点亮 = primary，未点亮 = onSurfaceVariantSummary */
@Composable
private fun StarRatingRow(
    rating: Int,
    maxStars: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        for (star in 1..maxStars) {
            val filled = star <= rating
            Text(
                text = if (filled) "★" else "☆",
                style = MiuixTheme.textStyles.headline2,
                color = if (filled) MiuixTheme.colorScheme.primary
                else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onRatingChange(star) }
                    .padding(2.dp),
            )
        }
    }
}
