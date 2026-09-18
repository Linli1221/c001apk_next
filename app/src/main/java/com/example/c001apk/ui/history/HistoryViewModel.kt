package com.example.c001apk.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.logic.model.HitHistoryData
import com.example.c001apk.logic.repository.NetworkRepo
import com.example.c001apk.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 浏览历史（云端）。
 *
 * 2026-09-19：由本地 Room 历史改为酷安云端历史 `GET /v6/user/hitHistoryList`。
 * 浏览记录由服务端在打开详情（feed/topic/product/apk detail）时自动写入，
 * 客户端无需（也没有）显式上报接口（已实测验证）。
 * 分页参数照抄官方：首页只带 page，后续页带首页首条的 firstItem + 上一页末条的 lastItem。
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val networkRepo: NetworkRepo,
) : ViewModel() {

    private val _historyList = MutableLiveData<List<HitHistoryData>>()
    val historyList: LiveData<List<HitHistoryData>> = _historyList

    /** 首屏加载指示 */
    val initialLoading = MutableLiveData<Boolean>()

    val toastText = MutableLiveData<Event<String>>()

    var page = 1
        private set
    var isEnd = false
        private set
    private var firstItem: String? = null
    private var lastItem: String? = null
    private var isLoading = false

    fun refresh() {
        page = 1
        firstItem = null
        lastItem = null
        isEnd = false
        load()
    }

    fun loadMore() {
        if (isEnd || isLoading) return
        load()
    }

    private fun load() {
        if (isLoading) return
        isLoading = true
        if (page == 1) initialLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            val data = runCatching {
                networkRepo.getHitHistoryList(page, firstItem, lastItem)
                    .firstOrNull()?.getOrNull()?.data
            }.getOrNull()
            when {
                !data.isNullOrEmpty() -> {
                    if (page == 1) firstItem = data.first().id
                    lastItem = data.last().id
                    val merged =
                        if (page == 1) data
                        else _historyList.value.orEmpty() + data
                    _historyList.postValue(merged)
                    page++
                }

                page == 1 -> {
                    // 首页就空（失败或真没有）
                    _historyList.postValue(emptyList())
                    isEnd = true
                    if (data == null) toastText.postValue(Event("加载失败，下拉重试"))
                }

                else -> isEnd = true // 翻到头
            }
            initialLoading.postValue(false)
            isLoading = false
        }
    }
}
