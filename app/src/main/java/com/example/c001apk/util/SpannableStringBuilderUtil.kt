package com.example.c001apk.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ReplacementSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import com.example.c001apk.view.CenteredImageSpan
import com.example.c001apk.view.MyURLSpan
import io.noties.markwon.core.spans.CodeBlockSpan
import java.util.regex.Pattern

object SpannableStringBuilderUtil {

    fun setText(
        mContext: Context,
        text: String,
        size: Float,
        imgList: List<String>?,
        showMoreReply: (() -> Unit)? = null
    ): SpannableStringBuilder {
        val mess: Spanned =
            if (MarkdownUtils.isMarkdown(text))
                MarkdownUtils.parse(mContext, text)
            else
                Html.fromHtml(
                    text.replace("\n", "<br/>"),
                    Html.FROM_HTML_MODE_COMPACT
                )
        val builder = SpannableStringBuilder(mess)
        val urls = builder.getSpans(
            0, mess.length,
            URLSpan::class.java
        )
        urls.forEach {
            val url = MarkdownUtils.cleanUrl(it.url)
            val start = builder.getSpanStart(it)
            var end = builder.getSpanEnd(it)
            // 服务端会把 "…(url)" 整段识别成链接（尾随标点被吞），
            // 显示文本等于 URL 时按清洗后的长度收缩 span，避免标点被染成链接
            if (builder.subSequence(start, end).toString() == it.url)
                end = start + url.length
            val flags = builder.getSpanFlags(it)
            builder.setSpan(MyURLSpan(mContext, url, imgList, showMoreReply), start, end, flags)
            builder.removeSpan(it)
        }
        if (PrefManager.showEmoji) {
            val pattern = Pattern.compile("\\[[^\\]]+\\]")
            val matcher = pattern.matcher(builder)
            while (matcher.find()) {
                val group = matcher.group()
                EmojiUtils.emojiMap[group]?.let {
                    mContext.getDrawable(it)?.let { emoji ->
                        if (group in listOf("[楼主]", "[层主]", "[置顶]"))
                            emoji.setBounds(0, 0, (size * 2).toInt(), size.toInt())
                        else
                            emoji.setBounds(0, 0, (size * 1.3).toInt(), (size * 1.3).toInt())
                        val imageSpan = CenteredImageSpan(emoji, (size * 1.3).toInt(), group)
                        builder.setSpan(
                            imageSpan,
                            matcher.start(),
                            matcher.end(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                }
            }
        }
        addCodeCopyButtons(mContext, builder)
        return builder
    }

    // ---------------- 代码块复制按钮 ----------------

    /**
     * 在每个 markdown 代码块首行行首插入一个「复制」按钮，
     * 点击即把该代码块内容写入剪贴板。
     */
    private fun addCodeCopyButtons(
        context: Context,
        builder: SpannableStringBuilder
    ) {
        val blocks = builder.getSpans(
            0, builder.length,
            CodeBlockSpan::class.java
        )
        if (blocks.isEmpty()) return
        blocks.map { builder.getSpanStart(it) to builder.getSpanEnd(it) }
            .filter { it.first >= 0 && it.second > it.first }
            .sortedByDescending { it.first } // 从后往前插入，避免下标偏移
            .forEach { (start, end) ->
                val code = builder.subSequence(start, end).toString().trim('\n')
                builder.insert(start, PLACEHOLDER)
                builder.setSpan(
                    CopyCodeButtonSpan(),
                    start, start + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                builder.setSpan(
                    object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                    as? ClipboardManager ?: return
                            cm.setPrimaryClip(ClipData.newPlainText("code", code))
                            Toast.makeText(context, "代码已复制", Toast.LENGTH_SHORT).show()
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            // 保持原样式，不做任何装饰
                        }
                    },
                    start, start + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
    }

    private const val PLACEHOLDER = "\uFFFC"

    /** 绘制成一个小圆角「复制」按钮，颜色跟随正文颜色 */
    private class CopyCodeButtonSpan : ReplacementSpan() {

        private val label = "复制"

        override fun getSize(
            paint: Paint,
            text: CharSequence,
            start: Int,
            end: Int,
            fm: Paint.FontMetricsInt?
        ): Int {
            val p = Paint(paint)
            p.textSize = paint.textSize * 0.72f
            return (p.measureText(label) + p.textSize).toInt()
        }

        override fun draw(
            canvas: Canvas,
            text: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            top: Int,
            y: Int,
            bottom: Int,
            paint: Paint
        ) {
            val p = Paint(paint)
            p.textSize = paint.textSize * 0.72f
            val width = p.measureText(label) + p.textSize
            val height = p.textSize * 1.7f
            val centerY = (top + bottom) / 2f
            val rect = RectF(x, centerY - height / 2f, x + width, centerY + height / 2f)
            val radius = height / 2f
            val textColor = paint.color

            p.style = Paint.Style.FILL
            p.color = ColorUtils.setAlphaComponent(textColor, 30)
            canvas.drawRoundRect(rect, radius, radius, p)

            p.style = Paint.Style.STROKE
            p.strokeWidth = 1f
            p.color = ColorUtils.setAlphaComponent(textColor, 120)
            canvas.drawRoundRect(rect, radius, radius, p)

            p.style = Paint.Style.FILL
            p.textAlign = Paint.Align.CENTER
            p.color = ColorUtils.setAlphaComponent(textColor, 190)
            canvas.drawText(
                label,
                x + width / 2f,
                centerY - (p.descent() + p.ascent()) / 2f,
                p
            )
        }
    }

}