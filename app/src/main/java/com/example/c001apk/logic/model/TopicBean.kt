package com.example.c001apk.logic.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TopicBean(
    val url: String,
    val title: String,
    // 服务端下发的 page_name，产品页的「参数」标签是 main
    val pageName: String? = null
): Parcelable