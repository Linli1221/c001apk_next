package com.example.c001apk.ui.article

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.observeAsState
import com.example.c001apk.logic.model.OSSUploadPrepareResponse
import com.example.c001apk.util.ImageUtil.getImageDimensionsAndMD5
import com.example.c001apk.util.ImageUtil.toHex
import com.example.c001apk.util.makeToast
import com.example.c001apk.util.ossUpload
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.UUID

private const val TITLE_MAX_LENGTH = 50
private const val DESCRIPTION_MAX_LENGTH = 200
private const val MAX_BODY_IMAGE = 20

/**
 * 发布图文的界面编辑状态（纯 UI 编辑态，业务/网络状态在 [ArticlePublishViewModel]）。
 * 对应老 ArticlePublishActivity 里的 blocks / coverUri / coverMd5 等字段。
 */
@Stable
class ArticlePublishState {
    var title by mutableStateOf("")
    var privateOnly by mutableStateOf(false)
    var isPublishing by mutableStateOf(false)

    /** 正文块（按顺序即正文混排结构） */
    val blocks = mutableStateListOf<ArticleBlock>()

    /** 封面（固定 1600:719 裁剪产物） */
    var coverUri by mutableStateOf<Uri?>(null)
    var coverName by mutableStateOf("")
    var coverMd5 by mutableStateOf("")
    var coverMd5Byte by mutableStateOf<ByteArray?>(null)
}

@Composable
fun rememberArticlePublishState(): ArticlePublishState = remember { ArticlePublishState() }

/**
 * 发布图文（酷安「图文」= type feed + is_html_article=1）。
 * 结构：标题 + 封面（固定 1600:719 裁剪）+ 正文（文本/图片块混排，图片可加说明）+ 查看权限。
 * 对应老 ArticlePublishActivity + activity_article_publish.xml。
 *
 * 状态与交互全部自含：
 * - 封面：PickVisualMedia 选图 → CropImageActivity 裁剪（RESULT_URI 回传）；
 * - 正文图：PickMultipleVisualMedia 多选（每次最多 9 张，累计最多 20 张）；
 * - 发布：校验 → 组装 uploadFileList / feedData → [ArticlePublishViewModel.onPostOSSUploadPrepare]
 *   → uploadImage 事件里组 message JSON 并走 ossUpload → onPostCreateFeed，与老 Activity 流程一致；
 * - LiveData（toastText / over / uploadImage）用 observeAsState 桥接（Event 单次消费语义由
 *   LaunchedEffect(key = event) 保证）。
 *
 * 宿主只接两个回调：[onBack]、[onPublishSuccess]（发布成功后 finish）。
 */
@Composable
fun ArticlePublishScreen(
    viewModel: ArticlePublishViewModel,
    onBack: () -> Unit,
    onPublishSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    state: ArticlePublishState = rememberArticlePublishState(),
    onToast: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = onToast ?: { context.makeToast(it) }
    val currentToast by rememberUpdatedState(toast)
    val currentOnPublishSuccess by rememberUpdatedState(onPublishSuccess)

    // 封面裁剪（平台裁剪 UI：CropImageActivity，手势/Matrix 裁剪保持现状，不经 Compose）
    val cropLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.getStringExtra(CropImageActivity.RESULT_URI)
                    ?.let { Uri.parse(it) }
                if (uri != null) {
                    state.coverUri = uri
                    state.coverName = "${UUID.randomUUID().toString().replace("-", "")}.jpg"
                    scope.launch(Dispatchers.IO) {
                        val res = getImageDimensionsAndMD5(context.contentResolver, uri)
                        state.coverMd5Byte = res.second
                        state.coverMd5 = res.second?.toHex() ?: ""
                    }
                }
            }
        }
    val pickCoverLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                cropLauncher.launch(
                    Intent(context, CropImageActivity::class.java)
                        .putExtra(CropImageActivity.EXTRA_URI, uri.toString())
                )
            }
        }
    val pickBodyLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(9)
        ) { uris ->
            scope.launch {
                for (uri in uris) {
                    if (state.blocks.count { it is ArticleBlock.Image } >= MAX_BODY_IMAGE) {
                        currentToast("正文最多插入20张图片")
                        break
                    }
                    val block = withContext(Dispatchers.IO) {
                        createImageBlock(context.contentResolver, uri)
                    }
                    state.blocks.add(block)
                }
            }
        }

    // LiveData 事件桥接（observeAsState + LaunchedEffect，单次消费）
    val toastEvent by viewModel.toastText.observeAsState()
    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let {
            state.isPublishing = false
            currentToast(it)
        }
    }
    val overEvent by viewModel.over.observeAsState()
    LaunchedEffect(overEvent) {
        overEvent?.getContentIfNotHandledOrReturnNull()?.let {
            state.isPublishing = false
            currentToast("发布成功")
            currentOnPublishSuccess()
        }
    }
    val uploadEvent by viewModel.uploadImage.observeAsState()
    LaunchedEffect(uploadEvent) {
        uploadEvent?.getContentIfNotHandledOrReturnNull()?.let { responseData ->
            launchOssUpload(context, scope, state, viewModel, responseData, currentToast)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            SmallTopAppBar(
                title = "发布图文",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        text = "发布",
                        onClick = {
                            startPublish(state, viewModel) { currentToast(it) }
                        },
                        enabled = !state.isPublishing,
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                },
            )
        },
        bottomBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MiuixTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Checkbox(
                    state = if (state.privateOnly) ToggleableState.On else ToggleableState.Off,
                    onClick = { state.privateOnly = !state.privateOnly },
                )
                Text(
                    text = "仅自己可见",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clickable { state.privateOnly = !state.privateOnly }
                        .padding(start = 8.dp),
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                // 标题（老界面限 50 字，TextInputLayout 带计数器）
                TextField(
                    value = state.title,
                    onValueChange = { if (it.length <= TITLE_MAX_LENGTH) state.title = it },
                    label = "标题",
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${state.title.length}/$TITLE_MAX_LENGTH",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }

                // 封面
                Text(
                    text = "封面",
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MiuixTheme.colorScheme.secondaryContainer)
                        .clickable {
                            pickCoverLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                ) {
                    val cover = state.coverUri
                    if (cover != null) {
                        GlideImage(
                            url = cover,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = "点击选择封面图",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
                Text(
                    text = "封面比例固定为 1600:719，选图后需裁剪",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 4.dp),
                )

                // 正文块
                Text(
                    text = "正文",
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp),
                )
                state.blocks.forEachIndexed { index, block ->
                    when (block) {
                        is ArticleBlock.Text -> TextBlockItem(
                            block = block,
                            onTextChange = { block.text = it },
                            onDelete = { state.blocks.removeAt(index) },
                        )

                        is ArticleBlock.Image -> ImageBlockItem(
                            block = block,
                            onDescriptionChange = { block.description = it },
                            onDelete = { state.blocks.removeAt(index) },
                        )
                    }
                }

                // 添加按钮
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = "插入图片",
                        onClick = {
                            pickBodyLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = "添加文字",
                        onClick = { state.blocks.add(ArticleBlock.Text()) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // 发布中：非阻断加载层（替代老 dialog_refresh 对话框）
            if (state.isPublishing) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MiuixTheme.colorScheme.windowDimming.copy(alpha = 0.4f)),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "发布中…",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

/** 正文文字块（老 item_article_text_block.xml） */
@Composable
private fun TextBlockItem(
    block: ArticleBlock.Text,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        BlockHeader(label = "段落", onDelete = onDelete)
        TextField(
            value = block.text,
            onValueChange = onTextChange,
            label = "输入文字",
            useLabelAsPlaceholder = true,
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}

/** 正文图片块（老 item_article_image_block.xml）：图 + 可选说明 + 删除 */
@Composable
private fun ImageBlockItem(
    block: ArticleBlock.Image,
    onDescriptionChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        BlockHeader(label = "图片", onDelete = onDelete)
        GlideImage(
            url = block.uri,
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        TextField(
            value = block.description,
            onValueChange = { if (it.length <= DESCRIPTION_MAX_LENGTH) onDescriptionChange(it) },
            label = "添加图片说明（可选）",
            useLabelAsPlaceholder = true,
            singleLine = true,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}

/** 块头：「段落 / 图片」+ 删除按钮 */
@Composable
private fun BlockHeader(
    label: String,
    onDelete: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onDelete) {
            Icon(
                MiuixIcons.Close,
                contentDescription = "删除$label",
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    }
}

/** 选图 → 组装正文图片块（含尺寸/mime/md5，与老 addImageBlock 一致） */
private fun createImageBlock(contentResolver: ContentResolver, uri: Uri): ArticleBlock.Image {
    val res = getImageDimensionsAndMD5(contentResolver, uri)
    val md5Byte = res.second
    val width = res.first?.first ?: 0
    val height = res.first?.second ?: 0
    val type = res.first?.third ?: "image/jpeg"
    val ext = if (type.startsWith("image/")) type.substringAfterLast("/") else "jpg"
    return ArticleBlock.Image(
        uri = uri,
        name = "${UUID.randomUUID().toString().replace("-", "")}.$ext",
        resolution = "${width}x${height}",
        md5 = md5Byte?.toHex() ?: "",
        type = type,
        md5Byte = md5Byte,
    )
}

/**
 * 发布校验 + 组装 uploadFileList / feedData，触发 ossUploadPrepare（与老 publish() 一致）。
 * 校验失败时通过 [toast] 提示并返回。
 */
private fun startPublish(
    state: ArticlePublishState,
    viewModel: ArticlePublishViewModel,
    toast: (String) -> Unit,
) {
    val title = state.title.trim()
    if (title.isEmpty()) {
        toast("请输入标题")
        return
    }
    if (state.coverUri == null) {
        toast("请选择封面图")
        return
    }
    val hasContent = state.blocks.any {
        when (it) {
            is ArticleBlock.Text -> it.text.isNotBlank()
            is ArticleBlock.Image -> true
        }
    }
    if (!hasContent) {
        toast("请输入正文")
        return
    }

    // uploadFileList：封面 + 正文图
    val uploadFiles = ArrayList<ArticleUploadFile>()
    uploadFiles.add(ArticleUploadFile(state.coverName, "1600x719", state.coverMd5, 0))
    state.blocks.filterIsInstance<ArticleBlock.Image>().forEach {
        uploadFiles.add(ArticleUploadFile(it.name, it.resolution, it.md5, 0))
    }

    viewModel.feedData.apply {
        put("id", "")
        put("type", "feed")
        put("status", "1")
        put("publish_status", if (state.privateOnly) "1" else "0")
        put("message_title", title)
        put("is_html_article", "1")
        put("pic", "")
    }

    state.isPublishing = true
    viewModel.onPostOSSUploadPrepare(uploadFiles)
}

/**
 * ossUploadPrepare 成功后：组 message JSON（text/image 块交错）、
 * 按 uploadFileList 顺序上传（封面 + 正文图），全部成功后 createFeed
 * （与老 ArticlePublishActivity 的 uploadImage 事件处理一致）。
 */
private fun launchOssUpload(
    context: Context,
    scope: CoroutineScope,
    state: ArticlePublishState,
    viewModel: ArticlePublishViewModel,
    responseData: OSSUploadPrepareResponse.Data,
    toast: (String) -> Unit,
) {
    val coverUri = state.coverUri ?: run {
        state.isPublishing = false
        toast("请选择封面图")
        return
    }
    val prefix = responseData.uploadPrepareInfo.uploadImagePrefix
    val fileInfo = responseData.fileInfo

    // message JSON 数组：text/image 块交错；fileInfo[0]=封面，fileInfo[1..]=正文图
    var bodyIdx = 0
    val msgList = ArrayList<Map<String, String>>()
    state.blocks.forEach { block ->
        when (block) {
            is ArticleBlock.Text ->
                msgList.add(mapOf("type" to "text", "message" to block.text))

            is ArticleBlock.Image -> {
                val url = prefix + "/" + fileInfo[1 + bodyIdx].uploadFileName
                msgList.add(
                    mapOf(
                        "type" to "image",
                        "url" to url,
                        "description" to block.description
                    )
                )
                bodyIdx++
            }
        }
    }
    viewModel.feedData["message"] = Gson().toJson(msgList)
    viewModel.feedData["message_cover"] = prefix + "/" + fileInfo[0].uploadFileName

    // 上传列表与 uploadFileList 顺序一致：封面 + 正文图
    val imageBlocks = state.blocks.filterIsInstance<ArticleBlock.Image>()
    val uriList = ArrayList<Uri>().apply {
        add(coverUri)
        imageBlocks.forEach { add(it.uri) }
    }
    val typeList = ArrayList<String>().apply {
        add("image/jpeg")
        imageBlocks.forEach { add(it.type) }
    }
    val md5List = ArrayList<ByteArray?>().apply {
        add(state.coverMd5Byte)
        imageBlocks.forEach { add(it.md5Byte) }
    }

    scope.launch(Dispatchers.IO) {
        ossUpload(
            context, responseData, uriList, typeList, md5List,
            iOnSuccess = { index ->
                if (index == uriList.lastIndex) {
                    viewModel.onPostCreateFeed()
                }
            },
            iOnFailure = {
                state.isPublishing = false
                toast("图片上传失败")
            },
            closeDialog = { state.isPublishing = false },
        )
    }
}
