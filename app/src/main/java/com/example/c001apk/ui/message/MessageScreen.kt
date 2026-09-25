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
import androidx.lifecycle.livedata.compose.observeAsState
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
 * 「我的 / 消息」页面（迁移 ui/message/ 下 MineFragment + MessageFirst/Second/ThirdAdapter +
 * MessageAdapter 的界面与交互）。
 *
 * - 头部：头像 + 昵称 + 等级 + 经验进度（未登录显示「点击登录」）
 * - 卡片一：动态 / 关注 / 粉丝 计数（MessageFirstAdapter）
 * - 卡片二：我的装备 / 浏览历史 / 我的常去 / 我的收藏 / 我的赞 / 我的回复（MessageSecondAdapter）
 * - 卡片三：@我的动态 / @我的评论 / 我收到的赞 / 好友关注 / 私信（MessageThirdAdapter，带未读角标）
 * - 通知列表：/v6/notification/list 的消息条目（MessageAdapter）
 *
 * 数据流全部复用 [MessageViewModel]（LiveData 用 observeAsState 桥接）；
 * 跳转默认行为复用现有 Activity，也可以用参数回调整体替换。
 */
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

    // ---------------- 状态桥接（LiveData -> Compose） ----------------
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

    // 未读角标：CookieUtil.atme / atcommentme / feedlike / contacts_follow，
    // messCountList 只是「刷新角标」的触发信号（与 MessageThirdAdapter.updateBadge 等价）。
    val badgeCounts = remember(messTick) {
        listOf(
            CookieUtil.atme ?: 0,
            CookieUtil.atcommentme ?: 0,
            CookieUtil.feedlike ?: 0,
            CookieUtil.contacts_follow ?: 0,
            0
        )
    }

    // 首次进入：与 MineFragment.getData() 一致
    LaunchedEffect(Unit) {
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

    // onResume：编辑资料（头像）返回后同步头部 + 消息角标清理（MineFragment.onResume）
    val lifecycleOwner = LocalLifecycleOwner.current
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

    // 头部资料刷新（MineFragment.showProfile 在 LoadingDone 时执行）
    LaunchedEffect(loadingState) {
        if (loadingState is LoadingState.LoadingDone) {
            profile = MessageProfileSnapshot.capture()
            refreshing = false
        }
        if (loadingState is LoadingState.LoadingFailed) refreshing = false
    }

    // footer 进入非 Loading 状态即结束下拉刷新（MineFragment 中 SwipeRefreshLayout 的收起时机）
    LaunchedEffect(footerState) {
        if (footerState != null && footerState !is FooterState.Loading) refreshing = false
    }

    // Toast（Event 只消费一次）
    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    // 加载更多：滚到最后一条时触发（MineFragment.initScroll 的 loadMore 判断）
    val listState = rememberLazyListState()
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
                title = "我的",
                actions = {
                    if (isLogin) {
                        TextButton(
                            text = "退出登录",
                            onClick = { showLogoutDialog = true }
                        )
                    }
                    IconButton(onClick = openSettings) {
                        Icon(MiuixIcons.Settings, contentDescription = "设置")
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
                    // 头部：资料卡 / 登录入口
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
                                        Text("点击登录")
                                    }
                                }
                            }
                        }
                    }

                    // 动态 / 关注 / 粉丝
                    if (isLogin) {
                        item(key = "fff") {
                            FffStatsCard(
                                counts = countList,
                                onClick = openFffList
                            )
                        }
                    }

                    // 我的装备 / 浏览历史 / 我的常去 / 我的收藏 / 我的赞 / 我的回复
                    item(key = "mine") {
                        MineEntryCard(
                            isLogin = isLogin,
                            onOpenDevice = openDevice,
                            onOpenHistory = openHistory,
                            onOpenFffList = openFffList,
                            onOpenCollection = openCollection
                        )
                    }

                    // @我的动态 / @我的评论 / 我收到的赞 / 好友关注 / 私信
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

                    // 通知列表
                    if (messageData.isEmpty() && isLogin && !refreshing) {
                        item(key = "empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无消息",
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

            // 退出登录确认（原 MineFragment.doLogout 的 MaterialAlertDialog）
            OverlayDialog(
                title = "确定退出登录？",
                show = showLogoutDialog,
                onDismissRequest = { showLogoutDialog = false }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        text = "取消",
                        onClick = { showLogoutDialog = false },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        text = "确定",
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

            // 条目菜单（原 MessageAdapter 的 PopupMenu：加入黑名单 / 举报）
            itemMenuTarget?.let { (data, position) ->
                OverlayDialog(
                    title = "来自 ${data.fromusername} 的通知",
                    show = true,
                    onDismissRequest = { itemMenuTarget = null }
                ) {
                    Column {
                        BasicComponent(
                            title = "加入黑名单",
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
                            title = "举报",
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

            // 长按删除（原 ItemListener.onMessLongClicked）
            deleteTarget?.let { (data, position) ->
                OverlayDialog(
                    title = "删除来自 ${data.fromusername} 的通知？",
                    show = true,
                    onDismissRequest = { deleteTarget = null }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            text = "取消",
                            onClick = { deleteTarget = null },
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            text = "确定",
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

// ---------------------------------------------------------------- 页面分区

/** 头像 + 昵称 + 等级 + 经验进度（fragment_mine.xml 的 profileLayout） */
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

/** 动态 / 关注 / 粉丝 三列计数（item_message_fff.xml + MessageFirstAdapter） */
@Composable
private fun FffStatsCard(
    counts: List<String>,
    onClick: (String) -> Unit
) {
    val titles = listOf("动态", "关注", "粉丝")
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

/** 我的装备 / 浏览历史 / 我的常去 / 我的收藏 / 我的赞 / 我的回复（item_message_mine.xml + MessageSecondAdapter） */
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
        Entry(R.drawable.ic_device, "我的装备", { if (isLogin) onOpenDevice() }),
        Entry(R.drawable.ic_history, "浏览历史", onOpenHistory),
        Entry(R.drawable.ic_freq, "我的常去", { if (isLogin) onOpenFffList("recentHistory") }),
        Entry(R.drawable.ic_star, "我的收藏", { if (isLogin) onOpenCollection() }),
        Entry(R.drawable.ic_fav, "我的赞", { if (isLogin) onOpenFffList("like") }),
        Entry(R.drawable.ic_chat, "我的回复", { if (isLogin) onOpenFffList("reply") })
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

/** @我的动态 / @我的评论 / 我收到的赞 / 好友关注 / 私信（item_message_mess.xml + MessageThirdAdapter） */
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

    // 老代码用硬编码 hex 做图标底色；按迁移契约改为 MiuixTheme 语义色对
    val entries = listOf(
        Entry(
            "@我的动态", "atMe", R.drawable.ic_at,
            MiuixTheme.colorScheme.primary, MiuixTheme.colorScheme.onPrimary
        ),
        Entry(
            "@我的评论", "atCommentMe", R.drawable.ic_comment,
            MiuixTheme.colorScheme.secondary, MiuixTheme.colorScheme.onSecondary
        ),
        Entry(
            "我收到的赞", "feedLike", R.drawable.ic_thumb,
            MiuixTheme.colorScheme.tertiaryContainer, MiuixTheme.colorScheme.onTertiaryContainer
        ),
        Entry(
            "好友关注", "contactsFollow", R.drawable.ic_add,
            MiuixTheme.colorScheme.error, MiuixTheme.colorScheme.onError
        ),
        Entry(
            "私信", "list", R.drawable.ic_message1,
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

/** 通知条目（item_message_item.xml + MessageAdapter） */
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
                            contentDescription = "更多",
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

// ---------------------------------------------------------------- 公共小组件

/** 用 Glide 链路加载圆形头像（不引入新图片库，见 CONVENTIONS §4） */
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
    when (footerState) {
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
                    text = footerState.errMsg,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.error
                )
                TextButton(text = "重试", onClick = onRetry)
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
                    text = footerState.msg,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }

        else -> Spacer(modifier = Modifier.height(4.dp))
    }
}

/** Html -> 纯文本（老代码用 LinkTextView 渲染 note/message，Compose 里先按纯文本显示） */
internal fun htmlToPlainText(html: String?): String {
    if (html.isNullOrEmpty()) return ""
    @Suppress("DEPRECATION")
    return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
}

/** 原 MineFragment.doLogout 的状态清理（recreate 交给宿主 [onAfterLogout]） */
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

/** 头部资料快照（PrefManager -> Compose 状态） */
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
 * 默认跳转行为：复用老代码 ItemListener / IntentUtil 的跳转目标，
 * Screen 级回调参数可以整体替换。
 */
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

    /** 复用 ItemListener.onMessClicked 的 note 链接解析（跳动态 / 网页） */
    fun openMessNote(context: Context, note: String) {
        object : ItemListener {}.onMessClicked(View(context), note)
    }
}
