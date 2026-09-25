package com.example.c001apk.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.livedata.observeAsState
import com.example.c001apk.R
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Search

/**
 * 棣栭〉楠ㄦ灦锛堣縼绉昏嚜 HomeFragment + fragment_home.xml锛夛細
 * 椤堕儴 TabRow锛坱ab 鍒楄〃鏉ヨ嚜 HomeViewModel.tabListLiveData锛屾暟鎹簱涓虹┖鏃跺洖钀介粯璁?tab锛? * + 鎼滅储鎸夐挳锛圫earchActivity锛? 缂栬緫 Tab 鎸夐挳锛圕opyActivity("homeMenu")锛夛紝
 * 鍐呭鍖烘槸鎻掓Ы锛岀敱瀹夸富鎸?tab 鏍囬鍐冲畾鏀惧摢涓〉闈紙鍏虫敞/澶存潯/鐑/閰峰浘 鈫?HomeFeedScreen锛? * 搴旂敤 鈫?AppList銆佽瘽棰?鏁扮爜 鈫?HomeTopic锛屽潎涓哄叾瀹冭縼绉荤粍鐨勪骇鍑猴級銆? *
 * 鐘舵€佸鐢ㄧ幇鏈?[HomeViewModel]锛圠iveData 鐢?observeAsState 妗ユ帴锛夛紝涓嶅湪鏈枃浠跺啓涓氬姟閫昏緫銆? *
 * @param viewModel 澶嶇敤鐜版湁 HomeViewModel锛坱ab 鏁版嵁婧愩€乸osition锛? * @param onSearchClick 鎼滅储鎸夐挳鐐瑰嚮锛堝師锛氳烦 SearchActivity锛屾帴绾胯疆鐢卞涓诲疄鐜帮級
 * @param onEditTabsClick 缂栬緫 Tab 鎸夐挳鐐瑰嚮锛堝師锛氳烦 CopyActivity("homeMenu")锛? * @param onTabReselected tab 鍐嶆鐐瑰嚮锛堝師銆屽叧娉ㄣ€嶅脊鍒嗙粍閫夋嫨銆佸叾浣欏洖椤?鍒锋柊骞舵樉绀哄簳閮ㄥ鑸級
 * @param tabContent 鍐呭鍖烘彃妲斤細(褰撳墠 tab 鏍囬, tab 鍐嶉€夎鏁板櫒)锛涜鏁板櫒姣忔 tab 閲嶉€?+1锛? * 渚夸簬瀛愰〉闈紙濡?HomeFeedScreen锛夊搷搴斻€屽洖鍒伴《閮?寮瑰叧娉ㄥ垎缁勩€? */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSearchClick: () -> Unit,
    onEditTabsClick: () -> Unit,
    onTabReselected: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    tabContent: @Composable (tabTitle: String, tabReselectedTick: Int) -> Unit,
) {
    val tabList by viewModel.tabListLiveData.observeAsState()
    var tabReselectedTick by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // 涓庢棫 HomeFragment 瑙傚療鑰呬竴鑷达細鏁版嵁搴撶┖ 鈫?鍐欓粯璁?tab锛涘叏閮ㄨ绂佺敤 鈫?鎭㈠榛樿
    LaunchedEffect(tabList) {
        val list = tabList.orEmpty()
        when {
            list.isEmpty() -> viewModel.initTab()
            list.none { it.isEnable } -> viewModel.updateTab(viewModel.defaultList)
        }
    }

    val enableList = remember(tabList) {
        val titles = tabList?.filter { it.isEnable }?.map { it.title }.orEmpty()
        if (titles.isEmpty()) viewModel.defaultList.filter { it.isEnable }.map { it.title }
        else titles
    }

    // 棣栨杩涘叆榛樿钀藉湪銆屽ご鏉°€嶏紙瀵瑰簲鏃?viewModel.isInit 鍒嗘敮锛?    val currentIndex: Int = when {
        selectedIndex in enableList.indices -> selectedIndex
        enableList.isEmpty() -> 0
        else -> enableList.indexOfFirst { it == "澶存潯" }.coerceAtLeast(0)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (enableList.isNotEmpty()) {
                        TabRow(
                            tabs = enableList,
                            selectedTabIndex = currentIndex,
                            onTabSelected = { index ->
                                if (index == currentIndex) {
                                    tabReselectedTick++
                                    enableList.getOrNull(index)?.let { onTabReselected(it) }
                                } else {
                                    selectedIndex = index
                                    viewModel.position = index
                                }
                            },
                            modifier = Modifier.weight(1f),
                            listState = rememberLazyListState(),
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = MiuixIcons.Search,
                            contentDescription = stringResource(R.string.search),
                        )
                    }
                    IconButton(onClick = onEditTabsClick) {
                        Icon(
                            imageVector = MiuixIcons.Edit,
                            contentDescription = stringResource(R.string.edit_tab),
                        )
                    }
                }
                HorizontalDivider()
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (enableList.isNotEmpty()) {
                tabContent(enableList[currentIndex], tabReselectedTick)
            }
        }
    }
}
