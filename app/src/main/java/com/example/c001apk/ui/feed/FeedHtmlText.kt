package com.example.c001apk.ui.feed

import android.util.TypedValue
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import com.example.c001apk.util.SpannableStringBuilderUtil
import com.example.c001apk.view.LinkMovementClickMethod
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 富文本（动态正文 / 评论正文）桥接组件。
 *
 * 老代码用 `LinkTextView` + [SpannableStringBuilderUtil] 渲染服务端 HTML
 * （`<a class="feed-link-uname">`、`<br/>`、`feed-forward-pic` 图片链接）以及 markdown、
 * 表情占位（`[楼主]`/`[doge]`）——这套解析属于 util 层，按迁移契约继续复用，
 * 这里用 `AndroidView` 包一个 [TextView] 承载（Glide 之外不引入新的富文本方案）。
 *
 * 颜色 / 字号仍从 MiuixTheme token 取（默认 [MiuixTheme.textStyles.main] + onSurface），
 * 链接点击由 [SpannableStringBuilderUtil] 生成的 MyURLSpan 自己处理（跳转用户/话题/链接）。
 */
@Composable
fun FeedHtmlText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MiuixTheme.textStyles.main,
    color: Color = MiuixTheme.colorScheme.onSurface,
    imgList: List<String>? = null,
    lineSpacingMultiplier: Float = 1.3f,
    onLongClick: (() -> Unit)? = null,
    onShowMoreReply: (() -> Unit)? = null,
) {
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    // emoji span 尺寸按字号像素值计算，与老代码 SpannableStringBuilderUtil.setText 的 size 参数一致
    val sizePx = with(androidx.compose.ui.platform.LocalDensity.current) {
        style.fontSize.toPx()
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                movementMethod = LinkMovementClickMethod.instance
                setOnLongClickListener {
                    val handler = currentOnLongClick
                    handler?.invoke()
                    handler != null
                }
            }
        },
        update = { textView ->
            textView.text = SpannableStringBuilderUtil.setText(
                textView.context, text, sizePx, imgList, onShowMoreReply,
            )
            textView.setTextColor(color.toArgb())
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx)
            textView.setLineSpacing(0f, lineSpacingMultiplier)
        },
    )
}

