package com.example.c001apk.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.c001apk.BuildConfig
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * 本应用自更新检查。
 *
 * 升级信息放在 GitHub 仓库 kongwufang/c001apk_next 的 `update` 分支，
 * 通过 cdn.jsdelivr.net 镜像拉取（国内可直连）：
 *
 *   正式版：https://cdn.jsdelivr.net/gh/kongwufang/c001apk_next@update/stable.json
 *   Beta 版：https://cdn.jsdelivr.net/gh/kongwufang/c001apk_next@update/beta.json
 *
 * JSON 格式：
 * {
 *   "versionName": "1.0.1",
 *   "versionCode": 468,
 *   "changelog": "## 更新日志\n- xxx（markdown 文本）",
 *   "download": [
 *     {"name": "线路一", "url": "https://..."},
 *     {"name": "线路二", "url": "https://..."}
 *   ]
 * }
 *
 * versionCode 大于当前 BuildConfig.VERSION_CODE 才算有更新；
 * 下载一律跳转外部浏览器打开，不在应用内下载。
 */
object UpdateChecker {

    private const val REPO = "kongwufang/c001apk_next"
    const val STABLE_URL = "https://cdn.jsdelivr.net/gh/$REPO@update/stable.json"
    const val BETA_URL = "https://cdn.jsdelivr.net/gh/$REPO@update/beta.json"

    /**
     * 关于页「组织」按钮配置（update 分支 org.json），随时可下发/改名/换链接：
     *
     *   {"buttons": [{"name": "官方群组", "url": "https://..."}]}
     * 也兼容 {"name": "...", "url": "..."} 和裸数组 [{"name": "...", "url": "..."}]
     *
     * 点击后强制跳外部浏览器打开。
     */
    const val ORG_URL = "https://cdn.jsdelivr.net/gh/$REPO@update/org.json"

    /** 每个进程只做一次启动时自动检查（Activity 重建不会重复弹窗） */
    var checkedThisSession = false

    const val CHANNEL_STABLE = "stable"
    const val CHANNEL_BETA = "beta"

    data class DownloadLine(val name: String, val url: String)

    data class UpdateInfo(
        val versionName: String,
        val versionCode: Long,
        val changelog: String,
        val lines: List<DownloadLine>,
    ) {
        val isNewer: Boolean get() = versionCode > BuildConfig.VERSION_CODE
    }

    private val client by lazy { OkHttpClient() }

    /** 拉取并解析；失败返回 null */
    suspend fun fetch(url: String): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val body = client.newCall(Request.Builder().url(url).build())
                .execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    resp.body?.string()
                } ?: return@runCatching null
            parse(body)
        }.getOrNull()
    }

    fun parse(json: String): UpdateInfo? = runCatching {
        val obj = JSONObject(json)
        val lines = obj.optJSONArray("download")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val u = o.optString("url")
                if (u.isBlank()) null
                else DownloadLine(o.optString("name").ifBlank { "线路${i + 1}" }, u)
            }
        }.orEmpty()
        UpdateInfo(
            versionName = obj.optString("versionName"),
            versionCode = obj.optLong("versionCode", -1L),
            changelog = obj.optString("changelog"),
            lines = lines,
        ).takeIf { it.versionCode > 0 && it.lines.isNotEmpty() }
    }.getOrNull()

    data class OrgLink(val name: String, val url: String)

    /** 拉取关于页「组织」按钮配置；失败返回空表（不显示按钮） */
    suspend fun fetchOrgLinks(): List<OrgLink> = withContext(Dispatchers.IO) {
        runCatching {
            val body = client.newCall(Request.Builder().url(ORG_URL).build())
                .execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    resp.body?.string()
                } ?: return@runCatching emptyList()
            parseOrgLinks(body)
        }.getOrDefault(emptyList())
    }

    fun parseOrgLinks(json: String): List<OrgLink> = runCatching {
        val text = json.trim()
        val arr = if (text.startsWith("[")) {
            JSONArray(text)
        } else {
            val obj = JSONObject(text)
            obj.optJSONArray("buttons") ?: obj.optJSONArray("groups")
            ?: return@runCatching listOfNotNull(parseOrgLink(obj))
        }
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { parseOrgLink(it) } }
    }.getOrDefault(emptyList())

    private fun parseOrgLink(o: JSONObject): OrgLink? {
        val url = o.optString("url")
        if (url.isBlank()) return null
        return OrgLink(o.optString("name").ifBlank { "加入群组" }, url)
    }

    /** 下载一定在外部浏览器打开 */
    fun openExternal(context: Context, url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            Toast.makeText(context, "打开下载链接失败", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 更新弹窗：展示更新日志，多线路时可选择线路后跳外部浏览器下载。
     *
     * @param channel [CHANNEL_STABLE] / [CHANNEL_BETA]，「不再提示」会关掉对应开关
     * @param onIgnored 「不再提示」后的回调（用来刷新界面上的开关状态，可空）
     */
    fun showUpdateDialog(
        context: Context,
        info: UpdateInfo,
        channel: String,
        onIgnored: (() -> Unit)? = null
    ) {
        val isBeta = channel == CHANNEL_BETA
        val scrollView = ScrollView(context)
        val textView = TextView(context).apply {
            text = info.changelog.ifBlank { "无更新日志" }
            setTextIsSelectable(true)
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
        }
        scrollView.addView(textView)

        MaterialAlertDialogBuilder(context).apply {
            setTitle("发现新版本${if (isBeta) "（Beta）" else ""} ${info.versionName}")
            setView(scrollView)
            setNegativeButton(android.R.string.cancel, null)
            setNeutralButton("不再提示") { _, _ ->
                if (isBeta) PrefManager.isCheckUpdateBeta = false
                else PrefManager.isCheckUpdateStable = false
                onIgnored?.invoke()
            }
            setPositiveButton("下载") { _, _ ->
                if (info.lines.size == 1) {
                    openExternal(context, info.lines[0].url)
                } else {
                    val names = info.lines.map { it.name }.toTypedArray()
                    MaterialAlertDialogBuilder(context).apply {
                        setTitle("选择下载线路")
                        setItems(names) { d, which ->
                            d.dismiss()
                            openExternal(context, info.lines[which].url)
                        }
                        setNegativeButton(android.R.string.cancel, null)
                        show()
                    }
                }
            }
            show()
        }
    }
}
