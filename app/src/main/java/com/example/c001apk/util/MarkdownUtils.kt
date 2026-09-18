package com.example.c001apk.util

import android.content.Context
import android.text.Spanned
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tables.TableTheme
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

    // 服务端已把 md 链接的 URL 换成 <a> 标签: [文字](<a href="...">查看链接</a>)
    private val serverLinkPattern = Pattern.compile("\\][ \\t]*\\(\\s*<a\\b", Pattern.CASE_INSENSITIVE)

    // 弱特征：累计 2 个以上才认为是 md，降低误报
    private val bulletPattern = Pattern.compile("(?m)^[-*+]\\s+\\S")
    private val orderedPattern = Pattern.compile("(?m)^\\d{1,3}\\.\\s+\\S")
    private val quotePattern = Pattern.compile("(?m)^>\\s?\\S")
    private val hrPattern = Pattern.compile("(?m)^ {0,3}(-{3,}|\\*{3,}|_{3,})\\s*$")
    private val tableRowPattern = Pattern.compile("(?m)^\\|.+\\|\\s*$")

    fun isMarkdown(text: String?): Boolean {
        if (text.isNullOrBlank() || text.length < 4) return false
        val t = topicPattern.matcher(text).replaceAll("")
        if (fencePattern.matcher(t).find() ||
            headingPattern.matcher(t).find() ||
            inlineCodePattern.matcher(t).find() ||
            strikePattern.matcher(t).find() ||
            linkPattern.matcher(t).find() ||
            imagePattern.matcher(t).find() ||
            serverLinkPattern.matcher(t).find() ||
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

    // 服务端把 md 链接的 URL 替换成 <a>查看链接</a> 后，与外层 md 语法形成嵌套
    // [外层文字]([查看链接](url))，降级还原为 [外层文字](url)
    private val nestedLinkPattern = Pattern.compile(
        "\\[([^\\]\\n]*)\\]\\(\\[[^\\]\\n]*\\]\\(\\s*([^)\\s]+)\\s*\\)\\)?"
    )

    /**
     * 服务端 URL 自动识别会把正文标点吞进 URL（如 "(url)" 被识别成 "url)"，
     * 去掉未配对的右括号及尾部句读标点。
     */
    fun cleanUrl(url: String): String {
        var s = url.trim()
        val open = s.count { it == '(' }
        val close = s.count { it == ')' }
        if (close > open) {
            var toDrop = close - open
            val sb = StringBuilder(s.length)
            for (i in s.indices.reversed()) {
                val c = s[i]
                if (c == ')' && toDrop > 0) {
                    toDrop--
                    continue
                }
                sb.append(c)
            }
            s = sb.reverse().toString()
        }
        while (s.isNotEmpty() && s.last() in ".,;:!?") s = s.dropLast(1)
        return s
    }

    private fun preprocess(text: String): String {
        var t = brPattern.matcher(text).replaceAll("\n")

        // <a href="url">label</a> → [label](url)
        val m = aTagPattern.matcher(t)
        if (m.find()) {
            val sb = StringBuffer()
            do {
                val rawUrl = m.group(1)?.trim().orEmpty()
                val url = cleanUrl(rawUrl)
                val rawLabel = m.group(2).orEmpty()
                // 服务端用 URL 本身当链接文字时，同样去掉被吞进去的尾随标点
                val label = (if (rawLabel.trim() == rawUrl) url else rawLabel)
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

        // 服务端 <a> 与外层 md 语法嵌套 → 还原成作者写的 [文字](url)
        val nm = nestedLinkPattern.matcher(t)
        if (nm.find()) {
            val sb = StringBuffer()
            do {
                nm.appendReplacement(
                    sb,
                    Matcher.quoteReplacement(
                        "[${nm.group(1)}](${cleanUrl(nm.group(2).orEmpty())})"
                    )
                )
            } while (nm.find())
            nm.appendTail(sb)
            t = sb.toString()
        }

        // HTML 实体解码：服务端把 " > < & 等转义了，
        // 不还原会导致引用块（&gt;）、代码块内容显示错乱
        t = decodeEntities(t)
        return t
    }

    // ---------------- HTML 实体 ----------------

    private val entityPattern = Pattern.compile(
        "&(#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|[a-zA-Z][a-zA-Z0-9]{1,9});"
    )

    private val namedEntities = mapOf(
        "quot" to "\"", "amp" to "&", "lt" to "<", "gt" to ">", "apos" to "'",
        "nbsp" to "\u00A0", "ldquo" to "\u201C", "rdquo" to "\u201D",
        "lsquo" to "\u2018", "rsquo" to "\u2019", "hellip" to "\u2026",
        "mdash" to "\u2014", "ndash" to "\u2013", "middot" to "\u00B7",
        "times" to "\u00D7", "laquo" to "\u00AB", "raquo" to "\u00BB",
        "euro" to "\u20AC", "pound" to "\u00A3", "yen" to "\u00A5",
        "sect" to "\u00A7", "para" to "\u00B6", "deg" to "\u00B0",
        "copy" to "\u00A9", "reg" to "\u00AE", "trade" to "\u2122",
        "bull" to "\u2022", "dagger" to "\u2020"
    )

    fun decodeEntities(text: String): String {
        if (text.indexOf('&') < 0) return text
        val m = entityPattern.matcher(text)
        if (!m.find()) return text
        val sb = StringBuffer()
        do {
            val name = m.group(1).orEmpty()
            val replacement = if (name.startsWith("#"))
                decodeNumericEntity(name) ?: m.group()
            else
                namedEntities[name.lowercase()] ?: m.group()
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement))
        } while (m.find())
        m.appendTail(sb)
        return sb.toString()
    }

    private fun decodeNumericEntity(name: String): String? = try {
        val codePoint = if (name.length > 1 && (name[1] == 'x' || name[1] == 'X'))
            name.substring(2).toInt(16)
        else
            name.substring(1).toInt()
        if (codePoint in 1..0x10FFFF) String(Character.toChars(codePoint)) else null
    } catch (e: NumberFormatException) {
        null
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
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(tableTheme(context)))
            .usePlugin(GlideImagesPlugin.create(context))
            .build()

    /** 默认 cellPadding 偏小，单元格文字容易贴边，这里放宽一点 */
    private fun tableTheme(context: Context): TableTheme {
        val padding = (context.resources.displayMetrics.density * 8F).toInt()
        return TableTheme.buildWithDefaults(context)
            .tableCellPadding(padding)
            .build()
    }

    /**
     * 解析 md 文本为 Spanned。
     * Markwon 的 LinkSpan 继承自 URLSpan，会被 SpannableStringBuilderUtil
     * 统一替换为 MyURLSpan，点击行为与普通正文链接一致。
     */
    fun parse(context: Context, text: String): Spanned =
        getMarkwon(context).toMarkdown(preprocess(text))
}
