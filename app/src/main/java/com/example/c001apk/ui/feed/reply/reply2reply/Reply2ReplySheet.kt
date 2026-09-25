package com.example.c001apk.ui.feed.reply.reply2reply

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.logic.model.TotalReplyResponse
import com.example.c001apk.ui.feed.reply.ReplyListFooter
import com.example.c001apk.ui.feed.reply.ReplyListItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

/*
 * 浜岀骇鍥炲寮瑰眰锛圕ompose 鐗?Reply2ReplyBottomSheetDialog + DialogReplyToReplyBottomSheetBinding锛夈€? *
 * - 寮瑰眰鐢?Miuix [OverlayBottomSheet]锛堥渶 Scaffold 绁栧厛锛岄〉闈㈠唴鑷惈 Scaffold 鍗冲彲锛夈€? * - 鏁版嵁娴佸畬鍏ㄥ鐢?[Reply2ReplyBottomSheetViewModel]锛圠iveData 鐢?observeAsState 妗ユ帴锛夈€? * - 鑰佹祦绋嬩粠寮瑰眰閲岀偣銆屽洖澶嶃€嶈烦 ReplyActivity锛涜繖閲屽彧鍙?[onReply] 鍥炶皟锛? *   鏄惁鍐呭祵 ReplyScreen / 璺宠浆鐢辨帴绾垮眰鍐冲畾锛屾垚鍔熷悗鐢辨帴绾垮眰璋?viewModel.updateReply(data) 鎻掑叆鏂板洖澶嶃€? */

@Composable
fun Reply2ReplySheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    id: String,
    fuid: String,
    uid: String,
    oriReply: List<TotalReplyResponse.Data> = emptyList(),
    position: Int = 0,
    viewModel: Reply2ReplyBottomSheetViewModel = viewModel(),
    onReply: (TotalReplyResponse.Data) -> Unit = {},
    onMore: (TotalReplyResponse.Data) -> Unit = {},
    onToast: (String) -> Unit = {},
) {
    // 涓?Reply2ReplyBottomSheetDialog.setData() 涓€鑷达細鍏堝啓鍏ユ煡璇㈠弬鏁?    LaunchedEffect(show, id, fuid, uid) {
        if (show) {
            viewModel.id = id
            viewModel.fuid = fuid
            viewModel.uid = uid
            viewModel.position = position
            viewModel.oriReply = ArrayList(oriReply)
            // initData()锛氶娆¤繘鍏ユ墠鎷夊彇
            if (viewModel.listSize == -1) {
                viewModel.isEnd = false
                viewModel.isLoadMore = false
                viewModel.fetchReplyTotal()
            }
        }
    }

    val list by viewModel.totalReplyData.observeAsState()
    val footerState by viewModel.footerState.observeAsState()
    val toastEvent by viewModel.toastText.observeAsState()
    val replyList = list.orEmpty()

    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let { onToast(it) }
    }

    fun loadMore() {
        if (!viewModel.isEnd && !viewModel.isRefreshing && !viewModel.isLoadMore) {
            viewModel.isLoadMore = true
            viewModel.fetchReplyTotal()
        }
    }

    OverlayBottomSheet(
        show = show,
        title = "鍥炲璇︽儏",
        onDismissRequest = onDismissRequest,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (replyList.isEmpty()) {
                val state = footerState
                if (state is FooterState.Loading) {
                    ReplyListFooter(
                        footerState = state,
                        onRetry = {},
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    Text(
                        text = "鏆傛棤鍥炲",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                ) {
                    items(replyList, key = { it.id }) { reply ->
                        ReplyListItem(
                            reply = reply,
                            // 棣栨潯鏄鍥炲鐨勫師妤硷紙鑰侀€昏緫 oriReply 鍗犱綅銆佹棤鑳屾櫙锛夛紝鐐瑰嚮鍚屾牱杩涘叆鍥炲
                            onClick = { onReply(reply) },
                            onLikeClick = {
                                viewModel.onPostLikeReply(
                                    reply.id,
                                    reply.userAction?.like ?: 0,
                                )
                            },
                            onMoreClick = { onMore(reply) },
                        )
                        HorizontalDivider(
                            color = MiuixTheme.colorScheme.dividerLine,
                            thickness = 0.5.dp,
                        )
                    }
                    item {
                        ReplyListFooter(
                            footerState = footerState,
                            onRetry = {
                                viewModel.isEnd = false
                                loadMore()
                            },
                        )
                    }
                }
                // 婊氬埌搴曡嚜鍔ㄥ姞杞芥洿澶氾紙瀵瑰簲鑰?OnScrollListener 鐨?loadMore 鍒ゆ柇锛?                LaunchedEffect(listState, replyList.size) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .collect { last ->
                            if (last != null && last >= replyList.size) {
                                loadMore()
                            }
                        }
                }
            }
        }
    }
}
