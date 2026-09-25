package com.example.c001apk.ui.message

import android.content.Context
import android.text.Html
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.c001apk.R
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.adapter.ItemListener
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.logic.model.MessageResponse
import com.example.c001apk.ui.collection.CollectionActivity
import com.example.c001apk.ui.follow.FFFListActivity
import com.example.c001apk.ui.history.HistoryActivity
import com.example.c001apk.ui.login.WebLoginActivity
import com.example.c001apk.ui.messagedetail.MessageActivity
import com.example.c001apk.ui.others.WebViewActivity
import com.example.c001apk.ui.settings.SettingsActivity
import com.example.c001apk.ui.user.UserActivity
import com.example.c001apk.util.CookieUtil
import com.example.c001apk.util.DateUtils
import com.example.c001apk.util.ImageUtil
import com.example.c001apk.util.IntentUtil
import com.example.c001apk.util.PrefManager
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.VerticalDivider
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 銆屾垜鐨?/ 娑堟伅銆嶉〉闈紙杩佺Щ ui/message/ 涓?MineFragment + MessageFirst/Second/ThirdAdapter +
 * MessageAdapter 鐨勭晫闈笌浜や簰锛夈€? *
 * - 澶撮儴锛氬ご鍍?+ 鏄电О + 绛夌骇 + 缁忛獙杩涘害锛堟湭鐧诲綍鏄剧ず銆岀偣鍑荤櫥褰曘€嶏級
 * - 鍗＄墖涓€锛氬姩鎬?/ 鍏虫敞 / 绮変笣 璁℃暟锛圡essageFirstAdapter锛? * - 鍗＄墖浜岋細鎴戠殑瑁呭 / 娴忚鍘嗗彶 / 鎴戠殑甯稿幓 / 鎴戠殑鏀惰棌 / 鎴戠殑璧?/ 鎴戠殑鍥炲锛圡essageSecondAdapter锛? * - 鍗＄墖涓夛細@鎴戠殑鍔ㄦ€?/ @鎴戠殑璇勮 / 鎴戞敹鍒扮殑璧?/ 濂藉弸鍏虫敞 / 绉佷俊锛圡essageThirdAdapter锛屽甫鏈瑙掓爣锛? * - 閫氱煡鍒楄〃锛?v6/notification/list 鐨勬秷鎭潯鐩紙MessageAdapter锛? *
 * 鏁版嵁娴佸叏閮ㄥ鐢?[MessageViewModel]锛圠iveData 鐢?observeAsState 妗ユ帴锛夛紱
 * 璺宠浆榛樿琛屼负澶嶇敤鐜版湁 Activity锛屼篃鍙互鐢ㄥ弬鏁板洖璋冩暣浣撴浛鎹€? */
@Composable
fun MessageScreen(
    viewModel: MessageViewModel = viewModel(),
    onOpenUser: ((uid: String?) -> Unit)? = null,
    onOpenSelfProfile: (() -> Unit)? = null,
    onLogin: (() -> Unit)? = null,
    onOpenFffList: ((type: String) -> Unit)? = null,
    onOpenDevice: (() -> Unit)? = null,
    onOpenHistory: (() -> Unit)? = null,
    onOpenCollection: (() -> Unit)? = null,
    onOpenMessageDetail: ((type: String) -> Unit)? = null,
    onOpenFeed: ((id: String?, rid: String?, viewReply: Boolean?) -> Unit)? = null,
    onOpenNote: ((note: String) -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onAfterLogout: () -> Unit = {},
) {
    val context = LocalContext.current

    val openUser: (String?) -> Unit = onOpenUser ?: { uid -> MessageNav.viewUser(context, uid) }
    val openSelfProfile: () -> Unit = onOpenSelfProfile ?: {
        MessageNav.viewUser(context, PrefManager.uid)
    }
    val login: () -> Unit = onLogin ?: {
        IntentUtil.startActivity<WebLoginActivity>(context) {}
    }
    val openFffList: (String) -> Unit = onOpenFffList ?: { type ->
        IntentUtil.startActivity<FFFListActivity>(context) {
            putExtra("uid", PrefManager.uid)
            putExtra("type", type)
        }
    }
    val openDevice: () -> Unit = onOpenDevice ?: {
        IntentUtil.startActivity<WebViewActivity>(context) {
            putExtra("url", "https://m.coolapk.com/myDevice/${PrefManager.uid}")
            putExtra(
                "editUrl",
                "https://m.coolapk.com/mp/do?c=product&m=editProductOwner&from=home"
            )
        }
    }
    val openHistory: () -> Unit = onOpenHistory ?: {
        IntentUtil.startActivity<HistoryActivity>(context) {
            putExtra("type", "browse")
        }
    }
    val openCollection: () -> Unit = onOpenCollection ?: {
        IntentUtil.startActivity<CollectionActivity>(context) {}
    }
    val openMessageDetail: (String) -> Unit = onOpenMessageDetail ?: { type ->
        MessageNav.openMessageDetail(context, type)
    }
    val openFeed: (String?, String?, Boolean?) -> Unit =
        onOpenFeed ?: { id, rid, viewReply -> MessageNav.viewFeed(context, id, rid, viewReply) }
    val openNote: (String) -> Unit = onOpenNote ?: { note -> MessageNav.openMessNote(context, note) }
    val openSettings: () -> Unit = onOpenSettings ?: {
        IntentUtil.startActivity<SettingsActivity>(context) {}
    }

    // ---------------- 鐘舵€佹ˉ鎺ワ紙LiveData -> Compose锛?----------------
    val countList by viewModel.countList.observeAsState(emptyList())
    val messTick by viewModel.messCountList.observeAsState()
    val messageData by viewModel.messageData.observeAsState(emptyList())
    val footerState by viewModel.footerState.observeAsState()
    val loadingState by viewModel.loadingState.observeAsState()
    val toastEvent by viewModel.toastText.observeAsState()

    val isLogin = remember { PrefManager.isLogin }
    var profile by remember { mutableStateOf(MessageProfileSnapshot.capture()) }
    var refreshing by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var itemMenuTarget by remember { mutableStateOf<Pair<MessageResponse.Data, Int>?>(null) }
    var deleteTarget by remember { mutableStateOf<Pair<MessageResponse.Data, Int>?>(null) }

    // 鏈瑙掓爣锛欳ookieUtil.atme / atcommentme / feedlike / contacts_follow锛?    // messCountList 鍙槸銆屽埛鏂拌鏍囥€嶇殑瑙﹀彂淇″彿锛堜笌 MessageThirdAdapter.updateBadge 绛変环锛夈€?    val badgeCounts = remember(messTick) {
        listOf(
            CookieUtil.atme ?: 0,
            CookieUtil.atcommentme ?: 0,
            CookieUtil.feedlike ?: 0,
            CookieUtil.contacts_follow ?: 0,
            0
        )
    }

    // 棣栨杩涘叆锛氫笌 MineFragment.getData() 涓€鑷?    LaunchedEffect(Unit) {
        if (isLogin) {
            viewModel.lastItem = null
            viewModel.page = 1
            viewModel.isEnd = false
            viewModel.isRefreshing = true
            viewModel.isLoadMore = false
            viewModel.onCheckCount()
            viewModel.fetchProfile()
        }
    }

    // onResume锛氱紪杈戣祫鏂欙紙澶村儚锛夎繑鍥炲悗鍚屾澶撮儴 + 娑堟伅瑙掓爣娓呯悊锛圡ineFragment.onResume锛?    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                profile = MessageProfileSnapshot.capture()
                if (PrefManager.isLogin && CookieUtil.badge != 0) {
                    CookieUtil.badge = 0
                    viewModel.messCountList.value = true
                    if (CookieUtil.notification != 0) {
                        CookieUtil.notification = 0
                        viewModel.refreshMessage()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 澶撮儴璧勬枡鍒锋柊锛圡ineFragment.showProfile 鍦?LoadingDone 鏃舵墽琛岋級
    LaunchedEffect(loadingState) {
        if (loadingState is LoadingState.LoadingDone) {
            profile = MessageProfileSnapshot.capture()
            refreshing = false
        }
        if (loadingState is LoadingState.LoadingFailed) refreshing = false
    }

    // footer 杩涘叆闈?Loading 鐘舵€佸嵆缁撴潫涓嬫媺鍒锋柊锛圡ineFragment 涓?SwipeRefreshLayout 鐨勬敹璧锋椂鏈猴級
    LaunchedEffect(footerState) {
        if (footerState != null && footerState !is FooterState.Loading) refreshing = false
    }

    // Toast锛圗vent 鍙秷璐逛竴娆★級
    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    // 鍔犺浇鏇村锛氭粴鍒版渶鍚庝竴鏉℃椂瑙﹀彂锛圡ineFragment.initScroll 鐨?loadMore 鍒ゆ柇锛?    val listState = rememberLazyListState()
    val atListEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 1
        }
    }
    LaunchedEffect(atListEnd) {
        if (atListEnd && PrefManager.isLogin && !viewModel.isEnd && !viewModel.isRefreshing
            && !viewModel.isLoadMore && !refreshing
        ) {
            viewModel.isLoadMore = true
            viewModel.fetchMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "鎴戠殑",
                actions = {
                    if (isLogin) {
                        TextButton(
                            text = "閫€鍑虹櫥褰?,
                            onClick = { showLogoutDialog = true }
                        )
                    }
                    IconButton(onClick = openSettings) {
                        Icon(MiuixIcons.Settings, contentDescription = "璁剧疆")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            PullToRefresh(
                isRefreshing = refreshing,
                onRefresh = {
                    if (isLogin && !viewModel.isLoadMore) {
                        refreshing = true
                        viewModel.lastItem = null
                        viewModel.page = 1
                        viewModel.isEnd = false
                        viewModel.isRefreshing = true
                        viewModel.isLoadMore = false
                        viewModel.onCheckCount()
                        viewModel.fetchProfile()
                    } else {
                        refreshing = false
                    }
                },
                pullToRefreshState = rememberPullToRefreshState(),
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 10.dp,
                        end = 10.dp,
                        top = 10.dp,
                        bottom = paddingValues.calculateBottomPadding() + 10.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 澶撮儴锛氳祫鏂欏崱 / 鐧诲綍鍏ュ彛
                    item(key = "profile") {
                        if (isLogin) {
                            ProfileHeader(
                                snapshot = profile,
                                onClick = openSelfProfile
                            )
                        } else {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(onClick = login) {
                                        Text("鐐瑰嚮鐧诲綍")
                                    }
                                }
                            }
                        }
                    }

                    // 鍔ㄦ€?/ 鍏虫敞 / 绮変笣
                    if (isLogin) {
                        item(key = "fff") {
                            FffStatsCard(
                                counts = countList,
                                onClick = openFffList
                            )
                        }
                    }

                    // 鎴戠殑瑁呭 / 娴忚鍘嗗彶 / 鎴戠殑甯稿幓 / 鎴戠殑鏀惰棌 / 鎴戠殑璧?/ 鎴戠殑鍥炲
                    item(key = "mine") {
                        MineEntryCard(
                            isLogin = isLogin,
                            onOpenDevice = openDevice,
                            onOpenHistory = openHistory,
                            onOpenFffList = openFffList,
                            onOpenCollection = openCollection
                        )
                    }

                    // @鎴戠殑鍔ㄦ€?/ @鎴戠殑璇勮 / 鎴戞敹鍒扮殑璧?/ 濂藉弸鍏虫敞 / 绉佷俊
                    if (isLogin) {
                        item(key = "mess") {
                            MessageEntryCard(
                                badgeCounts = badgeCounts,
                                onOpen = { type ->
                                    when (type) {
                                        "atMe" -> CookieUtil.atme = null
                                        "atCommentMe" -> CookieUtil.atcommentme = null
                                        "feedLike" -> CookieUtil.feedlike = null
                                        "contactsFollow" -> CookieUtil.contacts_follow = null
                                    }
                                    viewModel.messCountList.value = true
                                    openMessageDetail(type)
                                }
                            )
                        }
                    }

                    // 閫氱煡鍒楄〃
                    if (messageData.isEmpty() && isLogin && !refreshing) {
                        item(key = "empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "鏆傛棤娑堟伅",
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                    }
                    itemsIndexed(
                        items = messageData,
                        key = { _, item -> item.id }
                    ) { index, data ->
                        MessageNotificationItem(
                            data = data,
                            onClick = { openNote(data.note) },
                            onAvatarClick = { openUser(data.fromuid) },
                            onExpand = { itemMenuTarget = data to index },
                            onLongClick = { deleteTarget = data to index }
                        )
                    }

                    item(key = "footer") {
                        MessageFooterRow(
                            footerState = footerState,
                            onRetry = {
                                viewModel.isEnd = false
                                viewModel.isLoadMore = true
                                viewModel.fetchMessage()
                            }
                        )
                    }
                }
            }

            // 閫€鍑虹櫥褰曠‘璁わ紙鍘?MineFragment.doLogout 鐨?MaterialAlertDialog锛?            OverlayDialog(
                title = "纭畾閫€鍑虹櫥褰曪紵",
                show = showLogoutDialog,
                onDismissRequest = { showLogoutDialog = false }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        text = "鍙栨秷",
                        onClick = { showLogoutDialog = false },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        text = "纭畾",
                        onClick = {
                            showLogoutDialog = false
                            performLogout(viewModel)
                            profile = MessageProfileSnapshot.capture()
                            onAfterLogout()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 鏉＄洰鑿滃崟锛堝師 MessageAdapter 鐨?PopupMenu锛氬姞鍏ラ粦鍚嶅崟 / 涓炬姤锛?            itemMenuTarget?.let { (data, position) ->
                OverlayDialog(
                    title = "鏉ヨ嚜 ${data.fromusername} 鐨勯€氱煡",
                    show = true,
                    onDismissRequest = { itemMenuTarget = null }
                ) {
                    Column {
                        BasicComponent(
                            title = "鍔犲叆榛戝悕鍗?,
                            onClick = {
                                itemMenuTarget = null
                                viewModel.saveUid(data.fromuid)
                                val current =
                                    viewModel.messageData.value?.toMutableList() ?: ArrayList()
                                if (position in current.indices) {
                                    current.removeAt(position)
                                    viewModel.messageData.postValue(current)
                                }
                            }
                        )
                        BasicComponent(
                            title = "涓炬姤",
                            onClick = {
                                itemMenuTarget = null
                                IntentUtil.startActivity<WebViewActivity>(context) {
                                    putExtra(
                                        "url",
                                        "https://m.coolapk.com/mp/do?c=user&m=report&id=${data.fromuid}"
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // 闀挎寜鍒犻櫎锛堝師 ItemListener.onMessLongClicked锛?            deleteTarget?.let { (data, position) ->
                OverlayDialog(
                    title = "鍒犻櫎鏉ヨ嚜 ${data.fromusername} 鐨勯€氱煡锛?,
                    show = true,
                    onDismissRequest = { deleteTarget = null }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            text = "鍙栨秷",
                            onClick = { deleteTarget = null },
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            text = "纭畾",
                            onClick = {
                                deleteTarget = null
                                viewModel.onPostDelete(position, data.id)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 椤甸潰鍒嗗尯

/** 澶村儚 + 鏄电О + 绛夌骇 + 缁忛獙杩涘害锛坒ragment_mine.xml 鐨?profileLayout锛?*/
@Composable
private fun ProfileHeader(
    snapshot: MessageProfileSnapshot,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlideAvatar(
                url = snapshot.avatar,
                size = 60.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Text(
                    text = snapshot.name,
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Lv.${snapshot.level}",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${snapshot.experience}/${snapshot.nextLevelExperience}",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                val max = snapshot.nextLevelExperience.toIntOrNull() ?: 0
                val exp = snapshot.experience.toIntOrNull() ?: 0
                LinearProgressIndicator(
                    progress = if (max > 0) (exp.toFloat() / max).coerceIn(0f, 1f) else 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
            }
        }
    }
}

/** 鍔ㄦ€?/ 鍏虫敞 / 绮変笣 涓夊垪璁℃暟锛坕tem_message_fff.xml + MessageFirstAdapter锛?*/
@Composable
private fun FffStatsCard(
    counts: List<String>,
    onClick: (String) -> Unit
) {
    val titles = listOf("鍔ㄦ€?, "鍏虫敞", "绮変笣")
    val types = listOf("feed", "follow", "fans")
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            titles.forEachIndexed { index, title ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onClick(types[index]) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = counts.getOrElse(index) { "0" },
                        style = MiuixTheme.textStyles.title2,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = title,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
                if (index < titles.lastIndex) {
                    VerticalDivider(
                        modifier = Modifier.height(20.dp),
                        color = MiuixTheme.colorScheme.dividerLine
                    )
                }
            }
        }
    }
}

/** 鎴戠殑瑁呭 / 娴忚鍘嗗彶 / 鎴戠殑甯稿幓 / 鎴戠殑鏀惰棌 / 鎴戠殑璧?/ 鎴戠殑鍥炲锛坕tem_message_mine.xml + MessageSecondAdapter锛?*/
@Composable
private fun MineEntryCard(
    isLogin: Boolean,
    onOpenDevice: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenFffList: (String) -> Unit,
    onOpenCollection: () -> Unit
) {
    data class Entry(val icon: Int, val title: String, val onClick: () -> Unit)

    val entries = listOf(
        Entry(R.drawable.ic_device, "鎴戠殑瑁呭", { if (isLogin) onOpenDevice() }),
        Entry(R.drawable.ic_history, "娴忚鍘嗗彶", onOpenHistory),
        Entry(R.drawable.ic_freq, "鎴戠殑甯稿幓", { if (isLogin) onOpenFffList("recentHistory") }),
        Entry(R.drawable.ic_star, "鎴戠殑鏀惰棌", { if (isLogin) onOpenCollection() }),
        Entry(R.drawable.ic_fav, "鎴戠殑璧?, { if (isLogin) onOpenFffList("like") }),
        Entry(R.drawable.ic_chat, "鎴戠殑鍥炲", { if (isLogin) onOpenFffList("reply") })
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        entries.chunked(3).forEachIndexed { rowIndex, rowEntries ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowEntries.forEachIndexed { index, entry ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = entry.onClick)
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(entry.icon),
                            contentDescription = entry.title,
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = entry.title,
                            style = MiuixTheme.textStyles.footnote1
                        )
                    }
                    if (index < rowEntries.lastIndex) {
                        VerticalDivider(
                            modifier = Modifier.height(20.dp),
                            color = MiuixTheme.colorScheme.dividerLine
                        )
                    }
                }
            }
            if (rowIndex == 0) {
                HorizontalDivider(color = MiuixTheme.colorScheme.dividerLine)
            }
        }
    }
}

/** @鎴戠殑鍔ㄦ€?/ @鎴戠殑璇勮 / 鎴戞敹鍒扮殑璧?/ 濂藉弸鍏虫敞 / 绉佷俊锛坕tem_message_mess.xml + MessageThirdAdapter锛?*/
@Composable
private fun MessageEntryCard(
    badgeCounts: List<Int>,
    onOpen: (type: String) -> Unit
) {
    data class Entry(
        val title: String,
        val type: String,
        val icon: Int,
        val containerColor: androidx.compose.ui.graphics.Color,
        val contentColor: androidx.compose.ui.graphics.Color
    )

    // 鑰佷唬鐮佺敤纭紪鐮?hex 鍋氬浘鏍囧簳鑹诧紱鎸夎縼绉诲绾︽敼涓?MiuixTheme 璇箟鑹插
    val entries = listOf(
        Entry(
            "@鎴戠殑鍔ㄦ€?, "atMe", R.drawable.ic_at,
            MiuixTheme.colorScheme.primary, MiuixTheme.colorScheme.onPrimary
        ),
        Entry(
            "@鎴戠殑璇勮", "atCommentMe", R.drawable.ic_comment,
            MiuixTheme.colorScheme.secondary, MiuixTheme.colorScheme.onSecondary
        ),
        Entry(
            "鎴戞敹鍒扮殑璧?, "feedLike", R.drawable.ic_thumb,
            MiuixTheme.colorScheme.tertiaryContainer, MiuixTheme.colorScheme.onTertiaryContainer
        ),
        Entry(
            "濂藉弸鍏虫敞", "contactsFollow", R.drawable.ic_add,
            MiuixTheme.colorScheme.error, MiuixTheme.colorScheme.onError
        ),
        Entry(
            "绉佷俊", "list", R.drawable.ic_message1,
            MiuixTheme.colorScheme.secondaryContainer, MiuixTheme.colorScheme.onSecondaryContainer
        )
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        entries.forEachIndexed { index, entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(entry.type) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(entry.containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(entry.icon),
                        contentDescription = entry.title,
                        modifier = Modifier.size(25.dp)
                    )
                }
                Text(
                    text = entry.title,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                    style = MiuixTheme.textStyles.main
                )
                val count = badgeCounts.getOrElse(index) { 0 }
                if (count > 0) {
                    Badge(
                        containerColor = MiuixTheme.colorScheme.primary,
                        contentColor = MiuixTheme.colorScheme.onPrimary
                    ) {
                        Text(text = count.toString())
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    imageVector = MiuixIcons.ChevronForward,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (index < entries.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 60.dp),
                    color = MiuixTheme.colorScheme.dividerLine
                )
            }
        }
    }
}

/** 閫氱煡鏉＄洰锛坕tem_message_item.xml + MessageAdapter锛?*/
@Composable
private fun MessageNotificationItem(
    data: MessageResponse.Data,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onExpand: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        onLongPress = onLongClick
    ) {
        Row(modifier = Modifier.padding(10.dp)) {
            GlideAvatar(
                url = data.fromUserAvatar,
                size = 30.dp,
                onClick = onAvatarClick
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = data.fromusername,
                        modifier = Modifier.weight(1f),
                        style = MiuixTheme.textStyles.main,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = onExpand,
                        minWidth = 28.dp,
                        minHeight = 28.dp
                    ) {
                        Icon(
                            imageVector = MiuixIcons.More,
                            contentDescription = "鏇村",
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions
                        )
                    }
                }
                val noteText = remember(data.note) { htmlToPlainText(data.note) }
                if (noteText.isNotEmpty()) {
                    Text(
                        text = noteText,
                        modifier = Modifier.padding(top = 5.dp),
                        style = MiuixTheme.textStyles.body1,
                        lineHeight = 20.sp
                    )
                }
                Text(
                    text = DateUtils.fromToday(data.dateline),
                    modifier = Modifier.padding(top = 10.dp),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    }
}

// ---------------------------------------------------------------- 鍏叡灏忕粍浠?
/** 鐢?Glide 閾捐矾鍔犺浇鍦嗗舰澶村儚锛堜笉寮曞叆鏂板浘鐗囧簱锛岃 CONVENTIONS 搂4锛?*/
@Composable
private fun GlideAvatar(
    url: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView -> ImageUtil.showIMG(imageView, url) },
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    )
}

@Composable
internal fun MessageFooterRow(
    footerState: FooterState?,
    onRetry: () -> Unit
) {
    when (val footer = footerState) {
        is FooterState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }

        is FooterState.LoadingError -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = footer.errMsg,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.error
                )
                TextButton(text = "閲嶈瘯", onClick = onRetry)
            }
        }

        is FooterState.LoadingEnd -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = footer.msg,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }

        else -> Spacer(modifier = Modifier.height(4.dp))
    }
}

/** Html -> 绾枃鏈紙鑰佷唬鐮佺敤 LinkTextView 娓叉煋 note/message锛孋ompose 閲屽厛鎸夌函鏂囨湰鏄剧ず锛?*/
internal fun htmlToPlainText(html: String?): String {
    if (html.isNullOrEmpty()) return ""
    @Suppress("DEPRECATION")
    return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
}

/** 鍘?MineFragment.doLogout 鐨勭姸鎬佹竻鐞嗭紙recreate 浜ょ粰瀹夸富 [onAfterLogout]锛?*/
private fun performLogout(viewModel: MessageViewModel) {
    viewModel.countList.value = emptyList()
    CookieUtil.atme = null
    CookieUtil.atcommentme = null
    CookieUtil.feedlike = null
    CookieUtil.contacts_follow = null
    viewModel.messCountList.value = true
    viewModel.footerState.value = FooterState.LoadingDone
    viewModel.messageData.postValue(emptyList())
    viewModel.isInit = true
    viewModel.isEnd = false
    PrefManager.isLogin = false
    PrefManager.uid = ""
    PrefManager.username = ""
    PrefManager.token = ""
    PrefManager.userAvatar = ""
}

/** 澶撮儴璧勬枡蹇収锛圥refManager -> Compose 鐘舵€侊級 */
internal data class MessageProfileSnapshot(
    val name: String,
    val level: String,
    val experience: String,
    val nextLevelExperience: String,
    val avatar: String
) {
    companion object {
        fun capture(): MessageProfileSnapshot = MessageProfileSnapshot(
            name = try {
                java.net.URLDecoder.decode(PrefManager.username, "UTF-8")
            } catch (e: Exception) {
                PrefManager.username
            },
            level = PrefManager.level,
            experience = PrefManager.experience,
            nextLevelExperience = PrefManager.nextLevelExperience,
            avatar = PrefManager.userAvatar
        )
    }
}

/**
 * 榛樿璺宠浆琛屼负锛氬鐢ㄨ€佷唬鐮?ItemListener / IntentUtil 鐨勮烦杞洰鏍囷紝
 * Screen 绾у洖璋冨弬鏁板彲浠ユ暣浣撴浛鎹€? */
internal object MessageNav {

    fun viewUser(context: Context, uid: String?) {
        IntentUtil.startActivity<UserActivity>(context) {
            putExtra("id", uid)
        }
    }

    fun viewFeed(
        context: Context,
        id: String?,
        rid: String? = null,
        viewReply: Boolean? = null
    ) {
        IntentUtil.startActivity<com.example.c001apk.ui.feed.FeedActivity>(context) {
            putExtra("id", id)
            rid?.let { putExtra("rid", it) }
            viewReply?.let { putExtra("viewReply", it) }
        }
    }

    fun openMessageDetail(context: Context, type: String) {
        IntentUtil.startActivity<MessageActivity>(context) {
            putExtra("type", type)
        }
    }

    /** 澶嶇敤 ItemListener.onMessClicked 鐨?note 閾炬帴瑙ｆ瀽锛堣烦鍔ㄦ€?/ 缃戦〉锛?*/
    fun openMessNote(context: Context, note: String) {
        object : ItemListener {}.onMessClicked(View(context), note)
    }
}
