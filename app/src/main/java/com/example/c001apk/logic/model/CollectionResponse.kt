package com.example.c001apk.logic.model

/** 收藏夹（多收藏夹）相关模型，字段与官方接口一致（HAR 实测） */
data class CollectionListResponse(
    val data: List<CollectionData>?,
    val message: String?
)

data class CollectionDetailResponse(
    val data: CollectionData?,
    val message: String?
)

/** `POST /v6/collection/addItem` 的响应：收藏数 + 当前动态是否在该夹（1=已收藏） */
data class CollectionActionResponse(
    val favnum: Int?,
    val collect: Int?,
    val message: String?
)

data class CollectionCheckCountResponse(
    val data: String?,
    val message: String?
)

/** `POST /v6/collection/uploadImage` 返回图片 URL */
data class CollectionUploadResponse(
    val data: String?,
    val message: String?
)

data class CollectionData(
    val id: String?,
    val uid: String?,
    val username: String?,
    val title: String?,
    val description: String?,
    val cover_pic: String?,
    val is_open: Int?,
    val is_open_title: String?,
    val item_num: Int?,
    val follow_num: Int?,
    val type: Int?,
    val url: String?,
    val entityType: String?,
    val entityId: String?,
    /** 该动态是否已收藏进这个夹（1=已收藏），仅在带 `id=<动态id>&type=feed` 查询时下发 */
    val isBeCollected: Int?,
    /** 系统默认收藏夹 */
    val isDefault: Int? = null
)
