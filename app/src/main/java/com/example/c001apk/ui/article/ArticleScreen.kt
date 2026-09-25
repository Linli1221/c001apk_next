package com.example.c001apk.ui.article

import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.example.c001apk.logic.model.FeedArticleContentBean
import com.example.c001apk.util.ImageUtil.getImageLp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 图文（文章）渲染模型：按顺序排列即正文的「文本/图片/外链」混排结构，
 * 结构上对应 [ArticleBlock]（Text / Image），多一种 shareUrl 块
 * （老布局 item_feed_article_share_url.xml 的圆角外链卡片）。
 *
 * 只做展示，不做业务：数据转换见 [FeedArticleContentBean.Data.toArticleRenderBlock] /
 * [ArticleBlock.toArticleRenderBlock]。
 */
sealed class ArticleRenderBlock {

    /** 正文文本块（老 item_feed_article_text.xml） */
    data class Text(val text: String) : ArticleRenderBlock()

    /**
     * 正文图片块（老 item_feed_article_image.xml）。
     * [url] 是可直接交给 Glide 的最终地址（服务端图片已按老逻辑追加 ".s.jpg"）；
     * [width] / [height] 为原图尺寸，用于复刻 NineGridImageView 单图的 22:9 高度上限。
     */
    data class Image(
        val url: String,
        val description: String = "",
        val width: Int = 1,
        val height: Int = 1,
    ) : ArticleRenderBlock()

    /** 外链卡片块（老 item_feed_article_share_url.xml） */
    data class ShareUrl(val title: String, val url: String) : ArticleRenderBlock()
}

/**
 * 服务端图文正文块 → 渲染块。图片地址与老 setArticleImage 一致：追加 ".s.jpg" 走压缩图，
 * 尺寸从原始 url 的 @WxH 后缀解析（ImageUtil.getImageLp）。
 */
fun FeedArticleContentBean.Data.toArticleRenderBlock(): ArticleRenderBlock? = when (type) {
    "text" -> ArticleRenderBlock.Text(message.orEmpty())

    "image" -> {
        val raw = url.orEmpty()
        val (w, h) = getImageLp(raw)
        ArticleRenderBlock.Image(
            url = if (raw.isEmpty()) "" else "$raw.s.jpg",
            description = description.orEmpty(),
            width = w,
            height = h,
        )
    }

    "shareUrl" -> ArticleRenderBlock.ShareUrl(title = title.orEmpty(), url = url.orEmpty())
    else -> null
}

/** 服务端图文正文数组 → 渲染块列表（跳过不认识的块类型，与老 FeedDataAdapter 一致）。 */
fun List<FeedArticleContentBean.Data>.toArticleRenderBlocks(): List<ArticleRenderBlock> =
    mapNotNull { it.toArticleRenderBlock() }

/** 发布态 [ArticleBlock] → 渲染块（发布页/预览可直接复用渲染组件）。 */
fun ArticleBlock.toArticleRenderBlock(): ArticleRenderBlock = when (this) {
    is ArticleBlock.Text -> ArticleRenderBlock.Text(text)

    is ArticleBlock.Image -> {
        val parts = resolution.split("x")
        ArticleRenderBlock.Image(
            url = uri.toString(),
            description = description,
            width = parts.getOrNull(0)?.toIntOrNull() ?: 1,
            height = parts.getOrNull(1)?.toIntOrNull() ?: 1,
        )
    }
}

/**
 * 图文（文章）渲染页：标题 / 作者栏 / 正文块列表（文本、图片、外链卡片）。
 * 对应老 FeedViewModel.handleFeedData() 组装的 articleList 结构
 * （封面图 + 标题 + messageRawOutput 的 text/image/shareUrl 块）。
 *
 * 数据全部由参数传入（或复用现有 ViewModel 的 LiveData 用 observeAsState 桥接后传入），
 * 点击跳转/预览一律用回调表达：
 * - [onImageClick]：接 Mojito 大图预览（net.mikaelzero.mojito.Mojito）；
 * - [onCopyText]：接老 listener.onCopyText 的复制到剪贴板；
 * - [onOpenLink]：接老 listener.onOpenLink 的外链跳转（NetWorkUtil.openLink）。
 */
@Composable
fun ArticleScreen(
    title: String,
    authorName: String,
    blocks: List<ArticleRenderBlock>,
    modifier: Modifier = Modifier,
    authorAvatar: String? = null,
    dateline: String? = null,
    deviceTitle: String? = null,
    topBarTitle: String = "图文",
    onBack: (() -> Unit)? = null,
    onAuthorClick: () -> Unit = {},
    onImageClick: (url: String) -> Unit = {},
    onCopyText: (text: String) -> Unit = {},
    onOpenLink: (url: String, title: String?) -> Unit = { _, _ -> },
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            SmallTopAppBar(
                title = topBarTitle,
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(MiuixIcons.Back, contentDescription = "返回")
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item {
                ArticleHeader(
                    title = title,
                    authorName = authorName,
                    authorAvatar = authorAvatar,
                    dateline = dateline,
                    deviceTitle = deviceTitle,
                    onAuthorClick = onAuthorClick,
                )
            }
            if (blocks.isEmpty()) {
                item {
                    Text(
                        text = "暂无内容",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 32.dp),
                    )
                }
            }
            items(blocks.size) { index ->
                when (val block = blocks[index]) {
                    is ArticleRenderBlock.Text -> ArticleTextBlock(
                        block = block,
                        onCopyText = onCopyText,
                    )

                    is ArticleRenderBlock.Image -> ArticleImageBlock(
                        block = block,
                        onImageClick = onImageClick,
                    )

                    is ArticleRenderBlock.ShareUrl -> ArticleShareUrlBlock(
                        block = block,
                        onOpenLink = onOpenLink,
                    )
                }
            }
            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** 标题 + 作者栏（头像/昵称/时间·机型） */
@Composable
private fun ArticleHeader(
    title: String,
    authorName: String,
    authorAvatar: String?,
    dateline: String?,
    deviceTitle: String?,
    onAuthorClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAuthorClick() }
                .padding(top = 12.dp, bottom = 12.dp),
        ) {
            GlideImage(
                url = authorAvatar,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = authorName,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                val meta = listOfNotNull(dateline, deviceTitle)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
        HorizontalDivider(color = MiuixTheme.colorScheme.dividerLine)
        Spacer(Modifier.height(8.dp))
    }
}

/** 正文文本块：行距 1.3 对齐老 item_feed_article_text.xml，长按复制 */
@Composable
private fun ArticleTextBlock(
    block: ArticleRenderBlock.Text,
    onCopyText: (String) -> Unit,
) {
    if (block.text.isBlank()) return
    Text(
        text = block.text,
        style = MiuixTheme.textStyles.paragraph,
        color = MiuixTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .pointerInput(block.text) {
                detectTapGestures(onLongPress = { onCopyText(block.text) })
            },
    )
}

/**
 * 正文图片块：全宽显示，高度复刻 NineGridImageView 单图压缩模式
 * （高度最高 22:9，超高图 FIT_CENTER），下方居中灰色说明文字，点击图片回调预览。
 */
@Composable
private fun ArticleImageBlock(
    block: ArticleRenderBlock.Image,
    onImageClick: (url: String) -> Unit,
) {
    if (block.url.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        val ratio = when {
            block.width <= 0 || block.height <= 0 -> 1f
            block.height >= block.width * 22f / 9f -> 22f / 9f
            else -> block.width.toFloat() / block.height
        }
        GlideImage(
            url = block.url,
            fitCenter = block.width > 0 && block.height > block.width * 22f / 9f,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clickable { onImageClick(block.url) },
        )
        if (block.description.isNotBlank()) {
            Text(
                text = block.description,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

/** 外链卡片块（老 round_corners_12 圆角卡片，点击跳转） */
@Composable
private fun ArticleShareUrlBlock(
    block: ArticleRenderBlock.ShareUrl,
    onOpenLink: (url: String, title: String?) -> Unit,
) {
    Card(
        onClick = { onOpenLink(block.url, block.title) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = block.title,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Glide 图片桥接（Glide 无官方 Compose 集成，按迁移契约用 AndroidView 包 ImageView）。
 * 发布页封面/正文图、文章头像/正文图共用。后续如由 ui/common 收敛公共图片组件，可整体替换。
 */
@Composable
internal fun GlideImage(
    url: Any?,
    modifier: Modifier = Modifier,
    fitCenter: Boolean = false,
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType =
                    if (fitCenter) ImageView.ScaleType.FIT_CENTER else ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { view ->
            view.scaleType =
                if (fitCenter) ImageView.ScaleType.FIT_CENTER else ImageView.ScaleType.CENTER_CROP
            Glide.with(view).load(url).into(view)
        },
        modifier = modifier,
    )
}
