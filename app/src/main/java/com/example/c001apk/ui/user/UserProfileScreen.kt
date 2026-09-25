package com.example.c001apk.ui.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.livedata.observeAsState
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.util.PrefManager
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 涓汉涓婚〉锛圲serActivity / UserPagerFragment 鐨?Compose 鐗堬級锛? * 椤舵爮锛堣繑鍥?/ 鏄电О / 鎼滅储 / 鏇村锛? [UserProfileHeader] 璧勬枡澶?+ 鍐呭 tab 鍖烘彃妲姐€? *
 * - 澶嶇敤鐜版湁 [UserViewModel]锛圠iveData 鐢?observeAsState 妗ユ帴锛屼笉閲嶅啓 ViewModel锛夈€? * - tab 鍐呭鐢?[tabContent] 鎻掓Ы鎻愪緵锛堥€氬父鏄?[UserTabViewModel] 椹卞姩鐨勫垪琛級锛? *   鏈枃浠朵笉鍏冲績鍒楄〃鍐呴儴鎬庝箞娓叉煋銆? * - 鍔犺浇 / 澶辫触 / 绌烘€佺敱 activityState 椹卞姩锛涚偣鍑昏烦杞竴寰嬭蛋鍥炶皟鍙傛暟锛? *   鐢辨帴绾挎柟锛圓ctivity/Fragment锛夎礋璐ｇ湡姝ｇ殑 Intent銆? *
 * 绠€鍖栬鏄庯紙鐩稿鑰佺殑 CollapsingToolbarLayout锛夛細澶撮儴鍥哄畾涓嶉殢鍒楄〃鎶樺彔锛? * 鎶樺彔琛屼负鐣欑粰鍚庣画杞粺涓€澶勭悊銆? *
 * @param viewModel 澶嶇敤鐨勭敤鎴疯祫鏂?ViewModel
 * @param tabTitles tab 鏂囨锛岄粯璁?鍔ㄦ€?鐐硅瘎/鍥炬枃/闂瓟/閰峰浘
 * @param onBack 杩斿洖
 * @param onSearch 璺崇敤鎴峰唴鎼滅储锛堣€?UI锛歋earchActivity pageType=user锛? * @param onMore 鏇村鑿滃崟锛堟煡鐪嬭祫鏂?鎷夐粦/鍒嗕韩/涓炬姤绛夛紝鐢辨帴绾挎柟寮硅彍鍗曪級
 * @param onEditProfile 缂栬緫璧勬枡锛堣嚜宸辩殑涓婚〉锛? * @param onMessageClick 绉佷俊锛堝埆浜虹殑涓婚〉锛? * @param onShowFollowList 鍏虫敞/绮変笣鍒楄〃锛宼ype 涓?"follow" / "fans"
 * @param onOpenImage 澶村儚/灏侀潰澶у浘棰勮
 * @param onOpenEquip 瑁呭椤碉紙鑰?UI锛歮.coolapk.com/myDevice/{uid}锛? * @param onToast ViewModel 鐨?toast 鏂囨鍑哄彛
 * @param tabContent 鍐呭 tab 鍖烘彃妲斤紝鍙傛暟涓哄綋鍓嶉€変腑 tab 涓嬫爣
 */
@Composable
fun UserProfileScreen(
    viewModel: UserViewModel,
    tabTitles: List<String> = UserProfileScreenDefaults.TAB_TITLES,
    onBack: () -> Unit,
    onSearch: () -> Unit = {},
    onMore: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onMessageClick: () -> Unit = {},
    onShowFollowList: (uid: String, type: String) -> Unit = { _, _ -> },
    onOpenImage: (url: String) -> Unit = {},
    onOpenEquip: (uid: String) -> Unit = {},
    onToast: (String) -> Unit = {},
    onRetry: () -> Unit = { viewModel.fetchUser() },
    tabContent: @Composable (selectedTabIndex: Int) -> Unit,
) {
    // LiveData 妗ユ帴锛歱rofileState / followState 姣忔 post 閮芥槸鏂?Event 瀹炰緥锛?    // 褰撲綔銆寀serData 宸叉洿鏂般€嶇殑鍒锋柊淇″彿锛寀serData 鏈韩杩樻槸瀛樺湪 ViewModel 閲屻€?    val activityState by viewModel.activityState.observeAsState()
    val profileEvent by viewModel.profileState.observeAsState()
    val followEvent by viewModel.followState.observeAsState()
    val toastEvent by viewModel.toastText.observeAsState()

    val user = remember(profileEvent, followEvent) { viewModel.userData }

    val currentUid = user?.uid ?: viewModel.uid
    val isSelf = PrefManager.isLogin && PrefManager.uid.isNotEmpty() &&
            currentUid == PrefManager.uid

    LaunchedEffect(toastEvent) {
        toastEvent?.getContentIfNotHandledOrReturnNull()?.let { message ->
            message?.let(onToast)
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = user?.username.orEmpty(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "杩斿洖",
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(
                            imageVector = MiuixIcons.Search,
                            contentDescription = "鎼滅储",
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = onMore) {
                        Icon(
                            imageVector = MiuixIcons.More,
                            contentDescription = "鏇村",
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        when (val state = activityState) {
            null, LoadingState.Loading -> {
                // 璧勬枡鍔犺浇涓?                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is LoadingState.LoadingError -> UserProfileErrorState(
                message = state.errMsg,
                paddingValues = paddingValues,
                onRetry = onRetry,
            )

            is LoadingState.LoadingFailed -> UserProfileErrorState(
                message = state.msg,
                paddingValues = paddingValues,
                onRetry = onRetry,
            )

            else -> {
                // LoadingDone锛氬ご閮?+ tab + 鍐呭鎻掓Ы
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    UserProfileHeader(
                        userData = user,
                        isSelf = isSelf,
                        showActionButtons = isSelf || PrefManager.isLogin,
                        onFollowClick = {
                            viewModel.onPostFollowUnFollow(
                                if (user?.isFollow == 1) "/v6/user/unfollow"
                                else "/v6/user/follow"
                            )
                        },
                        onMessageClick = onMessageClick,
                        onEditProfile = onEditProfile,
                        onAvatarClick = { user?.userAvatar?.let(onOpenImage) },
                        onCoverClick = { user?.cover?.let(onOpenImage) },
                        onShowFollowList = { type -> onShowFollowList(viewModel.uid, type) },
                        onEquipClick = { onOpenEquip(viewModel.uid) },
                    )
                    if (tabTitles.isNotEmpty()) {
                        var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
                        if (selectedTabIndex > tabTitles.lastIndex) selectedTabIndex = 0
                        TabRow(
                            tabs = tabTitles,
                            selectedTabIndex = selectedTabIndex,
                            onTabSelected = { selectedTabIndex = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        ) {
                            tabContent(selectedTabIndex)
                        }
                    }
                }
            }
        }
    }
}

/** 璧勬枡鍔犺浇澶辫触 / 绌烘€侊細鏂囨 + 閲嶈瘯銆?*/
@Composable
private fun UserProfileErrorState(
    message: String,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(text = "閲嶈瘯", style = MiuixTheme.textStyles.button)
        }
    }
}

/** tab 鏂囨 / 绫诲瀷甯搁噺锛堜笌鑰?UserPagerFragment 鐨?tabType / tabTitle 瀵归綈锛夈€?*/
object UserProfileScreenDefaults {
    val TAB_TITLES = listOf("鍔ㄦ€?, "鐐硅瘎", "鍥炬枃", "闂瓟", "閰峰浘")
    val TAB_TYPES = listOf("feed", "rating", "article", "question", "coolpic")
}
