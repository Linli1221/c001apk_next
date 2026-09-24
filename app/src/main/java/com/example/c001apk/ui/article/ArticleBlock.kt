package com.example.c001apk.ui.article

import android.net.Uri
import com.google.gson.annotations.SerializedName

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
 *
 * 必须写 @SerializedName：这个类不在 `logic.model` 包里，`proguard-rules.pro` 的
 * `-keep class com.example.c001apk.logic.model.** { <fields>; }` 保不到它，
 * 而所有渠道的包都是 R8 混淆版（`app/build.gradle.kts` 的 release 变体）。
 * 少了注解，Gson 会按混淆后的字段名拼出 `[{"a":..,"b":..,"c":..}]`，
 * 服务端 `ossUploadPrepare` 直接回 `103 请选择正确的文件类型，不支持 ` —— 背景图和图文都发不出去。
 */
data class ArticleUploadFile(
    @SerializedName("name") val name: String,
    @SerializedName("resolution") val resolution: String,
    @SerializedName("md5") val md5: String,
    @SerializedName("hdr") val hdr: Int = 0,
)
