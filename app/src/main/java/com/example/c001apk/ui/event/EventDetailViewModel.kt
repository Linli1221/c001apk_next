package com.example.c001apk.ui.event

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.logic.model.EventDetailData
import com.example.c001apk.logic.repository.NetworkRepo
import com.example.c001apk.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 众测/活动详情。
 *
 * 2026-09-19：众测入口 `/event/<id>` 之前走 WebView（www.coolapk.com/event/<id>），
 * 结果加载出「下载 App」落地页。改为原生接口 `GET /v6/event/detail`。
 */
@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val networkRepo: NetworkRepo,
) : ViewModel() {

    private val _detail = MutableLiveData<EventDetailData>()
    val detail: LiveData<EventDetailData> = _detail

    val loading = MutableLiveData<Boolean>()
    val toastText = MutableLiveData<Event<String>>()

    fun load(id: String) {
        if (id.isEmpty()) return
        loading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val data = runCatching {
                networkRepo.getEventDetail(id).firstOrNull()?.getOrNull()?.data
            }.getOrNull()
            data?.let { _detail.postValue(it) }
                ?: toastText.postValue(Event("加载失败，请重试"))
            loading.postValue(false)
        }
    }
}
