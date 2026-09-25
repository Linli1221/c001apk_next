package com.example.c001apk.ui.search

/**
 * c001apk_next 鈫?Miuix 杩佺Щ锛氭悳绱㈤〉锛圕ompose 鐗?SearchFragment锛夈€? *
 * 涓庤€佺晫闈紙fragment_search.xml + SearchFragment锛夌殑瀵瑰簲鍏崇郴锛? *  - 椤舵爮鎼滅储妗?         鈫?Miuix [SearchBar] + [InputField]锛堟敞鎰忚兌鍥婂簳鑹插弬鏁版槸 InputField.color锛? *  - 榛樿鎬?             鈫?[SearchHomePanel]锛氭悳绱㈠巻鍙?+ 鐑棬鎼滅储 + 鐑悳姒滐紙tab + 妯悜姒滃崟锛? *  - 杈撳叆鎬侊紙鎼滅储鑱旀兂锛?  鈫?[SearchSuggestList]
 *  - 鎼滅储缁撴灉            鈫?[resultContent] 鎻掓Ы锛堢敱瀹夸富濉炲叆缁撴灉 tab 鍖猴紝鏈枃浠跺彧璐熻矗鐘舵€佸垏鎹級
 *
 * 鏁版嵁娴佸叏閮ㄥ鐢ㄧ幇鏈?[SearchFragmentViewModel]锛圠iveData 鈫?observeAsState 妗ユ帴锛夛紝涓嶉噸鍐欎换浣曢€昏緫灞傘€? *
 * 鐘舵€佹満锛堝搴旇€佷唬鐮?updatePanelState() / startSearch()锛夛細
 *  - Home    锛氶粯璁ゆ€侊紙鏃犺緭鍏ワ紝鎴栦粠缁撴灉鎬佽繑鍥炲悗锛屽叧閿瘝淇濈暀鍦ㄨ緭鍏ユ閲岋級
 *  - Suggest 锛氱敤鎴锋鍦ㄦ敼璇嶏紙query 闈炵┖涓旀湭澶勪簬缁撴灉鎬侊級鈫?灞曠ず鎼滅储鑱旀兂
 *  - Result  锛歴tartSearch() 鎻愪氦鍚?鈫?灞曠ず resultContent 鎻掓Ы锛屽啓鍏ユ悳绱㈠巻鍙? *
 * 浜や簰缁嗚妭锛? *  - InputField 鍦?expanded 鐢?true 鍙?false 涓斿綋鏃朵粛鎸佺劍鏃讹紝浼氬湪绾?100ms 鍚庤嚜鍔ㄦ竻绌?query
 *    锛圚yperOS 鐨勬敹璧蜂氦浜掞紝瑙?miuix SearchBar.kt 婧愮爜 LaunchedEffect(expanded)锛夈€? *    缁撴灉鎬侀渶瑕佷繚鐣欏叧閿瘝锛屽洜姝や富鍔ㄦ敹璧峰悗鐨勭煭鏃堕棿鍐呭悶鎺夈€屾竻绌轰负绌轰覆銆嶇殑鍥炶皟锛坘eepQueryUntil锛夈€? *  - 杩斿洖閿細缁撴灉鎬?鈫?鍥為粯璁ゆ€侊紙淇濈暀鍏抽敭璇嶏紝涓庤€?SearchResultFragment 杩斿洖琛屼负涓€鑷达級锛? *    鍏朵綑鎯呭喌浜ょ粰 [onBack]锛堣€侀€昏緫鏄?finish SearchActivity锛夈€? */

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.livedata.observeAsState
import com.example.c001apk.R
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 鎼滅储椤典笁鍧楅潰鏉匡細榛樿鎬?/ 鎼滅储鑱旀兂 / 鎼滅储缁撴灉 */
private enum class SearchPanel { Home, Suggest, Result }

/**
 * 鎼滅储椤垫牴 Composable銆? *
 * @param viewModel 澶嶇敤鐜版湁 [SearchFragmentViewModel]锛圚ilt assisted factory 鍒涘缓锛屽涓昏礋璐ｆ瀯閫狅級
 * @param onBack 椤舵爮杩斿洖 / 杩斿洖閿厹搴曪紙鑰侀€昏緫锛歠inish SearchActivity锛? * @param resultContent 鎼滅储缁撴灉 tab 鍖烘彃妲斤紝鍏ュ弬涓烘彁浜ょ殑鍏抽敭璇嶏紱
 *        姒滃崟鑼冨洿鍙傛暟锛坧ageType/pageParam/title锛夊彲鐩存帴浠?viewModel 璇诲彇
 */
@Composable
fun SearchScreen(
    viewModel: SearchFragmentViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    resultContent: @Composable (keyWord: String) -> Unit = {},
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // ---------------- LiveData 鈫?Compose 鐘舵€佹ˉ鎺?----------------

    val historyList by viewModel.blackListLiveData.observeAsState(initial = emptyList())
    val hotSearch by viewModel.hotSearch.observeAsState()
    val suggestList by viewModel.suggest.observeAsState()

    // ---------------- 椤甸潰鐘舵€?----------------

    var query by rememberSaveable { mutableStateOf("") }
    var submittedKeyword by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    /** 涓诲姩鏀惰捣杈撳叆妗嗙殑鏃堕棿鐐?+ 瀹瑰樊锛岀敤鏉ュ悶鎺?InputField 鏀惰捣鏃剁殑鑷姩娓呯┖锛堣鏂囦欢澶存敞閲婏級 */
    val keepQueryUntil = remember { mutableLongStateOf(0L) }

    // 鏃嬭浆灞忓箷鍚?ViewModel 閲屽凡鏈夋暟鎹紝涓嶅繀鍐嶈姹備竴娆★紙涓庤€?SearchFragment 涓€鑷达級
    LaunchedEffect(Unit) {
        if (viewModel.hotSearch.value == null) viewModel.fetchHotSearch()
    }

    val panel = when {
        submittedKeyword.isNotEmpty() && query == submittedKeyword -> SearchPanel.Result
        query.isNotBlank() && expanded -> SearchPanel.Suggest
        else -> SearchPanel.Home
    }

    // ---------------- 浜や簰鍔ㄤ綔 ----------------

    /** 鎻愪氦鎼滅储锛圛Me 鎼滅储閿?/ 鐑瘝 / 鍘嗗彶 / 鑱旀兂鏉＄洰锛夛紝瀵瑰簲鑰?SearchFragment.startSearch() */
    val startSearch: (String) -> Unit = { raw ->
        val keyWord = raw.trim()
        if (keyWord.isEmpty()) {
            Toast.makeText(context, "璇疯緭鍏ュ叧閿瘝", Toast.LENGTH_SHORT).show()
        } else {
            keepQueryUntil.longValue = System.currentTimeMillis() + 600
            query = keyWord
            submittedKeyword = keyWord
            expanded = false
            focusManager.clearFocus()
            keyboardController?.hide()
            viewModel.insertData(keyWord)
        }
    }

    val onQueryChange: (String) -> Unit = { new ->
        if (new.isEmpty() && System.currentTimeMillis() < keepQueryUntil.longValue) {
            // InputField 鏀惰捣瑙﹀彂鐨勮嚜鍔ㄦ竻绌猴細淇濈暀鍏抽敭璇嶏紝涓嶆敼鍙橀潰鏉?        } else {
            query = new
            if (new.isBlank()) {
                submittedKeyword = ""
                viewModel.clearSuggest()
            } else {
                if (new != submittedKeyword) submittedKeyword = ""
                viewModel.fetchSuggest(new)
            }
        }
    }

    val onExpandedChange: (Boolean) -> Unit = { newExpanded ->
        if (!newExpanded && query.isNotBlank()) {
            keepQueryUntil.longValue = System.currentTimeMillis() + 600
            keyboardController?.hide()
        }
        expanded = newExpanded
    }

    // 缁撴灉鎬佸厛鍥為粯璁ゆ€侊紙鍏抽敭璇嶄繚鐣欙級锛屽叾浣欒繑鍥炰氦缁?onBack
    BackHandler { onBack() }
    BackHandler(enabled = submittedKeyword.isNotEmpty()) { submittedKeyword = "" }

    // ---------------- 鐣岄潰 ----------------

    Scaffold(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .background(MiuixTheme.colorScheme.surface)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(start = 10.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = "杩斿洖",
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                }
                SearchBar(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    inputField = {
                        InputField(
                            query = query,
                            onQueryChange = onQueryChange,
                            onSearch = startSearch,
                            expanded = expanded,
                            onExpandedChange = onExpandedChange,
                            label = if (viewModel.pageType.isNotEmpty()) "鍦?${viewModel.title} 涓悳绱?
                            else stringResource(R.string.search),
                            color = MiuixTheme.colorScheme.surfaceContainerHigh,
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = onExpandedChange,
                    content = {},
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when (panel) {
                SearchPanel.Home -> SearchHomePanel(
                    history = historyList,
                    hotItems = hotSearch?.data.orEmpty()
                        .firstOrNull { it.entityTemplate == "hotSearch" }?.entities,
                    hotRankList = hotSearch?.data.orEmpty()
                        .firstOrNull { it.entityTemplate == "searchHotListCard" }?.entities,
                    loading = hotSearch == null,
                    onSearchWord = startSearch,
                    onDeleteHistory = { viewModel.deleteData(it) },
                    onClearAllHistory = { showClearAllDialog = true },
                    onRefreshHot = { viewModel.fetchHotSearch(1) },
                )

                SearchPanel.Suggest -> SearchSuggestList(
                    items = suggestList.orEmpty(),
                    keyword = query,
                    onItemClick = { word, _ -> startSearch(word) },
                    onFillClick = { word ->
                        query = word
                        if (word != submittedKeyword) submittedKeyword = ""
                        viewModel.fetchSuggest(word)
                    },
                )

                SearchPanel.Result -> resultContent(submittedKeyword)
            }
        }

        // 娓呴櫎鍏ㄩ儴鎼滅储鍘嗗彶鐨勭‘璁ゆ锛堣€佷唬鐮佺敤 MaterialAlertDialogBuilder锛岃繖閲屾崲鎴?Miuix OverlayDialog锛?        OverlayDialog(
            show = showClearAllDialog,
            title = stringResource(R.string.clearAllTitle),
            onDismissRequest = { showClearAllDialog = false },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = { showClearAllDialog = false },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = {
                        viewModel.deleteAll()
                        showClearAllDialog = false
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
