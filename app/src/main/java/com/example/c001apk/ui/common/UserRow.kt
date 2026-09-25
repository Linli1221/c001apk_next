package com.example.c001apk.ui.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 用户行：圆形头像 + 昵称 + 简介 + 右侧按钮插槽（基于 Miuix [BasicComponent]）。
 *
 * **用途**：用户列表 / @用户选择 / 黑名单 / 关注粉丝列表 / 消息里的用户条目。
 * 对应旧布局 `item_at_user.xml`（头像 40dp + 昵称 + 右侧控件）的通用形态，
 * 简介（bio / 认证标题等）由调用方拼好传入。
 *
 * **参数**
 * @param username 昵称（标题位，单行省略由 BasicComponent 排版保证）。
 * @param avatarUrl 头像地址，经 [SimpleImage] 走 Glide 加载，圆形裁剪；为空时留空占位。
 * @param modifier 应用于整行的 [Modifier]（列表里一般传 `Modifier.fillMaxWidth()`）。
 * @param description 简介/副标题，可空（null 时不显示第二行）。
 * @param onClick 整行点击回调（如跳转用户主页）；null 时整行不可点击。
 * @param trailing 右侧插槽（`RowScope`），放关注按钮、Checkbox 等；null 时不显示。
 *
 * **祖先要求**：必须位于根主题 `MiuixAppTheme`（即 [MiuixTheme]）之内；无其他前置要求。
 * 标题/摘要颜色由 `BasicComponentDefaults.titleColor()/summaryColor()` 跟随主题，无需手动上色。
 */
@Composable
fun UserRow(
    username: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    BasicComponent(
        modifier = modifier,
        title = username,
        summary = description,
        startAction = {
            SimpleImage(
                url = avatarUrl,
                shape = CircleShape,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(40.dp),
                contentDescription = "头像",
            )
        },
        endActions = trailing,
        onClick = onClick,
    )
}
