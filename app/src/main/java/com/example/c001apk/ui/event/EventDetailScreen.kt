package com.example.c001apk.ui.event

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.livedata.observeAsState
import com.example.c001apk.logic.model.EventDetailData
import com.example.c001apk.ui.common.ErrorState
import com.example.c001apk.ui.common.LoadingState as LoadingPlaceholder
import com.example.c001apk.ui.common.SimpleImage
import com.example.c001apk.ui.common.TopBarScaffold
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 浼楁祴/娲诲姩璇︽儏椤碉紙EventDetailActivity + activity_event_detail.xml 鐨?Compose 褰㈡€侊級銆? *
 * 鏁版嵁璧扮幇鏈?[EventDetailViewModel]锛圠iveData 鐢?observeAsState 妗ユ帴锛屼笉閲嶅啓 VM锛夛細
 * `detail` / `loading` / `toastText`銆傝繘鍏ラ〉闈㈣嚜鍔?`viewModel.load(id)`锛堝搴旀棫 onCreate 閲岀殑鍔犺浇锛夛紝
 * 澶辫触鏃跺睍绀洪敊璇€佸苟鍙噸璇曪紱銆岀珛鍗虫姤鍚嶃€嶆墦寮€ `actionUrl`锛堣烦杞柟寮忕敱瀹夸富 [onOpenUrl] 鍐冲畾锛夈€? *
 * **鍙傛暟**
 * @param id 娲诲姩 id锛堟棫 Intent extra `"id"`锛夛紝涓虹┖鏃舵彁绀恒€岀己灏戞椿鍔?id銆嶃€? * @param viewModel 澶嶇敤鐨?[EventDetailViewModel]锛圚ilt 娉ㄥ叆鍚庝紶鍏ワ級銆? * @param modifier 搴旂敤浜庨〉闈㈢殑 [Modifier]銆? * @param onBack 杩斿洖鍥炶皟锛堥《鏍忚繑鍥炵澶达級銆? * @param onOpenUrl 鐐瑰嚮銆岀珛鍗虫姤鍚嶃€嶇殑钀藉湴 url锛堝 startActivity ACTION_VIEW锛夛紝涓嶄紶鍒欓〉闈㈠唴鐩存帴 Toast 鍗犱綅銆? *
 * **绁栧厛瑕佹眰**锛氬繀椤讳綅浜庢牴涓婚 `MiuixAppTheme` 涔嬪唴锛涙湰缁勪欢鑷甫 TopBarScaffold锛圡iuix Scaffold锛夛紝
 * 鍏?content 鍐呭彲鐩存帴浣跨敤 Overlay* 寮圭獥銆? */
@Composable
fun EventDetailScreen(
    id: String,
    viewModel: EventDetailViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
) {
    val detail by viewModel.detail.observeAsState()
    val loading = viewModel.loading.observeAsState().value
    val toast = viewModel.toastText.observeAsState().value
    val context = LocalContext.current

    // 瀵瑰簲鏃?onCreate锛氭湁 id 灏卞姞杞斤紝娌℃湁灏辨彁绀?    LaunchedEffect(id) {
        if (id.isNotEmpty()) {
            viewModel.load(id)
        } else {
            Toast.makeText(context, "缂哄皯娲诲姩 id", Toast.LENGTH_SHORT).show()
        }
    }

    // toastText 涓€娆℃€т簨浠?鈫?Toast
    LaunchedEffect(toast) {
        toast?.getContentIfNotHandledOrReturnNull()?.let { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    TopBarScaffold(
        title = "浼楁祴璇︽儏",
        onBack = onBack,
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            val data = detail
            when {
                data != null -> EventDetailContent(
                    data = data,
                    onOpenUrl = { url ->
                        if (url.isNotEmpty()) onOpenUrl(url)
                        else Toast.makeText(context, "鏆傛棤鎶ュ悕鍏ュ彛", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // 鍔犺浇澶辫触锛坙oading 宸叉敹鏁涗负 false 涓旀病鏈夋暟鎹級鈫?閿欒鎬?+ 閲嶈瘯
                loading == false -> ErrorState(
                    onRetry = { if (id.isNotEmpty()) viewModel.load(id) },
                    modifier = Modifier.fillMaxSize(),
                    message = "鍔犺浇澶辫触锛岃閲嶈瘯",
                    retryText = "閲嶈瘯",
                )

                // loading 涓?null锛堝皻鏈紑濮嬶級/ true锛氬姞杞戒腑
                else -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/** 璇︽儏姝ｆ枃锛歜anner + 鏍囬 + 璧炲姪鏂?鎶ュ悕浜烘暟/鎶ュ悕鏃堕棿 + 瑙勫垯 + 濂栧搧 + 鎶ュ悕鎸夐挳锛堝搴旀棫 XML 鐨?LinearLayout锛夈€?*/
@Composable
private fun EventDetailContent(
    data: EventDetailData,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        val logo = data.logo
        if (!logo.isNullOrEmpty()) {
            SimpleImage(
                url = logo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentDescription = "娲诲姩 banner",
            )
        }

        val title = data.title
        if (!title.isNullOrBlank()) {
            Text(
                text = title.orEmpty(),
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            )
        }

        val sponsor = data.sponsorUser ?: data.sponsorUserList?.firstOrNull()?.username
        if (!sponsor.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "璧炲姪鏂癸細$sponsor",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        val regNum = data.showRegNum ?: data.regNum
        if (!regNum.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "鎶ュ悕浜烘暟锛?regNum",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        val start = data.timeRegStart
        val end = data.timeRegEnd
        if (start != null && end != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "鎶ュ悕鏃堕棿锛?{fmtDate(start)} ~ ${fmtDate(end)}",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        val rule = data.noticeRule
        if (!rule.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = rule.orEmpty(),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        val prizes = data.sponsorPrizeList.orEmpty()
            .mapNotNull { it.title }
            .joinToString("銆?)
        if (prizes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "濂栧搧锛?prizes",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        val applyUrl = data.actionUrl.orEmpty()
        if (applyUrl.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onOpenUrl(applyUrl) },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text("绔嬪嵆鎶ュ悕")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** 鏃?EventDetailActivity.fmt()锛氱绾ф椂闂存埑 鈫?yyyy-MM-dd锛堟湰鍦版椂鍖猴級銆?*/
private fun fmtDate(ts: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(ts * 1000L))
