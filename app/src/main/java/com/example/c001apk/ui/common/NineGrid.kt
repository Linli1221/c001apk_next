package com.example.c001apk.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.c001apk.util.ImageUtil
import com.example.c001apk.view.ninegridimageview.NineGridImageView

/**
 * 九宫格图片布局：AndroidView 包现有 [NineGridImageView]，完整桥接旧图片链路。
 *
 * **用途**：动态（feed）/ 评论 / 转发里的 `picArr` 图片组。选择包旧 View 而不是纯 Compose 重写，
 * 是为了原样保留：1~9 图的排布与单图尺寸策略、GIF/长图角标、12dp 圆角描边，
 * 以及点击进入 Mojito 大图预览（View 内部已接 `ImageUtil.startBigImgView`）。
 * URL 组装规则与旧 `ViewBindingAdapters.setGridView` 完全一致（缩略图统一加 `.s.jpg` 后缀）。
 *
 * **参数**
 * @param picArr 图片地址列表；为空 / null 时本组件不渲染任何内容（对应旧布局 `View.GONE`）。
 * @param modifier 应用于九宫格容器的 [Modifier]（宽度跟随父容器，高度由内部测量决定）。
 * @param pic 单图场景下的原图地址（用于解析 `@宽x高` 尺寸信息，见 [ImageUtil.getImageLp]）；
 *   为空时取 `picArr[0]`。
 * @param feedType 内容类型（`HomeFeedResponse` 的 `feedType` 字段）；
 *   为 `"feedArticle"` / `"trade"` 且单图横图时只显示一张 `pic` 缩略图（与旧逻辑一致）。
 * @param isCompress 单图是否按「压缩模式」测量（图片块 feedArticle 传 true，对应旧
 *   `setArticleImage` 的 `isCompress = true`；普通动态保持默认 false）。
 *
 * **祖先要求**：必须位于根主题 `MiuixAppTheme`（即 `MiuixTheme`）之内；无其他前置要求。
 * 大图预览（Mojito）由被包的 [NineGridImageView] 自行发起，不需要 Scaffold 祖先。
 */
@Composable
fun NineGrid(
    picArr: List<String>?,
    modifier: Modifier = Modifier,
    pic: String? = null,
    feedType: String? = null,
    isCompress: Boolean = false,
) {
    if (picArr.isNullOrEmpty()) return

    // 与旧 ViewBindingAdapters.setGridView 相同的尺寸解析与 URL 组装规则
    val imageLp = ImageUtil.getImageLp(pic ?: picArr[0])
    val imgWidth = imageLp.first
    val imgHeight = imageLp.second
    val urlList: List<String> =
        if (feedType in listOf("feedArticle", "trade") && imgWidth > imgHeight) {
            listOf(if (!pic.isNullOrEmpty()) "$pic.s.jpg" else "${picArr[0]}.s.jpg")
        } else {
            picArr.map { "$it.s.jpg" }
        }

    AndroidView(
        factory = { context ->
            NineGridImageView(context)
        },
        update = { nineGrid ->
            // setUrlList() 会重建全部子 View 并重新走 Glide，只在数据变化时调用
            val loadKey = Triple(urlList, imgWidth to imgHeight, isCompress)
            if (nineGrid.tag != loadKey) {
                nineGrid.tag = loadKey
                nineGrid.isCompress = isCompress
                nineGrid.imgWidth = imgWidth
                nineGrid.imgHeight = imgHeight
                nineGrid.setUrlList(urlList)
            }
        },
        modifier = modifier,
    )
}
