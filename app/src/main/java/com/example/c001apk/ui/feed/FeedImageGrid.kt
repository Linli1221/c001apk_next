package com.example.c001apk.ui.feed

import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.c001apk.util.ImageUtil

/**
 * 图片块：沿用 Glide 链路（[ImageUtil.showIMG] + `AndroidView` 包 [ImageView]），
 * 不引入新图片库（迁移契约 §4）。圆角 / 圆形裁剪交给 Compose `Modifier.clip`。
 */

/** 单张图片；圆角 / 点击由参数控制，尺寸由调用方 Modifier 控制 */
@Composable
fun GlideImage(
    url: String?,
    modifier: Modifier = Modifier,
    isCover: Boolean = false,
    contentDescription: String? = null,
    cornerRadius: Int = 12,
    onClick: (() -> Unit)? = null,
) {
    val shape = if (cornerRadius <= 0) null else RoundedCornerShape(cornerRadius.dp)
    var base = if (shape != null) modifier.clip(shape) else modifier
    if (onClick != null) base = base.clickable(onClick = onClick)
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                this.contentDescription = contentDescription
            }
        },
        update = { imageView ->
            imageView.contentDescription = contentDescription
            // url 变化才重新走 Glide，避免每次重组都发起加载
            if (imageView.tag != url) {
                imageView.tag = url
                ImageUtil.showIMG(imageView, url, isCover)
            }
        },
        modifier = base,
    )
}

/** 圆形头像 */
@Composable
fun FeedAvatar(
    url: String?,
    size: Int,
    onClick: (() -> Unit)? = null,
) {
    GlideImage(
        url = url,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape),
        cornerRadius = 0,
        onClick = onClick,
    )
}

/**
 * 九宫格图片（对应老的 NineGridImageView）：
 * 1 张大图、2~3 张一行、4 张 2x2、5 张以上三列。点击任意一张回调预览（Mojito 由外层接线）。
 */
@Composable
fun FeedImageGrid(
    picArr: List<String>?,
    pic: String?,
    modifier: Modifier = Modifier,
    onPreview: (urls: List<String>, index: Int) -> Unit = { _, _ -> },
) {
    val urls = when {
        !picArr.isNullOrEmpty() -> picArr
        !pic.isNullOrEmpty() -> listOf(pic)
        else -> return
    }
    when (urls.size) {
        1 -> {
            GlideImage(
                url = urls[0],
                modifier = modifier
                    .fillMaxWidth()
                    .height(240.dp),
                onClick = { onPreview(urls, 0) },
            )
        }

        in 2..3 -> {
            Row(modifier = modifier.fillMaxWidth()) {
                urls.forEachIndexed { index, url ->
                    GlideImage(
                        url = url,
                        modifier = Modifier
                            .padding(start = if (index == 0) 0.dp else 2.dp)
                            .weight(1f)
                            .aspectRatio(1f),
                        onClick = { onPreview(urls, index) },
                    )
                }
            }
        }

        4 -> {
            Column(modifier = modifier.fillMaxWidth()) {
                urls.chunked(2).forEachIndexed { rowIndex, row ->
                    Row(modifier = Modifier.padding(top = if (rowIndex == 0) 0.dp else 2.dp)) {
                        row.forEachIndexed { colIndex, url ->
                            GlideImage(
                                url = url,
                                modifier = Modifier
                                    .padding(start = if (colIndex == 0) 0.dp else 2.dp)
                                    .weight(1f)
                                    .aspectRatio(1f),
                                onClick = { onPreview(urls, rowIndex * 2 + colIndex) },
                            )
                        }
                    }
                }
            }
        }

        else -> {
            Column(modifier = modifier.fillMaxWidth()) {
                urls.chunked(3).forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.padding(top = if (rowIndex == 0) 0.dp else 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        row.forEachIndexed { colIndex, url ->
                            GlideImage(
                                url = url,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                onClick = { onPreview(urls, rowIndex * 3 + colIndex) },
                            )
                        }
                        // 补齐每行三列，保持网格对齐
                        repeat(3 - row.size) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
