package com.example.c001apk.ui.feed.reply

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.compose.observeAsState
import com.example.c001apk.R
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.logic.model.OSSUploadPrepareModel
import com.example.c001apk.logic.model.TotalReplyResponse
import com.example.c001apk.ui.feed.reply.attopic.AtUserSearchSheet
import com.example.c001apk.ui.feed.reply.emoji.EmojiPickerSheet
import com.example.c001apk.util.ImageUtil.getImageDimensionsAndMD5
import com.example.c001apk.util.ImageUtil.toHex
import com.example.c001apk.util.ossUpload
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.UUID

/*
 * 回复页（Compose 版 ReplyActivity + activity_reply.xml）：
 * 回复列表（可选，数据由宿主传入） + 发回复输入区（正文 / 表情 / 图片 / @用户 / #话题# / 外链 / 发布）。
 *
 * 数据流完全复用 [ReplyViewModel]（LiveData 用 observeAsState 桥接），本文件只负责界面与交互。
 * 发布时的 replyAndFeedData 组装逻辑按 ReplyActivity.onClick(R.id.publish) 原样移植。
 */

/** 选中的图片附件（Glide 缩略图 + OSS 上传准备信息） */
private class ReplyAttachment(
    val uri: Uri,
    val type: String,
    val md5: ByteArray?,
    val model: OSSUploadPrepareModel,
    val width: Int,
    val height: Int,
)

@Composable
fun ReplyScreen(
    viewModel: ReplyViewModel,
    type: String = "reply",
    rid: String? = null,
    username: String? = null,
    targetType: String? = null,
    targetId: String? = null,
    title: String? = null,
    ratingTarget: String? = null,
    ratingItems: List<HomeFeedResponse.RatingItem>? = null,
    replyList: List<TotalReplyResponse.Data> = emptyList(),
    replyListFooter: FooterState? = null,
    onReplyListLoadMore: () -> Unit = {},
    onReplyItemClick: (TotalReplyResponse.Data) -> Unit = {},
    onReplyItemLike: (TotalReplyResponse.Data) -> Unit = {},
    onReplyItemMore: (TotalReplyResponse.Data) -> Unit = {},
    onToast: (String) -> Unit = {},
    onFinish: () -> Unit = {},
) {
    val context = LocalContext.current

    // 与 ReplyActivity.onCreate 一致：先把 type/rid 交给 ViewModel
    LaunchedEffect(Unit) {
        viewModel.type = type
        viewModel.rid = rid
    }

    // ---------- 状态 ----------
    var text by remember {
        mutableStateOf(
            TextFieldValue(
                text = if (title.isNullOrEmpty()) "" else "#$title# ",
                selection = TextRange(if (title.isNullOrEmpty()) 0 else title.length + 3),
            )
        )
    }
    var replyAndForward by remember { mutableStateOf(false) }
    var buyStatus by remember { mutableStateOf(false) }
    var goodText by remember { mutableStateOf("") }
    var badText by remember { mutableStateOf("") }
    var ratingOverall by remember { mutableStateOf(0) }
    val subRatings = remember { mutableStateListOf<Int>() }
    LaunchedEffect(ratingItems) {
        subRatings.clear()
        ratingItems?.forEach { subRatings.add(0) }
    }

    val attachments = remember { mutableStateListOf<ReplyAttachment>() }
    var extraTitle by remember { mutableStateOf("") }
    var extraUrl by remember { mutableStateOf("") }
    var posting by remember { mutableStateOf(false) }
    var captchaBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var captchaCode by remember { mutableStateOf("") }

    var showEmojiSheet by remember { mutableStateOf(false) }
    var showAtSheet by remember { mutableStateOf(false) }
    var showTopicSheet by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var isFromAt by remember { mutableStateOf(false) }

    val canPublish = text.text.isNotBlank()

    // ---------- 光标处插入 / 删除（表情、@、#话题#） ----------
    fun insertAtCursor(s: String) {
        val start = text.selection.min
        val end = text.selection.max
        val newText = text.text.replaceRange(start, end, s)
        text = TextFieldValue(newText, TextRange(start + s.length))
    }

    fun deleteBeforeCursor() {
        val cursor = text.selection.start
        if (cursor > 0) {
            val newText = text.text.removeRange(cursor - 1, cursor)
            text = TextFieldValue(newText, TextRange(cursor - 1))
        }
    }

    fun insertAtResult(s: String) {
        // 用户输入「@」触发时替换掉光标前的 @（对应老代码 isFromAt 分支）
        val cursor = text.selection.start
        if (isFromAt && cursor > 0 && text.text.getOrNull(cursor - 1) == '@') {
            val newText = text.text.replaceRange(cursor - 1, cursor, s)
            text = TextFieldValue(newText, TextRange(cursor - 1 + s.length))
        } else {
            insertAtCursor(s)
        }
        isFromAt = false
    }

    // ---------- 选图（PickMultipleVisualMedia(9)，逻辑按 ReplyActivity.initPhotoPick 移植） ----------
    val pickMultipleMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(9)) { uris ->
            runCatching {
                uris.forEach { uri ->
                    if (attachments.size == 9) {
                        onToast("最多选择9张图片")
                        return@forEach
                    }
                    val result = getImageDimensionsAndMD5(context.contentResolver, uri)
                    val md5Byte = result.second
                    val width = result.first?.first ?: 0
                    val height = result.first?.second ?: 0
                    val mimeType = result.first?.third ?: ""
                    attachments.add(
                        ReplyAttachment(
                            uri = uri,
                            type = mimeType,
                            md5 = md5Byte,
                            width = width,
                            height = height,
                            model = OSSUploadPrepareModel(
                                name = "${UUID.randomUUID().toString().replace("-", "")}." +
                                    if (mimeType.startsWith("image/")) mimeType.substring(6) else mimeType,
                                resolution = "${width}x${height}",
                                md5 = md5Byte?.toHex() ?: "",
                            ),
                        )
                    )
                }
            }.onFailure {
                onToast("获取图片信息失败: ${it.message}")
            }
        }

    // ---------- LiveData 事件桥接 ----------
    val overEvent by viewModel.over.observeAsState()
    val toastEvent by viewModel.toastText.observeAsState()
    val uploadEvent by viewModel.uploadImage.observeAsState()
    val shareUrlEvent by viewModel.loadShareUrl.observeAsState()
    val captchaEvent by viewModel.createDialog.observeAsState()

    LaunchedEffect(overEvent) {
        overEvent?.getContentIfNotHandledOrReturnNull()?.let {
            posting = false
            onToast(if (type == "createFeed" || type == "rating") "发布成功" else "回复成功")
            onFinish()
        }
    }

    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let {
            posting = false
            onToast(it ?: "")
        }
    }

    LaunchedEffect(shareUrlEvent) {
        shareUrlEvent?.getContentIfNotHandledOrReturnNull()?.let {
            posting = false
            viewModel.replyAndFeedData["extra_title"] = it.title
            viewModel.replyAndFeedData["extra_url"] = it.url
            extraTitle = it.title
            extraUrl = it.url
        }
    }

    LaunchedEffect(captchaEvent) {
        captchaEvent?.getContentIfNotHandledOrReturnNull()?.let {
            posting = false
            captchaBitmap = it
            captchaCode = ""
        }
    }

    // 图片上传（逻辑按 ReplyActivity.initObserve 的 uploadImage 分支移植）
    LaunchedEffect(uploadEvent) {
        val responseData = uploadEvent?.getContentIfNotHandledOrReturnNull() ?: return@LaunchedEffect
        val uriList = attachments.map { it.uri }
        viewModel.replyAndFeedData["pic"] =
            responseData.fileInfo.joinToString(separator = ",") {
                responseData.uploadPrepareInfo.uploadImagePrefix + "/" + it.uploadFileName
            }
        runCatching {
            ossUpload(
                context,
                responseData,
                uriList,
                attachments.map { it.type },
                attachments.map { it.md5 },
                iOnSuccess = { index ->
                    if (index == uriList.lastIndex) {
                        if (type == "createFeed" || type == "rating") viewModel.onPostCreateFeed()
                        else viewModel.onPostReply()
                    }
                },
                iOnFailure = {
                    posting = false
                    onToast("图片上传失败")
                },
                closeDialog = {},
            )
        }.onFailure {
            posting = false
            onToast("图片上传失败: ${it.message}")
        }
    }

    // ---------- 发布（按 ReplyActivity.onClick(R.id.publish) 移植） ----------
    fun publish() {
        when (type) {
            "createFeed" -> {
                viewModel.replyAndFeedData["id"] = ""
                viewModel.replyAndFeedData["message"] = text.text
                viewModel.replyAndFeedData["type"] = "feed"
                // 酷安 16.2.2 起「仅自己可见」改为 publish_status：1=仅自己可见，0=公开
                viewModel.replyAndFeedData["status"] = "1"
                viewModel.replyAndFeedData["publish_status"] = if (replyAndForward) "1" else "0"
                targetType?.let {
                    if (it == "apk") viewModel.replyAndFeedData["type"] = "comment"
                    viewModel.replyAndFeedData["targetType"] = it
                }
                targetId?.let { viewModel.replyAndFeedData["targetId"] = it }
            }

            "rating" -> {
                // rating_score_1 为 0~10 总体分，v4_score_item_1..n 为各子项 0~5 分
                viewModel.replyAndFeedData.apply {
                    put("id", "")
                    put("message", text.text)
                    put("type", "rating")
                    put("status", "1")
                    put("publish_status", "0")
                    targetType?.let { put("targetType", it) }
                    targetId?.let { put("targetId", it) }
                    put("rating_score_1", ratingOverall.toString())
                    subRatings.forEachIndexed { index, star ->
                        put("v4_score_item_${index + 1}", star.toString())
                    }
                    put("comment_good", goodText)
                    put("comment_general", "")
                    put("comment_bad", badText)
                    put("buy_status", if (buyStatus) "1" else "0")
                }
            }

            else -> {
                viewModel.replyAndFeedData["message"] = text.text
                viewModel.replyAndFeedData["replyAndForward"] = if (replyAndForward) "1" else "0"
            }
        }
        posting = true
        if (attachments.isNotEmpty()) {
            viewModel.onPostOSSUploadPrepare(attachments.map { it.model })
        } else {
            if (type == "createFeed" || type == "rating") viewModel.onPostCreateFeed()
            else viewModel.onPostReply()
        }
    }

    // ---------- 页面 ----------
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // 上半区：回复列表（有数据时）/ 点击空白关闭（对应老布局的 out 视图）
            if (replyList.isNotEmpty()) {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(replyList, key = { it.id }) { reply ->
                        ReplyListItem(
                            reply = reply,
                            onClick = { onReplyItemClick(reply) },
                            onLikeClick = { onReplyItemLike(reply) },
                            onMoreClick = { onReplyItemMore(reply) },
                        )
                        HorizontalDivider(
                            color = MiuixTheme.colorScheme.dividerLine,
                            thickness = 0.5.dp,
                        )
                    }
                    item {
                        ReplyListFooter(
                            footerState = replyListFooter,
                            onRetry = onReplyListLoadMore,
                        )
                    }
                }
                LaunchedEffect(listState, replyList.size) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .collect { last ->
                            if (last != null && replyList.isNotEmpty() && last >= replyList.size - 1) {
                                onReplyListLoadMore()
                            }
                        }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable(onClick = onFinish),
                )
            }

            // 发表点评（type=rating）：评分面板
            if (type == "rating") {
                RatingInputPanel(
                    ratingTarget = ratingTarget.orEmpty(),
                    ratingItems = ratingItems.orEmpty(),
                    subRatings = subRatings,
                    ratingOverall = ratingOverall,
                    onRatingOverallChange = { ratingOverall = it },
                    onSubRatingChange = { index, star -> if (index in subRatings.indices) subRatings[index] = star },
                    goodText = goodText,
                    onGoodTextChange = { goodText = it },
                    badText = badText,
                    onBadTextChange = { badText = it },
                    buyStatus = buyStatus,
                    onBuyStatusChange = { buyStatus = it },
                )
            }

            // 发回复输入区
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 0.dp,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when (type) {
                            "createFeed" -> "发布动态"
                            "createArticle" -> "发布图文"
                            "rating" -> "发表点评"
                            else -> "回复"
                        },
                        style = MiuixTheme.textStyles.title3,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        text = "发布",
                        onClick = { if (canPublish && !posting) publish() },
                        enabled = canPublish && !posting,
                        colors = if (canPublish) ButtonDefaults.textButtonColors(
                            textColor = MiuixTheme.colorScheme.primary,
                        ) else ButtonDefaults.textButtonColors(
                            textColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        ),
                    )
                }

                TextField(
                    value = text,
                    onValueChange = { newValue ->
                        if (newValue.text.length <= 500) {
                            val typedAt = newValue.text.length == text.text.length + 1 &&
                                newValue.selection.start > 0 &&
                                newValue.text.getOrNull(newValue.selection.start - 1) == '@'
                            text = newValue
                            if (typedAt) {
                                isFromAt = true
                                showAtSheet = true
                            }
                        }
                    },
                    label = if (type != "createFeed" && !username.isNullOrEmpty()) "回复: $username"
                    else "说点什么…",
                    useLabelAsPlaceholder = true,
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Text(
                    text = "${text.text.length}/500",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 20.dp, top = 4.dp),
                )

                // 外链卡片（createFeed 添加网络链接后展示，点击移除）
                if (extraUrl.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable {
                                extraTitle = ""
                                extraUrl = ""
                                viewModel.replyAndFeedData["extra_title"] = ""
                                viewModel.replyAndFeedData["extra_url"] = ""
                            },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_link),
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.size(28.dp),
                            )
                            Column(modifier = Modifier.padding(start = 10.dp)) {
                                Text(
                                    text = extraTitle,
                                    style = MiuixTheme.textStyles.body2,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = extraUrl,
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                if (type != "rating") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp),
                    ) {
                        Checkbox(
                            state = if (replyAndForward) ToggleableState.On else ToggleableState.Off,
                            onClick = { replyAndForward = !replyAndForward },
                        )
                        Text(
                            text = if (type == "createFeed") "仅自己可见" else "回复并转发",
                            style = MiuixTheme.textStyles.body2,
                            modifier = Modifier.clickable { replyAndForward = !replyAndForward },
                        )
                    }
                }

                // 图片附件缩略图（点击移除）
                if (attachments.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    ) {
                        items(attachments) { attachment ->
                            val ratio =
                                if (attachment.height > 0) attachment.width.toFloat() / attachment.height else 1f
                            GlideUriImage(
                                uri = attachment.uri,
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .height(65.dp)
                                    .width(65.dp * ratio)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { attachments.remove(attachment) },
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MiuixTheme.colorScheme.dividerLine,
                    thickness = 0.5.dp,
                )

                // 工具栏：表情 / 图片 / @用户 / #话题# / 外链(仅 createFeed) / 键盘
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToolbarIconButton(
                        icon = R.drawable.ic_emoji,
                        contentDescription = "表情",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showEmojiSheet = !showEmojiSheet
                        },
                    )
                    ToolbarIconButton(
                        icon = R.drawable.ic_image,
                        contentDescription = "图片",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            pickMultipleMedia.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    )
                    ToolbarIconButton(
                        icon = R.drawable.ic_at,
                        contentDescription = "@用户",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            isFromAt = false
                            showAtSheet = true
                        },
                    )
                    ToolbarIconButton(
                        icon = R.drawable.outline_tag_24,
                        contentDescription = "#话题#",
                        modifier = Modifier.weight(1f),
                        onClick = { showTopicSheet = true },
                    )
                    if (type == "createFeed") {
                        ToolbarIconButton(
                            icon = R.drawable.outline_add_circle_outline_24,
                            contentDescription = "添加网络链接",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                // 外链录入：OverlayDialog 输入网址后走 loadShareUrl
                                showUrlDialog = true
                            },
                        )
                    }
                    ToolbarIconButton(
                        icon = R.drawable.outline_keyboard_show_24,
                        contentDescription = "收起表情面板",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (showEmojiSheet) showEmojiSheet = false
                        },
                    )
                }
            }
        }

        // ---------- 弹层（Overlay* 需要 Scaffold 祖先） ----------
        EmojiPickerSheet(
            show = showEmojiSheet,
            onDismissRequest = { showEmojiSheet = false },
            onEmojiClick = { code -> insertAtCursor(code) },
            onBackspace = { deleteBeforeCursor() },
            viewModel = viewModel,
        )

        AtUserSearchSheet(
            show = showAtSheet,
            onDismissRequest = { showAtSheet = false },
            type = "user",
            onResult = { result -> insertAtResult(result) },
            onToast = onToast,
        )

        AtUserSearchSheet(
            show = showTopicSheet,
            onDismissRequest = { showTopicSheet = false },
            type = "topic",
            onResult = { result -> insertAtCursor(result) },
            onToast = onToast,
        )

        // 添加网络链接（createFeed）
        UrlInputDialog(
            show = showUrlDialog,
            onDismissRequest = { showUrlDialog = false },
            onConfirm = { url ->
                showUrlDialog = false
                if (url.isNotBlank()) {
                    posting = true
                    viewModel.loadShareUrl(url.trim())
                }
            },
        )

        // 图形验证码（err_request_captcha）
        if (captchaBitmap != null) {
            OverlayDialog(
                show = true,
                title = "captcha",
                onDismissRequest = { captchaBitmap = null },
            ) {
                Column {
                    CaptchaImage(
                        bitmap = captchaBitmap,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                    )
                    TextField(
                        value = captchaCode,
                        onValueChange = { captchaCode = it },
                        label = "验证码",
                        useLabelAsPlaceholder = true,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                    Row(modifier = Modifier.padding(top = 12.dp)) {
                        TextButton(
                            text = "取消",
                            onClick = { captchaBitmap = null },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        TextButton(
                            text = "验证并继续",
                            onClick = {
                                viewModel.requestValidateData = HashMap<String, String?>().apply {
                                    put("type", "err_request_captcha")
                                    put("code", captchaCode)
                                    put("mobile", "")
                                    put("idcard", "")
                                    put("name", "")
                                }
                                captchaBitmap = null
                                posting = true
                                viewModel.onPostRequestValidate()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColors(
                                textColor = MiuixTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }
        }

        // 发送中遮罩（替代老的 dialog_refresh 进度对话框）
        if (posting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = false, onClick = {}),
                contentAlignment = Alignment.Center,
            ) {
                Card {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "发送中…",
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 工具栏图标按钮（对应老布局底部一排 ImageView 按钮） */
@Composable
private fun ToolbarIconButton(
    icon: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** 简易星级打分（0..max）：Miuix 无星评组件，用基础组件拼（自定义 wrapper，应用层持有） */
@Composable
private fun StarRatingBar(
    rating: Int,
    max: Int,
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        for (i in 1..max) {
            Text(
                text = "★",
                style = MiuixTheme.textStyles.main,
                color = if (i <= rating) MiuixTheme.colorScheme.primary
                else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier
                    .clickable { onRate(i) }
                    .padding(horizontal = 2.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * 发表点评（type=rating）评分面板：总体评分 + 各子项 + 优点/不足 + 已购机。
 * 对应 activity_reply.xml 的 ratingLayout。
 */
@Composable
private fun RatingInputPanel(
    ratingTarget: String,
    ratingItems: List<HomeFeedResponse.RatingItem>,
    subRatings: List<Int>,
    ratingOverall: Int,
    onRatingOverallChange: (Int) -> Unit,
    onSubRatingChange: (Int, Int) -> Unit,
    goodText: String,
    onGoodTextChange: (String) -> Unit,
    badText: String,
    onBadTextChange: (String) -> Unit,
    buyStatus: Boolean,
    onBuyStatusChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = ratingTarget,
            style = MiuixTheme.textStyles.title3,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "总体评分",
            style = MiuixTheme.textStyles.body1,
            modifier = Modifier.padding(top = 12.dp),
        )
        StarRatingBar(
            rating = ratingOverall,
            max = 10,
            onRate = onRatingOverallChange,
            modifier = Modifier.padding(top = 4.dp),
        )
        ratingItems.forEachIndexed { index, item ->
            Text(
                text = item.name.orEmpty(),
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.padding(top = 8.dp),
            )
            val star = subRatings.getOrElse(index) { 0 }
            StarRatingBar(
                rating = star,
                max = 5,
                onRate = { onSubRatingChange(index, it) },
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = if (star > 0) item.starDesc?.getOrNull(star - 1)?.let { "${item.name}：$it" }.orEmpty()
                else "",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        TextField(
            value = goodText,
            onValueChange = onGoodTextChange,
            label = "优点（选填）",
            useLabelAsPlaceholder = true,
            minLines = 1,
            maxLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
        TextField(
            value = badText,
            onValueChange = onBadTextChange,
            label = "不足（选填）",
            useLabelAsPlaceholder = true,
            minLines = 1,
            maxLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Checkbox(
                state = if (buyStatus) ToggleableState.On else ToggleableState.Off,
                onClick = { onBuyStatusChange(!buyStatus) },
            )
            Text(
                text = "已购买此机型",
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.clickable { onBuyStatusChange(!buyStatus) },
            )
        }
    }
}

/** 验证码图片（Bitmap → AndroidView） */
@Composable
private fun CaptchaImage(
    bitmap: android.graphics.Bitmap?,
    modifier: Modifier = Modifier,
) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { context -> android.widget.ImageView(context) },
        update = { imageView -> imageView.setImageBitmap(bitmap) },
        modifier = modifier,
    )
}

/** 添加网络链接输入对话框（createFeed，对应老代码 urlBtn 的 MaterialAlertDialog） */
@Composable
private fun UrlInputDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var url by remember(show) { mutableStateOf("") }
    OverlayDialog(
        show = show,
        title = "添加网络链接",
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            TextField(
                value = url,
                onValueChange = { url = it },
                label = "https://",
                useLabelAsPlaceholder = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.padding(top = 12.dp)) {
                TextButton(
                    text = "取消",
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(20.dp))
                TextButton(
                    text = "确定",
                    onClick = { onConfirm(url) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(
                        textColor = MiuixTheme.colorScheme.primary,
                    ),
                )
            }
        }
    }
}
