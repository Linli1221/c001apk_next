package com.example.c001apk.logic.model

import com.google.gson.annotations.SerializedName

/**
 * 众测/活动详情 `GET /v6/event/detail?id=<id>` 响应（HAR 实测）。
 *
 * 背景：众测入口的 url 是 `/event/<id>`，但 `www.coolapk.com/event/<id>` 只是
 * 一个「活动分享」落地页（引导下载 App），没有 H5 详情。真正的详情数据走原生接口。
 */
data class EventDetailResponse(
    val data: EventDetailData?,
    val message: String?
)

data class EventDetailData(
    val id: String?,
    val title: String?,
    val type: Int?,
    val logo: String?,
    val pic: String?,
    val description: String?,
    val content: String?,
    @SerializedName("notice_rule") val noticeRule: String?,
    @SerializedName("action_url") val actionUrl: String?,
    @SerializedName("sponsor_user") val sponsorUser: String?,
    @SerializedName("sponsor_prize") val sponsorPrize: String?,
    @SerializedName("prize_user") val prizeUser: String?,
    val tag: String?,
    val product: String?,
    @SerializedName("stage_status") val stageStatus: Int?,
    val status: Int?,
    val dateline: Long?,
    val lastupdate: Long?,
    @SerializedName("time_reg_start") val timeRegStart: Long?,
    @SerializedName("time_reg_end") val timeRegEnd: Long?,
    @SerializedName("time_end") val timeEnd: Long?,
    @SerializedName("reg_num") val regNum: String?,
    @SerializedName("show_reg_num") val showRegNum: String?,
    @SerializedName("sponsorPrize") val sponsorPrizeList: List<SponsorPrize>?,
    @SerializedName("sponsorUser") val sponsorUserList: List<SponsorUser>?,
)

/** 奖品（只取展示所需字段，忽略接口额外下发的敏感账号信息） */
data class SponsorPrize(
    val title: String?,
    val logo: String?
)

/** 赞助方（只取展示所需字段） */
data class SponsorUser(
    val uid: String?,
    val username: String?,
    @SerializedName("displayUsername") val displayUsername: String?,
    @SerializedName("userAvatar") val userAvatar: String?
)
