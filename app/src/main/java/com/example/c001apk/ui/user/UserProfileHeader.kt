package com.example.c001apk.ui.user

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.c001apk.logic.model.UserProfileResponse
import com.example.c001apk.util.DateUtils
import com.example.c001apk.util.ImageUtil
import com.example.c001apk.util.UserCardUtils
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 个人主页头部：封面 + 头像 + 昵称/等级徽章 + 经验值 + 关注/私信（或编辑信息）按钮
 * + 获赞/关注/粉丝 + 性别/地区/活跃标签 + 装备条。
 *
 * 与 ViewModel 解耦：只吃 [UserProfileResponse.Data] 和交互回调。
 * 「我的」页面顶部资料区（D1）可直接复用本组件（isSelf = true），
 * 或按需单用 [UserLevelBadge] / [UserExperienceBar] / [UserStatItem] / [UserTagChip]。
 *
 * @param userData 用户资料数据，null 时渲染占位
 * @param isSelf 是不是在看自己的主页（按钮变成「编辑信息」）
 * @param showActionButtons 未登录看别人主页时整排按钮隐藏（与老 UI 一致）
 * @param onFollowClick 关注/已关注按钮点击（关注逻辑在 ViewModel）
 * @param onMessageClick 私信按钮点击（新交互，跳转由调用方接线）
 * @param onEditProfile 编辑信息按钮点击
 * @param onAvatarClick 头像点击（老 UI 是大图预览）
 * @param onCoverClick 封面点击（老 UI 是大图预览）
 * @param onShowFollowList 关注/粉丝数字点击，type 为 "follow" / "fans"
 * @param onEquipClick 装备条点击（老 UI 跳设备页 WebView）
 */
@Composable
fun UserProfileHeader(
    userData: UserProfileResponse.Data?,
    isSelf: Boolean,
    modifier: Modifier = Modifier,
    showActionButtons: Boolean = true,
    onFollowClick: () -> Unit = {},
    onMessageClick: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onCoverClick: () -> Unit = {},
    onShowFollowList: (type: String) -> Unit = {},
    onEquipClick: () -> Unit = {},
) {
    val isFollowed = userData?.isFollow == 1

    Column(modifier = modifier.fillMaxWidth()) {

        // 封面 + 压着封面下沿的头像 / 操作按钮行
        Box(modifier = Modifier.fillMaxWidth()) {
            UserGlideImage(
                url = userData?.cover,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                isCover = true,
                onClick = onCoverClick,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 114.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UserGlideImage(
                    url = userData?.userAvatar,
                    modifier = Modifier.size(72.dp),
                    circleCrop = true,
                    onClick = onAvatarClick,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isSelf) {
                    Button(onClick = onEditProfile) {
                        Text(text = "编辑信息", style = MiuixTheme.textStyles.button)
                    }
                } else if (showActionButtons) {
                    Button(
                        onClick = onFollowClick,
                        colors = if (isFollowed) {
                            ButtonDefaults.buttonColors()
                        } else {
                            ButtonDefaults.buttonColorsPrimary()
                        },
                    ) {
                        Text(
                            text = if (isFollowed) "已关注" else "关注",
                            style = MiuixTheme.textStyles.button,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(text = "私信", onClick = onMessageClick)
                }
            }
        }

        // 昵称 + 等级徽章
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = userData?.username.orEmpty(),
                style = MiuixTheme.textStyles.title2,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(modifier = Modifier.width(6.dp))
            UserLevelBadge(level = userData?.level.orEmpty())
        }

        // 认证标题
        val verifyTitle = userData?.verifyTitle
        if (!verifyTitle.isNullOrEmpty()) {
            Text(
                text = verifyTitle,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 6.dp, end = 16.dp),
            )
        }

        // 简介
        val bio = userData?.bio
        if (!bio.isNullOrEmpty()) {
            Text(
                text = bio,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 6.dp, end = 16.dp),
            )
        }

        // 等级经验值
        UserExperienceBar(
            experience = userData?.experience ?: 0,
            nextLevelExperience = userData?.nextLevelExperience ?: 0,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
        )

        // 获赞 / 关注 / 粉丝
        Row(modifier = Modifier.padding(start = 16.dp, top = 12.dp)) {
            UserStatItem(
                count = UserCardUtils.countText(userData?.beLikeNum),
                label = "获赞",
            )
            Spacer(modifier = Modifier.width(16.dp))
            UserStatItem(
                count = UserCardUtils.countText(userData?.follow),
                label = "关注",
                onClick = { onShowFollowList("follow") },
            )
            Spacer(modifier = Modifier.width(16.dp))
            UserStatItem(
                count = UserCardUtils.countText(userData?.fans),
                label = "粉丝",
                onClick = { onShowFollowList("fans") },
            )
        }

        // 性别 / 地区 / 活跃
        val chips = buildList {
            UserCardUtils.genderText(userData?.gender)
                .takeIf { it.isNotEmpty() }?.let { add(it) }
            UserCardUtils.regionText(userData?.province, userData?.city)
                .takeIf { it.isNotEmpty() }?.let { add(it) }
            if (userData != null && userData.logintime > 0) {
                add(DateUtils.fromToday(userData.logintime) + "活跃")
            }
        }
        if (chips.isNotEmpty()) {
            Row(modifier = Modifier.padding(start = 16.dp, top = 10.dp)) {
                chips.forEachIndexed { index, chip ->
                    if (index > 0) Spacer(modifier = Modifier.width(8.dp))
                    UserTagChip(text = chip)
                }
            }
        }

        // 他的装备
        val equipCount = userData?.productOwnerCount
        if (equipCount != null) {
            Card(
                onClick = onEquipClick,
                insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "他的装备",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = UserCardUtils.equipText(equipCount),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = MiuixIcons.Basic.ArrowRight,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

/** 昵称旁边的等级徽章（Lv.x）。 */
@Composable
fun UserLevelBadge(
    level: String,
    modifier: Modifier = Modifier,
) {
    if (level.isEmpty()) return
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MiuixTheme.colorScheme.secondaryVariant)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(
            text = "Lv.$level",
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSecondaryVariant,
        )
    }
}

/**
 * 等级经验值：「经验值 x / y」+ 进度条。
 * nextLevelExperience <= 0 时只显示文字不显示进度条。
 */
@Composable
fun UserExperienceBar(
    experience: Int,
    nextLevelExperience: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "经验值 $experience / $nextLevelExperience",
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        if (nextLevelExperience > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = (experience.toFloat() / nextLevelExperience).coerceIn(0f, 1f),
                height = 4.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 「获赞 / 关注 / 粉丝」里的一个数字 + 标签，数字可点击。 */
@Composable
fun UserStatItem(
    count: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = count,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

/** 性别 / 地区 / 活跃 之类的小标签。 */
@Composable
fun UserTagChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MiuixTheme.colorScheme.secondaryVariant)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSecondaryVariant,
        )
    }
}

/**
 * Glide 图片桥（Compose 里没有官方 Glide 集成，包 ImageView）。
 * 头像用 [circleCrop] 圆形裁剪；封面传 [isCover] 会走 [ImageUtil.showIMG]
 * 自带的暗色滤镜（等价老布局的 scrim）。
 */
@Composable
internal fun UserGlideImage(
    url: String?,
    modifier: Modifier = Modifier,
    isCover: Boolean = false,
    circleCrop: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        modifier = modifier
            .then(if (circleCrop) Modifier.clip(CircleShape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        update = { view ->
            if (view.tag != url) {
                view.tag = url
                ImageUtil.showIMG(view, url, isCover)
            }
        },
    )
}
