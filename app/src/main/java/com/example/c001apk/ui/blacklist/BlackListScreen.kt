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
import androidx.compose.runtime.livedata.observeAsState
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
 * 鐢ㄦ埛榛戝悕鍗曪紙浜戠锛夐〉闈?鈥斺€?瀵瑰簲鑰佺殑 [BlackListActivity] + [UserBlackListAdapter]锛坱ype = "user"锛夈€? *
 * 鏁版嵁娴佸畬鍏ㄥ鐢?[BlackListViewModel]锛圠iveData 鐢?observeAsState 妗ユ帴锛夛紝鏈枃浠跺彧璐熻矗鐣岄潰涓庝氦浜掞細
 * 澶村儚 / 鏄电О / UID / 绉诲嚭鎸夐挳锛岀Щ鍑哄墠鐢?Miuix [OverlayDialog] 纭锛岄《閮ㄨ繑鍥?+ 鏍囬銆? *
 * 娉ㄦ剰锛? * - 涓嶈嚜甯?MiuixAppTheme锛堟牴涓婚鐢?Activity 鎺ョ嚎鏃跺锛夛紱椤甸潰鑷惈涓€涓?Scaffold锛圤verlayDialog 闇€瑕?Scaffold 绁栧厛锛夈€? * - [BlackListViewModel] 鏄?Hilt assisted ViewModel锛岄渶鍦?Activity 渚х敤
 *   `viewModels(extrasProducer = 鈥?withCreationCallback<BlackListViewModel.Factory> 鈥?` 鍒涘缓鍚庝紶鍏ャ€? * - 澶村儚娌跨敤 Glide 閾捐矾锛圛mageUtil.showIMG + AndroidView 鍖?ImageView锛夛紝鏈紩鍏ユ柊鍥剧墖搴撱€? */
@Composable
fun BlackListScreen(
    viewModel: BlackListViewModel,
    onBack: () -> Unit,
    onUserClick: (uid: String) -> Unit = {},
    onToast: (String) -> Unit = {},
) {
    // observeAsState 杩斿洖 State<T?>锛岀粺涓€鍦ㄨ繖閲屽綊涓€鍖?    val users by viewModel.cloudUsers.observeAsState(emptyList())
    val loading by viewModel.loading.observeAsState(false)
    val toastEvent by viewModel.toastText.observeAsState()
    val userList = users.orEmpty()
    val isLoading = loading == true

    // 杩涘叆椤甸潰鎷夊彇浜戠榛戝悕鍗曪紙涓庤€?Activity 鐨?initObserve 涓€鑷达級
    LaunchedEffect(Unit) {
        viewModel.loadCloudUsers()
    }

    // toast 浜嬩欢妗ユ帴锛圗vent 璇箟锛氬彧浼氳娑堣垂涓€娆★級
    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let { onToast(it) }
    }

    // 寰呯‘璁ょЩ鍑虹殑鐢ㄦ埛锛坣ull = 涓嶆樉绀虹‘璁ゅ璇濇锛?    var pendingRemove by remember { mutableStateOf<BlackListUser?>(null) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "${stringResource(R.string.user_black_list)}锛?{userList.size}锛?,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            MiuixIcons.Back,
                            contentDescription = "杩斿洖",
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
                        text = "榛戝悕鍗曚负绌?,
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

            // 绉诲嚭纭瀵硅瘽妗嗭紙OverlayDialog 闇€瑕?Scaffold 绁栧厛锛屾斁鍦?Scaffold content 鍐咃級
            OverlayDialog(
                title = "绉诲嚭榛戝悕鍗?,
                summary = "纭畾灏嗐€?{pendingRemove?.name.orEmpty()}銆嶇Щ鍑洪粦鍚嶅崟鍚楋紵",
                show = pendingRemove != null,
                onDismissRequest = { pendingRemove = null },
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = "鍙栨秷",
                        onClick = { pendingRemove = null },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    TextButton(
                        text = "绉诲嚭",
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

/** 涓€琛岄粦鍚嶅崟鐢ㄦ埛锛氬ご鍍?+ 鏄电О + UID + 绉诲嚭鎸夐挳锛堝搴?item_user_black_list.xml锛?*/
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
                contentDescription = "绉诲嚭榛戝悕鍗?,
            )
        }
    }
}

/** 鍦嗗舰澶村儚锛欸lide锛圛mageUtil.showIMG锛? AndroidView 鍖?ImageView锛屽渾瑙掕鍓氦缁?Compose */
@Composable
private fun UserAvatar(url: String?) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            // url 鍙樺寲鎵嶉噸鏂拌蛋 Glide锛岄伩鍏嶆瘡娆￠噸缁勯兘鍙戣捣鍔犺浇
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
