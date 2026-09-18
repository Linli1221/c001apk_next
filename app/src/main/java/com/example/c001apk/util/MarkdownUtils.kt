package com.example.c001apk.util

import android.content.Context
import android.text.Spanned
import com.google.android.material.color.MaterialColors
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * 酷安服务端不支持 Markdown，此处在客户端自行渲染：
 * 自动检测正文中的 md 特征（哪怕是插在普通文本中间的片段），
 * 命中则走 Markwon 渲染，否则保持原有 Html.fromHtml 逻辑。
 */
object MarkdownUtils {

    // ---------------- 自动检测 ----------------

    // 酷安话题 #[xxx]#，先剔除避免干扰标题/链接判断
    private val topicPattern = Pattern.compile("#\\[[^\\]\\n]*\\]#")

    // 强特征：任一命中即按 md 渲染
    private val fencePattern = Pattern.compile("(?m)^`{3,}")
    private val headingPattern = Pattern.compile("(?m)^#{1,6}\\s+\\S")
    private val inlineCodePattern = Pattern.compile("`[^`\\n]+`")
    private val strikePattern = Pattern.compile("~~[^~\\n]+~~")
    private val linkPattern = Pattern.compile("\\[[^\\]\\n]*\\]\\(\\s*[^\\s)]+\\s*\\)")
    private val imagePattern = Pattern.compile("!\\[[^\\]\\n]*\\]\\(\\s*[^\\s)]+\\s*\\)")
    private val boldPattern = Pattern.compile("(\\*\\*[^*\\n]+\\*\\*|__[^_\\n]+__)")

    // 弱特征：累计 2 个以上才认为是 md，降低误报
    private val bulletPattern = Pattern.compile("(?m)^[-*+]\\s+\\S")
    private val orderedPattern = Pattern.compile("(?m)^\\d{1,3}\\.\\s+\\S")
    private val quotePattern = Pattern.compile("(?m)^>\\s?\\S")
    private val hrPattern = Pattern.compile("(?m)^ {0,3}(-{3,}|\\*{3,}|_{3,})\\s*$")
    private val tableRowPattern = Pattern.compile("(?m)^\\|.+)\\|\\s*$")

    fun isMarkdown(text: String?): Boolean {
        if (text.isNullOrBlank() || text.length < 4) return false
        val t = topicPattern.matcher(text).replaceAll("")
        if (fencePattern.matcher(t).find() ||
            headingPattern.matcher(t).find() ||
            inlineCodePattern.matcher(t).find() ||
            strikePattern.matcher(t).find() ||
            linkPattern.matcher(t).find() ||
            imagePattern.matcher(t).find() ||
            boldPattern.matcher(t).find()
        ) return true
        var score = 0
        if (count(bulletPattern, t) >= 2) score++
        if (orderedPattern.matcher(t).find()) score++
        if (quotePattern.matcher(t).find()) score++
        if (hrPattern.matcher(t).find()) score++
        if (count(tableRowPattern, t) >= 2) score++
        return score >= 2
    }

    private fun count(p: Pattern, text: String): Int {
        val m = p.matcher(text)
        var c = 0
        while (m.find()) c++
        return c
    }

    // ---------------- HTML 预处理 ----------------

    private val brPattern = Pattern.compile("<br\\s*/?>", Pattern.CASE_INSENSITIVE)
    private val aTagPattern = Pattern.compile(
        "<a\\b[^>]*?href\\s*=\\s*[\"']([^\"']*)[\"'][^>]*>(.*?)</a>",
        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
    )

    private fun preprocess(text: String): String {
        var t = brPattern.matcher(text).replaceAll("\n")

        // <a href="url">label</a> → [label](url)
        val m = aTagPattern.matcher(t)
        if (m.find()) {
            val sb = StringBuffer()
            do {
                val url = m.group(1)?.trim().orEmpty()
                val label = m.group(2).orEmpty()
                    .replace("\\", "\\\\")
                    .replace("[", "\\[")
                    .replace("]", "\\]")
                val md = if (url.isEmpty()) label
                else "[$label](${
                    url.replace(" ", "%20")
                        .replace("(", "%28")
                        .replace(")", "%29")
                })"
                m.appendReplacement(sb, Matcher.quoteReplacement(md))
            } while (m.find())
            m.appendTail(sb)
            t = sb.toString()
        }

        // 常见行内 HTML 标签 → markdown 等价语法
        t = t.replace(Regex("(?i)</?(b|strong)>"), "**")
        t = t.replace(Regex("(?i)</?(i|em)>"), "*")
        t = t.replace(Regex("(?i)</?(s|strike|del)>"), "~~")
        return t
    }

    // ---------------- 渲染 ----------------

    @Volatile
    private var markwon: Markwon? = null

    private fun getMarkwon(context: Context): Markwon =
        markwon ?: synchronized(this) {
            markwon ?: build(context.applicationContext).also { markwon = it }
        }

    private fun build(context: Context): Markwon =
        Markwon.builder(context)
            .usePlugin(CorePlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(GlideImagesPlugin.create(context))
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    // 标题颜色跟随正文颜色，避免暗色主题下黑字
                    builder.headingColor(
                        MaterialColors.getColor(context, android.R.attr.textColorPrimary, 0)
                    )
                }
            })
            .build()

    /**
     * 解析 md 文本为 Spanned。
     * Markwon 的 LinkSpan 继承自 URLSpan，会被 SpannableStringBuilderUtil
     * 统一替换为 MyURLSpan，点击行为与普通正文链接一致。
     */
    fun parse(context: Context, text: String): Spanned =
        getMarkwon(context).toMarkdown(preprocess(text))
}
