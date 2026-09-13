package com.example.c001apk.ui.settings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.logic.repository.SpamConfigRepo
import com.example.c001apk.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 屏蔽项（关键字 / 用户 / 节点）管理页的 ViewModel */
@HiltViewModel
class SpamShieldViewModel @Inject constructor(
    private val spamConfigRepo: SpamConfigRepo
) : ViewModel() {

    /** word / user / node */
    var type: String = "word"
        private set

    val list = MutableLiveData<List<SpamShieldItem>>()
    val loading = MutableLiveData<Boolean>()
    val toastText = MutableLiveData<Event<String>>()

    fun init(type: String) {
        this.type = type
    }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            loading.postValue(true)
            val ok = runCatching { spamConfigRepo.refresh() }.getOrDefault(false)
            if (!ok && list.value == null)
                toastText.postValue(Event("读取失败，先显示本地缓存"))
            emit()
            loading.postValue(false)
        }
    }

    private fun emit() {
        val config = spamConfigRepo.current
        val items = when (type) {
            "user" -> config?.user.orEmpty().map {
                SpamShieldItem(
                    key = it.uid.orEmpty(),
                    title = it.name ?: it.uid.orEmpty(),
                    desc = it.uid,
                    logo = it.logo,
                    name = it.name
                )
            }

            "node" -> config?.node.orEmpty().map {
                SpamShieldItem(
                    key = it.targetFullId ?: it.id.orEmpty(),
                    title = it.title.orEmpty(),
                    desc = it.targetFullId ?: it.id,
                    logo = it.logo,
                    name = it.title
                )
            }

            else -> config?.custom.orEmpty().map {
                SpamShieldItem(key = it.title.orEmpty(), title = it.title.orEmpty())
            }
        }
        list.postValue(items)
    }

    fun add(data: String, name: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching {
                when (type) {
                    "user" -> spamConfigRepo.addUser(data)
                    "node" -> spamConfigRepo.addNode(data, name.orEmpty())
                    else -> spamConfigRepo.addKeyword(data)
                }
            }.getOrDefault(false)
            toastText.postValue(Event(if (ok) "添加成功" else "添加失败"))
            emit()
        }
    }

    fun remove(item: SpamShieldItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching {
                when (type) {
                    "user" -> spamConfigRepo.cancelUser(item.key)
                    "node" -> spamConfigRepo.cancelNode(item.key, item.name.orEmpty())
                    else -> spamConfigRepo.cancelKeyword(item.key)
                }
            }.getOrDefault(false)
            toastText.postValue(Event(if (ok) "已移除" else "移除失败"))
            emit()
        }
    }
}

data class SpamShieldItem(
    val key: String,
    val title: String,
    val desc: String? = null,
    val logo: String? = null,
    val name: String? = null
)
