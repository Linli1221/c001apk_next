package com.example.c001apk.ui.feed

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.logic.model.CollectionData
import com.example.c001apk.logic.repository.NetworkRepo
import com.example.c001apk.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import javax.inject.Inject

/**
 * 收藏夹（多收藏夹）弹窗的 ViewModel。
 *
 * 接口关系（HAR 实测）：
 * - 列表：`GET /v6/collection/list?uid=&id=<动态id>&type=feed&showDefault=1&page=1`，`isBeCollected=1` 表示已在该夹
 * - 收藏：`POST /v6/collection/addItem`（`id=夹id`、`cancelId` 空）
 * - 取消：同一接口（`id` 空、`cancelId=夹id`）
 * - 新建：`POST /v6/collection/create`（multipart），封面先 `uploadImage`
 * - 改：`POST /v6/collection/update`（表单）
 */
@HiltViewModel
class CollectionPickViewModel @Inject constructor(
    private val networkRepo: NetworkRepo
) : ViewModel() {

    val list = MutableLiveData<List<CollectionData>>()
    val loading = MutableLiveData<Boolean>()
    val toastText = MutableLiveData<Event<String>>()

    fun load(feedId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            loading.postValue(true)
            val data = runCatching {
                networkRepo.getCollectionList("", feedId.orEmpty(), "feed", 1, 1)
                    .firstOrNull()?.getOrNull()?.data
            }.getOrNull()
            list.postValue(data.orEmpty())
            loading.postValue(false)
        }
    }

    /** 点某个收藏夹：已在里面就取消，否则收藏进去 */
    fun toggle(feedId: String, item: CollectionData) {
        viewModelScope.launch(Dispatchers.IO) {
            val collected = item.isBeCollected == 1
            val ok = runCatching {
                networkRepo.addToCollection(
                    id = if (collected) "" else item.id.orEmpty(),
                    cancelId = if (collected) item.id.orEmpty() else "",
                    targetId = feedId,
                    type = "feed"
                ).firstOrNull()?.isSuccess == true
            }.getOrDefault(false)
            toastText.postValue(
                Event(
                    when {
                        !ok -> "操作失败"
                        collected -> "已取消收藏"
                        else -> "已收藏到「${item.title.orEmpty()}」"
                    }
                )
            )
            if (ok) load(feedId)
        }
    }

    /** 新建收藏夹（可选封面，传了 uri 会先上传拿 URL）；新建后若给了动态就顺带收藏进去 */
    fun create(
        feedId: String?,
        title: String,
        description: String,
        isOpen: Int,
        coverUri: Uri?,
        resolver: ContentResolver?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            loading.postValue(true)
            val pic = upload(coverUri, resolver).orEmpty()
            val result = runCatching {
                networkRepo.createCollection(
                    isOpen.toString(), pic, description, title, ""
                ).firstOrNull()
            }.getOrNull()
            val ok = result?.isSuccess == true
            val newId = result?.getOrNull()?.data?.id
            if (ok && !newId.isNullOrEmpty() && !feedId.isNullOrEmpty()) {
                runCatching {
                    networkRepo.addToCollection(newId, "", feedId, "feed").firstOrNull()
                }
            }
            toastText.postValue(Event(if (ok) "创建成功" else "创建失败"))
            load(feedId)
            loading.postValue(false)
        }
    }

    /** 改收藏夹：标题 / 简介 / 公开私密 / 封面 */
    fun update(
        id: String,
        title: String,
        description: String,
        isOpen: Int,
        coverUri: Uri?,
        oldCover: String?,
        resolver: ContentResolver?,
        feedId: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            loading.postValue(true)
            val pic = upload(coverUri, resolver) ?: oldCover.orEmpty()
            val ok = runCatching {
                networkRepo.updateCollection(id, title, description, pic, isOpen)
                    .firstOrNull()?.isSuccess == true
            }.getOrDefault(false)
            toastText.postValue(Event(if (ok) "已保存" else "保存失败"))
            load(feedId)
            loading.postValue(false)
        }
    }

    fun checkCount() {
        viewModelScope.launch(Dispatchers.IO) {
            val msg = runCatching {
                networkRepo.getCollectionCheckCount().firstOrNull()?.getOrNull()?.data
            }.getOrNull()
            if (!msg.isNullOrEmpty()) toastText.postValue(Event(msg))
        }
    }

    /** 收藏夹详情（编辑前取最新数据），成功后通过 [detail] 通知 */
    val detail = MutableLiveData<CollectionData>()
    val deleted = MutableLiveData<Event<Boolean>>()

    fun loadDetail(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val d = runCatching {
                networkRepo.getCollectionDetail(id).firstOrNull()?.getOrNull()?.data
            }.getOrNull()
            if (d == null) toastText.postValue(Event("加载收藏夹信息失败"))
            else publishDetail(d)
        }
    }

    /** lint NullSafeMutableLiveData 看不到 smart-cast，参数显式非空 */
    private fun publishDetail(d: CollectionData) {
        detail.postValue(d)
    }

    /** 清除收藏夹内无效内容（服务端异步执行，约 5 分钟后生效） */
    fun clearUnUse(colId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val r = runCatching {
                networkRepo.removeUnUseCollectionItem(colId).firstOrNull()
            }.getOrNull()
            toastText.postValue(Event(r?.getOrNull()?.data ?: "操作失败"))
        }
    }

    fun delete(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val r = runCatching {
                networkRepo.deleteCollection(id).firstOrNull()
            }.getOrNull()
            val ok = r?.isSuccess == true
            toastText.postValue(Event(r?.getOrNull()?.data ?: if (ok) "删除成功" else "删除失败"))
            if (ok) deleted.postValue(Event(true))
        }
    }

    private suspend fun upload(uri: Uri?, resolver: ContentResolver?): String? {
        if (uri == null || resolver == null) return null
        val bytes = runCatching {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes == null || bytes.isEmpty()) return null
        val md5 = MessageDigest.getInstance("MD5").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        val part = MultipartBody.Part.createFormData(
            "picFile", md5, bytes.toRequestBody("image/*".toMediaTypeOrNull())
        )
        return runCatching {
            networkRepo.uploadCollectionImage(md5, part).firstOrNull()?.getOrNull()?.data
        }.getOrNull()
    }
}
