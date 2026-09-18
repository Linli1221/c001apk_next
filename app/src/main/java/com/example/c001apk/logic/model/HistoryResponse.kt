package com.example.c001apk.logic.model

/** `GET /v6/user/hitHistoryList`：酷安云端浏览历史 */
data class HitHistoryListResponse(
    val data: List<HitHistoryData>?,
    val message: String?
)

data class HitHistoryData(
    val title: String?,
    val description: String?,
    val logo: String?,
    /** 相对路径，如 `/feed/73697914`、`/t/xxx`、`/product/4276`、`/apk/xxx` */
    val url: String?,
    val historyType: String?,
    /** 「话题」「数码」等类型名，feed 时空串 */
    val typeName: String?,
    /** 形如 `feed:73697914` */
    val id: String?,
    val entityType: String?,
    val dateline: Long?
)
