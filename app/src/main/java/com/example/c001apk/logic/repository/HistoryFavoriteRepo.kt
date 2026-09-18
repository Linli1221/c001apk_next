package com.example.c001apk.logic.repository

import androidx.lifecycle.LiveData
import com.example.c001apk.di.BrowseHistory
import com.example.c001apk.logic.dao.HistoryFavoriteDao
import com.example.c001apk.logic.model.FeedEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 浏览历史仓库。
 *
 * 2026-09-18：本地收藏（feed_favorite.db / @FeedFavorite dao）整块移除，
 * 收藏统一走云端 [com.example.c001apk.ui.collection.CollectionActivity]。
 */
@Singleton
class HistoryFavoriteRepo @Inject constructor(
    @BrowseHistory
    private val browseHistoryDao: HistoryFavoriteDao,
) {

    fun loadAllHistoryListLive(): LiveData<List<FeedEntity>> {
        return browseHistoryDao.loadAllListLive()
    }

    suspend fun insertHistory(history: FeedEntity) {
        browseHistoryDao.insert(history)
    }

    suspend fun checkHistory(fid: String): Boolean {
        return browseHistoryDao.isExist(fid)
    }

    suspend fun saveHistory(
        fid: String,
        uid: String,
        uname: String,
        avatar: String,
        device: String,
        message: String,
        pubDate: String
    ) {
        if (!browseHistoryDao.isExist(fid))
            browseHistoryDao.insert(
                FeedEntity(
                    fid,
                    uid,
                    uname,
                    avatar,
                    device,
                    message,
                    pubDate
                )
            )
    }

    suspend fun deleteHistory(fid: String) {
        browseHistoryDao.delete(fid)
    }

    suspend fun deleteAllHistory() {
        browseHistoryDao.deleteAll()
    }

}
