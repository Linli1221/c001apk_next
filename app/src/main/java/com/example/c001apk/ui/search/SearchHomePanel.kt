package com.example.c001apk.ui.search

/**
 * 搜索页默认态面板（Compose 版 fragment_search.xml 里的 searchHome 区块）：
 *  - 搜索历史（flex 换行词条，长按出删除按钮，对应 HistoryAdapter）
 *  - 热门搜索（词条胶囊，对应 HotSearchAdapter / item_search_hot_chip）
 *  - 热搜榜（顶部 tab + 横向滑动榜单，对应 HotRankColumnAdapter / HotRankItemAdapter）
 *
 * 数据全部由 [SearchHomePanel] 的参数传入（来自 SearchFragmentViewModel），
 * 本文件只负责展示与回调上抛，不触碰网络 / 数据库。
 */

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.c001apk.R
import com.example.c001apk.logic.model.SearchHotResponse
import com.example.c001apk.logic.model.StringEntity
import com.example.c001apk.util.http2https
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun SearchHomePanel(
    history: List<StringEntity>,
    hotItems: List<SearchHotResponse.Item>?,
    hotRankList: List<SearchHotResponse.Item>?,
    loading: Boolean,
    onSearchWord: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onClearAllHistory: () -> Unit,
    onRefreshHot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {

        // ---------------- 搜索历史 ----------------

        if (history.isNotEmpty()) {
            item(key = "historyHeader") {
                SectionHeader(
                    title = stringResource(R.string.search_history),
                    actionIcon = R.drawable.ic_delete,
                    actionDescription = stringResource(R.string.clearAll),
                    onAction = onClearAllHistory,
                )
            }
            item(key = "historyChips") {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    history.forEach { entity ->
                        HistoryChip(
                            word = entity.data,
                            onClick = { onSearchWord(entity.data) },
                            onDelete = { onDeleteHistory(entity.data) },
                        )
                    }
                }
            }
        }

        // ---------------- 热门搜索 ----------------

        if (!hotItems.isNullOrEmpty()) {
            item(key = "hotHeader") {
                SectionHeader(
                    title = stringResource(R.string.search_hot),
                    actionIcon = R.drawable.ic_refresh,
                    actionDescription = stringResource(R.string.refresh),
                    onAction = onRefreshHot,
                )
            }
            item(key = "hotChips") {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    hotItems.forEach { item ->
                        HotSearchChip(
                            item = item,
                            onClick = { onSearchWord(item.title.orEmpty()) },
                        )
                    }
                }
            }
        }

        // ---------------- 热搜榜（tab + 横向榜单） ----------------

        if (!hotRankList.isNullOrEmpty()) {
            item(key = "hotRank") {
                HotRankSection(
                    rankList = hotRankList,
                    onSearchWord = onSearchWord,
                )
            }
        }

        // ---------------- 加载 / 空态 ----------------

        if (loading) {
            item(key = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (history.isEmpty() && hotItems.isNullOrEmpty() && hotRankList.isNullOrEmpty()) {
            item(key = "empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无内容",
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.footnote1,
                    )
                }
            }
        }

        item(key = "bottomSpace") { Spacer(Modifier.height(20.dp)) }
    }
}

// ---------------- 区块标题（16sp 加粗 + 右侧动作按钮） ----------------

@Composable
private fun SectionHeader(
    title: String,
    actionIcon: Int,
    actionDescription: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, top = 10.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface,
        )
        IconButton(onClick = onAction) {
            Icon(
                painter = painterResource(actionIcon),
                contentDescription = actionDescription,
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    }
}

// ---------------- 搜索历史词条（长按显示删除） ----------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryChip(
    word: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDelete by remember(word) { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .padding(5.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDelete = !showDelete },
            )
            .padding(start = 12.dp, top = 7.dp, end = 12.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = word,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 140.dp),
        )
        if (showDelete) {
            Spacer(Modifier.width(6.dp))
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.clearAll),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                modifier = Modifier
                    .size(14.dp)
                    .clickable {
                        showDelete = false
                        onDelete()
                    },
            )
        }
    }
}

// ---------------- 热门搜索词条胶囊 ----------------

@Composable
private fun HotSearchChip(
    item: SearchHotResponse.Item,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(5.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 7.dp, end = 12.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // logo 只有是图片地址时才加载，本地图标名（如 ic_xxx）直接忽略（与 HotSearchAdapter 一致）
        val logoUrl = item.logo
        if (!logoUrl.isNullOrEmpty() && logoUrl.startsWith("http")) {
            GlideImage(
                url = logoUrl,
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = item.title.orEmpty(),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 140.dp),
        )
    }
}

// ---------------- 热搜榜：tab + 横向滑动的榜单列 ----------------

@Composable
private fun HotRankSection(
    rankList: List<SearchHotResponse.Item>,
    onSearchWord: (String) -> Unit,
) {
    var selectedTab by remember(rankList) { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 横向滑动时反查当前落在哪一列，同步选中的 tab（复刻老 hotRankScrollListener 的“过半”判定）
    LaunchedEffect(listState, rankList) {
        snapshotFlow {
            val first = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val itemSize = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
            if (itemSize > 0 && offset > itemSize / 2) first + 1 else first
        }.collect { index ->
            selectedTab = index.coerceIn(0, (rankList.size - 1).coerceAtLeast(0))
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        TabRow(
            tabs = rankList.map { it.title.orEmpty() },
            selectedTabIndex = selectedTab,
            onTabSelected = { index ->
                selectedTab = index
                coroutineScope.launch { listState.animateScrollToItem(index) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
        )
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 6.dp),
        ) {
            itemsIndexed(rankList) { _, column ->
                Column(
                    modifier = Modifier
                        // 列宽取屏宽的 62%，滑动时能同时露出左右两列（与老 HotRankColumnAdapter 一致）
                        .fillMaxWidth(0.62f)
                        .padding(horizontal = 6.dp),
                ) {
                    column.entities.orEmpty().forEachIndexed { index, item ->
                        HotRankRow(
                            rankNo = index + 1,
                            item = item,
                            onClick = { onSearchWord(item.title.orEmpty()) },
                        )
                    }
                }
            }
        }
    }
}

// ---------------- 热搜榜条目行：名次 + 标题 + 火焰 + 热度 ----------------

@Composable
private fun HotRankRow(
    rankNo: Int,
    item: SearchHotResponse.Item,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            .heightIn(min = 42.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankBadge(rankNo)
        Text(
            text = item.title.orEmpty(),
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            painter = painterResource(R.drawable.ic_search_hot),
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 6.dp)
                .size(14.dp),
        )
        Text(
            // 服务端已经给好「528万」这类文案，兜底才用原始数值（与 HotRankItemAdapter 一致）
            text = item.hotNumTxt ?: item.hotNum.orEmpty(),
            modifier = Modifier.padding(start = 2.dp),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.primary,
            maxLines = 1,
        )
    }
}

/**
 * 名次徽标：官方前三名是彩色圆角方块 + 白字，第 4 名开始只是普通灰色数字。
 * 前三名沿用现有 res 颜色 search_rank_1/2/3（老 HotRankItemAdapter 同款）。
 */
@Composable
private fun RankBadge(rankNo: Int) {
    val bgColorRes = when (rankNo) {
        1 -> R.color.search_rank_1
        2 -> R.color.search_rank_2
        3 -> R.color.search_rank_3
        else -> 0
    }
    val hasColor = bgColorRes != 0
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .then(
                if (hasColor) Modifier.background(colorResource(bgColorRes))
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rankNo.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (hasColor) Color.White else MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

// ---------------- Glide 图片（现有 Glide 链路，AndroidView 包 ImageView） ----------------

@Composable
private fun GlideImage(
    url: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            Glide.with(imageView).load(url.http2https).into(imageView)
        },
    )
}
