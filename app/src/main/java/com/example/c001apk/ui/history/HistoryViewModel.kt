package com.example.c001apk.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.logic.model.FeedEntity
import com.example.c001apk.logic.repository.BlackListRepo
import com.example.c001apk.logic.repository.HistoryFavoriteRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 浏览历史。
 *
 * 2026-09-18：本地收藏功能已移除（改用云端收藏 CollectionActivity），
 * 这里只剩「浏览历史」一种数据源，顺带把 assisted type 参数也一起去掉了。
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val blackListRepo: BlackListRepo,
    private val historyRepo: HistoryFavoriteRepo,
) : ViewModel() {

    val browseLiveData: LiveData<List<FeedEntity>> = historyRepo.loadAllHistoryListLive()

    fun saveUid(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            blackListRepo.saveUid(uid)
        }
    }

    fun deleteAll() {
        viewModelScope.launch(Dispatchers.IO) {
            historyRepo.deleteAllHistory()
        }
    }

    fun delete(fid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            historyRepo.deleteHistory(fid)
        }
    }

    fun saveHistory(
        id: String,
        uid: String,
        username: String,
        userAvatar: String,
        deviceTitle: String,
        message: String,
        dateline: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            historyRepo.saveHistory(
                id,
                uid,
                username,
                userAvatar,
                deviceTitle,
                message,
                dateline,
            )
        }
    }

}
