package com.example.c001apk.ui.article

import android.net.Uri

/**
 * 图文正文块：按顺序排列即正文的「文本/图片」混排结构。
 * 服务端 message 字段为 JSON 数组，text 块与 image 块交错。
 */
sealed class ArticleBlock {
    data class Text(var text: String = "") : ArticleBlock()

    data class Image(
        val uri: Uri,
        var description: String = "",
        var name: String = "",
        var resolution: String = "",
        var md5: String = "",
        val type: String = "",
        val md5Byte: ByteArray? = null,
    ) : ArticleBlock()
}

/**
 * ossUploadPrepare 的 uploadFileList 单项（图文用，比普通动态多 hdr 字段）。
 * hdr=0 表示普通图片，hdr=1 表示 Ultra HDR 图片（上传文件名会带 -uhdr 后缀）。
 */
data class ArticleUploadFile(
    val name: String,
    val resolution: String,
    val md5: String,
    val hdr: Int = 0,
)
