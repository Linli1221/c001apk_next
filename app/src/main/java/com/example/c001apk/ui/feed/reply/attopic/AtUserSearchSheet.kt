package com.example.c001apk.ui.feed.reply.attopic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.state.ToggleableState
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.logic.model.RecentAtUser
import com.example.c001apk.ui.feed.reply.GlideImage
import com.example.c001apk.ui.feed.reply.ReplyListFooter
import com.example.c001apk.util.PrefManager
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

/*
 * @鐢ㄦ埛 / #璇濋# 閫夋嫨寮瑰眰锛圕ompose 鐗?AtTopicActivity + SearchFragment/AtUserAdapter/SearchAdapter锛夈€? *
 * - type="user"锛氭渶杩戣仈绯讳汉 + 濂藉弸 + 鎼滅储缁撴灉锛屽嬀閫夊浜哄悗銆屽畬鎴愩€嶅悎骞舵垚 "@a @b "锛? *   鎼滅储缁撴灉鍗曞嚮鐢ㄦ埛 = 绔嬪嵆杩斿洖锛堜笌鑰?AtTopicActivity.onClickUser 涓€鑷达級銆? * - type="topic"锛氭渶杩戝弬涓?+ 鐑棬璇濋 + 鎼滅储缁撴灉锛岀偣閫夎繑鍥?"#鏍囬# "锛屽苟鍐欏叆 PrefManager.recentIds銆? * - 鎼滅储妗嗙敤 Miuix [InputField]锛堟敞鎰忛鑹插弬鏁版槸 InputField.color锛屼笉鏄?SearchBar.color锛夈€? * - 鏁版嵁娴佸鐢?[AtTopicViewModel]锛堟祻瑙?鏈€杩戣仈绯讳汉锛変笌 [SearchViewModel]锛堝叧閿瘝鎼滅储锛夈€? * - 寮瑰眰鐢?Miuix [OverlayBottomSheet]锛堥渶 Scaffold 绁栧厛锛夈€? */

@Composable
fun AtUserSearchSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    type: String = "user",
    onResult: (String) -> Unit,
    viewModel: AtTopicViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel(),
    onToast: (String) -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<RecentAtUser>() }

    val recentUsers by viewModel.recentAtUsersData.observeAsState()
    val browseList by viewModel.followListData.observeAsState()
    val browseFooter by viewModel.footerState.observeAsState()
    val searchList by searchViewModel.dataList.observeAsState()
    val searchFooter by searchViewModel.footerState.observeAsState()
    val toastEvent by viewModel.toastText.observeAsState()

    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let { onToast(it.orEmpty()) }
    }

    // 鎵撳紑鏃讹細娓呯┖杈撳叆/閫変腑銆佽仛鐒︽悳绱㈡锛圛nputField 鏀跺埌 expanded=true 浼氳姹傜劍鐐癸級銆侀鎷夊垪琛?    LaunchedEffect(show) {
        if (show) {
            query = ""
            expanded = true
            selected.clear()
            if (viewModel.isInit) {
                viewModel.isInit = false
                if (type == "user") viewModel.getFollowList() else viewModel.getHotTopics()
            }
        } else {
            expanded = false
        }
    }

    // 鍏抽敭璇嶆悳绱紙瀵瑰簲 SearchFragment.onSearch + BaseViewFragment.refreshData 鐨勯噸缃涔夛級
    LaunchedEffect(show, query, type) {
        if (!show || query.isBlank()) return@LaunchedEffect
        searchViewModel.type = type
        searchViewModel.keyword = query.trim()
        searchViewModel.lastItem = null
        searchViewModel.page = 1
        searchViewModel.isEnd = false
        searchViewModel.isRefreshing = true
        searchViewModel.isLoadMore = false
        searchViewModel.dataList.value = emptyList()
        searchViewModel.fetchData()
    }

    fun buildUserResult(clicked: RecentAtUser? = null): String {
        val users = (selected + listOfNotNull(clicked)).distinctBy { it.username }
        // 璁板綍鏈€杩戣仈绯讳汉锛堝搴?AtTopicActivity.onClickUser / FAB 鐨?updateList锛?        viewModel.updateList(users)
        return users.joinToString(separator = "") { "@${it.username} " }
    }

    fun pickTopic(title: String, id: String) {
        // 鏈€杩戝弬涓庤瘽棰橈紙瀵瑰簲 AtTopicActivity.onClickTopic锛?        if (PrefManager.recentIds.isEmpty()) {
            PrefManager.recentIds = id
        } else {
            val idList = PrefManager.recentIds.split(",").toMutableList()
            idList.remove(id)
            PrefManager.recentIds = (listOf(id) + idList).joinToString(separator = ",")
        }
        onResult("#$title# ")
        onDismissRequest()
    }

    val isSearching = query.isNotBlank()
    val searchResults = searchList.orEmpty()
    val browsing = if (type == "user") {
        (recentUsers.orEmpty() + browseList.orEmpty())
    } else {
        browseList.orEmpty()
    }

    OverlayBottomSheet(
        show = show,
        title = if (type == "user") "@鐢ㄦ埛" else "#璇濋#",
        onDismissRequest = onDismissRequest,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            InputField(
                query = query,
                onQueryChange = { query = it },
                onSearch = { },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                label = "鎼滅储${if (type == "user") "鐢ㄦ埛" else "璇濋"}",
                color = MiuixTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (isSearching && searchResults.isEmpty()) {
                Text(
                    text = "鏃犲尮閰嶇粨鏋?,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(24.dp),
                )
            } else if (!isSearching && browsing.isEmpty()) {
                ReplyListFooter(
                    footerState = browseFooter,
                    onRetry = {
                        viewModel.isEnd = false
                        viewModel.isLoadMore = true
                        if (type == "user") viewModel.getFollowList() else viewModel.getHotTopics()
                    },
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                val listState = rememberLazyListState()
                val shownList: List<Any> = if (isSearching) searchResults else browsing
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                ) {
                    if (isSearching) {
                        items(searchResults) { data ->
                            SearchResultRow(
                                data = data,
                                selected = selected.any {
                                    it.username == (data.username ?: data.title)
                                },
                                onClick = {
                                    when (data.entityType) {
                                        "user" -> {
                                            onResult(
                                                buildUserResult(
                                                    RecentAtUser(
                                                        avatar = data.userAvatar ?: "",
                                                        username = data.username.orEmpty(),
                                                    )
                                                )
                                            )
                                            onDismissRequest()
                                        }

                                        "topic" -> pickTopic(
                                            data.title.orEmpty(),
                                            data.id.toString(),
                                        )
                                    }
                                },
                                onToggle = {
                                    if (data.entityType == "user") {
                                        val user = RecentAtUser(
                                            avatar = data.userAvatar ?: "",
                                            username = data.username.orEmpty(),
                                        )
                                        val index = selected.indexOfFirst {
                                            it.username == user.username
                                        }
                                        if (index >= 0) selected.removeAt(index) else selected.add(user)
                                    }
                                },
                            )
                            HorizontalDivider(
                                color = MiuixTheme.colorScheme.dividerLine,
                                thickness = 0.5.dp,
                            )
                        }
                    } else {
                        items(browsing) { user ->
                            BrowseRow(
                                user = user,
                                showCheckbox = type == "user",
                                checked = selected.any { it.username == user.username },
                                onClick = {
                                    if (type == "user") {
                                        val index =
                                            selected.indexOfFirst { it.username == user.username }
                                        if (index >= 0) selected.removeAt(index)
                                        else selected.add(user)
                                    } else {
                                        pickTopic(user.username, user.id.toString())
                                    }
                                },
                            )
                        }
                        item {
                            ReplyListFooter(
                                footerState = browseFooter,
                                onRetry = {
                                    viewModel.isEnd = false
                                    viewModel.isLoadMore = true
                                    if (type == "user") viewModel.getFollowList()
                                    else viewModel.getHotTopics()
                                },
                            )
                        }
                    }
                    item {
                        if (isSearching) {
                            ReplyListFooter(
                                footerState = searchFooter,
                                onRetry = {
                                    searchViewModel.isEnd = false
                                    searchViewModel.isLoadMore = true
                                    searchViewModel.fetchData()
                                },
                            )
                        }
                    }
                }
                // 婊氬姩鍒板簳鑷姩鍔犺浇鏇村
                LaunchedEffect(listState, shownList.size) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .collect { last ->
                            if (last == null || last < shownList.size) return@collect
                            if (isSearching) {
                                if (!searchViewModel.isEnd && !searchViewModel.isLoadMore) {
                                    searchViewModel.isLoadMore = true
                                    searchViewModel.fetchData()
                                }
                            } else if (!viewModel.isEnd && !viewModel.isRefreshing && !viewModel.isLoadMore) {
                                viewModel.isLoadMore = true
                                if (type == "user") viewModel.getFollowList()
                                else viewModel.getHotTopics()
                            }
                        }
                }
            }

            // type=user 澶氶€夊悗纭锛堝搴?AtTopicActivity 鐨?FAB锛?            if (type == "user" && selected.isNotEmpty()) {
                Button(
                    onClick = {
                        onResult(buildUserResult())
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text(
                        text = "瀹屾垚锛園${selected.size}锛?,
                        style = MiuixTheme.textStyles.button,
                    )
                }
            }
        }
    }
}

/** 娴忚琛岋紙鏈€杩戣仈绯讳汉 / 濂藉弸 / 鏈€杩戝弬涓?/ 鐑棬璇濋锛夛細澶村儚 + 鍚嶇О锛坲ser 绫诲瀷甯﹀嬀閫夋锛?*/
@Composable
private fun BrowseRow(
    user: RecentAtUser,
    showCheckbox: Boolean,
    checked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlideImage(
            url = user.avatar,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
        )
        Text(
            text = user.username,
            style = MiuixTheme.textStyles.body1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        if (showCheckbox) {
            Checkbox(
                state = if (checked) ToggleableState.On else ToggleableState.Off,
                onClick = onClick,
            )
        }
    }
}

/** 鎼滅储缁撴灉琛岋紙user/topic 閫氱敤锛屽搴?item_message_mess.xml锛?*/
@Composable
private fun SearchResultRow(
    data: HomeFeedResponse.Data,
    selected: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlideImage(
            url = if (data.entityType == "user") data.userAvatar else data.logo,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
        )
        Text(
            text = (data.username ?: data.title).orEmpty(),
            style = MiuixTheme.textStyles.body1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        if (data.entityType == "user") {
            Checkbox(
                state = if (selected) ToggleableState.On else ToggleableState.Off,
                onClick = onToggle,
            )
        }
    }
}
