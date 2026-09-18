package com.example.c001apk.ui.applist

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.logic.model.AppItem
import com.example.c001apk.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rikka.core.content.pm.longVersionCodeCompat
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor() : BaseViewModel() {

    val items: MutableLiveData<List<AppItem>> = MutableLiveData()

    override fun fetchData() {}

    fun getItems(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val appList = context.packageManager
                .getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES)
            val newItems = ArrayList<AppItem>()

            appList.forEach { info ->
                if (((info.flags and ApplicationInfo.FLAG_SYSTEM) != ApplicationInfo.FLAG_SYSTEM)) {
                    val packageInfo = context.packageManager.getPackageInfo(info.packageName, 0)

                    val appItem = AppItem().apply {
                        packageName = info.packageName
                        versionName =
                            "${packageInfo.versionName}(${packageInfo.longVersionCodeCompat})"
                        lastUpdateTime = packageInfo.lastUpdateTime
                    }

                    // 自己不算（原来的「应用更新检查」会把安装列表的 MD5 上传给酷安，已整体移除）
                    if (appItem.packageName != "com.example.c001apk")
                        newItems.add(appItem)
                }
            }

            isEnd = true
            items.postValue(newItems.sortedByDescending { it.lastUpdateTime })
            loadingState.postValue(LoadingState.LoadingDone)
        }
    }

}
