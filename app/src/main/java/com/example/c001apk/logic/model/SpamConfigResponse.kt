package com.example.c001apk.logic.model

/**
 * 其他屏蔽项（关键字 / 用户 / 节点）的配置。
 *
 * `GET /v6/user/spamWordList` 返回 `spam_word_config`（JSON 字符串），实测形如：
 * `{"node":[{"id":"4063","title":"一加13","targetFullId":"7000004063","targetType":"7",...}],
 *   "custom":[{"title":"关键词","nodeMd5":"..."}],
 *   "user":[{"uid":"23047571","name":"...","logo":"...","url":"..."}]}`
 *
 * 写入走 `POST /v6/account/updateConfig`（form：`key=spam_word_config`），
 * 但**写键与读键不同**：关键字写 `word`、读 `custom`；用户写/读都是 `user`；节点写/读都是 `node`，值为 `[{tid,name}]`。
 */
data class SpamConfigResponse(
    val data: Data?,
    val message: String?
) {
    data class Data(
        val spam_word_config: String?,
        val spam_word_config_max_count: Int?
    )
}

data class SpamConfig(
    val custom: List<Custom> = emptyList(),
    val user: List<User> = emptyList(),
    val node: List<Node> = emptyList()
) {
    data class Custom(
        val title: String?,
        val nodeMd5: String?
    )

    data class User(
        val uid: String?,
        val name: String?,
        val logo: String?,
        val url: String?
    )

    data class Node(
        val id: String?,
        val title: String?,
        val targetFullId: String?,
        val targetType: String?,
        val logo: String?,
        val url: String?
    )
}
