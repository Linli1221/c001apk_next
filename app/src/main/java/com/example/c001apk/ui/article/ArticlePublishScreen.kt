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
import androidx.compose.runtime.livedata.observeAsState
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
 * 鍙戝竷鍥炬枃鐨勭晫闈㈢紪杈戠姸鎬侊紙绾?UI 缂栬緫鎬侊紝涓氬姟/缃戠粶鐘舵€佸湪 [ArticlePublishViewModel]锛夈€? * 瀵瑰簲鑰?ArticlePublishActivity 閲岀殑 blocks / coverUri / coverMd5 绛夊瓧娈点€? */
@Stable
class ArticlePublishState {
    var title by mutableStateOf("")
    var privateOnly by mutableStateOf(false)
    var isPublishing by mutableStateOf(false)

    /** 姝ｆ枃鍧楋紙鎸夐『搴忓嵆姝ｆ枃娣锋帓缁撴瀯锛?*/
    val blocks = mutableStateListOf<ArticleBlock>()

    /** 灏侀潰锛堝浐瀹?1600:719 瑁佸壀浜х墿锛?*/
    var coverUri by mutableStateOf<Uri?>(null)
    var coverName by mutableStateOf("")
    var coverMd5 by mutableStateOf("")
    var coverMd5Byte by mutableStateOf<ByteArray?>(null)
}

@Composable
fun rememberArticlePublishState(): ArticlePublishState = remember { ArticlePublishState() }

/**
 * 鍙戝竷鍥炬枃锛堥叿瀹夈€屽浘鏂囥€? type feed + is_html_article=1锛夈€? * 缁撴瀯锛氭爣棰?+ 灏侀潰锛堝浐瀹?1600:719 瑁佸壀锛? 姝ｆ枃锛堟枃鏈?鍥剧墖鍧楁贩鎺掞紝鍥剧墖鍙姞璇存槑锛? 鏌ョ湅鏉冮檺銆? * 瀵瑰簲鑰?ArticlePublishActivity + activity_article_publish.xml銆? *
 * 鐘舵€佷笌浜や簰鍏ㄩ儴鑷惈锛? * - 灏侀潰锛歅ickVisualMedia 閫夊浘 鈫?CropImageActivity 瑁佸壀锛圧ESULT_URI 鍥炰紶锛夛紱
 * - 姝ｆ枃鍥撅細PickMultipleVisualMedia 澶氶€夛紙姣忔鏈€澶?9 寮狅紝绱鏈€澶?20 寮狅級锛? * - 鍙戝竷锛氭牎楠?鈫?缁勮 uploadFileList / feedData 鈫?[ArticlePublishViewModel.onPostOSSUploadPrepare]
 *   鈫?uploadImage 浜嬩欢閲岀粍 message JSON 骞惰蛋 ossUpload 鈫?onPostCreateFeed锛屼笌鑰?Activity 娴佺▼涓€鑷达紱
 * - LiveData锛坱oastText / over / uploadImage锛夌敤 observeAsState 妗ユ帴锛圗vent 鍗曟娑堣垂璇箟鐢? *   LaunchedEffect(key = event) 淇濊瘉锛夈€? *
 * 瀹夸富鍙帴涓や釜鍥炶皟锛歔onBack]銆乕onPublishSuccess]锛堝彂甯冩垚鍔熷悗 finish锛夈€? */
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

    // 灏侀潰瑁佸壀锛堝钩鍙拌鍓?UI锛欳ropImageActivity锛屾墜鍔?Matrix 瑁佸壀淇濇寔鐜扮姸锛屼笉缁?Compose锛?    val cropLauncher =
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
                        currentToast("姝ｆ枃鏈€澶氭彃鍏?0寮犲浘鐗?)
                        break
                    }
                    val block = withContext(Dispatchers.IO) {
                        createImageBlock(context.contentResolver, uri)
                    }
                    state.blocks.add(block)
                }
            }
        }

    // LiveData 浜嬩欢妗ユ帴锛坥bserveAsState + LaunchedEffect锛屽崟娆℃秷璐癸級
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
            currentToast("鍙戝竷鎴愬姛")
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
                title = "鍙戝竷鍥炬枃",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "杩斿洖")
                    }
                },
                actions = {
                    TextButton(
                        text = "鍙戝竷",
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
                    text = "浠呰嚜宸卞彲瑙?,
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
                // 鏍囬锛堣€佺晫闈㈤檺 50 瀛楋紝TextInputLayout 甯﹁鏁板櫒锛?                TextField(
                    value = state.title,
                    onValueChange = { if (it.length <= TITLE_MAX_LENGTH) state.title = it },
                    label = "鏍囬",
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

                // 灏侀潰
                Text(
                    text = "灏侀潰",
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
                            text = "鐐瑰嚮閫夋嫨灏侀潰鍥?,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
                Text(
                    text = "灏侀潰姣斾緥鍥哄畾涓?1600:719锛岄€夊浘鍚庨渶瑁佸壀",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 4.dp),
                )

                // 姝ｆ枃鍧?                Text(
                    text = "姝ｆ枃",
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

                // 娣诲姞鎸夐挳
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = "鎻掑叆鍥剧墖",
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
                        text = "娣诲姞鏂囧瓧",
                        onClick = { state.blocks.add(ArticleBlock.Text()) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // 鍙戝竷涓細闈為樆鏂姞杞藉眰锛堟浛浠ｈ€?dialog_refresh 瀵硅瘽妗嗭級
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
                            text = "鍙戝竷涓€?,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

/** 姝ｆ枃鏂囧瓧鍧楋紙鑰?item_article_text_block.xml锛?*/
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
        BlockHeader(label = "娈佃惤", onDelete = onDelete)
        TextField(
            value = block.text,
            onValueChange = onTextChange,
            label = "杈撳叆鏂囧瓧",
            useLabelAsPlaceholder = true,
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}

/** 姝ｆ枃鍥剧墖鍧楋紙鑰?item_article_image_block.xml锛夛細鍥?+ 鍙€夎鏄?+ 鍒犻櫎 */
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
        BlockHeader(label = "鍥剧墖", onDelete = onDelete)
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
            label = "娣诲姞鍥剧墖璇存槑锛堝彲閫夛級",
            useLabelAsPlaceholder = true,
            singleLine = true,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}

/** 鍧楀ご锛氥€屾钀?/ 鍥剧墖銆? 鍒犻櫎鎸夐挳 */
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
                contentDescription = "鍒犻櫎$label",
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    }
}

/** 閫夊浘 鈫?缁勮姝ｆ枃鍥剧墖鍧楋紙鍚昂瀵?mime/md5锛屼笌鑰?addImageBlock 涓€鑷达級 */
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
 * 鍙戝竷鏍￠獙 + 缁勮 uploadFileList / feedData锛岃Е鍙?ossUploadPrepare锛堜笌鑰?publish() 涓€鑷达級銆? * 鏍￠獙澶辫触鏃堕€氳繃 [toast] 鎻愮ず骞惰繑鍥炪€? */
private fun startPublish(
    state: ArticlePublishState,
    viewModel: ArticlePublishViewModel,
    toast: (String) -> Unit,
) {
    val title = state.title.trim()
    if (title.isEmpty()) {
        toast("璇疯緭鍏ユ爣棰?)
        return
    }
    if (state.coverUri == null) {
        toast("璇烽€夋嫨灏侀潰鍥?)
        return
    }
    val hasContent = state.blocks.any {
        when (it) {
            is ArticleBlock.Text -> it.text.isNotBlank()
            is ArticleBlock.Image -> true
        }
    }
    if (!hasContent) {
        toast("璇疯緭鍏ユ鏂?)
        return
    }

    // uploadFileList锛氬皝闈?+ 姝ｆ枃鍥?    val uploadFiles = ArrayList<ArticleUploadFile>()
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
 * ossUploadPrepare 鎴愬姛鍚庯細缁?message JSON锛坱ext/image 鍧椾氦閿欙級銆? * 鎸?uploadFileList 椤哄簭涓婁紶锛堝皝闈?+ 姝ｆ枃鍥撅級锛屽叏閮ㄦ垚鍔熷悗 createFeed
 * 锛堜笌鑰?ArticlePublishActivity 鐨?uploadImage 浜嬩欢澶勭悊涓€鑷达級銆? */
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
        toast("璇烽€夋嫨灏侀潰鍥?)
        return
    }
    val prefix = responseData.uploadPrepareInfo.uploadImagePrefix
    val fileInfo = responseData.fileInfo

    // message JSON 鏁扮粍锛歵ext/image 鍧椾氦閿欙紱fileInfo[0]=灏侀潰锛宖ileInfo[1..]=姝ｆ枃鍥?    var bodyIdx = 0
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

    // 涓婁紶鍒楄〃涓?uploadFileList 椤哄簭涓€鑷达細灏侀潰 + 姝ｆ枃鍥?    val imageBlocks = state.blocks.filterIsInstance<ArticleBlock.Image>()
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
                toast("鍥剧墖涓婁紶澶辫触")
            },
            closeDialog = { state.isPublishing = false },
        )
    }
}
