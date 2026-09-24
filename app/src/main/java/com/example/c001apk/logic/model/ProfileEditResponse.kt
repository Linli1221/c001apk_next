package com.example.c001apk.logic.model

/**
 * `POST /v6/account/changeProfile`（改性别 / 生日 / 地区 / 签名）
 * 与 `POST /v6/account/resetProfile`（生日、地区恢复「保密」）的返回。
 *
 * 请求体是 `key` + `value` 两个表单字段（只提交改动的那一项）：
 * - `key=gender` + `value=1|0|-1`（男 / 女 / 保密）
 * - `key=`（空） + `value={"birthyear":2009,"birthmonth":10,"birthday":14}`
 * - `key=`（空） + `value={"province":"广东","city":"广州"}`
 * - `key=bio` + `value=签名正文`
 */
data class ProfileEditResponse(
    val data: ProfileEditData?,
    val message: String?,
)

data class ProfileEditData(
    val gender: Int?,
    val province: String?,
    val city: String?,
    val birthyear: Int?,
    val birthmonth: Int?,
    val birthday: Int?,
    val astro: String?,
    val bio: String?,
)
