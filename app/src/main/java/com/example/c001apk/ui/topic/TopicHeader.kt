package com.example.c001apk.ui.topic

/** 话题页头部卡片数据 */
data class TopicHeader(
    val logo: String?,
    val title: String?,
    val hotNum: String?,
    val commentNum: String?,
    val followNum: String?,
    val avatars: List<String>,
)
