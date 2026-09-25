package com.example.c001apk.ui.common

import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import com.example.c001apk.util.ImageUtil

/**
 * 图片封装：AndroidView 包 [ImageView]，沿用现有 Glide 链路（[ImageUtil.showIMG]）加载。
 *
 * **用途**：Compose 页面里所有静态图片（头像、封面、缩略图等）。不引入新图片库：
 * 加载、gif 识别、夜间模式滤色、缓存策略全部复用旧 `ImageUtil.showIMG`（Glide）。
 * 圆形/圆角通过 [shape] 裁剪（如 `CircleShape`、`RoundedCornerShape(12.dp)`）。
 *
 * **参数**
 * @param url 图片地址（可空；为空时 Glide 不发起加载，视图留空）。组件内部不做 https 改写，
 *   与旧链路一致（`showIMG` 自行处理 `http2https`）。
 * @param modifier 应用于图片容器的 [Modifier]（尺寸请在这里给，如 `Modifier.size(40.dp)`）。
 * @param shape 裁剪形状，默认 [RectangleShape] 不裁圆角；头像传 [androidx.compose.foundation.shape.CircleShape]。
 * @param isCover 是否按封面模式压暗加载（旧 `showIMG(view, url, isCover = true)` 的行为）。
 * @param contentDescription 无障碍描述，默认 null（纯装饰图片）。
 * @param contentScale 图片缩放语义，映射到 ImageView 的 ScaleType：[ContentScale.Crop]（默认）
 *   → CENTER_CROP、[ContentScale.Fit] → FIT_CENTER、[ContentScale.FillBounds] → FIT_XY，
 *   其余值按 Crop 处理。默认值与旧 `ShapeableImageView` 的 centerCrop 行为一致。
 *
 * **祖先要求**：必须位于根主题 `MiuixAppTheme`（即 `MiuixTheme`）之内；无其他前置要求。
 * 点击交互请由调用方在 [modifier] 上加 `Modifier.clickable { ... }`（如需 Mojito 大图预览，
 * 用 [NineGrid] 或直接调 `ImageUtil.startBigImgViewSimple(...)`）。
 */
@Composable
fun SimpleImage(
    url: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    isCover: Boolean = false,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    // 用 (url, isCover) 作加载 key，避免每次重组都重复发起 Glide 请求
    val loadKey = url to isCover
    AndroidView(
        factory = { context ->
            ImageView(context)
        },
        update = { imageView ->
            imageView.contentDescription = contentDescription
            imageView.scaleType = when (contentScale) {
                ContentScale.Fit -> ImageView.ScaleType.FIT_CENTER
                ContentScale.FillBounds -> ImageView.ScaleType.FIT_XY
                else -> ImageView.ScaleType.CENTER_CROP
            }
            if (imageView.tag != loadKey) {
                imageView.tag = loadKey
                ImageUtil.showIMG(imageView, url, isCover)
            }
        },
        modifier = modifier.clip(shape),
    )
}
