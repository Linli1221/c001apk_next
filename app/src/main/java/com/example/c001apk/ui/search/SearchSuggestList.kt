package com.example.c001apk.ui.search

/**
 * 搜索联想列表（Compose 版 SearchSuggestAdapter / item_search_suggest）：
 *  - 首项是「搜索用户：<关键词>」（人形图标、没有回填箭头），其余是普通联想词
 *  - 与输入关键词的最长公共片段染成主题色（模仿官方的关键词高亮）
 *  - 点击整行发起搜索；点击右侧箭头只把词回填输入框
 *
 * 与老 SearchSuggestAdapter 的一处差异（已在迁移报告中声明）：
 * 老实现把高亮基准传成了条目自身文本（普通词条会被整条染色），这里改成与用户输入的
 * 关键词求最长公共子串做高亮，与「模仿官方的关键词高亮」的注释意图一致。
 */

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.c001apk.R
import com.example.c001apk.logic.model.SearchSuggestResponse
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SearchSuggestList(
    items: List<SearchSuggestResponse.Data>,
    keyword: String,
    /** 点击整行：直接搜索该词；isUserItem 表示这是「搜索用户」条目 */
    onItemClick: (word: String, isUserItem: Boolean) -> Unit,
    /** 点击右侧箭头：只把词填回输入框，不发起搜索 */
    onFillClick: (word: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(items) { _, item ->
            val text = item.title.orEmpty()
            // 「搜索用户：xxx」这种条目走人形图标，且不提供回填
            val isUserItem = text.startsWith(USER_PREFIX)
            val word = if (isUserItem) text.removePrefix(USER_PREFIX) else text

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(word, isUserItem) }
                    .padding(horizontal = 16.dp)
                    .heightIn(min = 52.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(
                        if (isUserItem) R.drawable.ic_account else R.drawable.ic_search
                    ),
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = highlightKeyword(
                        text = text,
                        keyword = keyword,
                        highlightColor = MiuixTheme.colorScheme.primary,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!isUserItem) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search_fill),
                        contentDescription = stringResource(R.string.search),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onFillClick(word) },
                    )
                }
            }
        }
    }
}

private const val USER_PREFIX = "搜索用户："

/**
 * 把 [text] 里与 [keyword] 最长的公共片段染成 [highlightColor]，模仿官方的关键词高亮。
 * 公共片段不足 2 个字符时不做高亮（与老 SearchSuggestAdapter 一致）。
 */
private fun highlightKeyword(
    text: String,
    keyword: String,
    highlightColor: Color,
): AnnotatedString {
    val key = keyword.trim()
    if (key.isEmpty() || text.isEmpty()) return AnnotatedString(text)
    val (start, length) = longestCommonSubstring(text, key)
    if (length < 2) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        addStyle(SpanStyle(color = highlightColor), start, start + length)
    }
}

/**
 * 返回 [text] 与 [keyword] 最长公共子串的 (起始下标, 长度)，
 * 与老 SearchSuggestAdapter.longestCommonSubstring 同一算法。
 */
private fun longestCommonSubstring(text: String, keyword: String): Pair<Int, Int> {
    var bestStart = 0
    var bestLength = 0
    val dp = IntArray(text.length + 1)
    for (i in 1..keyword.length) {
        var diagonal = 0
        for (j in 1..text.length) {
            val temp = dp[j]
            dp[j] = if (keyword[i - 1].lowercaseChar() == text[j - 1].lowercaseChar())
                diagonal + 1
            else
                0
            if (dp[j] > bestLength) {
                bestLength = dp[j]
                bestStart = j - dp[j]
            }
            diagonal = temp
        }
    }
    return bestStart to bestLength
}
