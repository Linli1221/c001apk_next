package com.example.c001apk.ui.feed

import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.livedata.observeAsState
import com.example.c001apk.R
import com.example.c001apk.logic.model.CollectionData
import com.example.c001apk.util.ImageUtil
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 鏀惰棌鍒版敹钘忓す寮瑰眰锛堝搴旇€佺殑 [CollectionPickBottomSheet]锛夛細
 * 鐐规潯鐩敹钘?/ 鍙栨秷鏀惰棌锛岄暱鎸夋敼鏀惰棌澶癸紙鏍囬 / 绠€浠?/ 鍏紑绉佸瘑 / 灏侀潰锛夛紝鍙充笂瑙掓柊寤恒€? *
 * - 寮瑰眰鐢?Miuix [OverlayBottomSheet]锛岀紪杈戣〃鍗曠敤 [OverlayDialog]锛堥兘闇€瑕?Scaffold 绁栧厛锛? *   鐢?[FeedScreen] 鐨?Scaffold 鎻愪緵锛夈€? * - 鏁版嵁娴佸畬鍏ㄥ鐢?[CollectionPickViewModel]锛圠iveData 鐢?observeAsState 妗ユ帴锛夈€? * - 灏侀潰涓婁紶閫昏緫鍦?[CollectionPickViewModel.create] / [CollectionPickViewModel.update] 閲岋紝
 *   杩欓噷鍙礋璐ｉ€夊浘锛堢郴缁熷浘鐗囬€夋嫨鍣級涓庨瑙堛€? */
@Composable
fun CollectionPickSheet(
    show: Boolean,
    feedId: String,
    viewModel: CollectionPickViewModel,
    onDismissRequest: () -> Unit,
    onChanged: () -> Unit = {},
    onToast: (String) -> Unit = {},
) {
    val list by viewModel.list.observeAsState(emptyList())
    val loading by viewModel.loading.observeAsState(false)
    val toastEvent by viewModel.toastText.observeAsState()
    val collectionList = list.orEmpty()
    val isLoading = loading == true

    // toast 浜嬩欢妗ユ帴锛圗vent 璇箟锛氬彧浼氳娑堣垂涓€娆★級锛涜€佷唬鐮佹敹鍒?toast 鍚庨『甯﹂€氱煡澶栭儴鍒锋柊
    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let {
            onToast(it)
            onChanged()
        }
    }

    LaunchedEffect(show, feedId) {
        if (show) viewModel.load(feedId)
    }

    // 缂栬緫鏀惰棌澶癸紙null = 涓嶆樉绀虹紪杈戝脊绐楋紱鏂板缓鏃朵紶鍏ョ殑鏄┖鏁版嵁锛?    var editItem by remember { mutableStateOf<CollectionData?>(null) }
    var showEdit by remember { mutableStateOf(false) }

    OverlayBottomSheet(
        show = show,
        title = stringResource(R.string.collection_pick),
        endAction = {
            TextButton(
                text = stringResource(R.string.collection_create),
                onClick = {
                    editItem = null
                    showEdit = true
                },
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        },
        onDismissRequest = onDismissRequest,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (collectionList.isEmpty() && !isLoading) {
                Text(
                    text = "杩樻病鏈夋敹钘忓す锛屽厛鏂板缓涓€涓惂",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                items(collectionList, key = { it.id.orEmpty() }) { item ->
                    CollectionPickRow(
                        item = item,
                        onClick = { viewModel.toggle(feedId, item) },
                        onLongClick = {
                            editItem = item
                            showEdit = true
                        },
                    )
                }
            }
        }
    }

    if (showEdit) {
        CollectionEditDialog(
            item = editItem,
            feedId = feedId,
            viewModel = viewModel,
            onDismiss = { showEdit = false },
            onToast = onToast,
        )
    }
}

/** 鏀惰棌澶规潯鐩細灏侀潰 + 鍚嶇О + 銆孨 鏉″唴瀹?路 鍏紑/绉佸瘑銆? 閫変腑瀵瑰嬀锛堝搴?item_collection_pick.xml锛?*/
@Composable
private fun CollectionPickRow(
    item: CollectionData,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(item.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() },
                )
            }
            .padding(vertical = 8.dp),
    ) {
        GlideImage(
            url = item.cover_pic,
            modifier = Modifier.size(56.dp),
            cornerRadius = 8,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 12.dp),
        ) {
            Text(
                text = item.title.orEmpty(),
                style = MiuixTheme.textStyles.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(item.item_num ?: 0)
                    append(" 鏉″唴瀹?)
                    if (!item.is_open_title.isNullOrEmpty()) {
                        append(" 路 ")
                        append(item.is_open_title)
                    }
                },
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (item.isBeCollected == 1) {
            Icon(
                MiuixIcons.Basic.Check,
                contentDescription = "宸叉敹钘?,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * 鏂板缓 / 缂栬緫鏀惰棌澶硅〃鍗曪紙瀵瑰簲鑰佺殑 dialog_collection_edit.xml锛夈€? * 灏侀潰閫夋嫨鐢ㄧ郴缁熷浘鐗囬€夋嫨鍣紙PickVisualMedia锛夛紝鏈湴棰勮鐩存帴 setImageURI锛? * 缂栬緫宸叉湁鏀惰棌澶规椂鍏堟樉绀鸿繙绔皝闈紙Glide锛夈€? */
@Composable
private fun CollectionEditDialog(
    item: CollectionData?,
    feedId: String,
    viewModel: CollectionPickViewModel,
    onDismiss: () -> Unit,
    onToast: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var title by remember(item) { mutableStateOf(item?.title.orEmpty()) }
    var description by remember(item) { mutableStateOf(item?.description.orEmpty()) }
    var isOpen by remember(item) { mutableStateOf(item?.is_open == 1) }
    var coverUri by remember(item) { mutableStateOf<Uri?>(null) }

    val pickCover = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) coverUri = uri
    }

    OverlayDialog(
        show = true,
        title = stringResource(
            if (item == null) R.string.collection_create else R.string.collection_edit
        ),
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.collection_title_hint),
                useLabelAsPlaceholder = true,
                singleLine = true,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(R.string.collection_desc_hint),
                useLabelAsPlaceholder = true,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            CoverPicker(
                coverUri = coverUri,
                remoteCover = item?.cover_pic,
                onClick = {
                    pickCover.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            )
            Text(
                text = stringResource(R.string.collection_cover),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.collection_open),
                    style = MiuixTheme.textStyles.body1,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = isOpen,
                    onCheckedChange = { isOpen = it },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    text = "鍙栨秷",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(20.dp))
                TextButton(
                    text = "纭畾",
                    onClick = {
                        val newTitle = title.trim()
                        if (newTitle.isEmpty()) {
                            onToast("璇峰～鍐欐敹钘忓す鍚嶇О")
                            return@TextButton
                        }
                        val newDesc = description.trim()
                        val open = if (isOpen) 1 else 0
                        if (item == null) {
                            viewModel.create(
                                feedId, newTitle, newDesc, open, coverUri,
                                context.contentResolver,
                            )
                        } else {
                            viewModel.update(
                                item.id.orEmpty(), newTitle, newDesc, open, coverUri,
                                item.cover_pic, context.contentResolver, feedId,
                            )
                        }
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

/** 灏侀潰閫夋嫨鍖猴細宸查€夋湰鍦板浘浼樺厛锛屽叾娆¤繙绔皝闈?*/
@Composable
private fun CoverPicker(
    coverUri: Uri?,
    remoteCover: String?,
    onClick: () -> Unit,
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setOnClickListener { }
            }
        },
        update = { imageView ->
            imageView.setOnClickListener { onClick() }
            when {
                coverUri != null -> {
                    imageView.tag = coverUri
                    imageView.setImageURI(coverUri)
                }

                imageView.tag != remoteCover -> {
                    imageView.tag = remoteCover
                    imageView.setImageDrawable(null)
                    ImageUtil.showIMG(imageView, remoteCover)
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp)),
    )
}
