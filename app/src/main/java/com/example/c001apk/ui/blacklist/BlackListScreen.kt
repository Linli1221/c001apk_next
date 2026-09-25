package com.example.c001apk.ui.blacklist

import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.observeAsState
import com.example.c001apk.R
import com.example.c001apk.logic.model.BlackListUser
import com.example.c001apk.util.ImageUtil
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 用户黑名单（云端）页面 —— 对应老的 [BlackListActivity] + [UserBlackListAdapter]（type = "user"）。
 *
 * 数据流完全复用 [BlackListViewModel]（LiveData 用 observeAsState 桥接），本文件只负责界面与交互：
 * 头像 / 昵称 / UID / 移出按钮，移出前用 Miuix [OverlayDialog] 确认，顶部返回 + 标题。
 *
 * 注意：
 * - 不自带 MiuixAppTheme（根主题由 Activity 接线时套）；页面自含一个 Scaffold（OverlayDialog 需要 Scaffold 祖先）。
 * - [BlackListViewModel] 是 Hilt assisted ViewModel，需在 Activity 侧用
 *   `viewModels(extrasProducer = … withCreationCallback<BlackListViewModel.Factory> …)` 创建后传入。
 * - 头像沿用 Glide 链路（ImageUtil.showIMG + AndroidView 包 ImageView），未引入新图片库。
 */
@Composable
fun BlackListScreen(
    viewModel: BlackListViewModel,
    onBack: () -> Unit,
    onUserClick: (uid: String) -> Unit = {},
    onToast: (String) -> Unit = {},
) {
    // observeAsState 返回 State<T?>，统一在这里归一化
    val users by viewModel.cloudUsers.observeAsState(emptyList())
    val loading by viewModel.loading.observeAsState(false)
    val toastEvent by viewModel.toastText.observeAsState()
    val userList = users.orEmpty()
    val isLoading = loading == true

    // 进入页面拉取云端黑名单（与老 Activity 的 initObserve 一致）
    LaunchedEffect(Unit) {
        viewModel.loadCloudUsers()
    }

    // toast 事件桥接（Event 语义：只会被消费一次）
    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let { onToast(it) }
    }

    // 待确认移出的用户（null = 不显示确认对话框）
    var pendingRemove by remember { mutableStateOf<BlackListUser?>(null) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "${stringResource(R.string.user_black_list)}（${userList.size}）",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            MiuixIcons.Back,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                isLoading && userList.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                userList.isEmpty() -> {
                    Text(
                        text = "黑名单为空",
                        modifier = Modifier.align(Alignment.Center),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(userList) { user ->
                            UserBlackListRow(
                                user = user,
                                onClick = { user.uid?.let(onUserClick) },
                                onRemoveClick = { pendingRemove = user },
                            )
                        }
                    }
                }
            }

            // 移出确认对话框（OverlayDialog 需要 Scaffold 祖先，放在 Scaffold content 内）
            OverlayDialog(
                title = "移出黑名单",
                summary = "确定将「${pendingRemove?.name.orEmpty()}」移出黑名单吗？",
                show = pendingRemove != null,
                onDismissRequest = { pendingRemove = null },
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = "取消",
                        onClick = { pendingRemove = null },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    TextButton(
                        text = "移出",
                        onClick = {
                            val target = pendingRemove
                            pendingRemove = null
                            target?.uid?.let { viewModel.removeCloudUser(it) }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            textColor = MiuixTheme.colorScheme.error,
                        ),
                    )
                }
            }
        }
    }
}

/** 一行黑名单用户：头像 + 昵称 + UID + 移出按钮（对应 item_user_black_list.xml） */
@Composable
private fun UserBlackListRow(
    user: BlackListUser,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(url = user.userAvatar)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        ) {
            Text(
                text = user.name,
                style = MiuixTheme.textStyles.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "UID: ${user.uid.orEmpty()}",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRemoveClick) {
            Icon(
                MiuixIcons.Close,
                contentDescription = "移出黑名单",
            )
        }
    }
}

/** 圆形头像：Glide（ImageUtil.showIMG）+ AndroidView 包 ImageView，圆角裁剪交给 Compose */
@Composable
private fun UserAvatar(url: String?) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            // url 变化才重新走 Glide，避免每次重组都发起加载
            if (imageView.tag != url) {
                imageView.tag = url
                ImageUtil.showIMG(imageView, url)
            }
        },
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape),
    )
}
