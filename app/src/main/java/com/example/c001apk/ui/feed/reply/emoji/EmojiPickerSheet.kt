package com.example.c001apk.ui.feed.reply.emoji

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.livedata.observeAsState
import com.example.c001apk.R
import com.example.c001apk.ui.feed.reply.ReplyViewModel
import com.example.c001apk.util.EmojiUtils
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

/*
 * 琛ㄦ儏闈㈡澘锛圕ompose 鐗?EmojiPagerAdapter/EmojiChildPagerAdapter/EmojiGridAdapter + emojiLayout锛夈€? *
 * - 涓変釜鍒嗙被锛氭渶杩?/ 榛樿 / 閰峰竵锛堣€佸疄鐜版槸 ViewPager2 澶氶〉 + GridView锛岃繖閲屾敼涓哄垎绫?+ 鎳掑姞杞界綉鏍硷紝
 *   瑙嗚涓庝氦浜掔瓑浠凤紝婊氬姩鎹㈤〉鏇夸唬宸﹀彸缈婚〉锛夈€? * - 琛ㄦ儏璧勬簮瀹屽叏澶嶇敤 EmojiUtils.emojiMap锛坉rawable锛夛紝鏈€杩戣〃鎯呰蛋 ReplyViewModel.recentEmojiLiveData銆? * - 寮瑰眰鐢?Miuix [OverlayBottomSheet]锛堥渶 Scaffold 绁栧厛锛夈€? */

private const val TAB_RECENT = 0
private const val TAB_DEFAULT = 1
private const val TAB_COOLB = 2

@Composable
fun EmojiPickerSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onEmojiClick: (String) -> Unit,
    onBackspace: () -> Unit,
    viewModel: ReplyViewModel,
) {
    var tab by remember { mutableIntStateOf(TAB_DEFAULT) }

    // EmojiUtils.emojiMap 鐨勫浐瀹氬垏鍒嗭紙涓?ReplyActivity 鐨?init 涓€鑷达級锛?    // 鍓?4 椤规槸 [缃《]/[妤间富]/[灞備富]/[鍥剧墖] 鐗规畩鏍囪锛屼笉杩涜〃鎯呴潰鏉?    val dataList = remember { EmojiUtils.emojiMap.toList() }
    val defaultList = remember(dataList) {
        dataList.subList(4, minOf(112, dataList.size))
    }
    val coolBList = remember(dataList) {
        dataList.subList(minOf(112, dataList.size), dataList.size)
    }
    val recent by viewModel.recentEmojiLiveData.observeAsState()
    val recentList = recent.orEmpty().map { item ->
        Pair(item.data, EmojiUtils.emojiMap[item.data] ?: R.drawable.ic_logo)
    }

    OverlayBottomSheet(
        show = show,
        title = "琛ㄦ儏",
        onDismissRequest = onDismissRequest,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 鍒嗙被鎸囩ず鍣紙鏈€杩?/ 榛樿 / 閰峰竵锛?            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf("鏈€杩?, "榛樿", "閰峰竵").forEachIndexed { index, name ->
                    Text(
                        text = name,
                        style = MiuixTheme.textStyles.body1,
                        color = if (tab == index) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { tab = index }
                            .padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                if (tab == TAB_RECENT && recentList.isNotEmpty()) {
                    TextButton(
                        text = "娓呯┖",
                        onClick = { viewModel.deleteAll() },
                        colors = ButtonDefaults.textButtonColors(
                            textColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        ),
                    )
                }
            }
            HorizontalDivider(
                color = MiuixTheme.colorScheme.dividerLine,
                thickness = 0.5.dp,
            )

            when (tab) {
                TAB_RECENT -> {
                    if (recentList.isEmpty()) {
                        Text(
                            text = "鏆傛棤鏈€杩戜娇鐢?,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(24.dp),
                        )
                    } else {
                        EmojiGrid(
                            emojis = recentList,
                            onEmojiClick = { code ->
                                viewModel.updateRecentEmoji(code)
                                onEmojiClick(code)
                            },
                            onBackspace = null,
                        )
                    }
                }

                TAB_DEFAULT -> EmojiGrid(
                    emojis = defaultList,
                    onEmojiClick = { code ->
                        viewModel.updateRecentEmoji(code)
                        onEmojiClick(code)
                    },
                    onBackspace = onBackspace,
                )

                TAB_COOLB -> EmojiGrid(
                    emojis = coolBList,
                    onEmojiClick = { code ->
                        viewModel.updateRecentEmoji(code)
                        onEmojiClick(code)
                    },
                    onBackspace = onBackspace,
                )
            }
        }
    }
}

/** 7 鍒楄〃鎯呯綉鏍硷紙瀵瑰簲鑰?GridView numColumns=7锛夛紱onBackspace 闈炵┖鏃舵湯灏捐拷鍔犻€€鏍奸敭 */
@Composable
private fun EmojiGrid(
    emojis: List<Pair<String, Int>>,
    onEmojiClick: (String) -> Unit,
    onBackspace: (() -> Unit)?,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
    ) {
        items(emojis) { emoji ->
            IconButton(
                onClick = { onEmojiClick(emoji.first) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(emoji.second),
                    contentDescription = emoji.first,
                    tint = Color.Unspecified, // 褰╄壊琛ㄦ儏涓嶅仛鏌撹壊
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        if (onBackspace != null) {
            item {
                IconButton(
                    onClick = onBackspace,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_backspace),
                        contentDescription = "鍒犻櫎",
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
