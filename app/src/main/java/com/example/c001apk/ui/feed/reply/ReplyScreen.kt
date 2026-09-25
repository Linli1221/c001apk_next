package com.example.c001apk.ui.feed.reply

import android.net.Uri
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
import androidx.compose.runtime.livedata.observeAsState
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
 * 鍥炲椤碉紙Compose 鐗?ReplyActivity + activity_reply.xml锛夛細
 * 鍥炲鍒楄〃锛堝彲閫夛紝鏁版嵁鐢卞涓讳紶鍏ワ級 + 鍙戝洖澶嶈緭鍏ュ尯锛堟鏂?/ 琛ㄦ儏 / 鍥剧墖 / @鐢ㄦ埛 / #璇濋# / 澶栭摼 / 鍙戝竷锛夈€? *
 * 鏁版嵁娴佸畬鍏ㄥ鐢?[ReplyViewModel]锛圠iveData 鐢?observeAsState 妗ユ帴锛夛紝鏈枃浠跺彧璐熻矗鐣岄潰涓庝氦浜掋€? * 鍙戝竷鏃剁殑 replyAndFeedData 缁勮閫昏緫鎸?ReplyActivity.onClick(R.id.publish) 鍘熸牱绉绘銆? */

/** 閫変腑鐨勫浘鐗囬檮浠讹紙Glide 缂╃暐鍥?+ OSS 涓婁紶鍑嗗淇℃伅锛?*/
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

    // 涓?ReplyActivity.onCreate 涓€鑷达細鍏堟妸 type/rid 浜ょ粰 ViewModel
    LaunchedEffect(Unit) {
        viewModel.type = type
        viewModel.rid = rid
    }

    // ---------- 鐘舵€?----------
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

    // ---------- 鍏夋爣澶勬彃鍏?/ 鍒犻櫎锛堣〃鎯呫€丂銆?璇濋#锛?----------
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
        // 鐢ㄦ埛杈撳叆銆孈銆嶈Е鍙戞椂鏇挎崲鎺夊厜鏍囧墠鐨?@锛堝搴旇€佷唬鐮?isFromAt 鍒嗘敮锛?        val cursor = text.selection.start
        if (isFromAt && cursor > 0 && text.text.getOrNull(cursor - 1) == '@') {
            val newText = text.text.replaceRange(cursor - 1, cursor, s)
            text = TextFieldValue(newText, TextRange(cursor - 1 + s.length))
        } else {
            insertAtCursor(s)
        }
        isFromAt = false
    }

    // ---------- 閫夊浘锛圥ickMultipleVisualMedia(9)锛岄€昏緫鎸?ReplyActivity.initPhotoPick 绉绘锛?----------
    val pickMultipleMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(9)) { uris ->
            runCatching {
                uris.forEach { uri ->
                    if (attachments.size == 9) {
                        onToast("鏈€澶氶€夋嫨9寮犲浘鐗?)
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
                onToast("鑾峰彇鍥剧墖淇℃伅澶辫触: ${it.message}")
            }
        }

    // ---------- LiveData 浜嬩欢妗ユ帴 ----------
    val overEvent by viewModel.over.observeAsState()
    val toastEvent by viewModel.toastText.observeAsState()
    val uploadEvent by viewModel.uploadImage.observeAsState()
    val shareUrlEvent by viewModel.loadShareUrl.observeAsState()
    val captchaEvent by viewModel.createDialog.observeAsState()

    LaunchedEffect(overEvent) {
        overEvent?.getContentIfNotHandledOrReturnNull()?.let {
            posting = false
            onToast(if (type == "createFeed" || type == "rating") "鍙戝竷鎴愬姛" else "鍥炲鎴愬姛")
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

    // 鍥剧墖涓婁紶锛堥€昏緫鎸?ReplyActivity.initObserve 鐨?uploadImage 鍒嗘敮绉绘锛?    LaunchedEffect(uploadEvent) {
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
                    onToast("鍥剧墖涓婁紶澶辫触")
                },
                closeDialog = {},
            )
        }.onFailure {
            posting = false
            onToast("鍥剧墖涓婁紶澶辫触: ${it.message}")
        }
    }

    // ---------- 鍙戝竷锛堟寜 ReplyActivity.onClick(R.id.publish) 绉绘锛?----------
    fun publish() {
        when (type) {
            "createFeed" -> {
                viewModel.replyAndFeedData["id"] = ""
                viewModel.replyAndFeedData["message"] = text.text
                viewModel.replyAndFeedData["type"] = "feed"
                // 閰峰畨 16.2.2 璧枫€屼粎鑷繁鍙銆嶆敼涓?publish_status锛?=浠呰嚜宸卞彲瑙侊紝0=鍏紑
                viewModel.replyAndFeedData["status"] = "1"
                viewModel.replyAndFeedData["publish_status"] = if (replyAndForward) "1" else "0"
                targetType?.let {
                    if (it == "apk") viewModel.replyAndFeedData["type"] = "comment"
                    viewModel.replyAndFeedData["targetType"] = it
                }
                targetId?.let { viewModel.replyAndFeedData["targetId"] = it }
            }

            "rating" -> {
                // rating_score_1 涓?0~10 鎬讳綋鍒嗭紝v4_score_item_1..n 涓哄悇瀛愰」 0~5 鍒?                viewModel.replyAndFeedData.apply {
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

    // ---------- 椤甸潰 ----------
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // 涓婂崐鍖猴細鍥炲鍒楄〃锛堟湁鏁版嵁鏃讹級/ 鐐瑰嚮绌虹櫧鍏抽棴锛堝搴旇€佸竷灞€鐨?out 瑙嗗浘锛?            if (replyList.isNotEmpty()) {
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

            // 鍙戣〃鐐硅瘎锛坱ype=rating锛夛細璇勫垎闈㈡澘
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

            // 鍙戝洖澶嶈緭鍏ュ尯
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 0.dp,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when (type) {
                            "createFeed" -> "鍙戝竷鍔ㄦ€?
                            "createArticle" -> "鍙戝竷鍥炬枃"
                            "rating" -> "鍙戣〃鐐硅瘎"
                            else -> "鍥炲"
                        },
                        style = MiuixTheme.textStyles.title3,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        text = "鍙戝竷",
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
                    label = if (type != "createFeed" && !username.isNullOrEmpty()) "鍥炲: $username"
                    else "璇寸偣浠€涔堚€?,
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

                // 澶栭摼鍗＄墖锛坈reateFeed 娣诲姞缃戠粶閾炬帴鍚庡睍绀猴紝鐐瑰嚮绉婚櫎锛?                if (extraUrl.isNotEmpty()) {
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
                            text = if (type == "createFeed") "浠呰嚜宸卞彲瑙? else "鍥炲骞惰浆鍙?,
                            style = MiuixTheme.textStyles.body2,
                            modifier = Modifier.clickable { replyAndForward = !replyAndForward },
                        )
                    }
                }

                // 鍥剧墖闄勪欢缂╃暐鍥撅紙鐐瑰嚮绉婚櫎锛?                if (attachments.isNotEmpty()) {
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

                // 宸ュ叿鏍忥細琛ㄦ儏 / 鍥剧墖 / @鐢ㄦ埛 / #璇濋# / 澶栭摼(浠?createFeed) / 閿洏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToolbarIconButton(
                        icon = R.drawable.ic_emoji,
                        contentDescription = "琛ㄦ儏",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showEmojiSheet = !showEmojiSheet
                        },
                    )
                    ToolbarIconButton(
                        icon = R.drawable.ic_image,
                        contentDescription = "鍥剧墖",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            pickMultipleMedia.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    )
                    ToolbarIconButton(
                        icon = R.drawable.ic_at,
                        contentDescription = "@鐢ㄦ埛",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            isFromAt = false
                            showAtSheet = true
                        },
                    )
                    ToolbarIconButton(
                        icon = R.drawable.outline_tag_24,
                        contentDescription = "#璇濋#",
                        modifier = Modifier.weight(1f),
                        onClick = { showTopicSheet = true },
                    )
                    if (type == "createFeed") {
                        ToolbarIconButton(
                            icon = R.drawable.outline_add_circle_outline_24,
                            contentDescription = "娣诲姞缃戠粶閾炬帴",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                // 澶栭摼褰曞叆锛歄verlayDialog 杈撳叆缃戝潃鍚庤蛋 loadShareUrl
                                showUrlDialog = true
                            },
                        )
                    }
                    ToolbarIconButton(
                        icon = R.drawable.outline_keyboard_show_24,
                        contentDescription = "鏀惰捣琛ㄦ儏闈㈡澘",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (showEmojiSheet) showEmojiSheet = false
                        },
                    )
                }
            }
        }

        // ---------- 寮瑰眰锛圤verlay* 闇€瑕?Scaffold 绁栧厛锛?----------
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

        // 娣诲姞缃戠粶閾炬帴锛坈reateFeed锛?        UrlInputDialog(
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

        // 鍥惧舰楠岃瘉鐮侊紙err_request_captcha锛?        if (captchaBitmap != null) {
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
                        label = "楠岃瘉鐮?,
                        useLabelAsPlaceholder = true,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                    Row(modifier = Modifier.padding(top = 12.dp)) {
                        TextButton(
                            text = "鍙栨秷",
                            onClick = { captchaBitmap = null },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        TextButton(
                            text = "楠岃瘉骞剁户缁?,
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

        // 鍙戦€佷腑閬僵锛堟浛浠ｈ€佺殑 dialog_refresh 杩涘害瀵硅瘽妗嗭級
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
                            text = "鍙戦€佷腑鈥?,
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

/** 宸ュ叿鏍忓浘鏍囨寜閽紙瀵瑰簲鑰佸竷灞€搴曢儴涓€鎺?ImageView 鎸夐挳锛?*/
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

/** 绠€鏄撴槦绾ф墦鍒嗭紙0..max锛夛細Miuix 鏃犳槦璇勭粍浠讹紝鐢ㄥ熀纭€缁勪欢鎷硷紙鑷畾涔?wrapper锛屽簲鐢ㄥ眰鎸佹湁锛?*/
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
                text = "鈽?,
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
 * 鍙戣〃鐐硅瘎锛坱ype=rating锛夎瘎鍒嗛潰鏉匡細鎬讳綋璇勫垎 + 鍚勫瓙椤?+ 浼樼偣/涓嶈冻 + 宸茶喘鏈恒€? * 瀵瑰簲 activity_reply.xml 鐨?ratingLayout銆? */
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
            text = "鎬讳綋璇勫垎",
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
                text = if (star > 0) item.starDesc?.getOrNull(star - 1)?.let { "${item.name}锛?it" }.orEmpty()
                else "",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        TextField(
            value = goodText,
            onValueChange = onGoodTextChange,
            label = "浼樼偣锛堥€夊～锛?,
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
            label = "涓嶈冻锛堥€夊～锛?,
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
                text = "宸茶喘涔版鏈哄瀷",
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.clickable { onBuyStatusChange(!buyStatus) },
            )
        }
    }
}

/** 楠岃瘉鐮佸浘鐗囷紙Bitmap 鈫?AndroidView锛?*/
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

/** 娣诲姞缃戠粶閾炬帴杈撳叆瀵硅瘽妗嗭紙createFeed锛屽搴旇€佷唬鐮?urlBtn 鐨?MaterialAlertDialog锛?*/
@Composable
private fun UrlInputDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var url by remember(show) { mutableStateOf("") }
    OverlayDialog(
        show = show,
        title = "娣诲姞缃戠粶閾炬帴",
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
                    text = "鍙栨秷",
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(20.dp))
                TextButton(
                    text = "纭畾",
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
